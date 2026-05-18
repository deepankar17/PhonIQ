package com.phoniq.app.data.repository

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.content.ContentUris
import com.phoniq.app.data.db.dao.ContactDao
import com.phoniq.app.data.db.entity.ContactEntity
import com.phoniq.app.data.model.ContactPhoneEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ContactsRepository(
    private val context: Context,
    private val contactDao: ContactDao,
) {
    val allContacts: Flow<List<ContactEntity>> = contactDao.observeAll()

    val starredContacts: Flow<List<ContactEntity>> = contactDao.observeStarred()

    fun search(query: String): Flow<List<ContactEntity>> = contactDao.search(query)

    suspend fun findByNumber(number: String): ContactEntity? = contactDao.findByNumber(number)

    /** Replace local contact cache with device contacts (phones + starred flag). */
    suspend fun syncDeviceContacts() = withContext(Dispatchers.IO) {
        val resolver: ContentResolver = context.contentResolver
        val projection = arrayOf(
            Phone.DISPLAY_NAME,
            Phone.NUMBER,
            Phone.CONTACT_ID,
            Phone.TYPE,
            Phone.LABEL,
        )
        val cursor = resolver.query(
            Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${Phone.DISPLAY_NAME} ASC",
        ) ?: return@withContext

        data class Row(
            val name: String,
            val number: String,
            val contactId: Long,
            /** Resolved label for this phone row (Mobile, Work, custom, …). */
            val phoneLabel: String?,
        )

        val res = context.resources
        val rows = buildList {
            cursor.use { c ->
                val nameCol = c.getColumnIndex(Phone.DISPLAY_NAME)
                val numCol = c.getColumnIndex(Phone.NUMBER)
                val idCol = c.getColumnIndex(Phone.CONTACT_ID)
                val typeCol = c.getColumnIndex(Phone.TYPE)
                val labelCol = c.getColumnIndex(Phone.LABEL)
                while (c.moveToNext()) {
                    val name = c.getString(nameCol) ?: continue
                    val number = c.getString(numCol) ?: continue
                    val type =
                        when {
                            typeCol < 0 || c.isNull(typeCol) -> Phone.TYPE_OTHER
                            else -> c.getInt(typeCol)
                        }
                    val customLabel =
                        when {
                            labelCol < 0 || c.isNull(labelCol) -> null
                            else -> c.getString(labelCol)
                        }
                    val phoneLabel =
                        Phone.getTypeLabel(res, type, customLabel)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                    if (idCol < 0) {
                        add(Row(name = name, number = number, contactId = 0L, phoneLabel = phoneLabel))
                        continue
                    }
                    val cid = c.getLong(idCol)
                    add(Row(name = name, number = number, contactId = cid, phoneLabel = phoneLabel))
                }
            }
        }

        val starredById = loadStarredByContactId(resolver, rows.map { it.contactId }.filter { it > 0 }.distinct())

        contactDao.deleteAll()
        val batch = rows.map { r ->
            ContactEntity(
                name = r.name,
                number = r.number,
                deviceContactId = r.contactId,
                isStarred = starredById[r.contactId] == true,
                tag = r.phoneLabel,
            )
        }
        if (batch.isNotEmpty()) {
            contactDao.insertAll(batch)
        }
    }

    suspend fun save(contact: ContactEntity): Long = contactDao.insert(contact)
    suspend fun update(contact: ContactEntity) = contactDao.update(contact)
    suspend fun delete(contact: ContactEntity) = contactDao.delete(contact)

    /**
     * Insert a new device contact (no existing aggregate) and re-sync.
     * Returns the new [ContactsContract.Contacts._ID] or 0L on failure.
     */
    suspend fun insertDeviceContact(
        displayName: String,
        phones: List<ContactPhoneEntry>,
    ): Long = withContext(Dispatchers.IO) {
        try {
            val ops = ArrayList<ContentProviderOperation>()
            ops.add(
                ContentProviderOperation
                    .newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build(),
            )
            val name = displayName.trim()
            if (name.isNotEmpty()) {
                ops.add(
                    ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(StructuredName.DISPLAY_NAME, name)
                        .build(),
                )
            }
            for (entry in phones) {
                val num = entry.number.trim()
                if (num.isEmpty()) continue
                val (type, custom) = mapPhoneLabelToType(entry.label)
                val op =
                    ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                        .withValue(Phone.NUMBER, num)
                        .withValue(Phone.TYPE, type)
                if (custom != null) {
                    op.withValue(Phone.LABEL, custom)
                }
                ops.add(op.build())
            }
            val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            val rawId = results.firstOrNull()?.uri?.let { ContentUris.parseId(it) } ?: 0L
            val contactId = if (rawId > 0L) lookupContactIdForRawContact(rawId) else 0L
            try {
                syncDeviceContacts()
            } catch (_: Exception) {
            }
            contactId
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Replace name + all phone entries for an existing [deviceContactId]. Other data rows
     * (emails, addresses) are left untouched. Returns true on success.
     */
    suspend fun updateDeviceContactNameAndPhones(
        deviceContactId: Long,
        displayName: String,
        phones: List<ContactPhoneEntry>,
    ): Boolean = withContext(Dispatchers.IO) {
        if (deviceContactId <= 0L) return@withContext false
        val rawIds = lookupRawContactIdsForContact(deviceContactId)
        if (rawIds.isEmpty()) return@withContext false
        val primaryRaw = rawIds.first()
        try {
            val ops = ArrayList<ContentProviderOperation>()
            ops.add(
                ContentProviderOperation
                    .newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(primaryRaw.toString(), StructuredName.CONTENT_ITEM_TYPE),
                    )
                    .build(),
            )
            val name = displayName.trim()
            if (name.isNotEmpty()) {
                ops.add(
                    ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, primaryRaw)
                        .withValue(ContactsContract.Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(StructuredName.DISPLAY_NAME, name)
                        .build(),
                )
            }
            for (raw in rawIds) {
                ops.add(
                    ContentProviderOperation
                        .newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(raw.toString(), Phone.CONTENT_ITEM_TYPE),
                        )
                        .build(),
                )
            }
            for (entry in phones) {
                val num = entry.number.trim()
                if (num.isEmpty()) continue
                val (type, custom) = mapPhoneLabelToType(entry.label)
                val op =
                    ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, primaryRaw)
                        .withValue(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                        .withValue(Phone.NUMBER, num)
                        .withValue(Phone.TYPE, type)
                if (custom != null) {
                    op.withValue(Phone.LABEL, custom)
                }
                ops.add(op.build())
            }
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            try {
                syncDeviceContacts()
            } catch (_: Exception) {
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /** All `RawContacts._ID` rows for the aggregate [contactId]. Primary RawContact = first by id. */
    private fun lookupRawContactIdsForContact(contactId: Long): List<Long> {
        if (contactId <= 0L) return emptyList()
        val out = ArrayList<Long>()
        try {
            context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts._ID),
                "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                arrayOf(contactId.toString()),
                "${ContactsContract.RawContacts._ID} ASC",
            )?.use { c ->
                val idCol = c.getColumnIndex(ContactsContract.RawContacts._ID)
                while (c.moveToNext()) out.add(c.getLong(idCol))
            }
        } catch (_: Exception) {
        }
        return out
    }

    /** Aggregate `Contacts._ID` for a newly-inserted RawContact (system may aggregate after a short delay). */
    private fun lookupContactIdForRawContact(rawContactId: Long): Long {
        if (rawContactId <= 0L) return 0L
        return try {
            context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts.CONTACT_ID),
                "${ContactsContract.RawContacts._ID} = ?",
                arrayOf(rawContactId.toString()),
                null,
            )?.use { c ->
                if (c.moveToFirst()) {
                    val col = c.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)
                    if (col >= 0) c.getLong(col) else 0L
                } else {
                    0L
                }
            } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    /** Sets Android Contacts [ContactsContract.Contacts.STARRED] for [deviceContactId]. */
    suspend fun setDeviceContactStarred(deviceContactId: Long, starred: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (deviceContactId <= 0L) return@withContext false
        val values =
            ContentValues().apply {
                put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0)
            }
        val uri: Uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, deviceContactId)
        try {
            context.contentResolver.update(uri, values, null, null) > 0
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Deletes aggregate contacts from the device provider. Returns the count of contacts removed.
     */
    suspend fun deleteDeviceContacts(deviceContactIds: Collection<Long>): Int = withContext(Dispatchers.IO) {
        if (deviceContactIds.isEmpty()) return@withContext 0
        var deleted = 0
        for (id in deviceContactIds.distinct()) {
            if (id <= 0L) continue
            try {
                val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id)
                if (context.contentResolver.delete(uri, null, null) > 0) deleted++
            } catch (_: SecurityException) {
            } catch (_: Exception) {
            }
        }
        if (deleted > 0) {
            try {
                syncDeviceContacts()
            } catch (_: Exception) {
            }
        }
        deleted
    }
}

