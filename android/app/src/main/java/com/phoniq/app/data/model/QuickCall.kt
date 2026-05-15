package com.phoniq.app.data.model

import com.phoniq.app.data.db.entity.CallLogEntity
import com.phoniq.app.util.dedupeRecentCallKey

private data class FreqAgg(val rawNumber: String, var count: Int)

private fun isCallTypeForFrequentStrip(type: String): Boolean =
    when (type) {
        "INCOMING", "OUTGOING", "MISSED" -> true
        else -> false
    }

/**
 * Top [maxEntries] numbers by call volume (incoming + outgoing + missed).
 * Excludes rejected/blocked so the strip stays meaningful.
 */
fun buildTopFrequentCallEntries(
    allCalls: List<CallLogEntity>,
    nameByKey: Map<String, String>,
    deviceContactIdByKey: Map<String, Long>,
    maxEntries: Int = 20,
): List<QuickCallEntry> {
    if (allCalls.isEmpty()) return emptyList()
    val freq = mutableMapOf<String, FreqAgg>()
    for (call in allCalls) {
        if (!isCallTypeForFrequentStrip(call.type)) continue
        val key = dedupeRecentCallKey(call.number)
        if (key == "__empty__") continue
        val agg = freq.getOrPut(key) { FreqAgg(call.number, 0) }
        agg.count++
    }
    if (freq.isEmpty()) return emptyList()
    return freq.entries
        .sortedByDescending { it.value.count }
        .take(maxEntries)
        .mapIndexed { idx, (key, agg) ->
            val name = nameByKey[key] ?: agg.rawNumber
            val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            val colorBits = (key.hashCode().toLong() xor (idx.toLong() * 0x9E3779B9L)) and 0x00FFFFFFL
            val startArgb = 0xFF000000L or colorBits
            val endArgb = 0xFF000000L or (colorBits xor 0x002A2A2AL)
            QuickCallEntry(
                id = key,
                name = name,
                meta = "",
                quickLaneIndex = kotlin.math.abs(key.hashCode()) % 4,
                quickTimes = agg.count,
                initial = initial,
                avatarColorArgb = startArgb,
                avatarEndArgb = endArgb,
                dialableNumber = agg.rawNumber,
                deviceContactId = deviceContactIdByKey[key] ?: 0L,
            )
        }
}

data class QuickCallEntry(
    val id: String,
    val name: String,
    /** Legacy subtitle when [quickTimes] is 0 (e.g. relative time from recents fallback). */
    val meta: String,
    /** Index into `quick_call_lanes` string array for mockup-style lines (`WA Video · 5x`). */
    val quickLaneIndex: Int = 0,
    val quickTimes: Int = 0,
    val initial: String,
    /** Gradient start (ARGB). */
    val avatarColorArgb: Long,
    /** Gradient end (ARGB), or 0 to derive a darker tone from [avatarColorArgb] in the UI. */
    val avatarEndArgb: Long = 0L,
    /** Raw number for [android.content.Intent.ACTION_DIAL]. */
    val dialableNumber: String,
    val deviceContactId: Long = 0L,
)

fun List<RecentCall>.toQuickCallStripEntries(maxEntries: Int = 12): List<QuickCallEntry> {
    val seen = LinkedHashSet<String>()
    val out = ArrayList<QuickCallEntry>()
    for (call in this) {
        val key = call.numberOrLabel
        if (!seen.add(key)) continue
        out.add(
            QuickCallEntry(
                id = call.id,
                name = call.contactName,
                meta = call.timeLabel,
                initial = call.contactName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                avatarColorArgb = call.avatarStartArgb,
                avatarEndArgb = call.avatarEndArgb,
                dialableNumber = call.numberOrLabel,
                deviceContactId = call.deviceContactId,
            ),
        )
        if (out.size >= maxEntries) break
    }
    return out
}
