package com.phoniq.app.data.repository

import com.phoniq.app.data.db.dao.AccountDao
import com.phoniq.app.data.db.dao.BudgetDao
import com.phoniq.app.data.db.dao.TransactionDao
import com.phoniq.app.data.db.entity.AccountEntity
import com.phoniq.app.data.db.entity.BudgetEntity
import com.phoniq.app.data.db.entity.TransactionEntity
import com.phoniq.app.data.model.TxnNotificationMoneyInsights
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val accountDao: AccountDao,
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.observeAll()
    val allAccounts: Flow<List<AccountEntity>> = accountDao.observeAll()

    fun transactionsForPeriod(fromEpoch: Long, toEpoch: Long): Flow<List<TransactionEntity>> =
        transactionDao.observeForPeriod(fromEpoch, toEpoch)

    fun totalSpentInPeriod(fromEpoch: Long, toEpoch: Long): Flow<Double?> =
        transactionDao.totalSpentInPeriod(fromEpoch, toEpoch)

    fun netBalanceForAccount(accountId: Long): Flow<Double> =
        transactionDao.netBalanceForAccount(accountId)

    fun budgetsForMonth(monthYear: String): Flow<List<BudgetEntity>> =
        budgetDao.observeForMonth(monthYear)

    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    fun monthYearKey(year: Int, month: Int): String =
        LocalDate.of(year, month, 1).format(monthFormatter)

    /** Inclusive start, inclusive end — uses system default zone for transaction `date` epoch millis. */
    fun monthEpochRangeFor(year: Int, month: Int): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val first = LocalDate.of(year, month, 1)
        val start = first.atStartOfDay(zone).toInstant().toEpochMilli()
        val last = first.withDayOfMonth(first.lengthOfMonth())
        val end = last.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
        return start to end
    }

    /**
     * Offline aggregates for transaction notification copy: current calendar month spend (debits),
     * optional “safe to spend / day” when user has monthly budgets set.
     * Adds [freshDebitAmount] once so the just-received debit is reflected before SMS sync completes.
     */
    suspend fun loadTxnNotificationInsights(freshDebitAmount: Double): TxnNotificationMoneyInsights {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val monthKey = monthYearKey(today.year, today.monthValue)
        val (start, end) = monthEpochRangeFor(today.year, today.monthValue)
        val spentBase = transactionDao.sumDebitsInPeriodSync(start, end)
        val spent = spentBase + freshDebitAmount.coerceAtLeast(0.0)
        val budgetTotalRaw = budgetDao.sumBudgetLimitsForMonthSync(monthKey)
        val budgetTotal = budgetTotalRaw.takeIf { it > 0.0 }
        val lastDay = today.withDayOfMonth(today.lengthOfMonth())
        val daysLeftInclusive = (lastDay.toEpochDay() - today.toEpochDay() + 1).coerceAtLeast(1)
        val remaining = ((budgetTotal ?: 0.0) - spent).coerceAtLeast(0.0)
        val safePerDay =
            if (budgetTotal != null) {
                remaining / daysLeftInclusive.toDouble()
            } else {
                null
            }
        val monthDisplay =
            today.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault()))
        return TxnNotificationMoneyInsights(
            monthSpendTotal = spent,
            monthDisplayName = monthDisplay,
            monthYear = today.year,
            budgetTotal = budgetTotal,
            safeToSpendPerDay = safePerDay,
        )
    }

    suspend fun addTransaction(txn: TransactionEntity) = transactionDao.insert(txn)

    suspend fun setBudget(category: String, limitRupees: Double, monthYear: String) {
        val existing = budgetDao.findByCategoryAndMonth(category, monthYear)
        if (existing != null) {
            budgetDao.update(existing.copy(monthlyLimit = limitRupees))
        } else {
            budgetDao.insert(BudgetEntity(category = category, monthlyLimit = limitRupees, monthYear = monthYear))
        }
    }
}
