package com.phoniq.app.data.model

enum class MessageThreadCategory {
    All,
    Unread,
    Personal,
    Transaction,
    Otp,
    Spam,
}

data class MessageThread(
    val id: String,
    val title: String,
    val snippet: String,
    val timeLabel: String,
    val unread: Boolean,
    val categories: Set<MessageThreadCategory>,
    val subtitleBadge: String? = null,
)

fun MessageThread.matches(category: MessageThreadCategory): Boolean =
    when (category) {
        MessageThreadCategory.All -> true
        MessageThreadCategory.Unread -> unread
        else -> category in categories
    }
