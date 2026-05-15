package com.phoniq.app.util

import com.phoniq.app.data.model.MessageThread

/** Resolve a [MessageThread.id] (e.g. `sms_…`) when [rawNumber] matches thread [MessageThread.peerAddress]. */
fun findMessageThreadIdForNumber(threads: List<MessageThread>, rawNumber: String): String? {
    val key = normalizePhoneKey(rawNumber)
    if (key.isEmpty()) return null
    return threads.firstOrNull { t ->
        val peer = t.peerAddress?.trim().orEmpty()
        peer.isNotEmpty() && normalizePhoneKey(peer) == key
    }?.id
}
