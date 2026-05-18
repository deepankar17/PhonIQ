package com.phoniq.app.data.repository

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.provider.CallLog
import com.phoniq.app.data.db.dao.CallLogDao
import com.phoniq.app.data.db.dao.SpamNumberDao
import com.phoniq.app.data.db.entity.CallLogEntity
import com.phoniq.app.data.db.entity.SpamNumberEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.phoniq.app.util.normalizePhoneKey
import com.phoniq.app.util.smsSpamPeerKey
import java.util.Locale

private const val TRUST_PREFS = "phoniq_user_trust"
private const val TRUST_KEYS = "trusted_normalized_keys"

class CallLogRepository(
    private val context: Context,
    private val callLogDao: CallLogDao,
    private val spamNumberDao: SpamNumberDao,
) {
    val allCalls: Flow<List<CallLogEntity>> = callLogDao.observeAll()

    private val trustPrefs = context.getSharedPreferences(TRUST_PREFS, Context.MODE_PRIVATE)

    private val _userTrustedKeys = MutableStateFlow(loadTrustedKeys())
    /** Normalized keys the user marked “safe” (suppresses Likely Spam badge per PROJECT.md). */
    val userTrustedKeys: StateFlow<Set<String>> = _userTrustedKeys.asStateFlow()

    private fun loadTrustedKeys(): Set<String> =
        trustPrefs.getString(TRUST_KEYS, null)
            ?.split(',')
            ?.mapNotNull { it.trim().takeIf(String::isNotEmpty) }
            ?.toSet()
            ?: emptySet()

    private fun persistTrustedKeys(keys: Set<String>) {
        trustPrefs.edit().putString(TRUST_KEYS, keys.joinToString(",")).apply()
    }

    suspend fun markUserTrustedNumber(rawNumber: String) = withContext(Dispatchers.IO) {
        val key = normalizePhoneKey(rawNumber)
        if (key.isEmpty()) return@withContext
        val next = _userTrustedKeys.value.toMutableSet().apply { add(key) }
        persistTrustedKeys(next)
        _userTrustedKeys.value = next
    }

    suspend fun clearUserTrustedNumber(rawNumber: String) = withContext(Dispatchers.IO) {
        val key = normalizePhoneKey(rawNumber)
        if (key.isEmpty()) return@withContext
        val next = _userTrustedKeys.value.filterNot { it == key }.toSet()
        persistTrustedKeys(next)
        _userTrustedKeys.value = next
    }

    /** Keys for numbers/sender IDs marked spam (SMS peer + call UI). */
    val spamNumberKeys: Flow<Set<String>> =
        spamNumberDao.observeAll().map { rows ->
            rows.map { smsSpamPeerKey(it.number) }.filter { it.isNotEmpty() }.toSet()
        }

    fun callsByType(type: String): Flow<List<CallLogEntity>> = callLogDao.observeByType(type)

    fun callsForNumber(number: String): Flow<List<CallLogEntity>> = callLogDao.observeByNumber(number)

    /**
     * Syncs Android’s unified [CallLog.Calls] into Room.
     *
     * **WhatsApp / third-party VoIP:** Rows appear here only when the app and OEM integrate with
     * Telecom and write to the system call log (often visible in the stock Phone app’s recents).
     * Many builds keep WhatsApp history in-app only; there is **no supported public API** to read
     * that off-device log. This method does not filter by account or call type—if the provider
     * exposes a row and [READ_CALL_LOG] is granted, we ingest it.
     */
    suspend fun syncDeviceCallLog() = withContext(Dispatchers.IO) {
        val resolver: ContentResolver = context.contentResolver
        val projection =
            buildList {
                add(CallLog.Calls._ID)
                add(CallLog.Calls.NUMBER)
                add(CallLog.Calls.CACHED_NAME)
                add(CallLog.Calls.CACHED_NUMBER_LABEL)
                add(CallLog.Calls.DURATION)
                add(CallLog.Calls.TYPE)
                add(CallLog.Calls.DATE)
                add(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME)
                add(CallLog.Calls.PHONE_ACCOUNT_ID)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(CallLog.Calls.FEATURES)
                }
            }.toTypedArray()

        val cursor = resolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC",
        ) ?: return@withContext

        val batch = mutableListOf<CallLogEntity>()
        cursor.use { c ->
            val idCol = c.getColumnIndex(CallLog.Calls._ID)
            val numCol = c.getColumnIndex(CallLog.Calls.NUMBER)
            val cachedNameCol = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val cachedLabelCol = c.getColumnIndex(CallLog.Calls.CACHED_NUMBER_LABEL)
            val durCol = c.getColumnIndex(CallLog.Calls.DURATION)
            val typeCol = c.getColumnIndex(CallLog.Calls.TYPE)
            val dateCol = c.getColumnIndex(CallLog.Calls.DATE)
            val accountCol = c.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME)
            val accountIdCol = c.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)
            val featuresCol =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    c.getColumnIndex(CallLog.Calls.FEATURES)
                } else {
                    -1
                }

            fun safeString(col: Int): String =
                if (col >= 0) c.getString(col)?.trim().orEmpty() else ""

            while (c.moveToNext()) {
                val rowId = c.getLong(idCol)
                val rawNumber = safeString(numCol)
                val cachedName = safeString(cachedNameCol)
                val cachedLabel = safeString(cachedLabelCol)
                val numberResolved =
                    sequenceOf(rawNumber, cachedLabel, cachedName)
                        .firstOrNull { it.isNotEmpty() }
                        ?: "raw_call_$rowId"

                val accountComponent = if (accountCol >= 0) c.getString(accountCol) else null
                val accountId = if (accountIdCol >= 0) c.getString(accountIdCol) else null
                val features =
                    if (featuresCol >= 0) {
                        c.getInt(featuresCol)
                    } else {
                        0
                    }
                val channelStored = resolveStoredCallChannel(accountComponent, accountId, features, numberResolved)

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
                        number = numberResolved,
                        durationSec = c.getInt(durCol),
                        type = type,
                        timestamp = c.getLong(dateCol),
                        callChannel = channelStored,
                    ),
                )
            }
        }

        callLogDao.replaceAll(batch)
    }

    suspend fun isSpam(number: String): Boolean =
        spamNumberDao.findByNumber(smsSpamPeerKey(number)) != null

    suspend fun markSpam(number: String) {
        val key = smsSpamPeerKey(number)
        if (key.isEmpty()) return
        spamNumberDao.insert(SpamNumberEntity(number = key, source = "USER"))
    }

    suspend fun unmarkSpam(number: String) {
        spamNumberDao.deleteByNumber(smsSpamPeerKey(number))
    }

    suspend fun clearAll() = callLogDao.deleteAll()

    /**
     * Delete all rows in the local Room cache + the system CallLog provider that share the
     * normalized phone key of [rawNumber]. Mirrors Google Dialer's "Delete from call history"
     * which removes every entry for the contact.
     *
     * @return number of system CallLog rows deleted (Room rows are deleted as a side effect of
     *     the next observation).
     */
    suspend fun deleteCallsForNumber(rawNumber: String): Int = withContext(Dispatchers.IO) {
        val key = normalizePhoneKey(rawNumber)
        if (key.isEmpty()) return@withContext 0

        val snapshot = callLogDao.recentSnapshot()
        snapshot
            .filter { normalizePhoneKey(it.number) == key }
            .forEach { runCatching { callLogDao.delete(it) } }

        val resolver: ContentResolver = context.contentResolver
        val deletedSystem =
            try {
                resolver.delete(
                    CallLog.Calls.CONTENT_URI,
                    "${CallLog.Calls.NUMBER} = ?",
                    arrayOf(rawNumber),
                )
            } catch (_: SecurityException) {
                0
            } catch (_: IllegalArgumentException) {
                0
            }
        deletedSystem
    }

    /** Batch delete: each distinct number uses the same path as [deleteCallsForNumber]. */
    suspend fun deleteCallsForNumbers(rawNumbers: Collection<String>) = withContext(Dispatchers.IO) {
        rawNumbers.map { it.trim() }.filter { it.isNotEmpty() }.distinct().forEach { n ->
            deleteCallsForNumber(n)
        }
    }

    /** Attach a user note to the most recent call row matching [number] after phone normalization. */
    suspend fun saveNoteForLatestCall(number: String, note: String) = withContext(Dispatchers.IO) {
        val key = normalizePhoneKey(number)
        if (key.isEmpty()) return@withContext
        val row =
            callLogDao.recentSnapshot().firstOrNull { normalizePhoneKey(it.number) == key }
                ?: return@withContext
        callLogDao.update(row.copy(notes = note))
    }
}

private fun resolveStoredCallChannel(
    accountComponent: String?,
    phoneAccountId: String?,
    features: Int,
    numberResolved: String,
): String {
    val ac =
        "${accountComponent.orEmpty()} ${phoneAccountId.orEmpty()}"
            .lowercase(Locale.US)
    val n = numberResolved.lowercase(Locale.US)
    // WhatsApp / WhatsApp Business Telecom + JID-shaped numbers (OEM strings vary).
    val whatsapp =
        ac.contains("whatsapp") ||
            ac.contains("com.whatsapp") ||
            ac.contains("whatsappconnectionservice") ||
            ac.contains("com.whatsapp.w4b") ||
            n.contains("@s.whatsapp.net") ||
            n.contains("@g.us") ||
            n.contains("whatsapp.net")
    if (!whatsapp) return "PSTN"
    val video =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            (features and CallLog.Calls.FEATURES_VIDEO) != 0
    return if (video) "WHATSAPP_VIDEO" else "WHATSAPP_VOICE"
}
