package com.phoniq.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phoniq.app.R
import com.phoniq.app.telecom.CallRecordingPlayer
import java.io.File

/** Local (encrypted) call recordings — on-device only, listed from the app-private recordings dir. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallRecordingLibraryOverlay(onDismiss: () -> Unit, onUserMessage: (String) -> Unit) {
    val context = LocalContext.current
    val recordingsDir = remember { File(context.filesDir, "recordings").also { it.mkdirs() } }
    val encFiles =
        remember(recordingsDir.path) {
            recordingsDir.listFiles { f -> f.isFile && f.name.endsWith(".enc") }?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        }

    val player = remember { CallRecordingPlayer(context) }
    var playingPath by remember { mutableStateOf<String?>(null) }
    val playbackError = stringResource(R.string.recording_playback_error)

    DisposableEffect(Unit) {
        player.onError = { onUserMessage(it.ifBlank { playbackError }) }
        onDispose {
            player.stop()
            player.onError = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.menu_phone_recording)) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
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
                            .padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.recording_library_disclosure),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    if (encFiles.isEmpty()) {
                        Text(
                            text = stringResource(R.string.recording_library_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            items(encFiles, key = { it.absolutePath }) { file ->
                                val isPlaying = playingPath == file.absolutePath && player.isPlaying
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isPlaying) {
                                                    player.stop()
                                                    playingPath = null
                                                } else {
                                                    player.stop()
                                                    playingPath = file.absolutePath
                                                    player.play(file.absolutePath)
                                                }
                                            }
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                            .padding(horizontal = 12.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(file.nameWithoutExtension, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            }
                        }
                    }
                }
            }
        }
    }
}
