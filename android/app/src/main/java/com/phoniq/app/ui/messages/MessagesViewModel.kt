package com.phoniq.app.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phoniq.app.data.db.entity.SmsMessageEntity
import com.phoniq.app.data.mapper.toMessageThreads
import com.phoniq.app.data.model.MessageThread
import com.phoniq.app.data.repository.SmsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MessagesViewModel(
    private val smsRepository: SmsRepository,
) : ViewModel() {

    val unreadCount: StateFlow<Int> = smsRepository.unreadCount
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val allMessages: StateFlow<List<SmsMessageEntity>> = smsRepository.allMessages
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Inbox threads derived off the main thread (avoids ANRs on large inboxes).
     *
     * Tech debt: this still materializes the **full** thread list whenever [allMessages] changes;
     * [com.phoniq.app.ui.messages.MessagesScreen] only composes a windowed slice for the LazyColumn.
     * True query-level paging would cap memory for very large inboxes (Room + Paging or LIMIT queries).
     */
    val messageThreads: StateFlow<List<MessageThread>> =
        (allMessages as Flow<List<SmsMessageEntity>>)
            .distinctUntilChanged()
            .map { it.toMessageThreads() }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val otpMessages: StateFlow<List<SmsMessageEntity>> = smsRepository.otpMessages
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val transactionMessages: StateFlow<List<SmsMessageEntity>> = smsRepository.transactionMessages
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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

    suspend fun markThreadRead(threadId: String) {
        smsRepository.markThreadRead(threadId)
    }

    fun setThreadPinned(threadId: String, pinned: Boolean) {
        viewModelScope.launch { smsRepository.setThreadPinned(threadId, pinned) }
    }

    fun setThreadArchived(threadId: String, archived: Boolean) {
        viewModelScope.launch { smsRepository.setThreadArchived(threadId, archived) }
    }

    fun syncFromDevice() {
        viewModelScope.launch { refreshFromDevice() }
    }

    suspend fun refreshFromDevice() {
        try {
            smsRepository.syncDeviceSms()
        } catch (_: Exception) {
        }
    }

    fun threadMessages(threadId: String): Flow<List<SmsMessageEntity>> =
        smsRepository.thread(threadId)

    suspend fun loadThreadLatestPage(threadId: String, limit: Int = SMS_THREAD_PAGE_SIZE): List<SmsMessageEntity> =
        smsRepository.getThreadLatestPageChronological(threadId, limit)

    suspend fun loadThreadOlderPage(
        threadId: String,
        beforeTimestamp: Long,
        beforeId: Long,
        limit: Int = SMS_THREAD_PAGE_SIZE,
    ): List<SmsMessageEntity> =
        smsRepository.getThreadOlderPageChronological(threadId, beforeTimestamp, beforeId, limit)

    companion object {
        const val SMS_THREAD_PAGE_SIZE = 20
    }

    class Factory(private val repo: SmsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MessagesViewModel(repo) as T
    }
}
