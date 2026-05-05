package com.phoniq.app.data.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.CallLog
import com.phoniq.app.data.db.dao.CallLogDao
import com.phoniq.app.data.db.dao.SpamNumberDao
import com.phoniq.app.data.db.entity.CallLogEntity
import com.phoniq.app.data.db.entity.SpamNumberEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CallLogRepository(
    private val context: Context,
    private val callLogDao: CallLogDao,
    private val spamNumberDao: SpamNumberDao,
) {
    val allCalls: Flow<List<CallLogEntity>> = callLogDao.observeAll()

    fun callsByType(type: String): Flow<List<CallLogEntity>> = callLogDao.observeByType(type)

    /** Sync device call log into Room. */
    suspend fun syncDeviceCallLog() = withContext(Dispatchers.IO) {
        val resolver: ContentResolver = context.contentResolver
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
        )
        val cursor = resolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC",
        ) ?: return@withContext

        val batch = mutableListOf<CallLogEntity>()
        cursor.use { c ->
            val numCol = c.getColumnIndex(CallLog.Calls.NUMBER)
            val durCol = c.getColumnIndex(CallLog.Calls.DURATION)
            val typeCol = c.getColumnIndex(CallLog.Calls.TYPE)
            val dateCol = c.getColumnIndex(CallLog.Calls.DATE)

            while (c.moveToNext()) {
                val type = when (c.getInt(typeCol)) {
                    CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                    CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                    CallLog.Calls.MISSED_TYPE -> "MISSED"
                    CallLog.Calls.REJECTED_TYPE -> "REJECTED"
                    CallLog.Calls.BLOCKED_TYPE -> "BLOCKED"
                    else -> "OTHER"
                }
                batch.add(
                    CallLogEntity(
                        number = c.getString(numCol) ?: continue,
                        durationSec = c.getInt(durCol),
                        type = type,
                        timestamp = c.getLong(dateCol),
                    )
                )
            }
        }

        for (entry in batch) callLogDao.insert(entry)
    }

    suspend fun isSpam(number: String): Boolean =
        spamNumberDao.findByNumber(number) != null

    suspend fun markSpam(number: String) {
        spamNumberDao.insert(SpamNumberEntity(number = number, source = "USER"))
    }

    suspend fun unmarkSpam(number: String) {
        spamNumberDao.deleteByNumber(number)
    }

    suspend fun clearAll() = callLogDao.deleteAll()
}
