package com.phoniq.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.phoniq.app.data.db.entity.OtpLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OtpLogDao {
    @Query("SELECT * FROM otp_log ORDER BY expiry_at DESC")
    fun observeAll(): Flow<List<OtpLogEntity>>

    @Query("SELECT * FROM otp_log WHERE expiry_at > :nowEpoch ORDER BY expiry_at DESC")
    fun observeActive(nowEpoch: Long): Flow<List<OtpLogEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(otp: OtpLogEntity): Long

    @Query("UPDATE otp_log SET was_copied = 1 WHERE id = :id")
    suspend fun markCopied(id: Long)
}
