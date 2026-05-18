package com.phoniq.app.data.mapper

import com.phoniq.app.data.model.MessageTxnPreview
import com.phoniq.app.data.model.TxnBubbleStructured
import com.phoniq.app.domain.sms.SmsParser
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val categoryEmoji =
    mapOf(
        "FOOD" to "🍽️",
        "SHOPPING" to "🛍️",
        "BILLS" to "📄",
        "TRANSPORT" to "🚗",
        "EMI" to "🏦",
        "HEALTH" to "💊",
        "ENTERTAINMENT" to "🎬",
        "INVESTMENT" to "📈",
        "SALARY" to "💰",
        "ATM" to "🏧",
        "OTHER" to "💳",
    )

private val txnDateFormatter = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())

private val inrWholeFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))

private fun formatInrAmount(amount: Double, forceDecimals: Boolean = false): String {
    return if (forceDecimals || amount % 1.0 != 0.0) {
        "₹${inrWholeFormat.format(amount)}"
    } else {
        "₹${inrWholeFormat.format(amount.roundToInt())}"
    }
}

fun formatMaskedAccount(accountDigits: String?): String? =
    accountDigits?.trim()?.takeIf { it.isNotEmpty() }?.let { "··$it" }

fun SmsParser.TransactionResult.toMessageTxnPreview(timestampMillis: Long): MessageTxnPreview {
    val isCredit = type == "CREDIT"
    val amountLabel =
        if (isCredit) {
            "+₹${amount.roundToInt()}"
        } else {
            "₹${amount.roundToInt()}"
        }
    val title =
        narrative?.trim()?.takeIf { it.isNotBlank() }
            ?: merchant?.trim()?.takeIf { it.isNotBlank() }
            ?: if (isCredit) "Credited" else "Debited"
    val subtitle =
        buildList {
            formatMaskedAccount(account)?.let { add(it) }
        }.joinToString(" · ").ifBlank {
            txnDateFormatter.format(Date(timestampMillis))
        }
    return MessageTxnPreview(
        title = title,
        subtitle = subtitle,
        amountLabel = amountLabel,
        isCredit = isCredit,
        emoji = categoryEmoji[category] ?: "💳",
        categoryTag = category.lowercase(Locale.US),
    )
}

fun SmsParser.TransactionResult.toTxnBubbleStructured(body: String): TxnBubbleStructured {
    val isCredit = type == "CREDIT"
    val amountLabel =
        if (isCredit) {
            "+${formatInrAmount(amount)}"
        } else {
            formatInrAmount(amount)
        }
    return TxnBubbleStructured(
        maskedAccount = formatMaskedAccount(account),
        narrative =
            narrative?.trim()?.takeIf { it.isNotBlank() }
                ?: merchant?.trim()?.takeIf { it.isNotBlank() },
        typeLabel = if (isCredit) "CREDIT" else "DEBIT",
        amountLabel = amountLabel,
        isCredit = isCredit,
        availableBalanceLabel =
            availableBalance?.let { formatInrAmount(it, forceDecimals = true) },
        emoji = categoryEmoji[category] ?: "💳",
        categoryTag = category.lowercase(Locale.US),
        fullBody = body.trim(),
    )
}
