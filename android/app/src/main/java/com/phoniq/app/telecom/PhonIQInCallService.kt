package com.phoniq.app.telecom

import android.telecom.Call
import android.telecom.InCallService

/**
 * Registered as the InCallService so the system routes call events here
 * when PhonIQ is set as the default phone app.
 *
 * Updates [CallStateRepository] which the Compose UI observes via Flow.
 */
class PhonIQInCallService : InCallService() {

    // Callback per active call — keep references to unregister on removal
    private val callbacks = mutableMapOf<Call, Call.Callback>()

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        pushState(call)

        val cb = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
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
        // If no more calls, clear state
        if (calls.isEmpty()) CallStateRepository.update(null)
    }

    // -----------------------------------------------------------------------

    private fun pushState(call: Call) {
        val number = call.details.handle?.schemeSpecificPart
            ?: call.details.gatewayInfo?.originalAddress?.schemeSpecificPart
            ?: "Unknown"
        val name = call.details.callerDisplayName?.takeIf { it.isNotBlank() } ?: number
        val state = when (call.state) {
            Call.STATE_RINGING -> CallState.RINGING
            Call.STATE_DIALING, Call.STATE_CONNECTING -> CallState.DIALING
            Call.STATE_ACTIVE -> CallState.ACTIVE
            Call.STATE_HOLDING -> CallState.HOLDING
            else -> CallState.DISCONNECTED
        }
        CallStateRepository.update(
            ActiveCallInfo(callerNumber = number, callerName = name, state = state, call = call)
        )
    }
}
