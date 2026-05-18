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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phoniq.app.MoneyNotifMode
import com.phoniq.app.PhonIQLaunchRouter
import com.phoniq.app.R
import com.phoniq.app.data.model.BudgetStatus
import com.phoniq.app.data.model.CategorySpend
import com.phoniq.app.data.model.MoneyIntelligenceSummary
import com.phoniq.app.data.model.MoneySummary
import com.phoniq.app.data.model.RecentTransaction
import com.phoniq.app.ui.money.AccountBalance
import com.phoniq.app.ui.money.MonthlySpend
import com.phoniq.app.ui.components.MockupSectionLabel
import com.phoniq.app.ui.shell.ShellMenuAction
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.LocalBlurMoneyAmounts
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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val ColorFood = PhoniqAccent
private val ColorShopping = Color(0xFFF5A623)
private val ColorBills = Color(0xFFFF6B6B)
private val ColorTransport = Color(0xFF00D4AA)
private val ColorOthers = Color(0xFF888888)
private val ColorCredit = PhoniqSecondary

private val TxnTitleColor = Color(0xFFDDDDDD)

private fun fallbackMonthLabel(ym: YearMonth): String =
    ym.format(DateTimeFormatter.ofPattern("MMMM yyyy · 'Total Spent'", Locale.getDefault()))

private fun monthsInYear(year: Int, bounds: MonthPickerBounds): List<Int> {
    if (year < bounds.earliest.year || year > bounds.latest.year) return emptyList()
    val start = if (year == bounds.earliest.year) bounds.earliest.monthValue else 1
    val end = if (year == bounds.latest.year) bounds.latest.monthValue else 12
    return (start..end).toList()
}

