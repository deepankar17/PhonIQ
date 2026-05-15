package com.phoniq.app.data.model

import com.phoniq.app.util.normalizePhoneKey

enum class CallDirection {
    Incoming,
    Outgoing,
    Missed,
    Rejected,
}

enum class CallChannel {
    Pstn,
    WhatsAppVoice,
    WhatsAppVideo,
}

data class RecentCall(
    val id: String,
    val contactName: String,
    val numberOrLabel: String,
    val timeLabel: String,
    val direction: CallDirection,
    val channel: CallChannel,
    val missedStreak: Int = 0,
    val isSpam: Boolean = false,
    val isBlocked: Boolean = false,
    /** User marked this normalized number as trusted (suppresses heuristics). */
    val isUserTrusted: Boolean = false,
    val isInternational: Boolean = false,
    /** True when this number matched an entry in the device contacts cache. */
    val hasDeviceContact: Boolean = false,
    /** Android [ContactsContract.Contacts._ID] for loading a contact photo (0 if unknown). */
    val deviceContactId: Long = 0L,
    /**
     * Second line under the name, matching mockup `call-number` (e.g. `Incoming · 3m`, `Missed (2)`,
     * `WhatsApp · 7m`). When null, the UI synthesizes a short line from [direction] + [channel].
     */
    val metaCaption: String? = null,
    /** Avatar gradient (mockup `call-avatar` inline gradients). */
    val avatarStartArgb: Long = 0xFF607D8BL,
    val avatarEndArgb: Long = 0xFF455A64L,
)

enum class RecentCallFilter {
    All,
    Missed,
    Incoming,
    Outgoing,
    Rejected,
}

fun RecentCall.matches(filter: RecentCallFilter): Boolean =
    when (filter) {
        RecentCallFilter.All -> true
        RecentCallFilter.Missed -> direction == CallDirection.Missed
        RecentCallFilter.Incoming -> direction == CallDirection.Incoming
        RecentCallFilter.Outgoing -> direction == CallDirection.Outgoing
        RecentCallFilter.Rejected -> direction == CallDirection.Rejected
    }

/** Map a recent-call row to a contact profile (mockup: tap call-item → `#view-contact`). */
fun RecentCall.toContactProfileRow(): ContactRow {
    val risk =
        when {
            isBlocked -> "Blocked"
            isSpam -> "Likely spam"
            isUserTrusted -> "Trusted"
            else -> null
        }
    return ContactRow(
        id = "recent_$id",
        name = contactName,
        subtitle = numberOrLabel,
        detailNumber = numberOrLabel,
        riskNote = risk,
        avatarStartArgb = avatarStartArgb,
        avatarEndArgb = avatarEndArgb,
        deviceContactId = deviceContactId,
    )
}

data class ContactRow(
    val id: String,
    val name: String,
    val subtitle: String,
    val riskNote: String? = null,
    /** Shown under name (e.g. +91 …); falls back to [subtitle] if null. */
    val detailNumber: String? = null,
    /** All raw numbers when grouped from device contacts; primary row actions use first entry. */
    val allPhoneNumbers: List<String> = emptyList(),
    val avatarStartArgb: Long = 0xFF8C5FE8L,
    val avatarEndArgb: Long = 0xFF6C63FFL,
    val deviceContactId: Long = 0L,
)

/** Numbers to show/call-history match: merged list first, otherwise de-duplicated [detailNumber]/[subtitle]. */
fun ContactRow.effectivePhoneNumbers(): List<String> {
    if (allPhoneNumbers.isNotEmpty()) return allPhoneNumbers
    val seen = LinkedHashSet<String>()
    val out = ArrayList<String>(2)
    for (raw in sequenceOf(detailNumber, subtitle)) {
        val t = raw?.trim()?.takeIf { it.isNotEmpty() } ?: continue
        val key = normalizePhoneKey(t).ifEmpty { "raw:$t" }
        if (seen.add(key)) out.add(t)
    }
    return out
}

data class ContactHistoryEntry(
    val directionMeta: String,
    val timeMeta: String,
    val incoming: Boolean,
)
