package com.phoniq.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens the default SMS app.
 * @param address when null/blank, opens an empty compose sheet when supported; otherwise `smsto:` with normalized digits.
 */
fun Context.startSmsCompose(address: String?, body: String? = null): Boolean {
    val dest = address?.trim().orEmpty()
    val uri =
        if (dest.isEmpty()) {
            Uri.parse("smsto:")
        } else {
            val n = sanitizeForTelDial(dest)
            val target = if (n.isNotEmpty() && n != "+") n else dest
            Uri.parse("smsto:${Uri.encode(target)}")
        }
    val text = body?.trim().orEmpty()
    return try {
        val i = Intent(Intent.ACTION_SENDTO, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (text.isNotEmpty()) {
            i.putExtra("sms_body", text)
        }
        startActivity(i)
        true
    } catch (_: Exception) {
        false
    }
}

/** Opens WhatsApp (chat for [rawAddress]; user can start a voice/video call from there). Digits-only wa.me. */
fun Context.tryOpenWhatsAppForNumber(rawAddress: String): Boolean {
    val digits = buildString { for (c in rawAddress) if (c.isDigit()) append(c) }
    if (digits.isEmpty()) return false
    val uri = Uri.parse("https://wa.me/$digits")
    return try {
        startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: Exception) {
        false
    }
}