/** Replaces ASCII digits with bullet when privacy blur is enabled (settings). */
private fun privacyMaskMoneyDigits(text: String, blur: Boolean): String {
    if (!blur) return text
    return buildString {
        for (c in text) {
            append(if (c.isDigit()) '•' else c)
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(
    onUserMessage: (String) -> Unit,
    onMoneyTool: (ShellMenuAction) -> Unit,
    selectedMonth: YearMonth,
    monthPickerBounds: MonthPickerBounds,
    onMonthYearChange: (year: Int, month: Int) -> Unit,
    realSummary: MoneySummary? = null,
    realCategories: List<CategorySpend>? = null,
    realTransactions: List<RecentTransaction>? = null,
    budgetStatuses: List<BudgetStatus> = emptyList(),
    onSetBudget: (category: String, limit: Double) -> Unit = { _, _ -> },
    accountBalances: List<AccountBalance> = emptyList(),
    monthlySpends: List<MonthlySpend> = emptyList(),
    moneyIntelligence: MoneyIntelligenceSummary = MoneyIntelligenceSummary.Empty,
    moneyReminderLines: List<MoneyReminderLine> = emptyList(),
    salaryFySummary: SalaryFySummary? = null,
    upcomingBillHints: List<UpcomingBillHint> = emptyList(),
    investmentHighlights: List<RecentTransaction> = emptyList(),
) {
    val context = LocalContext.current
    val emptyHint = stringResource(R.string.money_empty_hint)
    val blurPrivacy = LocalBlurMoneyAmounts.current
    val summary =
        realSummary
            ?: MoneySummary(
                monthLabel = fallbackMonthLabel(selectedMonth),
                spentLabel = "₹0",
                savingsLabel = "+₹0",
                currencyHint = "INR",
                budgetProgress = 0f,
                budgetCaption = emptyHint,
            )
    val categories = realCategories.orEmpty()
    val transactions = realTransactions.orEmpty()
    val budgetCategories = categories.filterNot { it.name == "Others" }

    var showBudgetSheet by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val pendingMoneyNotif by PhonIQLaunchRouter.pendingMoneyNotif.collectAsStateWithLifecycle(lifecycleOwner)
    var splitDialog by remember { mutableStateOf<Pair<Double, String>?>(null) }
    val inrFormat = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")) }

    LaunchedEffect(pendingMoneyNotif) {
        val n = pendingMoneyNotif ?: return@LaunchedEffect
        val isPlainDefault =
            n.mode == MoneyNotifMode.DEFAULT &&
                n.splitAmount == null &&
                n.splitMerchant.isNullOrBlank()
        if (isPlainDefault) {
            PhonIQLaunchRouter.consumeMoneyNotifExtras()
            return@LaunchedEffect
        }
        PhonIQLaunchRouter.consumeMoneyNotifExtras()
        when (n.mode) {
            MoneyNotifMode.STATS -> onUserMessage(context.getString(R.string.money_notif_stats_message))
            MoneyNotifMode.SPLIT -> {
                val amt = n.splitAmount
                if (amt != null && amt > 0) {
                    splitDialog = amt to (n.splitMerchant.orEmpty())
                } else {
                    onUserMessage(context.getString(R.string.money_notif_split_generic))
                }
            }
            MoneyNotifMode.DEFAULT -> Unit
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
        item {
            MonthYearSelectorRow(
                selected = selectedMonth,
                bounds = monthPickerBounds,
                onChange = { ym -> onMonthYearChange(ym.year, ym.monthValue) },
            )
        }
        if (accountBalances.isNotEmpty()) {
            item {
                AccountBalanceSection(
                    accounts = accountBalances,
                    sectionTitle = stringResource(R.string.money_passbook_section),
                )
            }
        }
        item {
            SummaryHeroCard(
                summary = summary,
                blurPrivacy = blurPrivacy,
                onClick = { onUserMessage(context.getString(R.string.toast_money_summary_tap)) },
            )
        }
        item { MockupSectionLabel(text = stringResource(R.string.money_tools_label), topPadding = 4.dp) }
        item { MoneyToolsStrip(onTool = { action ->
            if (action == ShellMenuAction.MoneyBudget) showBudgetSheet = true
            else onMoneyTool(action)
        }) }
        item {
            MoneyIntelligenceSection(
                summary = moneyIntelligence,
                onUserMessage = onUserMessage,
            )
        }
        if (investmentHighlights.isNotEmpty()) {
            item {
                MockupSectionLabel(
                    text = stringResource(R.string.money_invest_sms_section),
                    topPadding = 8.dp,
                )
            }
            item {
                Column(
                    modifier =
                        Modifier
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    investmentHighlights.forEach { txn ->
                        TransactionRow(txn = txn, blurPrivacy = blurPrivacy)
                    }
                }
            }
        }
        if (moneyReminderLines.isNotEmpty()) {
            item { UpcomingMoneyRemindersSection(lines = moneyReminderLines) }
        }
        if (salaryFySummary != null) {
            item { SalaryFySummaryCard(summary = salaryFySummary) }
        }
        if (upcomingBillHints.isNotEmpty()) {
            item { UpcomingBillsCard(hints = upcomingBillHints) }
        }
        if (categories.isNotEmpty()) {
            item {
                SpendingDonutRow(
                    categories = categories,
                    centerAmount = privacyMaskMoneyDigits(summary.spentLabel, blurPrivacy),
                )
            }
        } else {
            item {
                Text(
                    text = stringResource(R.string.money_no_spending_breakdown),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        if (monthlySpends.any { it.totalSpent > 0 }) {
            item { MockupSectionLabel(text = "Spending trends", topPadding = 8.dp) }
            item { SpendingAnalyticsSection(monthlySpends = monthlySpends) }
        }
        item { MockupSectionLabel(text = stringResource(R.string.money_categories_title)) }
        if (budgetCategories.isNotEmpty()) {
            item {
                BudgetTrackerGrid(
                    categories = budgetCategories,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 8.dp),
                )
            }
        }
        item {
            RecentTransactionsHeader(
                onSeeAll = { onUserMessage(context.getString(R.string.toast_money_summary_tap)) },
            )
        }
        if (transactions.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.money_no_transactions),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        } else {
            items(transactions, key = { it.merchant + it.dateLine }) { txn ->
                TransactionRow(txn, blurPrivacy)
                if (txn !== transactions.last()) {
                    HorizontalDivider(
                        color = PhoniqBorderSoft,
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
        }

        splitDialog?.let { (amt, merch) ->
            AlertDialog(
                onDismissRequest = { splitDialog = null },
                title = { Text(stringResource(R.string.money_split_dialog_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.money_split_dialog_body,
                            inrFormat.format(amt),
                            merch.ifBlank { "—" },
                        ),
                    )
                },
                confirmButton = {
                    TextButton(onClick = { splitDialog = null }) {
                        Text(stringResource(R.string.action_got_it))
                    }
                },
            )
        }
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
private fun UpcomingMoneyRemindersSection(lines: List<MoneyReminderLine>) {
    MockupSectionLabel(text = stringResource(R.string.money_upcoming_reminders_title), topPadding = 12.dp)
    Column(
        modifier =
            Modifier
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        lines.forEach { line ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PhoniqSurface,
                border = BorderStroke(1.dp, PhoniqBorderSoft),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        line.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = PhoniqAccent,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        line.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = PhoniqTextSubtle,
                    )
                }
            }
        }
    }
}

@Composable
private fun SalaryFySummaryCard(summary: SalaryFySummary) {
    MockupSectionLabel(text = stringResource(R.string.money_salary_fy_title), topPadding = 12.dp)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PhoniqSurface,
        modifier =
            Modifier
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .fillMaxWidth(),
        border = BorderStroke(1.dp, PhoniqBorderSoft),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                summary.fyLabel,
                style = MaterialTheme.typography.titleSmall,
                color = PhoniqAccent,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.money_salary_fy_body, summary.totalRupee, summary.creditCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UpcomingBillsCard(hints: List<UpcomingBillHint>) {
    MockupSectionLabel(text = stringResource(R.string.money_upcoming_bills_title), topPadding = 12.dp)
    Column(
        modifier =
            Modifier
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        hints.forEach { h ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PhoniqSurface,
                border = BorderStroke(1.dp, PhoniqBorderSoft),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(h.title, style = MaterialTheme.typography.titleSmall, color = PhoniqAccent, fontWeight = FontWeight.SemiBold)
                    Text(h.hint, style = MaterialTheme.typography.bodySmall, color = PhoniqTextSubtle)
                }
            }
        }
    }
}

@Composable
private fun MoneyIntelligenceSection(
    summary: MoneyIntelligenceSummary,
    onUserMessage: (String) -> Unit,
) {
    MockupSectionLabel(text = stringResource(R.string.money_intel_section), topPadding = 12.dp)
    if (!summary.hasAny) {
        Text(
            text = stringResource(R.string.money_intel_empty),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = PhoniqTextSubtle,
        )
    } else {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (summary.recurringMerchantCount > 0) {
                val title = stringResource(R.string.money_intel_recurring)
                val body = stringResource(R.string.money_intel_recurring_body, summary.recurringMerchantCount)
                MoneyIntelCard(
                    title = title,
                    body = body,
                    onClick = { onUserMessage("$title: $body") },
                )
            }
            if (summary.salaryCreditCount > 0) {
                val title = stringResource(R.string.money_intel_salary)
                val body = stringResource(R.string.money_intel_salary_body, summary.salaryCreditCount)
                MoneyIntelCard(
                    title = title,
                    body = body,
                    onClick = { onUserMessage("$title: $body") },
                )
            }
            if (summary.investmentTxnCount > 0) {
                val title = stringResource(R.string.money_intel_invest)
                val body = stringResource(R.string.money_intel_invest_body, summary.investmentTxnCount)
                MoneyIntelCard(
                    title = title,
                    body = body,
                    onClick = { onUserMessage("$title: $body") },
                )
            }
        }
    }
}

@Composable
private fun MoneyIntelCard(
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PhoniqSurface,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        border = BorderStroke(1.dp, PhoniqBorderSoft),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = PhoniqAccent, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MonthYearSelectorRow(
    selected: YearMonth,
    bounds: MonthPickerBounds,
    onChange: (YearMonth) -> Unit,
) {
    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM", Locale.getDefault()) }
    val years = remember(bounds) { (bounds.earliest.year..bounds.latest.year).toList().reversed() }
    val months = remember(selected.year, bounds) { monthsInYear(selected.year, bounds) }

    if (showMonthPicker) {
        AlertDialog(
            onDismissRequest = { showMonthPicker = false },
            title = { Text(stringResource(R.string.money_period_month)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(months, key = { it }) { m ->
                        val label = YearMonth.of(selected.year, m).format(monthFormatter)
                        Text(
                            text = label,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onChange(YearMonth.of(selected.year, m))
                                        showMonthPicker = false
                                    }
                                    .padding(vertical = 14.dp, horizontal = 4.dp),
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMonthPicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showYearPicker) {
        AlertDialog(
            onDismissRequest = { showYearPicker = false },
            title = { Text(stringResource(R.string.money_period_year)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(years, key = { it }) { year ->
                        Text(
                            text = year.toString(),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val validMonths = monthsInYear(year, bounds)
                                        val newMonth =
                                           if (validMonths.isNotEmpty() && selected.monthValue in validMonths) {
                                                selected.monthValue
                                            } else {
                                                validMonths.last()
                                            }
                                        onChange(YearMonth.of(year, newMonth))
                                        showYearPicker = false
                                    }
                                    .padding(vertical = 14.dp, horizontal = 4.dp),
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showYearPicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            onClick = { showMonthPicker = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.weight(1f),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    stringResource(R.string.money_period_month),
                    fontSize = 11.sp,
                    color = PhoniqTextSubtle,
                )
                Text(
                    selected.format(monthFormatter),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Surface(
            onClick = { showYearPicker = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.weight(1f),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    stringResource(R.string.money_period_year),
                    fontSize = 11.sp,
                    color = PhoniqTextSubtle,
                )
                Text(
                    selected.year.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun SummaryHeroCard(summary: MoneySummary, blurPrivacy: Boolean, onClick: () -> Unit) {
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
                privacyMaskMoneyDigits(summary.spentLabel, blurPrivacy),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = Color.White,
            )
            val budgetParts = privacyMaskMoneyDigits(summary.budgetCaption, blurPrivacy).split(" · ")
            val budgetDisplay =
                buildAnnotatedString {
                    append(budgetParts.getOrElse(0) { privacyMaskMoneyDigits(summary.budgetCaption, blurPrivacy) })
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
                    privacyMaskMoneyDigits(summary.savingsLabel, blurPrivacy),
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
private fun TransactionRow(txn: RecentTransaction, blurPrivacy: Boolean) {
    TransactionSmsRowContent(
        title = txn.merchant,
        subtitle = txn.dateLine,
        amountLabel = privacyMaskMoneyDigits(txn.amountLabel, blurPrivacy),
        isCredit = txn.isCredit,
        emoji = txn.emoji,
        categoryTag = txn.categoryTag,
    )
}

private fun categoryColor(row: CategorySpend): Color =
    when (row.name) {
        "Food & Dining" -> ColorFood
        "Shopping" -> ColorShopping
        "Bills & Utilities" -> ColorBills
        "Transport" -> ColorTransport
        else -> ColorOthers
    }

