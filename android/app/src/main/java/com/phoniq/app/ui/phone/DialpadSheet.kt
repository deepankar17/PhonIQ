package com.phoniq.app.ui.phone

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.phoniq.app.data.db.entity.ContactEntity
import com.phoniq.app.data.model.RecentCall
import com.phoniq.app.ui.components.AvatarInitialsText
import com.phoniq.app.ui.components.ContactPhotoAvatar
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.util.computeDialpadMatches
import com.phoniq.app.util.DialpadMatchRow
import com.phoniq.app.util.sanitizeForTelDial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class DialKey(val digit: String, val letters: String)

private val DIAL_KEYS = listOf(
    DialKey("1", ""),     DialKey("2", "ABC"),  DialKey("3", "DEF"),
    DialKey("4", "GHI"),  DialKey("5", "JKL"),  DialKey("6", "MNO"),
    DialKey("7", "PQRS"), DialKey("8", "TUV"),  DialKey("9", "WXYZ"),
    DialKey("*", ""),     DialKey("0", "+"),     DialKey("#", ""),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialpadSheet(
    onDismiss: () -> Unit,
    onAddContact: (phoneNumber: String) -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        DialpadContent(onDismiss = onDismiss, onAddContact = onAddContact)
    }
}

@Composable
fun DialpadContent(
    modifier: Modifier = Modifier,
    initialDigits: String = "",
    contacts: List<ContactEntity> = emptyList(),
    recentCalls: List<RecentCall> = emptyList(),
    onDismiss: () -> Unit = {},
    onAddContact: (phoneNumber: String) -> Unit = {},
) {
    val context = LocalContext.current
    var digits by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(initialDigits) {
        if (initialDigits.isNotEmpty()) {
            digits = sanitizeForTelDial(initialDigits)
        }
    }

    var debouncedDigits by remember { mutableStateOf("") }
    LaunchedEffect(digits) {
        delay(75)
        debouncedDigits = digits
    }

    val matchRows by produceState(initialValue = emptyList<DialpadMatchRow>(), debouncedDigits, contacts, recentCalls) {
        value =
            withContext(Dispatchers.Default) {
                computeDialpadMatches(contacts, recentCalls, debouncedDigits)
            }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Number display
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (digits.isNotEmpty()) {
                    IconButton(onClick = { onAddContact(digits) }) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = stringResource(R.string.dialpad_add_contact),
                            tint = PhoniqAccent,
                        )
                    }
                }
            }
            Text(
                text = digits.ifEmpty { stringResource(R.string.dialpad_enter_number_hint) },
                style = MaterialTheme.typography.headlineLarge,
                color =
                    if (digits.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (digits.isNotEmpty()) {
                    IconButton(onClick = { digits = digits.dropLast(1) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = stringResource(R.string.cd_dialpad_backspace),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (matchRows.isNotEmpty()) {
            HorizontalDivider(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            )
            Column(Modifier.fillMaxWidth()) {
                matchRows.forEach { row ->
                    DialpadMatchRowView(
                        row = row,
                        onApplyNumber = { digits = row.telSanitized },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Key grid — 3 columns
        DIAL_KEYS.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { key ->
                    DialKeyButton(
                        digit = key.digit,
                        letters = key.letters,
                        onClick = { digits += key.digit },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(16.dp))

        // Call button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (digits.isNotEmpty()) PhoniqAccent else MaterialTheme.colorScheme.surfaceVariant)
                .clickable(
                    enabled = digits.isNotEmpty(),
                    indication = ripple(),
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${digits}"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    onDismiss()
                },
        ) {
            Icon(
                Icons.Default.Call,
                contentDescription = "Call",
                tint = if (digits.isNotEmpty()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DialpadMatchRowView(
    row: DialpadMatchRow,
    onApplyNumber: () -> Unit,
) {
    val applyCd = stringResource(R.string.cd_dialpad_apply_match, row.title)
    val g0 = Color(row.avatarStartArgb.toInt())
    val g1 = Color(row.avatarEndArgb.toInt())
    val initial =
        row.title
            .trim()
            .split(" ")
            .mapNotNull { w -> w.firstOrNull() }
            .take(2)
            .joinToString("") { ch -> ch.uppercaseChar().toString() }
            .ifEmpty { row.title.firstOrNull()?.uppercaseChar()?.toString() ?: "?" }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(
                    indication = ripple(),
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onApplyNumber,
                )
                .semantics { contentDescription = applyCd }
                .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (row.deviceContactId > 0L) {
            ContactPhotoAvatar(
                deviceContactId = row.deviceContactId,
                initials = initial,
                gradientStart = g0,
                gradientEnd = g1,
                size = 44.dp,
                fontSize = 15.sp,
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(g0, g1))),
                contentAlignment = Alignment.Center,
            ) {
                AvatarInitialsText(
                    text = initial,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = row.subtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DialKeyButton(digit: String, letters: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = digit,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
