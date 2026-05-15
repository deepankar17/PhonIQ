package com.phoniq.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.phoniq.app.data.db.entity.SmsMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SmsDao {
    @Query("SELECT * FROM sms_messages ORDER BY timestamp DESC")
    abstract fun observeAll(): Flow<List<SmsMessageEntity>>

    @Query("SELECT * FROM sms_messages WHERE category = :category ORDER BY timestamp DESC")
    abstract fun observeByCategory(category: String): Flow<List<SmsMessageEntity>>

    @Query("SELECT * FROM sms_messages WHERE is_otp = 1 ORDER BY timestamp DESC")
    abstract fun observeOtps(): Flow<List<SmsMessageEntity>>

    @Query("SELECT * FROM sms_messages WHERE is_transaction = 1 ORDER BY timestamp DESC")
    abstract fun observeTransactions(): Flow<List<SmsMessageEntity>>

    @Query("SELECT * FROM sms_messages WHERE thread_id = :threadId ORDER BY timestamp ASC")
    abstract fun observeThread(threadId: String): Flow<List<SmsMessageEntity>>

    @Query(
        """
        SELECT * FROM sms_messages
        WHERE thread_id = :threadId
        ORDER BY timestamp DESC, id DESC
        LIMIT :limit
        """,
    )
    abstract suspend fun getThreadLatestPageDesc(threadId: String, limit: Int): List<SmsMessageEntity>

    @Query(
        """
        SELECT * FROM sms_messages
        WHERE thread_id = :threadId
          AND (timestamp < :beforeTimestamp OR (timestamp = :beforeTimestamp AND id < :beforeId))
        ORDER BY timestamp DESC, id DESC
        LIMIT :limit
        """,
    )
    abstract suspend fun getThreadOlderPageDesc(
        threadId: String,
        beforeTimestamp: Long,
        beforeId: Long,
        limit: Int,
    ): List<SmsMessageEntity>

    @Query(
        """
        SELECT * FROM sms_messages
        WHERE body LIKE '%' || :query || '%' OR sender LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """,
    )
    abstract fun search(query: String): Flow<List<SmsMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(sms: SmsMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(messages: List<SmsMessageEntity>): List<Long>

    @Query("DELETE FROM sms_messages")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun replaceInbox(messages: List<SmsMessageEntity>): List<Long> {
        deleteAll()
        if (messages.isEmpty()) return emptyList()
        return insertAll(messages)
    }

    @Update
    abstract suspend fun update(sms: SmsMessageEntity)

    @Query("UPDATE sms_messages SET is_read = 1 WHERE id = :id")
    abstract suspend fun markRead(id: Long)

    @Query("UPDATE sms_messages SET is_read = 1 WHERE thread_id = :threadId")
    abstract suspend fun markThreadRead(threadId: String)

    @Query("UPDATE sms_messages SET is_read = 1")
    abstract suspend fun markAllRead()

    @Query("SELECT COUNT(*) FROM sms_messages WHERE is_read = 0")
    abstract fun observeUnreadCount(): Flow<Int>

    @Query("SELECT DISTINCT thread_id FROM sms_messages WHERE is_pinned = 1")
    abstract suspend fun getPinnedThreadIds(): List<String>

    @Query("SELECT DISTINCT thread_id FROM sms_messages WHERE is_archived = 1")
    abstract suspend fun getArchivedThreadIds(): List<String>

    @Query("UPDATE sms_messages SET is_pinned = :pinned WHERE thread_id = :threadId")
    abstract suspend fun setThreadPinned(threadId: String, pinned: Boolean)

    @Query("UPDATE sms_messages SET is_archived = :archived WHERE thread_id = :threadId")
    abstract suspend fun setThreadArchived(threadId: String, archived: Boolean)
}
