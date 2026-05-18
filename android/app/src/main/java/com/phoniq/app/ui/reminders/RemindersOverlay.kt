package com.phoniq.app.ui.reminders

import android.content.ClipData
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phoniq.app.R
import com.phoniq.app.domain.reminders.tryStartUpiPay
import java.text.DateFormat
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DAY_MS = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RemindersOverlay(
    reminders: List<ReminderRow>,
    remindBeforeDays: Int,
    onRemindBeforeDaysChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onAdd: (title: String, dueAtMillis: Long) -> Unit,
    onSetDone: (id: Long, done: Boolean) -> Unit,
    onSnooze: (id: Long, addMillis: Long) -> Unit,
    onImportBillSms: () -> Int,
    onOpenThread: (String) -> Unit,
    onUserMessage: (String) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    var draftTitle by remember { mutableStateOf("") }
    var draftDueOffsetDays by remember { mutableIntStateOf(1) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val dateFmt = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            now = System.currentTimeMillis()
        }
    }

    val overdue = remember(reminders, now) { reminders.filter { it.section(now) == ReminderSection.Overdue } }
    val upcoming = remember(reminders, now) { reminders.filter { it.section(now) == ReminderSection.Upcoming } }
    val done = remember(reminders, now) { reminders.filter { it.section(now) == ReminderSection.Done } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.reminders_title)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val n = onImportBillSms()
                                val msg =
                                    if (n > 0) {
                                        context.getString(R.string.reminders_import_added, n)
                                    } else {
                                        context.getString(R.string.reminders_import_none)
                                    }
                                onUserMessage(msg)
                            },
                        ) {
                            Icon(Icons.Outlined.Event, contentDescription = stringResource(R.string.reminders_import_cd))
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAdd = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.reminders_add_cd))
                }
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            stringResource(R.string.reminders_policy_remind_before),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            listOf(1, 3, 7).forEach { d ->
                                FilterChip(
                                    selected = remindBeforeDays == d,
                                    onClick = { onRemindBeforeDaysChange(d) },
                                    label = { Text(stringResource(R.string.reminders_remind_before_days, d)) },
                                )
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                }

                if (reminders.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.reminders_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        )
                    }
                } else {
                    item {
                        SectionHeader(stringResource(R.string.reminders_section_overdue), overdue.size)
                    }
                    items(overdue, key = { it.id }) { r ->
                        ReminderRowItem(
                            r = r,
                            now = now,
                            dateFmt = dateFmt,
                            onSetDone = onSetDone,
                            onSnooze = onSnooze,
                            onOpenThread = onOpenThread,
                            clipboard = clipboard,
                            scope = scope,
                            context = context,
                            onUserMessage = onUserMessage,
                        )
                    }
                    item {
                        SectionHeader(stringResource(R.string.reminders_section_upcoming), upcoming.size)
                    }
                    items(upcoming, key = { it.id }) { r ->
                        ReminderRowItem(
                            r = r,
                            now = now,
                            dateFmt = dateFmt,
                            onSetDone = onSetDone,
                            onSnooze = onSnooze,
                            onOpenThread = onOpenThread,
                            clipboard = clipboard,
                            scope = scope,
                            context = context,
                            onUserMessage = onUserMessage,
                        )
                    }
                    item {
                        SectionHeader(stringResource(R.string.reminders_section_done), done.size)
                    }
                    items(done, key = { it.id }) { r ->
                        ReminderRowItem(
                            r = r,
                            now = now,
                            dateFmt = dateFmt,
                            onSetDone = onSetDone,
                            onSnooze = onSnooze,
                            onOpenThread = onOpenThread,
                            clipboard = clipboard,
                            scope = scope,
                            context = context,
                            onUserMessage = onUserMessage,
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(stringResource(R.string.reminders_add_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = { draftTitle = it },
                        label = { Text(stringResource(R.string.reminders_add_label)) },
                        singleLine = false,
                    )
                    Text(
                        stringResource(R.string.reminders_add_due_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(0, 1, 3, 7).forEach { dayOffset ->
                            FilterChip(
                                selected = draftDueOffsetDays == dayOffset,
                                onClick = { draftDueOffsetDays = dayOffset },
                                label = {
                                    Text(
                                        when (dayOffset) {
                                            0 -> stringResource(R.string.reminders_due_today)
                                            1 -> stringResource(R.string.reminders_due_tomorrow)
                                            else -> stringResource(R.string.reminders_due_in_days, dayOffset)
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val t = draftTitle.trim()
                        if (t.isNotEmpty()) {
                            val due =
                                Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, draftDueOffsetDays)
                                    set(Calendar.HOUR_OF_DAY, 9)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            onAdd(t, due)
                            draftTitle = ""
                        }
                        showAdd = false
                    },
                ) {
                    Text(stringResource(R.string.reminders_add_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) {
                    Text(stringResource(R.string.reminders_add_cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Text(
        "$title · $count",
        style = MaterialTheme.typography.titleSmall,
        color = PhoniqSectionHeaderTint(),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Composable
private fun PhoniqSectionHeaderTint() =
    MaterialTheme.colorScheme.primary

@Composable
private fun ReminderRowItem(
    r: ReminderRow,
    now: Long,
    dateFmt: DateFormat,
    onSetDone: (id: Long, done: Boolean) -> Unit,
    onSnooze: (id: Long, addMillis: Long) -> Unit,
    onOpenThread: (String) -> Unit,
    clipboard: Clipboard,
    scope: CoroutineScope,
    context: Context,
    onUserMessage: (String) -> Unit,
) {
    var menuOpen by rememberSaveable(r.id) { mutableStateOf(false) }
    val rel =
        when {
            r.isDone -> stringResource(R.string.reminders_done_label)
            r.dueAt < now -> {
                val delta = now - r.dueAt
                val days = (delta / DAY_MS).toInt().coerceAtLeast(0)
                stringResource(R.string.reminders_overdue_by_days, days)
            }
            else -> {
                val delta = r.dueAt - now
                val days = ((delta + DAY_MS - 1) / DAY_MS).toInt().coerceAtLeast(0)
                stringResource(R.string.reminders_due_in_days_short, days)
            }
        }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = r.isDone,
            onCheckedChange = { onSetDone(r.id, it) },
        )
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(r.title, style = MaterialTheme.typography.titleSmall)
            Text(
                "${dateFmt.format(r.dueAt)} · $rel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            r.detail?.let { d ->
                Text(
                    d,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        BoxWithReminderMenu(
            r = r,
            menuOpen = menuOpen,
            onMenuOpenChange = { menuOpen = it },
            onSnooze = onSnooze,
            onOpenThread = onOpenThread,
            clipboard = clipboard,
            scope = scope,
            context = context,
            onUserMessage = onUserMessage,
        )
    }
}

@Composable
private fun BoxWithReminderMenu(
    r: ReminderRow,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onSnooze: (id: Long, addMillis: Long) -> Unit,
    onOpenThread: (String) -> Unit,
    clipboard: Clipboard,
    scope: CoroutineScope,
    context: Context,
    onUserMessage: (String) -> Unit,
) {
    Box {
        IconButton(onClick = { onMenuOpenChange(true) }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.reminders_row_menu_cd))
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { onMenuOpenChange(false) }) {
            if (!r.isDone) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reminders_snooze_1d)) },
                    onClick = {
                        onMenuOpenChange(false)
                        onSnooze(r.id, DAY_MS)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reminders_snooze_3d)) },
                    onClick = {
                        onMenuOpenChange(false)
                        onSnooze(r.id, 3 * DAY_MS)
                    },
                )
            }
            r.linkedThreadId?.takeIf { it.isNotEmpty() }?.let { tid ->
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Outlined.Event, contentDescription = null) },
                    text = { Text(stringResource(R.string.reminders_open_thread)) },
                    onClick = {
                        onMenuOpenChange(false)
                        onOpenThread(tid)
                    },
                )
            }
            r.upiVpa?.takeIf { it.isNotEmpty() }?.let { vpa ->
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Outlined.Savings, contentDescription = null) },
                    text = { Text(stringResource(R.string.reminders_copy_upi)) },
                    onClick = {
                        onMenuOpenChange(false)
                        scope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("UPI", vpa)))
                        }
                        onUserMessage(context.getString(R.string.reminders_upi_copied))
                    },
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Outlined.Savings, contentDescription = null) },
                    text = { Text(stringResource(R.string.reminders_open_upi_pay)) },
                    onClick = {
                        onMenuOpenChange(false)
                        val ok = context.tryStartUpiPay(vpa)
                        onUserMessage(
                            if (ok) context.getString(R.string.reminders_upi_pay_launched)
                            else context.getString(R.string.reminders_upi_pay_failed),
                        )
                    },
                )
            }
        }
    }
}
