package com.phoniq.app.ui.phone

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.unit.Dp
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
import com.phoniq.app.data.model.toContactRowForDialPolicy
import com.phoniq.app.data.model.RecentCallFilter
import com.phoniq.app.data.model.matches
import com.phoniq.app.data.model.toContactProfileRow
import com.phoniq.app.data.model.effectivePhoneNumbers
import com.phoniq.app.ui.components.AvatarInitialsText
import com.phoniq.app.ui.components.ContactPhotoAvatar
import com.phoniq.app.ui.components.contactAvatarClip
import com.phoniq.app.ui.components.contactAvatarShapeForSize
import com.phoniq.app.ui.components.MockupSectionLabel
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqBorder
import com.phoniq.app.ui.theme.PhoniqSecondary
import com.phoniq.app.ui.theme.PhoniqSurface
import com.phoniq.app.ui.theme.PhoniqTextSecondaryMock
import com.phoniq.app.ui.theme.PhoniqTextSubtle
import java.util.Locale
import com.phoniq.app.util.normalizePhoneKey
import com.phoniq.app.util.openBlockedNumbersSettings
import com.phoniq.app.util.placeOrDial
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

/** Inner avatar diameter inside accent ring on Contacts favorites strip (QuickCall-style polish). */
private val ContactFavStripAvatarDp = 46.dp
private val ContactFavStripTileDp = 52.dp
private val ContactFavStripRingStroke = 2.dp
/** Avatar diameter for the 3-column favorites grid only (other grids keep their own sizes). */
private val FavoritesGridAvatarDp = 76.dp

