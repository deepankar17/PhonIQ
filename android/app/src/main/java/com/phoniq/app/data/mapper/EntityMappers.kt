package com.phoniq.app.data.mapper

import android.text.format.DateUtils
import com.phoniq.app.data.db.entity.CallLogEntity
import com.phoniq.app.data.db.entity.ContactEntity
import com.phoniq.app.data.db.entity.SmsMessageEntity
import com.phoniq.app.data.model.CallChannel
import com.phoniq.app.data.model.CallDirection
import com.phoniq.app.data.model.ContactHistoryEntry
import com.phoniq.app.data.model.ContactRow
import com.phoniq.app.data.model.MessageThread
import com.phoniq.app.data.model.MessageThreadCategory
import com.phoniq.app.data.model.RecentCall
import com.phoniq.app.domain.sms.SmsParser
import com.phoniq.app.util.dedupeRecentCallKey
import com.phoniq.app.util.normalizePhoneKey
import java.util.Locale

// ---------------------------------------------------------------------------
// CallLogEntity → RecentCall
// ---------------------------------------------------------------------------

fun CallLogEntity.toRecentCall(
    contactName: String? = null,
    isSpam: Boolean = false,
    isUserTrusted: Boolean = false,
    hasDeviceContact: Boolean = false,
    deviceContactId: Long = 0L,
    missedStreak: Int = 0,
): RecentCall {
    val direction = when (type) {
        "INCOMING" -> CallDirection.Incoming
        "OUTGOING" -> CallDirection.Outgoing
        "MISSED" -> CallDirection.Missed
        "REJECTED", "BLOCKED" -> CallDirection.Rejected
        else -> CallDirection.Incoming
    }
    val channel = when (callChannel) {
        "WHATSAPP_VIDEO" -> CallChannel.WhatsAppVideo
        "WHATSAPP_VOICE" -> CallChannel.WhatsAppVoice
        else -> CallChannel.Pstn
    }
    val displayName = contactName ?: number
    val timeStr = DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()

    val meta = when {
        type == "BLOCKED" -> "Blocked automatically"
        type == "MISSED" -> null
        durationSec > 0 -> {
            val mins = durationSec / 60
            val secs = durationSec % 60
            val dur = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
            "${if (direction == CallDirection.Outgoing) "Outgoing" else "Incoming"} · $dur"
        }
        else -> null
    }

    val hue = dedupeRecentCallKey(number).hashCode().toLong() and 0x00FFFFFFL
    val startArgb = 0xFF000000L or hue
    val endArgb = 0xFF000000L or (hue xor 0x00333333L)

    return RecentCall(
        id = id.toString(),
        contactName = displayName,
        numberOrLabel = number,
        timeLabel = timeStr,
        direction = direction,
        channel = channel,
        missedStreak = missedStreak,
        isSpam = isSpam,
        isUserTrusted = isUserTrusted,
        isBlocked = type == "BLOCKED",
        isInternational = number.startsWith("+") && !number.startsWith("+91"),
        metaCaption = meta,
        hasDeviceContact = hasDeviceContact,
        deviceContactId = deviceContactId,
        avatarStartArgb = startArgb,
        avatarEndArgb = endArgb,
        timestampMs = timestamp,
    )
}

private val threadSmsParser = SmsParser()

internal fun SmsMessageEntity.messageThreadCategories(): Set<MessageThreadCategory> =
    when (category) {
        "OTP" -> setOf(MessageThreadCategory.Otp)
        "TRANSACTION" -> setOf(MessageThreadCategory.Transaction)
        "PROMO" -> setOf(MessageThreadCategory.Offers)
        "SPAM" -> setOf(MessageThreadCategory.Spam)
        "BILL" -> setOf(MessageThreadCategory.Bill)
        "DELIVERY" -> setOf(MessageThreadCategory.Delivery)
        "TRAVEL" -> setOf(MessageThreadCategory.Travel)
        "PERSONAL" -> setOf(MessageThreadCategory.Personal)
        else -> emptySet()
    }

/** Matches bill-thread `INV` pill: transaction SMS classified as investment capital. */
internal fun SmsMessageEntity.isInvestmentTxnSms(parser: SmsParser): Boolean {
    if (category != "TRANSACTION") return false
    return parser.parse(sender, body).transaction?.category == "INVESTMENT"
}

// ---------------------------------------------------------------------------
// SmsMessageEntity → MessageThread
// ---------------------------------------------------------------------------

