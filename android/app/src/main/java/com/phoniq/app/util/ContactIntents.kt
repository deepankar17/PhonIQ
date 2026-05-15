package com.phoniq.app.util

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract

/** Intent to open the system contact editor for a new contact (People / Contacts app). */
fun buildInsertContactIntent(
    displayName: String?,
    phoneNumber: String?,
): Intent {
    val phone = phoneNumber?.trim().orEmpty()
    val nameRaw = displayName?.trim().orEmpty()
    val name =
        if (nameRaw.isNotEmpty() && normalizePhoneKey(nameRaw) != normalizePhoneKey(phone)) {
            nameRaw
        } else {
            null
        }
    return Intent(ContactsContract.Intents.Insert.ACTION).apply {
        type = ContactsContract.RawContacts.CONTENT_TYPE
        if (name != null) {
            putExtra(ContactsContract.Intents.Insert.NAME, name)
        }
        if (phone.isNotEmpty()) {
            putExtra(ContactsContract.Intents.Insert.PHONE, phone)
        }
    }
}

/** [ContactsContract.Contacts._ID] from a [ActivityResultContracts.PickContact] result URI. */
fun contactIdFromPickUri(resolver: ContentResolver, uri: Uri): Long? {
    val projection = arrayOf(ContactsContract.Contacts._ID)
    resolver.query(uri, projection, null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val col = c.getColumnIndex(ContactsContract.Contacts._ID)
            if (col >= 0) return c.getLong(col)
        }
    }
    return null
}
