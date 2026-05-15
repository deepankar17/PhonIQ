package com.phoniq.app.data.model

import com.phoniq.app.data.db.entity.CallLogEntity
import com.phoniq.app.data.db.entity.ContactEntity
import com.phoniq.app.util.normalizePhoneKey
import com.phoniq.app.util.primaryNameByNormalizedPhone

/** On-device aggregates for the Phone ⋮ Communication insights screen. */
data class CommunicationInsights(
    val totalCalls: Int,
    val pstnCalls: Int,
    val missedCount: Int,
    val incomingCount: Int,
    val outgoingCount: Int,
    val rejectedCount: Int,
    val topContactLabel: String?,
    val topContactCallCount: Int,
    val totalTalkSeconds: Long,
    val hasData: Boolean,
) {
    companion object {
        val Empty =
            CommunicationInsights(
                totalCalls = 0,
                pstnCalls = 0,
                missedCount = 0,
                incomingCount = 0,
                outgoingCount = 0,
                rejectedCount = 0,
                topContactLabel = null,
                topContactCallCount = 0,
                totalTalkSeconds = 0L,
                hasData = false,
            )
    }
}

fun formatTalkDuration(seconds: Long): String {
    if (seconds <= 0L) return "0m"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return when {
        h > 0 -> "${h}h ${m}m"
        else -> "${m}m"
    }
}

fun buildCommunicationInsights(
    calls: List<CallLogEntity>,
    contacts: List<ContactEntity>,
): CommunicationInsights {
    if (calls.isEmpty()) return CommunicationInsights.Empty
    val nameByKey = contacts.primaryNameByNormalizedPhone()
    val byNumber = calls.groupBy { normalizePhoneKey(it.number) }
    val topEntry = byNumber.maxByOrNull { it.value.size }
    val topLabel =
        topEntry?.value?.firstOrNull()?.let { entity ->
            val nk = normalizePhoneKey(entity.number)
            nameByKey[nk]?.takeIf { it.isNotBlank() } ?: entity.number
        }
    val topCount = topEntry?.value?.size ?: 0
    val missed = calls.count { it.type == "MISSED" }
    val incoming = calls.count { it.type == "INCOMING" }
    val outgoing = calls.count { it.type == "OUTGOING" }
    val rejected = calls.count { it.type == "REJECTED" }
    val talkSeconds =
        calls
            .filter { it.type == "INCOMING" || it.type == "OUTGOING" }
            .sumOf { it.durationSec.toLong() }
    val pstn = calls.count { it.callChannel == "PSTN" }
    return CommunicationInsights(
        totalCalls = calls.size,
        pstnCalls = pstn,
        missedCount = missed,
        incomingCount = incoming,
        outgoingCount = outgoing,
        rejectedCount = rejected,
        topContactLabel = topLabel,
        topContactCallCount = topCount,
        totalTalkSeconds = talkSeconds,
        hasData = true,
    )
}