fun SmsMessageEntity.toMessageThread(): MessageThread {
    val uiCategories = messageThreadCategories()

    val pills = buildList {
        if (MessageThreadCategory.Otp in uiCategories) add("OTP")
        if (MessageThreadCategory.Transaction in uiCategories) add("TXN")
        if (MessageThreadCategory.Bill in uiCategories) add("BILL")
        if (MessageThreadCategory.Offers in uiCategories) add("OFFERS")
        if (MessageThreadCategory.Spam in uiCategories) add("SPAM")
    }

    val timeStr = DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()

    return MessageThread(
        id = "sms_${threadId}",
        title = sender,
        snippet = body.take(100),
        timeLabel = timeStr,
        unread = !isRead,
        unreadCount = if (!isRead) 1 else 0,
        categories = uiCategories,
        peerAddress = sender,
        rowPills = pills,
    )
}

// ---------------------------------------------------------------------------
// Group SMS entities into per-thread MessageThreads
// (one MessageThread per unique threadId, using the latest message as snippet)
// ---------------------------------------------------------------------------

fun List<SmsMessageEntity>.toMessageThreads(): List<MessageThread> {
    if (isEmpty()) return emptyList()
    return groupBy { it.threadId }
        .values
        .mapNotNull { messages ->
            val latest = messages.maxByOrNull { it.timestamp } ?: return@mapNotNull null
            val isPinned = messages.any { it.isPinned }
            val isArchived = messages.any { it.isArchived }
            val categories = messages.flatMap { it.messageThreadCategories().toList() }.toSet()
            // List preview: show OTP code (not full SMS body) when the latest message is unread OTP.
            val latestUnreadOtp =
                if (latest.isOtp && !latest.isRead) {
                    threadSmsParser.parse(latest.sender, latest.body).otp
                } else {
                    null
                }
            val otpTtlSec = latestUnreadOtp?.ttlSeconds?.coerceIn(60, 3600) ?: 600
            val otpExpiresAt =
                latestUnreadOtp?.let { latest.timestamp + otpTtlSec * 1000L }
            val listSnippet =
                latestUnreadOtp?.code?.takeIf { it.isNotBlank() }
                    ?: latest.body.take(100)
            // Money-style inbox preview when the latest message is transactional.
            val latestParse = threadSmsParser.parse(latest.sender, latest.body)
            val latestTxn = latestParse.transaction
            val latestIsTxn =
                latest.isTransaction ||
                    latest.category == "TRANSACTION" ||
                    latestParse.category == SmsParser.SmsCategory.TRANSACTION
            val latestTxnPreview =
                if (latestIsTxn && latestTxn != null && !latest.isInvestmentTxnSms(threadSmsParser)) {
                    latestTxn.toMessageTxnPreview(latest.timestamp)
                } else {
                    null
                }
            val subtitleBadge =
                if (latestTxnPreview != null) {
                    null
                } else {
                    latestTxn?.let { txn ->
                        val sign = if (txn.type == "DEBIT") "−" else "+"
                        sign + String.format(Locale.US, "₹%,.0f", txn.amount)
                    }
                }
            val pills = buildList {
                if (messages.any { it.category == "PROMO" }) add("OFFERS")
                if (messages.any { it.category == "SPAM" }) add("SPAM")
                if (MessageThreadCategory.Otp in categories) add("OTP")
                if (MessageThreadCategory.Transaction in categories) add("TXN")
                if (MessageThreadCategory.Bill in categories) add("BILL")
                if (MessageThreadCategory.Delivery in categories) add("TRACKING")
                if (MessageThreadCategory.Travel in categories) add("TRAVEL")
                val inv =
                    messages.any { m ->
                        m.category == "TRANSACTION" &&
                            threadSmsParser.parse(m.sender, m.body).transaction?.category == "INVESTMENT"
                    }
                if (inv) add("INV")
                val billSnip =
                    messages.filter { it.category == "BILL" }
                        .maxByOrNull { it.timestamp }
                        ?.body
                        ?.lowercase()
                        .orEmpty()
                if (billSnip.isNotEmpty()) {
                    when {
                        "overdue" in billSnip || "pending since" in billSnip -> add("OVERDUE")
                        "due on" in billSnip || ("due" in billSnip && "₹" in billSnip) -> add("DUE")
                    }
                }
            }
            latest.toMessageThread()
                .copy(
                    snippet = listSnippet,
                    categories = categories,
                    rowPills = pills,
                    unread = messages.any { !it.isRead },
                    unreadCount = messages.count { !it.isRead },
                    otpCode = latestUnreadOtp?.code,
                    otpExpiresAtEpochMillis = otpExpiresAt,
                    subtitleBadge = subtitleBadge,
                    latestTxnPreview = latestTxnPreview,
                    isPinned = isPinned,
                    isArchived = isArchived,
                    lastTimestamp = latest.timestamp,
                )
        }
        .sortedWith(
            compareByDescending<MessageThread> { it.isPinned }
                .thenByDescending { it.lastTimestamp },
        )
}

// ---------------------------------------------------------------------------
// ContactEntity → ContactRow
// ---------------------------------------------------------------------------

