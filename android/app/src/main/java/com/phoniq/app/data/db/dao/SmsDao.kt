package com.phoniq.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.phoniq.app.data.db.entity.SmsMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsDao {
    @Query("SELECT * FROM sms_messages ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<SmsMessageEntity>>

    @Query("SELECT * FROM sms_messages WHERE category = :category ORDER BY timestamp DESC")
    fun observeByCategory(category: String): Flow<List<SmsMessageEntity>>

    @Query("SELECT * FROM sms_messages WHERE is_otp = 1 ORDER BY timestamp DESC")
    fun observeOtps(): Flow<List<SmsMessageEntity>>

    @Query("SELECT * FROM sms_messages WHERE is_transaction = 1 ORDER BY timestamp DESC")
    fun observeTransactions(): Flow<List<SmsMessageEntity>>

    @Query("SELECT * FROM sms_messages WHERE thread_id = :threadId ORDER BY timestamp ASC")
    fun observeThread(threadId: String): Flow<List<SmsMessageEntity>>

    @Query("""
        SELECT * FROM sms_messages
        WHERE body LIKE '%' || :query || '%' OR sender LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun search(query: String): Flow<List<SmsMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(sms: SmsMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(messages: List<SmsMessageEntity>)

    @Update
    suspend fun update(sms: SmsMessageEntity)

    @Query("UPDATE sms_messages SET is_read = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE sms_messages SET is_read = 1")
    suspend fun markAllRead()

    @Query("SELECT COUNT(*) FROM sms_messages WHERE is_read = 0")
    fun observeUnreadCount(): Flow<Int>
}
