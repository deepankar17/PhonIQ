package com.phoniq.app.data.model

import com.phoniq.app.data.db.entity.CallLogEntity
import com.phoniq.app.data.db.entity.ContactEntity
import com.phoniq.app.data.db.entity.SmsMessageEntity
import com.phoniq.app.util.normalizePhoneKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Fused on-device context for the “Who is this?” sheet (call log + SMS + notes). */
data class WhoIsThisSnapshot(
    val rawNumber: String,
    val displayNumber: String,
    val contactName: String?,
    val lastCallLine: String?,
    val lastSmsPreview: String?,
    val lastSmsDateLine: String?,
    val callNote: String?,
    val contactNote: String?,
    val hasData: Boolean,
)

private val callTimeFmt = SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault())
private val smsTimeFmt = SimpleDateFormat("d MMM yyyy · h:mm a", Locale.getDefault())

private fun formatCallLine(call: CallLogEntity): String {
    val whenStr = callTimeFmt.format(Date(call.timestamp))
    val typeLabel =
        when (call.type) {
            "INCOMING" -> "Incoming"
            "OUTGOING" -> "Outgoing"
            "MISSED" -> "Missed"
            "REJECTED" -> "Rejected"
            "BLOCKED" -> "Blocked"
            else -> call.type
        }
    val dur =
        when {
            call.type == "MISSED" || call.type == "REJECTED" -> null
            call.durationSec > 0 -> "${call.durationSec / 60}m ${call.durationSec % 60}s"
            else -> null
        }
    return buildString {
        append(whenStr)
        append(" · ")
        append(typeLabel)
        if (dur != null) {
            append(" · ")
            append(dur)
        }
    }
}

private fun truncateSms(body: String, max: Int = 280): String {
    val single = body.replace('\n', ' ').trim()
    if (single.length <= max) return single
    return single.take(max - 1) + "…"
}

fun buildWhoIsThisSnapshot(
    rawNumber: String,
    calls: List<CallLogEntity>,
    contacts: List<ContactEntity>,
    messages: List<SmsMessageEntity>,
): WhoIsThisSnapshot {
    val trimmed = rawNumber.trim()
    if (trimmed.isEmpty()) {
        return WhoIsThisSnapshot(
            rawNumber = "",
            displayNumber = "",
            contactName = null,
            lastCallLine = null,
            lastSmsPreview = null,
            lastSmsDateLine = null,
            callNote = null,
            contactNote = null,
            hasData = false,
        )
    }
    val key = normalizePhoneKey(trimmed)
    val matchingCalls =
        if (key.isEmpty()) {
            calls.filter { it.number.trim() == trimmed }.sortedByDescending { it.timestamp }
        } else {
            calls.filter { normalizePhoneKey(it.number) == key }.sortedByDescending { it.timestamp }
        }
    val latestCall = matchingCalls.firstOrNull()

    val matchingSms =
        if (key.isEmpty()) {
            messages.filter { it.sender.trim() == trimmed }.maxByOrNull { it.timestamp }
        } else {
            messages.filter { normalizePhoneKey(it.sender) == key }.maxByOrNull { it.timestamp }
        }

    val contact =
        if (key.isEmpty()) {
            contacts.firstOrNull { it.number.trim() == trimmed }
        } else {
            contacts.filter { normalizePhoneKey(it.number) == key }.maxByOrNull { it.name.length }
        }

    val lastCallLine = latestCall?.let { formatCallLine(it) }
    val callNote = latestCall?.notes?.takeIf { !it.isNullOrBlank() }
    val contactNote = contact?.notes?.takeIf { !it.isNullOrBlank() }
    val lastSmsPreview = matchingSms?.body?.let { truncateSms(it) }
    val lastSmsDateLine = matchingSms?.let { smsTimeFmt.format(Date(it.timestamp)) }

    val hasData =
        lastCallLine != null ||
            lastSmsPreview != null ||
            callNote != null ||
            contactNote != null ||
            contact?.name?.isNotBlank() == true

    return WhoIsThisSnapshot(
        rawNumber = trimmed,
        displayNumber = trimmed,
        contactName = contact?.name?.takeIf { it.isNotBlank() },
        lastCallLine = lastCallLine,
        lastSmsPreview = lastSmsPreview,
        lastSmsDateLine = lastSmsDateLine,
        callNote = callNote,
        contactNote = contactNote,
        hasData = hasData,
    )
}
