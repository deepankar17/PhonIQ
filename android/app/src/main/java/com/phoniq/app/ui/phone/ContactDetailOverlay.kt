package com.phoniq.app.ui.phone

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phoniq.app.R
import com.phoniq.app.data.db.entity.CallLogEntity
import com.phoniq.app.data.mapper.toContactHistoryEntry
import com.phoniq.app.data.model.ContactHistoryEntry
import com.phoniq.app.data.model.ContactRow
import com.phoniq.app.data.model.effectivePhoneNumbers
import com.phoniq.app.ui.components.MockupSectionLabel
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqBackground
import com.phoniq.app.ui.theme.PhoniqBorder
import com.phoniq.app.ui.theme.PhoniqBorderSoft
import com.phoniq.app.ui.theme.PhoniqOnBackground
import com.phoniq.app.ui.theme.PhoniqSecondary
import com.phoniq.app.ui.theme.PhoniqSurface
import com.phoniq.app.ui.theme.PhoniqTextSecondaryMock
import com.phoniq.app.ui.components.AvatarInitialsText
import com.phoniq.app.ui.components.ContactPhotoAvatar
import com.phoniq.app.util.normalizePhoneKey
import com.phoniq.app.util.openBlockedNumbersSettings
import com.phoniq.app.util.startDialer
import com.phoniq.app.util.startSmsCompose

/**
 * Full-screen contact profile aligned with `design/phoniq-mockup-v1.html` `#view-contact`.
 */
