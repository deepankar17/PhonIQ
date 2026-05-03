package com.phoniq.app.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phoniq.app.R

private data class SearchHit(
    val title: String,
    val subtitle: String,
    val kind: String,
)

@Composable
fun GlobalSearchOverlay(onDismiss: () -> Unit) {
    val query = remember { mutableStateOf("") }
    val hits =
        remember(query.value) {
            sampleHits.filter {
                query.value.isBlank() ||
                    it.title.contains(query.value, ignoreCase = true) ||
                    it.subtitle.contains(query.value, ignoreCase = true)
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
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.search_overlay_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                TextField(
                    value = query.value,
                    onValueChange = { query.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.search_placeholder)) },
                    singleLine = true,
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(hits, key = { it.title + it.subtitle }) { hit ->
                        ListItem(
                            headlineContent = { Text(hit.title) },
                            supportingContent = { Text(hit.subtitle) },
                            overlineContent = { Text(hit.kind) },
                        )
                        HorizontalDivider()
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.action_close))
                }
            }
        }
    }
}

private val sampleHits =
    listOf(
        SearchHit("Priya Sharma", "Recent call · WhatsApp voice", "Calls"),
        SearchHit("HDFCBK", "INR 2,450 debited at BLINKIT", "Messages"),
        SearchHit("Food & dining", "₹8,420 this month · sample", "Money"),
        SearchHit("Rahul Verma", "Outgoing video · Sun", "Calls"),
        SearchHit("VM-VFSOTP", "OTP 482910 · valid 3 min", "Messages"),
    )
