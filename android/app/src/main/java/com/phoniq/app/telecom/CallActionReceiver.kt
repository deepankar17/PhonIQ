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
            IncomingCallNotification.ACTION_ANSWER -> CallStateRepository.answer()
            IncomingCallNotification.ACTION_REJECT -> {
                CallStateRepository.reject()
                IncomingCallNotification.dismiss(context)
            }
        }
    }
}
