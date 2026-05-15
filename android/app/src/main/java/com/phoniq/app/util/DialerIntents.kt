package com.phoniq.app.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat

/**
 * Strips spaces/punctuation from [raw] so [Uri] `tel:` works reliably across devices.
 * Keeps a single leading `+`, digits, and `*` / `#` (DTMF).
 */
fun sanitizeForTelDial(raw: String): String {
    val out = StringBuilder()
    for (c in raw.trim()) {
        when {
            c.isDigit() -> out.append(c)
            c == '+' && out.isEmpty() -> out.append(c)
            c == '*' || c == '#' -> out.append(c)
        }
    }
    return out.toString()
}

/** Opens the system dialer with [rawNumber] prefilled (does not place a call). */
fun Context.startDialer(rawNumber: String): Boolean {
    val n = sanitizeForTelDial(rawNumber)
    if (n.isEmpty() || n == "+") return false
    return try {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", n, null)))
        true
    } catch (_: Exception) {
        false
    }
}

/**
 * Places an outgoing PSTN call through [TelecomManager] when [Manifest.permission.CALL_PHONE] is granted.
 * Returns false if permission is missing or [TelecomManager.placeCall] fails — use [startDialer] as fallback.
 */
fun Context.placeOutgoingTelCall(rawNumber: String): Boolean {
    val n = sanitizeForTelDial(rawNumber)
    if (n.isEmpty() || n == "+") return false
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return false
    }
    return try {
        val tm = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        tm.placeCall(Uri.fromParts("tel", n, null), Bundle())
        true
    } catch (_: Exception) {
        false
    }
}
