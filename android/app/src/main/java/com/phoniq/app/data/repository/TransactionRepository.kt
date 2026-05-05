package com.phoniq.app.data.repository

import com.phoniq.app.data.db.dao.AccountDao
import com.phoniq.app.data.db.dao.BudgetDao
import com.phoniq.app.data.db.dao.TransactionDao
import com.phoniq.app.data.db.entity.AccountEntity
import com.phoniq.app.data.db.entity.BudgetEntity
import com.phoniq.app.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
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

    fun budgetsForCurrentMonth(): Flow<List<BudgetEntity>> =
        budgetDao.observeForMonth(currentMonthYear())

    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    private fun currentMonthYear(): String = LocalDate.now().format(monthFormatter)

    fun currentMonthEpochRange(): Pair<Long, Long> {
        val now = LocalDate.now()
        val startOfMonth = now.withDayOfMonth(1).atStartOfDay()
            .toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
        val endOfMonth = now.withDayOfMonth(now.lengthOfMonth())
            .atTime(23, 59, 59)
            .toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
        return startOfMonth to endOfMonth
    }

    suspend fun addTransaction(txn: TransactionEntity) = transactionDao.insert(txn)

    suspend fun setBudget(category: String, limitRupees: Double) {
        val month = currentMonthYear()
        val existing = budgetDao.findByCategoryAndMonth(category, month)
        if (existing != null) {
            budgetDao.update(existing.copy(monthlyLimit = limitRupees))
        } else {
            budgetDao.insert(BudgetEntity(category = category, monthlyLimit = limitRupees, monthYear = month))
        }
    }
}
