package com.phoniq.app.ui.phone

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoniq.app.R
import com.phoniq.app.data.mapper.aggregateContactsToRows
import com.phoniq.app.data.model.CallChannel
import com.phoniq.app.data.model.CallDirection
import com.phoniq.app.data.model.ContactRow
import com.phoniq.app.data.model.QuickCallEntry
import com.phoniq.app.data.model.RecentCall
import com.phoniq.app.data.model.toQuickCallStripEntries
import com.phoniq.app.data.model.RecentCallFilter
import com.phoniq.app.data.model.matches
import com.phoniq.app.data.model.toContactProfileRow
import com.phoniq.app.ui.components.AvatarInitialsText
import com.phoniq.app.ui.components.ContactPhotoAvatar
import com.phoniq.app.ui.components.MockupSectionLabel
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqBorder
import com.phoniq.app.ui.theme.PhoniqBorderSoft
import com.phoniq.app.ui.theme.PhoniqSecondary
import com.phoniq.app.ui.theme.PhoniqSurface
import com.phoniq.app.ui.theme.PhoniqSurfaceLow
import com.phoniq.app.ui.theme.PhoniqTextSecondaryMock
import com.phoniq.app.ui.theme.PhoniqTextSubtle
import java.util.Locale
import com.phoniq.app.util.normalizePhoneKey
import com.phoniq.app.util.openBlockedNumbersSettings
import com.phoniq.app.util.placeOutgoingTelCall
import com.phoniq.app.util.startDialer
import com.phoniq.app.util.startSmsCompose
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/** Items per "page" for Recent calls window (initial + each scroll load); matches Messages inbox. */
private const val RECENT_CALLS_PAGE_SIZE = 10
/** Load the next page when the last visible row is within this many items of the list end. */
private const val RECENT_CALLS_SCROLL_LOAD_THRESHOLD = 3
/** Coalesce rapid layout updates so we do not bump the window multiple times per scroll settle. */
private const val RECENT_CALLS_END_SCROLL_DEBOUNCE_MS = 120L

/** Avatar diameter for the 3-column favorites grid only (other grids keep their own sizes). */
private val FavoritesGridAvatarDp = 68.dp
private val FavoritesGridStarBadgeDp = 24.dp
private val FavoritesGridStarIconDp = 14.dp

private enum class PhoneSection {
    Recent,
    Contacts,
    Favorites,
}

@Composable
fun PhoneScreen(
    recents: List<RecentCall>,
    frequentQuickCalls: List<QuickCallEntry>,
    phoneViewModel: PhoneViewModel,
    onUserMessage: (String) -> Unit,
    onAddContact: (displayName: String?, phoneNumber: String?) -> Unit = { _, _ -> },
    onPickFavoriteContact: () -> Unit = {},
    onOpenDialpadFullScreen: () -> Unit = {},
) {
    val context = LocalContext.current
    var section by remember { mutableStateOf(PhoneSection.Recent) }
    var recentFilter by remember { mutableStateOf(RecentCallFilter.All) }
    var selectedContact by remember { mutableStateOf<ContactRow?>(null) }
    var showContactPolicies by remember { mutableStateOf(false) }

    selectedContact?.let { contact ->
        ContactDetailOverlay(
            contact = contact,
            phoneViewModel = phoneViewModel,
            onDismiss = { selectedContact = null },
            onUserMessage = onUserMessage,
            onOpenContactPolicies = { showContactPolicies = true },
        )
    }

    if (showContactPolicies && selectedContact != null) {
        ContactPoliciesBottomSheet(
            contact = selectedContact!!,
            onDismissRequest = { showContactPolicies = false },
        )
    }

    val fabIcon =
        when (section) {
            PhoneSection.Recent -> Icons.Default.Dialpad
            PhoneSection.Contacts -> Icons.Default.PersonAdd
            PhoneSection.Favorites -> Icons.Default.Star
        }
    val fabCd =
        when (section) {
            PhoneSection.Recent -> stringResource(R.string.cd_fab_keypad)
            PhoneSection.Contacts -> stringResource(R.string.cd_fab_add_contact)
            PhoneSection.Favorites -> stringResource(R.string.cd_fab_add_favorite)
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (section) {
                PhoneSection.Recent ->
                    RecentCallsPanel(
                        recents = recents,
                        frequentQuickCalls = frequentQuickCalls,
                        phoneViewModel = phoneViewModel,
                        filter = recentFilter,
                        onFilterChange = { recentFilter = it },
                        onUserMessage = onUserMessage,
                        onAddContact = onAddContact,
                        onOpenRecentProfile = { selectedContact = it.toContactProfileRow() },
                    )
                PhoneSection.Contacts ->
                    ContactsPanel(
                        phoneViewModel = phoneViewModel,
                        onUserMessage = onUserMessage,
                        onContactOpen = { selectedContact = it },
                        onAddFavorite = onPickFavoriteContact,
                    )
                PhoneSection.Favorites ->
                    FavoritesPanel(
                        phoneViewModel = phoneViewModel,
                        onContactOpen = { selectedContact = it },
                        onUserMessage = onUserMessage,
                    )
            }
        }
        DialerBottomBar(
            section = section,
            onSection = { section = it },
            fabIcon = fabIcon,
            fabContentDescription = fabCd,
            onFabClick = {
                when (section) {
                    PhoneSection.Recent -> onOpenDialpadFullScreen()
                    PhoneSection.Contacts -> onAddContact(null, null)
                    PhoneSection.Favorites -> onPickFavoriteContact()
                }
            },
        )
    }
}

