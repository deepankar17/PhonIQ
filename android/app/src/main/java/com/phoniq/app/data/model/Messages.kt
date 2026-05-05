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
    Spam,
}

data class MessageThread(
    val id: String,
    val title: String,
    val snippet: String,
    val timeLabel: String,
    val unread: Boolean,
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
    /** Initial seconds for the OTP countdown chip (default 10 min). */
    val otpExpiresSeconds: Int = 600,
)

fun MessageThread.matches(category: MessageThreadCategory): Boolean =
    when (category) {
        MessageThreadCategory.All -> true
        MessageThreadCategory.Unread -> unread
        else -> category in categories
    }
