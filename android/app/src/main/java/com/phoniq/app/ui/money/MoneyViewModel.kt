package com.phoniq.app.ui.money

import android.content.Context
import android.text.format.DateUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phoniq.app.data.db.entity.BudgetEntity
import com.phoniq.app.data.db.entity.TransactionEntity
import com.phoniq.app.data.model.BudgetStatus
import com.phoniq.app.data.model.CategorySpend
import com.phoniq.app.data.model.MoneyIntelligenceSummary
import com.phoniq.app.data.model.MoneySummary
import com.phoniq.app.data.model.RecentTransaction
import com.phoniq.app.data.model.buildMoneyIntelligence
import com.phoniq.app.data.mapper.toMessageThreads
import com.phoniq.app.data.model.MessageThreadCategory
import com.phoniq.app.data.model.billDueHintLabel
import com.phoniq.app.data.repository.TransactionRepository
import com.phoniq.app.data.repository.SmsRepository
import com.phoniq.app.notification.OverBudgetNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class MonthPickerBounds(
    val earliest: YearMonth,
    val latest: YearMonth,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MoneyViewModel(
    private val transactionRepository: TransactionRepository,
    private val appContext: Context? = null,
    private val smsRepository: SmsRepository,
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now())

    val selectedMonthState: StateFlow<YearMonth> = selectedMonth.asStateFlow()

    fun selectMonth(year: Int, month: Int) {
        val ym = runCatching { YearMonth.of(year, month) }.getOrElse { YearMonth.now() }
        val b = monthPickerBounds.value
        selectedMonth.value =
            when {
                ym.isBefore(b.earliest) -> b.earliest
                ym.isAfter(b.latest) -> b.latest
                else -> ym
            }
    }

    val monthPickerBounds: StateFlow<MonthPickerBounds> =
        transactionRepository.allTransactions
            .map { txns ->
                val now = YearMonth.now()
                val zone = ZoneId.systemDefault()
                val fromData =
                    txns.minOfOrNull { it.date }?.let { ms ->
                        YearMonth.from(Instant.ofEpochMilli(ms).atZone(zone).toLocalDate())
                    }
                val earliest = (fromData ?: now).let { e -> if (e.isAfter(now)) now else e }
                MonthPickerBounds(earliest = earliest, latest = now)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                MonthPickerBounds(YearMonth.now(), YearMonth.now()),
            )

    val currentMonthTransactions: StateFlow<List<TransactionEntity>> =
        selectedMonth
            .flatMapLatest { ym ->
                val (from, to) = transactionRepository.monthEpochRangeFor(ym.year, ym.monthValue)
                transactionRepository.transactionsForPeriod(from, to)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentMonthBudgets: StateFlow<List<BudgetEntity>> =
        selectedMonth
            .flatMapLatest { ym ->
                val key = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                transactionRepository.budgetsForMonth(key)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---------------------------------------------------------------------------
    // Derived UI models from Room data
    // ---------------------------------------------------------------------------

    /** Derived from Room transactions for the selected month. */
    val derivedSummary: StateFlow<MoneySummary?> =
        combine(selectedMonth, currentMonthTransactions) { ym, txns ->
            val label = monthLabelFor(ym)
            if (txns.isEmpty()) {
                return@combine MoneySummary(
                    monthLabel = label,
                    spentLabel = "₹0",
                    savingsLabel = "+₹0",
                    currencyHint = "INR",
                    budgetProgress = 0f,
                    budgetCaption = "0 transactions this month",
                )
            }
            val spent = txns.filter { it.txnType == "DEBIT" }.sumOf { it.amount }
            val credit = txns.filter { it.txnType == "CREDIT" }.sumOf { it.amount }
            MoneySummary(
                monthLabel = label,
                spentLabel = "₹${"%,d".format(spent.roundToInt())}",
                savingsLabel = "+₹${credit.roundToInt()}",
                currencyHint = "INR",
                budgetProgress = 0.5f,
                budgetCaption = "${txns.size} transactions this month",
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val derivedCategories: StateFlow<List<CategorySpend>> =
        currentMonthTransactions.map { txns ->
            if (txns.isEmpty()) return@map emptyList()
            val debits = txns.filter { it.txnType == "DEBIT" }
            val total = debits.sumOf { it.amount }.takeIf { it > 0 } ?: 1.0

            CATEGORY_META.map { (key, meta) ->
                val amount = debits.filter { it.category == key }.sumOf { it.amount }
                CategorySpend(
                    name = meta.displayName,
                    amountLabel = "₹${amount.roundToInt()}",
                    fraction = (amount / total).toFloat().coerceIn(0f, 1f),
                    emoji = meta.emoji,
                    budgetLabel = "of ₹0 budget",
                )
            }.filter { it.fraction > 0f }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val derivedRecentTransactions: StateFlow<List<RecentTransaction>> =
        currentMonthTransactions.map { txns ->
            txns.sortedByDescending { it.date }.take(20).map { it.toRecentTransaction() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Per-account balance and last movement from all parsed SMS transactions (passbook-style).
     */
    val accountBalances: StateFlow<List<AccountBalance>> =
        combine(
            transactionRepository.allAccounts,
            transactionRepository.allTransactions,
        ) { accounts, txns ->
            if (accounts.isEmpty()) return@combine emptyList()
            val byAccount = txns.filter { it.accountId != null }.groupBy { it.accountId!! }
            accounts.map { acct ->
                val list = byAccount[acct.id].orEmpty()
                val net =
                    list.sumOf { txn ->
                        if (txn.txnType == "CREDIT") txn.amount else -txn.amount
                    }
                val last = list.maxByOrNull { it.date }
                val lastLabel =
                    last?.let { l ->
                        val sign = if (l.txnType == "CREDIT") "+" else "−"
                        val merchant = l.merchant?.takeIf { it.isNotBlank() } ?: l.category
                        "$sign₹${"%,.0f".format(l.amount)} · $merchant"
                    }
                val lastTime =
                    last?.date?.let { d ->
                        DateUtils.getRelativeTimeSpanString(
                            d,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                        ).toString()
                    }
                AccountBalance(
                    accountId = acct.id,
                    bankName = acct.bankName,
                    last4 = acct.last4,
                    accountType = acct.accountType,
                    netBalance = net,
                    lastMovementLabel = lastLabel,
                    lastMovementTimeLabel = lastTime,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Monthly spending for the past 6 months (for Vico bar chart). */
    val monthlySpends: StateFlow<List<MonthlySpend>> =
        transactionRepository.allTransactions.map { txns ->
            val zone = ZoneId.systemDefault()
            val now = LocalDate.now()
            (5 downTo 0).map { monthsBack ->
                val target = now.minusMonths(monthsBack.toLong())
                val spent =
                    txns.filter { txn ->
                        if (txn.txnType != "DEBIT") return@filter false
                        val txnDate = Instant.ofEpochMilli(txn.date).atZone(zone).toLocalDate()
                        txnDate.year == target.year && txnDate.monthValue == target.monthValue
                    }.sumOf { it.amount }
                MonthlySpend(month = target.month, year = target.year, totalSpent = spent)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Heuristic debits that look recurring / transfer-like with an upcoming window. */
    val moneyReminderLines: StateFlow<List<MoneyReminderLine>> =
        transactionRepository.allTransactions
            .map { buildMoneyReminderLines(it) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Indian financial year (Apr–Mar) salary credits totals from parsed SMS. */
    val salaryFySummary: StateFlow<SalaryFySummary?> =
        transactionRepository.allTransactions
            .map { buildSalaryFySummary(it) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Bill threads with due / overdue hints (read-only, same heuristics as Messages). */
    val upcomingBillHints: StateFlow<List<UpcomingBillHint>> =
        smsRepository.allMessages
            .map { messages ->
                messages
                    .toMessageThreads()
                    .asSequence()
                    .filter { !it.isArchived && MessageThreadCategory.Bill in it.categories }
                    .mapNotNull { t ->
                        t.billDueHintLabel()?.let { hint -> UpcomingBillHint(threadId = t.id, title = t.title, hint = hint) }
                    }
                    .distinctBy { it.threadId }
                    .take(16)
                    .toList()
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** All-time parsed-transaction heuristics (on-device). */
    val moneyIntelligence: StateFlow<MoneyIntelligenceSummary> =
        transactionRepository.allTransactions
            .map { buildMoneyIntelligence(it) }
            .flowOn(Dispatchers.Default)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                MoneyIntelligenceSummary.Empty,
            )

    /** Recent parsed rows that look investment / SIP / broker related (read-only SMS IQ slice). */
    val investmentHighlightTransactions: StateFlow<List<RecentTransaction>> =
        transactionRepository.allTransactions
            .map { txns ->
                txns
                    .asSequence()
                    .filter { txn ->
                        if (txn.category == "INVESTMENT") return@filter true
                        val m = txn.merchant?.uppercase(Locale.US).orEmpty()
                        INVESTMENT_MERCHANT_HINTS.any { hint -> m.contains(hint) }
                    }
                    .sortedByDescending { it.date }
                    .take(12)
                    .map { it.toRecentTransaction() }
                    .toList()
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Live budget status per category: spent amount vs user-set limit. */
    val budgetStatuses: StateFlow<List<BudgetStatus>> =
        combine(currentMonthTransactions, currentMonthBudgets) { txns, budgets ->
            CATEGORY_META.map { (key, meta) ->
                val spent = txns.filter { it.txnType == "DEBIT" && it.category == key }.sumOf { it.amount }
                val limit = budgets.find { it.category == key }?.monthlyLimit ?: 0.0
                val fraction = if (limit > 0) (spent / limit).toFloat().coerceIn(0f, 1f) else 0f
                BudgetStatus(
                    category = key,
                    displayName = meta.displayName,
                    emoji = meta.emoji,
                    spent = spent,
                    limit = limit,
                    fraction = fraction,
                    isOverBudget = limit > 0 && spent > limit,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---------------------------------------------------------------------------

    fun addManualTransaction(txn: TransactionEntity) {
        viewModelScope.launch { transactionRepository.addTransaction(txn) }
    }

    fun exportTransactionsCsv(onResult: (String) -> Unit) {
        if (appContext == null) return
        viewModelScope.launch {
            val txns = transactionRepository.allTransactions.first()
            val result = com.phoniq.app.export.CsvExporter.exportTransactions(appContext, txns)
            onResult(result.getOrElse { "Export failed: ${it.message}" })
        }
    }

    fun exportTransactionsPdf(onResult: (String) -> Unit) {
        if (appContext == null) return
        viewModelScope.launch {
            val txns = transactionRepository.allTransactions.first()
            val monthLabel =
                java.time.LocalDate
                    .now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
            com.phoniq.app.export.PdfExporter.exportTransactions(
                context = appContext,
                transactions = txns,
                monthLabel = monthLabel,
                onResult = onResult,
            )
        }
    }

    fun setBudget(category: String, limitRupees: Double) {
        viewModelScope.launch {
            val monthKey = transactionRepository.monthYearKey(selectedMonth.value.year, selectedMonth.value.monthValue)
            transactionRepository.setBudget(category, limitRupees, monthKey)
            if (appContext != null && limitRupees > 0) {
                val spent = budgetStatuses.value.find { it.category == category }?.spent ?: 0.0
                if (spent > limitRupees) {
                    OverBudgetNotifier.notify(appContext, category, spent, limitRupees)
                }
            }
        }
    }

    class Factory(
        private val repo: TransactionRepository,
        private val ctx: Context? = null,
        private val smsRepo: SmsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MoneyViewModel(repo, ctx, smsRepo) as T
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private data class CategoryMeta(val displayName: String, val emoji: String)

private val CATEGORY_META =
    mapOf(
        "FOOD" to CategoryMeta("Food & Dining", "🍽️"),
        "SHOPPING" to CategoryMeta("Shopping", "🛍️"),
        "BILLS" to CategoryMeta("Bills & Utilities", "📄"),
        "TRANSPORT" to CategoryMeta("Transport", "🚗"),
        "EMI" to CategoryMeta("EMI / Loan", "🏦"),
        "HEALTH" to CategoryMeta("Health", "💊"),
        "ENTERTAINMENT" to CategoryMeta("Entertainment", "🎬"),
        "INVESTMENT" to CategoryMeta("Investments", "📈"),
        "OTHER" to CategoryMeta("Others", "📦"),
    )

private val INVESTMENT_MERCHANT_HINTS =
    arrayOf(
        "SIP",
        "MF",
        "MUTUAL",
        "NSDL",
        "CDSL",
        "ZERODHA",
        "GROWW",
        "HDFC MF",
        "ICICI PRU",
        "CAMS",
        "KFIN",
    )

private val dateFormatter = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())

private fun TransactionEntity.toRecentTransaction() =
    RecentTransaction(
        merchant = merchant ?: "Unknown",
        dateLine = dateFormatter.format(Date(date)),
        amountLabel = if (txnType == "CREDIT") "+₹${amount.roundToInt()}" else "₹${amount.roundToInt()}",
        isCredit = txnType == "CREDIT",
        emoji = CATEGORY_META[category]?.emoji ?: "💳",
        categoryTag = category,
    )

private fun monthLabelFor(ym: YearMonth): String {
    val fmt = DateTimeFormatter.ofPattern("MMMM yyyy · 'Total Spent'", Locale.getDefault())
    return ym.atDay(1).format(fmt)
}
