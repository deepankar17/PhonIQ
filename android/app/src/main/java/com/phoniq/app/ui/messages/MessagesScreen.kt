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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoniq.app.PhonIQLaunchRouter
import com.phoniq.app.R
import com.phoniq.app.data.model.MessageThread
import com.phoniq.app.data.model.MessageThreadCategory
import com.phoniq.app.data.model.matches
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqBorderSoft
import com.phoniq.app.ui.theme.PhoniqSecondary
import com.phoniq.app.ui.theme.PhoniqTextSecondaryMock
import com.phoniq.app.ui.theme.PhoniqTextSubtle
import com.phoniq.app.ui.components.AvatarInitialsText
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/** Items per "page" for the thread list window (initial + each scroll load). */
private const val MESSAGE_INBOX_PAGE_SIZE = 10
/** Load the next page when the last visible row is within this many items of the list end. */
private const val MESSAGE_LIST_SCROLL_THRESHOLD = 3
/** Coalesce rapid layout updates so we do not bump the window multiple times per scroll settle. */
private const val MESSAGE_INBOX_END_SCROLL_DEBOUNCE_MS = 120L

private val InboxStripCategories =
    listOf(
        MessageThreadCategory.All,
        MessageThreadCategory.Unread,
        MessageThreadCategory.Archived,
    )

