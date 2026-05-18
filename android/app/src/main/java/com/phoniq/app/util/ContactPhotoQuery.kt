package com.phoniq.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/** Contact row IDs that have a photo URI (for settings previews). */
fun Context.contactIdsWithPhoto(limit: Int): List<Long> {
    if (limit <= 0) return emptyList()
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
        return emptyList()
    }
    val out = ArrayList<Long>(limit)
    contentResolver.query(
        ContactsContract.Contacts.CONTENT_URI,
        arrayOf(ContactsContract.Contacts._ID),
        "${ContactsContract.Contacts.PHOTO_URI} IS NOT NULL",
        null,
        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC",
    )?.use { c ->
        val idx = c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
        while (c.moveToNext() && out.size < limit) {
            out.add(c.getLong(idx))
        }
    }
    return out
}
