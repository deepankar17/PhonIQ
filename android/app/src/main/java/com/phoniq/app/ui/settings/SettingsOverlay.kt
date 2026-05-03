package com.phoniq.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phoniq.app.R

private enum class SettingsPane {
    Root,
    Personalization,
    DataDevice,
}

/** Settings root + Personalization + Data & device (mockup `design/phoniq-mockup-v1.html`). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsFullScreenOverlay(onDismiss: () -> Unit) {
    var pane by remember { mutableStateOf(SettingsPane.Root) }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                when (pane) {
                    SettingsPane.Root ->
                        TopAppBar(
                            title = { Text(stringResource(R.string.settings_title)) },
                            actions = {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close))
                                }
                            },
                        )
                    SettingsPane.Personalization ->
                        TopAppBar(
                            title = { Text(stringResource(R.string.settings_personalization_title)) },
                            navigationIcon = {
                                IconButton(onClick = { pane = SettingsPane.Root }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.cd_back),
                                    )
                                }
                            },
                        )
                    SettingsPane.DataDevice ->
                        TopAppBar(
                            title = { Text(stringResource(R.string.settings_data_device_title)) },
                            navigationIcon = {
                                IconButton(onClick = { pane = SettingsPane.Root }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.cd_back),
                                    )
                                }
                            },
                        )
                }
            },
        ) { padding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
            ) {
                when (pane) {
                    SettingsPane.Root -> {
                        Text(
                            text = stringResource(R.string.settings_group_appearance),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                        )
                        Surface(onClick = { pane = SettingsPane.Personalization }, tonalElevation = 0.dp) {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.settings_row_personalization)) },
                                supportingContent = { Text(stringResource(R.string.settings_row_personalization_sub)) },
                                trailingContent = { Text("›", style = MaterialTheme.typography.bodyLarge) },
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                        Surface(onClick = { pane = SettingsPane.DataDevice }, tonalElevation = 0.dp) {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.settings_row_data_device)) },
                                supportingContent = { Text(stringResource(R.string.settings_row_data_device_sub)) },
                                trailingContent = { Text("›", style = MaterialTheme.typography.bodyLarge) },
                            )
                        }
                    }
                    SettingsPane.Personalization -> PersonalizationBody()
                    SettingsPane.DataDevice -> DataDeviceBody()
                }
            }
        }
        }
    }
}

@Composable
private fun PersonalizationBody() {
    var denseThreads by remember { mutableStateOf(true) }
    var showDialpadLetters by remember { mutableStateOf(true) }
    Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.personalization_theme_section),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(8.dp),
        )
        Text(
            text = stringResource(R.string.personalization_theme_deep_dark),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.personalization_dense_threads)) },
            supportingContent = { Text(stringResource(R.string.personalization_dense_threads_sub)) },
            trailingContent = {
                Switch(checked = denseThreads, onCheckedChange = { denseThreads = it })
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.personalization_dialpad_letters)) },
            supportingContent = { Text(stringResource(R.string.personalization_dialpad_letters_sub)) },
            trailingContent = {
                Switch(checked = showDialpadLetters, onCheckedChange = { showDialpadLetters = it })
            },
        )
        Text(
            text = stringResource(R.string.personalization_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun DataDeviceBody() {
    Column(Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.data_device_backup_intro),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.data_device_backup_wire),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