@OptIn(FlowPreview::class)
@Composable
fun MessagesScreen(
    threads: List<MessageThread>,
    messagesViewModel: MessagesViewModel,
    pendingOpenThreadId: String? = null,
    onConsumePendingOpenThread: () -> Unit = {},
    pendingComposeAddress: String? = null,
    onConsumePendingCompose: () -> Unit = {},
    pendingOpenBlankComposer: Boolean = false,
    onConsumePendingOpenBlankComposer: () -> Unit = {},
    onNavigateToMoney: () -> Unit = {},
    onThreadAction: (String) -> Unit = {},
) {
    var category by remember { mutableStateOf(MessageThreadCategory.All) }
    var openThreadId by remember { mutableStateOf<String?>(null) }
    var draftThread by remember { mutableStateOf<MessageThread?>(null) }
    // Full thread list comes from the VM; we only compose a growing window here (see VM tech-debt note).
    val visibleThreads = threads.filter { it.matches(category) }
    val openThread = openThreadId?.let { id -> threads.find { it.id == id } }
    val overlayThread = draftThread ?: openThread
    val threadNotFoundMessage = stringResource(R.string.search_thread_not_found)
    val inboxListState = rememberLazyListState()
    var inboxVisibleLimit by remember(category) { mutableIntStateOf(MESSAGE_INBOX_PAGE_SIZE) }
    val activeInboxFullSize = visibleThreads.size
    val pagedThreads =
        remember(visibleThreads, inboxVisibleLimit) {
            visibleThreads.take(minOf(inboxVisibleLimit, visibleThreads.size))
        }

    LaunchedEffect(activeInboxFullSize) {
        when {
            activeInboxFullSize == 0 -> inboxVisibleLimit = 0
            inboxVisibleLimit > activeInboxFullSize -> inboxVisibleLimit = activeInboxFullSize
            inboxVisibleLimit == 0 -> inboxVisibleLimit = minOf(MESSAGE_INBOX_PAGE_SIZE, activeInboxFullSize)
        }
    }

    LaunchedEffect(category) {
        inboxListState.scrollToItem(0)
    }

    LaunchedEffect(inboxListState, category, activeInboxFullSize, inboxVisibleLimit) {
        snapshotFlow {
            val layout = inboxListState.layoutInfo
            val last = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = layout.totalItemsCount
            last to total
        }
            .distinctUntilChanged()
            .debounce(MESSAGE_INBOX_END_SCROLL_DEBOUNCE_MS)
            .collect { (last, total) ->
                if (activeInboxFullSize == 0) return@collect
                if (inboxVisibleLimit < activeInboxFullSize &&
                    total > 0 &&
                    last >= total - MESSAGE_LIST_SCROLL_THRESHOLD
                ) {
                    inboxVisibleLimit =
                        (inboxVisibleLimit + MESSAGE_INBOX_PAGE_SIZE).coerceAtMost(activeInboxFullSize)
                }
            }
    }

    LaunchedEffect(pendingComposeAddress) {
        val raw = pendingComposeAddress?.trim()?.takeIf { it.isNotEmpty() } ?: return@LaunchedEffect
        draftThread = draftThreadForAddress(raw)
        openThreadId = null
        category = MessageThreadCategory.All
        onConsumePendingCompose()
    }

    LaunchedEffect(pendingOpenBlankComposer) {
        if (!pendingOpenBlankComposer) return@LaunchedEffect
        draftThread = draftThreadForAddress("")
        openThreadId = null
        category = MessageThreadCategory.All
        onConsumePendingOpenBlankComposer()
    }

    LaunchedEffect(pendingOpenThreadId, threads) {
        val id = pendingOpenThreadId ?: return@LaunchedEffect
        if (threads.isEmpty()) return@LaunchedEffect
        val thread = threads.find { it.id == id }
        if (thread != null) {
            if (thread.isArchived) {
                category = MessageThreadCategory.Archived
            } else if (!thread.matches(category)) {
                category = MessageThreadCategory.All
            }
            openThreadId = id
        } else {
            onThreadAction(threadNotFoundMessage)
        }
        onConsumePendingOpenThread()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            MessageCategoryFilterStrip(
                threads = threads,
                stripCategories = InboxStripCategories,
                selected = category,
                onSelect = { category = it },
            )
            LazyColumn(
                state = inboxListState,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                if (visibleThreads.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.messages_inbox_empty),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                } else {
                    itemsIndexed(pagedThreads, key = { _, t -> t.id }) { index, thread ->
                        Column(Modifier.fillMaxWidth()) {
                            ThreadRow(
                                thread = thread,
                                onOpen = { openThreadId = it.id },
                                messagesViewModel = messagesViewModel,
                            )
                            if (index < pagedThreads.lastIndex) {
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
        }
        overlayThread?.let { t ->
            ThreadDetailOverlay(
                thread = t,
                messagesViewModel = messagesViewModel,
                onDismiss = {
                    openThreadId = null
                    draftThread = null
                },
                onUserMessage = onThreadAction,
                onNavigateToMoney = onNavigateToMoney,
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
                    .clickable(onClick = { PhonIQLaunchRouter.offerBlankSmsCompose() }),
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

private fun draftThreadForAddress(addr: String): MessageThread {
    val peer = addr.trim()
    val id = "sms_$peer"
    return MessageThread(
        id = id,
        title = peer,
        snippet = "",
        timeLabel = "",
        unread = false,
        categories = emptySet(),
        peerAddress = peer,
    )
}

private fun categoryMatchCount(
    threads: Collection<MessageThread>,
    category: MessageThreadCategory,
): Int =
    when (category) {
        MessageThreadCategory.All -> threads.count { !it.isArchived }
        MessageThreadCategory.Archived -> threads.count { it.isArchived }
        MessageThreadCategory.Unread -> threads.count { it.unread && !it.isArchived }
        MessageThreadCategory.Personal -> threads.count { !it.isArchived && MessageThreadCategory.Personal in it.categories }
        MessageThreadCategory.Transaction -> threads.count { !it.isArchived && MessageThreadCategory.Transaction in it.categories }
        MessageThreadCategory.Otp -> threads.count { !it.isArchived && MessageThreadCategory.Otp in it.categories }
        MessageThreadCategory.Bill -> threads.count { !it.isArchived && MessageThreadCategory.Bill in it.categories }
        MessageThreadCategory.Delivery -> threads.count { !it.isArchived && MessageThreadCategory.Delivery in it.categories }
        MessageThreadCategory.Travel -> threads.count { !it.isArchived && MessageThreadCategory.Travel in it.categories }
        MessageThreadCategory.Spam -> threads.count { !it.isArchived && MessageThreadCategory.Spam in it.categories }
    }

@Composable
private fun MessageCategoryFilterStrip(
    threads: Collection<MessageThread>,
    stripCategories: List<MessageThreadCategory>,
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
        val selectedSuffixStr = stringResource(R.string.msg_filter_selected_suffix)
        stripCategories.forEach { c ->
            val count = categoryMatchCount(threads, c)
            val labelStr = inboxStripCategoryLabel(c)
            val semLabel =
                buildString {
                    append(labelStr)
                    append(": ")
                    append(count)
                    if (selected == c) {
                        append(", ")
                        append(selectedSuffixStr)
                    }
                }
            MsgTabChip(
                icon = inboxStripCategoryIcon(c),
                label = labelStr,
                count = count,
                selected = selected == c,
                semanticsLabel = semLabel,
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
    semanticsLabel: String,
    onClick: () -> Unit,
) {
    val borderColor =
        if (selected) PhoniqAccent.copy(alpha = 0.22f) else Color.Transparent
    val bg = if (selected) PhoniqAccent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface
    val fg = if (selected) PhoniqAccent else PhoniqTextSecondaryMock
    val countBg =
        if (selected) PhoniqAccent.copy(alpha = 0.24f) else PhoniqTextSubtle.copy(alpha = 0.22f)
    val countFg = if (selected) PhoniqAccent else PhoniqTextSecondaryMock
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = bg,
        border = BorderStroke(1.dp, borderColor),
        modifier =
            Modifier
                .defaultMinSize(minHeight = 48.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = semanticsLabel
                },
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
private fun inboxStripCategoryIcon(c: MessageThreadCategory): ImageVector =
    when (c) {
        MessageThreadCategory.All -> Icons.Outlined.Inbox
        MessageThreadCategory.Unread -> Icons.Outlined.MarkEmailUnread
        MessageThreadCategory.Archived -> Icons.Outlined.Archive
        else -> Icons.Outlined.Inbox
    }

@Composable
private fun inboxStripCategoryLabel(c: MessageThreadCategory): String =
    when (c) {
        MessageThreadCategory.All -> stringResource(R.string.msg_filter_all)
        MessageThreadCategory.Unread -> stringResource(R.string.msg_filter_unread)
        MessageThreadCategory.Archived -> stringResource(R.string.msg_filter_archived)
        else -> stringResource(R.string.msg_filter_all)
    }

@Composable
private fun ThreadRow(
    thread: MessageThread,
    onOpen: (MessageThread) -> Unit,
    messagesViewModel: MessagesViewModel,
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
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(g0, g1))),
                    contentAlignment = Alignment.Center,
                ) {
                    AvatarInitialsText(
                        text = initial,
                        fontSize = 18.sp,
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
                        if (thread.isPinned) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PhoniqAccent.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, PhoniqAccent.copy(alpha = 0.3f)),
                            ) {
                                Text(
                                    stringResource(R.string.thread_pin_chip),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PhoniqAccent,
                                )
                            }
                        }
                        thread.subtitleBadge?.let { badge ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x2200D4AA),
                                border = BorderStroke(1.dp, Color(0x4400D4AA)),
                            ) {
                                Text(
                                    badge,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00D4AA),
                                )
                            }
                        }
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
                val peer = thread.peerAddress
                if (!peer.isNullOrBlank() && peer != thread.title) {
                    Text(
                        peer,
                        fontSize = 11.sp,
                        color = PhoniqTextSubtle,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp),
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
                    Text(
                        thread.snippet,
                        fontSize = 12.sp,
                        color = previewColor,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    if (thread.unread && thread.unreadCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PhoniqSecondary,
                        ) {
                            Text(
                                if (thread.unreadCount > 99) "99+" else thread.unreadCount.toString(),
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
                    OtpCountdownCopyStrip(
                        code = thread.otpCode,
                        expiresAtEpochMillis = thread.otpExpiresAtEpochMillis ?: 0L,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }
            var menuOpen by remember(thread.id) { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                ) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.cd_overflow_menu),
                        tint = PhoniqTextSubtle,
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (thread.isPinned) {
                                    stringResource(R.string.thread_unpin)
                                } else {
                                    stringResource(R.string.thread_pin)
                                },
                            )
                        },
                        onClick = {
                            menuOpen = false
                            messagesViewModel.setThreadPinned(thread.id, !thread.isPinned)
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (thread.isArchived) {
                                    stringResource(R.string.thread_unarchive)
                                } else {
                                    stringResource(R.string.thread_archive)
                                },
                            )
                        },
                        onClick = {
                            menuOpen = false
                            messagesViewModel.setThreadArchived(thread.id, !thread.isArchived)
                        },
                    )
                }
            }
        }
    }
}
