package com.phoniq.app.ui.phone

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Message
import android.content.Intent
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phoniq.app.R
import com.phoniq.app.data.db.entity.CallLogEntity
import com.phoniq.app.data.mapper.toContactHistoryEntry
import com.phoniq.app.data.model.ContactHistoryEntry
import com.phoniq.app.data.model.ContactPhoneEntry
import com.phoniq.app.data.model.ContactRow
import com.phoniq.app.data.model.effectivePhoneNumbers
import com.phoniq.app.ui.components.AvatarInitialsText
import com.phoniq.app.ui.components.ContactPhotoAvatar
import com.phoniq.app.ui.components.contactAvatarClip
import com.phoniq.app.ui.components.contactAvatarShapeForSize
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqSecondary
import com.phoniq.app.ui.theme.PhoniqTextSecondaryMock
import com.phoniq.app.util.normalizePhoneKey
import com.phoniq.app.util.openBlockedNumbersSettings
import com.phoniq.app.util.placeOrDial
import com.phoniq.app.util.startSmsCompose
import com.phoniq.app.util.tryOpenWhatsAppForNumber
import java.util.Locale

private val DetailCardRadius = 18.dp
private val HeroAvatarSize = 92.dp
private val HeroAvatarOverlap = 46.dp

/**
 * Full-screen contact profile aligned with `design/phoniq-mockup-v1.html` `#view-contact`.
 */
