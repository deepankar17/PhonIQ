package com.phoniq.app.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
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
import kotlinx.coroutines.withContext

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
    fun search(query: String): Flow<List<SmsMessageEntity>> = smsDao.search(query)
    fun byCategory(category: String): Flow<List<SmsMessageEntity>> = smsDao.observeByCategory(category)

    /** Sync device SMS inbox into Room. Call once on first launch (or on demand). */
    suspend fun syncDeviceSms() = withContext(Dispatchers.IO) {
        val resolver: ContentResolver = context.contentResolver
        val uri: Uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.READ,
        )
        val cursor = resolver.query(uri, projection, null, null, "${Telephony.Sms.DATE} DESC") ?: return@withContext

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
                val entity = SmsMessageEntity(
                    sender = sender,
                    body = body,
                    timestamp = timestamp,
                    category = result.category.name,
                    threadId = threadId,
                    isTransaction = result.category == SmsCategory.TRANSACTION,
                    isOtp = result.category == SmsCategory.OTP,
                    isSpam = result.category == SmsCategory.SPAM,
                    isRead = isRead,
                )
                smsBatch.add(entity)
                if (result.otp != null) otpBatch.add(entity to result.otp)
                if (result.transaction != null) txnBatch.add(entity to result.transaction)
            }
        }

        smsDao.insertAll(smsBatch)

        // Insert OTP log entries (need to re-query ids since insertAll ignores conflicts)
        for ((smsEntity, otp) in otpBatch) {
            val inserted = smsDao.search(smsEntity.body).let { null } // ids not available easily here
            // Simplified: insert OTP log with smsId = 0 as placeholder until we have a two-pass approach
            otpLogDao.insert(
                OtpLogEntity(
                    smsId = 0,
                    otpCode = otp.code,
                    sender = smsEntity.sender,
                    expiryAt = smsEntity.timestamp + (otp.ttlSeconds * 1000L),
                )
            )
        }

        // Insert transactions
        val txnEntities = txnBatch.map { (_, txn) ->
            TransactionEntity(
                amount = txn.amount,
                txnType = txn.type,
                merchant = txn.merchant,
                category = txn.category,
                date = System.currentTimeMillis(),
            )
        }
        if (txnEntities.isNotEmpty()) transactionDao.insertAll(txnEntities)
    }

    suspend fun markAllRead() = smsDao.markAllRead()
}
