package com.phoniq.app.ui.phone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.automirrored.filled.Note
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoniq.app.R
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqOnSurfaceMuted
import com.phoniq.app.util.startSmsCompose

/**
 * Bottom sheet shown after a call ends.
 * Offers: add note, add contact, add favourite, send SMS, block, who is this.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AfterCallSheet(
    callerName: String,
    callerNumber: String,
    durationLabel: String,
    recordingPath: String? = null,
    onDismiss: () -> Unit,
    onUserMessage: (String) -> Unit = {},
    onAddContact: () -> Unit = {},
    onFavorite: () -> Unit = {},
    onWhoIsThis: () -> Unit = {},
    onBlock: () -> Unit = {},
    onSaveCallNote: (String) -> Unit = {},
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val context = LocalContext.current
    var note by remember { mutableStateOf("") }
    var noteSaved by remember { mutableStateOf(false) }
    var showPlayback by remember { mutableStateOf(false) }

    fun openSms(body: String? = null) {
        if (!context.startSmsCompose(callerNumber, body)) {
            onUserMessage(context.getString(R.string.snackbar_no_sms_app))
        } else {
            onDismiss()
        }
    }

    if (showPlayback && recordingPath != null) {
        RecordingPlaybackSheet(
            encPath = recordingPath,
            callerLabel = "$callerName · $durationLabel",
            onDismiss = { showPlayback = false },
        )
        return
    }

    val smsTemplates = stringArrayResource(R.array.after_call_sms_templates)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
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

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AfterCallAction(Icons.Default.PersonAdd, stringResource(R.string.after_call_save_contact), onAddContact)
                AfterCallAction(Icons.Default.ChatBubbleOutline, stringResource(R.string.after_call_send_sms)) { openSms() }
                AfterCallAction(Icons.Default.Star, stringResource(R.string.after_call_favourite)) {
                    onDismiss()
                    onFavorite()
                }
                AfterCallAction(Icons.Default.PersonSearch, stringResource(R.string.after_call_who_is_this)) {
                    onDismiss()
                    onWhoIsThis()
                }
                AfterCallAction(Icons.Default.Block, stringResource(R.string.after_call_block)) {
                    onDismiss()
                    onBlock()
                }
                if (recordingPath != null) {
                    AfterCallAction(Icons.Default.GraphicEq, stringResource(R.string.after_call_play_recording)) {
                        showPlayback = true
                    }
                }
            }

            Text(
                stringResource(R.string.after_call_block_explainer),
                style = MaterialTheme.typography.bodySmall,
                color = PhoniqOnSurfaceMuted,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text(
                stringResource(R.string.after_call_note_heading),
                style = MaterialTheme.typography.labelMedium,
                color = PhoniqAccent,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it; noteSaved = false },
                placeholder = { Text(stringResource(R.string.after_call_note_hint)) },
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
                        onSaveCallNote(note.trim())
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Note, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (noteSaved) {
                                stringResource(R.string.after_call_note_saved_label)
                            } else {
                                stringResource(R.string.after_call_save_note)
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // SMS templates
            Text(
                stringResource(R.string.after_call_quick_message_heading),
                style = MaterialTheme.typography.labelMedium,
                color = PhoniqAccent,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            smsTemplates.forEach { template ->
                Surface(
                    onClick = { openSms(template) },
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
