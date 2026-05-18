package com.phoniq.app.data.model

/** WhatsApp-style ticks under outgoing bubbles (mockup `bubble-ticks`). */
enum class MessageTickVisual {
    None,
    Single,
    Double,
    Read,
}

/** Conversation bubbles for thread UI, built from on-device SMS rows ([SmsMessageEntity]) in the mapper. */
sealed interface ConversationBubble {
    data class DayDivider(val label: String) : ConversationBubble

    data class TextMessage(
        val body: String,
        val time: String,
        val outgoing: Boolean,
        val ticks: MessageTickVisual,
    ) : ConversationBubble

    data class ReactionRow(val emoji: String, val count: Int) : ConversationBubble

    /** RCS link-preview card inside a received bubble. */
    data class RichLinkBubble(
        val thumbEmoji: String,
        val host: String,
        val title: String,
        val description: String,
        val footerMessage: String,
        val time: String,
    ) : ConversationBubble

    data class VoiceNote(
        val durationLabel: String,
        val bubbleTime: String,
        val ticks: MessageTickVisual,
    ) : ConversationBubble

    data object TypingIndicator : ConversationBubble

    data class SystemLine(val text: String) : ConversationBubble

    data class OtpBubble(
        val intro: String,
        val code: String,
        val footer: String,
        val time: String,
        /** When set (real SMS), thread shows live countdown + copy like the inbox list. */
        val expiresAtEpochMillis: Long? = null,
    ) : ConversationBubble

    data class TxnBubble(
        val maskedAccount: String?,
        val narrative: String?,
        /** Localized "Credited" / "Debited" label for the intelligent card. */
        val typeLabel: String,
        val amountLabel: String,
        val isCredit: Boolean,
        val availableBalanceLabel: String?,
        val emoji: String,
        val categoryTag: String,
        val fullBody: String,
        val time: String,
    ) : ConversationBubble
}

/** Parsed transaction fields for thread txn bubbles (from [SmsParser] + body). */
data class TxnBubbleStructured(
    val maskedAccount: String?,
    val narrative: String?,
    /** "CREDIT" | "DEBIT" — map to strings in UI. */
    val typeLabel: String,
    val amountLabel: String,
    val isCredit: Boolean,
    val availableBalanceLabel: String?,
    val emoji: String,
    val categoryTag: String,
    val fullBody: String,
)