/** Resolve `Phone.TYPE` + optional `Phone.LABEL` from a user-facing phone label (`Mobile`, `Home`, …). */
private fun mapPhoneLabelToType(label: String?): Pair<Int, String?> {
    val s = label?.trim()
    if (s.isNullOrEmpty()) return Phone.TYPE_MOBILE to null
    return when (s.lowercase()) {
        "mobile", "cell", "cell phone" -> Phone.TYPE_MOBILE to null
        "home" -> Phone.TYPE_HOME to null
        "work", "office" -> Phone.TYPE_WORK to null
        "main" -> Phone.TYPE_MAIN to null
        "work fax", "fax work" -> Phone.TYPE_FAX_WORK to null
        "home fax", "fax home" -> Phone.TYPE_FAX_HOME to null
        "pager" -> Phone.TYPE_PAGER to null
        "other" -> Phone.TYPE_OTHER to null
        "whatsapp" -> Phone.TYPE_CUSTOM to "Whatsapp"
        else -> Phone.TYPE_CUSTOM to s
    }
}

private fun loadStarredByContactId(resolver: ContentResolver, contactIds: List<Long>): Map<Long, Boolean> {
    if (contactIds.isEmpty()) return emptyMap()
    val sel = contactIds.joinToString(",")
    val map = mutableMapOf<Long, Boolean>()
    val c = resolver.query(
        ContactsContract.Contacts.CONTENT_URI,
        arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.STARRED),
        "${ContactsContract.Contacts._ID} IN ($sel)",
        null,
        null,
    ) ?: return emptyMap()
    c.use { cur ->
        val idCol = cur.getColumnIndex(ContactsContract.Contacts._ID)
        val starCol = cur.getColumnIndex(ContactsContract.Contacts.STARRED)
        while (cur.moveToNext()) {
            val id = cur.getLong(idCol)
            map[id] = cur.getInt(starCol) != 0
        }
    }
    return map
}
