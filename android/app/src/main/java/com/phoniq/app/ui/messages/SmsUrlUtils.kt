package com.phoniq.app.ui.messages

import java.util.regex.Pattern

internal const val SMS_URL_ANNOTATION_TAG = "phoniq_sms_url"

/**
 * Finds http(s) and www. URLs common in SMS bodies. Trims trailing punctuation.
 */
internal fun smsUrlSpans(text: String): List<Triple<Int, Int, String>> {
    if (text.isEmpty()) return emptyList()
    val p =
        Pattern.compile(
            """(?i)(https?://|www\.)\S+""",
        )
    val m = p.matcher(text)
    val raw = mutableListOf<Triple<Int, Int, String>>()
    while (m.find()) {
        var end = m.end()
        var slice = text.substring(m.start(), end)
        slice = trimTrailingUrlPunctuation(slice)
        end = m.start() + slice.length
        if (slice.isEmpty()) continue
        val normalized = normalizeSmsUrl(slice)
        raw.add(Triple(m.start(), end, normalized))
    }
    return mergeNonOverlapping(raw)
}

private fun trimTrailingUrlPunctuation(s: String): String {
    var t = s
    while (t.isNotEmpty()) {
        val c = t.last()
        if (c in ".,;:!?)\"'»”’,，、–—") {
            t = t.dropLast(1)
            continue
        }
        break
    }
    return t
}

internal fun normalizeSmsUrl(raw: String): String {
    val t = raw.trim()
    if (t.startsWith("http://", ignoreCase = true) || t.startsWith("https://", ignoreCase = true)) {
        return t
    }
    return "https://$t"
}

private fun mergeNonOverlapping(sorted: List<Triple<Int, Int, String>>): List<Triple<Int, Int, String>> {
    if (sorted.isEmpty()) return emptyList()
    val byStart = sorted.sortedBy { it.first }
    val out = ArrayList<Triple<Int, Int, String>>()
    var lastEnd = -1
    for (t in byStart) {
        if (t.first < lastEnd) continue
        out.add(t)
        lastEnd = t.second
    }
    return out
}
