package com.phoniq.app.util

import android.content.Context
import android.content.Intent
import android.os.Build

private const val ACTION_MANAGE_BLOCKED_NUMBERS = "android.telecom.action.MANAGE_BLOCKED_NUMBERS"

/** Opens the system Blocked numbers screen (API 24+). */
fun Context.openBlockedNumbersSettings(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
    return try {
        startActivity(
            Intent(ACTION_MANAGE_BLOCKED_NUMBERS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    } catch (_: Exception) {
        false
    }
}
