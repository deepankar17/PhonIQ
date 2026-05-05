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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoniq.app.R
import com.phoniq.app.data.SampleData
import com.phoniq.app.data.model.CallChannel
import com.phoniq.app.data.model.CallDirection
import com.phoniq.app.data.model.ContactRow
import com.phoniq.app.data.model.QuickCallEntry
import com.phoniq.app.data.model.RecentCall
import com.phoniq.app.data.model.RecentCallFilter
import com.phoniq.app.data.model.matches
import com.phoniq.app.ui.components.MockupSectionLabel
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqBorder
import com.phoniq.app.ui.theme.PhoniqBorderSoft
import com.phoniq.app.ui.theme.PhoniqSecondary
import com.phoniq.app.ui.theme.PhoniqSurface
import com.phoniq.app.ui.theme.PhoniqTextSecondaryMock
import com.phoniq.app.ui.theme.PhoniqTextSubtle

private enum class PhoneSection {
    Recent,
    Contacts,
    Favorites,
}

@Composable
fun PhoneScreen(
    recents: List<RecentCall>,
    onUserMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    var section by remember { mutableStateOf(PhoneSection.Recent) }
    var recentFilter by remember { mutableStateOf(RecentCallFilter.All) }
    var showDialpad by remember { mutableStateOf(false) }

    if (showDialpad) {
        DialpadSheet(onDismiss = { showDialpad = false })
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

    Column(modifier = Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (section) {
                PhoneSection.Recent ->
                    RecentCallsPanel(
                        recents = recents,
                        filter = recentFilter,
                        onFilterChange = { recentFilter = it },
                        onUserMessage = onUserMessage,
                    )
                PhoneSection.Contacts ->
                    ContactsPanel(onUserMessage = onUserMessage)
                PhoneSection.Favorites ->
                    FavoritesPanel(onUserMessage = onUserMessage)
            }
        }
        DialerBottomBar(
            section = section,
            onSection = { section = it },
            fabIcon = fabIcon,
            fabContentDescription = fabCd,
            onFabClick = {
                when (section) {
                    PhoneSection.Recent -> showDialpad = true
                    PhoneSection.Contacts -> onUserMessage(context.getString(R.string.toast_add_contact_placeholder))
                    PhoneSection.Favorites -> onUserMessage(context.getString(R.string.toast_add_favorite_placeholder))
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
                color = PhoniqSurface,
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

@Composable
private fun RecentCallsPanel(
    recents: List<RecentCall>,
    filter: RecentCallFilter,
    onFilterChange: (RecentCallFilter) -> Unit,
    onUserMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val filtered = recents.filter { it.matches(filter) }

    Column(modifier = Modifier.fillMaxSize()) {
        QuickCallStrip(
            onQuickCall = { entry: QuickCallEntry ->
                onUserMessage(context.getString(R.string.toast_quick_call, entry.name))
            },
        )
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
        if (filtered.isEmpty()) {
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
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                itemsIndexed(
                    filtered,
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
                        RecentCallRow(call = call, onUserMessage = onUserMessage)
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
    val bg = if (selected) PhoniqAccent.copy(alpha = 0.22f) else PhoniqSurface
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
    onUserMessage: (String) -> Unit,
) {
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
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onUserMessage(context.getString(R.string.toast_call_contact, call.contactName))
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val g0 = Color(call.avatarStartArgb.toInt())
        val g1 = Color(call.avatarEndArgb.toInt())
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(g0, g1))),
            contentAlignment = Alignment.Center,
        ) {
            if (spamNumberAvatar) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(
                    text = call.contactName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Column(Modifier.weight(1f)) {
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
        IconButton(
            onClick = {
                when (call.channel) {
                    CallChannel.Pstn ->
                        onUserMessage(context.getString(R.string.toast_redial_pstn, call.contactName))
                    CallChannel.WhatsAppVoice ->
                        onUserMessage(context.getString(R.string.toast_wa_voice_handoff, call.contactName))
                    CallChannel.WhatsAppVideo ->
                        onUserMessage(context.getString(R.string.toast_wa_video_handoff, call.contactName))
                }
            },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                Icons.Default.Phone,
                contentDescription = stringResource(R.string.cd_redial),
                modifier = Modifier.size(18.dp),
                tint = PhoniqTextSubtle,
            )
        }
    }
}

@Composable
private fun synthesizedMetaCaption(call: RecentCall): String {
    val dir = directionLabel(call.direction)
    val channel =
        when (call.channel) {
            CallChannel.Pstn -> stringResource(R.string.recent_channel_phone)
            CallChannel.WhatsAppVoice,
            CallChannel.WhatsAppVideo,
            -> stringResource(R.string.recent_channel_whatsapp)
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

@Composable
private fun ContactsPanel(onUserMessage: (String) -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) SampleData.contacts
        else SampleData.contacts.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.subtitle.contains(query, ignoreCase = true)
        }
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
                    onValueChange = { query = it },
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
        if (filtered.isNotEmpty()) {
            item {
                MockupSectionLabel(text = stringResource(R.string.contacts_all_label))
            }
        }
        items(filtered, key = { it.id }) { row ->
            ContactRowItem(row = row, onUserMessage = onUserMessage)
        }
        item {
            BlockedSection(onManage = {
                onUserMessage(context.getString(R.string.contacts_blocked_manage_toast))
            })
        }
    }
}

@Composable
private fun BlockedSection(onManage: () -> Unit) {
    val context = LocalContext.current
    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    ListItem(
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
            IconButton(onClick = onManage) {
                Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        },
    )
}

@Composable
private fun ContactRowItem(
    row: ContactRow,
    onUserMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(row.name) },
        supportingContent = {
            Column {
                Text(row.subtitle, style = MaterialTheme.typography.bodySmall)
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
                IconButton(onClick = { onUserMessage(context.getString(R.string.toast_call_contact, row.name)) }) {
                    Icon(Icons.Default.Phone, contentDescription = stringResource(R.string.cd_call_contact))
                }
                IconButton(onClick = { onUserMessage(context.getString(R.string.toast_sms_contact, row.name)) }) {
                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = stringResource(R.string.cd_message_contact))
                }
            }
        },
    )
}

@Composable
private fun FavoritesPanel(onUserMessage: (String) -> Unit) {
    val context = LocalContext.current
    val favorites =
        remember {
            SampleData.contacts.filter { it.id in SampleData.favoriteContactIds }
        }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 8.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(favorites, key = { it.id }) { row ->
            Card(
                onClick = { onUserMessage(context.getString(R.string.toast_favorite_open, row.name)) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = row.name,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}
