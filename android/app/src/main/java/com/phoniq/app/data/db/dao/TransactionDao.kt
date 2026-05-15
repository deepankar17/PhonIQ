package com.phoniq.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.phoniq.app.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY date DESC")
    fun observeByCategory(category: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE date >= :fromEpoch AND date <= :toEpoch
        ORDER BY date DESC
    """)
    fun observeForPeriod(fromEpoch: Long, toEpoch: Long): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE txn_type = 'DEBIT' AND date >= :fromEpoch AND date <= :toEpoch")
    fun totalSpentInPeriod(fromEpoch: Long, toEpoch: Long): Flow<Double?>

    /** Returns all transactions for a specific account, newest first. */
    @Query("SELECT * FROM transactions WHERE account_id = :accountId ORDER BY date DESC")
    fun observeForAccount(accountId: Long): Flow<List<TransactionEntity>>

    /** Net balance for an account: SUM(credits) - SUM(debits). */
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN txn_type = 'CREDIT' THEN amount ELSE 0 END), 0)
             - COALESCE(SUM(CASE WHEN txn_type = 'DEBIT'  THEN amount ELSE 0 END), 0)
        FROM transactions WHERE account_id = :accountId
    """)
    fun netBalanceForAccount(accountId: Long): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(txn: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(txns: List<TransactionEntity>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Update
    suspend fun update(txn: TransactionEntity)
}
