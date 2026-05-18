package com.phoniq.app.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract.PhoneLookup

/**
 * Resolves [ContactsContract.Contacts._ID] and a display name from the device contact DB for a
 * PSTN handle (Telecom may already supply CNAP / contact name; this merges in richer device data).
 */
fun Context.lookupContactIdForDialableNumber(rawNumber: String): Long {
    val trimmed = rawNumber.trim()
    if (trimmed.isEmpty() || trimmed.equals("Unknown", ignoreCase = true)) return 0L
    val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(trimmed))
    return try {
        contentResolver.query(
            uri,
            arrayOf(PhoneLookup._ID),
            null,
            null,
            null,
        ).use { c ->
            if (c != null && c.moveToFirst()) {
                c.getLong(0).takeIf { it > 0L } ?: 0L
            } else {
                0L
            }
        }
    } catch (_: SecurityException) {
        0L
    } catch (_: IllegalArgumentException) {
        0L
    }
}

/**
 * Best display name from [PhoneLookup] when available (may match Telecom's or improve it).
 */
fun Context.lookupContactDisplayNameForDialableNumber(rawNumber: String): String? {
    val trimmed = rawNumber.trim()
    if (trimmed.isEmpty()) return null
    val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(trimmed))
    return try {
        contentResolver.query(
            uri,
            arrayOf(PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null,
        ).use { c ->
            if (c != null && c.moveToFirst()) {
                c.getString(0)?.trim()?.takeIf { it.isNotEmpty() }
            } else {
                null
            }
        }
    } catch (_: SecurityException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
