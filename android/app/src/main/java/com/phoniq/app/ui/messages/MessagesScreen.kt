package com.phoniq.app.ui.messages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoniq.app.R
import com.phoniq.app.data.model.MessageThread
import com.phoniq.app.data.model.MessageThreadCategory
import com.phoniq.app.data.model.matches
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqBorderSoft
import com.phoniq.app.ui.theme.PhoniqSecondary
import com.phoniq.app.ui.theme.PhoniqSurface
import com.phoniq.app.ui.theme.PhoniqTextSecondaryMock
import com.phoniq.app.ui.theme.PhoniqTextSubtle

@Composable
fun MessagesScreen(
    threads: List<MessageThread>,
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
                itemsIndexed(visibleThreads, key = { _, t -> t.id }) { index, thread ->
                    Column(Modifier.fillMaxWidth()) {
                        ThreadRow(thread = thread, onOpen = { openThreadId = it.id })
                        if (index < visibleThreads.lastIndex) {
                            HorizontalDivider(
                                color = PhoniqBorderSoft,
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                            )
                        }
                    }
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
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 20.dp)
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF00D4AA), Color(0xFF009980))))
                    .clickable(onClick = onComposeClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = stringResource(R.string.cd_compose_new_message),
                tint = Color.White,
                modifier = Modifier.size(22.dp),
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MessageThreadCategory.entries.forEach { c ->
            val count = categoryMatchCount(threads, c)
            MsgTabChip(
                icon = categoryIcon(c),
                label = threadCategoryLabel(c),
                count = count,
                selected = selected == c,
                onClick = { onSelect(c) },
            )
        }
    }
}

@Composable
private fun MsgTabChip(
    icon: ImageVector,
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor =
        if (selected) PhoniqAccent.copy(alpha = 0.22f) else Color.Transparent
    val bg = if (selected) PhoniqAccent.copy(alpha = 0.18f) else PhoniqSurface
    val fg = if (selected) PhoniqAccent else PhoniqTextSecondaryMock
    val countBg =
        if (selected) PhoniqAccent.copy(alpha = 0.24f) else PhoniqTextSubtle.copy(alpha = 0.22f)
    val countFg = if (selected) PhoniqAccent else PhoniqTextSecondaryMock
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = bg,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = fg,
            )
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
            Box(
                modifier =
                    Modifier
                        .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(countBg)
                        .padding(horizontal = 5.dp, vertical = 0.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    count.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = countFg,
                )
            }
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
        MessageThreadCategory.Bill -> Icons.AutoMirrored.Outlined.ReceiptLong
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
    val initial = thread.title.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val g0 = Color(thread.avatarStartArgb.toInt())
    val g1 = Color(thread.avatarEndArgb.toInt())
    val nameColor =
        if (thread.unread) MaterialTheme.colorScheme.onBackground else Color(0xFFF0F0F0)
    val nameWeight = if (thread.unread) FontWeight.Bold else FontWeight.Medium
    val timeColor = if (thread.unread) PhoniqSecondary else Color(0xFF777777)
    val previewColor = if (thread.unread) Color(0xFFCCCCCC) else Color(0xFF777777)
    Surface(
        onClick = { onOpen(thread) },
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box {
                Box(
                    modifier =
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(g0, g1))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (thread.showOnlineDot) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00C472))
                                .border(2.dp, Color(0xFF0A0A0F), CircleShape),
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            thread.title,
                            fontSize = 14.sp,
                            fontWeight = nameWeight,
                            color = nameColor,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (thread.showRcsBadge) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PhoniqAccent.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, PhoniqAccent.copy(alpha = 0.25f)),
                            ) {
                                Text(
                                    "RCS",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.4.sp,
                                    color = PhoniqAccent,
                                )
                            }
                        }
                    }
                    Text(
                        thread.timeLabel,
                        fontSize = 11.sp,
                        color = timeColor,
                    )
                }
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            thread.snippet,
                            fontSize = 12.sp,
                            color = previewColor,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        thread.rowPills.forEach { pill ->
                            val (bg, fg) = pillColors(pill)
                            Surface(shape = RoundedCornerShape(20.dp), color = bg) {
                                Text(
                                    text = pill,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp),
                                    color = fg,
                                )
                            }
                        }
                    }
                    if (thread.unread) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PhoniqSecondary,
                        ) {
                            Text(
                                "1",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                            )
                        }
                    }
                }
                if (thread.listTypingHint) {
                    Text(
                        text = stringResource(R.string.msg_list_typing_hint),
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF53BDEB),
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
                if (thread.otpCode != null) {
                    OtpCopyStrip(code = thread.otpCode, expiresSeconds = thread.otpExpiresSeconds)
                }
            }
        }
    }
}