@Composable
private fun DialerBottomBar(
    section: PhoneSection,
    onSection: (PhoneSection) -> Unit,
    fabIcon: ImageVector,
    fabContentDescription: String,
    onFabClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, PhoniqBorder),
            ) {
                Row(
                    modifier =
                        Modifier
                            .height(52.dp)
                            .width(180.dp)
                            .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    val tabs =
                        listOf(
                            Triple(PhoneSection.Recent, Icons.Default.History, R.string.phone_tab_recent),
                            Triple(PhoneSection.Contacts, Icons.Filled.Groups, R.string.phone_tab_contacts),
                            Triple(PhoneSection.Favorites, Icons.Default.Star, R.string.phone_tab_favorites),
                        )
                    tabs.forEach { (sec, icon, labelRes) ->
                        val selected = section == sec
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (selected) PhoniqAccent.copy(alpha = 0.2f) else Color.Transparent,
                                    )
                                    .clickable { onSection(sec) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = stringResource(labelRes),
                                modifier = Modifier.size(22.dp),
                                tint = if (selected) PhoniqAccent else PhoniqTextSecondaryMock,
                            )
                        }
                    }
                }
            }
            Box(
                modifier =
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(PhoniqAccent, PhoniqSecondary)))
                        .clickable(onClick = onFabClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    fabIcon,
                    contentDescription = fabContentDescription,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

private fun RecentCall.canAddToDeviceContacts(): Boolean =
    channel == CallChannel.Pstn && !isSpam && numberOrLabel.isNotBlank() && !hasDeviceContact

/** Name to pre-fill when it is not just the raw number (e.g. caller ID). */
private fun RecentCall.editorContactNameOrNull(): String? {
    if (!canAddToDeviceContacts()) return null
    return if (normalizePhoneKey(contactName) == normalizePhoneKey(numberOrLabel)) {
        null
    } else {
        contactName.trim().takeIf { it.isNotEmpty() }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun RecentCallsPanel(
    recents: List<RecentCall>,
    frequentQuickCalls: List<QuickCallEntry>,
    phoneViewModel: PhoneViewModel,
    filter: RecentCallFilter,
    onFilterChange: (RecentCallFilter) -> Unit,
    onUserMessage: (String) -> Unit,
    onAddContact: (displayName: String?, phoneNumber: String?) -> Unit,
    onOpenRecentProfile: (RecentCall) -> Unit,
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val filtered = recents.filter { it.matches(filter) }
    val recentListState = rememberLazyListState()
    var recentVisibleCount by remember(filter) { mutableIntStateOf(RECENT_CALLS_PAGE_SIZE) }
    // Full recent list is built in PhoneViewModel (in-memory); compose only a growing window — same pattern as Messages inbox.
    val pagedRecents =
        remember(filtered, recentVisibleCount) {
            filtered.take(minOf(recentVisibleCount, filtered.size))
        }

    val activeRecentFullSize = filtered.size
    LaunchedEffect(activeRecentFullSize) {
        when {
            activeRecentFullSize == 0 -> recentVisibleCount = 0
            recentVisibleCount > activeRecentFullSize -> recentVisibleCount = activeRecentFullSize
            recentVisibleCount == 0 ->
                recentVisibleCount = minOf(RECENT_CALLS_PAGE_SIZE, activeRecentFullSize)
        }
    }

    LaunchedEffect(filter) {
        recentListState.scrollToItem(0)
    }

    LaunchedEffect(recentListState, filter, activeRecentFullSize, recentVisibleCount) {
        snapshotFlow {
            val layout = recentListState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = layout.totalItemsCount
            lastVisible to total
        }
            .distinctUntilChanged()
            .debounce(RECENT_CALLS_END_SCROLL_DEBOUNCE_MS)
            .collect { (lastVisible, total) ->
                if (filtered.isEmpty()) return@collect
                if (recentVisibleCount < filtered.size &&
                    total > 0 &&
                    lastVisible >= total - RECENT_CALLS_SCROLL_LOAD_THRESHOLD
                ) {
                    recentVisibleCount =
                        (recentVisibleCount + RECENT_CALLS_PAGE_SIZE).coerceAtMost(filtered.size)
                }
            }
    }

    val quickEntries =
        remember(frequentQuickCalls, recents) {
            frequentQuickCalls.ifEmpty { recents.toQuickCallStripEntries() }
        }

    Column(modifier = Modifier.fillMaxSize()) {
        if (quickEntries.isNotEmpty()) {
            QuickCallStrip(
                entries = quickEntries,
                onQuickCall = { entry: QuickCallEntry ->
                    if (!context.startDialer(entry.dialableNumber)) {
                        onUserMessage(context.getString(R.string.toast_dial_failed))
                    }
                },
            )
        }
        MockupSectionLabel(text = stringResource(R.string.phone_section_recent_calls))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scroll)
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RecentCallFilter.entries.forEach { f ->
                RecentFilterChip(
                    label = recentFilterLabel(f),
                    selected = filter == f,
                    onClick = { onFilterChange(f) },
                )
            }
        }
        if (recents.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.phone_recent_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        } else if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.phone_recent_empty_filtered),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        } else {
            LazyColumn(
                state = recentListState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                itemsIndexed(
                    pagedRecents,
                    key = { _, call -> call.id },
                ) { index, call ->
                    Column(Modifier.fillMaxWidth()) {
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                                thickness = 1.dp,
                                color = PhoniqBorderSoft,
                            )
                        }
                        RecentCallRow(
                            call = call,
                            phoneViewModel = phoneViewModel,
                            onUserMessage = onUserMessage,
                            onAddContact = onAddContact,
                            onOpenProfile = { onOpenRecentProfile(call) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun recentFilterLabel(filter: RecentCallFilter): String =
    when (filter) {
        RecentCallFilter.All -> stringResource(R.string.filter_all)
        RecentCallFilter.Missed -> stringResource(R.string.filter_missed)
        RecentCallFilter.Incoming -> stringResource(R.string.filter_incoming)
        RecentCallFilter.Outgoing -> stringResource(R.string.filter_outgoing)
        RecentCallFilter.Rejected -> stringResource(R.string.filter_rejected)
    }

@Composable
private fun RecentFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) PhoniqAccent.copy(alpha = 0.45f) else PhoniqBorder
    val bg = if (selected) PhoniqAccent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface
    val fg = if (selected) PhoniqAccent else PhoniqTextSecondaryMock
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg,
        )
    }
}

@Composable
private fun RecentCallRow(
    call: RecentCall,
    phoneViewModel: PhoneViewModel,
    onUserMessage: (String) -> Unit,
    onAddContact: (displayName: String?, phoneNumber: String?) -> Unit,
    onOpenProfile: () -> Unit,
) {
    val spamKeys by phoneViewModel.spamNumberKeysState.collectAsState()
    var menuOpen by remember(call.id) { mutableStateOf(false) }
    val spamKey = normalizePhoneKey(call.numberOrLabel)
    val inSpamDb = spamKey in spamKeys
    val context = LocalContext.current
    val spamNumberAvatar = call.isSpam && call.contactName.startsWith("+")
    val (dirIcon, dirTint) =
        when (call.direction) {
            CallDirection.Incoming -> Icons.AutoMirrored.Filled.CallReceived to Color(0xFF00D4AA)
            CallDirection.Outgoing -> Icons.AutoMirrored.Filled.CallMade to PhoniqAccent
            CallDirection.Missed,
            CallDirection.Rejected,
            -> Icons.AutoMirrored.Filled.CallMissed to Color(0xFFFF5050)
        }
    val cap = call.metaCaption ?: synthesizedMetaCaption(call)
    val pstnDial: () -> Unit = {
        when (call.channel) {
            CallChannel.Pstn -> {
                if (!context.startDialer(call.numberOrLabel)) {
                    onUserMessage(context.getString(R.string.toast_dial_failed))
                }
            }
            CallChannel.WhatsAppVoice ->
                onUserMessage(context.getString(R.string.toast_wa_voice_handoff, call.contactName))
            CallChannel.WhatsAppVideo ->
                onUserMessage(context.getString(R.string.toast_wa_video_handoff, call.contactName))
        }
    }
    /** Phone icon on recents: place call via Telecom when allowed; dialpad prefilled only as fallback. */
    val recentsCallIconClick: () -> Unit = {
        when (call.channel) {
            CallChannel.Pstn -> {
                if (!context.placeOutgoingTelCall(call.numberOrLabel) &&
                    !context.startDialer(call.numberOrLabel)
                ) {
                    onUserMessage(context.getString(R.string.toast_dial_failed))
                }
            }
            CallChannel.WhatsAppVoice ->
                onUserMessage(context.getString(R.string.toast_wa_voice_handoff, call.contactName))
            CallChannel.WhatsAppVideo ->
                onUserMessage(context.getString(R.string.toast_wa_video_handoff, call.contactName))
        }
    }
    val redialIcon: ImageVector =
        when (call.channel) {
            CallChannel.WhatsAppVideo -> Icons.Filled.Videocam
            CallChannel.WhatsAppVoice,
            CallChannel.Pstn,
            -> Icons.Default.Phone
        }
    val redialCd: Int =
        when (call.channel) {
            CallChannel.WhatsAppVideo -> R.string.cd_wa_video
            CallChannel.WhatsAppVoice -> R.string.cd_wa_voice
            CallChannel.Pstn -> R.string.cd_redial
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val g0 = Color(call.avatarStartArgb.toInt())
        val g1 = Color(call.avatarEndArgb.toInt())
        val initial = call.contactName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        Box(
            modifier = Modifier.clickable(onClick = onOpenProfile),
        ) {
            if (spamNumberAvatar) {
                Box(
                    modifier =
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(g0, g1))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            } else if (call.deviceContactId > 0L) {
                ContactPhotoAvatar(
                    deviceContactId = call.deviceContactId,
                    initials = initial,
                    gradientStart = g0,
                    gradientEnd = g1,
                    size = 56.dp,
                    fontSize = 18.sp,
                )
            } else {
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
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Column(
            Modifier
                .weight(1f)
                .clickable(onClick = pstnDial),
        ) {
            if (call.isBlocked && !spamNumberAvatar) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        call.contactName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF444444).copy(alpha = 0.35f),
                    ) {
                        Text(
                            stringResource(R.string.chip_blocked_call),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFCCCCCC),
                        )
                    }
                }
            } else
            if (call.isSpam && !spamNumberAvatar) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        call.contactName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFF5050).copy(alpha = 0.12f),
                    ) {
                        Text(
                            stringResource(R.string.chip_likely_spam),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF5050),
                        )
                    }
                }
            } else {
                Text(
                    call.contactName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Icon(dirIcon, contentDescription = null, modifier = Modifier.size(16.dp), tint = dirTint)
                Text(
                    cap,
                    fontSize = 12.sp,
                    color = PhoniqTextSecondaryMock,
                )
            }
            Text(
                call.timeLabel,
                fontSize = 11.sp,
                color = PhoniqTextSubtle,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (call.canAddToDeviceContacts()) {
            IconButton(
                onClick = {
                    onAddContact(call.editorContactNameOrNull(), call.numberOrLabel)
                },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = stringResource(R.string.cd_recent_add_contact),
                    modifier = Modifier.size(18.dp),
                    tint = PhoniqTextSubtle,
                )
            }
        }
        Box {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.cd_overflow_menu),
                    modifier = Modifier.size(18.dp),
                    tint = PhoniqTextSubtle,
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                if (!call.isUserTrusted) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.call_menu_mark_trusted)) },
                        onClick = {
                            menuOpen = false
                            phoneViewModel.markTrustedNumber(call.numberOrLabel)
                        },
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.call_menu_clear_trusted)) },
                        onClick = {
                            menuOpen = false
                            phoneViewModel.clearTrustedNumber(call.numberOrLabel)
                        },
                    )
                }
                if (!inSpamDb) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.call_menu_mark_spam)) },
                        onClick = {
                            menuOpen = false
                            phoneViewModel.markSpam(call.numberOrLabel)
                        },
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.call_menu_unmark_spam)) },
                        onClick = {
                            menuOpen = false
                            phoneViewModel.unmarkSpam(call.numberOrLabel)
                        },
                    )
                }
            }
        }
        IconButton(
            onClick = recentsCallIconClick,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                redialIcon,
                contentDescription = stringResource(redialCd),
                modifier = Modifier.size(18.dp),
                tint = PhoniqTextSubtle,
            )
        }
    }
}

