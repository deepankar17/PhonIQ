package com.phoniq.app.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phoniq.app.R
import com.phoniq.app.data.model.WhoIsThisSnapshot
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqOnSurfaceMuted
import com.phoniq.app.ui.theme.PhoniqSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhoIsThisOverlay(
    snapshot: WhoIsThisSnapshot,
    numberField: String,
    onNumberChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onOpenMessageThread: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.menu_phone_who_is_this)) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_back),
                                )
                            }
                        },
                    )
                },
            ) { padding ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 20.dp)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.who_is_this_disclosure),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = numberField,
                        onValueChange = onNumberChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.who_is_this_phone_label)) },
                        placeholder = { Text(stringResource(R.string.who_is_this_phone_hint)) },
                        singleLine = true,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done,
                            ),
                        keyboardActions = KeyboardActions(onDone = { }),
                    )
                    if (snapshot.contactName != null) {
                        Text(
                            text = stringResource(R.string.who_is_this_saved_as, snapshot.contactName),
                            style = MaterialTheme.typography.titleSmall,
                            color = PhoniqAccent,
                        )
                    }
                    if (!snapshot.hasData && numberField.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.who_is_this_empty_for_number),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PhoniqOnSurfaceMuted,
                        )
                    }
                    WhoSectionTitle(stringResource(R.string.who_is_this_section_call))
                    WhoInfoCard {
                        Text(
                            snapshot.lastCallLine ?: stringResource(R.string.who_is_this_none),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (snapshot.lastCallLine != null) MaterialTheme.colorScheme.onSurface else PhoniqOnSurfaceMuted,
                        )
                    }
                    WhoSectionTitle(stringResource(R.string.who_is_this_section_sms))
                    WhoInfoCard {
                        if (snapshot.lastSmsPreview != null) {
                            if (snapshot.lastSmsDateLine != null) {
                                Text(
                                    snapshot.lastSmsDateLine,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PhoniqOnSurfaceMuted,
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            Text(snapshot.lastSmsPreview, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text(
                                stringResource(R.string.who_is_this_none),
                                style = MaterialTheme.typography.bodyMedium,
                                color = PhoniqOnSurfaceMuted,
                            )
                        }
                    }
                    WhoSectionTitle(stringResource(R.string.who_is_this_section_notes))
                    WhoInfoCard {
                        val callN = snapshot.callNote
                        val contactN = snapshot.contactNote
                        when {
                            callN == null && contactN == null ->
                                Text(
                                    stringResource(R.string.who_is_this_no_notes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PhoniqOnSurfaceMuted,
                                )
                            else -> {
                                if (callN != null) {
                                    Text(
                                        stringResource(R.string.who_is_this_note_call),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = PhoniqOnSurfaceMuted,
                                    )
                                    Text(callN, style = MaterialTheme.typography.bodyMedium)
                                    if (contactN != null) Spacer(Modifier.height(8.dp))
                                }
                                if (contactN != null) {
                                    Text(
                                        stringResource(R.string.who_is_this_note_contact),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = PhoniqOnSurfaceMuted,
                                    )
                                    Text(contactN, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    Button(
                        onClick = onOpenMessageThread,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = numberField.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PhoniqAccent),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.who_is_this_open_thread), color = Color.White)
                        }
                    }
                    Spacer(Modifier.height(72.dp))
                }
            }
        }
    }
}

@Composable
private fun WhoSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun WhoInfoCard(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = PhoniqSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) { content() }
    }
}
