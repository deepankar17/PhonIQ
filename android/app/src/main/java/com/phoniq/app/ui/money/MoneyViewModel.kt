package com.phoniq.app.ui.money

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phoniq.app.data.db.entity.BudgetEntity
import com.phoniq.app.data.db.entity.TransactionEntity
import com.phoniq.app.data.model.BudgetStatus
import com.phoniq.app.data.model.CategorySpend
import com.phoniq.app.data.model.MoneySummary
import com.phoniq.app.data.model.RecentTransaction
import com.phoniq.app.data.repository.TransactionRepository
import com.phoniq.app.ui.money.AccountBalance
import com.phoniq.app.ui.money.MonthlySpend
import java.time.Instant
import java.time.ZoneId
import com.phoniq.app.notification.OverBudgetNotifier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MoneyViewModel(
    private val transactionRepository: TransactionRepository,
    private val appContext: Context? = null,
) : ViewModel() {

    private val monthRange = transactionRepository.currentMonthEpochRange()

    val currentMonthTransactions: StateFlow<List<TransactionEntity>> =
        transactionRepository.transactionsForPeriod(monthRange.first, monthRange.second)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalSpentThisMonth: StateFlow<Double?> =
        transactionRepository.totalSpentInPeriod(monthRange.first, monthRange.second)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val currentMonthBudgets: StateFlow<List<BudgetEntity>> =
        transactionRepository.budgetsForCurrentMonth()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---------------------------------------------------------------------------
    // Derived UI models from Room data
    // ---------------------------------------------------------------------------

    /** Derived from Room transactions; null when DB is empty (caller uses SampleData). */
    val derivedSummary: StateFlow<MoneySummary?> =
        currentMonthTransactions.map { txns ->
            if (txns.isEmpty()) return@map null
            val spent = txns.filter { it.txnType == "DEBIT" }.sumOf { it.amount }
            val credit = txns.filter { it.txnType == "CREDIT" }.sumOf { it.amount }
            MoneySummary(
                monthLabel = currentMonthLabel(),
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
            txns.take(20).map { it.toRecentTransaction() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Per-account net balance (credits - debits) derived from all parsed SMS transactions.
     * Accounts are populated by SmsRepository when it parses bank messages.
     */
    val accountBalances: StateFlow<List<AccountBalance>> =
        transactionRepository.allAccounts.flatMapLatest { accounts ->
            if (accounts.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                combine(accounts.map { acct ->
                    transactionRepository.netBalanceForAccount(acct.id).map { net ->
                        AccountBalance(
                            accountId = acct.id,
                            bankName = acct.bankName,
                            last4 = acct.last4,
                            accountType = acct.accountType,
                            netBalance = net,
                        )
                    }
                }) { it.toList() }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Monthly spending for the past 6 months (for Vico bar chart). */
    val monthlySpends: StateFlow<List<MonthlySpend>> =
        transactionRepository.allTransactions.map { txns ->
            val zone = ZoneId.systemDefault()
            val now = java.time.LocalDate.now()
            (5 downTo 0).map { monthsBack ->
                val target = now.minusMonths(monthsBack.toLong())
                val spent = txns.filter { txn ->
                    if (txn.txnType != "DEBIT") return@filter false
                    val txnDate = Instant.ofEpochMilli(txn.date).atZone(zone).toLocalDate()
                    txnDate.year == target.year && txnDate.monthValue == target.monthValue
                }.sumOf { it.amount }
                MonthlySpend(month = target.month, year = target.year, totalSpent = spent)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
            val monthLabel = java.time.LocalDate.now()
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
            transactionRepository.setBudget(category, limitRupees)
            // Check and fire over-budget notification
            if (appContext != null && limitRupees > 0) {
                val spent = budgetStatuses.value.find { it.category == category }?.spent ?: 0.0
                if (spent > limitRupees) {
                    OverBudgetNotifier.notify(appContext, category, spent, limitRupees)
                }
            }
        }
    }

    class Factory(private val repo: TransactionRepository, private val ctx: Context? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MoneyViewModel(repo, ctx) as T
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private data class CategoryMeta(val displayName: String, val emoji: String)

private val CATEGORY_META = mapOf(
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

private val dateFormatter = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())

private fun TransactionEntity.toRecentTransaction() = RecentTransaction(
    merchant = merchant ?: "Unknown",
    dateLine = dateFormatter.format(Date(date)),
    amountLabel = if (txnType == "CREDIT") "+₹${amount.roundToInt()}" else "₹${amount.roundToInt()}",
    isCredit = txnType == "CREDIT",
    emoji = CATEGORY_META[category]?.emoji ?: "💳",
    categoryTag = category,
)

private fun currentMonthLabel(): String {
    val fmt = DateTimeFormatter.ofPattern("MMMM yyyy · 'Total Spent'", Locale.getDefault())
    return LocalDate.now().format(fmt)
}
