package com.phoniq.app.telecom

import android.telecom.Call
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CallState { RINGING, DIALING, ACTIVE, HOLDING, DISCONNECTED }

/** Audio route abstraction over [android.telecom.CallAudioState] routes. */
enum class CallAudioRoute { EARPIECE, SPEAKER, BLUETOOTH, WIRED_HEADSET }

/** [PhonIQInCallService] registers to process start/stop immediately when the user taps Record. */
fun interface CallRecordingCommandSink {
    fun flushRecordingCommands()
}

/**
 * Bridge for [PhonIQInCallService] to receive in-call control requests from the Compose UI
 * (mute toggle, audio-route switch, hold). Decoupled so [CallStateRepository] stays a
 * process-singleton with no Telecom dependency on previews/tests.
 */
interface CallControlsSink {
    fun applyMuted(muted: Boolean)
    fun applyAudioRoute(route: CallAudioRoute)
    fun applyHold(hold: Boolean)
}

data class ActiveCallInfo(
    val callerNumber: String,
    val callerName: String,
    val state: CallState,
    /** Android [ContactsContract.Contacts._ID] when resolved via PhoneLookup (0 if unknown). */
    val deviceContactId: Long = 0L,
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

    /** True while [CallRecorder] is capturing audio for the current PSTN session. */
    private val _isCallRecording = MutableStateFlow(false)
    val isCallRecording: StateFlow<Boolean> = _isCallRecording.asStateFlow()

    /** Path to the last encrypted recording, or null if none. */
    private val _lastRecordingPath = MutableStateFlow<String?>(null)
    val lastRecordingPath: StateFlow<String?> = _lastRecordingPath.asStateFlow()

    private val pendingStopRecording = AtomicBoolean(false)
    private val pendingStartRecording = AtomicBoolean(false)
    private var recordingCommandSink: CallRecordingCommandSink? = null

    /** Real Telecom-backed sink registered by [PhonIQInCallService]. */
    private var controlsSink: CallControlsSink? = null

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _audioRoute = MutableStateFlow(CallAudioRoute.EARPIECE)
    val audioRoute: StateFlow<CallAudioRoute> = _audioRoute.asStateFlow()

    /** Bluetooth / wired-headset present so UI can dim/enable the speaker affordance. */
    private val _hasBluetoothRoute = MutableStateFlow(false)
    val hasBluetoothRoute: StateFlow<Boolean> = _hasBluetoothRoute.asStateFlow()

    fun registerControlsSink(sink: CallControlsSink?) {
        controlsSink = sink
        if (sink == null) {
            _isMuted.value = false
            _audioRoute.value = CallAudioRoute.EARPIECE
            _hasBluetoothRoute.value = false
        }
    }

    /** Called from the InCallService callback. */
    fun reportMutedState(muted: Boolean) {
        _isMuted.value = muted
    }

    fun reportAudioRoute(route: CallAudioRoute, hasBluetooth: Boolean) {
        _audioRoute.value = route
        _hasBluetoothRoute.value = hasBluetooth
    }

    fun requestToggleMute() {
        controlsSink?.applyMuted(!_isMuted.value)
    }

    fun requestToggleSpeaker() {
        val next =
            if (_audioRoute.value == CallAudioRoute.SPEAKER) CallAudioRoute.EARPIECE
            else CallAudioRoute.SPEAKER
        controlsSink?.applyAudioRoute(next)
    }

    fun requestHold(hold: Boolean) {
        controlsSink?.applyHold(hold)
    }

    fun registerRecordingCommandSink(sink: CallRecordingCommandSink?) {
        recordingCommandSink = sink
    }

    fun requestStopCallRecording() {
        pendingStopRecording.set(true)
        recordingCommandSink?.flushRecordingCommands()
    }

    fun requestStartCallRecording() {
        pendingStartRecording.set(true)
        recordingCommandSink?.flushRecordingCommands()
    }

    internal fun takePendingStopRecording(): Boolean = pendingStopRecording.getAndSet(false)

    internal fun takePendingStartRecording(): Boolean = pendingStartRecording.getAndSet(false)

    fun setCallRecordingActive(active: Boolean) {
        _isCallRecording.value = active
    }

    fun setLastRecordingPath(path: String?) {
        _lastRecordingPath.value = path
    }

    fun update(info: ActiveCallInfo?) {
        _callInfo.value = info
        if (info == null) {
            _isCallRecording.value = false
        }
    }

    fun hangUp() {
        _callInfo.value?.call?.disconnect()
        _callInfo.value = null
        _isCallRecording.value = false
    }

    fun answer() {
        _callInfo.value?.call?.answer(0)
    }

    fun reject() {
        _callInfo.value?.call?.reject(false, null)
        _callInfo.value = null
        _isCallRecording.value = false
    }
}
