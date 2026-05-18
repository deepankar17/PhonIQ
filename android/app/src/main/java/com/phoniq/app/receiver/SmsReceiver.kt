package com.phoniq.app.receiver

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.phoniq.app.PhonIQApp
import com.phoniq.app.data.db.entity.SmsMessageEntity
import com.phoniq.app.domain.sms.SmsParser
import com.phoniq.app.notification.SmsIncomingNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives [Telephony.Sms.Intents.SMS_DELIVER_ACTION] when PhonIQ is the default SMS app
 * (must write to the SMS provider), and [Telephony.Sms.Intents.SMS_RECEIVED_ACTION] when not.
 */
class SmsReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Telephony.Sms.Intents.SMS_DELIVER_ACTION -> deliverToProviderThenSync(context, intent)
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> onSmsReceivedLegacy(context, intent)
            else -> Unit
        }
    }

    private fun deliverToProviderThenSync(context: Context, intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return
        val sender = messages.first().displayOriginatingAddress ?: messages.first().originatingAddress ?: return
        val body =
            buildString {
                for (sms in messages) {
                    append(sms.displayMessageBody ?: sms.messageBody ?: "")
                }
            }.trim().ifEmpty { return }
        val timestamp = messages.first().timestampMillis

        val values =
            ContentValues().apply {
                put(Telephony.Sms.ADDRESS, sender)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, timestamp)
                put(Telephony.Sms.DATE_SENT, timestamp)
                put(Telephony.Sms.READ, 0)
                put(Telephony.Sms.SEEN, 0)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
            }
        runCatching {
            context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
        }

        val app = context.applicationContext as PhonIQApp
        val parseResult = app.smsParser.parse(sender, body)
        SmsIncomingNotifier.maybeShow(
            context = context.applicationContext,
            sender = sender,
            body = body,
            category = parseResult.category,
            threadId = sender,
        )
        scope.launch {
            runCatching { app.smsRepository.syncDeviceSms(throttleReceiverBurst = true) }
        }
    }

    private fun onSmsReceivedLegacy(context: Context, intent: Intent) {
        val defaultPkg = Telephony.Sms.getDefaultSmsPackage(context)
        if (defaultPkg == context.packageName) {
            // Default SMS role uses SMS_DELIVER + provider sync; avoid duplicate Room rows.
            return
        }
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
                val entity =
                    SmsMessageEntity(
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
                SmsIncomingNotifier.maybeShow(
                    context = context.applicationContext,
                    sender = sender,
                    body = body,
                    category = parseResult.category,
                    threadId = threadId,
                )
            }
        }
    }
}
