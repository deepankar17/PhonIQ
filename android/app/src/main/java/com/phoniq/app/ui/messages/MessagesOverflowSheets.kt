package com.phoniq.app.ui.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phoniq.app.R
import com.phoniq.app.data.model.MessageThread
import com.phoniq.app.data.model.billDueHintLabel
import com.phoniq.app.ui.theme.PhoniqBorderSoft
import com.phoniq.app.ui.theme.PhoniqTextSecondaryMock

enum class MessagesOverflowSheetKind {
    MarkAllRead,
    InboxCleaner,
    BillHygiene,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesOverflowBottomSheet(
    kind: MessagesOverflowSheetKind,
    onDismissRequest: () -> Unit,
    onMarkAllReadConfirm: () -> Unit,
    onInboxDryRun: () -> Unit,
    billThreads: List<MessageThread> = emptyList(),
    onOpenBillThread: (String) -> Unit = {},
    onBillFocusFilter: () -> Unit = {},
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
            MessagesOverflowSheetKind.BillHygiene ->
                BillHygieneSheetContent(
                    billThreads = billThreads,
                    onDismiss = onDismissRequest,
                    onOpenThread = onOpenBillThread,
                    onFocusBillFilter = onBillFocusFilter,
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

@Composable
private fun BillHygieneSheetContent(
    billThreads: List<MessageThread>,
    onDismiss: () -> Unit,
    onOpenThread: (String) -> Unit,
    onFocusBillFilter: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.sheet_bill_hygiene_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.sheet_bill_hygiene_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                onFocusBillFilter()
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.sheet_bill_hygiene_open_filter))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.sheet_bill_hygiene_threads_heading),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(8.dp))
        if (billThreads.isEmpty()) {
            Text(
                text = stringResource(R.string.sheet_bill_hygiene_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = PhoniqTextSecondaryMock,
            )
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
            ) {
                items(billThreads, key = { it.id }) { thread ->
                    val hint = thread.billDueHintLabel()
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOpenThread(thread.id)
                                    onDismiss()
                                }
                                .padding(vertical = 10.dp),
                    ) {
                        Text(
                            text = thread.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (hint != null) {
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = thread.snippet,
                            style = MaterialTheme.typography.bodySmall,
                            color = PhoniqTextSecondaryMock,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    HorizontalDivider(color = PhoniqBorderSoft, thickness = 1.dp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_close))
        }
    }
}
