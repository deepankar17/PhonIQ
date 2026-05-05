package com.phoniq.app.ui.money

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoniq.app.R
import com.phoniq.app.data.SampleData
import com.phoniq.app.data.model.BudgetStatus
import com.phoniq.app.data.model.CategorySpend
import com.phoniq.app.data.model.MoneySummary
import com.phoniq.app.data.model.RecentTransaction
import com.phoniq.app.ui.money.AccountBalance
import com.phoniq.app.ui.money.MonthlySpend
import com.phoniq.app.ui.components.MockupSectionLabel
import com.phoniq.app.ui.shell.ShellMenuAction
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqBorder
import com.phoniq.app.ui.theme.PhoniqBorderSoft
import com.phoniq.app.ui.theme.PhoniqBudgetBarTrack
import com.phoniq.app.ui.theme.PhoniqCategoryBarTrack
import com.phoniq.app.ui.theme.PhoniqDebit
import com.phoniq.app.ui.theme.PhoniqDonutTrack
import com.phoniq.app.ui.theme.PhoniqLegendMuted
import com.phoniq.app.ui.theme.PhoniqSecondary
import com.phoniq.app.ui.theme.PhoniqSummaryBorder
import com.phoniq.app.ui.theme.PhoniqSummaryGradientA
import com.phoniq.app.ui.theme.PhoniqSummaryGradientB
import com.phoniq.app.ui.theme.PhoniqSurface
import com.phoniq.app.ui.theme.PhoniqTextSecondaryMock
import com.phoniq.app.ui.theme.PhoniqTextSubtle
import kotlin.math.roundToInt

private val ColorFood = PhoniqAccent
private val ColorShopping = Color(0xFFF5A623)
private val ColorBills = Color(0xFFFF6B6B)
private val ColorTransport = Color(0xFF00D4AA)
private val ColorOthers = Color(0xFF888888)
private val ColorCredit = PhoniqSecondary

private val TxnTitleColor = Color(0xFFDDDDDD)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(
    onUserMessage: (String) -> Unit,
    onMoneyTool: (ShellMenuAction) -> Unit,
    realSummary: MoneySummary? = null,
    realCategories: List<CategorySpend>? = null,
    realTransactions: List<RecentTransaction>? = null,
    budgetStatuses: List<BudgetStatus> = emptyList(),
    onSetBudget: (category: String, limit: Double) -> Unit = { _, _ -> },
    accountBalances: List<AccountBalance> = emptyList(),
    monthlySpends: List<MonthlySpend> = emptyList(),
) {
    val context = LocalContext.current
    val summary = realSummary ?: SampleData.moneySummary
    val categories = realCategories?.takeIf { it.isNotEmpty() } ?: SampleData.categorySpends
    val transactions = realTransactions?.takeIf { it.isNotEmpty() } ?: SampleData.recentTransactions
    val budgetCategories = categories.filterNot { it.name == "Others" }

    var showBudgetSheet by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item { SummaryHeroCard(summary = summary, onClick = { onUserMessage(context.getString(R.string.toast_money_summary_tap)) }) }
        if (accountBalances.isNotEmpty()) {
            item { MockupSectionLabel(text = "Accounts", topPadding = 4.dp) }
            item { AccountBalanceSection(accounts = accountBalances) }
        }
        item { MockupSectionLabel(text = stringResource(R.string.money_tools_label), topPadding = 4.dp) }
        item { MoneyToolsStrip(onTool = { action ->
            if (action == ShellMenuAction.MoneyBudget) showBudgetSheet = true
            else onMoneyTool(action)
        }) }
        item { SpendingDonutRow(categories = categories, centerAmount = summary.spentLabel) }
        if (monthlySpends.any { it.totalSpent > 0 }) {
            item { MockupSectionLabel(text = "Spending trends", topPadding = 8.dp) }
            item { SpendingAnalyticsSection(monthlySpends = monthlySpends) }
        }
        item { MockupSectionLabel(text = stringResource(R.string.money_categories_title)) }
        item {
            BudgetTrackerGrid(
                categories = budgetCategories,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 8.dp),
            )
        }
        item {
            RecentTransactionsHeader(
                onSeeAll = { onUserMessage(context.getString(R.string.toast_money_summary_tap)) },
            )
        }
        items(transactions, key = { it.merchant + it.dateLine }) { txn ->
            TransactionRow(txn)
            if (txn !== transactions.last()) {
                HorizontalDivider(
                    color = PhoniqBorderSoft,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                )
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }

    if (showBudgetSheet) {
        BudgetSetupSheet(
            budgetStatuses = budgetStatuses,
            onSave = onSetBudget,
            onDismiss = { showBudgetSheet = false },
        )
    }
}

