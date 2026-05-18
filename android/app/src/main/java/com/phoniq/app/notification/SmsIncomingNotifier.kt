package com.phoniq.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.phoniq.app.MainActivity
import com.phoniq.app.MoneyNotifExtras
import com.phoniq.app.MoneyNotifMode
import com.phoniq.app.PhonIQApp
import com.phoniq.app.R
import com.phoniq.app.domain.sms.SmsParser
import com.phoniq.app.util.PersonalizationStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

private const val CHANNEL_OTP = "phoniq_sms_otp"
private const val CHANNEL_TXN = "phoniq_sms_txn"

/**
 * Heads-up alerts for high-signal SMS (OTP / bank transactions) when PhonIQ receives a message.
 */
object SmsIncomingNotifier {

    fun maybeShow(
        context: Context,
        sender: String,
        body: String,
        category: SmsParser.SmsCategory,
        threadId: String,
    ) {
        if (!NotificationPermissionHelper.canPostNotifications(context)) return
        when (category) {
            SmsParser.SmsCategory.OTP,
            SmsParser.SmsCategory.TRANSACTION,
            -> Unit
            else -> return
        }

        if (category == SmsParser.SmsCategory.OTP) {
            val snap = PersonalizationStore.load(context.applicationContext)
            if (snap.otpAutoCopy) {
                val otpCode = SmsParser().parse(sender, body).otp?.code
                if (!otpCode.isNullOrEmpty()) {
                    runCatching {
                        val cm = context.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("OTP", otpCode))
                    }
                }
            }
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId =
            when (category) {
                SmsParser.SmsCategory.OTP -> {
                    ensureChannel(nm, CHANNEL_OTP, context.getString(R.string.notif_channel_sms_otp), NotificationManager.IMPORTANCE_HIGH)
                    CHANNEL_OTP
                }
                else -> {
                    ensureChannel(nm, CHANNEL_TXN, context.getString(R.string.notif_channel_sms_txn), NotificationManager.IMPORTANCE_HIGH)
                    CHANNEL_TXN
                }
            }

        val title =
            when (category) {
                SmsParser.SmsCategory.OTP -> context.getString(R.string.notif_sms_otp_title, sender)
                SmsParser.SmsCategory.TRANSACTION -> context.getString(R.string.notif_sms_txn_title, sender)
                else -> sender
            }
        val preview = body.trim().lineSequence().firstOrNull()?.take(180) ?: body.take(180)

        val openIntent =
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_OPEN_THREAD_ID, threadId)
                putExtra(EXTRA_OPEN_MESSAGES_TAB, true)
            }
        val contentPi =
            PendingIntent.getActivity(
                context,
                threadId.hashCode(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notifId = NOTIF_BASE_ID + threadId.hashCode()

        val parsed = SmsParser().parse(sender, body)
        val txn = parsed.transaction

        val moneyInsights =
            if (category == SmsParser.SmsCategory.TRANSACTION && txn != null) {
                runCatching {
                    val app = context.applicationContext as PhonIQApp
                    val freshDebit =
                        if (txn.type == "DEBIT") txn.amount else 0.0
                    runBlocking(Dispatchers.IO) {
                        app.transactionRepository.loadTxnNotificationInsights(freshDebit)
                    }
                }.getOrNull()
            } else {
                null
            }

        val rich =
            if (category == SmsParser.SmsCategory.TRANSACTION && txn != null) {
                TransactionNotificationViews.build(context, sender, body, txn, moneyInsights)
            } else {
                null
            }
        val replyTitle = rich?.summaryTitle ?: title
        val replyAction = buildQuickReplyAction(context, sender, channelId, replyTitle, body, notifId)
        val moneyAction = if (rich != null) buildOpenMoneyAction(context, notifId) else null
        val statsAction = if (rich != null) buildStatsAction(context, notifId) else null
        val splitAction =
            if (rich != null && txn != null && txn.type == "DEBIT") {
                buildSplitAction(context, notifId, txn)
            } else {
                null
            }

        val notificationBuilder =
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_phoniq_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .setContentIntent(contentPi)

        if (rich != null) {
            notificationBuilder
                .setContentTitle(rich.summaryTitle)
                .setContentText(rich.summaryText)
                .setCustomContentView(rich.collapsed)
                .setCustomBigContentView(rich.expanded)
                .setCustomHeadsUpContentView(rich.collapsed)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
        } else {
            notificationBuilder
                .setContentTitle(title)
                .setContentText(preview)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body.take(500)))
        }

        val notification =
            notificationBuilder
                .apply {
                    moneyAction?.let { addAction(it) }
                    statsAction?.let { addAction(it) }
                    splitAction?.let { addAction(it) }
                    if (replyAction != null) addAction(replyAction)
                }
                .build()

        nm.notify(notifId, notification)
    }

    private fun moneyTabIntent(
        context: Context,
        notifMode: String?,
        splitAmount: Double? = null,
        splitMerchant: String? = null,
    ): Intent =
        Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_OPEN_MONEY_TAB, true)
            if (!notifMode.isNullOrEmpty()) {
                putExtra(EXTRA_MONEY_NOTIF_MODE, notifMode)
            }
            splitAmount?.let { putExtra(EXTRA_TXN_SPLIT_AMOUNT, it) }
            splitMerchant?.takeIf { it.isNotEmpty() }?.let { putExtra(EXTRA_TXN_SPLIT_MERCHANT, it) }
        }

    private fun buildOpenMoneyAction(context: Context, notifId: Int): NotificationCompat.Action {
        val intent = moneyTabIntent(context, notifMode = null)
        val pi =
            PendingIntent.getActivity(
                context,
                notifId + 31,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        return NotificationCompat.Action.Builder(
            R.mipmap.ic_phoniq_launcher,
            context.getString(R.string.notif_txn_action_money),
            pi,
        ).build()
    }

    private fun buildStatsAction(context: Context, notifId: Int): NotificationCompat.Action {
        val intent = moneyTabIntent(context, notifMode = "stats")
        val pi =
            PendingIntent.getActivity(
                context,
                notifId + 33,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        return NotificationCompat.Action.Builder(
            R.mipmap.ic_phoniq_launcher,
            context.getString(R.string.notif_txn_action_stats),
            pi,
        ).build()
    }

    private fun buildSplitAction(context: Context, notifId: Int, txn: SmsParser.TransactionResult): NotificationCompat.Action {
        val intent =
            moneyTabIntent(
                context,
                notifMode = "split",
                splitAmount = txn.amount.takeIf { txn.type == "DEBIT" },
                splitMerchant = txn.merchant?.trim().orEmpty().ifEmpty { txn.narrative?.trim() },
            )
        val pi =
            PendingIntent.getActivity(
                context,
                notifId + 35,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        return NotificationCompat.Action.Builder(
            R.mipmap.ic_phoniq_launcher,
            context.getString(R.string.notif_txn_action_split),
            pi,
        ).build()
    }

    /**
     * Build a RemoteInput "Reply" action for the SMS notification. Returns null when SEND_SMS
     * permission is not granted (so we don't show a useless action).
     */
    private fun buildQuickReplyAction(
        context: Context,
        sender: String,
        channelId: String,
        title: String,
        body: String,
        notifId: Int,
    ): NotificationCompat.Action? {
        val hasSend =
            android.content.pm.PackageManager.PERMISSION_GRANTED ==
                context.checkSelfPermission(android.Manifest.permission.SEND_SMS)
        if (!hasSend) return null

        val remoteInput =
            RemoteInput.Builder(SmsQuickReplyReceiver.KEY_TEXT_REPLY)
                .setLabel(context.getString(R.string.notif_sms_reply_hint))
                .build()
        val replyIntent =
            Intent(context, SmsQuickReplyReceiver::class.java).apply {
                action = SmsQuickReplyReceiver.ACTION_REPLY
                putExtra(SmsQuickReplyReceiver.EXTRA_NOTIF_ID, notifId)
                putExtra(SmsQuickReplyReceiver.EXTRA_SENDER, sender)
                putExtra(SmsQuickReplyReceiver.EXTRA_CHANNEL_ID, channelId)
                putExtra(SmsQuickReplyReceiver.EXTRA_TITLE, title)
                putExtra(SmsQuickReplyReceiver.EXTRA_ORIGINAL_BODY, body)
                putExtra(SmsQuickReplyReceiver.EXTRA_ORIGINAL_TS, System.currentTimeMillis())
            }
        val replyPi =
            PendingIntent.getBroadcast(
                context,
                notifId,
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
        return NotificationCompat.Action.Builder(
            R.mipmap.ic_phoniq_launcher,
                context.getString(R.string.notif_sms_reply_action),
                replyPi,
            )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setShowsUserInterface(false)
            .build()
            .also { Person.Builder().setName(sender).build() }
    }

    private fun ensureChannel(
        nm: NotificationManager,
        id: String,
        name: String,
        importance: Int,
    ) {
        if (nm.getNotificationChannel(id) != null) return
        nm.createNotificationChannel(
            NotificationChannel(id, name, importance).apply {
                description = name
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            },
        )
    }

    const val EXTRA_OPEN_THREAD_ID = "phoniq_open_thread_id"
    const val EXTRA_OPEN_MESSAGES_TAB = "phoniq_open_messages_tab"
    const val EXTRA_OPEN_MONEY_TAB = "phoniq_open_money_tab"
    const val EXTRA_MONEY_NOTIF_MODE = "phoniq_money_notif_mode"
    const val EXTRA_TXN_SPLIT_AMOUNT = "phoniq_txn_split_amount"
    const val EXTRA_TXN_SPLIT_MERCHANT = "phoniq_txn_split_merchant"

    private const val NOTIF_BASE_ID = 20_000
}
