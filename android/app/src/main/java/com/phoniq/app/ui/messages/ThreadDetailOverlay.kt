package com.phoniq.app.ui.messages

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phoniq.app.R
import com.phoniq.app.data.ThreadConversationSamples
import com.phoniq.app.data.model.ConversationBubble
import com.phoniq.app.data.model.MessageTickVisual
import com.phoniq.app.data.model.MessageThread
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqSecondary

/**
 * Full-screen thread overlay aligned with `phoniq-mockup-v1.html` `#overlay-sms-thread`
 * (header, RCS/SMS bars, bubble column, composer strip).
 */
@Composable
fun ThreadDetailOverlay(
    thread: MessageThread,
    onDismiss: () -> Unit,
    onUserMessage: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val script = remember(thread.id) { ThreadConversationSamples.scriptFor(thread.id) }
    val bubbles =
        remember(thread.id, thread.snippet, thread.timeLabel, script) {
            script?.bubbles
                ?: listOf(
                    ConversationBubble.DayDivider("Preview"),
                    ConversationBubble.TextMessage(
                        body = thread.snippet,
                        time = thread.timeLabel,
                        outgoing = false,
                        ticks = MessageTickVisual.None,
                    ),
                )
        }
    val smsPlainFallback = stringResource(R.string.thread_sms_plain_default)
    val plainSmsHint = script?.plainSmsHint ?: smsPlainFallback
    val listState = rememberLazyListState()

    LaunchedEffect(thread.id, bubbles.size) {
        if (bubbles.isNotEmpty()) {
            listState.scrollToItem(bubbles.lastIndex)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.fillMaxSize()) {
                ThreadChatHeader(
                    thread = thread,
                    onBack = onDismiss,
                    onVideo = { onUserMessage(context.getString(R.string.toast_thread_video)) },
                    onVoice = { onUserMessage(context.getString(R.string.toast_quick_call, thread.title)) },
                    onOverflow = { onUserMessage(context.getString(R.string.toast_thread_overflow)) },
                )
                if (thread.showRcsBadge) {
                    ThreadRcsFeatureBar()
                    ThreadE2EBar()
                } else {
                    ThreadSmsPlainBar(text = plainSmsHint)
                }
                LazyColumn(
                    state = listState,
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(
                        bubbles,
                        key = { idx, _ -> "${thread.id}_$idx" },
                    ) { _, bubble ->
                        ConversationBubbleBlock(
                            bubble = bubble,
                            onCopyOtp = { code ->
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.toast_otp_copied, code),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            onViewInMoney = {
                                onUserMessage(context.getString(R.string.toast_view_in_money))
                            },
                        )
                    }
                    if (script == null && (thread.lastCallSummary != null || thread.localNote != null)) {
                        item {
                            ThreadFusionExtras(thread)
                        }
                    }
                    item {
                        Text(
                            text = stringResource(R.string.thread_full_conversation_wire),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                        )
                    }
                }
                ThreadComposerBar(
                    onEmoji = { onUserMessage(context.getString(R.string.thread_cd_emoji)) },
                    onAttach = { onUserMessage(context.getString(R.string.thread_cd_attach)) },
                    onSend = { onUserMessage(context.getString(R.string.thread_cd_send)) },
                )
            }
        }
    }
}

