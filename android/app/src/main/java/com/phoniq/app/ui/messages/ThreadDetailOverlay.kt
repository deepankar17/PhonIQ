package com.phoniq.app.ui.messages

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phoniq.app.R
import com.phoniq.app.data.db.entity.SmsMessageEntity
import com.phoniq.app.data.mapper.isInvestmentTxnSms
import com.phoniq.app.data.mapper.messageThreadCategories
import com.phoniq.app.domain.sms.SmsParser
import com.phoniq.app.data.model.MessageThreadCategory
import com.phoniq.app.data.mapper.KeyedConversationBubble
import com.phoniq.app.data.mapper.toKeyedConversationBubbles
import com.phoniq.app.data.model.ConversationBubble
import com.phoniq.app.data.model.MessageTickVisual
import com.phoniq.app.data.model.MessageThread
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.components.AvatarInitialsText
import com.phoniq.app.ui.theme.PhoniqSecondary
import com.phoniq.app.util.isShortCodeThreadPeer
import com.phoniq.app.util.sanitizeForTelDial
import com.phoniq.app.util.startDialer
import com.phoniq.app.util.startSmsCompose
import com.phoniq.app.util.tryOpenWhatsAppForNumber
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

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

private enum class ThreadContentFilter {
    All,
    Otp,
    Transaction,
    Investment,
    Bill,
    Delivery,
    Travel,
    Spam,
    Personal,
}

private val ThreadOptionalFiltersOrdered =
    listOf(
        ThreadContentFilter.Otp,
        ThreadContentFilter.Transaction,
        ThreadContentFilter.Investment,
        ThreadContentFilter.Bill,
        ThreadContentFilter.Delivery,
        ThreadContentFilter.Travel,
        ThreadContentFilter.Spam,
        ThreadContentFilter.Personal,
    )

private fun SmsMessageEntity.matchesThreadContentFilter(f: ThreadContentFilter, parser: SmsParser): Boolean =
    when (f) {
        ThreadContentFilter.All -> true
        ThreadContentFilter.Otp -> MessageThreadCategory.Otp in messageThreadCategories()
        ThreadContentFilter.Transaction ->
            category == "TRANSACTION" && !isInvestmentTxnSms(parser)
        ThreadContentFilter.Investment -> isInvestmentTxnSms(parser)
        ThreadContentFilter.Bill -> MessageThreadCategory.Bill in messageThreadCategories()
        ThreadContentFilter.Delivery -> MessageThreadCategory.Delivery in messageThreadCategories()
        ThreadContentFilter.Travel -> MessageThreadCategory.Travel in messageThreadCategories()
        ThreadContentFilter.Spam -> MessageThreadCategory.Spam in messageThreadCategories()
        ThreadContentFilter.Personal -> MessageThreadCategory.Personal in messageThreadCategories()
    }

/** Best-effort PSTN dial target from thread header/subtitle lines. */
private fun MessageThread.dialablePeer(): String? =
    sequenceOf(peerAddress, title)
        .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
        .firstOrNull { sanitizeForTelDial(it).isNotEmpty() }

private fun MessageThread.smsComposeTarget(): String? {
    peerAddress?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    return dialablePeer()
}

private data class ThreadScrollLoadSnapshot(
    val firstVisibleIndex: Int,
    val mayHaveOlder: Boolean,
    val loadingOlder: Boolean,
    val canScrollBackward: Boolean,
)

private fun waSentBubbleFill(accent: Color): Color = lerp(WaBubbleSentBase, accent, 0.72f)

/** Index in [keyedBubbles] of the first unread SMS row (chronological); null when every loaded message is read. */
private fun firstUnreadKeyedBubbleIndex(
    filteredMessages: List<SmsMessageEntity>,
    keyedBubbles: List<KeyedConversationBubble>,
): Int? {
    val firstUnread =
        filteredMessages.asSequence().sortedBy { it.timestamp }.firstOrNull { !it.isRead }
            ?: return null
    val needle = "sms-${firstUnread.id}"
    val i = keyedBubbles.indexOfFirst { it.stableKey == needle }
    return i.takeIf { it >= 0 }
}

/**
 * Full-screen thread overlay aligned with `phoniq-mockup-v1.html` `#overlay-sms-thread`
 * (header, RCS/SMS bars, bubble column, composer strip).
 */
