package com.phoniq.app.ui.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.phoniq.app.R

enum class MessagesOverflowSheetKind {
    MarkAllRead,
    InboxCleaner,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesOverflowBottomSheet(
    kind: MessagesOverflowSheetKind,
    onDismissRequest: () -> Unit,
    onMarkAllReadConfirm: () -> Unit,
    onInboxDryRun: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        when (kind) {
            MessagesOverflowSheetKind.MarkAllRead ->
                MarkAllReadSheetContent(
                    onDismiss = onDismissRequest,
                    onConfirmMarkAllRead = onMarkAllReadConfirm,
                )
            MessagesOverflowSheetKind.InboxCleaner ->
                InboxCleanerSheetContent(
                    onDismiss = onDismissRequest,
                    onDryRun = onInboxDryRun,
                )
        }
    }
}

@Composable
private fun MarkAllReadSheetContent(
    onDismiss: () -> Unit,
    onConfirmMarkAllRead: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
    ) {
        Text(
            text = stringResource(R.string.sheet_mark_all_read_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.sheet_mark_all_read_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
            TextButton(
                onClick = {
                    onConfirmMarkAllRead()
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.sheet_mark_all_read_confirm))
            }
        }
    }
}

@Composable
private fun InboxCleanerSheetContent(
    onDismiss: () -> Unit,
    onDryRun: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
    ) {
        Text(
            text = stringResource(R.string.sheet_inbox_cleaner_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.sheet_inbox_cleaner_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "• " + stringResource(R.string.sheet_inbox_bullet_promos),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "• " + stringResource(R.string.sheet_inbox_bullet_otp),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "• " + stringResource(R.string.sheet_inbox_bullet_bills),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                onDryRun()
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.sheet_inbox_run_preview))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_close))
        }
    }
}
