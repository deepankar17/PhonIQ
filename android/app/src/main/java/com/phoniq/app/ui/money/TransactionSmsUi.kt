package com.phoniq.app.ui.money

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoniq.app.R
import com.phoniq.app.data.model.ConversationBubble
import com.phoniq.app.data.model.MessageTxnPreview
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqDebit
import com.phoniq.app.ui.theme.PhoniqSecondary

private val ColorCredit = PhoniqSecondary
private val TxnBubbleBorder = Color(0xFF2A3942)
private val TxnBubbleSubtle = Color(0xFF8696A0)

fun txnCategoryBackground(tag: String): Color =
    when (tag) {
        "food" -> Color(0xFF6C63FF).copy(alpha = 0.15f)
        "salary" -> Color(0xFF00D4AA).copy(alpha = 0.15f)
        "shopping" -> Color(0xFFF5A623).copy(alpha = 0.15f)
        "bills" -> Color(0xFFFF6B6B).copy(alpha = 0.15f)
        "transport" -> Color(0xFF00D4AA).copy(alpha = 0.15f)
        "investment" -> Color(0xFF6C63FF).copy(alpha = 0.15f)
        else -> Color(0xFF888888).copy(alpha = 0.15f)
    }

/** Compact inbox preview — matches Money tab [TransactionRow] layout. */
@Composable
fun TransactionSmsListPreview(
    preview: MessageTxnPreview,
    modifier: Modifier = Modifier,
) {
    TransactionSmsRowContent(
        title = preview.title,
        subtitle = preview.subtitle,
        amountLabel = preview.amountLabel,
        isCredit = preview.isCredit,
        emoji = preview.emoji,
        categoryTag = preview.categoryTag,
        modifier = modifier,
        horizontalPadding = 0.dp,
        verticalPadding = 0.dp,
    )
}

/** Shared row for Money recent transactions and message inbox previews. */
@Composable
fun TransactionSmsRowContent(
    title: String,
    subtitle: String,
    amountLabel: String,
    isCredit: Boolean,
    emoji: String,
    categoryTag: String,
    modifier: Modifier = Modifier,
    horizontalPadding: androidx.compose.ui.unit.Dp = 14.dp,
    verticalPadding: androidx.compose.ui.unit.Dp = 10.dp,
) {
    val iconShape = RoundedCornerShape(12.dp)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(38.dp)
                    .clip(iconShape)
                    .background(txnCategoryBackground(categoryTag)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 18.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                subtitle,
                fontSize = 11.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(top = 1.dp),
                maxLines = 1,
            )
        }
        Text(
            amountLabel,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isCredit) ColorCredit else PhoniqDebit,
        )
    }
}

/** Thread bubble for bank/UPI transaction SMS — structured summary with optional raw body toggle. */
@Composable
fun TransactionTxnBubbleCard(
    bubble: ConversationBubble.TxnBubble,
    bubbleKey: String,
    shortCodeFeedLayout: Boolean,
    modifier: Modifier = Modifier,
) {
    var showRawSms by remember(bubbleKey) { mutableStateOf(false) }
    val surfaceModifier =
        if (shortCodeFeedLayout) {
            Modifier.fillMaxWidth()
        } else {
            Modifier.widthIn(max = 320.dp)
        }
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(if (shortCodeFeedLayout) 12.dp else 18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            border =
                if (shortCodeFeedLayout) {
                    BorderStroke(1.dp, TxnBubbleBorder.copy(alpha = 0.35f))
                } else {
                    null
                },
            modifier = surfaceModifier,
        ) {
            Column(Modifier.padding(12.dp)) {
                if (showRawSms) {
                    Text(
                        bubble.fullBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp,
                    )
                    TextButton(
                        onClick = { showRawSms = false },
                        colors =
                            ButtonDefaults.textButtonColors(
                                contentColor = PhoniqAccent,
                            ),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            stringResource(R.string.thread_txn_back_to_card),
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(txnCategoryBackground(bubble.categoryTag)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(bubble.emoji, fontSize = 16.sp)
                        }
                        Column(Modifier.weight(1f)) {
                            bubble.maskedAccount?.let { masked ->
                                Text(
                                    stringResource(R.string.thread_txn_account, masked),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TxnBubbleSubtle,
                                )
                            }
                            bubble.narrative?.let { narrative ->
                                Text(
                                    narrative,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = if (bubble.maskedAccount != null) 4.dp else 0.dp),
                                    maxLines = 3,
                                )
                            }
                            Row(
                                modifier =
                                    Modifier.padding(
                                        top =
                                            when {
                                                bubble.narrative != null -> 6.dp
                                                bubble.maskedAccount != null -> 4.dp
                                                else -> 0.dp
                                            },
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    bubble.typeLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (bubble.isCredit) ColorCredit else PhoniqDebit,
                                )
                                Text(
                                    bubble.amountLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (bubble.isCredit) ColorCredit else PhoniqDebit,
                                )
                            }
                            bubble.availableBalanceLabel?.let { balance ->
                                Text(
                                    stringResource(R.string.thread_txn_avl_balance, balance),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TxnBubbleSubtle,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                    if (bubble.fullBody.isNotBlank()) {
                        TextButton(
                            onClick = { showRawSms = true },
                            colors =
                                ButtonDefaults.textButtonColors(
                                    contentColor = PhoniqAccent,
                                ),
                            modifier = Modifier.padding(top = 2.dp),
                        ) {
                            Text(stringResource(R.string.thread_view_sms_text))
                        }
                    }
                }
                Text(
                    bubble.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}
