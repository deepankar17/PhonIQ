package com.phoniq.app.data.model

/** WhatsApp-style ticks under outgoing bubbles (mockup `bubble-ticks`). */
enum class MessageTickVisual {
    None,
    Single,
    Double,
    Read,
}

/** Scripted bubbles for thread overlay (`phoniq-mockup-v1.html` `openSmsThread`). */
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
    ) : ConversationBubble

    data class TxnBubble(
        val label: String,
        val amountLine: String,
        val body: String,
        val time: String,
        val showViewInMoney: Boolean,
    ) : ConversationBubble
}

/** Plain-SMS hint when RCS is off — mock `plainHint`. */
data class ThreadConversationScript(
    val plainSmsHint: String?,
    val bubbles: List<ConversationBubble>,
)
