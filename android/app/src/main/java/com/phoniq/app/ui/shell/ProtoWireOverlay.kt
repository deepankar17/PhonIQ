package com.phoniq.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phoniq.app.R
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqOnSurfaceMuted
import com.phoniq.app.ui.theme.PhoniqSurface

private val TealAccent = Color(0xFF00D4AA)

/** Full-screen proto-wire overlay keyed by ShellMenuAction (mockup `proto-generic-overlay` parity). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtoWireOverlay(action: ShellMenuAction, onDismiss: () -> Unit) {
    val title = wireTitle(action)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = { Text(title) },
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    WireBanner(title)
                    when (action) {
                        ShellMenuAction.PhoneCommunicationInsights -> CommInsightsBody()
                        ShellMenuAction.PhoneAfterCall -> AfterCallBody(onDismiss)
                        ShellMenuAction.PhoneWhoIsThis -> WhoIsThisBody(onDismiss)
                        else -> SimpleWireBody(action)
                    }
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun WireBanner(title: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = PhoniqAccent.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Wire · $title",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
            color = PhoniqAccent,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun WireCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PhoniqSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(4.dp)) { content() }
    }
}

@Composable
private fun WireRow(label: String, value: String = "", isChip: Boolean = false, isLast: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        if (value.isNotEmpty()) {
            if (isChip) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PhoniqAccent.copy(alpha = 0.12f),
                ) {
                    Text(
                        value,
                        style = MaterialTheme.typography.labelSmall,
                        color = PhoniqAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            } else {
                Text(value, style = MaterialTheme.typography.bodyMedium, color = PhoniqOnSurfaceMuted)
            }
        }
    }
    if (!isLast) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
private fun WireActionRow(label: String, isLast: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text("›", style = MaterialTheme.typography.bodyLarge, color = PhoniqOnSurfaceMuted)
    }
    if (!isLast) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

// ── Specific wire bodies ────────────────────────────────────────────────────

@Composable
private fun CommInsightsBody() {
    Text(
        "This week",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(vertical = 4.dp),
    )
    WireCard {
        WireRow("Voice time", "3h 12m")
        WireRow("Top contact", "Priya · 48m")
        WireRow("Missed trend", "+2 vs last week", isChip = true, isLast = true)
    }
    Text(
        "Chart",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
    )
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PhoniqSurface,
        modifier = Modifier.fillMaxWidth().height(120.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                "Sparkline / bar chart placeholder",
                style = MaterialTheme.typography.bodySmall,
                color = PhoniqOnSurfaceMuted,
            )
        }
    }
}

@Composable
private fun AfterCallBody(onDismiss: () -> Unit) {
    Text(
        "Actions after call ends",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(vertical = 4.dp),
    )
    WireCard {
        WireActionRow("Add to contacts")
        WireActionRow("Add note")
        WireActionRow("Block number")
        WireActionRow("SMS template", isLast = true)
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "Shown after PSTN call ends · scroll for more actions",
            style = MaterialTheme.typography.bodySmall,
            color = PhoniqOnSurfaceMuted,
            modifier = Modifier.padding(12.dp),
        )
    }
    Button(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = PhoniqAccent),
    ) {
        Text("Got it", color = Color.White)
    }
}

@Composable
private fun WhoIsThisBody(onDismiss: () -> Unit) {
    Text(
        "Context",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(vertical = 4.dp),
    )
    WireCard {
        WireRow("Last call", "Tue · 2 missed")
        WireRow("Last SMS", "OTP 12 Apr")
        WireRow("Your note", "—", isLast = true)
    }
    Button(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = PhoniqAccent),
    ) {
        Text("Open thread", color = Color.White)
    }
}

@Composable
private fun SimpleWireBody(action: ShellMenuAction) {
    val strings = action.wireStrings()
    if (strings != null) {
        val (_, bodyRes) = strings
        Text(
            stringResource(bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    } else {
        Text(
            "Wire placeholder — implementation tracked in PROJECT.md",
            style = MaterialTheme.typography.bodyLarge,
            color = PhoniqOnSurfaceMuted,
        )
    }
}

private fun wireTitle(action: ShellMenuAction): String =
    when (action) {
        ShellMenuAction.PhoneCommunicationInsights -> "Communication insights"
        ShellMenuAction.PhoneAfterCall -> "End-of-call sheet"
        ShellMenuAction.PhoneWhoIsThis -> "Who is this?"
        ShellMenuAction.PhoneMergeContacts -> "Merge contacts"
        ShellMenuAction.PhoneRecording -> "Call recording"
        ShellMenuAction.MessagesBillHygiene -> "Bill reminders & hygiene"
        ShellMenuAction.MessagesOtpCenter -> "OTP center"
        ShellMenuAction.MoneyBillDue -> "Bill due"
        ShellMenuAction.MoneyRecurring -> "Recurring"
        ShellMenuAction.MoneySalaryYearly -> "Salary (FY)"
        ShellMenuAction.MoneyInvestments -> "Investments"
        ShellMenuAction.MoneyExport -> "Export"
        else -> "Wire"
    }