fun ContactEntity.toContactRow(): ContactRow {
    val argb = runCatching {
        val c = android.graphics.Color.parseColor(avatarColor)
        c.toLong() and 0xFFFFFFFFL
    }.getOrElse { 0xFF6C63FFL }
    val endArgb = 0xFF000000L or ((argb and 0x00FFFFFFL) xor 0x002A2A2AL)
    val label = tag?.trim()?.takeIf { it.isNotEmpty() }
    return ContactRow(
        id = id.toString(),
        name = name,
        subtitle = number,
        detailNumber = number,
        allPhoneNumbers = listOf(number),
        phoneEntries = listOf(com.phoniq.app.data.model.ContactPhoneEntry(number = number, label = label)),
        avatarStartArgb = argb,
        avatarEndArgb = endArgb,
        deviceContactId = deviceContactId,
    )
}

private fun deviceContactAggregationKey(entity: ContactEntity): String =
    when {
        entity.deviceContactId > 0L -> "d:${entity.deviceContactId}"
        else -> "i:${entity.id}"
    }

private fun orderedDistinctPhoneEntries(rows: Iterable<ContactEntity>): List<com.phoniq.app.data.model.ContactPhoneEntry> {
    val ordered = LinkedHashMap<String, com.phoniq.app.data.model.ContactPhoneEntry>()
    for (e in rows) {
        val raw = e.number.trim()
        if (raw.isEmpty()) continue
        val key = normalizePhoneKey(raw).ifEmpty { "raw:$raw" }
        if (key in ordered) continue
        val label = e.tag?.trim()?.takeIf { it.isNotEmpty() }
        ordered[key] = com.phoniq.app.data.model.ContactPhoneEntry(number = raw, label = label)
    }
    return ordered.values.toList()
}

private fun mergeContactEntitiesToContactRow(rows: List<ContactEntity>): ContactRow {
    require(rows.isNotEmpty())
    val sortedById = rows.sortedBy { it.id }
    val primary = sortedById.first()
    val entries = orderedDistinctPhoneEntries(sortedById)
    val numbers = entries.map { it.number }
    val argb =
        runCatching {
            val c = android.graphics.Color.parseColor(primary.avatarColor)
            c.toLong() and 0xFFFFFFFFL
        }.getOrElse { 0xFF6C63FFL }
    val endArgb = 0xFF000000L or ((argb and 0x00FFFFFFL) xor 0x002A2A2AL)
    val subtitleMerged = numbers.joinToString(separator = " · ")
    val rowId =
        when {
            primary.deviceContactId > 0L -> "aggdc:${primary.deviceContactId}"
            else -> "${primary.id}"
        }
    return ContactRow(
        id = rowId,
        name = primary.name,
        subtitle = subtitleMerged.ifEmpty { primary.number },
        detailNumber = numbers.firstOrNull() ?: primary.number,
        allPhoneNumbers = numbers,
        phoneEntries = entries,
        avatarStartArgb = argb,
        avatarEndArgb = endArgb,
        deviceContactId = primary.deviceContactId,
    )
}

/**
 * One [ContactRow] per Android aggregate contact ([ContactEntity.deviceContactId]) —
 * aligns list UI with [ContactsRepository.syncDeviceContacts], which persists one Room row per phone entry.
 */
fun List<ContactEntity>.aggregateContactsToRows(): List<ContactRow> {
    if (isEmpty()) return emptyList()
    return groupBy(::deviceContactAggregationKey)
        .values
        .map { mergeContactEntitiesToContactRow(it) }
        .sortedWith(
            compareBy(
                { it.name.lowercase(Locale.getDefault()) },
                { it.subtitle.lowercase(Locale.getDefault()) },
            ),
        )
}

// ---------------------------------------------------------------------------
// CallLogEntity → ContactHistoryEntry (contact profile)
// ---------------------------------------------------------------------------

fun CallLogEntity.toContactHistoryEntry(): ContactHistoryEntry {
    val incoming =
        type == "INCOMING" || type == "MISSED" || type == "BLOCKED"
    val verb = when (type) {
        "INCOMING" -> "Incoming"
        "OUTGOING" -> "Outgoing"
        "MISSED" -> "Missed"
        "REJECTED" -> "Rejected"
        "BLOCKED" -> "Blocked"
        else -> type
    }
    val durationPart =
        if (durationSec > 0 && type != "MISSED" && type != "REJECTED" && type != "BLOCKED") {
            val mins = durationSec / 60
            val secs = durationSec % 60
            val dur = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
            " · $dur"
        } else {
            ""
        }
    val timeMeta = DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
    return ContactHistoryEntry(
        directionMeta = verb + durationPart,
        timeMeta = timeMeta,
        incoming = incoming,
    )
        }
