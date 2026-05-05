package com.phoniq.app.ui.messages

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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

/** `.overlay-thread-wa` tokens from `design/phoniq-mockup-v1.html` */
private val WaChatBg = Color(0xFF0B141A)
private val WaHeaderBg = Color(0xFF1F2C34)
private val WaHeaderBorder = Color(0xFF2A3942)
private val WaBubbleSentBase = Color(0xFF1F2C34)
private val WaBubbleReceived = Color(0xFF202C33)
private val WaBubbleText = Color(0xFFE9EDEF)
private val WaSubtleText = Color(0xFF8696A0)
private val WaTimeOutgoing = Color(0x8CE9EDEF)
private val WaTimeIncoming = Color(0x73E9EDEF)
private val WaTickMuted = Color(0x8CE9EDEF)
private val WaComposerField = Color(0xFF2A3942)
private val WaSendGreen = Color(0xFF00A884)
private val WaDatePillBg = Color(0xFF182329)
private val WaAvatarSurface = Color(0xFF2A3942)

private fun waSentBubbleFill(accent: Color): Color = lerp(WaBubbleSentBase, accent, 0.72f)

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
            color = WaChatBg,
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
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors =
                                        listOf(
                                            WaChatBg,
                                            Color(0xFF0B1620).copy(alpha = 0.92f),
                                            WaChatBg,
                                        ),
                                    start = Offset.Zero,
                                    end = Offset(800f, 1200f),
                                ),
                            ),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
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
                            color = WaSubtleText,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                        )
                    }
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
                .background(WaHeaderBg)
                .drawBehind {
                    val stroke = 1.dp.toPx()
                    drawLine(
                        WaHeaderBorder,
                        Offset(0f, size.height - stroke / 2),
                        Offset(size.width, size.height - stroke / 2),
                        strokeWidth = stroke,
                    )
                }
                .padding(start = 4.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = WaSubtleText,
            )
        }
        val initial =
            thread.title.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        Box(
            modifier =
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(WaAvatarSurface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initial,
                style = MaterialTheme.typography.titleSmall,
                color = WaBubbleText,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
        ) {
            Text(
                thread.title,
                style = MaterialTheme.typography.titleMedium,
                color = WaBubbleText,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                thread.peerAddress?.let { addr ->
                    Text(
                        addr,
                        style = MaterialTheme.typography.bodySmall,
                        color = WaSubtleText,
                    )
                    Text(
                        "·",
                        style = MaterialTheme.typography.bodySmall,
                        color = WaSubtleText,
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
                            WaSubtleText
                        },
                )
            }
        }
        IconButton(onClick = onVideo) {
            Icon(
                Icons.Default.Videocam,
                contentDescription = stringResource(R.string.thread_cd_video_call),
                tint = WaSubtleText,
            )
        }
        IconButton(onClick = onVoice) {
            Icon(
                Icons.Default.Call,
                contentDescription = stringResource(R.string.thread_cd_voice_call),
                tint = WaSubtleText,
            )
        }
        IconButton(onClick = onOverflow) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.thread_cd_thread_menu),
                tint = WaSubtleText,
            )
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
        color = WaBubbleReceived.copy(alpha = 0.92f),
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
                color = WaSubtleText,
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
        color = WaBubbleReceived.copy(alpha = 0.55f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = WaSubtleText,
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
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = WaHeaderBg,
        modifier =
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    val stroke = 1.dp.toPx()
                    drawLine(
                        WaHeaderBorder,
                        Offset(0f, stroke / 2),
                        Offset(size.width, stroke / 2),
                        strokeWidth = stroke,
                    )
                },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 6.dp, end = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            IconButton(onClick = onEmoji) {
                Icon(
                    Icons.Default.Mood,
                    contentDescription = stringResource(R.string.thread_cd_emoji),
                    tint = WaSubtleText,
                )
            }
            Surface(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(42.dp),
                shape = RoundedCornerShape(22.dp),
                color = WaComposerField,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        stringResource(R.string.thread_composer_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WaSubtleText,
                    )
                }
            }
            IconButton(onClick = onAttach) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = stringResource(R.string.thread_cd_attach),
                    tint = WaSubtleText,
                )
            }
            FilledIconButton(
                onClick = onSend,
                modifier = Modifier.size(42.dp),
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = WaSendGreen,
                        contentColor = Color.White,
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
        HorizontalDivider(modifier = Modifier.weight(1f), color = WaHeaderBorder.copy(alpha = 0.35f))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            modifier =
                Modifier
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(WaDatePillBg)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            color = WaSubtleText,
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = WaHeaderBorder.copy(alpha = 0.35f))
    }
}

