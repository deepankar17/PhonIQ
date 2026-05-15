package com.phoniq.app.data.model

import com.phoniq.app.data.db.entity.ContactEntity
import com.phoniq.app.util.normalizePhoneKey

/** Contacts that share the same normalized phone key (merge candidates). */
data class DuplicateContactGroup(
    val displayNumber: String,
    val contacts: List<ContactEntity>,
)

fun buildDuplicateContactGroups(contacts: List<ContactEntity>): List<DuplicateContactGroup> =
    contacts
        .groupBy { normalizePhoneKey(it.number) }
        .filter { (key, rows) -> key.isNotEmpty() && rows.size > 1 }
        .map { (_, rows) ->
            DuplicateContactGroup(
                displayNumber = rows.firstOrNull()?.number?.trim().orEmpty(),
                contacts = rows.sortedBy { it.name.lowercase() },
            )
        }
        .sortedByDescending { it.contacts.size }
