package com.phoniq.app.ui.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.phoniq.app.R
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqSecondary

@Composable
private fun headerSearchPlaceholderRes(tab: MainTabRoute): Int =
    when (tab) {
        MainTabRoute.Phone -> R.string.header_search_placeholder_phone
        MainTabRoute.Messages -> R.string.header_search_placeholder_messages
        MainTabRoute.Money -> R.string.header_search_placeholder_money
    }

@Composable
private fun tabHeaderIcon(tab: MainTabRoute): ImageVector =
    when (tab) {
        MainTabRoute.Phone -> Icons.Default.Call
        MainTabRoute.Messages -> Icons.AutoMirrored.Filled.Message
        MainTabRoute.Money -> Icons.Default.AttachMoney
    }

@Composable
fun PhonIQTopBar(
    currentTab: MainTabRoute,
    onSearchClick: () -> Unit,
    onMenuAction: (ShellMenuAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val headerBrush =
        remember {
            Brush.linearGradient(
                colors =
                    listOf(
                        PhoniqAccent,
                        PhoniqSecondary,
                    ),
            )
        }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(headerBrush),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = tabHeaderIcon(currentTab),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White,
                    )
                }
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
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Surface(
                onClick = onSearchClick,
                modifier =
                    Modifier
                        .weight(1f)
                        .height(36.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border =
                    BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    ),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(headerSearchPlaceholderRes(currentTab)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.cd_overflow_menu),
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
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
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_phone_who_is_this)) },
                                onClick = {
                                    menuOpen = false
                                    onMenuAction(ShellMenuAction.PhoneWhoIsThis)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_phone_merge)) },
                                onClick = {
                                    menuOpen = false
                                    onMenuAction(ShellMenuAction.PhoneMergeContacts)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_phone_after_call)) },
                                onClick = {
                                    menuOpen = false
                                    onMenuAction(ShellMenuAction.PhoneAfterCall)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_phone_recording)) },
                                onClick = {
                                    menuOpen = false
                                    onMenuAction(ShellMenuAction.PhoneRecording)
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
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_messages_bill_hygiene)) },
                                onClick = {
                                    menuOpen = false
                                    onMenuAction(ShellMenuAction.MessagesBillHygiene)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_messages_otp_center)) },
                                onClick = {
                                    menuOpen = false
                                    onMenuAction(ShellMenuAction.MessagesOtpCenter)
                                },
                            )
                        }
                        MainTabRoute.Money -> {
                            // Order aligned with mockup ⋮ (money panel)
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_money_export)) },
                                onClick = {
                                    menuOpen = false
                                    onMenuAction(ShellMenuAction.MoneyExport)
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
                                text = { Text(stringResource(R.string.menu_money_recurring)) },
                                onClick = {
                                    menuOpen = false
                                    onMenuAction(ShellMenuAction.MoneyRecurring)
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
                                text = { Text(stringResource(R.string.menu_money_bill_due)) },
                                onClick = {
                                    menuOpen = false
                                    onMenuAction(ShellMenuAction.MoneyBillDue)
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
            }
        }
    }
}
