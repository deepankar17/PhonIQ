package com.phoniq.app.ui.money

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoniq.app.R
import com.phoniq.app.ui.shell.ShellMenuAction
import com.phoniq.app.ui.theme.PhoniqBorder
import com.phoniq.app.ui.theme.PhoniqSurface
import com.phoniq.app.ui.theme.PhoniqTextSecondaryMock

private data class MoneyToolChip(
    val action: ShellMenuAction,
    val titleRes: Int,
    val subRes: Int,
)

private val moneyToolChips =
    listOf(
        MoneyToolChip(ShellMenuAction.MoneyBudget, R.string.money_tool_budget_title, R.string.money_tool_budget_sub),
        MoneyToolChip(ShellMenuAction.MoneyBillDue, R.string.money_tool_bill_title, R.string.money_tool_bill_sub),
        MoneyToolChip(ShellMenuAction.MoneyRecurring, R.string.money_tool_recurring_title, R.string.money_tool_recurring_sub),
        MoneyToolChip(ShellMenuAction.MoneySalaryYearly, R.string.money_tool_salary_title, R.string.money_tool_salary_sub),
        MoneyToolChip(ShellMenuAction.MoneyInvestments, R.string.money_tool_invest_title, R.string.money_tool_invest_sub),
        MoneyToolChip(ShellMenuAction.MoneyExport, R.string.money_tool_export_title, R.string.money_tool_export_sub),
    )

@Composable
fun MoneyToolsStrip(onTool: (ShellMenuAction) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(start = 12.dp, top = 2.dp, end = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(moneyToolChips, key = { it.action.name }) { chip ->
            Card(
                onClick = { onTool(chip.action) },
                modifier = Modifier.widthIn(min = 118.dp, max = 150.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PhoniqSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, PhoniqBorder),
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        stringResource(chip.titleRes),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stringResource(chip.subRes),
                        fontSize = 10.sp,
                        lineHeight = 13.5.sp,
                        color = PhoniqTextSecondaryMock,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
