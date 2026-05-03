package com.phoniq.app.ui.money

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.phoniq.app.R
import com.phoniq.app.data.SampleData
import com.phoniq.app.data.model.CategorySpend
import com.phoniq.app.ui.shell.ShellMenuAction
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqOnSurfaceMuted
import com.phoniq.app.ui.theme.PhoniqSurface
import com.phoniq.app.ui.theme.PhoniqSecondary

@Composable
fun MoneyScreen(
    onUserMessage: (String) -> Unit,
    onMoneyTool: (ShellMenuAction) -> Unit,
) {
    val context = LocalContext.current
    val summary = SampleData.moneySummary
    val categories = SampleData.categorySpends

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PhoniqSurface),
                onClick = { onUserMessage(context.getString(R.string.toast_money_summary_tap)) },
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(summary.monthLabel, style = MaterialTheme.typography.labelMedium)
                    Text(
                        stringResource(R.string.money_spent_label, summary.spentLabel),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        summary.budgetCaption,
                        style = MaterialTheme.typography.bodySmall,
                        color = PhoniqSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    LinearProgressIndicator(
                        progress = { summary.budgetProgress.coerceIn(0f, 1f) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .padding(top = 10.dp),
                    )
                    Text(
                        stringResource(R.string.money_income_label, summary.incomeLabel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        summary.currencyHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = PhoniqOnSurfaceMuted,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
        item {
            MoneyToolsStrip(onTool = onMoneyTool)
        }
        item {
            Text(
                stringResource(R.string.money_donut_section),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
            SpendingDonutPreview(fractions = categories.map { it.fraction to categoryColor(it) })
        }
        item {
            Text(
                stringResource(R.string.money_categories_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        items(categories, key = { it.name }) { row ->
            CategorySpendRow(row = row)
        }
    }
}

private fun categoryColor(row: CategorySpend): Color =
    when (row.name) {
        "Food" -> Color(0xFFFF8A65)
        "Transport" -> Color(0xFF4FC3F7)
        "Bills" -> Color(0xFF9575CD)
        "Shopping" -> Color(0xFFAED581)
        else -> PhoniqAccent
    }

@Composable
private fun SpendingDonutPreview(fractions: List<Pair<Float, Color>>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PhoniqSurface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Canvas(modifier = Modifier.size(120.dp)) {
                var start = -90f
                val stroke = 22.dp.toPx()
                fractions.forEach { (frac, color) ->
                    val sweep = 360f * frac
                    drawArc(
                        color = color,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                        size = Size(size.minDimension, size.minDimension),
                    )
                    start += sweep
                }
            }
            Column {
                Text(
                    stringResource(R.string.money_donut_caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = PhoniqOnSurfaceMuted,
                )
            }
        }
    }
}

@Composable
private fun CategorySpendRow(row: CategorySpend) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PhoniqSurface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(row.name, style = MaterialTheme.typography.titleSmall)
                Text(row.amountLabel, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { row.fraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
        }
    }
}
