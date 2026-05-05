package com.phoniq.app.telecom

import android.telecom.Call
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CallState { RINGING, DIALING, ACTIVE, HOLDING, DISCONNECTED }

data class ActiveCallInfo(
    val callerNumber: String,
    val callerName: String,
    val state: CallState,
    /** The live Telecom Call handle — null in tests / preview. */
    val call: Call? = null,
)

/**
 * Process-singleton that bridges [PhonIQInCallService] (system callbacks)
 * with the Compose UI layer (collects via [callInfo] Flow).
 *
 * Also exposes the path of the most recent encrypted call recording so the
 * AfterCallSheet can offer playback.
 */
object CallStateRepository {

    private val _callInfo = MutableStateFlow<ActiveCallInfo?>(null)
    val callInfo: StateFlow<ActiveCallInfo?> = _callInfo.asStateFlow()

    /** Path to the last encrypted recording, or null if none. */
    private val _lastRecordingPath = MutableStateFlow<String?>(null)
    val lastRecordingPath: StateFlow<String?> = _lastRecordingPath.asStateFlow()

    fun setLastRecordingPath(path: String?) {
        _lastRecordingPath.value = path
    }

    fun update(info: ActiveCallInfo?) {
        _callInfo.value = info
    }

    fun hangUp() {
        _callInfo.value?.call?.disconnect()
        _callInfo.value = null
    }

    fun answer() {
        _callInfo.value?.call?.answer(0)
    }

    fun reject() {
        _callInfo.value?.call?.reject(false, null)
        _callInfo.value = null
    }
}
