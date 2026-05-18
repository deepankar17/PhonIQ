package com.phoniq.app.telecom

import android.content.pm.ServiceInfo
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.phoniq.app.notification.NotificationPermissionHelper
import com.phoniq.app.util.lookupContactDisplayNameForDialableNumber
import com.phoniq.app.util.lookupContactIdForDialableNumber
import com.phoniq.app.util.ContactPoliciesStore
import com.phoniq.app.util.shouldAutoRejectIncoming

/**
 * Registered as the InCallService so the system routes call events here
 * when PhonIQ is set as the default phone app.
 *
 * Updates [CallStateRepository] which the Compose UI observes via Flow.
 * Posts a foreground call notification (heads-up / full-screen) while ringing or active.
 */
class PhonIQInCallService : InCallService(), CallRecordingCommandSink, CallControlsSink {

    private val callbacks = mutableMapOf<Call, Call.Callback>()
    private val recorder by lazy { CallRecorder(applicationContext) }
    /** User stopped recording from the in-call UI; blocks auto-start until they start again. */
    private var userOptedOutThisSession = false

    override fun onCreate() {
        super.onCreate()
        CallStateRepository.registerRecordingCommandSink(this)
        CallStateRepository.registerControlsSink(this)
    }

    override fun onDestroy() {
        CallStateRepository.registerRecordingCommandSink(null)
        CallStateRepository.registerControlsSink(null)
        stopCallForeground()
        super.onDestroy()
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        if (audioState == null) return
        val mapped =
            when (audioState.route) {
                CallAudioState.ROUTE_SPEAKER -> CallAudioRoute.SPEAKER
                CallAudioState.ROUTE_BLUETOOTH -> CallAudioRoute.BLUETOOTH
                CallAudioState.ROUTE_WIRED_HEADSET -> CallAudioRoute.WIRED_HEADSET
                else -> CallAudioRoute.EARPIECE
            }
        val hasBluetooth =
            (audioState.supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH) != 0
        CallStateRepository.reportAudioRoute(mapped, hasBluetooth)
        CallStateRepository.reportMutedState(audioState.isMuted)
    }

    override fun applyMuted(muted: Boolean) {
        setMuted(muted)
        CallStateRepository.reportMutedState(muted)
    }

    override fun applyAudioRoute(route: CallAudioRoute) {
        val telecomRoute =
            when (route) {
                CallAudioRoute.SPEAKER -> CallAudioState.ROUTE_SPEAKER
                CallAudioRoute.BLUETOOTH -> CallAudioState.ROUTE_BLUETOOTH
                CallAudioRoute.WIRED_HEADSET -> CallAudioState.ROUTE_WIRED_HEADSET
                CallAudioRoute.EARPIECE -> CallAudioState.ROUTE_EARPIECE
            }
        setAudioRoute(telecomRoute)
    }

    override fun applyHold(hold: Boolean) {
        val target =
            calls.firstOrNull { it.details.state == Call.STATE_ACTIVE || it.details.state == Call.STATE_HOLDING }
                ?: return
        if (hold) target.hold() else target.unhold()
    }

    override fun flushRecordingCommands() {
        applyPendingRecordingControls()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val numberEarly =
            call.details.handle?.schemeSpecificPart
                ?: call.details.gatewayInfo?.originalAddress?.schemeSpecificPart
                ?: ""
        if (numberEarly.isNotBlank() && applicationContext.shouldAutoRejectIncoming(numberEarly)) {
            call.disconnect()
            return
        }
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
        if (calls.isEmpty()) {
            CallStateRepository.update(null)
            stopCallForeground()
        }
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
        val deviceContactId = applicationContext.lookupContactIdForDialableNumber(number)
        val lookupName = applicationContext.lookupContactDisplayNameForDialableNumber(number)
        val telName = call.details.callerDisplayName?.trim()?.takeIf { it.isNotBlank() }
        val name =
            when {
                !lookupName.isNullOrBlank() -> lookupName
                !telName.isNullOrBlank() -> telName
                else -> number
            }
        val state = mapState(call.details.state)
        CallStateRepository.update(
            ActiveCallInfo(
                callerNumber = number,
                callerName = name,
                state = state,
                deviceContactId = deviceContactId,
                call = call,
            ),
        )
        syncCallNotification(name, number, state, deviceContactId)
    }

    private fun syncCallNotification(
        callerName: String,
        callerNumber: String,
        state: CallState,
        deviceContactId: Long,
    ) {
        val policy =
            applicationContext.run {
                ContactPoliciesStore.loadForIncomingCall(this, deviceContactId, callerNumber)
            }
        when (state) {
            CallState.RINGING -> {
                val notification =
                    IncomingCallNotification.show(
                        applicationContext,
                        callerName,
                        callerNumber,
                        deviceContactId,
                        policy,
                    )
                startCallForeground(notification)
                CallOverlayActivity.launch(applicationContext)
            }
            CallState.DIALING,
            CallState.ACTIVE,
            CallState.HOLDING,
            -> {
                val notification =
                    IncomingCallNotification.buildOngoing(
                        applicationContext,
                        callerName,
                        callerNumber,
                        deviceContactId,
                    )
                startCallForeground(notification)
            }
            CallState.DISCONNECTED -> {
                if (calls.isEmpty()) {
                    stopCallForeground()
                }
            }
        }
    }

    private fun startCallForeground(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                } else {
                    0
                }
            if (type != 0) {
                startForeground(IncomingCallNotification.CALL_NOTIF_ID, notification, type)
            } else {
                startForeground(IncomingCallNotification.CALL_NOTIF_ID, notification)
            }
        } else {
            @Suppress("DEPRECATION")
            startForeground(IncomingCallNotification.CALL_NOTIF_ID, notification)
        }
    }

    private fun stopCallForeground() {
        IncomingCallNotification.dismiss(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
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
