package com.phoniq.app.data.mapper

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import com.phoniq.app.data.db.entity.SmsMessageEntity
import com.phoniq.app.data.model.ConversationBubble
import com.phoniq.app.data.model.MessageTickVisual
import com.phoniq.app.domain.sms.SmsParser
import com.phoniq.app.domain.sms.SmsParser.SmsCategory
import com.phoniq.app.R
import java.util.Calendar
import java.util.Date

data class KeyedConversationBubble(
    val stableKey: String,
    val bubble: ConversationBubble,
)

private fun buildOtpIntroLine(body: String, code: String): String {
    val withoutCode = body.trim().replace(code, " ").trim()
    return withoutCode
        .lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
        .take(220)
}

fun List<SmsMessageEntity>.toKeyedConversationBubbles(context: Context): List<KeyedConversationBubble> {
    if (isEmpty()) return emptyList()
    val parser = SmsParser()
    val sorted = sortedBy { it.timestamp }
    val cal = Calendar.getInstance()
    val result = mutableListOf<KeyedConversationBubble>()
    var lastDayKey: Int? = null
    for (msg in sorted) {
        cal.timeInMillis = msg.timestamp
        val dayKey =
            cal.get(Calendar.YEAR) * 10_000 + cal.get(Calendar.MONTH) * 100 + cal.get(Calendar.DAY_OF_MONTH)
        if (dayKey != lastDayKey) {
            val label = DateUtils.formatDateTime(
                context,
                msg.timestamp,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL,
            )
            result.add(KeyedConversationBubble("day-$dayKey-${msg.id}", ConversationBubble.DayDivider(label)))
            lastDayKey = dayKey
        }
        val time = DateFormat.getTimeFormat(context).format(Date(msg.timestamp))
        val parse = parser.parse(msg.sender, msg.body)
        val otp = parse.otp
        val bubble =
            if (otp != null && parse.category == SmsCategory.OTP) {
                val ttlSec = otp.ttlSeconds.coerceIn(60, 3600)
                val expiresAt = msg.timestamp + ttlSec * 1000L
                val intro =
                    buildOtpIntroLine(msg.body, otp.code).ifBlank {
                        context.getString(R.string.otp_bubble_default_intro)
                    }
                val ttlMin = (ttlSec / 60).coerceAtLeast(1)
                val footer = context.getString(R.string.otp_bubble_footer_approx, ttlMin)
                ConversationBubble.OtpBubble(
                    intro = intro,
                    code = otp.code,
                    footer = footer,
                    time = time,
                    expiresAtEpochMillis = expiresAt,
                )
            } else {
                ConversationBubble.TextMessage(
                    body = msg.body,
                    time = time,
                    outgoing = false,
                    ticks = MessageTickVisual.None,
                )
            }
        result.add(
            KeyedConversationBubble(
                "sms-${msg.id}",
                bubble,
            ),
        )
    }
    return result
}

fun List<SmsMessageEntity>.toConversationBubbles(context: Context): List<ConversationBubble> =
    toKeyedConversationBubbles(context).map { it.bubble }
