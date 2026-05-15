package com.phoniq.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.phoniq.app.data.db.entity.CallLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CallLogDao {
    @Query("SELECT * FROM call_log ORDER BY timestamp DESC")
    abstract fun observeAll(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_log WHERE type = :type ORDER BY timestamp DESC")
    abstract fun observeByType(type: String): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_log WHERE number = :number ORDER BY timestamp DESC")
    abstract fun observeByNumber(number: String): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(entry: CallLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(entries: List<CallLogEntity>)

    @Transaction
    open suspend fun replaceAll(entries: List<CallLogEntity>) {
        deleteAll()
        if (entries.isNotEmpty()) {
            insertAll(entries)
        }
    }

    @Update
    abstract suspend fun update(entry: CallLogEntity)

    @Query("SELECT * FROM call_log WHERE number = :number ORDER BY timestamp DESC LIMIT 1")
    abstract suspend fun getLatestByNumber(number: String): CallLogEntity?

    @Query("SELECT * FROM call_log ORDER BY timestamp DESC LIMIT 400")
    abstract suspend fun recentSnapshot(): List<CallLogEntity>

    @Query("DELETE FROM call_log")
    abstract suspend fun deleteAll()

    @Delete
    abstract suspend fun delete(entry: CallLogEntity)
}