@Composable
fun ContactDetailOverlay(
    contact: ContactRow,
    phoneViewModel: PhoneViewModel,
    onDismiss: () -> Unit,
    onUserMessage: (String) -> Unit,
    onOpenContactPolicies: () -> Unit = {},
) {
    val context = LocalContext.current
    val displayNumbers = remember(contact) { contact.effectivePhoneNumbers() }
    val primaryLine = displayNumbers.firstOrNull().orEmpty()
    val normalizedKeysForHistory =
        remember(displayNumbers) {
            displayNumbers.map { normalizePhoneKey(it) }.filter { it.isNotEmpty() }.toSet()
        }
    val spamKeys by phoneViewModel.spamNumberKeysState.collectAsState()
    val trustedKeys by phoneViewModel.userTrustedNumberKeys.collectAsState()
    val normKey = remember(primaryLine) { normalizePhoneKey(primaryLine) }
    val allContacts by phoneViewModel.allContacts.collectAsState()
    val resolvedDeviceContactId =
        remember(contact.deviceContactId, normKey, allContacts) {
            if (contact.deviceContactId > 0L) {
                contact.deviceContactId
            } else if (normKey.isNotEmpty()) {
                allContacts
                    .firstOrNull { normalizePhoneKey(it.number) == normKey && it.deviceContactId > 0L }
                    ?.deviceContactId
                    ?: 0L
            } else {
                0L
            }
        }
    val inSpamDb = normKey.isNotEmpty() && normKey in spamKeys
    val isTrusted = normKey.isNotEmpty() && normKey in trustedKeys
    val showSpamTools = normKey.isNotEmpty() && resolvedDeviceContactId == 0L
    var historyEntities by remember(contact.id, normalizedKeysForHistory) { mutableStateOf<List<CallLogEntity>>(emptyList()) }
    LaunchedEffect(normalizedKeysForHistory) {
        phoneViewModel.callsMatchingNormalizedKeys(normalizedKeysForHistory).collect { historyEntities = it }
    }
    val history = remember(historyEntities) { historyEntities.map { it.toContactHistoryEntry() } }
    var historyExpanded by remember(contact.id, normalizedKeysForHistory) { mutableStateOf(false) }
    val historyVisibleCount = remember(history.size, historyExpanded) {
        if (historyExpanded) minOf(10, history.size) else minOf(3, history.size)
    }
    val historyVisible = remember(history, historyVisibleCount) { history.take(historyVisibleCount) }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = PhoniqBackground) {
            Column(Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(R.string.contact_detail_back),
                    style = MaterialTheme.typography.labelLarge,
                    color = PhoniqAccent,
                    fontWeight = FontWeight.Medium,
                    modifier =
                        Modifier
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                )
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                ) {
                    ContactDetailHeader(
                        contact = contact,
                        phoneNumbers = displayNumbers,
                        deviceContactIdForPhoto = resolvedDeviceContactId,
                    )
                    QuickActionsRow(
                        onCall = {
                            if (!context.startDialer(primaryLine)) {
                                onUserMessage(context.getString(R.string.toast_dial_failed))
                            }
                        },
                        onMessage = {
                            if (!context.startSmsCompose(primaryLine)) {
                                onUserMessage(context.getString(R.string.snackbar_no_sms_app))
                            }
                        },
                        onNote = { onUserMessage(context.getString(R.string.toast_contact_note)) },
                        onSchedule = { onUserMessage(context.getString(R.string.toast_contact_schedule)) },
                    )
                    if (showSpamTools) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (!isTrusted) {
                                TextButton(
                                    onClick = { phoneViewModel.markTrustedNumber(primaryLine) },
                                ) {
                                    Text(stringResource(R.string.call_menu_mark_trusted), color = PhoniqAccent)
                                }
                            } else {
                                TextButton(
                                    onClick = { phoneViewModel.clearTrustedNumber(primaryLine) },
                                ) {
                                    Text(stringResource(R.string.call_menu_clear_trusted), color = PhoniqAccent)
                                }
                            }
                            if (!inSpamDb) {
                                TextButton(
                                    onClick = { phoneViewModel.markSpam(primaryLine) },
                                ) {
                                    Text(stringResource(R.string.call_menu_mark_spam), color = Color(0xFFFF5050))
                                }
                            } else {
                                TextButton(
                                    onClick = { phoneViewModel.unmarkSpam(primaryLine) },
                                ) {
                                    Text(stringResource(R.string.call_menu_unmark_spam), color = PhoniqAccent)
                                }
                            }
                        }
                    }
                    ContactPolicyCard(
                        onClick = onOpenContactPolicies,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = PhoniqBorderSoft,
                        thickness = 1.dp,
                    )
                    MockupSectionLabel(text = stringResource(R.string.contact_history_section), topPadding = 8.dp)
                    if (history.isEmpty()) {
                        Text(
                            text = stringResource(R.string.contact_history_empty),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PhoniqTextSecondaryMock,
                        )
                    } else {
                        historyVisible.forEach { entry ->
                            ContactHistoryRow(entry)
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                color = PhoniqBorderSoft,
                                thickness = 1.dp,
                            )
                        }
                        if (history.size > 3) {
                            TextButton(
                                onClick = { historyExpanded = !historyExpanded },
                                modifier = Modifier.padding(horizontal = 8.dp),
                            ) {
                                Text(
                                    text =
                                        if (historyExpanded) {
                                            stringResource(R.string.contact_history_show_less)
                                        } else {
                                            stringResource(R.string.contact_history_view_more)
                                        },
                                    color = PhoniqAccent,
                                )
                            }
                        }
                    }
                    Surface(
                        onClick = {
                            if (context.openBlockedNumbersSettings()) {
                                onUserMessage(context.getString(R.string.contact_block_open_system))
                            } else {
                                onUserMessage(context.getString(R.string.after_call_blocked_numbers_unavailable))
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x1AFF5050),
                        border = BorderStroke(1.dp, Color(0x40FF5050)),
                    ) {
                        Text(
                            text = stringResource(R.string.contact_block_cta),
                            modifier = Modifier.padding(13.dp),
                            color = Color(0xFFFF5050),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactDetailHeader(
    contact: ContactRow,
    phoneNumbers: List<String>,
    deviceContactIdForPhoto: Long,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF141428),
                                Color.Transparent,
                            ),
                    ),
                )
                .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val g0 = Color(contact.avatarStartArgb.toInt())
        val g1 = Color(contact.avatarEndArgb.toInt())
        val initial = contact.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        Box(
            modifier =
                Modifier
                    .size(88.dp)
                    .border(3.dp, PhoniqAccent.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (deviceContactIdForPhoto > 0L) {
                ContactPhotoAvatar(
                    deviceContactId = deviceContactIdForPhoto,
                    initials = initial,
                    gradientStart = g0,
                    gradientEnd = g1,
                    size = 88.dp,
                    fontSize = 28.sp,
                )
            } else {
                Box(
                    modifier =
                        Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(g0, g1))),
                    contentAlignment = Alignment.Center,
                ) {
                    AvatarInitialsText(
                        text = initial,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                contact.name,
                color = PhoniqOnBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            if (phoneNumbers.size <= 1) {
                Text(
                    phoneNumbers.firstOrNull().orEmpty(),
                    color = PhoniqTextSecondaryMock,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            } else {
                Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    phoneNumbers.forEach { line ->
                        Text(
                            line,
                            color = PhoniqTextSecondaryMock,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
            contact.riskNote?.let { note ->
                Surface(
                    modifier = Modifier.padding(top = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = PhoniqSecondary.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = note,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PhoniqSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onNote: () -> Unit,
    onSchedule: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        QuickAction(
            label = stringResource(R.string.contact_qa_call),
            icon = Icons.Default.Phone,
            iconBrush = Brush.linearGradient(colors = listOf(Color(0xFF00C472), Color(0xFF00A854))),
            onClick = onCall,
        )
        QuickAction(
            label = stringResource(R.string.contact_qa_message),
            icon = Icons.AutoMirrored.Filled.Message,
            iconBrush = Brush.linearGradient(colors = listOf(Color(0xFF6C63FF), Color(0xFF4A43CC))),
            onClick = onMessage,
        )
        QuickAction(
            label = stringResource(R.string.contact_qa_note),
            icon = Icons.Default.Edit,
            iconBrush = Brush.linearGradient(colors = listOf(Color(0xFFF5A623), Color(0xFFE87D20))),
            onClick = onNote,
        )
        QuickAction(
            label = stringResource(R.string.contact_qa_schedule),
            icon = Icons.Default.CalendarMonth,
            iconBrush = Brush.linearGradient(colors = listOf(Color(0xFF888888), Color(0xFF555555))),
            onClick = onSchedule,
        )
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    iconBrush: Brush,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier =
            Modifier
                .clickable(onClick = onClick)
                .semantics(mergeDescendants = true) {
                    contentDescription = label
                },
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconBrush),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = PhoniqTextSecondaryMock)
    }
}

@Composable
private fun ContactPolicyCard(onClick: () -> Unit) {
    val brush = Brush.linearGradient(colors = listOf(Color(0xFF8E44AD), Color(0xFF5B2C6F)))
    val titleText = stringResource(R.string.contact_policy_title)
    val subText = stringResource(R.string.contact_policy_sub)
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 0.dp)
                .padding(bottom = 8.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$titleText. $subText"
                },
        shape = RoundedCornerShape(16.dp),
        color = PhoniqSurface,
        border = BorderStroke(1.dp, PhoniqBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(brush),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.contact_policy_title),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = PhoniqOnBackground,
                )
                Text(
                    stringResource(R.string.contact_policy_sub),
                    fontSize = 11.sp,
                    color = PhoniqTextSecondaryMock,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            Text("›", fontSize = 14.sp, color = Color(0xFF444444))
        }
    }
}

@Composable
private fun ContactHistoryRow(entry: ContactHistoryEntry) {
    val tint = if (entry.incoming) PhoniqSecondary else PhoniqAccent
    val bg = tint.copy(alpha = 0.12f)
    val icon: ImageVector =
        if (entry.incoming) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "${entry.directionMeta}, ${entry.timeMeta}"
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                entry.directionMeta,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = PhoniqOnBackground,
            )
            Text(
                entry.timeMeta,
                fontSize = 12.sp,
                color = PhoniqTextSecondaryMock,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
