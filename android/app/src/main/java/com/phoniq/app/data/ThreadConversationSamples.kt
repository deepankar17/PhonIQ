package com.phoniq.app.data

import com.phoniq.app.data.model.ConversationBubble
import com.phoniq.app.data.model.MessageTickVisual
import com.phoniq.app.data.model.ThreadConversationScript

/**
 * Rich thread bodies keyed by [com.phoniq.app.data.model.MessageThread.id],
 * aligned with `design/phoniq-mockup-v1.html` `openSmsThread`.
 */
object ThreadConversationSamples {
    fun scriptFor(threadId: String): ThreadConversationScript? = scripts[threadId]

    private val scripts: Map<String, ThreadConversationScript> =
        mapOf(
            "m1" to
                ThreadConversationScript(
                    plainSmsHint = null,
                    bubbles =
                        listOf(
                            ConversationBubble.DayDivider("Today"),
                            ConversationBubble.TextMessage(
                                body = "Hey, are you joining the stand-up?",
                                time = "9:02",
                                outgoing = false,
                                ticks = MessageTickVisual.None,
                            ),
                            ConversationBubble.TextMessage(
                                body = "Running 10 min late — start without me.",
                                time = "9:04",
                                outgoing = true,
                                ticks = MessageTickVisual.Single,
                            ),
                            ConversationBubble.TextMessage(
                                body = "Sent the notes as well.",
                                time = "9:06",
                                outgoing = true,
                                ticks = MessageTickVisual.Double,
                            ),
                            ConversationBubble.TextMessage(
                                body = "Perfect, thanks!",
                                time = "9:08",
                                outgoing = true,
                                ticks = MessageTickVisual.Read,
                            ),
                            ConversationBubble.ReactionRow("❤️", 1),
                            ConversationBubble.RichLinkBubble(
                                thumbEmoji = "📅",
                                host = "calendar.phoniq.app",
                                title = "Tomorrow · Team sync · 10:30 AM",
                                description = "Rich preview · shared via RCS",
                                footerMessage = "Tomorrow works — here is the invite.",
                                time = "9:12",
                            ),
                            ConversationBubble.TextMessage(
                                body = "Are you coming to the office tomorrow?",
                                time = "9:15",
                                outgoing = false,
                                ticks = MessageTickVisual.None,
                            ),
                        ),
                ),
            "m_rahul" to
                ThreadConversationScript(
                    plainSmsHint = null,
                    bubbles =
                        listOf(
                            ConversationBubble.DayDivider("Yesterday"),
                            ConversationBubble.TextMessage(
                                body = "Bhai, lunch at 1pm?",
                                time = "12:08",
                                outgoing = false,
                                ticks = MessageTickVisual.None,
                            ),
                            ConversationBubble.TextMessage(
                                body = "Sounds good, see you at 1",
                                time = "12:09",
                                outgoing = true,
                                ticks = MessageTickVisual.Read,
                            ),
                            ConversationBubble.VoiceNote(
                                durationLabel = "0:12",
                                bubbleTime = "12:10",
                                ticks = MessageTickVisual.Double,
                            ),
                            ConversationBubble.ReactionRow("😂", 1),
                            ConversationBubble.TypingIndicator,
                        ),
                ),
            "m_phonepe" to
                ThreadConversationScript(
                    plainSmsHint =
                        "SMS transactional feed · No RCS from payment gateways",
                    bubbles =
                        listOf(
                            ConversationBubble.DayDivider("Today"),
                            ConversationBubble.TxnBubble(
                                label = "PHONEPE · DEBIT",
                                amountLine = "₹799.00",
                                body =
                                    "Paid to Blinkit using UPI · Ref 423897233 · Bank XX8812",
                                time = "7:12",
                                showViewInMoney = true,
                            ),
                        ),
                ),
            "m3" to
                ThreadConversationScript(
                    plainSmsHint = "SMS OTP channel · Auto-detected code cards",
                    bubbles =
                        listOf(
                            ConversationBubble.DayDivider("Today"),
                            ConversationBubble.OtpBubble(
                                intro = "Your OTP for VFS Global login is",
                                code = "847291",
                                footer = "Expires in 8 min · Do not share with anyone",
                                time = "8:47",
                            ),
                        ),
                ),
            "m_hdfc" to
                ThreadConversationScript(
                    plainSmsHint =
                        "SMS · Bank alerts use plain text · RCS not available from this sender",
                    bubbles =
                        listOf(
                            ConversationBubble.DayDivider("Today"),
                            ConversationBubble.TxnBubble(
                                label = "HDFCBK · DEBIT",
                                amountLine = "₹2,450.00",
                                body =
                                    "Swiggy order · A/c XX4521 · Avl bal ₹1,24,560 · 30-Apr-26, 10:32 AM",
                                time = "10:32",
                                showViewInMoney = true,
                            ),
                            ConversationBubble.OtpBubble(
                                intro = "HDFC NetBanking: Your OTP is",
                                code = "482916",
                                footer =
                                    "Valid for 3 min · Do not share OTP or CVV with anyone",
                                time = "10:33",
                            ),
                            ConversationBubble.SystemLine(
                                "Delivered as SMS · parsed locally for Money tab",
                            ),
                        ),
                ),
            "m_amazon" to
                ThreadConversationScript(
                    plainSmsHint =
                        "SMS delivery updates · internet not required to read",
                    bubbles =
                        listOf(
                            ConversationBubble.DayDivider("Today"),
                            ConversationBubble.TextMessage(
                                body =
                                    "Amazon: Your order #408-1182726-9925147 is out for delivery. OTP for delivery: 7412",
                                time = "6:41",
                                outgoing = false,
                                ticks = MessageTickVisual.None,
                            ),
                            ConversationBubble.TextMessage(
                                body =
                                    "Driver Ravi is 2 stops away near Koramangala 5th Block. ETA by 8:00 PM.",
                                time = "7:08",
                                outgoing = false,
                                ticks = MessageTickVisual.None,
                            ),
                        ),
                ),
            "m_swiggy" to
                ThreadConversationScript(
                    plainSmsHint = "SMS delivery updates from Swiggy",
                    bubbles =
                        listOf(
                            ConversationBubble.DayDivider("Today"),
                            ConversationBubble.TextMessage(
                                body =
                                    "Swiggy: Order #3188729 confirmed. Veg Thali + Sweet Lassi · Total ₹312.",
                                time = "9:31",
                                outgoing = false,
                                ticks = MessageTickVisual.None,
                            ),
                            ConversationBubble.TextMessage(
                                body =
                                    "Rider Imran (KA03 XX 4821) picked up your order. Live ETA 12 minutes.",
                                time = "9:42",
                                outgoing = false,
                                ticks = MessageTickVisual.None,
                            ),
                        ),
                ),
            "m_flip_log" to
                ThreadConversationScript(
                    plainSmsHint = "SMS parcel tracking feed",
                    bubbles =
                        listOf(
                            ConversationBubble.DayDivider("Today"),
                            ConversationBubble.TextMessage(
                                body =
                                    "Flipkart: Shipment FMPC278341 reached BLR South FC hub. Expected delivery tomorrow.",
                                time = "8:30",
                                outgoing = false,
                                ticks = MessageTickVisual.None,
                            ),
                            ConversationBubble.TextMessage(
                                body =
                                    "You will receive an OTP at delivery time. Keep your phone reachable.",
                                time = "8:31",
                                outgoing = false,
                                ticks = MessageTickVisual.None,
                            ),
                        ),
                ),
        )
}
