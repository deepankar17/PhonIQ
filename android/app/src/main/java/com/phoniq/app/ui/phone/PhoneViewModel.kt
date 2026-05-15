package com.phoniq.app.ui.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phoniq.app.data.db.entity.CallLogEntity
import com.phoniq.app.data.db.entity.ContactEntity
import com.phoniq.app.data.db.entity.SmsMessageEntity
import com.phoniq.app.data.mapper.toRecentCall
import com.phoniq.app.data.model.CommunicationInsights
import com.phoniq.app.data.model.QuickCallEntry
import com.phoniq.app.data.model.RecentCall
import com.phoniq.app.data.model.WhoIsThisSnapshot
import com.phoniq.app.data.model.buildCommunicationInsights
import com.phoniq.app.data.model.buildTopFrequentCallEntries
import com.phoniq.app.data.model.buildWhoIsThisSnapshot
import com.phoniq.app.data.repository.CallLogRepository
import com.phoniq.app.data.repository.ContactsRepository
import com.phoniq.app.data.repository.SmsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import com.phoniq.app.util.dedupeCallsLatestFirst
import com.phoniq.app.util.missedStreakForNumber
import com.phoniq.app.util.normalizePhoneKey
import com.phoniq.app.util.primaryDeviceContactIdByNormalizedPhone
import com.phoniq.app.util.primaryNameByNormalizedPhone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PhoneViewModel(
    private val callLogRepository: CallLogRepository,
    private val contactsRepository: ContactsRepository,
    private val smsRepository: SmsRepository,
) : ViewModel() {

    val allCalls: StateFlow<List<CallLogEntity>> = callLogRepository.allCalls
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allContacts: StateFlow<List<ContactEntity>> = contactsRepository.allContacts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val starredContacts: StateFlow<List<ContactEntity>> = contactsRepository.starredContacts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val frequentQuickCalls: StateFlow<List<QuickCallEntry>> =
        combine(allCalls, allContacts) { calls, contacts ->
            val nameByKey = contacts.primaryNameByNormalizedPhone()
            val idByKey = contacts.primaryDeviceContactIdByNormalizedPhone()
            buildTopFrequentCallEntries(calls, nameByKey, idByKey, maxEntries = 20)
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val communicationInsights: StateFlow<CommunicationInsights> =
        combine(allCalls, allContacts) { calls, contacts ->
            buildCommunicationInsights(calls, contacts)
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, CommunicationInsights.Empty)

    val spamNumberKeysState: StateFlow<Set<String>> =
        callLogRepository.spamNumberKeys
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val userTrustedNumberKeys: StateFlow<Set<String>> = callLogRepository.userTrustedKeys

    /** Recent call rows for UI; built off the main thread to avoid ANRs on large logs. */
    val recentCalls: StateFlow<List<RecentCall>> =
        combine(
            combine(allCalls, allContacts, starredContacts) { c, ct, st -> Triple(c, ct, st) },
            callLogRepository.spamNumberKeys,
            callLogRepository.userTrustedKeys,
        ) { triple, spamKeys, trustedKeys ->
            val (calls, contacts, starred) = triple
            val nameByKey = contacts.primaryNameByNormalizedPhone()
            val idByKey = contacts.primaryDeviceContactIdByNormalizedPhone()
            val starredKeys =
                starred.map { normalizePhoneKey(it.number) }.filter { it.isNotEmpty() }.toSet()
            dedupeCallsLatestFirst(calls).map { call ->
                val key = normalizePhoneKey(call.number)
                val hasContact = nameByKey.containsKey(key)
                val inSpamDb = key in spamKeys
                val patternSpam = repeatCallerLikelySpam(calls, key)
                val userTrusted = key in trustedKeys
                val starredSafe = key in starredKeys
                val isSpam = (inSpamDb || patternSpam) && !userTrusted && !hasContact && !starredSafe
                call.toRecentCall(
                    contactName = nameByKey[key],
                    isSpam = isSpam,
                    isUserTrusted = userTrusted,
                    hasDeviceContact = hasContact,
                    deviceContactId = idByKey[key] ?: 0L,
                    missedStreak = missedStreakForNumber(calls, call.number),
                )
            }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allSmsMessages: StateFlow<List<SmsMessageEntity>> =
        smsRepository.allMessages
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _whoIsThisOpen = MutableStateFlow(false)
    private val _whoIsThisInput = MutableStateFlow("")

    val whoIsThisInput: StateFlow<String> = _whoIsThisInput.asStateFlow()

    /** Non-null while the “Who is this?” sheet is visible. */
    val whoIsThisSnapshot: StateFlow<WhoIsThisSnapshot?> =
        combine(allCalls, allContacts, allSmsMessages, _whoIsThisOpen, _whoIsThisInput) { c, ct, sms, open, input ->
            if (!open) null
            else buildWhoIsThisSnapshot(input, c, ct, sms)
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun openWhoIsThis(prefillRaw: String? = null) {
        val initial =
            prefillRaw?.takeIf { it.isNotBlank() }
                ?: dedupeCallsLatestFirst(allCalls.value.sortedByDescending { it.timestamp })
                    .firstOrNull()
                    ?.number
                    .orEmpty()
        _whoIsThisInput.value = initial
        _whoIsThisOpen.value = true
    }

    fun dismissWhoIsThis() {
        _whoIsThisOpen.value = false
    }

    fun setWhoIsThisInput(value: String) {
        _whoIsThisInput.value = value
    }

    val contactSearchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val filteredContacts: StateFlow<List<ContactEntity>> = contactSearchQuery
        .debounce(250)
        .flatMapLatest { q ->
            if (q.isBlank()) contactsRepository.allContacts
            else contactsRepository.search(q)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setContactSearch(q: String) { contactSearchQuery.value = q }

    fun syncFromDevice() {
        viewModelScope.launch { refreshFromDevice() }
    }

    suspend fun refreshFromDevice() {
        try {
            callLogRepository.syncDeviceCallLog()
        } catch (_: Exception) {
        }
        try {
            contactsRepository.syncDeviceContacts()
        } catch (_: Exception) {
        }
    }

    suspend fun refreshContactsFromDeviceOnly() {
        try {
            contactsRepository.syncDeviceContacts()
        } catch (_: Exception) {
        }
    }

    fun enqueueContactsRefresh() {
        viewModelScope.launch { refreshContactsFromDeviceOnly() }
    }

    fun clearCallLog() {
        viewModelScope.launch { callLogRepository.clearAll() }
    }

    fun markSpam(number: String) {
        viewModelScope.launch { callLogRepository.markSpam(number) }
    }

    fun unmarkSpam(number: String) {
        viewModelScope.launch { callLogRepository.unmarkSpam(number) }
    }

    fun markTrustedNumber(number: String) {
        viewModelScope.launch { callLogRepository.markUserTrustedNumber(number) }
    }

    fun clearTrustedNumber(number: String) {
        viewModelScope.launch { callLogRepository.clearUserTrustedNumber(number) }
    }

    fun saveCallNote(number: String, note: String) {
        viewModelScope.launch { callLogRepository.saveNoteForLatestCall(number, note) }
    }

    fun starDeviceContact(deviceContactId: Long, onDone: (Boolean) -> Unit) {
        applyDeviceContactStarred(deviceContactId, starred = true, onDone)
    }

    /** Clears Android Contacts [ContactsContract.Contacts.STARRED] for this aggregate contact. */
    fun unstarDeviceContact(deviceContactId: Long, onDone: (Boolean) -> Unit) {
        applyDeviceContactStarred(deviceContactId, starred = false, onDone)
    }

    private fun applyDeviceContactStarred(deviceContactId: Long, starred: Boolean, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok =
                if (deviceContactId > 0L) {
                    contactsRepository.setDeviceContactStarred(deviceContactId, starred)
                } else {
                    false
                }
            if (ok) {
                try {
                    contactsRepository.syncDeviceContacts()
                } catch (_: Exception) {
                }
            }
            onDone(ok)
        }
    }

    /** Star the contact in People that matches [rawNumber] (any phone row with that normalized key). */
    fun starContactForPhoneNumber(rawNumber: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val key = normalizePhoneKey(rawNumber)
            if (key.isEmpty()) {
                onDone(false)
                return@launch
            }
            val match =
                allContacts.value.firstOrNull {
                    normalizePhoneKey(it.number) == key && it.deviceContactId > 0L
                }
            if (match == null) {
                onDone(false)
                return@launch
            }
            val ok = contactsRepository.setDeviceContactStarred(match.deviceContactId, starred = true)
            if (ok) {
                try {
                    contactsRepository.syncDeviceContacts()
                } catch (_: Exception) {
                }
            }
            onDone(ok)
        }
    }

    /** All call-log rows whose number matches [rawNumber] after normalization (handles +91 vs 0… vs spaces). */
    fun callsMatchingNumber(rawNumber: String): Flow<List<CallLogEntity>> =
        allCalls
            .map { calls ->
                val key = normalizePhoneKey(rawNumber)
                if (key.isEmpty()) return@map emptyList()
                calls
                    .filter { normalizePhoneKey(it.number) == key }
                    .sortedByDescending { it.timestamp }
            }
            .flowOn(Dispatchers.Default)

    /** All call-history rows matching any PSTN normalized key from a multi-number contact detail. */
    fun callsMatchingNormalizedKeys(normalizedKeys: Set<String>): Flow<List<CallLogEntity>> =
        allCalls
            .map { calls ->
                if (normalizedKeys.isEmpty()) emptyList()
                else
                    calls
                        .filter {
                            normalizePhoneKey(it.number) in normalizedKeys
                        }.sortedByDescending { it.timestamp }
            }
            .flowOn(Dispatchers.Default)

    class Factory(
        private val callLogRepo: CallLogRepository,
        private val contactsRepo: ContactsRepository,
        private val smsRepo: SmsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PhoneViewModel(callLogRepo, contactsRepo, smsRepo) as T
    }
}

private fun repeatCallerLikelySpam(calls: List<CallLogEntity>, normalizedKey: String): Boolean {
    if (normalizedKey.isEmpty()) return false
    val weekMs = 7L * 24 * 60 * 60 * 1000
    val since = System.currentTimeMillis() - weekMs
    val n =
        calls.count {
            normalizePhoneKey(it.number) == normalizedKey &&
                it.timestamp >= since &&
                it.callChannel == "PSTN"
        }
    return n >= 4
}
