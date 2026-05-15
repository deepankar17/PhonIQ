package com.phoniq.app.data.repository

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.content.ContentUris
import com.phoniq.app.data.db.dao.ContactDao
import com.phoniq.app.data.db.entity.ContactEntity
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
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
        )
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
        ) ?: return@withContext

        data class Row(val name: String, val number: String, val contactId: Long)

        val rows = buildList {
            cursor.use { c ->
                val nameCol = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numCol = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val idCol = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                while (c.moveToNext()) {
                    val name = c.getString(nameCol) ?: continue
                    val number = c.getString(numCol) ?: continue
                    if (idCol < 0) {
                        add(Row(name = name, number = number, contactId = 0L))
                        continue
                    }
                    val cid = c.getLong(idCol)
                    add(Row(name = name, number = number, contactId = cid))
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
            )
        }
        if (batch.isNotEmpty()) {
            contactDao.insertAll(batch)
        }
    }

    suspend fun save(contact: ContactEntity): Long = contactDao.insert(contact)
    suspend fun update(contact: ContactEntity) = contactDao.update(contact)
    suspend fun delete(contact: ContactEntity) = contactDao.delete(contact)

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
