package com.phoniq.app.data.mapper

import android.text.format.DateUtils
import com.phoniq.app.data.db.entity.CallLogEntity
import com.phoniq.app.data.db.entity.SmsMessageEntity
import com.phoniq.app.data.model.CallChannel
import com.phoniq.app.data.model.CallDirection
import com.phoniq.app.data.model.MessageThread
import com.phoniq.app.data.model.MessageThreadCategory
import com.phoniq.app.data.model.RecentCall

// ---------------------------------------------------------------------------
// CallLogEntity → RecentCall
// ---------------------------------------------------------------------------

fun CallLogEntity.toRecentCall(
    contactName: String? = null,
    isSpam: Boolean = false,
): RecentCall {
    val direction = when (type) {
        "INCOMING" -> CallDirection.Incoming
        "OUTGOING" -> CallDirection.Outgoing
        "MISSED" -> CallDirection.Missed
        "REJECTED", "BLOCKED" -> CallDirection.Rejected
        else -> CallDirection.Incoming
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

    return RecentCall(
        id = id.toString(),
        contactName = displayName,
        numberOrLabel = number,
        timeLabel = timeStr,
        direction = direction,
        channel = CallChannel.Pstn,
        isSpam = isSpam,
        isBlocked = type == "BLOCKED",
        isInternational = number.startsWith("+") && !number.startsWith("+91"),
        metaCaption = meta,
    )
}

// ---------------------------------------------------------------------------
// SmsMessageEntity → MessageThread
// ---------------------------------------------------------------------------

fun SmsMessageEntity.toMessageThread(): MessageThread {
    val uiCategories: Set<MessageThreadCategory> = when (category) {
        "OTP" -> setOf(MessageThreadCategory.Otp)
        "TRANSACTION" -> setOf(MessageThreadCategory.Transaction)
        "SPAM" -> setOf(MessageThreadCategory.Spam)
        "PROMO" -> setOf(MessageThreadCategory.Spam)
        "BILL" -> setOf(MessageThreadCategory.Bill)
        "DELIVERY" -> setOf(MessageThreadCategory.Delivery)
        "TRAVEL" -> setOf(MessageThreadCategory.Travel)
        "PERSONAL" -> setOf(MessageThreadCategory.Personal)
        else -> emptySet()
    }

    val pills = buildList {
        if (MessageThreadCategory.Otp in uiCategories) add("OTP")
        if (MessageThreadCategory.Transaction in uiCategories) add("TXN")
        if (MessageThreadCategory.Bill in uiCategories) add("BILL")
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
        categories = uiCategories,
        peerAddress = sender,
        rowPills = pills,
    )
}

// ---------------------------------------------------------------------------
// Group SMS entities into per-thread MessageThreads
// (one MessageThread per unique threadId, using the latest message as snippet)
// ---------------------------------------------------------------------------

fun List<SmsMessageEntity>.toMessageThreads(): List<MessageThread> =
    this.groupBy { it.threadId }
        .values
        .mapNotNull { messages ->
            messages.maxByOrNull { it.timestamp }?.toMessageThread()
                ?.copy(unread = messages.any { !it.isRead })
        }
        .sortedByDescending { thread ->
            // Re-derive timestamp for sort from the threadId prefix
            this.filter { "sms_${it.threadId}" == thread.id || it.threadId == thread.id.removePrefix("sms_") }
                .maxOfOrNull { it.timestamp } ?: 0L
        }
