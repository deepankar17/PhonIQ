package com.phoniq.app.telecom

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log

/**
 * Handles [Intent.ACTION_RESPOND_VIA_MESSAGE] for quick responses from other apps.
 * Required manifest component for default SMS app eligibility.
 */
class PhonIQRespondViaSmsService : Service() {

    companion object {
        private const val TAG = "PhonIQRespondViaSms"
        private const val ACTION_RESPOND_VIA_MESSAGE = "android.intent.action.RESPOND_VIA_MESSAGE"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RESPOND_VIA_MESSAGE) {
            val dest = intent.data?.schemeSpecificPart?.trim().orEmpty()
            val message =
                intent.getStringExtra(Intent.EXTRA_TEXT)
                    ?: intent.getStringExtra("android.intent.extra.TEXT")
                    ?: ""
            if (dest.isNotEmpty() && message.isNotEmpty()) {
                try {
                    val sms = smsManager()
                    sms.sendTextMessage(dest, null, message, null, null)
                } catch (e: Exception) {
                    Log.e(TAG, "sendText failed", e)
                }
            }
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    private fun smsManager(): SmsManager =
        applicationContext.getSystemService(SmsManager::class.java)}
