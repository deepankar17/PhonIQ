package com.phoniq.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.phoniq.app.ui.theme.PhonIQTheme
import com.phoniq.app.ui.screens.MessagesPlaceholderScreen
import com.phoniq.app.ui.screens.MoneyPlaceholderScreen
import com.phoniq.app.ui.screens.PhonePlaceholderScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhonIQTheme {
                PhonIQShell()
            }
        }
    }
}

private sealed class MainTab(val route: String, val labelRes: Int) {
    data object Phone : MainTab("phone", R.string.nav_phone)
    data object Messages : MainTab("messages", R.string.nav_messages)
    data object Money : MainTab("money", R.string.nav_money)
}

@Composable
private fun PhonIQShell() {
    val navController = rememberNavController()
    val tabs =
        listOf(
            MainTab.Phone,
            MainTab.Messages,
            MainTab.Money,
        )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    val selected =
                        currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        icon = {
                            when (tab) {
                                MainTab.Phone -> Icon(Icons.Default.Call, contentDescription = null)
                                MainTab.Messages ->
                                    Icon(
                                        Icons.Default.Message,
                                        contentDescription = null,
                                    )
                                MainTab.Money ->
                                    Icon(
                                        Icons.Default.AttachMoney,
                                        contentDescription = null,
                                    )
                            }
                        },
                        label = { Text(stringResource(tab.labelRes)) },
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainTab.Phone.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(MainTab.Phone.route) { PhonePlaceholderScreen() }
            composable(MainTab.Messages.route) { MessagesPlaceholderScreen() }
            composable(MainTab.Money.route) { MoneyPlaceholderScreen() }
        }
    }
}