@Composable
private fun synthesizedMetaCaption(call: RecentCall): String {
    if (call.direction == CallDirection.Missed) {
        return when {
            call.missedStreak > 1 ->
                stringResource(R.string.recent_meta_missed_streak, call.missedStreak)
            else -> stringResource(R.string.recent_meta_missed_plain)
        }
    }
    val dir = directionLabel(call.direction)
    val channel =
        when (call.channel) {
            CallChannel.Pstn -> stringResource(R.string.recent_channel_phone)
            CallChannel.WhatsAppVoice -> stringResource(R.string.recent_channel_wa_audio)
            CallChannel.WhatsAppVideo -> stringResource(R.string.recent_channel_wa_video)
        }
    return stringResource(R.string.recent_meta_synth, dir, channel)
}

@Composable
private fun directionLabel(direction: CallDirection): String =
    when (direction) {
        CallDirection.Incoming -> stringResource(R.string.direction_incoming)
        CallDirection.Outgoing -> stringResource(R.string.direction_outgoing)
        CallDirection.Missed -> stringResource(R.string.direction_missed)
        CallDirection.Rejected -> stringResource(R.string.direction_rejected)
    }

private data class ContactBucket(val letter: String, val rows: List<ContactRow>)

private fun contactBuckets(rows: List<ContactRow>): List<ContactBucket> {
    val grouped =
        rows.groupBy { r ->
            val ch = r.name.trim().firstOrNull()?.uppercaseChar()
            when {
                ch == null -> "#"
                ch.isLetter() -> ch.toString()
                else -> "#"
            }
        }
    return grouped.entries
        .sortedWith(compareBy({ if (it.key == "#") "\uFFFF" else it.key }, { it.key }))
        .map { (letter, list) ->
            ContactBucket(letter, list.sortedBy { it.name.lowercase(Locale.getDefault()) })
        }
}

