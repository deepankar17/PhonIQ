package com.phoniq.app.ui.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoniq.app.data.model.BudgetStatus
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqOnSurfaceMuted

private val BUDGET_CATEGORIES = listOf(
    "FOOD" to ("🍽️" to "Food & Dining"),
    "SHOPPING" to ("🛍️" to "Shopping"),
    "BILLS" to ("📄" to "Bills & Utilities"),
    "TRANSPORT" to ("🚗" to "Transport"),
    "EMI" to ("🏦" to "EMI / Loan"),
    "HEALTH" to ("💊" to "Health"),
    "ENTERTAINMENT" to ("🎬" to "Entertainment"),
    "INVESTMENT" to ("📈" to "Investments"),
    "OTHER" to ("📦" to "Others"),
)

/**
 * Bottom sheet that lets the user set monthly budget limits per spending category.
 * Pre-fills from existing [budgetStatuses]; calls [onSave] with (category, limitRupees).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSetupSheet(
    budgetStatuses: List<BudgetStatus>,
    onSave: (category: String, limitRupees: Double) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    // Initialise from existing budgets
    val limits = remember {
        mutableStateMapOf<String, String>().also { map ->
            BUDGET_CATEGORIES.forEach { (key, _) ->
                val existing = budgetStatuses.find { it.category == key }
                map[key] = if ((existing?.limit ?: 0.0) > 0.0) existing!!.limit.toInt().toString() else ""
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Monthly Budgets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Set ₹ limits per category (leave blank for no limit)",
                        style = MaterialTheme.typography.bodySmall,
                        color = PhoniqOnSurfaceMuted,
                    )
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }

            HorizontalDivider()

            // Category rows
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(BUDGET_CATEGORIES, key = { it.first }) { (key, meta) ->
                    val (emoji, displayName) = meta
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(emoji, fontSize = 22.sp, modifier = Modifier.padding(end = 2.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            val spent = budgetStatuses.find { it.category == key }?.spent ?: 0.0
                            if (spent > 0.0) {
                                Text(
                                    "Spent ₹${spent.toInt()} this month",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PhoniqOnSurfaceMuted,
                                )
                            }
                        }
                        OutlinedTextField(
                            value = limits[key] ?: "",
                            onValueChange = { limits[key] = it.filter { c -> c.isDigit() } },
                            prefix = { Text("₹") },
                            placeholder = { Text("Limit") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.7f),
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                }
            }

            // Save button
            Button(
                onClick = {
                    limits.forEach { (cat, raw) ->
                        val rupees = raw.toDoubleOrNull() ?: 0.0
                        onSave(cat, rupees)
                    }
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Text("Save budgets")
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
