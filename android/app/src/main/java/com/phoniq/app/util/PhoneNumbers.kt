package com.phoniq.app.util

import com.phoniq.app.data.db.entity.CallLogEntity
import com.phoniq.app.data.db.entity.ContactEntity
import com.phoniq.app.data.model.MessageThread

/**
 * Canonical comparison key for PSTN numbers on the device (call log vs contact vs search).
 * Uses digit-only form and, when long enough, the last 10 digits so +91… and local forms align.
 */
fun normalizePhoneKey(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val digits = buildString(raw.length) {
        for (c in raw) {
            if (c.isDigit()) append(c)
        }
    }
    return when {
        digits.length >= 10 -> digits.takeLast(10)
        else -> digits
    }
}

/**
 * Heuristic: SMS short codes / alphanumeric sender IDs cannot be replied to like normal numbers.
 *
 * Rules (aligned with [normalizePhoneKey]):
 * - **Not** a short code if normalized digits look like a full local subscriber (10+ digits).
 * - **Numeric** short codes: only digits after stripping decorations, length in `[shortCodeNumericMinLen, shortCodeNumericMaxLen]`.
 * - **Alphanumeric** senders (e.g. branded headers): letters + digits, length 3–11 after removing
 *   spaces/hyphens; all-letter labels need length ≥ [shortCodeMinAllLetterLen] to reduce "Mom"/"Dad" false positives.
 */
fun isShortCodeAddress(
    raw: String?,
    shortCodeNumericMinLen: Int = 5,
    shortCodeNumericMaxLen: Int = 6,
    shortCodeMinAllLetterLen: Int = 4,
    shortCodeMaxAlnumLen: Int = 11,
): Boolean {
    if (raw.isNullOrBlank()) return false
    val trimmed = raw.trim()
    if (normalizePhoneKey(trimmed).length >= 10) return false

    val compact =
        trimmed
            .removePrefix("+")
            .replace("-", "")
            .replace(" ", "")
    val alnum = buildString(compact.length) {
        for (c in compact) {
            if (c.isLetterOrDigit()) append(c)
        }
    }
    if (alnum.isEmpty()) return false

    val hasLetter = alnum.any { it.isLetter() }
    if (hasLetter) {
        if (alnum.length > shortCodeMaxAlnumLen) return false
        if (!alnum.all { it.isLetterOrDigit() }) return false
        val hasDigit = alnum.any { it.isDigit() }
        val lenOk =
            when {
                hasDigit -> alnum.length >= 3
                else -> alnum.length >= shortCodeMinAllLetterLen
            }
        return lenOk
    }

    if (alnum.length in shortCodeNumericMinLen..shortCodeNumericMaxLen &&
        alnum.all { it.isDigit() }
    ) {
        return true
    }
    return false
}

/** True when the thread peer looks like a short code / sender ID ([isShortCodeAddress]). */
fun MessageThread.isShortCodeThreadPeer(): Boolean {
    val addr =
        peerAddress?.trim()?.takeIf { it.isNotEmpty() }
            ?: id.removePrefix("sms_").trim().takeIf { it.isNotEmpty() }
            ?: return false
    return isShortCodeAddress(addr)
}

/**
 * Key for one row per subscriber in recent calls: aligns with [normalizePhoneKey] when
 * there are enough digits; otherwise uses trimmed raw digits so unrelated blanks do not merge.
 */
fun dedupeRecentCallKey(raw: String?): String {
    if (raw.isNullOrBlank()) return "__empty__"
    val k = normalizePhoneKey(raw)
    if (k.isNotEmpty()) return k
    return "raw:${raw.trim()}"
}

/**
 * Count of consecutive **MISSED** rows at the head of [callsNewestFirst] for [rawNumber]
 * (mockup: `Missed (2)` on the deduped recent row).
 */
fun missedStreakForNumber(callsNewestFirst: List<CallLogEntity>, rawNumber: String): Int {
    if (callsNewestFirst.isEmpty()) return 0
    val key = dedupeRecentCallKey(rawNumber)
    var streak = 0
    for (c in callsNewestFirst) {
        if (dedupeRecentCallKey(c.number) != key) continue
        if (c.type == "MISSED") streak++ else break
    }
    return streak
}

/**
 * Collapse [calls] to the latest call per subscriber **and** [CallLogEntity.callChannel].
 * List must be newest-first. Including channel lets PSTN and WhatsApp (same number) both appear
 * when the system call log contains both rows.
 */
fun dedupeCallsLatestFirst(calls: List<CallLogEntity>): List<CallLogEntity> {
    if (calls.size <= 1) return calls
    val seen = LinkedHashSet<String>()
    val out = ArrayList<CallLogEntity>(calls.size)
    for (call in calls) {
        val key = "${dedupeRecentCallKey(call.number)}|${call.callChannel}"
        if (seen.add(key)) {
            out.add(call)
        }
    }
    return out
}
fun List<ContactEntity>.primaryNameByNormalizedPhone(): Map<String, String> {
    val acc = mutableMapOf<String, String>()
    for (c in this) {
        val k = normalizePhoneKey(c.number)
        if (k.isEmpty()) continue
        val existing = acc[k]
        if (existing == null || c.name.length > existing.length) {
            acc[k] = c.name
        }
    }
    return acc
}

/** First device [ContactEntity.deviceContactId] per [normalizePhoneKey] (for Photos). */
fun List<ContactEntity>.primaryDeviceContactIdByNormalizedPhone(): Map<String, Long> {
    val acc = mutableMapOf<String, Long>()
    for (c in this) {
        val k = normalizePhoneKey(c.number)
        if (k.isEmpty() || c.deviceContactId <= 0L) continue
        acc.putIfAbsent(k, c.deviceContactId)
    }
    return acc
}
