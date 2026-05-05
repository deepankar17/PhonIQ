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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(txn: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(txns: List<TransactionEntity>)

    @Update
    suspend fun update(txn: TransactionEntity)
}
