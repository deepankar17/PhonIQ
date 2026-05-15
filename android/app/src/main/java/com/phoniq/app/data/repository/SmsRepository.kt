package com.phoniq.app.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.provider.Telephony
import com.phoniq.app.data.db.dao.OtpLogDao
import com.phoniq.app.data.db.dao.SmsDao
import com.phoniq.app.data.db.dao.TransactionDao
import com.phoniq.app.data.db.entity.OtpLogEntity
import com.phoniq.app.data.db.entity.SmsMessageEntity
import com.phoniq.app.data.db.entity.TransactionEntity
import com.phoniq.app.domain.sms.SmsParser
import com.phoniq.app.domain.sms.SmsParser.SmsCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Indexes device SMS into Room. **`syncFullInbox`** is still a wholesale rewrite (SQLite `replaceInbox`)
 * plus derived otp/txn tables so results stay deterministic; bursts from [SmsReceiver] are serialized
 * and **throttled** so rapid telephony intents do not enqueue overlapping expensive syncs back-to-back.
 */
class SmsRepository(
    private val context: Context,
    private val smsDao: SmsDao,
    private val otpLogDao: OtpLogDao,
    private val transactionDao: TransactionDao,
    private val parser: SmsParser,
) {
    val allMessages: Flow<List<SmsMessageEntity>> = smsDao.observeAll()
    val otpMessages: Flow<List<SmsMessageEntity>> = smsDao.observeOtps()
    val transactionMessages: Flow<List<SmsMessageEntity>> = smsDao.observeTransactions()
    val unreadCount: Flow<Int> = smsDao.observeUnreadCount()

    fun thread(threadId: String): Flow<List<SmsMessageEntity>> = smsDao.observeThread(threadId)

    suspend fun getThreadLatestPageChronological(threadId: String, limit: Int): List<SmsMessageEntity> =
        withContext(Dispatchers.IO) {
            smsDao.getThreadLatestPageDesc(threadId, limit).reversed()
        }

    suspend fun getThreadOlderPageChronological(
        threadId: String,
        beforeTimestamp: Long,
        beforeId: Long,
        limit: Int,
    ): List<SmsMessageEntity> =
        withContext(Dispatchers.IO) {
            smsDao.getThreadOlderPageDesc(threadId, beforeTimestamp, beforeId, limit).reversed()
        }
    fun search(query: String): Flow<List<SmsMessageEntity>> = smsDao.search(query)
    fun byCategory(category: String): Flow<List<SmsMessageEntity>> = smsDao.observeByCategory(category)

    companion object {
        private const val SYNC_PREFS = "phoniq_sms_sync"
        private const val KEY_LAST_RECEIVER_ELAPSED = "sms_sync_last_receiver_elapsed_rt"
        private const val RECEIVER_MIN_INTERVAL_MS = 5_000L
    }

    private val prefs by lazy {
        context.applicationContext.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
    }

    private val fullSyncMutex = Mutex()

    /**
     * Sync device SMS inbox into Room. Used from UI pulls and app-init refresh — always runs when awaited.
     * @param throttleReceiverBurst skip true for [SmsReceiver] path to coalesce rapid deliveries.
     */
    suspend fun syncDeviceSms(throttleReceiverBurst: Boolean = false) = withContext(Dispatchers.IO) {
        fullSyncMutex.withLock {
            if (throttleReceiverBurst) {
                val now = SystemClock.elapsedRealtime()
                val last = prefs.getLong(KEY_LAST_RECEIVER_ELAPSED, 0L)
                if (last != 0L && now - last < RECEIVER_MIN_INTERVAL_MS) {
                    return@withLock
                }
                prefs.edit().putLong(KEY_LAST_RECEIVER_ELAPSED, now).apply()
            }

            syncFullInboxUnlocked()
        }
    }

    /** Full replace from Telephony provider; preserves pinned/archived threads by re-applying IDs from Dao. */
    private suspend fun syncFullInboxUnlocked() {
        val resolver: ContentResolver = context.applicationContext.contentResolver
        val uri: Uri = Telephony.Sms.CONTENT_URI
        val projection =
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.READ,
            )
        val cursor = resolver.query(uri, projection, null, null, "${Telephony.Sms.DATE} DESC") ?: return

        val pinnedThreads = smsDao.getPinnedThreadIds().toSet()
        val archivedThreads = smsDao.getArchivedThreadIds().toSet()

        val smsBatch = mutableListOf<SmsMessageEntity>()
        val otpBatch = mutableListOf<Pair<SmsMessageEntity, SmsParser.OtpResult>>()
        val txnBatch = mutableListOf<Pair<SmsMessageEntity, SmsParser.TransactionResult>>()

        cursor.use { c ->
            val idCol = c.getColumnIndex(Telephony.Sms._ID)
            val addrCol = c.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyCol = c.getColumnIndex(Telephony.Sms.BODY)
            val dateCol = c.getColumnIndex(Telephony.Sms.DATE)
            val threadCol = c.getColumnIndex(Telephony.Sms.THREAD_ID)
            val readCol = c.getColumnIndex(Telephony.Sms.READ)

            while (c.moveToNext()) {
                val sender = c.getString(addrCol) ?: continue
                val body = c.getString(bodyCol) ?: continue
                val timestamp = c.getLong(dateCol)
                val threadId = c.getString(threadCol) ?: sender
                val isRead = c.getInt(readCol) == 1

                val result = parser.parse(sender, body)
                val entity =
                    SmsMessageEntity(
                        sender = sender,
                        body = body,
                        timestamp = timestamp,
                        category = result.category.name,
                        threadId = threadId,
                        isTransaction = result.category == SmsCategory.TRANSACTION,
                        isOtp = result.category == SmsCategory.OTP,
                        isSpam = result.category == SmsCategory.SPAM,
                        isRead = isRead,
                        isPinned = threadId in pinnedThreads,
                        isArchived = threadId in archivedThreads,
                    )
                smsBatch.add(entity)
                if (result.otp != null) otpBatch.add(entity to result.otp)
                if (result.transaction != null) txnBatch.add(entity to result.transaction)
            }
        }

        transactionDao.deleteAll()
        otpLogDao.deleteAll()

        val indexByRef = smsBatch.withIndex().associate { it.value to it.index }
        val insertedIds = smsDao.replaceInbox(smsBatch)

        for ((smsEntity, otp) in otpBatch) {
            val idx = indexByRef.getValue(smsEntity)
            val smsId = insertedIds[idx]
            otpLogDao.insert(
                OtpLogEntity(
                    smsId = smsId,
                    otpCode = otp.code,
                    sender = smsEntity.sender,
                    expiryAt = smsEntity.timestamp + (otp.ttlSeconds * 1000L),
                ),
            )
        }

        val txnEntities =
            txnBatch.map { (smsEntity, txn) ->
                val idx = indexByRef.getValue(smsEntity)
                TransactionEntity(
                    smsId = insertedIds[idx],
                    amount = txn.amount,
                    txnType = txn.type,
                    merchant = txn.merchant,
                    category = txn.category,
                    date = smsEntity.timestamp,
                )
            }
        if (txnEntities.isNotEmpty()) transactionDao.insertAll(txnEntities)
    }

    suspend fun markAllRead() = smsDao.markAllRead()

    suspend fun markThreadRead(threadId: String) =
        withContext(Dispatchers.IO) {
            smsDao.markThreadRead(threadId)
        }

    suspend fun setThreadPinned(threadId: String, pinned: Boolean) =
        withContext(Dispatchers.IO) {
            smsDao.setThreadPinned(threadId, pinned)
        }

    suspend fun setThreadArchived(threadId: String, archived: Boolean) =
        withContext(Dispatchers.IO) {
            smsDao.setThreadArchived(threadId, archived)
        }
}
