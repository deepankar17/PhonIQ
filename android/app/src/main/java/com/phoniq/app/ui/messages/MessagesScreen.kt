package com.phoniq.app.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoniq.app.R
import com.phoniq.app.data.model.MessageThread
import com.phoniq.app.data.model.MessageThreadCategory
import com.phoniq.app.data.model.matches
import com.phoniq.app.ui.theme.PhoniqAccent
import kotlinx.coroutines.delay

@Composable
fun MessagesScreen(
    threads: SnapshotStateList<MessageThread>,
    onComposeClick: () -> Unit = {},
    onThreadAction: (String) -> Unit = {},
) {
    var category by remember { mutableStateOf(MessageThreadCategory.All) }
    var openThreadId by remember { mutableStateOf<String?>(null) }
    val visibleThreads = threads.filter { it.matches(category) }
    val openThread = openThreadId?.let { id -> threads.find { it.id == id } }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            MessageCategoryFilterStrip(
                threads = threads,
                selected = category,
                onSelect = { category = it },
            )
            LazyColumn(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                items(visibleThreads, key = { it.id }) { thread ->
                    ThreadRow(thread = thread, onOpen = { openThreadId = it.id })
                }
            }
        }
        openThread?.let { t ->
            ThreadDetailOverlay(
                thread = t,
                onDismiss = { openThreadId = null },
                onUserMessage = onThreadAction,
            )
        }
        FloatingActionButton(
            onClick = onComposeClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = PhoniqAccent,
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = stringResource(R.string.cd_compose_new_message),
            )
        }
    }
}

private fun categoryMatchCount(
    threads: Collection<MessageThread>,
    category: MessageThreadCategory,
): Int =
    when (category) {
        MessageThreadCategory.All -> threads.size
        MessageThreadCategory.Unread -> threads.count { it.unread }
        else -> threads.count { category in it.categories }
    }