/** Lazy list bottom inset so rows clear the floating dialer pill + FAB (they sit over the scroll area). */
private val PhoneFloatingDialerClearanceDp = 88.dp

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
    onEditContact: (deviceContactId: Long, displayName: String, phones: List<com.phoniq.app.data.model.ContactPhoneEntry>) -> Unit =
        { _, _, _ -> },
    onPickFavoriteContact: () -> Unit = {},
    onOpenDialpadFullScreen: () -> Unit = {},
    onBulkMergeContacts: (Set<Long>) -> Unit = {},
) {
    val context = LocalContext.current
    var section by remember { mutableStateOf(PhoneSection.Recent) }
    var recentFilter by remember { mutableStateOf(RecentCallFilter.All) }
    var selectedContact by remember { mutableStateOf<ContactRow?>(null) }
    var showContactPolicies by remember { mutableStateOf(false) }
    var recentSelectMode by remember { mutableStateOf(false) }
    var recentSelectedIds by remember { mutableStateOf(setOf<String>()) }
    var contactSelectMode by remember { mutableStateOf(false) }
    var contactSelectedIds by remember { mutableStateOf(setOf<String>()) }
    var recentVisibleIds by remember { mutableStateOf(setOf<String>()) }
    var contactVisibleRowIds by remember { mutableStateOf(setOf<String>()) }
    var contactBulkDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteDeviceContactIds by remember { mutableStateOf<List<Long>>(emptyList()) }

    LaunchedEffect(section) {
        recentSelectMode = false
        recentSelectedIds = emptySet()
        contactSelectMode = false
        contactSelectedIds = emptySet()
    }

    selectedContact?.let { contact ->
        ContactDetailOverlay(
            contact = contact,
            phoneViewModel = phoneViewModel,
            onDismiss = { selectedContact = null },
            onUserMessage = onUserMessage,
            onEditContact = onEditContact,
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

    val phoneLabelByKey by phoneViewModel.phoneLabelByKey.collectAsState()
    val filteredContactEntities by phoneViewModel.filteredContacts.collectAsState()

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listBottomInset = PhoneFloatingDialerClearanceDp + navBottom

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        when (section) {
            PhoneSection.Recent ->
                RecentCallsPanel(
                    recents = recents,
                    frequentQuickCalls = frequentQuickCalls,
                    phoneViewModel = phoneViewModel,
                    phoneLabelByKey = phoneLabelByKey,
                    filter = recentFilter,
                    onFilterChange = { recentFilter = it },
                    onUserMessage = onUserMessage,
                    onAddContact = onAddContact,
                    onOpenRecentProfile = { selectedContact = it.toContactProfileRow() },
                    scrollBottomInset = listBottomInset,
                    selectionMode = recentSelectMode,
                    selectedIds = recentSelectedIds,
                    onToggleRecentSelection = { id ->
                        recentSelectedIds =
                            if (id in recentSelectedIds) recentSelectedIds - id else recentSelectedIds + id
                    },
                    onStartRecentSelection = { call ->
                        recentSelectMode = true
                        recentSelectedIds = setOf(call.id)
                    },
                    onVisibleRecentIdsChange = { recentVisibleIds = it },
                )
            PhoneSection.Contacts ->
                ContactsPanel(
                    phoneViewModel = phoneViewModel,
                    onUserMessage = onUserMessage,
                    onContactOpen = { selectedContact = it },
                    onAddFavorite = onPickFavoriteContact,
                    scrollBottomInset = listBottomInset,
                    selectionMode = contactSelectMode,
                    selectedIds = contactSelectedIds,
                    onToggleContactSelection = { id ->
                        contactSelectedIds =
                            if (id in contactSelectedIds) contactSelectedIds - id else contactSelectedIds + id
                    },
                    onStartContactSelection = { row ->
                        contactSelectMode = true
                        contactSelectedIds = setOf(row.id)
                    },
                    onVisibleContactRowIdsChange = { contactVisibleRowIds = it },
                )
            PhoneSection.Favorites ->
                FavoritesPanel(
                    phoneViewModel = phoneViewModel,
                    onContactOpen = { selectedContact = it },
                    onUserMessage = onUserMessage,
                    onAddFavorite = onPickFavoriteContact,
                    scrollBottomInset = listBottomInset,
                )
        }
        if (section == PhoneSection.Recent && recentSelectMode) {
            PhoneRecentBulkBar(
                count = recentSelectedIds.size,
                visibleRecentsCount = recentVisibleIds.size,
                onDismiss = {
                    recentSelectMode = false
                    recentSelectedIds = emptySet()
                },
                onSelectAllVisible = { recentSelectedIds = recentSelectedIds + recentVisibleIds },
                onTrusted = {
                    val nums =
                        recents.filter { it.id in recentSelectedIds }.map { it.numberOrLabel }.distinct()
                    if (nums.isNotEmpty()) {
                        phoneViewModel.markTrustedBulk(nums)
                        onUserMessage(context.getString(R.string.bulk_trusted_marked))
                    }
                    recentSelectMode = false
                    recentSelectedIds = emptySet()
                },
                onDelete = {
                    val nums =
                        recents
                            .filter { it.id in recentSelectedIds }
                            .map { it.numberOrLabel }
                            .distinct()
                    if (nums.isNotEmpty()) {
                        phoneViewModel.deleteRecentEntries(nums)
                        onUserMessage(context.getString(R.string.call_menu_delete_history_done))
                    }
                    recentSelectMode = false
                    recentSelectedIds = emptySet()
                },
                onSpam = {
                    val nums =
                        recents.filter { it.id in recentSelectedIds }.map { it.numberOrLabel }.distinct()
                    phoneViewModel.markSpamBulk(nums)
                    recentSelectMode = false
                    recentSelectedIds = emptySet()
                },
                onBlock = {
                    if (context.openBlockedNumbersSettings()) {
                        onUserMessage(context.getString(R.string.after_call_blocked_numbers_screen))
                    } else {
                        onUserMessage(context.getString(R.string.after_call_blocked_numbers_unavailable))
                    }
                },
                onShare = {
                    val nums =
                        recents.filter { it.id in recentSelectedIds }.map { it.numberOrLabel }.distinct()
                    if (nums.isNotEmpty()) context.shareNumbersLines(nums)
                },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, bottom = PhoneFloatingDialerClearanceDp + 6.dp),
            )
        }
        if (section == PhoneSection.Contacts && contactSelectMode) {
            PhoneContactsBulkBar(
                count = contactSelectedIds.size,
                visibleContactsCount = contactVisibleRowIds.size,
                onDismiss = {
                    contactSelectMode = false
                    contactSelectedIds = emptySet()
                },
                onSelectAllVisible = {
                    contactSelectedIds = contactSelectedIds + contactVisibleRowIds
                },
                onStar = {
                    val rows =
                        filteredContactEntities.aggregateContactsToRows().filter { it.id in contactSelectedIds }
                    val ids = rows.mapNotNull { r -> r.deviceContactId.takeIf { it > 0L } }.distinct()
                    phoneViewModel.starDeviceContactsBulk(ids) { n ->
                        onUserMessage(context.getString(R.string.bulk_contacts_starred, n))
                    }
                    contactSelectMode = false
                    contactSelectedIds = emptySet()
                },
                onTrusted = {
                    val rows =
                        filteredContactEntities.aggregateContactsToRows().filter { it.id in contactSelectedIds }
                    val nums = rows.flatMap { it.effectivePhoneNumbers() }.distinct()
                    if (nums.isNotEmpty()) {
                        phoneViewModel.markTrustedBulk(nums)
                        onUserMessage(context.getString(R.string.bulk_trusted_marked))
                    }
                    contactSelectMode = false
                    contactSelectedIds = emptySet()
                },
                onMerge = {
                    val rows =
                        filteredContactEntities.aggregateContactsToRows().filter { it.id in contactSelectedIds }
                    val ids = rows.mapNotNull { r -> r.deviceContactId.takeIf { it > 0L } }.toSet()
                    if (ids.isEmpty()) {
                        onUserMessage(context.getString(R.string.bulk_merge_none))
                    } else {
                        onBulkMergeContacts(ids)
                    }
                },
                onDelete = {
                    val rows =
                        filteredContactEntities.aggregateContactsToRows().filter { it.id in contactSelectedIds }
                    val ids = rows.mapNotNull { r -> r.deviceContactId.takeIf { it > 0L } }.distinct()
                    if (ids.isEmpty()) {
                        onUserMessage(context.getString(R.string.bulk_delete_contacts_none_device))
                    } else {
                        pendingDeleteDeviceContactIds = ids
                        contactBulkDeleteDialog = true
                    }
                },
                onSpam = {
                    val rows =
                        filteredContactEntities.aggregateContactsToRows().filter { it.id in contactSelectedIds }
                    val nums = rows.flatMap { it.effectivePhoneNumbers() }.distinct()
                    phoneViewModel.markSpamBulk(nums)
                    contactSelectMode = false
                    contactSelectedIds = emptySet()
                },
                onBlock = {
                    if (context.openBlockedNumbersSettings()) {
                        onUserMessage(context.getString(R.string.after_call_blocked_numbers_screen))
                    } else {
                        onUserMessage(context.getString(R.string.after_call_blocked_numbers_unavailable))
                    }
                },
                onShare = {
                    val rows =
                        filteredContactEntities.aggregateContactsToRows().filter { it.id in contactSelectedIds }
                    val lines = rows.flatMap { r -> listOf(r.name) + r.effectivePhoneNumbers() }.distinct()
                    if (lines.isNotEmpty()) context.shareNumbersLines(lines)
                },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, bottom = PhoneFloatingDialerClearanceDp + 6.dp),
            )
        }
        if (contactBulkDeleteDialog && pendingDeleteDeviceContactIds.isNotEmpty()) {
            val nDel = pendingDeleteDeviceContactIds.size
            AlertDialog(
                onDismissRequest = {
                    contactBulkDeleteDialog = false
                    pendingDeleteDeviceContactIds = emptyList()
                },
                title = { Text(stringResource(R.string.bulk_delete_contacts_confirm_title)) },
                text = { Text(stringResource(R.string.bulk_delete_contacts_confirm_body, nDel)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val toDel = pendingDeleteDeviceContactIds
                            contactBulkDeleteDialog = false
                            pendingDeleteDeviceContactIds = emptyList()
                            contactSelectMode = false
                            contactSelectedIds = emptySet()
                            phoneViewModel.deleteDeviceContactsBulk(toDel) { deleted ->
                                onUserMessage(context.getString(R.string.bulk_delete_contacts_done, deleted))
                                phoneViewModel.enqueueContactsRefresh()
                            }
                        },
                    ) {
                        Text(stringResource(R.string.bulk_delete))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            contactBulkDeleteDialog = false
                            pendingDeleteDeviceContactIds = emptyList()
                        },
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }
        DialerBottomBar(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 8.dp),
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
    modifier: Modifier = Modifier,
    section: PhoneSection,
    onSection: (PhoneSection) -> Unit,
    fabIcon: ImageVector,
    fabContentDescription: String,
    onFabClick: () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
                tonalElevation = 2.dp,
                shadowElevation = 8.dp,
                border =
                    BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                    ),
            ) {
                Row(
                    modifier =
                        Modifier
                            .height(54.dp)
                            .width(190.dp)
                            .padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (selected) PhoniqAccent.copy(alpha = 0.26f) else Color.Transparent,
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
                        .size(54.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(22.dp),
                            spotColor = PhoniqAccent.copy(alpha = 0.4f),
                            ambientColor = Color.Black.copy(alpha = 0.22f),
                        )
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(PhoniqAccent, PhoniqSecondary)))
                        .clickable(onClick = onFabClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    fabIcon,
                    contentDescription = fabContentDescription,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
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
    phoneLabelByKey: Map<String, String>,
    filter: RecentCallFilter,
    onFilterChange: (RecentCallFilter) -> Unit,
    onUserMessage: (String) -> Unit,
    onAddContact: (displayName: String?, phoneNumber: String?) -> Unit,
    onOpenRecentProfile: (RecentCall) -> Unit,
    scrollBottomInset: Dp,
    selectionMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onToggleRecentSelection: (String) -> Unit = {},
    onStartRecentSelection: (RecentCall) -> Unit = {},
    onVisibleRecentIdsChange: (Set<String>) -> Unit = {},
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

    LaunchedEffect(pagedRecents) {
        onVisibleRecentIdsChange(pagedRecents.map { it.id }.toSet())
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
        remember(frequentQuickCalls, recents, phoneLabelByKey) {
            frequentQuickCalls.ifEmpty {
                recents.toQuickCallStripEntries(phoneLabelByKey = phoneLabelByKey)
            }
        }

    val emptyAllPadding = PaddingValues(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 24.dp)
    val listHorizontalPadding = PaddingValues(start = 8.dp, end = 8.dp)
    val nowMs = remember(pagedRecents) { System.currentTimeMillis() }

    LazyColumn(
        state = recentListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 12.dp + scrollBottomInset),
    ) {
        if (quickEntries.isNotEmpty()) {
            item(key = "recent_quick_strip", contentType = "header") {
                QuickCallStrip(
                    entries = quickEntries,
                    onQuickCall = { entry: QuickCallEntry ->
                        if (!context.placeOrDial(entry.dialableNumber, entry.toContactRowForDialPolicy())) {
                            onUserMessage(context.getString(R.string.toast_dial_failed))
                        }
                    },
                )
            }
        }
        item(key = "recent_section_label", contentType = "header") {
            MockupSectionLabel(
                text = stringResource(R.string.phone_section_recent_calls),
                topPadding = if (quickEntries.isNotEmpty()) 2.dp else 8.dp,
                bottomPadding = 4.dp,
                letterSpacing = 0.95.sp,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.84f),
            )
        }
        item(key = "recent_filters", contentType = "header") {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scroll)
                        .padding(start = 14.dp, end = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RecentCallFilter.entries.forEach { f ->
                    RecentFilterChip(
                        label = recentFilterLabel(f),
                        selected = filter == f,
                        onClick = { onFilterChange(f) },
                    )
                }
            }
        }
        
        when {
            recents.isEmpty() ->
                item(key = "recent_empty_all", contentType = "empty") {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 280.dp)
                                .padding(emptyAllPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.phone_recent_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            filtered.isEmpty() ->
                item(key = "recent_empty_filtered", contentType = "empty") {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 280.dp)
                                .padding(emptyAllPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.phone_recent_empty_filtered),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            else -> {
                var lastBucket: RecentDateBucket? = null
                pagedRecents.forEach { call ->
                    val bucket = recentDateBucket(call.timestampMs, nowMs)
                    if (bucket != lastBucket) {
                        lastBucket = bucket
                        item(
                            key = "recent_header_${bucket.name}",
                            contentType = "recent_header",
                        ) {
                            RecentDateHeader(bucket)
                        }
                    }
                    item(
                        key = "recent_${call.id}",
                        contentType = "recent_call",
                    ) {
                        Box(Modifier.padding(listHorizontalPadding)) {
                            Surface(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 5.dp),
                                shape = RoundedCornerShape(16.dp),
                                color =
                                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                                        alpha = 0.42f,
                                    ),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                                border =
                                    BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.09f),
                                    ),
                            ) {
                                RecentCallRow(
                                    call = call,
                                    phoneLabelByKey = phoneLabelByKey,
                                    phoneViewModel = phoneViewModel,
                                    onUserMessage = onUserMessage,
                                    onAddContact = onAddContact,
                                    onOpenProfile = { onOpenRecentProfile(call) },
                                    selectionMode = selectionMode,
                                    selected = call.id in selectedIds,
                                    onToggleSelected = { onToggleRecentSelection(call.id) },
                                    onStartSelectionForThis = { onStartRecentSelection(call) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class RecentDateBucket {
    Today,
    Yesterday,
    ThisWeek,
    Older,
}

private fun recentDateBucket(timestampMs: Long, nowMs: Long): RecentDateBucket {
    if (timestampMs <= 0L) return RecentDateBucket.Older
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = nowMs
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    val startOfToday = cal.timeInMillis
    val startOfYesterday = startOfToday - 24L * 60L * 60L * 1000L
    val startOfWeek = startOfToday - 7L * 24L * 60L * 60L * 1000L
    return when {
        timestampMs >= startOfToday -> RecentDateBucket.Today
        timestampMs >= startOfYesterday -> RecentDateBucket.Yesterday
        timestampMs >= startOfWeek -> RecentDateBucket.ThisWeek
        else -> RecentDateBucket.Older
    }
}

@Composable
private fun RecentDateHeader(bucket: RecentDateBucket) {
    val resId =
        when (bucket) {
            RecentDateBucket.Today -> R.string.recent_section_today
            RecentDateBucket.Yesterday -> R.string.recent_section_yesterday
            RecentDateBucket.ThisWeek -> R.string.recent_section_this_week
            RecentDateBucket.Older -> R.string.recent_section_older
        }
    Text(
        text = stringResource(resId),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 14.dp, top = 12.dp, bottom = 2.dp),
    )
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
    val borderColor =
        if (selected) PhoniqAccent.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val bg =
        if (selected) PhoniqAccent.copy(alpha = 0.26f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val fg = if (selected) PhoniqAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = if (selected) 2.dp else 0.dp,
        shadowElevation = if (selected) 4.dp else 1.dp,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg,
        )
    }
}

private fun channelIcon(channel: CallChannel): ImageVector =
    when (channel) {
        CallChannel.Pstn -> Icons.Default.Phone
        CallChannel.WhatsAppVoice -> Icons.AutoMirrored.Filled.Message
        CallChannel.WhatsAppVideo -> Icons.Default.Videocam
    }

@Composable
private fun recentChannelLabel(channel: CallChannel): String =
    when (channel) {
        CallChannel.Pstn -> stringResource(R.string.recent_channel_phone)
        CallChannel.WhatsAppVoice -> stringResource(R.string.recent_channel_wa_audio)
        CallChannel.WhatsAppVideo -> stringResource(R.string.recent_channel_wa_video)
    }

@Composable
private fun RecentCallMetaLine(
    call: RecentCall,
    metaText: String,
    dirIcon: ImageVector,
    dirTint: Color,
    contactPhoneLabel: String,
) {
    val metaMuted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
    /**
     * For PSTN rows the **contact phone-type label** (Mobile, Whatsapp, Home, …) reads better
     * than the generic "Phone" channel name. Fall back to the channel label when the contact
     * label is unknown (number not in address book) so we never end up with an empty subtitle.
     */
    val secondaryLabel: String =
        when (call.channel) {
            CallChannel.WhatsAppVoice -> stringResource(R.string.recent_channel_wa_audio)
            CallChannel.WhatsAppVideo -> stringResource(R.string.recent_channel_wa_video)
            CallChannel.Pstn ->
                contactPhoneLabel.ifEmpty { stringResource(R.string.recent_channel_phone) }
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.padding(top = 2.dp),
    ) {
        Icon(
            imageVector = dirIcon,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = dirTint,
        )
        when {
            call.metaCaption != null ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = metaText,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = metaMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "·",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PhoniqTextSubtle,
                    )
                    Icon(
                        imageVector = channelIcon(call.channel),
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = PhoniqTextSecondaryMock.copy(alpha = 0.88f),
                    )
                    Text(
                        text = secondaryLabel,
                        fontSize = 12.sp,
                        color = PhoniqTextSecondaryMock,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            call.direction == CallDirection.Missed || call.direction == CallDirection.Rejected ->
                Text(
                    text = metaText,
                    style = MaterialTheme.typography.bodySmall,
                    color = metaMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            else -> {
                Text(
                    text = directionLabel(call.direction),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = dirTint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "·",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PhoniqTextSubtle,
                )
                Icon(
                    imageVector = channelIcon(call.channel),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = PhoniqTextSecondaryMock.copy(alpha = 0.88f),
                )
                Text(
                    text = secondaryLabel,
                    fontSize = 12.sp,
                    color = PhoniqTextSecondaryMock,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun RecentCallRow(
    call: RecentCall,
    phoneLabelByKey: Map<String, String>,
    phoneViewModel: PhoneViewModel,
    onUserMessage: (String) -> Unit,
    onAddContact: (displayName: String?, phoneNumber: String?) -> Unit,
    onOpenProfile: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onToggleSelected: () -> Unit = {},
    onStartSelectionForThis: () -> Unit = {},
) {
    val spamKeys by phoneViewModel.spamNumberKeysState.collectAsState()
    var menuOpen by remember(call.id) { mutableStateOf(false) }
    val spamKey = normalizePhoneKey(call.numberOrLabel)
    val inSpamDb = spamKey in spamKeys
    val context = LocalContext.current
    val spamNumberAvatar = call.isSpam && call.contactName.startsWith("+")
    val contactPhoneLabel = phoneLabelByKey[spamKey].orEmpty()
    val (dirIcon, dirTint) =
        when (call.direction) {
            CallDirection.Incoming -> Icons.AutoMirrored.Filled.CallReceived to Color(0xFF00D4AA)
            CallDirection.Outgoing -> Icons.AutoMirrored.Filled.CallMade to PhoniqAccent
            CallDirection.Missed,
            CallDirection.Rejected,
            -> Icons.AutoMirrored.Filled.CallMissed to Color(0xFFFF5050)
        }
    val cap = call.metaCaption ?: synthesizedMetaCaption(call, contactPhoneLabel)
    val pstnDial: () -> Unit = {
        when (call.channel) {
            CallChannel.Pstn -> {
                if (!context.placeOrDial(call.numberOrLabel, call.toContactProfileRow())) {
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
                if (!context.placeOrDial(call.numberOrLabel, call.toContactProfileRow())) {
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
                .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (selectionMode) {
            Icon(
                imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) PhoniqAccent else PhoniqTextSubtle,
                modifier = Modifier.size(22.dp),
            )
        }
        val g0 = Color(call.avatarStartArgb.toInt())
        val g1 = Color(call.avatarEndArgb.toInt())
        val initial = call.contactName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        Box(
            modifier =
                Modifier.combinedClickable(
                    onClick = {
                        if (selectionMode) onToggleSelected() else onOpenProfile()
                    },
                    onLongClick = {
                        if (selectionMode) {
                            onToggleSelected()
                        } else {
                            onStartSelectionForThis()
                        }
                    },
                ),
        ) {
            if (spamNumberAvatar) {
                Box(
                    modifier =
                        Modifier
                            .size(56.dp)
                            .contactAvatarClip(56.dp)
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
                            .contactAvatarClip(56.dp)
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
        Box(modifier = Modifier.weight(1f)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (selectionMode) onToggleSelected() else onOpenProfile()
                        },
                        onLongClick = {
                            if (selectionMode) {
                                onToggleSelected()
                            } else {
                                menuOpen = true
                            }
                        },
                    ),
            ) {
            if (call.isBlocked && !spamNumberAvatar) {
                Text(
                    call.contactName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else if (call.isSpam && !spamNumberAvatar) {
                Row(
                verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                    Text(
                        call.contactName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    RecentCallMetaLine(
                        call = call,
                        metaText = cap,
                        dirIcon = dirIcon,
                        dirTint = dirTint,
                        contactPhoneLabel = contactPhoneLabel,
                    )
                    Text(
                call.timeLabel,
                style = MaterialTheme.typography.labelMedium,
                color = PhoniqTextSubtle,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.call_menu_select_for_bulk)) },
                    onClick = {
                        menuOpen = false
                        onStartSelectionForThis()
                    },
                )
                if (call.canAddToDeviceContacts()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.dialpad_add_contact)) },
                        onClick = {
                            menuOpen = false
                            onAddContact(call.editorContactNameOrNull(), call.numberOrLabel)
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.call_menu_call_back)) },
                    onClick = {
                        menuOpen = false
                        recentsCallIconClick()
                    },
                )
                if (call.channel == CallChannel.Pstn) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.call_menu_send_sms)) },
                        onClick = {
                            menuOpen = false
                            if (!context.startSmsCompose(call.numberOrLabel)) {
                                onUserMessage(context.getString(R.string.snackbar_no_sms_app))
                            }
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.call_menu_view_contact)) },
                    onClick = {
                        menuOpen = false
                        onOpenProfile()
                    },
                )
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
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.call_menu_delete_history),
                            color = Color(0xFFFF5050),
                        )
                    },
                    onClick = {
                        menuOpen = false
                        phoneViewModel.deleteRecentEntry(call.numberOrLabel)
                        onUserMessage(
                            context.getString(R.string.call_menu_delete_history_done),
                        )
                    },
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp),
        ) {
            if (!selectionMode) {
                IconButton(
                    onClick = recentsCallIconClick,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        redialIcon,
                        contentDescription = stringResource(redialCd),
                        modifier = Modifier.size(21.dp),
                        tint = PhoniqAccent.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

@Composable
private fun synthesizedMetaCaption(call: RecentCall, contactPhoneLabel: String): String {
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
            CallChannel.Pstn ->
                contactPhoneLabel.ifEmpty { stringResource(R.string.recent_channel_phone) }
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
private fun ContactsCompactSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                stringResource(R.string.contacts_search_placeholder),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f),
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
        colors =
            OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = PhoniqSurface,
                focusedContainerColor = PhoniqSurface,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
            ),
    )
}

@Composable
private fun ContactsPrivacyBanner() {
    val bannerCd = stringResource(R.string.cd_contacts_privacy_banner)
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = bannerCd },
        shape = RoundedCornerShape(12.dp),
        color = PhoniqAccent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, PhoniqAccent.copy(alpha = 0.20f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = PhoniqAccent.copy(alpha = 0.92f),
            )
            Text(
                text = stringResource(R.string.contacts_privacy_banner),
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
            )
        }
    }
}

