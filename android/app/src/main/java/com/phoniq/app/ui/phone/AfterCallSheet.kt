package com.phoniq.app.ui.phone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqOnSurfaceMuted

/**
 * Bottom sheet shown after a call ends.
 * Offers: add note, add contact, add favourite, send SMS, block, who is this.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AfterCallSheet(
    callerName: String,
    callerNumber: String,
    durationLabel: String,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    var note by remember { mutableStateOf("") }
    var noteSaved by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            // Header
            Text(
                callerName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "$callerNumber  ·  $durationLabel",
                style = MaterialTheme.typography.bodySmall,
                color = PhoniqOnSurfaceMuted,
            )

            Spacer(Modifier.height(16.dp))

            // Quick actions grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                AfterCallAction(Icons.Default.PersonAdd, "Save Contact", onDismiss)
                AfterCallAction(Icons.Default.ChatBubbleOutline, "Send SMS", onDismiss)
                AfterCallAction(Icons.Default.Star, "Favourite", onDismiss)
                AfterCallAction(Icons.Default.PersonSearch, "Who is this?", onDismiss)
                AfterCallAction(Icons.Default.Block, "Block", onDismiss)
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // Call note
            Text(
                "Call note",
                style = MaterialTheme.typography.labelMedium,
                color = PhoniqAccent,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it; noteSaved = false },
                placeholder = { Text("What was this call about?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )
            if (note.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = {
                        noteSaved = true
                        // TODO: persist via PhoneViewModel
                    }) {
                        Icon(Icons.Default.Note, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (noteSaved) "Saved" else "Save note")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // SMS templates
            Text(
                "Send a quick message",
                style = MaterialTheme.typography.labelMedium,
                color = PhoniqAccent,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            listOf(
                "I'll call you back shortly.",
                "Sorry, I missed your call.",
                "Can we talk later?",
            ).forEach { template ->
                Surface(
                    onClick = onDismiss,
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                ) {
                    Text(
                        template,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AfterCallAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp),
    ) {
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = PhoniqAccent,
                modifier = Modifier
                    .padding(12.dp)
                    .size(24.dp),
            )
        }
        Text(
            label,
            fontSize = 9.sp,
            color = PhoniqOnSurfaceMuted,
            modifier = Modifier.padding(top = 4.dp),
            maxLines = 1,
        )
    }
}
