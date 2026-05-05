package com.phoniq.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.phoniq.app.R

private const val CHANNEL_ID = "phoniq_budget"

/**
 * Posts an over-budget alert notification.
 * Called after a new debit transaction is parsed from SMS if spending exceeds
 * the user-set monthly limit for that category.
 */
object OverBudgetNotifier {

    fun notify(context: Context, category: String, spent: Double, limit: Double) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(nm)

        val overage = spent - limit
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_phoniq_launcher)
            .setContentTitle("⚠️ Over budget: $category")
            .setContentText("Spent ₹${spent.toInt()} · limit ₹${limit.toInt()} · over by ₹${overage.toInt()}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        nm.notify(CHANNEL_ID.hashCode() + category.hashCode(), notification)
    }

    private fun ensureChannel(nm: NotificationManager) {
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Budget alerts", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Notifies when monthly spending exceeds your set budget limit" }
        )
    }
}
