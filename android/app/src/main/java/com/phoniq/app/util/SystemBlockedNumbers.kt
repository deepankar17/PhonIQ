package com.phoniq.app.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.BlockedNumberContract

/**
 * Best-effort insert into the system blocked-numbers provider ([BlockedNumberContract]).
 * Succeeds only when the host grants access (typically default SMS / Phone role or privileged permission).
 */
fun Context.tryAddToSystemBlockedNumbers(dialableNumber: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
    val n = dialableNumber.trim()
    if (n.isEmpty()) return false
    return try {
        val values =
            ContentValues().apply {
                put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, n)
            }
        contentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values) != null
    } catch (_: SecurityException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }
}

/** E.164-ish or plain digits suitable for [tryAddToSystemBlockedNumbers], or null for alphanumeric peers. */
fun dialableForBlockedNumberContract(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val digits = buildString {
        for (c in raw.trim()) {
            if (c.isDigit()) append(c)
        }
    }
    if (digits.length < 10) return null
    val last10 = digits.takeLast(10)
    return when {
        raw.contains("+91", ignoreCase = true) || digits.length in 10..11 -> "+91$last10"
        raw.trimStart().startsWith("+") -> raw.trim() // trust user/device format
        else -> last10
    }
}