@Composable
fun ThreadDetailOverlay(
    thread: MessageThread,
    messagesViewModel: MessagesViewModel,
    onDismiss: () -> Unit,
    onUserMessage: (String) -> Unit = {},
    onNavigateToMoney: () -> Unit = {},
) {
    val context = LocalContext.current
    var emojiPickerOpen by remember(thread.id) { mutableStateOf(false) }
    val attachPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val cr = context.contentResolver
            val type = cr.getType(uri) ?: "*/*"
            val send =
                Intent(Intent.ACTION_SEND).apply {
                    setType(type)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            try {
                context.startActivity(
                    Intent.createChooser(send, context.getString(R.string.thread_attach_chooser_title))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (_: Exception) {
                onUserMessage(context.getString(R.string.thread_attach_open_failed))
            }
        }
    val threadKey = remember(thread.id) { thread.id.removePrefix("sms_") }
    var messages by remember(thread.id) { mutableStateOf<List<SmsMessageEntity>>(emptyList()) }
    var mayHaveOlder by remember(thread.id) { mutableStateOf(true) }
    var loadingOlder by remember(thread.id) { mutableStateOf(false) }
    var didInitialScroll by remember(thread.id) { mutableStateOf(false) }

    LaunchedEffect(threadKey) {
        didInitialScroll = false
        loadingOlder = false
        messages = messagesViewModel.loadThreadLatestPage(threadKey)
        mayHaveOlder = messages.size >= MessagesViewModel.SMS_THREAD_PAGE_SIZE
    }

    val smsCategoryParser = remember { SmsParser() }
    var contentFilter by remember(thread.id) { mutableStateOf(ThreadContentFilter.All) }
    val optionalThreadFiltersPresent =
        remember(messages, smsCategoryParser) {
            ThreadOptionalFiltersOrdered.filter { cand ->
                messages.any { it.matchesThreadContentFilter(cand, smsCategoryParser) }
            }
        }

    LaunchedEffect(messages, contentFilter, smsCategoryParser) {
        if (contentFilter != ThreadContentFilter.All &&
            messages.none { it.matchesThreadContentFilter(contentFilter, smsCategoryParser) }
        ) {
            contentFilter = ThreadContentFilter.All
        }
    }

    val filteredMessages =
        remember(messages, contentFilter, smsCategoryParser) {
            when (contentFilter) {
                ThreadContentFilter.All -> messages
                else -> messages.filter { it.matchesThreadContentFilter(contentFilter, smsCategoryParser) }
            }
        }

    val keyedBubbles =
        remember(filteredMessages, messages, thread.snippet, thread.timeLabel, context) {
            when {
                filteredMessages.isNotEmpty() -> filteredMessages.toKeyedConversationBubbles(context)
                messages.isNotEmpty() -> emptyList()
                else ->
                    listOf(
                        KeyedConversationBubble(
                            "preview-div",
                            ConversationBubble.DayDivider(context.getString(R.string.thread_preview_label)),
                        ),
                        KeyedConversationBubble(
                            "preview-snippet",
                            ConversationBubble.TextMessage(
                                body = thread.snippet.ifBlank { context.getString(R.string.thread_preview_only) },
                                time = thread.timeLabel,
                                outgoing = false,
                                ticks = MessageTickVisual.None,
                            ),
                        ),
                    )
            }
        }
    val plainSmsHint = stringResource(R.string.thread_sms_plain_default)
    val blockShortCodeReply = remember(thread.id, thread.peerAddress) { thread.isShortCodeThreadPeer() }
    val listState = remember(thread.id) { LazyListState() }
    val listCoroutineScope = rememberCoroutineScope()
    val loadOlderThreshold = 2

    val threadUnreadCount by remember {
        derivedStateOf { filteredMessages.count { !it.isRead } }
    }
    val showJumpToBottomFab by remember {
        derivedStateOf {
            messages.isNotEmpty() &&
                keyedBubbles.isNotEmpty() &&
                listState.canScrollForward
        }
    }

    LaunchedEffect(threadKey, keyedBubbles.size, keyedBubbles.lastOrNull()?.stableKey, messages.size, contentFilter) {
        if (didInitialScroll) return@LaunchedEffect
        if (messages.isNotEmpty() && keyedBubbles.isNotEmpty()) {
            val bubbleIdx = firstUnreadKeyedBubbleIndex(filteredMessages, keyedBubbles)
            val targetBubble = bubbleIdx ?: keyedBubbles.lastIndex
            listState.scrollToItem(targetBubble.coerceIn(0, keyedBubbles.lastIndex))
            didInitialScroll = true
            messagesViewModel.markThreadRead(threadKey)
            if (filteredMessages.any { !it.isRead }) {
                messages = messages.map { it.copy(isRead = true) }
            }
        } else if (messages.isEmpty() && keyedBubbles.isNotEmpty()) {
            listState.scrollToItem(0)
            didInitialScroll = true
        }
    }

    LaunchedEffect(contentFilter, keyedBubbles.lastOrNull()?.stableKey, didInitialScroll) {
        if (!didInitialScroll) return@LaunchedEffect
        if (keyedBubbles.isEmpty()) return@LaunchedEffect
        listState.scrollToItem(keyedBubbles.lastIndex)
    }

    LaunchedEffect(threadKey, didInitialScroll) {
        if (!didInitialScroll) return@LaunchedEffect
        snapshotFlow {
            ThreadScrollLoadSnapshot(
                listState.firstVisibleItemIndex,
                mayHaveOlder,
                loadingOlder,
                listState.canScrollBackward,
            )
        }
            .distinctUntilChanged()
            .collect { snap ->
                if (snap.firstVisibleIndex <= loadOlderThreshold &&
                    snap.mayHaveOlder &&
                    !snap.loadingOlder &&
                    messages.isNotEmpty() &&
                    snap.canScrollBackward
                ) {
                    loadingOlder = true
                    try {
                        val oldKeyedCount = messages.toKeyedConversationBubbles(context).size
                        val scrollIdx = listState.firstVisibleItemIndex
                        val scrollOff = listState.firstVisibleItemScrollOffset
                        val anchor = messages.first()
                        val older =
                            messagesViewModel.loadThreadOlderPage(
                                threadKey,
                                anchor.timestamp,
                                anchor.id,
                            )
                        if (older.isEmpty()) {
                            mayHaveOlder = false
                        } else {
                            messages = older + messages
                            delay(32)
                            val delta = messages.toKeyedConversationBubbles(context).size - oldKeyedCount
                            if (delta > 0) {
                                listState.scrollToItem((scrollIdx + delta).coerceAtLeast(0), scrollOff)
                            }
                            mayHaveOlder = older.size >= MessagesViewModel.SMS_THREAD_PAGE_SIZE
                        }
                    } finally {
                        loadingOlder = false
                    }
                }
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
                    onVideo = {
                        val raw =
                            thread.dialablePeer()
                                ?: thread.peerAddress.orEmpty()
                        if (!context.tryOpenWhatsAppForNumber(raw)) {
                            onUserMessage(context.getString(R.string.toast_whatsapp_open_failed))
                        }
                    },
                    onVoice = {
                        val dest = thread.dialablePeer()
                        if (dest == null || !context.startDialer(dest)) {
                            onUserMessage(context.getString(R.string.toast_dial_failed))
                        }
                    },
                    onUserMessage = onUserMessage,
                )
                if (thread.showRcsBadge) {
                    ThreadRcsFeatureBar()
                    ThreadE2EBar()
                } else {
                    ThreadSmsPlainBar(text = plainSmsHint)
                }
                if (messages.isNotEmpty()) {
                    ThreadContentFilterChipsRow(
                        optionalFiltersPresent = optionalThreadFiltersPresent,
                        selected = contentFilter,
                        onSelect = { contentFilter = it },
                    )
                }
                if (messages.isNotEmpty() && threadUnreadCount > 0) {
                    ThreadUnreadBanner(threadUnreadCount)
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
                        verticalArrangement =
                            Arrangement.spacedBy(if (blockShortCodeReply) 8.dp else 2.dp),
                    ) {
                    items(
                        items = keyedBubbles,
                        key = { it.stableKey },
                    ) { keyed ->
                        ConversationBubbleBlock(
                            bubble = keyed.bubble,
                            shortCodeFeedLayout = blockShortCodeReply,
                            onCopyOtp = { code ->
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.toast_otp_copied, code),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            onViewInMoney = {
                                onUserMessage(context.getString(R.string.toast_view_in_money))
                                onDismiss()
                                onNavigateToMoney()
                            },
                        )
                    }
                    if (messages.isEmpty() && (thread.lastCallSummary != null || thread.localNote != null)) {
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
                    if (showJumpToBottomFab) {
                        SmallFloatingActionButton(
                            onClick = {
                                listCoroutineScope.launch {
                                    val last = keyedBubbles.lastIndex
                                    if (last >= 0) listState.animateScrollToItem(last)
                                }
                            },
                            modifier =
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp),
                            containerColor = WaBubbleReceived,
                            contentColor = WaSubtleText,
                        ) {
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription =
                                    stringResource(R.string.thread_cd_scroll_to_bottom),
                            )
                        }
                    }
                }
                Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
                    if (blockShortCodeReply) {
                        ThreadShortCodeNoReplyBar()
                    } else {
                        ThreadComposerBar(
                            onEmoji = { emojiPickerOpen = true },
                            onAttach = { attachPicker.launch("*/*") },
                            onSend = {
                                val dest = thread.smsComposeTarget()
                                if (dest == null || !context.startSmsCompose(dest)) {
                                    onUserMessage(context.getString(R.string.snackbar_no_sms_app))
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (emojiPickerOpen && !blockShortCodeReply) {
        AlertDialog(
            onDismissRequest = { emojiPickerOpen = false },
            title = { Text(stringResource(R.string.thread_emoji_pick_title)) },
            text = {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (e in listOf("😊", "👍", "❤️", "🙏", "😂", "✅")) {
                        TextButton(
                            onClick = {
                                emojiPickerOpen = false
                                if (!context.startSmsCompose(thread.smsComposeTarget(), body = e)) {
                                    onUserMessage(context.getString(R.string.snackbar_no_sms_app))
                                }
                            },
                        ) {
                            Text(e, fontSize = 22.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { emojiPickerOpen = false }) {
                    Text(stringResource(R.string.action_close))
                }
            },
        )
    }
}

@Composable
private fun ThreadUnreadBanner(unreadCount: Int) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 8.dp),
        color = WaDatePillBg,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            pluralStringResource(R.plurals.thread_unread_banner, unreadCount, unreadCount),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = WaSubtleText,
        )
    }
}

@Composable
private fun ThreadContentFilterChipsRow(
    optionalFiltersPresent: List<ThreadContentFilter>,
    selected: ThreadContentFilter,
    onSelect: (ThreadContentFilter) -> Unit,
) {
    val scroll = rememberScrollState()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThreadFilterChip(
            label = stringResource(R.string.msg_filter_all),
            icon = Icons.Outlined.Inbox,
            selected = selected == ThreadContentFilter.All,
            onClick = { onSelect(ThreadContentFilter.All) },
        )
        optionalFiltersPresent.forEach { f ->
            ThreadFilterChip(
                label = threadContentFilterLabel(f),
                icon = threadContentFilterIcon(f),
                selected = selected == f,
                onClick = { onSelect(f) },
            )
        }
    }
}

@Composable
private fun threadContentFilterLabel(f: ThreadContentFilter): String =
    when (f) {
        ThreadContentFilter.All -> stringResource(R.string.msg_filter_all)
        ThreadContentFilter.Otp -> stringResource(R.string.msg_filter_otp)
        ThreadContentFilter.Transaction -> stringResource(R.string.msg_filter_transaction)
        ThreadContentFilter.Investment -> stringResource(R.string.msg_filter_investment)
        ThreadContentFilter.Bill -> stringResource(R.string.msg_filter_bill)
        ThreadContentFilter.Delivery -> stringResource(R.string.msg_filter_delivery)
        ThreadContentFilter.Travel -> stringResource(R.string.msg_filter_travel)
        ThreadContentFilter.Spam -> stringResource(R.string.msg_filter_spam)
        ThreadContentFilter.Personal -> stringResource(R.string.msg_filter_personal)
    }

private fun threadContentFilterIcon(f: ThreadContentFilter): ImageVector =
    when (f) {
        ThreadContentFilter.All -> Icons.Outlined.Inbox
        ThreadContentFilter.Otp -> Icons.Outlined.Key
        ThreadContentFilter.Transaction -> Icons.Outlined.CurrencyRupee
        ThreadContentFilter.Investment -> Icons.Outlined.AccountBalance
        ThreadContentFilter.Bill -> Icons.AutoMirrored.Outlined.ReceiptLong
        ThreadContentFilter.Delivery -> Icons.Outlined.LocalShipping
        ThreadContentFilter.Travel -> Icons.Outlined.FlightTakeoff
        ThreadContentFilter.Spam -> Icons.Outlined.Report
        ThreadContentFilter.Personal -> Icons.Outlined.Person
    }

@Composable
private fun ThreadFilterChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) PhoniqAccent.copy(alpha = 0.55f) else WaHeaderBorder.copy(alpha = 0.6f)
    val bg = if (selected) WaBubbleReceived.copy(alpha = 0.95f) else WaBubbleReceived.copy(alpha = 0.55f)
    val fg = if (selected) PhoniqAccent else WaSubtleText
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bg,
        border = BorderStroke(1.dp, borderColor),
        modifier =
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = fg)
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
        }
    }
}

