package com.phoniq.app.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.provider.Telephony
import android.telephony.SmsManager
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

    fun thread(threadId: String): Flow<List<SmsMessageEntity>> =
        smsDao.observeThread(normalizeProviderThreadId(threadId))

    suspend fun getThreadLatestPageChronological(threadId: String, limit: Int): List<SmsMessageEntity> =
        withContext(Dispatchers.IO) {
            smsDao.getThreadLatestPageDesc(normalizeProviderThreadId(threadId), limit).reversed()
        }

    suspend fun getThreadOlderPageChronological(
        threadId: String,
        beforeTimestamp: Long,
        beforeId: Long,
        limit: Int,
    ): List<SmsMessageEntity> =
        withContext(Dispatchers.IO) {
            smsDao.getThreadOlderPageDesc(normalizeProviderThreadId(threadId), beforeTimestamp, beforeId, limit).reversed()
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

    private fun normalizeProviderThreadId(raw: String): String {
        val t = raw.trim()
        return when {
            t.startsWith("sms_") -> t.removePrefix("sms_").trim()
            t.startsWith("mms_") -> t.removePrefix("mms_").trim()
            else -> t
        }
    }

    private fun threadIdsForProvider(ids: Collection<String>): List<String> =
        ids.map { normalizeProviderThreadId(it) }.filter { it.isNotEmpty() }.distinct()

    private fun deleteThreadsFromTelephony(providerThreadIds: List<String>) {
        val resolver = context.applicationContext.contentResolver
        for (tid in providerThreadIds) {
            try {
                resolver.delete(
                    Telephony.Sms.CONTENT_URI,
                    "${Telephony.Sms.THREAD_ID} = ?",
                    arrayOf(tid),
                )
                resolver.delete(
                    Telephony.Mms.CONTENT_URI,
                    "${Telephony.Mms.THREAD_ID} = ?",
                    arrayOf(tid),
                )
            } catch (_: SecurityException) {
            } catch (_: IllegalArgumentException) {
            }
        }
    }

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

        appendInboxMms(resolver, smsBatch, pinnedThreads, archivedThreads)

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
            smsDao.markThreadRead(normalizeProviderThreadId(threadId))
        }

    suspend fun markThreadsRead(threadIds: Collection<String>) =
        withContext(Dispatchers.IO) {
            val ids = threadIdsForProvider(threadIds)
            if (ids.isNotEmpty()) smsDao.markThreadsRead(ids)
        }

    suspend fun setThreadPinned(threadId: String, pinned: Boolean) =
        withContext(Dispatchers.IO) {
            smsDao.setThreadPinned(normalizeProviderThreadId(threadId), pinned)
        }

    suspend fun setThreadArchived(threadId: String, archived: Boolean) =
        withContext(Dispatchers.IO) {
            smsDao.setThreadArchived(normalizeProviderThreadId(threadId), archived)
        }

    suspend fun setThreadsArchived(threadIds: Collection<String>, archived: Boolean) =
        withContext(Dispatchers.IO) {
            val ids = threadIdsForProvider(threadIds)
            if (ids.isNotEmpty()) smsDao.setThreadsArchived(ids, archived)
        }

    /** Removes matching rows from Room only (legacy); prefer [deleteThreadsPermanently]. */
    suspend fun deleteLocalThreads(threadIds: Collection<String>) =
        withContext(Dispatchers.IO) {
            val ids = threadIdsForProvider(threadIds)
            if (ids.isNotEmpty()) smsDao.deleteThreads(ids)
        }

    /**
     * Deletes all SMS/MMS for the given conversation thread id(s) from the Telephony provider (when permitted),
     * then rebuilds the local inbox from Telephony.
     */
    suspend fun deleteThreadsPermanently(uiThreadIds: Collection<String>) =
        withContext(Dispatchers.IO) {
            fullSyncMutex.withLock {
                val keys = threadIdsForProvider(uiThreadIds)
                if (keys.isEmpty()) return@withContext
                deleteThreadsFromTelephony(keys)
                syncFullInboxUnlocked()
            }
        }

    /**
     * Result of an outgoing SMS attempt.
     *
     * @param success true when [SmsManager.sendTextMessage] returned without throwing AND
     *     the message was written to the system Sent box.
     * @param insertedRowId Telephony provider row id for the inserted Sent record, or -1 when
     *     the app is not the default SMS app and the system blocked the write.
     * @param errorMessage human-readable error string for the UI when [success] is false.
     */
    data class SendResult(
        val success: Boolean,
        val insertedRowId: Long,
        val errorMessage: String? = null,
    )

    /**
     * Send a single text SMS via [SmsManager] and persist it in the Telephony Sent provider
     * so device-wide SMS listings (and our [syncDeviceSms]) reflect the new message.
     *
     * Note: writing into [Telephony.Sms.Sent] is restricted to the default SMS app on Android.
     * If we are not yet default, the SmsManager send still succeeds (we have SEND_SMS),
     * but the message will not appear in the Telephony provider until the OS catches up
     * through its own sent-broadcast handling. Caller should treat [insertedRowId] == -1L
     * as "sent over the air but not yet visible in inbox".
     */
    suspend fun sendSms(destination: String, body: String, threadIdHint: String? = null): SendResult =
        withContext(Dispatchers.IO) {
            val cleanedDest = destination.trim()
            val cleanedBody = body.trim()
            if (cleanedDest.isEmpty() || cleanedBody.isEmpty()) {
                return@withContext SendResult(false, -1L, "Recipient or message empty")
            }
            try {
                val sms = context.applicationContext.getSystemService(SmsManager::class.java)
                val parts = sms.divideMessage(cleanedBody)
                if (parts.size <= 1) {
                    sms.sendTextMessage(cleanedDest, null, cleanedBody, null, null)
                } else {
                    sms.sendMultipartTextMessage(cleanedDest, null, parts, null, null)
                }
            } catch (t: Throwable) {
                return@withContext SendResult(false, -1L, t.localizedMessage ?: "Send failed")
            }

            val rowId = persistOutgoingSent(cleanedDest, cleanedBody, threadIdHint)
            SendResult(success = true, insertedRowId = rowId, errorMessage = null)
        }

    /**
     * Insert a Sent record into the Telephony provider. Returns the new row id or -1L when
     * we are not the default SMS app (provider silently rejects the insert).
     */
    private fun persistOutgoingSent(destination: String, body: String, threadIdHint: String?): Long {
        val now = System.currentTimeMillis()
        val values =
            ContentValues().apply {
                put(Telephony.Sms.ADDRESS, destination)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, now)
                put(Telephony.Sms.DATE_SENT, now)
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.SEEN, 1)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                threadIdHint?.toLongOrNull()?.let { put(Telephony.Sms.THREAD_ID, it) }
            }
        return try {
            val uri: Uri? = context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
            uri?.lastPathSegment?.toLongOrNull() ?: -1L
        } catch (_: SecurityException) {
            -1L
        } catch (_: IllegalArgumentException) {
            -1L
        }
    }

    /**
     * Ingests Telephony MMS inbox rows into the same Room table as SMS (subject line + placeholder body).
     * Attachment bytes are not loaded; grouping uses thread id when present, else `mms_{id}` or sender.
     */
    private fun appendInboxMms(
        resolver: ContentResolver,
        batch: MutableList<SmsMessageEntity>,
        pinnedThreads: Set<String>,
        archivedThreads: Set<String>,
    ) {
        val projection =
            arrayOf(
                Telephony.Mms._ID,
                Telephony.Mms.DATE,
                Telephony.Mms.READ,
                Telephony.Mms.THREAD_ID,
                Telephony.Mms.SUBJECT,
            )
        val cursor =
            try {
                resolver.query(
                    Telephony.Mms.Inbox.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${Telephony.Mms.DATE} DESC",
                )
            } catch (_: SecurityException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            } ?: return

        cursor.use { c ->
            val idCol = c.getColumnIndex(Telephony.Mms._ID)
            val dateCol = c.getColumnIndex(Telephony.Mms.DATE)
            val readCol = c.getColumnIndex(Telephony.Mms.READ)
            val threadCol = c.getColumnIndex(Telephony.Mms.THREAD_ID)
            val subCol = c.getColumnIndex(Telephony.Mms.SUBJECT)
            while (c.moveToNext()) {
                val mmsId = c.getLong(idCol)
                val timestamp = c.getLong(dateCol)
                val isRead = readCol >= 0 && c.getInt(readCol) == 1
                val threadFromProvider =
                    if (threadCol >= 0) {
                        c.getString(threadCol)?.trim()?.takeIf { it.isNotEmpty() }
                    } else {
                        null
                    }
                val subject = if (subCol >= 0) c.getString(subCol)?.trim().orEmpty() else ""
                val from = resolveMmsFromAddress(resolver, mmsId) ?: "MMS"
                val threadId = threadFromProvider ?: from.ifBlank { "mms_$mmsId" }
                val body = if (subject.isNotEmpty()) "[MMS] $subject" else "[MMS]"
                val result = parser.parse(from, body)
                batch.add(
                    SmsMessageEntity(
                        sender = from,
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
                    ),
                )
            }
        }
    }

    /** Best-effort FROM address for an MMS PDU (`addr` row with PDU FROM type 137). */
    private fun resolveMmsFromAddress(resolver: ContentResolver, mmsId: Long): String? {
        val addrUri =
            ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, mmsId)
                .buildUpon()
                .appendPath("addr")
                .build()
        val cur =
            try {
                resolver.query(
                    addrUri,
                    arrayOf(Telephony.Mms.Addr.ADDRESS, Telephony.Mms.Addr.TYPE),
                    null,
                    null,
                    null,
                )
            } catch (_: Exception) {
                null
            } ?: return null
        cur.use { ac ->
            val addrCol = ac.getColumnIndex(Telephony.Mms.Addr.ADDRESS)
            val typeCol = ac.getColumnIndex(Telephony.Mms.Addr.TYPE)
            while (ac.moveToNext()) {
                val type = if (typeCol >= 0) ac.getInt(typeCol) else -1
                if (type == 137) {
                    return ac.getString(addrCol)?.trim()?.takeIf { it.isNotEmpty() }
                }
            }
        }
        return null
    }
}
