package com.phoniq.app.data.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
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

    fun search(query: String): Flow<List<ContactEntity>> = contactDao.search(query)

    suspend fun findByNumber(number: String): ContactEntity? = contactDao.findByNumber(number)

    /** One-time or on-demand sync of device contacts into Room. */
    suspend fun syncDeviceContacts() = withContext(Dispatchers.IO) {
        val resolver: ContentResolver = context.contentResolver
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
        ) ?: return@withContext

        cursor.use { c ->
            val nameCol = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numCol = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (c.moveToNext()) {
                val name = c.getString(nameCol) ?: continue
                val number = c.getString(numCol) ?: continue
                contactDao.insert(ContactEntity(name = name, number = number))
            }
        }
    }

    suspend fun save(contact: ContactEntity): Long = contactDao.insert(contact)
    suspend fun update(contact: ContactEntity) = contactDao.update(contact)
    suspend fun delete(contact: ContactEntity) = contactDao.delete(contact)
}