@Composable
private fun SummaryHeroCard(summary: MoneySummary, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier =
            Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clip(shape)
                .border(BorderStroke(1.dp, PhoniqSummaryBorder), shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(PhoniqSummaryGradientA, PhoniqSummaryGradientB),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                    ),
                )
                .clickable(onClick = onClick),
    ) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .size(100.dp)
                    .offset(x = 20.dp, y = (-20).dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(PhoniqAccent.copy(alpha = 0.22f), Color.Transparent),
                        ),
                    ),
        )
        Column(Modifier.padding(16.dp)) {
            Text(
                summary.monthLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = PhoniqTextSecondaryMock,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                summary.spentLabel,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = Color.White,
            )
            val budgetParts = summary.budgetCaption.split(" · ")
            val budgetDisplay =
                buildAnnotatedString {
                    append(budgetParts.getOrElse(0) { summary.budgetCaption })
                    if (budgetParts.size > 1) {
                        append(" · ")
                        withStyle(SpanStyle(color = ColorCredit)) { append(budgetParts[1]) }
                    }
                }
            Text(
                budgetDisplay,
                fontSize = 12.sp,
                color = PhoniqTextSecondaryMock,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.money_savings_label),
                    fontSize = 11.sp,
                    color = PhoniqTextSecondaryMock,
                )
                Text(
                    summary.savingsLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorCredit,
                )
            }
            BudgetGradientBar(
                progress = summary.budgetProgress,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun BudgetGradientBar(progress: Float, modifier: Modifier = Modifier) {
    val p = progress.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(PhoniqBudgetBarTrack),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(p)
                .clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(listOf(PhoniqAccent, PhoniqSecondary))),
        )
    }
}

@Composable
private fun SpendingDonutRow(categories: List<CategorySpend>, centerAmount: String) {
    val colors = categories.map { categoryColor(it) }
    val fractions = categories.zip(colors).map { (cat, color) -> cat.fraction to color }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DonutCanvas(
            fractions = fractions,
            centerAmount = centerAmount,
            modifier = Modifier.size(110.dp),
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

@Composable
private fun DonutCanvas(
    fractions: List<Pair<Float, Color>>,
    centerAmount: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokePx = (18f / 120f) * size.minDimension
            val inset = strokePx / 2f
            val arcSize = Size(size.minDimension - strokePx, size.minDimension - strokePx)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = PhoniqDonutTrack,
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
                text = stringResource(R.string.money_donut_caption),
                color = Color(0xFF666666),
                fontSize = 8.sp,
            )
        }
    }
}

@Composable
private fun DonutLegendItem(color: Color, label: String, pct: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
        )
        Text(
            label,
            fontSize = 11.sp,
            color = PhoniqLegendMuted,
            modifier = Modifier.weight(1f),
        )
        Text(
            pct,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}

@Composable
private fun BudgetTrackerGrid(categories: List<CategorySpend>, modifier: Modifier = Modifier) {
    val colors = categories.map { categoryColor(it) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.zip(categories).chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pair.forEach { (color, cat) ->
                    CategoryBudgetCard(cat, color, Modifier.weight(1f))
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CategoryBudgetCard(cat: CategorySpend, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = PhoniqSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, PhoniqBorder),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Text(cat.emoji, fontSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                cat.name,
                fontSize = 11.sp,
                color = PhoniqTextSecondaryMock,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 2.dp),
            )
            Text(
                cat.amountLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                cat.budgetLabel,
                fontSize = 10.sp,
                color = PhoniqTextSubtle,
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { cat.fraction.coerceIn(0f, 1f) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = PhoniqCategoryBarTrack,
            )
        }
    }
}

@Composable
private fun RecentTransactionsHeader(onSeeAll: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.money_txn_title),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TxnTitleColor,
        )
        Text(
            stringResource(R.string.money_txn_see_all),
            fontSize = 11.sp,
            color = PhoniqAccent,
            modifier = Modifier.clickable(onClick = onSeeAll),
        )
    }
}

@Composable
private fun TransactionRow(txn: RecentTransaction) {
    val iconShape = RoundedCornerShape(12.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(38.dp)
                    .clip(iconShape)
                    .background(txnBgColor(txn.categoryTag)),
            contentAlignment = Alignment.Center,
        ) {
            Text(txn.emoji, fontSize = 18.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(
                txn.merchant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                txn.dateLine,
                fontSize = 11.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Text(
            txn.amountLabel,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (txn.isCredit) ColorCredit else PhoniqDebit,
        )
    }
}

private fun categoryColor(row: CategorySpend): Color =
    when (row.name) {
        "Food & Dining" -> ColorFood
        "Shopping" -> ColorShopping
        "Bills & Utilities" -> ColorBills
        "Transport" -> ColorTransport
        else -> ColorOthers
    }

private fun txnBgColor(tag: String): Color =
    when (tag) {
        "food" -> Color(0xFF6C63FF).copy(alpha = 0.15f)
        "salary" -> Color(0xFF00D4AA).copy(alpha = 0.15f)
        "shopping" -> Color(0xFFF5A623).copy(alpha = 0.15f)
        "bills" -> Color(0xFFFF6B6B).copy(alpha = 0.15f)
        "transport" -> Color(0xFF00D4AA).copy(alpha = 0.15f)
        else -> Color(0xFF888888).copy(alpha = 0.15f)
    }
