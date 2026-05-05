package com.phoniq.app.ui.phone

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.phoniq.app.R
import com.phoniq.app.data.SampleData
import com.phoniq.app.data.model.CallChannel
import com.phoniq.app.data.model.CallDirection
import com.phoniq.app.data.model.ContactRow
import com.phoniq.app.data.model.QuickCallEntry
import com.phoniq.app.data.model.RecentCall
import com.phoniq.app.data.model.RecentCallFilter
import com.phoniq.app.data.model.matches
import com.phoniq.app.ui.theme.PhoniqAccent

private enum class PhoneSection {
    Recent,
    Contacts,
    Favorites,
}

@Composable
fun PhoneScreen(
    recents: SnapshotStateList<RecentCall>,
    onUserMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    var section by remember { mutableStateOf(PhoneSection.Recent) }
    var recentFilter by remember { mutableStateOf(RecentCallFilter.All) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SecondaryTabRow(selectedTabIndex = section.ordinal) {
                Tab(
                    selected = section == PhoneSection.Recent,
                    onClick = { section = PhoneSection.Recent },
                    text = { Text(stringResource(R.string.phone_tab_recent)) },
                )
                Tab(
                    selected = section == PhoneSection.Contacts,
                    onClick = { section = PhoneSection.Contacts },
                    text = { Text(stringResource(R.string.phone_tab_contacts)) },
                )
                Tab(
                    selected = section == PhoneSection.Favorites,
                    onClick = { section = PhoneSection.Favorites },
                    text = { Text(stringResource(R.string.phone_tab_favorites)) },
                )
            }
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
        FloatingActionButton(
            onClick = {
                onUserMessage(
                    when (section) {
                        PhoneSection.Recent -> context.getString(R.string.toast_keypad_placeholder)
                        PhoneSection.Contacts -> context.getString(R.string.toast_add_contact_placeholder)
                        PhoneSection.Favorites -> context.getString(R.string.toast_add_favorite_placeholder)
                    },
                )
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = PhoniqAccent,
        ) {
            Icon(fabIcon, contentDescription = fabCd)
        }
    }
}

@Composable
private fun RecentCallsPanel(
    recents: SnapshotStateList<RecentCall>,
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
        Text(
            text = stringResource(R.string.phone_section_recent_calls),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scroll)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RecentCallFilter.entries.forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { onFilterChange(f) },
                    label = { Text(recentFilterLabel(f)) },
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
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                itemsIndexed(
                    filtered,
                    key = { _, call -> call.id },
                ) { index, call ->
                    Column(Modifier.fillMaxWidth()) {
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
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
private fun RecentCallRow(
    call: RecentCall,
    onUserMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(call.contactName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = call.metaCaption ?: synthesizedMetaCaption(call),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (call.metaCaption != null &&
                        call.numberOrLabel.isNotBlank() &&
                        call.numberOrLabel != call.contactName
                    ) {
                        Text(
                            call.numberOrLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    } else if (call.metaCaption == null) {
                        Text(
                            call.numberOrLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        if (call.isSpam) {
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text(stringResource(R.string.chip_likely_spam)) },
                                colors =
                                    AssistChipDefaults.assistChipColors(
                                        disabledContainerColor = MaterialTheme.colorScheme.errorContainer,
                                        disabledLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                                    ),
                            )
                        }
                        if (call.isBlocked) {
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text(stringResource(R.string.chip_blocked)) },
                            )
                        }
                        if (call.missedStreak > 1 && call.metaCaption?.contains("Missed (") != true) {
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text(stringResource(R.string.chip_missed_count, call.missedStreak)) },
                            )
                        }
                        if (call.isInternational) {
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text(stringResource(R.string.chip_international)) },
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(call.timeLabel, style = MaterialTheme.typography.labelLarge)
                    Text(
                        directionLabel(call.direction),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        if (call.channel == CallChannel.Pstn) {
                            IconButton(
                                onClick = {
                                    onUserMessage(
                                        context.getString(R.string.toast_redial_pstn, call.contactName),
                                    )
                                },
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = stringResource(R.string.cd_redial))
                            }
                        }
                        if (call.channel == CallChannel.WhatsAppVoice) {
                            IconButton(
                                onClick = {
                                    onUserMessage(
                                        context.getString(R.string.toast_wa_voice_handoff, call.contactName),
                                    )
                                },
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Message, contentDescription = stringResource(R.string.cd_wa_voice))
                            }
                        }
                        if (call.channel == CallChannel.WhatsAppVideo) {
                            IconButton(
                                onClick = {
                                    onUserMessage(
                                        context.getString(R.string.toast_wa_video_handoff, call.contactName),
                                    )
                                },
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = stringResource(R.string.cd_wa_video))
                            }
                        }
                    }
                }
            }
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
        contentPadding = PaddingValues(bottom = 88.dp),
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.contacts_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
            )
        }
        if (filtered.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.contacts_all_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 2.dp),
                )
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
        contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp),
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
