package com.phoniq.app.telecom

import android.telecom.Call
import android.telecom.InCallService

/**
 * Registered as the InCallService so the system routes call events here
 * when PhonIQ is set as the default phone app.
 *
 * Updates [CallStateRepository] which the Compose UI observes via Flow.
 * Starts/stops [CallRecorder] when the call becomes active, when the user
 * taps Record from in-call UI (see [com.phoniq.app.ui.phone.InCallScreen]), and on disconnect.
 */
class PhonIQInCallService : InCallService(), CallRecordingCommandSink {

    private val callbacks = mutableMapOf<Call, Call.Callback>()
    private val recorder by lazy { CallRecorder(applicationContext) }
    /** User stopped recording from the in-call UI; blocks auto-start until they start again. */
    private var userOptedOutThisSession = false

    override fun onCreate() {
        super.onCreate()
        CallStateRepository.registerRecordingCommandSink(this)
    }

    override fun onDestroy() {
        CallStateRepository.registerRecordingCommandSink(null)
        super.onDestroy()
    }

    override fun flushRecordingCommands() {
        applyPendingRecordingControls()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        if (calls.size <= 1) {
            userOptedOutThisSession = false
        }
        pushState(call)

        val cb =
            object : Call.Callback() {
                override fun onStateChanged(call: Call, state: Int) {
                    val mapped = mapState(state)
                    applyPendingRecordingControls()
                    if (mapped == CallState.DISCONNECTED && recorder.isRecording) {
                        val path = recorder.stopRecording()
                        CallStateRepository.setLastRecordingPath(path)
                        CallStateRepository.setCallRecordingActive(false)
                    } else if (mapped == CallState.ACTIVE &&
                        CallRecordingPreferences.isEnabled(this@PhonIQInCallService) &&
                        !userOptedOutThisSession &&
                        !recorder.isRecording
                    ) {
                        if (recorder.startRecording()) {
                            CallStateRepository.setCallRecordingActive(true)
                        }
                    }
                    pushState(call)
                }

                override fun onDetailsChanged(call: Call, details: Call.Details) {
                    pushState(call)
                }
            }
        callbacks[call] = cb
        call.registerCallback(cb)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        callbacks.remove(call)?.let { call.unregisterCallback(it) }
        if (recorder.isRecording) {
            val path = recorder.stopRecording()
            CallStateRepository.setLastRecordingPath(path)
            CallStateRepository.setCallRecordingActive(false)
        }
        if (calls.isEmpty()) CallStateRepository.update(null)
    }

    private fun applyPendingRecordingControls() {
        if (CallStateRepository.takePendingStopRecording()) {
            if (recorder.isRecording) {
                val path = recorder.stopRecording()
                CallStateRepository.setLastRecordingPath(path)
                CallStateRepository.setCallRecordingActive(false)
            }
            userOptedOutThisSession = true
        }
        if (CallStateRepository.takePendingStartRecording()) {
            userOptedOutThisSession = false
            val hasActive = calls.any { mapState(it.details.state) == CallState.ACTIVE }
            if (CallRecordingPreferences.isEnabled(this) &&
                hasActive &&
                !recorder.isRecording
            ) {
                if (recorder.startRecording()) {
                    CallStateRepository.setCallRecordingActive(true)
                }
            }
        }
    }

    private fun pushState(call: Call) {
        val number =
            call.details.handle?.schemeSpecificPart
                ?: call.details.gatewayInfo?.originalAddress?.schemeSpecificPart
                ?: "Unknown"
        val name = call.details.callerDisplayName?.takeIf { it.isNotBlank() } ?: number
        val state = mapState(call.details.state)
        CallStateRepository.update(
            ActiveCallInfo(callerNumber = number, callerName = name, state = state, call = call),
        )
    }

    private fun mapState(state: Int): CallState =
        when (state) {
            Call.STATE_RINGING -> CallState.RINGING
            Call.STATE_DIALING, Call.STATE_CONNECTING -> CallState.DIALING
            Call.STATE_ACTIVE -> CallState.ACTIVE
            Call.STATE_HOLDING -> CallState.HOLDING
            else -> CallState.DISCONNECTED
        }
}