@Composable
private fun MessageCategoryFilterStrip(
    threads: Collection<MessageThread>,
    selected: MessageThreadCategory,
    onSelect: (MessageThreadCategory) -> Unit,
) {
    val scroll = rememberScrollState()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MessageThreadCategory.entries.forEach { c ->
            val count = categoryMatchCount(threads, c)
            FilterChip(
                selected = selected == c,
                onClick = { onSelect(c) },
                leadingIcon = {
                    Icon(
                        imageVector = categoryIcon(c),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                label = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(threadCategoryLabel(c))
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PhoniqAccent.copy(alpha = 0.18f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                    ),
            )
        }
    }
}

@Composable
private fun categoryIcon(c: MessageThreadCategory): ImageVector =
    when (c) {
        MessageThreadCategory.All -> Icons.Outlined.Inbox
        MessageThreadCategory.Unread -> Icons.Outlined.MarkEmailUnread
        MessageThreadCategory.Personal -> Icons.Outlined.Person
        MessageThreadCategory.Transaction -> Icons.Outlined.CurrencyRupee
        MessageThreadCategory.Otp -> Icons.Outlined.Key
        MessageThreadCategory.Bill -> Icons.Outlined.ReceiptLong
        MessageThreadCategory.Delivery -> Icons.Outlined.LocalShipping
        MessageThreadCategory.Travel -> Icons.Outlined.FlightTakeoff
        MessageThreadCategory.Spam -> Icons.Outlined.Report
    }

@Composable
private fun threadCategoryLabel(c: MessageThreadCategory): String =
    when (c) {
        MessageThreadCategory.All -> stringResource(R.string.msg_filter_all)
        MessageThreadCategory.Unread -> stringResource(R.string.msg_filter_unread)
        MessageThreadCategory.Personal -> stringResource(R.string.msg_filter_personal)
        MessageThreadCategory.Transaction -> stringResource(R.string.msg_filter_transaction)
        MessageThreadCategory.Otp -> stringResource(R.string.msg_filter_otp)
        MessageThreadCategory.Bill -> stringResource(R.string.msg_filter_bill)
        MessageThreadCategory.Delivery -> stringResource(R.string.msg_filter_delivery)
        MessageThreadCategory.Travel -> stringResource(R.string.msg_filter_travel)
        MessageThreadCategory.Spam -> stringResource(R.string.msg_filter_spam)
    }

@Composable
private fun ThreadRow(
    thread: MessageThread,
    onOpen: (MessageThread) -> Unit,
) {
    val context = LocalContext.current
    Surface(
        onClick = { onOpen(thread) },
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
    ) {
        ListItem(
            leadingContent = {
                val initial =
                    thread.title.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            headlineContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(thread.title, style = MaterialTheme.typography.titleMedium)
                    if (thread.showRcsBadge) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        ) {
                            Text(
                                "RCS",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (thread.unread) {
                        Text("·", color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        thread.snippet,
                        maxLines = 2,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (thread.rowPills.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            thread.rowPills.forEach { pill ->
                                val (bg, fg) = pillColors(pill)
                                Surface(shape = RoundedCornerShape(20.dp), color = bg) {
                                    Text(
                                        text = pill,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        color = fg,
                                    )
                                }
                            }
                        }
                    }
                    if (thread.otpCode != null) {
                        OtpListStrip(
                            code = thread.otpCode,
                            initialSeconds = thread.otpExpiresSeconds,
                            onCopy = {
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(R.string.otp_copied_toast, thread.otpCode),
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                            },
                        )
                    }
                    if (thread.listTypingHint) {
                        Text(
                            text = stringResource(R.string.msg_list_typing_hint),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            overlineContent = {
                thread.subtitleBadge?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall)
                }
            },
            trailingContent = {
                Text(thread.timeLabel, style = MaterialTheme.typography.labelMedium)
            },
        )
    }
}

@Composable
private fun OtpListStrip(code: String, initialSeconds: Int, onCopy: () -> Unit) {
    var remaining by rememberSaveable { mutableIntStateOf(initialSeconds) }
    LaunchedEffect(code) {
        while (remaining > 0) {
            delay(1000L)
            remaining--
        }
    }
    val isExpired = remaining == 0
    val countdownLabel = if (isExpired) {
        stringResource(R.string.otp_expired)
    } else {
        "%d:%02d".format(remaining / 60, remaining % 60)
    }
    val otpTeal = Color(0xFF00D4AA)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = code,
            color = if (isExpired) MaterialTheme.colorScheme.outline else otpTeal,
            fontSize = 16.sp,
            letterSpacing = 3.sp,
            style = MaterialTheme.typography.titleSmall,
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isExpired) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            } else {
                otpTeal.copy(alpha = 0.12f)
            },
        ) {
            Text(
                text = countdownLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (isExpired) MaterialTheme.colorScheme.error else otpTeal,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        if (!isExpired) {
            TextButton(
                onClick = onCopy,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Text(
                    stringResource(R.string.otp_copy_btn),
                    style = MaterialTheme.typography.labelMedium,
                    color = otpTeal,
                )
            }
        }
    }
}

private fun pillColors(pill: String): Pair<Color, Color> {
    val teal = Color(0xFF00D4AA)
    val amber = Color(0xFFF5A623)
    val red = Color(0xFFFF6B6B)
    val purple = Color(0xFF9575CD)
    return when (pill.uppercase()) {
        "OTP"                   -> teal.copy(alpha = 0.14f) to teal
        "TXN", "TRANSACTION"    -> amber.copy(alpha = 0.14f) to amber
        "BILL", "OVERDUE", "DUE"-> red.copy(alpha = 0.14f) to red
        "PROMO", "SPAM"         -> purple.copy(alpha = 0.14f) to purple
        else                    -> Color(0xFF888888).copy(alpha = 0.14f) to Color(0xFF888888)
    }
}