@Composable
fun ContactDetailOverlay(
    contact: ContactRow,
    phoneViewModel: PhoneViewModel,
    onDismiss: () -> Unit,
    onUserMessage: (String) -> Unit,
    onEditContact: (deviceContactId: Long, displayName: String, phones: List<ContactPhoneEntry>) -> Unit = { _, _, _ -> },
    onOpenContactPolicies: () -> Unit = {},
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val allContacts by phoneViewModel.allContacts.collectAsState()
    /**
     * Resolve `(number, label)` entries for the contact. Prefer `phoneEntries` from the row,
     * but **always** also enrich from `allContacts` so when the row was built from a single
     * recents tap (no aggregate yet), we still surface every saved phone with its label.
     */
    val displayEntries =
        remember(contact, allContacts) {
            buildEnrichedPhoneEntries(contact, allContacts)
        }
    val displayNumbers = displayEntries.map { it.number }
    val primaryLine = displayNumbers.firstOrNull().orEmpty()
    val normalizedKeysForHistory =
        remember(displayNumbers) {
            displayNumbers.map { normalizePhoneKey(it) }.filter { it.isNotEmpty() }.toSet()
        }
    val spamKeys by phoneViewModel.spamNumberKeysState.collectAsState()
    val trustedKeys by phoneViewModel.userTrustedNumberKeys.collectAsState()
    val normKey = remember(primaryLine) { normalizePhoneKey(primaryLine) }
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

    val lastCallRelative =
        remember(historyEntities) {
            historyEntities.maxByOrNull { it.timestamp }?.let { ent ->
                DateUtils.getRelativeTimeSpanString(
                    ent.timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                ).toString()
            }
        }

    val onBlockNumber: () -> Unit = {
        if (context.openBlockedNumbersSettings()) {
            onUserMessage(context.getString(R.string.contact_block_open_system))
        } else {
            onUserMessage(context.getString(R.string.after_call_blocked_numbers_unavailable))
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
        Surface(modifier = Modifier.fillMaxSize(), color = scheme.background) {
            Column(Modifier.fillMaxSize()) {
                val lastSeenLine =
                    lastCallRelative?.let { stringResource(R.string.contact_last_call_subtitle, it) }
                val metaJoined =
                    buildList {
                        val p = primaryLine.trim()
                        if (p.startsWith("+") && !p.startsWith("+91")) {
                            add(stringResource(R.string.chip_international))
                        }
                        if (
                            historyEntities.any {
                                it.callChannel == "WHATSAPP_VOICE" ||
                                    it.callChannel == "WHATSAPP_VIDEO"
                            }
                        ) {
                            add(stringResource(R.string.contact_meta_whatsapp_in_log))
                        }
                    }.takeIf { it.isNotEmpty() }
                        ?.joinToString(" · ")

                val starredContacts by phoneViewModel.starredContacts.collectAsState()
                val isStarred =
                    remember(starredContacts, resolvedDeviceContactId) {
                        resolvedDeviceContactId > 0L &&
                            starredContacts.any { it.deviceContactId == resolvedDeviceContactId }
                    }
                ContactDetailHero(
                    contact = contact,
                    deviceContactIdForPhoto = resolvedDeviceContactId,
                    subtitleLine = lastSeenLine,
                    onDismiss = onDismiss,
                    isStarred = isStarred,
                    showStarToggle = resolvedDeviceContactId > 0L,
                    onToggleStar = {
                        if (resolvedDeviceContactId <= 0L) return@ContactDetailHero
                        if (isStarred) {
                            phoneViewModel.unstarDeviceContact(resolvedDeviceContactId) { ok ->
                                if (!ok) onUserMessage(context.getString(R.string.contact_star_failed))
                            }
                        } else {
                            phoneViewModel.starDeviceContact(resolvedDeviceContactId) { ok ->
                                if (!ok) onUserMessage(context.getString(R.string.contact_star_failed))
                            }
                        }
                    },
                    onShare = {
                        val vcard = buildVCard(contact.name, displayEntries)
                        val send =
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/x-vcard"
                                putExtra(Intent.EXTRA_TEXT, vcard)
                                putExtra(Intent.EXTRA_SUBJECT, contact.name)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        runCatching {
                            context.startActivity(
                                Intent.createChooser(send, context.getString(R.string.contact_share_chooser)),
                            )
                        }.onFailure {
                            onUserMessage(context.getString(R.string.contact_share_failed))
                        }
                    },
                )
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                ) {
                    Spacer(Modifier.height(HeroAvatarOverlap))
                    QuickActionsRow(
                        showWhatsApp = primaryLine.any { it.isDigit() },
                        onCall = {
                            if (!context.placeOrDial(primaryLine, contact)) {
                                onUserMessage(context.getString(R.string.toast_dial_failed))
                            }
                        },
                        onMessage = {
                            if (!context.startSmsCompose(primaryLine)) {
                                onUserMessage(context.getString(R.string.snackbar_no_sms_app))
                            }
                        },
                        onWhatsApp = {
                            if (!context.tryOpenWhatsAppForNumber(primaryLine)) {
                                onUserMessage(context.getString(R.string.toast_whatsapp_open_failed))
                            }
                        },
                        onEdit = {
                            onEditContact(resolvedDeviceContactId, contact.name, displayEntries)
                        },
                        onBlock = onBlockNumber,
                    )
                    ContactPhonesCard(
                        entries = displayEntries,
                        metaLine = metaJoined,
                        onDial = { num ->
                            if (!context.placeOrDial(num, contact)) {
                                onUserMessage(context.getString(R.string.toast_dial_failed))
                            }
                        },
                        onSms = { num ->
                            if (!context.startSmsCompose(num)) {
                                onUserMessage(context.getString(R.string.snackbar_no_sms_app))
                            }
                        },
                        onWhatsApp = { num ->
                            if (!context.tryOpenWhatsAppForNumber(num)) {
                                onUserMessage(context.getString(R.string.toast_whatsapp_open_failed))
                            }
                        },
                    )
                    if (showSpamTools) {
                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(DetailCardRadius),
                            color = scheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, scheme.outline.copy(alpha = 0.12f)),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                    }
                    ContactPolicyCard(
                        onClick = onOpenContactPolicies,
                    )
                    ContactHistorySection(
                        history = history,
                        historyVisible = historyVisible,
                        historyExpanded = historyExpanded,
                        onToggleExpand = { historyExpanded = !historyExpanded },
                    )
                    Surface(
                        onClick = onBlockNumber,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(DetailCardRadius),
                        color = scheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, Color(0x40FF5050)),
                    ) {
                        Text(
                            text = stringResource(R.string.contact_block_cta),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFFF5050),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactDetailHero(
    contact: ContactRow,
    deviceContactIdForPhoto: Long,
    subtitleLine: String?,
    onDismiss: () -> Unit,
    isStarred: Boolean = false,
    showStarToggle: Boolean = false,
    onToggleStar: () -> Unit = {},
    onShare: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val heroGradient =
        remember(scheme) {
            Brush.verticalGradient(
                colors =
                    listOf(
                        PhoniqAccent,
                        PhoniqAccent.copy(alpha = 0.75f),
                        PhoniqSecondary.copy(alpha = 0.45f),
                        scheme.background,
                    ),
            )
        }
    val g0 = Color(contact.avatarStartArgb.toInt())
    val g1 = Color(contact.avatarEndArgb.toInt())
    val initial = contact.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(heroGradient),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(bottom = HeroAvatarOverlap + 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.contact_detail_back),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.92f),
                    fontWeight = FontWeight.Medium,
                    modifier =
                        Modifier
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                )
                Spacer(Modifier.weight(1f))
                if (showStarToggle) {
                    IconButton(onClick = onToggleStar) {
                        Icon(
                            imageVector = if (isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription =
                                stringResource(
                                    if (isStarred) R.string.contact_unstar_cd else R.string.contact_star_cd,
                                ),
                            tint = if (isStarred) Color(0xFFFFC107) else Color.White,
                        )
                    }
                }
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = stringResource(R.string.contact_share_cd),
                        tint = Color.White,
                    )
                }
                Spacer(Modifier.size(4.dp))
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 4.dp),
            ) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                subtitleLine?.let { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                contact.riskNote?.let { note ->
                    Surface(
                        modifier = Modifier.padding(top = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.22f),
                    ) {
                        Text(
                            text = note,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                }
            }
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = HeroAvatarOverlap)
                    .size(HeroAvatarSize)
                    .border(3.dp, Color.White.copy(alpha = 0.38f), contactAvatarShapeForSize(HeroAvatarSize)),
            contentAlignment = Alignment.Center,
        ) {
            val innerAvatar = HeroAvatarSize - 6.dp
            if (deviceContactIdForPhoto > 0L) {
                ContactPhotoAvatar(
                    deviceContactId = deviceContactIdForPhoto,
                    initials = initial,
                    gradientStart = g0,
                    gradientEnd = g1,
                    size = innerAvatar,
                    fontSize = 28.sp,
                )
            } else {
                Box(
                    modifier =
                        Modifier
                            .size(innerAvatar)
                            .contactAvatarClip(innerAvatar)
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
    }
}

@Composable
private fun QuickActionsRow(
    showWhatsApp: Boolean,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onWhatsApp: () -> Unit,
    onEdit: () -> Unit,
    onBlock: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        QuickAction(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.contact_qa_call),
            icon = Icons.Default.Phone,
            iconBrush = Brush.linearGradient(colors = listOf(Color(0xFF00C472), Color(0xFF00A854))),
            onClick = onCall,
        )
        QuickAction(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.contact_qa_message),
            icon = Icons.AutoMirrored.Filled.Message,
            iconBrush = Brush.linearGradient(colors = listOf(Color(0xFF3A8DFF), Color(0xFF1F5FD1))),
            onClick = onMessage,
        )
        if (showWhatsApp) {
            QuickAction(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.contact_qa_whatsapp),
                icon = Icons.AutoMirrored.Filled.Chat,
                iconBrush = Brush.linearGradient(colors = listOf(Color(0xFF25D366), Color(0xFF128C4E))),
                onClick = onWhatsApp,
            )
        }
        QuickAction(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.contact_qa_edit),
            icon = Icons.Default.Edit,
            iconBrush = Brush.linearGradient(colors = listOf(Color(0xFFF5A623), Color(0xFFE87D20))),
            onClick = onEdit,
        )
        QuickAction(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.contact_qa_block),
            icon = Icons.Default.Block,
            iconBrush = Brush.linearGradient(colors = listOf(Color(0xFF888888), Color(0xFF444444))),
            onClick = onBlock,
        )
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    iconBrush: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
            modifier
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp)
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
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.95f),
            maxLines = 2,
        )
    }
}

