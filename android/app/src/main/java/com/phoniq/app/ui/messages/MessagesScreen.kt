package com.phoniq.app.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.phoniq.app.R
import com.phoniq.app.data.SampleData
import com.phoniq.app.data.model.MessageThread
import com.phoniq.app.data.model.MessageThreadCategory
import com.phoniq.app.data.model.matches

@Composable
fun MessagesScreen(onUserMessage: (String) -> Unit) {
    var category by remember { mutableStateOf(MessageThreadCategory.All) }
    val threads =
        remember(category) {
            SampleData.messageThreads.filter { it.matches(category) }
        }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MessageThreadCategory.entries.forEach { c ->
                FilterChip(
                    selected = category == c,
                    onClick = { category = c },
                    label = { Text(threadCategoryLabel(c)) },
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(threads, key = { it.id }) { thread ->
                ThreadRow(thread = thread, onUserMessage = onUserMessage)
            }
        }
    }
}

@Composable
private fun threadCategoryLabel(c: MessageThreadCategory): String =
    when (c) {
        MessageThreadCategory.All -> stringResource(R.string.msg_filter_all)
        MessageThreadCategory.Unread -> stringResource(R.string.msg_filter_unread)
        MessageThreadCategory.Personal -> stringResource(R.string.msg_filter_personal)
        MessageThreadCategory.Transaction -> stringResource(R.string.msg_filter_transaction)
        MessageThreadCategory.Otp -> stringResource(R.string.msg_filter_otp)
        MessageThreadCategory.Spam -> stringResource(R.string.msg_filter_spam)
    }

@Composable
private fun ThreadRow(
    thread: MessageThread,
    onUserMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    Surface(
        onClick = { onUserMessage(context.getString(R.string.toast_open_thread, thread.title)) },
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
    ) {
        ListItem(
            leadingContent = {
                val initial =
                    thread.title.firstOrNull()?.uppercaseChar()?.toString()
                        ?: "?"
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            headlineContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(thread.title, style = MaterialTheme.typography.titleMedium)
                    if (thread.unread) {
                        Text("·", color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            supportingContent = { Text(thread.snippet, maxLines = 2) },
            overlineContent = {
                thread.subtitleBadge?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall)
                }
            },
            trailingContent = {
                Text(thread.timeLabel, style = MaterialTheme.typography.labelMedium)
            },
        )
    }
}
