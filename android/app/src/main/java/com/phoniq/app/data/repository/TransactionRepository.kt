package com.phoniq.app.data.repository

import com.phoniq.app.data.db.dao.AccountDao
import com.phoniq.app.data.db.dao.BudgetDao
import com.phoniq.app.data.db.dao.TransactionDao
import com.phoniq.app.data.db.entity.AccountEntity
import com.phoniq.app.data.db.entity.BudgetEntity
import com.phoniq.app.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