@Composable
private fun OtpCopyStrip(code: String, expiresSeconds: Int) {
    val clipboard = LocalClipboardManager.current
    var secondsLeft by remember(code) { mutableIntStateOf(expiresSeconds) }
    var copied by remember(code) { mutableStateOf(false) }

    LaunchedEffect(code) {
        while (secondsLeft > 0) {
            delay(1_000)
            secondsLeft--
        }
    }

    val expired = secondsLeft <= 0
    val timerText = if (expired) "Expired" else {
        val m = secondsLeft / 60
        val s = secondsLeft % 60
        if (m > 0) "${m}m ${s}s" else "${s}s"
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (expired) Color(0x20888888) else PhoniqAccent.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, if (expired) Color(0x30888888) else PhoniqAccent.copy(alpha = 0.28f)),
        modifier = Modifier.padding(top = 5.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Default.Timer,
                contentDescription = null,
                tint = if (expired) Color(0xFF888888) else PhoniqAccent,
                modifier = Modifier.size(12.dp),
            )
            Text(
                code,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = if (expired) Color(0xFF888888) else PhoniqAccent,
            )
            Text(
                "·  $timerText",
                fontSize = 10.sp,
                color = if (expired) Color(0xFF666666) else PhoniqAccent.copy(alpha = 0.7f),
            )
            if (!expired) {
                Surface(
                    onClick = {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    },
                    shape = RoundedCornerShape(6.dp),
                    color = if (copied) PhoniqSecondary.copy(alpha = 0.15f) else PhoniqAccent.copy(alpha = 0.18f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            if (copied) Icons.Default.Done else Icons.Default.ContentCopy,
                            contentDescription = "Copy OTP",
                            tint = if (copied) PhoniqSecondary else PhoniqAccent,
                            modifier = Modifier.size(10.dp),
                        )
                        Text(
                            if (copied) "Copied!" else "Copy",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (copied) PhoniqSecondary else PhoniqAccent,
                        )
                    }
                }
            }
        }
    }
}

private fun pillColors(pill: String): Pair<Color, Color> {
    val p = pill.uppercase()
    val accentSoft = PhoniqAccent.copy(alpha = 0.14f)
    return when (p) {
        "OTP" ->
            PhoniqAccent.copy(alpha = 0.15f) to Color(0xFFB6AFFF)
        "TXN", "TRANSACTION" ->
            PhoniqAccent.copy(alpha = 0.17f) to Color(0xFFB6AFFF)
        "BILL", "DUE" ->
            Color(0x29FFC400) to Color(0xFFFFD86B)
        "OVERDUE" ->
            Color(0x30FF5050) to Color(0xFFFF8F8F)
        "PROMO", "SPAM" ->
            PhoniqAccent.copy(alpha = 0.13f) to Color(0xFFD0CAFF)
        "DELIVERY", "TRACKING" ->
            Color(0x2900D4AA) to Color(0xFF5BE8C6)
        "TRAVEL" ->
            PhoniqAccent.copy(alpha = 0.18f) to Color(0xFFB6AFFF)
        else -> accentSoft to PhoniqTextSecondaryMock
    }
}