@Composable
private fun ThreadChatHeader(
    thread: MessageThread,
    onBack: () -> Unit,
    onVideo: () -> Unit,
    onVoice: () -> Unit,
    onUserMessage: (String) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val menuScope = rememberCoroutineScope()
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
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(WaAvatarSurface),
            contentAlignment = Alignment.Center,
        ) {
            AvatarInitialsText(
                text = initial,
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
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
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.thread_cd_thread_menu),
                    tint = WaSubtleText,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.thread_menu_compose_sms)) },
                    onClick = {
                        menuOpen = false
                        val dest = thread.smsComposeTarget()
                        if (!context.startSmsCompose(dest)) {
                            onUserMessage(context.getString(R.string.snackbar_no_sms_app))
                        }
                    },
                )
                val addr =
                    thread.peerAddress?.trim().orEmpty().ifBlank {
                        thread.dialablePeer().orEmpty()
                    }
                if (addr.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.thread_menu_copy_address)) },
                        onClick = {
                            menuOpen = false
                            menuScope.launch {
                                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("PhonIQ", addr)))
                            }
                            onUserMessage(context.getString(R.string.thread_menu_address_copied))
                        },
                    )
                }
            }
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
private fun ThreadShortCodeNoReplyBar() {
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
                    .padding(start = 14.dp, top = 10.dp, end = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = WaSubtleText,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = stringResource(R.string.thread_short_code_no_reply),
                style = MaterialTheme.typography.bodySmall,
                color = WaSubtleText,
                modifier = Modifier.weight(1f),
            )
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
    shortCodeFeedLayout: Boolean,
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
                shortCodeFeedLayout = shortCodeFeedLayout,
            )
        is ConversationBubble.ReactionRow -> ReactionChipRow(bubble.emoji, bubble.count)
        is ConversationBubble.RichLinkBubble ->
            RichLinkBubbleRow(bubble, shortCodeFeedLayout = shortCodeFeedLayout)
        is ConversationBubble.VoiceNote ->
            VoiceBubbleRow(bubble, shortCodeFeedLayout = shortCodeFeedLayout)
        ConversationBubble.TypingIndicator ->
            TypingBubbleRow(shortCodeFeedLayout = shortCodeFeedLayout)
        is ConversationBubble.SystemLine -> SystemLineRow(bubble.text)
        is ConversationBubble.OtpBubble ->
            OtpBubbleCard(
                bubble = bubble,
                shortCodeFeedLayout = shortCodeFeedLayout,
                onCopy = { onCopyOtp(bubble.code) },
            )
        is ConversationBubble.TxnBubble ->
            TxnBubbleCard(
                bubble = bubble,
                shortCodeFeedLayout = shortCodeFeedLayout,
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
    shortCodeFeedLayout: Boolean = false,
) {
    if (shortCodeFeedLayout) {
        val cardShape = RoundedCornerShape(12.dp)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = cardShape,
            color = WaBubbleReceived,
            border = BorderStroke(1.dp, WaHeaderBorder.copy(alpha = 0.35f)),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(body, style = MaterialTheme.typography.bodyMedium, color = WaBubbleText, lineHeight = 22.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            time,
                            style = MaterialTheme.typography.labelSmall,
                            color = WaTimeIncoming,
                            fontSize = 11.sp,
                        )
                        TickGlyphs(ticks, outgoing)
                    }
                }
            }
        }
        return
    }
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
private fun RichLinkBubbleRow(
    bubble: ConversationBubble.RichLinkBubble,
    shortCodeFeedLayout: Boolean = false,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            shape =
                if (shortCodeFeedLayout) {
                    RoundedCornerShape(12.dp)
                } else {
                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 2.dp)
                },
            color = WaBubbleReceived,
            border =
                if (shortCodeFeedLayout) {
                    BorderStroke(1.dp, WaHeaderBorder.copy(alpha = 0.35f))
                } else {
                    null
                },
            modifier =
                if (shortCodeFeedLayout) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.widthIn(max = 320.dp)
                },
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
private fun VoiceBubbleRow(
    bubble: ConversationBubble.VoiceNote,
    shortCodeFeedLayout: Boolean = false,
) {
    val waves = listOf(10.dp, 16.dp, 8.dp, 14.dp, 6.dp, 18.dp, 10.dp, 12.dp, 8.dp, 14.dp)
    val sentShape =
        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 2.dp, bottomStart = 8.dp)
    val feedShape = RoundedCornerShape(12.dp)
    val arrangement = if (shortCodeFeedLayout) Arrangement.Start else Arrangement.End
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = arrangement) {
        Surface(
            shape = if (shortCodeFeedLayout) feedShape else sentShape,
            color = if (shortCodeFeedLayout) WaBubbleReceived else waSentBubbleFill(PhoniqAccent),
            border =
                if (shortCodeFeedLayout) {
                    BorderStroke(1.dp, WaHeaderBorder.copy(alpha = 0.35f))
                } else {
                    null
                },
            modifier =
                if (shortCodeFeedLayout) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.widthIn(max = 280.dp)
                },
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
                        color = if (shortCodeFeedLayout) WaTimeIncoming else WaTimeOutgoing,
                        fontSize = 11.sp,
                    )
                    TickGlyphs(bubble.ticks, outgoing = !shortCodeFeedLayout)
                }
            }
        }
    }
}

