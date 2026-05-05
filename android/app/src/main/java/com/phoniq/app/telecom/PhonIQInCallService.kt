package com.phoniq.app.telecom

import android.telecom.Call
import android.telecom.InCallService

/**
 * Registered as the InCallService so the system routes call events here
 * when PhonIQ is set as the default phone app.
 *
 * Updates [CallStateRepository] which the Compose UI observes via Flow.
 * Also starts/stops [CallRecorder] on call state transitions.
 */
class PhonIQInCallService : InCallService() {

    private val callbacks = mutableMapOf<Call, Call.Callback>()
    private val recorder by lazy { CallRecorder(applicationContext) }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        pushState(call)

        val cb = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                val mapped = mapState(state)
                if (mapped == CallState.ACTIVE && !recorder.isRecording) {
                    recorder.startRecording()
                }
                if (mapped == CallState.DISCONNECTED && recorder.isRecording) {
                    val path = recorder.stopRecording()
                    CallStateRepository.setLastRecordingPath(path)
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
        }
        if (calls.isEmpty()) CallStateRepository.update(null)
    }

    // -----------------------------------------------------------------------

    private fun pushState(call: Call) {
        val number = call.details.handle?.schemeSpecificPart
            ?: call.details.gatewayInfo?.originalAddress?.schemeSpecificPart
            ?: "Unknown"
        val name = call.details.callerDisplayName?.takeIf { it.isNotBlank() } ?: number
        val state = mapState(call.state)
        CallStateRepository.update(
            ActiveCallInfo(callerNumber = number, callerName = name, state = state, call = call)
        )
    }

    private fun mapState(state: Int): CallState = when (state) {
        Call.STATE_RINGING -> CallState.RINGING
        Call.STATE_DIALING, Call.STATE_CONNECTING -> CallState.DIALING
        Call.STATE_ACTIVE -> CallState.ACTIVE
        Call.STATE_HOLDING -> CallState.HOLDING
        else -> CallState.DISCONNECTED
    }
}