@Composable
private fun TextBubbleRow(
    body: String,
    time: String,
    outgoing: Boolean,
    ticks: MessageTickVisual,
) {
    val sentShape =
        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 2.dp, bottomStart = 8.dp)
    val receivedShape =
        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 2.dp)
    val bg =
        if (outgoing) {
            waSentBubbleFill(PhoniqAccent)
        } else {
            WaBubbleReceived
        }
    val shape = if (outgoing) sentShape else receivedShape
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = shape,
            color = bg,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(Modifier.padding(start = 10.dp, top = 6.dp, end = 10.dp, bottom = 4.dp)) {
                Text(body, style = MaterialTheme.typography.bodyMedium, color = WaBubbleText, lineHeight = 20.sp)
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        time,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (outgoing) WaTimeOutgoing else WaTimeIncoming,
                        fontSize = 11.sp,
                    )
                    TickGlyphs(ticks, outgoing)
                }
            }
        }
    }
}

@Composable
private fun TickGlyphs(ticks: MessageTickVisual, outgoing: Boolean) {
    val color =
        when (ticks) {
            MessageTickVisual.Read -> PhoniqAccent
            MessageTickVisual.None -> Color.Transparent
            else ->
                if (outgoing) {
                    WaTickMuted
                } else {
                    WaTimeIncoming
                }
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
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontSize = 11.sp)
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
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 2.dp),
            color = WaBubbleReceived,
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
                        Text(
                            bubble.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = WaBubbleText,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            bubble.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = WaSubtleText,
                        )
                    }
                }
                Text(
                    bubble.footerMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WaBubbleText,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    bubble.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = WaTimeIncoming,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Composable
private fun VoiceBubbleRow(bubble: ConversationBubble.VoiceNote) {
    val waves = listOf(10.dp, 16.dp, 8.dp, 14.dp, 6.dp, 18.dp, 10.dp, 12.dp, 8.dp, 14.dp)
    val sentShape =
        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 2.dp, bottomStart = 8.dp)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            shape = sentShape,
            color = waSentBubbleFill(PhoniqAccent),
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Column(Modifier.padding(start = 10.dp, top = 6.dp, end = 10.dp, bottom = 4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = WaBubbleText,
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
                                        .background(WaBubbleText.copy(alpha = 0.35f)),
                            )
                        }
                    }
                    Text(
                        bubble.durationLabel,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 4.dp),
                        color = WaBubbleText,
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
                        color = WaTimeOutgoing,
                        fontSize = 11.sp,
                    )
                    TickGlyphs(bubble.ticks, outgoing = true)
                }
            }
        }
    }
}

@Composable
private fun TypingBubbleRow() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 2.dp),
            color = WaBubbleReceived,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(3) { index ->
                    TypingDot(delayMs = index * 200)
                }
            }
        }
    }
}

@Composable
private fun TypingDot(delayMs: Int) {
    val transition = rememberInfiniteTransition(label = "typing")
    val fraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(delayMs),
            ),
        label = "typingPulse",
    )
    val density = LocalDensity.current
    val travel = with(density) { 5.dp.toPx() }
    Box(
        modifier =
            Modifier
                .size(6.dp)
                .graphicsLayer {
                    translationY = -fraction * travel
                    alpha = 0.4f + fraction * 0.6f
                }
                .clip(CircleShape)
                .background(WaSubtleText),
    )
}

@Composable
private fun SystemLineRow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = WaSubtleText.copy(alpha = 0.85f),
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