/**
 * Lists **every** saved phone for the contact, one row per number, with the contact's own
 * label (Mobile, Whatsapp, Home, custom…) on top and quick Call + SMS + WhatsApp icons trailing.
 * Tapping the row dials that specific number.
 */
@Composable
private fun ContactPhonesCard(
    entries: List<ContactPhoneEntry>,
    metaLine: String?,
    onDial: (String) -> Unit,
    onSms: (String) -> Unit,
    onWhatsApp: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(DetailCardRadius),
        color = scheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, scheme.outline.copy(alpha = 0.1f)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
        ) {
            entries.forEachIndexed { index, entry ->
                ContactPhoneRow(
                    entry = entry,
                    onDial = { onDial(entry.number) },
                    onSms = { onSms(entry.number) },
                    onWhatsApp = { onWhatsApp(entry.number) },
                )
                if (index < entries.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = scheme.outline.copy(alpha = 0.08f),
                        thickness = 1.dp,
                    )
                }
            }
            if (entries.isEmpty()) {
                Text(
                    text = stringResource(R.string.contact_phone_list_empty),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
            metaLine?.let { meta ->
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.9f),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ContactPhoneRow(
    entry: ContactPhoneEntry,
    onDial: () -> Unit,
    onSms: () -> Unit,
    onWhatsApp: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onDial)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val labelText = entry.label?.trim()?.takeIf { it.isNotEmpty() }
            if (labelText != null) {
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelMedium,
                    color = PhoniqTextSecondaryMock,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                text = entry.number,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                modifier = Modifier.padding(top = if (labelText != null) 2.dp else 0.dp),
            )
        }
        IconButton(onClick = onDial) {
            Icon(
                Icons.Default.Phone,
                contentDescription = stringResource(R.string.contact_qa_call),
                tint = Color(0xFF00C472),
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onSms) {
            Icon(
                Icons.AutoMirrored.Filled.Message,
                contentDescription = stringResource(R.string.contact_cd_sms_compose),
                tint = Color(0xFF3A8DFF),
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onWhatsApp) {
            Icon(
                Icons.AutoMirrored.Filled.Chat,
                contentDescription = stringResource(R.string.contact_qa_whatsapp),
                tint = Color(0xFF25D366),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Merge per-number labels from [allContacts] into the `phoneEntries` we already have on [row].
 * Falls back to looking up by normalized phone key when the row came from a recents tap
 * (no aggregate). De-duplicates by normalized digits while preserving insertion order.
 */
private fun buildEnrichedPhoneEntries(
    row: ContactRow,
    allContacts: List<com.phoniq.app.data.db.entity.ContactEntity>,
): List<ContactPhoneEntry> {
    val out = LinkedHashMap<String, ContactPhoneEntry>()
    fun put(entry: ContactPhoneEntry) {
        val key = normalizePhoneKey(entry.number).ifEmpty { "raw:${entry.number.trim()}" }
        val existing = out[key]
        if (existing == null) {
            out[key] = entry
        } else if (existing.label.isNullOrEmpty() && !entry.label.isNullOrEmpty()) {
            out[key] = existing.copy(label = entry.label)
        }
    }
    for (entry in row.phoneEntries) put(entry)
    if (row.deviceContactId > 0L) {
        for (e in allContacts) {
            if (e.deviceContactId == row.deviceContactId) {
                put(ContactPhoneEntry(number = e.number, label = e.tag?.trim()?.takeIf { it.isNotEmpty() }))
            }
        }
    } else if (row.phoneEntries.isEmpty()) {
        for (raw in row.effectivePhoneNumbers()) put(ContactPhoneEntry(number = raw, label = null))
    }
    return out.values.toList()
}

@Composable
private fun ContactPolicyCard(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val brush = Brush.linearGradient(colors = listOf(Color(0xFF8E44AD), Color(0xFF5B2C6F)))
    val titleText = stringResource(R.string.contact_policy_title)
    val subText = stringResource(R.string.contact_policy_sub)
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$titleText. $subText"
                },
        shape = RoundedCornerShape(DetailCardRadius),
        color = scheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, scheme.outline.copy(alpha = 0.1f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(brush),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.contact_policy_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = scheme.onSurface,
                )
                Text(
                    stringResource(R.string.contact_policy_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.88f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text("›", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ContactHistorySection(
    history: List<ContactHistoryEntry>,
    historyVisible: List<ContactHistoryEntry>,
    historyExpanded: Boolean,
    onToggleExpand: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val sectionTitle = stringResource(R.string.contact_history_section).uppercase(Locale.getDefault())
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(DetailCardRadius),
        color = scheme.surfaceContainerLow,
        border = BorderStroke(1.dp, scheme.outline.copy(alpha = 0.08f)),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.1.sp,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.85f),
                )
                if (history.size > 3) {
                    TextButton(onClick = onToggleExpand) {
                        Text(
                            text =
                                if (historyExpanded) {
                                    stringResource(R.string.contact_history_show_less).uppercase(Locale.getDefault())
                                } else {
                                    stringResource(R.string.contact_history_view_all).uppercase(Locale.getDefault())
                                },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = PhoniqAccent,
                        )
                    }
                }
            }
            if (history.isEmpty()) {
                Text(
                    text = stringResource(R.string.contact_history_empty),
                    modifier =
                        Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.9f),
                )
            } else {
                historyVisible.forEachIndexed { index, entry ->
                    ContactHistoryRow(entry)
                    if (index < historyVisible.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = scheme.outline.copy(alpha = 0.08f),
                            thickness = 1.dp,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ContactHistoryRow(entry: ContactHistoryEntry) {
    val scheme = MaterialTheme.colorScheme
    val tint = if (entry.incoming) PhoniqSecondary else PhoniqAccent
    val bg = tint.copy(alpha = 0.12f)
    val icon: ImageVector =
        if (entry.incoming) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "${entry.directionMeta}, ${entry.timeMeta}"
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(
                entry.directionMeta,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = scheme.onSurface,
            )
            Text(
                entry.timeMeta,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant.copy(alpha = 0.88f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/**
 * Build a minimal vCard 3.0 string for [name] + [entries] suitable for ACTION_SEND chooser.
 * Phone labels map to vCard TYPE parameters where possible (Mobile → CELL, Home → HOME, …).
 */
private fun buildVCard(name: String, entries: List<ContactPhoneEntry>): String =
    buildString {
        append("BEGIN:VCARD\r\n")
        append("VERSION:3.0\r\n")
        if (name.isNotBlank()) {
            append("FN:").append(name).append("\r\n")
        }
        entries.forEach { entry ->
            val number = entry.number.trim()
            if (number.isEmpty()) return@forEach
            val type =
                when (entry.label?.lowercase()) {
                    "mobile" -> "CELL"
                    "home" -> "HOME"
                    "work" -> "WORK"
                    "fax" -> "FAX"
                    "whatsapp" -> "VOICE"
                    null, "" -> "VOICE"
                    else -> "VOICE"
                }
            append("TEL;TYPE=").append(type).append(":").append(number).append("\r\n")
        }
        append("END:VCARD\r\n")
    }
