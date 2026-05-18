package com.phoniq.app.ui.messages

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phoniq.app.data.db.entity.SmsMessageEntity
import com.phoniq.app.data.mapper.toMessageThreads
import com.phoniq.app.data.model.MessageThread
import com.phoniq.app.data.repository.CallLogRepository
import com.phoniq.app.data.repository.SmsRepository
import com.phoniq.app.util.dialableForBlockedNumberContract
import com.phoniq.app.util.smsSpamPeerKey
import com.phoniq.app.util.tryAddToSystemBlockedNumbers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MessagesViewModel(
    private val smsRepository: SmsRepository,
    private val callLogRepository: CallLogRepository,
    private val app: Application,
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
        combine(
            allMessages as Flow<List<SmsMessageEntity>>,
            callLogRepository.spamNumberKeys,
        ) { messages, spamKeys ->
            val threads = messages.toMessageThreads()
            threads.filter { thread ->
                val raw = thread.peerAddress?.trim()?.takeIf { it.isNotEmpty() } ?: thread.title
                val key = smsSpamPeerKey(raw)
                key.isEmpty() || key !in spamKeys
            }
        }
            .distinctUntilChanged()
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

    /**
     * Send an outgoing SMS to [destination] and refresh the local SMS cache so the new bubble
     * appears in the thread. Returns the [SmsRepository.SendResult] for UI to surface
     * snackbars / errors.
     */
    suspend     fun sendSms(destination: String, body: String, threadIdHint: String? = null): SmsRepository.SendResult {
        val result = smsRepository.sendSms(destination, body, threadIdHint)
        if (result.success) {
            refreshFromDevice()
        }
        return result
    }

    fun markThreadsReadBulk(threadIds: Collection<String>) {
        viewModelScope.launch { smsRepository.markThreadsRead(threadIds) }
    }

    fun setThreadsArchivedBulk(threadIds: Collection<String>, archived: Boolean) {
        viewModelScope.launch { smsRepository.setThreadsArchived(threadIds, archived) }
    }

    fun deleteThreadsBulk(threadIds: Collection<String>) {
        viewModelScope.launch {
            smsRepository.deleteThreadsPermanently(threadIds)
        }
    }

    /**
     * Adds the peer to the on-device [spam_numbers] list (hides thread) and attempts
     * [android.provider.BlockedNumberContract] when the peer is dialable.
     *
     * @param userMessage "(reason)" string resource name values are filled by the caller from UI layer
     */
    fun blockSmsSender(rawPeer: String, onResult: (BlockSmsSenderResult) -> Unit) {
        viewModelScope.launch {
            val peer = rawPeer.trim()
            if (peer.isEmpty()) {
                onResult(BlockSmsSenderResult.emptyPeer())
                return@launch
            }
            callLogRepository.markSpam(peer)
            val dialable = dialableForBlockedNumberContract(peer)
            val systemOk =
                dialable != null && app.applicationContext.tryAddToSystemBlockedNumbers(dialable)
            onResult(
                BlockSmsSenderResult(
                    addedLocalBlock = true,
                    attemptedSystemBlock = dialable != null,
                    systemBlockSucceeded = systemOk,
                ),
            )
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

    data class BlockSmsSenderResult(
        val addedLocalBlock: Boolean,
        val attemptedSystemBlock: Boolean,
        val systemBlockSucceeded: Boolean,
    ) {
        companion object {
            fun emptyPeer() =
                BlockSmsSenderResult(
                    addedLocalBlock = false,
                    attemptedSystemBlock = false,
                    systemBlockSucceeded = false,
                )
        }
    }

    class Factory(
        private val repo: SmsRepository,
        private val callLogRepository: CallLogRepository,
        private val app: Application,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MessagesViewModel(repo, callLogRepository, app) as T
    }
}
