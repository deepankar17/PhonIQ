package com.phoniq.app.data.model

enum class MessageThreadCategory {
    All,
    Unread,
    Personal,
    Transaction,
    Otp,
    Bill,
    Delivery,
    Travel,
    /** Marketing / offers SMS (parser PROMO), distinct from [Spam] and personal inbox. */
    Offers,
    Spam,
    Archived,
}

data class MessageThread(
    val id: String,
    val title: String,
    val snippet: String,
    val timeLabel: String,
    val unread: Boolean,
    /** Number of SMS in this thread with [com.phoniq.app.data.db.entity.SmsMessageEntity.isRead] == false when threads were built. */
    val unreadCount: Int = 0,
    val categories: Set<MessageThreadCategory>,
    /** List-row avatar gradient (`wa-thread-avatar` in mockup). */
    val avatarStartArgb: Long = 0xFF6C63FFL,
    val avatarEndArgb: Long = 0xFF4A43CCL,
    val showOnlineDot: Boolean = false,
    val subtitleBadge: String? = null,
    /** Read-only mock for thread header / "who is this" style fusion. */
    val lastCallSummary: String? = null,
    val localNote: String? = null,
    /** Address shown under thread title (+91…, VM-HDFCBK, BH-PhonePe, …). */
    val peerAddress: String? = null,
    /** Small pills after the snippet preview (OTP, TXN, Due, …) — mockup `wa-category-pill`. */
    val rowPills: List<String> = emptyList(),
    val showRcsBadge: Boolean = false,
    /** List-row "typing…" line like mockup Rahul row. */
    val listTypingHint: Boolean = false,
    /** Extracted OTP code for list-row countdown+copy strip (null = no strip). */
    val otpCode: String? = null,
    /** Wall-clock expiry for the OTP in the list row (`sms.timestamp + ttl`). */
    val otpExpiresAtEpochMillis: Long? = null,
    val lastTimestamp: Long = 0L,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    /** When the latest SMS in the thread is a bank/UPI transaction, inbox shows Money-style preview. */
    val latestTxnPreview: MessageTxnPreview? = null,
)

/** Parsed transaction fields for inbox list preview (from [SmsParser] on latest message). */
data class MessageTxnPreview(
    val title: String,
    val subtitle: String,
    val amountLabel: String,
    val isCredit: Boolean,
    val emoji: String,
    val categoryTag: String,
)

fun MessageThread.matches(category: MessageThreadCategory): Boolean =
    when (category) {
        MessageThreadCategory.All -> !isArchived
        MessageThreadCategory.Archived -> isArchived
        MessageThreadCategory.Unread -> unread && !isArchived
        MessageThreadCategory.Personal -> !isArchived && MessageThreadCategory.Personal in categories
        MessageThreadCategory.Transaction -> !isArchived && MessageThreadCategory.Transaction in categories
        MessageThreadCategory.Otp -> !isArchived && MessageThreadCategory.Otp in categories
        MessageThreadCategory.Bill -> !isArchived && MessageThreadCategory.Bill in categories
        MessageThreadCategory.Delivery -> !isArchived && MessageThreadCategory.Delivery in categories
        MessageThreadCategory.Travel -> !isArchived && MessageThreadCategory.Travel in categories
        MessageThreadCategory.Offers -> !isArchived && MessageThreadCategory.Offers in categories
        MessageThreadCategory.Spam -> !isArchived && MessageThreadCategory.Spam in categories
    }

/** On-device hint for bill hygiene UI (pills, badges, or light keyword scan on [snippet]). */
fun MessageThread.billDueHintLabel(): String? {
    rowPills.firstOrNull { pill ->
        pill.contains("due", ignoreCase = true) || pill.contains("overdue", ignoreCase = true)
    }?.let { return it.trim() }
    subtitleBadge?.takeIf { badge ->
        badge.contains("due", ignoreCase = true) || badge.contains("bill", ignoreCase = true)
    }?.let { return it.trim() }
    val s = snippet.trim()
    if (s.isEmpty()) return null
    val lower = s.lowercase()
    if ("due" in lower || "outstanding" in lower || "bill" in lower && anyBillKeyword(lower)) {
        return s.lineSequence().first().take(72).trim()
    }
    return null
}

private fun anyBillKeyword(lower: String): Boolean =
    listOf("electric", "utility", "rent", "recharge", "postpaid", "broadband", "gas", "credit card")
        .any { it in lower }
