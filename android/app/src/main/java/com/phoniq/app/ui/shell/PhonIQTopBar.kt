package com.phoniq.app.ui.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.phoniq.app.R
import com.phoniq.app.ui.theme.PhoniqAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhonIQTopBar(
    currentTab: MainTabRoute,
    onSearchClick: () -> Unit,
    onMenuAction: (ShellMenuAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                        append(stringResource(R.string.brand_phone_part))
                    }
                    withStyle(
                        SpanStyle(
                            color = PhoniqAccent,
                            fontWeight = FontWeight.Bold,
                        ),
                    ) {
                        append(stringResource(R.string.brand_iq_part))
                    }
                },
                style = MaterialTheme.typography.titleLarge,
            )
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.cd_search))
            }
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.cd_overflow_menu))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                when (currentTab) {
                    MainTabRoute.Phone -> {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_phone_delete_all)) },
                            onClick = {
                                menuOpen = false
                                onMenuAction(ShellMenuAction.PhoneDeleteAllCalls)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_phone_insights)) },
                            onClick = {
                                menuOpen = false
                                onMenuAction(ShellMenuAction.PhoneCommunicationInsights)
                            },
                        )
                    }
                    MainTabRoute.Messages -> {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_messages_mark_read)) },
                            onClick = {
                                menuOpen = false
                                onMenuAction(ShellMenuAction.MessagesMarkAllRead)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_messages_inbox_cleaner)) },
                            onClick = {
                                menuOpen = false
                                onMenuAction(ShellMenuAction.MessagesInboxCleaner)
                            },
                        )
                    }
                    MainTabRoute.Money -> {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_money_bill_due)) },
                            onClick = {
                                menuOpen = false
                                onMenuAction(ShellMenuAction.MoneyBillDue)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_money_recurring)) },
                            onClick = {
                                menuOpen = false
                                onMenuAction(ShellMenuAction.MoneyRecurring)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_money_salary)) },
                            onClick = {
                                menuOpen = false
                                onMenuAction(ShellMenuAction.MoneySalaryYearly)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_money_investments)) },
                            onClick = {
                                menuOpen = false
                                onMenuAction(ShellMenuAction.MoneyInvestments)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_money_export)) },
                            onClick = {
                                menuOpen = false
                                onMenuAction(ShellMenuAction.MoneyExport)
                            },
                        )
                    }
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_settings)) },
                    onClick = {
                        menuOpen = false
                        onMenuAction(ShellMenuAction.Settings)
                    },
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
    )
}