@Composable
private fun TypingBubbleRow(shortCodeFeedLayout: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            shape =
                if (shortCodeFeedLayout) {
                    RoundedCornerShape(12.dp)
                } else {
                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 2.dp)
                },
            color = WaBubbleReceived,
            border =
                if (shortCodeFeedLayout) {
                    BorderStroke(1.dp, WaHeaderBorder.copy(alpha = 0.35f))
                } else {
                    null
                },
            modifier = if (shortCodeFeedLayout) Modifier.fillMaxWidth() else Modifier,
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
    shortCodeFeedLayout: Boolean,
    onCopy: () -> Unit,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val surfaceModifier =
        if (shortCodeFeedLayout) {
            Modifier.fillMaxWidth()
        } else {
            Modifier.widthIn(max = 320.dp)
        }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(if (shortCodeFeedLayout) 12.dp else 18.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
            border =
                if (shortCodeFeedLayout) {
                    BorderStroke(1.dp, WaHeaderBorder.copy(alpha = 0.35f))
                } else {
                    null
                },
            modifier = surfaceModifier,
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    stringResource(R.string.msg_filter_otp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
                Text(bubble.intro, style = MaterialTheme.typography.bodyMedium)
                if (bubble.expiresAtEpochMillis != null) {
                    OtpCountdownCopyStrip(
                        code = bubble.code,
                        expiresAtEpochMillis = bubble.expiresAtEpochMillis,
                        modifier = Modifier.padding(top = 8.dp),
                        onCopied = onCopy,
                    )
                } else {
                    Text(
                        bubble.code,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, letterSpacing = 3.sp),
                        modifier = Modifier.padding(vertical = 8.dp),
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(
                        onClick = {
                            scope.launch {
                                clipboard.setClipEntry(
                                    ClipEntry(ClipData.newPlainText("OTP", bubble.code)),
                                )
                                onCopy()
                            }
                        },
                    ) {
                        Text(stringResource(R.string.thread_copy_otp))
                    }
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
    shortCodeFeedLayout: Boolean,
    onViewInMoney: () -> Unit,
) {
    val surfaceModifier =
        if (shortCodeFeedLayout) {
            Modifier.fillMaxWidth()
        } else {
            Modifier.widthIn(max = 320.dp)
        }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(if (shortCodeFeedLayout) 12.dp else 18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            border =
                if (shortCodeFeedLayout) {
                    BorderStroke(1.dp, WaHeaderBorder.copy(alpha = 0.35f))
                } else {
                    null
                },
            modifier = surfaceModifier,
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