@Composable
private fun ThreadChatHeader(
    thread: MessageThread,
    onBack: () -> Unit,
    onVideo: () -> Unit,
    onVoice: () -> Unit,
    onOverflow: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
            )
        }
        val initial =
            thread.title.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        Box(
            modifier =
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(initial, style = MaterialTheme.typography.titleMedium)
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
        ) {
            Text(thread.title, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                thread.peerAddress?.let { addr ->
                    Text(
                        addr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "·",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Text(
                    text =
                        if (thread.showRcsBadge) {
                            stringResource(R.string.thread_rcs_active_short)
                        } else {
                            "SMS"
                        },
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        if (thread.showRcsBadge) {
                            PhoniqAccent
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
        IconButton(onClick = onVideo) {
            Icon(Icons.Default.Videocam, contentDescription = stringResource(R.string.thread_cd_video_call))
        }
        IconButton(onClick = onVoice) {
            Icon(Icons.Default.Call, contentDescription = stringResource(R.string.thread_cd_voice_call))
        }
        IconButton(onClick = onOverflow) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.thread_cd_thread_menu))
        }
    }
}

@Composable
private fun ThreadRcsFeatureBar() {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.thread_rcs_active_short),
                style = MaterialTheme.typography.labelMedium,
                color = PhoniqAccent,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.thread_rcs_active_detail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text("📎📍🎤", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ThreadE2EBar() {
    Text(
        text = stringResource(R.string.thread_e2e_rcs),
        style = MaterialTheme.typography.labelSmall,
        color = PhoniqSecondary,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
    )
}

@Composable
private fun ThreadSmsPlainBar(text: String) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ThreadFusionExtras(thread: MessageThread) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        thread.lastCallSummary?.let { line ->
            Text(
                stringResource(R.string.thread_last_call_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(line, style = MaterialTheme.typography.bodyMedium)
        }
        thread.localNote?.let { note ->
            Text(
                stringResource(R.string.thread_local_note_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(note, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ThreadComposerBar(
    onEmoji: () -> Unit,
    onAttach: () -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            IconButton(onClick = onEmoji) {
                Icon(Icons.Default.Mood, contentDescription = stringResource(R.string.thread_cd_emoji))
            }
            Surface(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        stringResource(R.string.thread_composer_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    )
                }
            }
            IconButton(onClick = onAttach) {
                Icon(Icons.Default.AttachFile, contentDescription = stringResource(R.string.thread_cd_attach))
            }
            FilledIconButton(
                onClick = onSend,
                modifier = Modifier.size(44.dp),
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = PhoniqAccent,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.thread_cd_send),
                )
            }
        }
    }
}

@Composable
private fun ConversationBubbleBlock(
    bubble: ConversationBubble,
    onCopyOtp: (String) -> Unit,
    onViewInMoney: () -> Unit,
) {
    when (bubble) {
        is ConversationBubble.DayDivider -> ThreadDayDivider(bubble.label)
        is ConversationBubble.TextMessage ->
            TextBubbleRow(
                body = bubble.body,
                time = bubble.time,
                outgoing = bubble.outgoing,
                ticks = bubble.ticks,
            )
        is ConversationBubble.ReactionRow -> ReactionChipRow(bubble.emoji, bubble.count)
        is ConversationBubble.RichLinkBubble -> RichLinkBubbleRow(bubble)
        is ConversationBubble.VoiceNote -> VoiceBubbleRow(bubble)
        ConversationBubble.TypingIndicator -> TypingBubbleRow()
        is ConversationBubble.SystemLine -> SystemLineRow(bubble.text)
        is ConversationBubble.OtpBubble ->
            OtpBubbleCard(
                bubble = bubble,
                onCopy = { onCopyOtp(bubble.code) },
            )
        is ConversationBubble.TxnBubble ->
            TxnBubbleCard(
                bubble = bubble,
                onViewInMoney = onViewInMoney,
            )
    }
}

@Composable
private fun ThreadDayDivider(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            modifier =
                Modifier
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    }
}

@Composable
private fun TextBubbleRow(
    body: String,
    time: String,
    outgoing: Boolean,
    ticks: MessageTickVisual,
) {
    val bg =
        if (outgoing) {
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.32f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = bg,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(body, style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    TickGlyphs(ticks)
                }
            }
        }
    }
}

@Composable
private fun TickGlyphs(ticks: MessageTickVisual) {
    val color =
        when (ticks) {
            MessageTickVisual.Read -> PhoniqAccent
            MessageTickVisual.None -> androidx.compose.ui.graphics.Color.Transparent
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        }
    val text =
        when (ticks) {
            MessageTickVisual.None -> ""
            MessageTickVisual.Single -> "✓"
            MessageTickVisual.Double,
            MessageTickVisual.Read,
            -> "✓✓"
        }
    if (text.isNotEmpty()) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun ReactionChipRow(
    emoji: String,
    count: Int,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                "$emoji $count",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun RichLinkBubbleRow(bubble: ConversationBubble.RichLinkBubble) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(Modifier.padding(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(bubble.thumbEmoji, style = MaterialTheme.typography.headlineSmall)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            bubble.host,
                            style = MaterialTheme.typography.labelSmall,
                            color = PhoniqAccent,
                        )
                        Text(bubble.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            bubble.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    bubble.footerMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
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

@Composable
private fun VoiceBubbleRow(bubble: ConversationBubble.VoiceNote) {
    val waves = listOf(10.dp, 16.dp, 8.dp, 14.dp, 6.dp, 18.dp, 10.dp, 12.dp, 8.dp, 14.dp)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.32f),
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Column(Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        waves.forEach { h ->
                            Box(
                                modifier =
                                    Modifier
                                        .width(3.dp)
                                        .height(h)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            )
                        }
                    }
                    Text(
                        bubble.durationLabel,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        bubble.bubbleTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    TickGlyphs(bubble.ticks)
                }
            }
        }
    }
}

@Composable
private fun TypingBubbleRow() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(3) {
                    Box(
                        modifier =
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemLineRow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
    )
}

@Composable
private fun OtpBubbleCard(
    bubble: ConversationBubble.OtpBubble,
    onCopy: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    stringResource(R.string.msg_filter_otp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
                Text(bubble.intro, style = MaterialTheme.typography.bodyMedium)
                Text(
                    bubble.code,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, letterSpacing = 3.sp),
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onCopy) {
                    Text(stringResource(R.string.thread_copy_otp))
                }
                Text(
                    bubble.footer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

@Composable
private fun TxnBubbleCard(
    bubble: ConversationBubble.TxnBubble,
    onViewInMoney: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    bubble.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    bubble.amountLine,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    bubble.body,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (bubble.showViewInMoney) {
                    TextButton(
                        onClick = onViewInMoney,
                        colors =
                            ButtonDefaults.textButtonColors(
                                contentColor = PhoniqAccent,
                            ),
                    ) {
                        Text(stringResource(R.string.thread_view_in_money))
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
