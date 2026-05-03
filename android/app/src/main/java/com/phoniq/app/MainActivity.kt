package com.phoniq.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.phoniq.app.ui.messages.MessagesScreen
import com.phoniq.app.ui.money.MoneyScreen
import com.phoniq.app.ui.phone.PhoneScreen
import com.phoniq.app.ui.settings.SettingsFullScreenOverlay
import com.phoniq.app.ui.shell.GlobalSearchOverlay
import com.phoniq.app.ui.shell.PhonIQTopBar
import com.phoniq.app.ui.shell.ProtoWireOverlay
import com.phoniq.app.ui.shell.ShellMenuAction
import com.phoniq.app.ui.shell.mainTabFromRoute
import com.phoniq.app.ui.shell.wireStrings
import com.phoniq.app.ui.theme.PhonIQTheme
import kotlinx.coroutines.launch

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
    val currentRoute = currentDestination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showSearch by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var wireResources by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    fun showMessage(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                PhonIQTopBar(
                    currentTab = mainTabFromRoute(currentRoute),
                    onSearchClick = { showSearch = true },
                    onMenuAction = { action ->
                        if (action == ShellMenuAction.Settings) {
                            showSettings = true
                        } else {
                            wireResources = action.wireStrings()
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    tabs.forEach { tab ->
                        val selected =
                            currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            colors =
                                NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            icon = {
                                when (tab) {
                                    MainTab.Phone -> Icon(Icons.Default.Call, contentDescription = null)
                                    MainTab.Messages ->
                                        Icon(
                                            Icons.AutoMirrored.Filled.Message,
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
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = MainTab.Phone.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(MainTab.Phone.route) {
                    PhoneScreen(onUserMessage = { showMessage(it) })
                }
                composable(MainTab.Messages.route) {
                    MessagesScreen(onUserMessage = { showMessage(it) })
                }
                composable(MainTab.Money.route) {
                    MoneyScreen(
                        onUserMessage = { showMessage(it) },
                        onMoneyTool = { action ->
                            wireResources = action.wireStrings()
                        },
                    )
                }
            }
        }

        if (showSearch) {
            GlobalSearchOverlay(onDismiss = { showSearch = false })
        }
        if (showSettings) {
            SettingsFullScreenOverlay(onDismiss = { showSettings = false })
        }
        wireResources?.let { (titleRes, bodyRes) ->
            ProtoWireOverlay(
                titleRes = titleRes,
                bodyRes = bodyRes,
                onDismiss = { wireResources = null },
            )
        }
    }
}
