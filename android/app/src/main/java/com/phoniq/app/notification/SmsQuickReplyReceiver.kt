package com.phoniq.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.app.Person
import com.phoniq.app.PhonIQApp
import com.phoniq.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives the user's inline reply from a [SmsIncomingNotifier] notification, sends the SMS via
 * [com.phoniq.app.data.repository.SmsRepository.sendSms], then updates the notification body so
 * the reply appears alongside the original message until it is dismissed.
 */
class SmsQuickReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
        val sender = intent.getStringExtra(EXTRA_SENDER) ?: return
        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val originalBody = intent.getStringExtra(EXTRA_ORIGINAL_BODY).orEmpty()
        val originalTs = intent.getLongExtra(EXTRA_ORIGINAL_TS, System.currentTimeMillis())

        val replyText =
            RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_TEXT_REPLY)?.toString()?.trim()
        if (replyText.isNullOrEmpty()) return

        val app = context.applicationContext as? PhonIQApp ?: return
        val nm = NotificationManagerCompat.from(context)

        // Synchronous-ish: dispatch send via repository; update notification immediately to show the reply.
        val person = Person.Builder().setName(sender).build()
        val style =
            NotificationCompat.MessagingStyle(Person.Builder().setName(context.getString(R.string.notif_self_label)).build())
                .addMessage(originalBody, originalTs, person)
                .addMessage(replyText, System.currentTimeMillis(), null as Person?)

        val updated =
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(  R.mipmap.ic_phoniq_launcher)
                .setStyle(style)
                .setContentTitle(title)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .build()

        if (notifId >= 0) {
            nm.notify(notifId, updated)
        }

        CoroutineScope(Dispatchers.IO).launch {
            val result = app.smsRepository.sendSms(sender, replyText)
            if (!result.success && notifId >= 0) {
                val failedBuilder =
                    NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(  R.mipmap.ic_phoniq_launcher)
                        .setContentTitle(title)
                        .setContentText(
                            context.getString(
                                R.string.thread_send_failed,
                                result.errorMessage ?: "unknown",
                            ),
                        )
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .build()
                nm.notify(notifId, failedBuilder)
            }
        }
    }

    companion object {
        const val ACTION_REPLY = "com.phoniq.app.notification.ACTION_SMS_REPLY"
        const val KEY_TEXT_REPLY = "phoniq_sms_reply_text"
        const val EXTRA_NOTIF_ID = "phoniq_sms_notif_id"
        const val EXTRA_SENDER = "phoniq_sms_sender"
        const val EXTRA_CHANNEL_ID = "phoniq_sms_channel"
        const val EXTRA_TITLE = "phoniq_sms_title"
        const val EXTRA_ORIGINAL_BODY = "phoniq_sms_original_body"
        const val EXTRA_ORIGINAL_TS = "phoniq_sms_original_ts"
    }
}
