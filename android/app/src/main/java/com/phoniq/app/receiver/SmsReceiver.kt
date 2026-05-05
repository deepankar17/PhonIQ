package com.phoniq.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.phoniq.app.PhonIQApp
import com.phoniq.app.data.db.entity.SmsMessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives incoming SMS messages via RECEIVE_SMS broadcast.
 * Parses each message through SmsParser and inserts into Room.
 * The coroutine scope outlives the BroadcastReceiver lifecycle (intentional — short-lived work).
 */
class SmsReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val app = context.applicationContext as PhonIQApp

        scope.launch {
            for (sms in messages) {
                val sender = sms.displayOriginatingAddress ?: sms.originatingAddress ?: continue
                val body = sms.displayMessageBody ?: sms.messageBody ?: continue
                val timestamp = sms.timestampMillis
                val threadId = sender

                val parseResult = app.smsParser.parse(sender, body)
                val entity = SmsMessageEntity(
                    sender = sender,
                    body = body,
                    timestamp = timestamp,
                    category = parseResult.category.name,
                    threadId = threadId,
                    isTransaction = parseResult.category.name == "TRANSACTION",
                    isOtp = parseResult.category.name == "OTP",
                    isSpam = parseResult.category.name == "SPAM",
                    isRead = false,
                )
                app.database.smsDao().insert(entity)
            }
        }
    }
}
