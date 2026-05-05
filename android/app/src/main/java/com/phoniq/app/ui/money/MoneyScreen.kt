package com.phoniq.app.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoniq.app.R
import com.phoniq.app.data.SampleData
import com.phoniq.app.data.model.CategorySpend
import com.phoniq.app.data.model.MoneySummary
import com.phoniq.app.data.model.RecentTransaction
import com.phoniq.app.ui.shell.ShellMenuAction
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqOnSurfaceMuted
import com.phoniq.app.ui.theme.PhoniqSecondary
import com.phoniq.app.ui.theme.PhoniqSurface
import kotlin.math.roundToInt

private val ColorFood      = PhoniqAccent
private val ColorShopping  = Color(0xFFF5A623)
private val ColorBills     = Color(0xFFFF6B6B)
private val ColorTransport = Color(0xFF00D4AA)
private val ColorOthers    = Color(0xFF888888)
private val ColorCredit    = Color(0xFF00D4AA)

@Composable
fun MoneyScreen(
    onUserMessage: (String) -> Unit,
    onMoneyTool: (ShellMenuAction) -> Unit,
) {
    val context = LocalContext.current
    val summary = SampleData.moneySummary
    val categories = SampleData.categorySpends
    val transactions = SampleData.recentTransactions

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    ) {
        item {
            SummaryHeroCard(
                summary = summary,
                onClick = { onUserMessage(context.getString(R.string.toast_money_summary_tap)) },
            )
        }
        item { MoneyToolsStrip(onTool = onMoneyTool) }
        item {
            Text(
                stringResource(R.string.money_donut_section),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
            )
            SpendingDonutCard(categories = categories, centerAmount = summary.spentLabel)
        }
        item {
            Text(
                stringResource(R.string.money_categories_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        item { BudgetTrackerGrid(categories) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.money_txn_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = {
                    onUserMessage(context.getString(R.string.toast_money_summary_tap))
                }) {
                    Text(stringResource(R.string.money_txn_see_all))
                }
            }
        }
        items(transactions, key = { it.merchant + it.dateLine }) { txn ->
            TransactionRow(txn)
            if (txn !== transactions.last()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SummaryHeroCard(summary: MoneySummary, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PhoniqSurface),
        onClick = onClick,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(summary.monthLabel, style = MaterialTheme.typography.labelMedium)
            Text(
                stringResource(R.string.money_spent_label, summary.spentLabel),
                style = MaterialTheme.typography.headlineSmall,
            )
            val budgetParts = summary.budgetCaption.split(" · ")
            val budgetDisplay = buildAnnotatedString {
                append(budgetParts.getOrElse(0) { summary.budgetCaption })
                if (budgetParts.size > 1) {
                    append(" · ")
                    withStyle(SpanStyle(color = ColorCredit)) { append(budgetParts[1]) }
                }
            }
            Text(
                budgetDisplay,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            LinearProgressIndicator(
                progress = { summary.budgetProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 10.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.money_savings_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    summary.savingsLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorCredit,
                )
            }
            Text(
                summary.currencyHint,
                style = MaterialTheme.typography.labelSmall,
                color = PhoniqOnSurfaceMuted,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SpendingDonutCard(categories: List<CategorySpend>, centerAmount: String) {
    val colors = categories.map { categoryColor(it) }
    val fractions = categories.zip(colors).map { (cat, color) -> cat.fraction to color }
    Card(
        colors = CardDefaults.cardColors(containerColor = PhoniqSurface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DonutCanvas(
                fractions = fractions,
                centerAmount = centerAmount,
                modifier = Modifier.size(150.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                categories.zip(colors).forEach { (cat, color) ->
                    DonutLegendItem(
                        color = color,
                        label = cat.name,
                        pct = "${(cat.fraction * 100).roundToInt()}%",
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutCanvas(
    fractions: List<Pair<Float, Color>>,
    centerAmount: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            val strokePx = 24.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(size.minDimension - strokePx, size.minDimension - strokePx)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = Color(0xFF1A1A2E),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Butt),
                topLeft = topLeft,
                size = arcSize,
            )

            var start = -90f
            fractions.forEach { (frac, color) ->
                val sweep = 360f * frac
                drawArc(
                    color = color,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Butt),
                    topLeft = topLeft,
                    size = arcSize,
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerAmount,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "this month",
                color = Color(0xFF888888),
                fontSize = 8.sp,
            )
        }
    }
}

@Composable
private fun DonutLegendItem(color: Color, label: String, pct: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(1f),
        )
        Text(pct, style = MaterialTheme.typography.labelSmall, color = PhoniqOnSurfaceMuted)
    }
}

@Composable
private fun BudgetTrackerGrid(categories: List<CategorySpend>) {
    val colors = categories.map { categoryColor(it) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.zip(colors).chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pair.forEach { (cat, color) ->
                    CategoryBudgetCard(cat, color, Modifier.weight(1f))
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CategoryBudgetCard(cat: CategorySpend, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PhoniqSurface),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(cat.emoji, style = MaterialTheme.typography.titleMedium)
            Text(
                cat.name,
                style = MaterialTheme.typography.labelSmall,
                color = PhoniqOnSurfaceMuted,
                maxLines = 1,
            )
            Text(cat.amountLabel, style = MaterialTheme.typography.titleSmall)
            Text(cat.budgetLabel, style = MaterialTheme.typography.labelSmall, color = PhoniqOnSurfaceMuted)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { cat.fraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = color,
            )
        }
    }
}

@Composable
private fun TransactionRow(txn: RecentTransaction) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(txnBgColor(txn.categoryTag)),
            contentAlignment = Alignment.Center,
        ) {
            Text(txn.emoji, style = MaterialTheme.typography.bodyMedium)
        }
        Column(Modifier.weight(1f)) {
            Text(txn.merchant, style = MaterialTheme.typography.bodyMedium)
            Text(txn.dateLine, style = MaterialTheme.typography.labelSmall, color = PhoniqOnSurfaceMuted)
        }
        Text(
            txn.amountLabel,
            style = MaterialTheme.typography.titleSmall,
            color = if (txn.isCredit) ColorCredit else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun categoryColor(row: CategorySpend): Color =
    when (row.name) {
        "Food & Dining"     -> ColorFood
        "Shopping"          -> ColorShopping
        "Bills & Utilities" -> ColorBills
        "Transport"         -> ColorTransport
        else                -> ColorOthers
    }

private fun txnBgColor(tag: String): Color =
    when (tag) {
        "food"      -> Color(0xFF6C63FF).copy(alpha = 0.15f)
        "salary"    -> Color(0xFF00D4AA).copy(alpha = 0.15f)
        "shopping"  -> Color(0xFFF5A623).copy(alpha = 0.15f)
        "bills"     -> Color(0xFFFF6B6B).copy(alpha = 0.15f)
        "transport" -> Color(0xFF00D4AA).copy(alpha = 0.15f)
        else        -> Color(0xFF888888).copy(alpha = 0.15f)
    }