@Composable
private fun ContactBucketHeader(letter: String) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background),
    ) {
        Text(
            text = letter,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 5.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.85.sp,
            color = PhoniqAccent,
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        )
    }
}

@Composable
private fun ContactFavoritesStrip(
    favorites: List<ContactRow>,
    onOpen: (ContactRow) -> Unit,
    onAddFavorite: () -> Unit,
) {
    val rowCd = stringResource(R.string.cd_fav_chip_row)
    val ringColor = PhoniqAccent.copy(alpha = 0.30f)
    LazyRow(
        modifier = Modifier.semantics { contentDescription = rowCd },
        contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 4.dp),
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
                        .widthIn(min = 64.dp)
                        .clickable { onOpen(row) },
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                val ringOutline = contactAvatarShapeForSize(ContactFavStripTileDp)
                val avDp = ContactFavStripAvatarDp
                val avShape = contactAvatarShapeForSize(avDp)
                Box(
                    modifier = Modifier.size(ContactFavStripTileDp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(ContactFavStripTileDp)
                                .border(ContactFavStripRingStroke, ringColor, ringOutline),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(avDp)
                                    .shadow(
                                        elevation = 2.dp,
                                        shape = avShape,
                                        spotColor = Color.Black.copy(alpha = 0.14f),
                                        ambientColor = Color.Black.copy(alpha = 0.06f),
                                    )
                                    .contactAvatarClip(avDp),
                        ) {
                            if (row.deviceContactId > 0L) {
                                ContactPhotoAvatar(
                                    deviceContactId = row.deviceContactId,
                                    initials = initial,
                                    gradientStart = g0,
                                    gradientEnd = g1,
                                    size = ContactFavStripAvatarDp,
                                    fontSize = 17.sp,
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
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }
                }
                Text(
                    shortName,
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier =
                    Modifier
                        .widthIn(min = 64.dp)
                        .clickable(onClick = onAddFavorite),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(ContactFavStripTileDp)
                            .clip(CircleShape)
                            .background(PhoniqSurface)
                            .border(1.dp, PhoniqBorder, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = PhoniqAccent,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    stringResource(R.string.contacts_fav_add_chip),
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 11.sp,
                    color = PhoniqTextSecondaryMock,
                )
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun ContactsPanel(
    phoneViewModel: PhoneViewModel,
    onUserMessage: (String) -> Unit,
    onContactOpen: (ContactRow) -> Unit,
    onAddFavorite: () -> Unit,
    scrollBottomInset: Dp,
    selectionMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onToggleContactSelection: (String) -> Unit = {},
    onStartContactSelection: (ContactRow) -> Unit = {},
    onVisibleContactRowIdsChange: (Set<String>) -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    val filteredEntities by phoneViewModel.filteredContacts.collectAsState()
    val starredEntities by phoneViewModel.starredContacts.collectAsState()
    val favoriteChips =
        remember(starredEntities) { starredEntities.aggregateContactsToRows() }
    val filtered = remember(filteredEntities) { filteredEntities.aggregateContactsToRows() }
    LaunchedEffect(filtered) {
        onVisibleContactRowIdsChange(filtered.map { it.id }.toSet())
    }
    val buckets = remember(filtered) { contactBuckets(filtered) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Pre-compute index in the LazyColumn where each bucket header starts so the A–Z rail can
    // jump directly to the right header item via animateScrollToItem.
    val headerIndexByLetter =
        remember(favoriteChips, query, buckets) {
            val map = LinkedHashMap<String, Int>()
            var cursor = 1 // item 0 is the search/privacy header
            if (favoriteChips.isNotEmpty() && query.isBlank()) cursor += 1 // favorites strip
            cursor += 1 // "All" label
            buckets.forEach { bucket ->
                map[bucket.letter] = cursor
                cursor += 1 + bucket.rows.size // header + rows
            }
            map
        }

    LaunchedEffect(Unit) {
        query = ""
        phoneViewModel.setContactSearch("")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 8.dp + scrollBottomInset),
        ) {
            item {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, top = 2.dp, end = 14.dp, bottom = 2.dp),
                ) {
                    ContactsCompactSearchField(
                        query = query,
                        onQueryChange = {
                            query = it
                            phoneViewModel.setContactSearch(it)
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    ContactsPrivacyBanner()
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
                        MockupSectionLabel(
                            text = stringResource(R.string.contacts_all_label),
                            topPadding = 4.dp,
                            bottomPadding = 4.dp,
                            letterSpacing = 0.95.sp,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.84f),
                        )
                    }
                    buckets.forEach { bucket ->
                        stickyHeader(key = "hdr_${bucket.letter}") {
                            ContactBucketHeader(bucket.letter)
                        }
                        items(
                            bucket.rows,
                            key = { it.id },
                        ) { row ->
                            ContactRowItem(
                                row = row,
                                onUserMessage = onUserMessage,
                                onOpen = { onContactOpen(row) },
                                selectionMode = selectionMode,
                                selected = row.id in selectedIds,
                                onToggleSelect = { onToggleContactSelection(row.id) },
                                onStartSelectionForThis = { onStartContactSelection(row) },
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
        if (headerIndexByLetter.size >= 2 && query.isBlank()) {
            ContactsAlphabetRail(
                letters = headerIndexByLetter.keys.toList(),
                onJump = { letter ->
                    val target = headerIndexByLetter[letter] ?: return@ContactsAlphabetRail
                    scope.launch { listState.animateScrollToItem(target) }
                },
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 2.dp),
            )
        }
    }
}

@Composable
private fun ContactsAlphabetRail(
    letters: List<String>,
    onJump: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val railColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            letters.forEach { letter ->
                Text(
                    text = letter,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = railColor,
                    modifier =
                        Modifier
                            .clickable { onJump(letter) }
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ContactRowItem(
    row: ContactRow,
    onUserMessage: (String) -> Unit,
    onOpen: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onStartSelectionForThis: () -> Unit = {},
) {
    val context = LocalContext.current
    var menuOpen by remember(row.id) { mutableStateOf(false) }
    val initial = row.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val g0 = Color(row.avatarStartArgb.toInt())
    val g1 = Color(row.avatarEndArgb.toInt())
    val phoneLine =
        row.detailNumber?.takeIf { it.isNotBlank() }
            ?: row.subtitle.lineSequence().firstOrNull { it.isNotBlank() }
            ?: row.subtitle
    val actionNumber =
        row.detailNumber?.takeIf { it.isNotBlank() }
            ?: row.subtitle.lineSequence().firstOrNull { it.isNotBlank() }
            ?: row.subtitle

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .combinedClickable(
                    onClick = {
                        if (selectionMode) onToggleSelect() else onOpen()
                    },
                    onLongClick = {
                        if (selectionMode) {
                            onToggleSelect()
                        } else {
                            menuOpen = true
                        }
                    },
                ),
        shape = RoundedCornerShape(16.dp),
        color =
            if (selected) {
                PhoniqAccent.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                    alpha = 0.42f,
                )
            },
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border =
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.09f),
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (selectionMode) {
                Icon(
                    imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (selected) PhoniqAccent else PhoniqTextSubtle,
                    modifier = Modifier.size(22.dp),
                )
            }
            Box(modifier = Modifier.size(48.dp)) {
                if (row.deviceContactId > 0L) {
                    ContactPhotoAvatar(
                        deviceContactId = row.deviceContactId,
                        initials = initial,
                        gradientStart = g0,
                        gradientEnd = g1,
                        size = 48.dp,
                        fontSize = 17.sp,
                    )
                } else {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .contactAvatarClip(48.dp)
                                .background(Brush.linearGradient(listOf(g0, g1))),
                        contentAlignment = Alignment.Center,
                    ) {
                        AvatarInitialsText(
                            text = initial,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = phoneLine,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
                row.riskNote?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (!selectionMode) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            if (!context.placeOrDial(actionNumber, row)) {
                                onUserMessage(context.getString(R.string.toast_dial_failed))
                            }
                        },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = stringResource(R.string.cd_call_contact),
                            modifier = Modifier.size(21.dp),
                            tint = PhoniqAccent.copy(alpha = 0.85f),
                        )
                    }
                    IconButton(
                        onClick = {
                            if (!context.startSmsCompose(actionNumber)) {
                                onUserMessage(context.getString(R.string.snackbar_no_sms_app))
                            }
                        },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Message,
                            contentDescription = stringResource(R.string.cd_message_contact),
                            modifier = Modifier.size(21.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        )
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(44.dp)) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.cd_overflow_menu),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.contact_menu_select_for_bulk)) },
                                onClick = {
                                    menuOpen = false
                                    onStartSelectionForThis()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
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
    var menuExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val removeFavoriteLabel = stringResource(R.string.favorites_remove_action)
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
                    .padding(top = 10.dp, start = 6.dp, end = 6.dp, bottom = 10.dp),
        ) {
            Box(modifier = Modifier.size(FavoritesGridAvatarDp)) {
                val avDp = FavoritesGridAvatarDp
                val avShape = contactAvatarShapeForSize(avDp)
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .size(avDp)
                            .shadow(
                                elevation = 4.dp,
                                shape = avShape,
                                spotColor = Color.Black.copy(alpha = 0.22f),
                                ambientColor = Color.Black.copy(alpha = 0.08f),
                            )
                            .border(
                                width = if (isPressed) 1.5.dp else 0.dp,
                                color = PhoniqAccent.copy(alpha = if (isPressed) 0.5f else 0f),
                                shape = avShape,
                            )
                            .contactAvatarClip(avDp)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = onOpenDetail,
                                onLongClick = { menuExpanded = true },
                            ),
                ) {
                    if (row.deviceContactId > 0L) {
                        ContactPhotoAvatar(
                            deviceContactId = row.deviceContactId,
                            initials = initial,
                            gradientStart = g0,
                            gradientEnd = g1,
                            size = FavoritesGridAvatarDp,
                            fontSize = 23.sp,
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
                                fontSize = 23.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(removeFavoriteLabel) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = PhoniqAccent,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onRemoveFavorite()
                        },
                    )
                }
            }
            Text(
                text = row.name,
                fontSize = 12.sp,
                lineHeight = 15.sp,
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
    onAddFavorite: () -> Unit,
    scrollBottomInset: Dp,
) {
    val context = LocalContext.current
    val starredEntities by phoneViewModel.starredContacts.collectAsState()
    val favorites =
        remember(starredEntities) { starredEntities.aggregateContactsToRows() }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 8.dp, end = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = onAddFavorite) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = stringResource(R.string.cd_fab_add_favorite),
                    tint = PhoniqAccent,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        if (favorites.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 24.dp + scrollBottomInset),
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
                contentPadding =
                    PaddingValues(
                        start = 16.dp,
                        top = 4.dp,
                        end = 16.dp,
                        bottom = 16.dp + scrollBottomInset,
                    ),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
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
