package com.phoniq.app.notification

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.phoniq.app.R
import com.phoniq.app.data.model.TxnNotificationMoneyInsights
import com.phoniq.app.domain.sms.SmsParser
import java.text.NumberFormat
import java.util.Locale

/**
 * Rich [RemoteViews] for bank / UPI transaction SMS (headline amount + merchant + category + SMS body).
 */
object TransactionNotificationViews {

    data class RichTxnNotificationContent(
        val collapsed: RemoteViews,
        val expanded: RemoteViews,
        val summaryTitle: String,
        val summaryText: String,
    )

    fun build(
        context: Context,
        sender: String,
        body: String,
        txn: SmsParser.TransactionResult,
        moneyInsights: TxnNotificationMoneyInsights?,
    ): RichTxnNotificationContent {
        val pkg = context.packageName
        val money = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
        val amountLabel = money.format(txn.amount)

        val spentVerb = context.getString(R.string.notif_txn_spent)
        val receivedVerb = context.getString(R.string.notif_txn_received)
        val atWord = context.getString(R.string.notif_txn_at)

        val verb = if (txn.type == "CREDIT") receivedVerb else spentVerb
        val merchantClean = txn.merchant?.trim()?.takeIf { it.isNotEmpty() }
        val narrativeClean = txn.narrative?.trim()?.takeIf { it.isNotEmpty() && it != merchantClean }

        val headline =
            buildString {
                append(verb)
                append(" ")
                append(amountLabel)
                if (merchantClean != null) {
                    append(" ")
                    append(atWord)
                    append(" ")
                    append(merchantClean)
                } else if (narrativeClean != null) {
                    append(" — ")
                    append(narrativeClean.take(48))
                }
            }

        val subtitle =
            buildString {
                append(shortSender(sender))
                txn.account?.let { acct ->
                    append(" · ")
                    append(context.getString(R.string.notif_txn_card_suffix, acct))
                }
                val catLabel = humanCategory(context, txn.category)
                if (catLabel.isNotEmpty()) {
                    append(" · ")
                    append(catLabel)
                }
            }

        val smsLines =
            buildString {
                txn.availableBalance?.let { av ->
                    append(context.getString(R.string.notif_txn_avl_fmt, money.format(av)))
                }
                if (txn.type == "DEBIT" && narrativeClean != null && merchantClean == null) {
                    if (this.isNotEmpty()) append("\n")
                    append(narrativeClean.take(120))
                }
            }.trim()

        val dbLines =
            moneyInsights?.let { m ->
                buildString {
                    append(
                        context.getString(
                            R.string.notif_txn_month_spend_fmt,
                            money.format(m.monthSpendTotal),
                            m.monthDisplayName,
                        ),
                    )
                    m.safeToSpendPerDay?.let { d ->
                        append("\n")
                        append(context.getString(R.string.notif_txn_safe_per_day_fmt, money.format(d)))
                    }
                }.trim()
            }.orEmpty()

        val mergedInsight =
            buildList {
                if (dbLines.isNotEmpty()) add(dbLines)
                if (smsLines.isNotEmpty()) add(smsLines)
            }.joinToString("\n")

        val iconRes = categoryIconRes(txn.category)

        val collapsed = RemoteViews(pkg, R.layout.notification_txn_collapsed)
        collapsed.setTextViewText(R.id.notif_txn_primary, headline)
        collapsed.setTextViewText(R.id.notif_txn_meta, subtitle)
        collapsed.setImageViewResource(R.id.notif_txn_cat_icon, iconRes)

        val expanded = RemoteViews(pkg, R.layout.notification_txn_expanded)
        expanded.setTextViewText(R.id.notif_txn_amount_line, headline)
        expanded.setTextViewText(R.id.notif_txn_subtitle, subtitle)
        expanded.setImageViewResource(R.id.notif_txn_cat_icon, iconRes)
        if (mergedInsight.isNotEmpty()) {
            expanded.setViewVisibility(R.id.notif_txn_insight, View.VISIBLE)
            expanded.setTextViewText(R.id.notif_txn_insight, mergedInsight)
        } else {
            expanded.setViewVisibility(R.id.notif_txn_insight, View.GONE)
        }
        expanded.setTextViewText(R.id.notif_txn_full_sms, body.trim().take(2000))

        return RichTxnNotificationContent(
            collapsed = collapsed,
            expanded = expanded,
            summaryTitle = headline.take(80),
            summaryText = subtitle.take(120),
        )
    }

    fun categoryIconRes(code: String): Int =
        when (code) {
            "FOOD" -> R.drawable.ic_notif_cat_food
            "SHOPPING" -> R.drawable.ic_notif_cat_shopping
            "TRANSPORT" -> R.drawable.ic_notif_cat_transport
            "BILLS" -> R.drawable.ic_notif_cat_bills
            "EMI" -> R.drawable.ic_notif_cat_emi
            "SALARY" -> R.drawable.ic_notif_cat_salary
            "INVESTMENT" -> R.drawable.ic_notif_cat_investment
            "HEALTH" -> R.drawable.ic_notif_cat_health
            "ENTERTAINMENT" -> R.drawable.ic_notif_cat_entertainment
            "ATM" -> R.drawable.ic_notif_cat_atm
            else -> R.drawable.ic_notif_cat_other
        }

    private fun shortSender(sender: String): String {
        val s = sender.trim()
        if (s.length <= 22) return s
        return s.take(20) + "…"
    }

    private fun humanCategory(context: Context, code: String): String =
        when (code) {
            "FOOD" -> context.getString(R.string.notif_txn_cat_food)
            "SHOPPING" -> context.getString(R.string.notif_txn_cat_shopping)
            "TRANSPORT" -> context.getString(R.string.notif_txn_cat_transport)
            "BILLS" -> context.getString(R.string.notif_txn_cat_bills)
            "EMI" -> context.getString(R.string.notif_txn_cat_emi)
            "SALARY" -> context.getString(R.string.notif_txn_cat_salary)
            "INVESTMENT" -> context.getString(R.string.notif_txn_cat_investment)
            "HEALTH" -> context.getString(R.string.notif_txn_cat_health)
            "ENTERTAINMENT" -> context.getString(R.string.notif_txn_cat_entertainment)
            "ATM" -> context.getString(R.string.notif_txn_cat_atm)
            else -> ""
        }
}
