package com.phoniq.app.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phoniq.app.R
import com.phoniq.app.data.model.CommunicationInsights
import com.phoniq.app.data.model.formatTalkDuration
import com.phoniq.app.ui.theme.PhoniqOnSurfaceMuted

/** On-device call statistics from the local call log (no network). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunicationInsightsOverlay(
    insights: CommunicationInsights,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.menu_phone_insights)) },
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
                            .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    Text(
                        text = stringResource(R.string.insights_disclosure),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                    if (!insights.hasData) {
                        Text(
                            text = stringResource(R.string.insights_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = PhoniqOnSurfaceMuted,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    } else {
                        InsightRow(
                            stringResource(R.string.insights_total_calls),
                            insights.totalCalls.toString(),
                        )
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        InsightRow(
                            stringResource(R.string.insights_pstn_calls),
                            insights.pstnCalls.toString(),
                        )
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        InsightRow(
                            stringResource(R.string.insights_missed),
                            insights.missedCount.toString(),
                        )
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        InsightRow(
                            stringResource(R.string.insights_incoming),
                            insights.incomingCount.toString(),
                        )
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        InsightRow(
                            stringResource(R.string.insights_outgoing),
                            insights.outgoingCount.toString(),
                        )
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        InsightRow(
                            stringResource(R.string.insights_rejected),
                            insights.rejectedCount.toString(),
                        )
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        InsightRow(
                            stringResource(R.string.insights_voice_time),
                            formatTalkDuration(insights.totalTalkSeconds),
                        )
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        val topLine =
                            insights.topContactLabel?.let { label ->
                                stringResource(
                                    R.string.insights_top_contact_line,
                                    label,
                                    insights.topContactCallCount,
                                )
                            }
                                ?: stringResource(R.string.insights_top_contact_none)
                        InsightRow(
                            stringResource(R.string.insights_top_contact),
                            topLine,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightRow(
    label: String,
    value: String,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = PhoniqOnSurfaceMuted)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 4,
        )
    }
}
