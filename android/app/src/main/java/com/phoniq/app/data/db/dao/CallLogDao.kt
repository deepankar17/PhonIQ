package com.phoniq.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phoniq.app.data.db.entity.CallLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_log ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_log WHERE type = :type ORDER BY timestamp DESC")
    fun observeByType(type: String): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_log WHERE number = :number ORDER BY timestamp DESC")
    fun observeByNumber(number: String): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: CallLogEntity): Long

    @Query("DELETE FROM call_log")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(entry: CallLogEntity)
}
