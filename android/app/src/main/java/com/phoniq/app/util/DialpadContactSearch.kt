package com.phoniq.app.util

import android.graphics.Color
import com.phoniq.app.data.db.entity.ContactEntity
import com.phoniq.app.data.model.RecentCall

/** One row shown under the dialpad when the typed buffer matches a contact or recent call. */
data class DialpadMatchRow(
    val title: String,
    val subtitle: String,
    val telSanitized: String,
    val deviceContactId: Long,
    val avatarStartArgb: Long,
    val avatarEndArgb: Long,
)

private const val MAX_MATCH_ROWS = 30

/** Digits-only view of the dial buffer (ignore `+`, spaces, `*`, `#` for matching). */
fun dialDigitsOnly(raw: String): String =
    buildString(raw.length) {
        for (c in raw) {
            if (c.isDigit()) append(c)
        }
    }

/** Map contact/display name letters to ITU-T keypad digits (2–9); skip non-letters. */
fun nameToT9Digits(name: String): String =
    buildString(name.length) {
        for (ch in name) {
            when (ch.uppercaseChar()) {
                in 'A'..'C' -> append('2')
                in 'D'..'F' -> append('3')
                in 'G'..'I' -> append('4')
                in 'J'..'L' -> append('5')
                in 'M'..'O' -> append('6')
                in 'P'..'S' -> append('7') // P,Q,R,S on 7
                in 'T'..'V' -> append('8')
                in 'W'..'Z' -> append('9')
                else -> Unit
            }
        }
    }

private fun contactAvatarArgbPair(colorHex: String): Pair<Long, Long> {
    val argb =
        runCatching {
            Color.parseColor(colorHex).toLong() and 0xFFFFFFFFL
        }.getOrElse { 0xFF6C63FFL }
    val endArgb = 0xFF000000L or ((argb and 0x00FFFFFFL) xor 0x002A2A2AL)
    return argb to endArgb
}

/**
 * Inclusive character index range in [subtitle] to highlight when typed [queryDigits] match a
 * substring of [normalizePhoneKey](subtitle). Returns null when digits should not be highlighted
 * (no match, or T9-only name match). Used for the T9 match list number line.
 */
fun dialpadPhoneHighlightRangeInSubtitle(subtitle: String, queryDigits: String): IntRange? {
    if (queryDigits.isEmpty()) return null
    val phoneKey = normalizePhoneKey(subtitle)
    if (!phoneKey.contains(queryDigits)) return null

    val digitPositions =
        buildList {
            subtitle.forEachIndexed { i, c ->
                if (c.isDigit()) add(i)
            }
        }
    if (digitPositions.isEmpty()) return null

    val fullDigits =
        buildString(digitPositions.size) {
            for (i in digitPositions) append(subtitle[i])
        }
    val keyOffsetInFull =
        if (fullDigits.length >= phoneKey.length) {
            fullDigits.length - phoneKey.length
        } else {
            0
        }
    if (fullDigits.substring(keyOffsetInFull) != phoneKey) return null

    val startInKey = phoneKey.lastIndexOf(queryDigits).takeIf { it >= 0 } ?: return null
    val startInFull = keyOffsetInFull + startInKey
    val endInFull = startInFull + queryDigits.length - 1
    if (endInFull >= digitPositions.size) return null

    val startChar = digitPositions[startInFull]
    val endChar = digitPositions[endInFull]
    return startChar..endChar
}

private fun matchesDialpadQuery(queryDigits: String, phoneKey: String, nameT9: String): Boolean {
    if (queryDigits.isEmpty()) return false
    if (phoneKey.contains(queryDigits)) return true
    if (nameT9.startsWith(queryDigits)) return true
    return false
}

/**
 * Live dialpad matches: substring match on normalized phone digits **or** T9 prefix match on names.
 * Contacts first (repository order), then recent-call hits excluding numbers already matched by contacts.
 */
fun computeDialpadMatches(
    contacts: List<ContactEntity>,
    recentCalls: List<RecentCall>,
    dialBuffer: String,
): List<DialpadMatchRow> {
    val q = dialDigitsOnly(dialBuffer)
    if (q.isEmpty()) return emptyList()

    val out = ArrayList<DialpadMatchRow>(16)
    val matchedContactKeys = mutableSetOf<String>()

    val seenAggregate = mutableSetOf<String>()
    for (c in contacts) {
        if (out.size >= MAX_MATCH_ROWS) break
        val aggKey =
            when {
                c.deviceContactId > 0L -> "d:${c.deviceContactId}"
                else -> "i:${c.id}"
            }
        val phoneKey = normalizePhoneKey(c.number)
        val nameT9 = nameToT9Digits(c.name)
        if (!matchesDialpadQuery(q, phoneKey, nameT9)) continue
        if (aggKey in seenAggregate) continue
        seenAggregate.add(aggKey)
        matchedContactKeys.add(phoneKey)
        val (a0, a1) = contactAvatarArgbPair(c.avatarColor)
        out.add(
            DialpadMatchRow(
                title = c.name.ifBlank { c.number },
                subtitle = c.number,
                telSanitized = sanitizeForTelDial(c.number),
                deviceContactId = c.deviceContactId,
                avatarStartArgb = a0,
                avatarEndArgb = a1,
            ),
        )
    }

    for (r in recentCalls) {
        if (out.size >= MAX_MATCH_ROWS) break
        val phoneKey = normalizePhoneKey(r.numberOrLabel)
        if (phoneKey.isNotEmpty() && phoneKey in matchedContactKeys) continue
        val nameT9 = nameToT9Digits(r.contactName)
        if (!matchesDialpadQuery(q, phoneKey, nameT9)) continue
        val title = r.contactName.ifBlank { r.numberOrLabel }
        out.add(
            DialpadMatchRow(
                title = title,
                subtitle = r.numberOrLabel,
                telSanitized = sanitizeForTelDial(r.numberOrLabel),
                deviceContactId = r.deviceContactId,
                avatarStartArgb = r.avatarStartArgb,
                avatarEndArgb = r.avatarEndArgb,
            ),
        )
        if (phoneKey.isNotEmpty()) matchedContactKeys.add(phoneKey)
    }

    return out
}
