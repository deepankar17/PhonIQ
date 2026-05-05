package com.phoniq.app.ui.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phoniq.app.data.db.entity.CallLogEntity
import com.phoniq.app.data.db.entity.ContactEntity
import com.phoniq.app.data.repository.CallLogRepository
import com.phoniq.app.data.repository.ContactsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PhoneViewModel(
    private val callLogRepository: CallLogRepository,
    private val contactsRepository: ContactsRepository,
) : ViewModel() {

    val allCalls: StateFlow<List<CallLogEntity>> = callLogRepository.allCalls
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allContacts: StateFlow<List<ContactEntity>> = contactsRepository.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val contactSearchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val filteredContacts: StateFlow<List<ContactEntity>> = contactSearchQuery
        .debounce(250)
        .flatMapLatest { q ->
            if (q.isBlank()) contactsRepository.allContacts
            else contactsRepository.search(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setContactSearch(q: String) { contactSearchQuery.value = q }

    fun syncFromDevice() {
        viewModelScope.launch {
            runCatching { callLogRepository.syncDeviceCallLog() }
            runCatching { contactsRepository.syncDeviceContacts() }
        }
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

    class Factory(
        private val callLogRepo: CallLogRepository,
        private val contactsRepo: ContactsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PhoneViewModel(callLogRepo, contactsRepo) as T
    }
}
