package com.phoniq.app.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Handles Answer / Reject taps from the incoming-call notification action buttons.
 */
class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            IncomingCallNotification.ACTION_ANSWER -> {
                CallStateRepository.answer()
                IncomingCallNotification.dismiss(context)
                CallOverlayActivity.launch(context)
            }
            IncomingCallNotification.ACTION_REJECT -> {
                when (CallStateRepository.callInfo.value?.state) {
                    CallState.RINGING -> CallStateRepository.reject()
                    else -> CallStateRepository.hangUp()
                }
                IncomingCallNotification.dismiss(context)
            }
        }
    }
}
