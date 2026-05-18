package com.phoniq.app.ui.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.KeyboardOptions
import com.phoniq.app.R
import com.phoniq.app.data.model.ContactPhoneEntry
import com.phoniq.app.ui.theme.PhoniqAccent
import kotlinx.coroutines.launch

/**
 * In-app contact editor used for **both add and edit** flows so we never hand off to the system
 * contacts app. Persists via [PhoneViewModel.saveContact] which writes through ContactsContract.
 */
@Composable
fun EditContactOverlay(
    initialDeviceContactId: Long,
    initialName: String,
    initialPhones: List<ContactPhoneEntry>,
    phoneViewModel: PhoneViewModel,
    onDismiss: () -> Unit,
    onSaved: (deviceContactId: Long) -> Unit,
    onUserMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val isEdit = initialDeviceContactId > 0L

    var nameInput by remember(initialName) { mutableStateOf(initialName) }
    val phones: SnapshotStateList<EditableContactPhone> = remember(initialPhones) {
        initialPhones
            .ifEmpty { listOf(ContactPhoneEntry(number = "", label = null)) }
            .map { EditableContactPhone(number = it.number, label = it.label.orEmpty()) }
            .toMutableStateList()
    }
    var saving by remember { mutableStateOf(false) }
    var pendingCustomFor by remember { mutableStateOf<Int?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = scheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
            ) {
                EditorTopBar(
                    title =
                        if (isEdit) stringResource(R.string.contact_editor_title_edit)
                        else stringResource(R.string.contact_editor_title_new),
                    saving = saving,
                    onCancel = onDismiss,
                    onSave = {
                        val cleanedPhones =
                            phones
                                .filter { it.number.trim().isNotEmpty() }
                                .map {
                                    ContactPhoneEntry(
                                        number = it.number.trim(),
                                        label = it.label.trim().takeIf { l -> l.isNotEmpty() },
                                    )
                                }
                        if (nameInput.trim().isEmpty() && cleanedPhones.isEmpty()) {
                            onUserMessage(
                                context.getString(R.string.contact_editor_validation_name_or_number),
                            )
                            return@EditorTopBar
                        }
                        saving = true
                        scope.launch {
                            val newId =
                                phoneViewModel.saveContact(
                                    existingDeviceContactId = initialDeviceContactId,
                                    name = nameInput.trim(),
                                    phones = cleanedPhones,
                                )
                            saving = false
                            if (newId > 0L || (isEdit && newId == initialDeviceContactId)) {
                                onUserMessage(
                                    context.getString(
                                        if (isEdit) R.string.contact_editor_save_ok_update
                                        else R.string.contact_editor_save_ok_new,
                                    ),
                                )
                                onSaved(if (newId > 0L) newId else initialDeviceContactId)
                            } else {
                                onUserMessage(
                                    context.getString(R.string.contact_editor_save_failed),
                                )
                            }
                        }
                    },
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(stringResource(R.string.contact_editor_name)) },
                        singleLine = true,
                        keyboardOptions =
                            KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next,
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    phones.forEachIndexed { idx, entry ->
                        ContactPhoneInputRow(
                            entry = entry,
                            onNumberChange = { phones[idx] = entry.copy(number = it) },
                            onLabelChosen = { phones[idx] = entry.copy(label = it) },
                            onRequestCustomLabel = { pendingCustomFor = idx },
                            onRemove = if (phones.size > 1) {
                                { phones.removeAt(idx) }
                            } else null,
                        )
                    }
                    TextButton(
                        onClick = {
                            phones.add(EditableContactPhone(number = "", label = ""))
                        },
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(R.string.contact_editor_add_phone))
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    val pendingIdx = pendingCustomFor
    if (pendingIdx != null) {
        val current = phones.getOrNull(pendingIdx)
        if (current == null) {
            pendingCustomFor = null
        } else {
            CustomLabelDialog(
                initial = current.label,
                onCancel = { pendingCustomFor = null },
                onConfirm = { newLabel ->
                    phones[pendingIdx] = current.copy(label = newLabel)
                    pendingCustomFor = null
                },
            )
        }
    }
}

private data class EditableContactPhone(val number: String, val label: String)

@Composable
private fun EditorTopBar(
    title: String,
    saving: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.contact_editor_cancel),
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            )
            Button(
                onClick = onSave,
                enabled = !saving,
                colors = ButtonDefaults.buttonColors(containerColor = PhoniqAccent),
            ) {
                Text(stringResource(R.string.contact_editor_save))
            }
        }
    }
}

@Composable
private fun ContactPhoneInputRow(
    entry: EditableContactPhone,
    onNumberChange: (String) -> Unit,
    onLabelChosen: (String) -> Unit,
    onRequestCustomLabel: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = scheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LabelPickerButton(
                    label = entry.label,
                    onLabelChosen = onLabelChosen,
                    onRequestCustom = onRequestCustomLabel,
                )
                Spacer(Modifier.weight(1f))
                if (onRemove != null) {
                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Default.RemoveCircleOutline,
                            contentDescription =
                                stringResource(R.string.contact_editor_remove_phone),
                            tint = scheme.error,
                        )
                    }
                }
            }
            OutlinedTextField(
                value = entry.number,
                onValueChange = onNumberChange,
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LabelPickerButton(
    label: String,
    onLabelChosen: (String) -> Unit,
    onRequestCustom: () -> Unit,
) {
    val options = listOf(
        stringResource(R.string.contact_editor_label_mobile),
        stringResource(R.string.contact_editor_label_home),
        stringResource(R.string.contact_editor_label_work),
        stringResource(R.string.contact_editor_label_main),
        stringResource(R.string.contact_editor_label_whatsapp),
        stringResource(R.string.contact_editor_label_other),
    )
    val customText = stringResource(R.string.contact_editor_label_custom)
    var expanded by remember { mutableStateOf(false) }
    val display = label.takeIf { it.isNotEmpty() } ?: options.first()
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = display,
                color = PhoniqAccent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = stringResource(R.string.contact_editor_label_change),
                tint = PhoniqAccent,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        expanded = false
                        onLabelChosen(opt)
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(customText) },
                onClick = {
                    expanded = false
                    onRequestCustom()
                },
            )
        }
    }
}

@Composable
private fun CustomLabelDialog(
    initial: String,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.contact_editor_custom_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text(stringResource(R.string.contact_editor_custom_dialog_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }) {
                Text(stringResource(R.string.contact_editor_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.contact_editor_cancel))
            }
        },
    )
}