@Composable
private fun ContactFavoritesStrip(
    favorites: List<ContactRow>,
    onOpen: (ContactRow) -> Unit,
    onAddFavorite: () -> Unit,
) {
    val rowCd = stringResource(R.string.cd_fav_chip_row)
    LazyRow(
        modifier = Modifier.semantics { contentDescription = rowCd },
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(favorites, key = { it.id }) { row ->
            val shortName = row.name.split(" ").firstOrNull() ?: row.name
            val g0 = Color(row.avatarStartArgb.toInt())
            val g1 = Color(row.avatarEndArgb.toInt())
            val initial = row.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .widthIn(min = 62.dp)
                        .clickable { onOpen(row) },
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(42.dp)
                            .shadow(
                                elevation = 3.dp,
                                shape = CircleShape,
                                spotColor = Color.Black.copy(alpha = 0.2f),
                                ambientColor = Color.Black.copy(alpha = 0.06f),
                            )
                            .clip(CircleShape),
                ) {
                    if (row.deviceContactId > 0L) {
                        ContactPhotoAvatar(
                            deviceContactId = row.deviceContactId,
                            initials = initial,
                            gradientStart = g0,
                            gradientEnd = g1,
                            size = 42.dp,
                            fontSize = 16.sp,
                        )
                    } else {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(listOf(g0, g1))),
                            contentAlignment = Alignment.Center,
                        ) {
                            AvatarInitialsText(
                                text = initial,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Text(
                    shortName,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier =
                    Modifier
                        .widthIn(min = 62.dp)
                        .clickable(onClick = onAddFavorite),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PhoniqSurface)
                            .border(1.dp, PhoniqBorder, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = PhoniqAccent, modifier = Modifier.size(20.dp))
                }
                Text(
                    stringResource(R.string.contacts_fav_add_chip),
                    fontSize = 11.sp,
                    color = PhoniqTextSecondaryMock,
                )
            }
        }
    }
}

