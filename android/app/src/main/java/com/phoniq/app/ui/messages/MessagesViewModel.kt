package com.phoniq.app.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phoniq.app.data.db.entity.SmsMessageEntity
import com.phoniq.app.data.repository.SmsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MessagesViewModel(
    private val smsRepository: SmsRepository,
) : ViewModel() {

    val unreadCount: StateFlow<Int> = smsRepository.unreadCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val allMessages: StateFlow<List<SmsMessageEntity>> = smsRepository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val otpMessages: StateFlow<List<SmsMessageEntity>> = smsRepository.otpMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val transactionMessages: StateFlow<List<SmsMessageEntity>> = smsRepository.transactionMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: StateFlow<List<SmsMessageEntity>> = searchQuery
        .debounce(300)
        .flatMapLatest { q ->
            if (q.isBlank()) smsRepository.allMessages
            else smsRepository.search(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearch(q: String) { searchQuery.value = q }

    fun markAllRead() {
        viewModelScope.launch { smsRepository.markAllRead() }
    }

    fun syncFromDevice() {
        viewModelScope.launch {
            runCatching { smsRepository.syncDeviceSms() }
        }
    }

    class Factory(private val repo: SmsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MessagesViewModel(repo) as T
    }
}
