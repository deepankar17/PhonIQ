package com.phoniq.app.ui.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.phoniq.app.R
import com.phoniq.app.ui.shell.ShellMenuAction

private data class MoneyToolChip(
    val action: ShellMenuAction,
    val titleRes: Int,
    val subRes: Int,
)

private val moneyToolChips =
    listOf(
        MoneyToolChip(ShellMenuAction.MoneyBillDue, R.string.money_tool_bill_title, R.string.money_tool_bill_sub),
        MoneyToolChip(ShellMenuAction.MoneyRecurring, R.string.money_tool_recurring_title, R.string.money_tool_recurring_sub),
        MoneyToolChip(ShellMenuAction.MoneySalaryYearly, R.string.money_tool_salary_title, R.string.money_tool_salary_sub),
        MoneyToolChip(ShellMenuAction.MoneyInvestments, R.string.money_tool_invest_title, R.string.money_tool_invest_sub),
        MoneyToolChip(ShellMenuAction.MoneyExport, R.string.money_tool_export_title, R.string.money_tool_export_sub),
    )

@Composable
fun MoneyToolsStrip(onTool: (ShellMenuAction) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Text(
            text = stringResource(R.string.money_tools_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(moneyToolChips, key = { it.action.name }) { chip ->
                Card(
                    onClick = { onTool(chip.action) },
                    modifier = Modifier.width(132.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(chip.titleRes), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(chip.subRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
    }
}
