package com.phoniq.app.telecom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.media.AudioManager
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.phoniq.app.R
import com.phoniq.app.notification.NotificationPermissionHelper
import com.phoniq.app.util.ContactPoliciesStore

private const val CHANNEL_ID = "phoniq_calls"

/**
 * Posts the system incoming-call notification with full-screen intent and CallStyle (API 31+).
 * [PhonIQInCallService] promotes this to a foreground notification while a call is active.
 */
object IncomingCallNotification {

    const val CALL_NOTIF_ID = 1001

    fun show(
        context: Context,
        callerName: String,
        callerNumber: String,
        deviceContactId: Long = 0L,
        policy: ContactPoliciesStore.State = ContactPoliciesStore.State(),
    ): Notification {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(nm)

        val notification = buildIncoming(context, callerName, callerNumber, deviceContactId, policy)
        nm.notify(CALL_NOTIF_ID, notification)
        return notification
    }

    fun buildIncoming(
        context: Context,
        callerName: String,
        callerNumber: String,
        deviceContactId: Long = 0L,
        policy: ContactPoliciesStore.State = ContactPoliciesStore.State(),
    ): Notification {
        val fullScreenPi = fullScreenPendingIntent(context)
        val answerPi = buildActionIntent(context, ACTION_ANSWER)
        val rejectPi = buildActionIntent(context, ACTION_REJECT)

        val caller =
            callerPerson(context, callerName, deviceContactId)

        val builder =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(  R.mipmap.ic_phoniq_launcher)
                .setContentTitle(callerName)
                .setContentText(callerNumber)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(fullScreenPi)

        if (NotificationPermissionHelper.canUseFullScreenIntent(context)) {
            builder.setFullScreenIntent(fullScreenPi, true)
        }

        applyIncomingRingPolicy(builder, policy)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setStyle(NotificationCompat.CallStyle.forIncomingCall(caller, rejectPi, answerPi))
        } else {
            builder
                .addAction(android.R.drawable.ic_menu_call, context.getString(R.string.notif_call_answer), answerPi)
                .addAction(android.R.drawable.ic_delete, context.getString(R.string.notif_call_decline), rejectPi)
        }

        return builder.build()
    }

    fun buildOngoing(
        context: Context,
        callerName: String,
        callerNumber: String,
        deviceContactId: Long = 0L,
    ): Notification {
        val fullScreenPi = fullScreenPendingIntent(context)
        val hangUpPi = buildActionIntent(context, ACTION_REJECT)

        val caller =
            callerPerson(context, callerName, deviceContactId)

        val builder =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(  R.mipmap.ic_phoniq_launcher)
                .setContentTitle(callerName)
                .setContentText(callerNumber)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setContentIntent(fullScreenPi)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setStyle(NotificationCompat.CallStyle.forOngoingCall(caller, hangUpPi))
        }

        return builder.build()
    }

    fun dismiss(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(CALL_NOTIF_ID)
    }

    private fun fullScreenPendingIntent(context: Context): PendingIntent {
        val intent =
            Intent(context, CallOverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_SHOW_CALL, true)
            }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel(nm: NotificationManager) {
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Incoming calls",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "PhonIQ incoming and active call alerts"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
        nm.createNotificationChannel(channel)
    }

    private fun applyIncomingRingPolicy(
        builder: NotificationCompat.Builder,
        policy: ContactPoliciesStore.State,
    ) {
        if (!policy.ringNormally) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setSilent(true)
            }
            builder.setPriority(NotificationCompat.PRIORITY_LOW)
        } else if (policy.customRingtoneEnabled && policy.customRingtoneUri.isNotBlank()) {
            runCatching {
                val u = Uri.parse(policy.customRingtoneUri)
                builder.setSound(u, AudioManager.STREAM_RING)
            }
        }
    }

    private fun callerPerson(context: Context, callerName: String, deviceContactId: Long): Person {
        val b =
            Person.Builder()
                .setName(callerName)
                .setImportant(true)
        if (deviceContactId > 0L) {
            val photoUri =
                Uri.withAppendedPath(
                    ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, deviceContactId),
                    ContactsContract.Contacts.Photo.CONTENT_DIRECTORY,
                )
            try {
                b.setIcon(IconCompat.createWithContentUri(photoUri))
            } catch (_: Exception) {
                // Icon may fail if contact has no photo — Person still shows name.
            }
        }
        return b.build()
    }

    private fun buildActionIntent(context: Context, action: String): PendingIntent {
        val intent =
            Intent(context, CallActionReceiver::class.java).apply {
                this.action = action
            }
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    const val EXTRA_SHOW_CALL = "phoniq_show_call"
    const val ACTION_ANSWER = "com.phoniq.app.ACTION_ANSWER"
    const val ACTION_REJECT = "com.phoniq.app.ACTION_REJECT"
}
