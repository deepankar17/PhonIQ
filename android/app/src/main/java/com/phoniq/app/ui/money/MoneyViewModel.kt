package com.phoniq.app.ui.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phoniq.app.data.db.entity.BudgetEntity
import com.phoniq.app.data.db.entity.TransactionEntity
import com.phoniq.app.data.model.CategorySpend
import com.phoniq.app.data.model.MoneySummary
import com.phoniq.app.data.model.RecentTransaction
import com.phoniq.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    // ---------------------------------------------------------------------------

    fun addManualTransaction(txn: TransactionEntity) {
        viewModelScope.launch { transactionRepository.addTransaction(txn) }
    }

    fun setBudget(category: String, limitRupees: Double) {
        viewModelScope.launch { transactionRepository.setBudget(category, limitRupees) }
    }

    class Factory(private val repo: TransactionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MoneyViewModel(repo) as T
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