@Composable
private fun ContactsPanel(
    phoneViewModel: PhoneViewModel,
    onUserMessage: (String) -> Unit,
    onContactOpen: (ContactRow) -> Unit,
    onAddFavorite: () -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val filteredEntities by phoneViewModel.filteredContacts.collectAsState()
    val starredEntities by phoneViewModel.starredContacts.collectAsState()
    val favoriteChips =
        remember(starredEntities) { starredEntities.aggregateContactsToRows() }
    val filtered = remember(filteredEntities) { filteredEntities.aggregateContactsToRows() }
    val buckets = remember(filtered) { contactBuckets(filtered) }

    LaunchedEffect(Unit) {
        query = ""
        phoneViewModel.setContactSearch("")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        item {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, top = 8.dp, end = 14.dp, bottom = 4.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        phoneViewModel.setContactSearch(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.contacts_search_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        TextFieldDefaults.colors(
                            unfocusedContainerColor = PhoniqSurface,
                            focusedContainerColor = PhoniqSurface,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                        ),
                )
            }
        }
        if (favoriteChips.isNotEmpty() && query.isBlank()) {
            item {
                ContactFavoritesStrip(
                    favorites = favoriteChips,
                    onOpen = onContactOpen,
                    onAddFavorite = onAddFavorite,
                )
            }
        }
        when {
            filtered.isNotEmpty() -> {
                item {
                    MockupSectionLabel(text = stringResource(R.string.contacts_all_label))
                }
                for (bucket in buckets) {
                    item(key = "hdr_${bucket.letter}") {
                        Text(
                            bucket.letter,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
                            color = PhoniqAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    items(bucket.rows, key = { it.id }) { row ->
                        ContactRowItem(
                            row = row,
                            onUserMessage = onUserMessage,
                            onOpen = { onContactOpen(row) },
                        )
                    }
                }
            }
            query.isNotBlank() -> {
                item {
                    Text(
                        text = stringResource(R.string.contacts_no_matches),
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            else -> {
                item {
                    Text(
                        text = stringResource(R.string.contacts_empty_device),
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
        item {
            BlockedSection(onUserMessage = onUserMessage)
        }
    }
}

@Composable
private fun BlockedSection(onUserMessage: (String) -> Unit) {
    val context = LocalContext.current
    val openBlocked: () -> Unit = {
        if (context.openBlockedNumbersSettings()) {
            onUserMessage(context.getString(R.string.after_call_blocked_numbers_screen))
        } else {
            onUserMessage(context.getString(R.string.after_call_blocked_numbers_unavailable))
        }
    }
    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    ListItem(
        modifier = Modifier.clickable(onClick = openBlocked),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        headlineContent = { Text(stringResource(R.string.contacts_blocked_label)) },
        supportingContent = { Text(stringResource(R.string.contacts_blocked_sub)) },
        trailingContent = {
            IconButton(onClick = openBlocked) {
                Icon(
                    Icons.Default.Block,
                    contentDescription = stringResource(R.string.cd_contacts_open_blocked),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

@Composable
private fun ContactRowItem(
    row: ContactRow,
    onUserMessage: (String) -> Unit,
    onOpen: () -> Unit,
) {
    val context = LocalContext.current
    val initial = row.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val g0 = Color(row.avatarStartArgb.toInt())
    val g1 = Color(row.avatarEndArgb.toInt())
    ListItem(
        modifier = Modifier.clickable { onOpen() },
        leadingContent = {
            Box(modifier = Modifier.size(44.dp)) {
                if (row.deviceContactId > 0L) {
                    ContactPhotoAvatar(
                        deviceContactId = row.deviceContactId,
                        initials = initial,
                        gradientStart = g0,
                        gradientEnd = g1,
                        size = 44.dp,
                        fontSize = 16.sp,
                    )
                } else {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(g0, g1))),
                        contentAlignment = Alignment.Center,
                    ) {
                        AvatarInitialsText(
                            text = initial,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
        headlineContent = { Text(row.name) },
        supportingContent = {
            Column {
                Text(
                    row.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
                row.riskNote?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        trailingContent = {
            Row {
                IconButton(onClick = {
                    val num = row.detailNumber ?: row.subtitle
                    if (!context.startDialer(num)) {
                        onUserMessage(context.getString(R.string.toast_dial_failed))
                    }
                }) {
                    Icon(Icons.Default.Phone, contentDescription = stringResource(R.string.cd_call_contact))
                }
                IconButton(onClick = {
                    val num = row.detailNumber ?: row.subtitle
                    if (!context.startSmsCompose(num)) {
                        onUserMessage(context.getString(R.string.snackbar_no_sms_app))
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = stringResource(R.string.cd_message_contact))
                }
            }
        },
    )
}

@Composable
private fun FavoriteContactGridCell(
    row: ContactRow,
    onOpenDetail: () -> Unit,
    onRemoveFavorite: () -> Unit,
) {
    val initial = row.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val g0 = Color(row.avatarStartArgb.toInt())
    val g1 = Color(row.avatarEndArgb.toInt())
    val removeFavoriteCd = stringResource(R.string.cd_favorites_remove_star)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = 4.dp, end = 4.dp, bottom = 8.dp),
        ) {
            Box(modifier = Modifier.size(FavoritesGridAvatarDp)) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .size(FavoritesGridAvatarDp)
                            .shadow(
                                elevation = 4.dp,
                                shape = CircleShape,
                                spotColor = Color.Black.copy(alpha = 0.22f),
                                ambientColor = Color.Black.copy(alpha = 0.08f),
                            )
                            .clip(CircleShape)
                            .clickable(onClick = onOpenDetail),
                ) {
                    if (row.deviceContactId > 0L) {
                        ContactPhotoAvatar(
                            deviceContactId = row.deviceContactId,
                            initials = initial,
                            gradientStart = g0,
                            gradientEnd = g1,
                            size = FavoritesGridAvatarDp,
                            fontSize = 22.sp,
                        )
                    } else {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(listOf(g0, g1))),
                            contentAlignment = Alignment.Center,
                        ) {
                            AvatarInitialsText(
                                text = initial,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(FavoritesGridStarBadgeDp)
                            .clip(CircleShape)
                            .background(PhoniqSurfaceLow)
                            .border(1.dp, PhoniqBorder, CircleShape)
                            .clickable(onClick = onRemoveFavorite)
                            .semantics { contentDescription = removeFavoriteCd },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(FavoritesGridStarIconDp),
                        tint = PhoniqAccent,
                    )
                }
            }
            Text(
                text = row.name,
                fontSize = 11.sp,
                lineHeight = 13.75.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenDetail),
            )
        }
    }
}

@Composable
private fun FavoritesPanel(
    phoneViewModel: PhoneViewModel,
    onContactOpen: (ContactRow) -> Unit,
    onUserMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val starredEntities by phoneViewModel.starredContacts.collectAsState()
    val favorites =
        remember(starredEntities) { starredEntities.aggregateContactsToRows() }
    Column(modifier = Modifier.fillMaxSize()) {
        MockupSectionLabel(text = stringResource(R.string.phone_tab_favorites))
        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.favorites_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(favorites, key = { it.id }) { row ->
                    FavoriteContactGridCell(
                        row = row,
                        onOpenDetail = { onContactOpen(row) },
                        onRemoveFavorite = {
                            phoneViewModel.unstarDeviceContact(row.deviceContactId) { ok ->
                                if (!ok) {
                                    onUserMessage(context.getString(R.string.snackbar_favorite_failed))
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
