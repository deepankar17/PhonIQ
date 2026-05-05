package com.phoniq.app.telecom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.phoniq.app.MainActivity
import com.phoniq.app.R

private const val CHANNEL_ID = "phoniq_calls"
private const val CALL_NOTIF_ID = 1001

/**
 * Posts or updates the system-style incoming call notification.
 * Uses CallStyle.forIncomingCall() (API 31+) to get the full-screen
 * accept/decline UI on lock-screen and heads-up.
 */
object IncomingCallNotification {

    fun show(context: Context, callerName: String, callerNumber: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(nm)

        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_SHOW_CALL, true)
        }
        val fullScreenPi = PendingIntent.getActivity(
            context, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val answerPi = buildActionIntent(context, ACTION_ANSWER)
        val rejectPi = buildActionIntent(context, ACTION_REJECT)

        val caller = Person.Builder()
            .setName(callerName)
            .setImportant(true)
            .build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_phoniq_launcher)
            .setContentTitle(callerName)
            .setContentText(callerNumber)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPi, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_menu_call, "Answer", answerPi)
            .addAction(android.R.drawable.ic_delete, "Decline", rejectPi)
            .setStyle(NotificationCompat.CallStyle.forIncomingCall(caller, rejectPi, answerPi))
            .build()

        nm.notify(CALL_NOTIF_ID, notification)
    }

    fun dismiss(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(CALL_NOTIF_ID)
    }

    private fun ensureChannel(nm: NotificationManager) {
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Incoming calls",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "PhonIQ incoming call alerts"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }

    private fun buildActionIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, CallActionReceiver::class.java).apply {
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
