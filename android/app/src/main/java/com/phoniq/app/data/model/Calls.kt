package com.phoniq.app.data.model

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
    val isInternational: Boolean = false,
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

data class ContactRow(
    val id: String,
    val name: String,
    val subtitle: String,
    val riskNote: String? = null,
)
