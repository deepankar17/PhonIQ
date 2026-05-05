package com.phoniq.app

import android.app.role.RoleManager
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.phoniq.app.data.SampleData
import com.phoniq.app.data.mapper.toMessageThreads
import com.phoniq.app.data.mapper.toRecentCall
import com.phoniq.app.data.model.MessageThread
import com.phoniq.app.data.model.RecentCall
import com.phoniq.app.ui.messages.MessagesOverflowBottomSheet
import com.phoniq.app.ui.messages.MessagesOverflowSheetKind
import com.phoniq.app.ui.messages.MessagesScreen
import com.phoniq.app.ui.messages.MessagesViewModel
import com.phoniq.app.ui.money.MoneyScreen
import com.phoniq.app.ui.money.MoneyViewModel
import com.phoniq.app.ui.permission.CORE_PERMISSIONS
import com.phoniq.app.ui.permission.OPTIONAL_PERMISSIONS
import com.phoniq.app.ui.permission.PermissionBanner
import com.phoniq.app.ui.permission.PermissionScreen
import com.phoniq.app.ui.permission.allCorePermissionsGranted
import com.phoniq.app.ui.phone.AfterCallSheet
import com.phoniq.app.ui.phone.InCallScreen
import com.phoniq.app.ui.phone.PhoneScreen
import com.phoniq.app.ui.phone.PhoneViewModel
import com.phoniq.app.telecom.CallState
import com.phoniq.app.telecom.CallStateRepository
import com.phoniq.app.ui.settings.SettingsFullScreenOverlay
import com.phoniq.app.ui.shell.GlobalSearchOverlay
import com.phoniq.app.ui.shell.PhonIQTopBar
import com.phoniq.app.ui.shell.ProtoWireOverlay
import com.phoniq.app.ui.shell.ShellMenuAction
import com.phoniq.app.ui.shell.mainTabFromRoute
import com.phoniq.app.ui.theme.PhonIQTheme
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqBorder
import com.phoniq.app.ui.theme.PhoniqSurfaceLow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhonIQTheme {
                PhonIQRoot()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Root: permission gate → shell
// ---------------------------------------------------------------------------

@Composable
private fun PhonIQRoot() {
    val context = LocalContext.current
    var permissionsGranted by rememberSaveable { mutableStateOf(allCorePermissionsGranted(context)) }
    var permissionsSkipped by rememberSaveable { mutableStateOf(false) }

    val allPerms = CORE_PERMISSIONS + OPTIONAL_PERMISSIONS
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val coreGranted = CORE_PERMISSIONS.all { results[it] == true }
        if (coreGranted) {
            permissionsGranted = true
            permissionsSkipped = false
        }
    }

    // Default dialer / phone role request
    val defaultDialerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* result handled silently — TelecomManager check on next compose */ }

    fun requestDefaultDialer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(RoleManager::class.java)
            if (rm != null && !rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
                val intent = rm.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                defaultDialerLauncher.launch(intent)
            }
        } else {
            val intent = android.content.Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
            defaultDialerLauncher.launch(intent)
        }
    }

    // After permissions granted, also prompt for default dialer role
    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) requestDefaultDialer()
    }

    when {
        permissionsGranted || permissionsSkipped -> {
            PhonIQShell(
                permissionsGranted = permissionsGranted,
                onRequestPermissions = { permLauncher.launch(allPerms) },
            )
        }
        else -> {
            PermissionScreen(
                onPermissionsResult = { granted ->
                    if (granted) permissionsGranted = true else permissionsSkipped = true
                },
                onSkip = { permissionsSkipped = true },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Tab routing
// ---------------------------------------------------------------------------

private sealed class MainTab(val route: String, val labelRes: Int) {
    data object Phone : MainTab("phone", R.string.nav_phone)
    data object Messages : MainTab("messages", R.string.nav_messages)
    data object Money : MainTab("money", R.string.nav_money)
}

// ---------------------------------------------------------------------------
// Main shell
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhonIQShell(
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as PhonIQApp

    // ViewModels via factories
    val messagesVm: MessagesViewModel = viewModel(factory = MessagesViewModel.Factory(app.smsRepository))
    val phoneVm: PhoneViewModel = viewModel(factory = PhoneViewModel.Factory(app.callLogRepository, app.contactsRepository))
    val moneyVm: MoneyViewModel = viewModel(factory = MoneyViewModel.Factory(app.transactionRepository, LocalContext.current.applicationContext))

    // Trigger device sync once when permissions become available
    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) {
            messagesVm.syncFromDevice()
            phoneVm.syncFromDevice()
        }
    }

    // Collect live data from ViewModels
    val dbSmsMessages by messagesVm.allMessages.collectAsState()
    val dbCalls by phoneVm.allCalls.collectAsState()
    val moneyRealSummary by moneyVm.derivedSummary.collectAsState()
    val moneyRealCategories by moneyVm.derivedCategories.collectAsState()
    val moneyRealTransactions by moneyVm.derivedRecentTransactions.collectAsState()
    val moneyBudgetStatuses by moneyVm.budgetStatuses.collectAsState()
    val moneyAccountBalances by moneyVm.accountBalances.collectAsState()
    val moneyMonthlySpends by moneyVm.monthlySpends.collectAsState()

    // Merge: prefer real data when the Room DB has entries, fall back to SampleData
    val messageThreads: List<MessageThread> = remember(dbSmsMessages) {
        dbSmsMessages.toMessageThreads().ifEmpty { SampleData.messageThreads }
    }
    val recentCalls: List<RecentCall> = remember(dbCalls) {
        dbCalls.map { it.toRecentCall() }.ifEmpty { SampleData.recentCalls }
    }

    val navController = rememberNavController()
    val tabs = listOf(MainTab.Phone, MainTab.Messages, MainTab.Money)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showSearch by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var wireAction by remember { mutableStateOf<ShellMenuAction?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var messagesOverflowSheet by remember { mutableStateOf<MessagesOverflowSheetKind?>(null) }

    val recentsClearedMessage = stringResource(R.string.snackbar_recents_cleared)
    val allThreadsReadMessage = stringResource(R.string.snackbar_all_threads_read)
    val inboxDryRunMessage = stringResource(R.string.snackbar_inbox_dry_run)
    val composeNewMessageToast = stringResource(R.string.toast_compose_new_message)

    fun showMessage(msg: String) { scope.launch { snackbarHostState.showSnackbar(msg) } }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Column {
                    PhonIQTopBar(
                        currentTab = mainTabFromRoute(currentRoute),
                        onSearchClick = { showSearch = true },
                        onMenuAction = { action ->
                            when (action) {
                                ShellMenuAction.Settings -> showSettings = true
                                ShellMenuAction.PhoneDeleteAllCalls -> showDeleteAllDialog = true
                                ShellMenuAction.MessagesMarkAllRead -> {
                                    messagesVm.markAllRead()
                                    messagesOverflowSheet = MessagesOverflowSheetKind.MarkAllRead
                                }
                                ShellMenuAction.MessagesInboxCleaner ->
                                    messagesOverflowSheet = MessagesOverflowSheetKind.InboxCleaner
                                else -> wireAction = action
                            }
                        },
                    )
                    if (!permissionsGranted) {
                        PermissionBanner(onGrant = onRequestPermissions)
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    modifier =
                        Modifier.drawBehind {
                            val stroke = 1.dp.toPx()
                            drawLine(
                                color = PhoniqBorder.copy(alpha = 0.55f),
                                strokeWidth = stroke,
                                start = Offset(0f, stroke * 0.5f),
                                end = Offset(size.width, stroke * 0.5f),
                            )
                        },
                    containerColor = PhoniqSurfaceLow.copy(alpha = 0.92f),
                    tonalElevation = NavigationBarDefaults.Elevation,
                ) {
                    tabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = PhoniqAccent.copy(alpha = 0.14f),
                                selectedIconColor = PhoniqAccent,
                                selectedTextColor = PhoniqAccent,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            icon = {
                                when (tab) {
                                    MainTab.Phone -> Icon(Icons.Default.Call, contentDescription = null)
                                    MainTab.Messages -> Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null)
                                    MainTab.Money -> Icon(Icons.Default.AttachMoney, contentDescription = null)
                                }
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
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
                    PhoneScreen(
                        recents = recentCalls,
                        onUserMessage = { showMessage(it) },
                    )
                }
                composable(MainTab.Messages.route) {
                    MessagesScreen(
                        threads = messageThreads,
                        onComposeClick = { showMessage(composeNewMessageToast) },
                        onThreadAction = { showMessage(it) },
                    )
                }
                composable(MainTab.Money.route) {
                    MoneyScreen(
                        onUserMessage = { showMessage(it) },
                        onMoneyTool = { action -> wireAction = action },
                        realSummary = moneyRealSummary,
                        realCategories = moneyRealCategories.takeIf { it.isNotEmpty() },
                        realTransactions = moneyRealTransactions.takeIf { it.isNotEmpty() },
                        budgetStatuses = moneyBudgetStatuses,
                        onSetBudget = { cat, limit -> moneyVm.setBudget(cat, limit) },
                        accountBalances = moneyAccountBalances,
                        monthlySpends = moneyMonthlySpends,
                    )
                }
            }
        }

        // In-call overlay — slides up over everything when a call is active/ringing
        val activeCall by CallStateRepository.callInfo.collectAsState()

        // Track last call for the after-call sheet
        var lastCall by remember { mutableStateOf<com.phoniq.app.telecom.ActiveCallInfo?>(null) }
        var callDurationSecs by remember { mutableStateOf(0) }
        var showAfterCallSheet by remember { mutableStateOf(false) }

        // Observe call state transitions
        LaunchedEffect(activeCall?.state) {
            when (activeCall?.state) {
                CallState.ACTIVE -> {
                    // Timer while call is active
                    callDurationSecs = 0
                    while (CallStateRepository.callInfo.value?.state == CallState.ACTIVE) {
                        kotlinx.coroutines.delay(1_000)
                        callDurationSecs++
                    }
                }
                CallState.DISCONNECTED -> {
                    lastCall = activeCall
                    showAfterCallSheet = true
                }
                else -> Unit
            }
        }

        AnimatedVisibility(
            visible = activeCall != null && activeCall!!.state != CallState.DISCONNECTED,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            activeCall?.let { info ->
                InCallScreen(
                    callerName = info.callerName,
                    callerNumber = info.callerNumber,
                    onHangUp = { CallStateRepository.hangUp() },
                    onUserMessage = { showMessage(it) },
                )
            }
        }

        // After-call sheet
        val lastRecordingPath by CallStateRepository.lastRecordingPath.collectAsState()
        if (showAfterCallSheet && lastCall != null) {
            val mins = callDurationSecs / 60
            val secs = callDurationSecs % 60
            val dur = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
            AfterCallSheet(
                callerName = lastCall!!.callerName,
                callerNumber = lastCall!!.callerNumber,
                durationLabel = dur,
                recordingPath = lastRecordingPath,
                onDismiss = { showAfterCallSheet = false },
            )
        }

        if (showSearch) GlobalSearchOverlay(onDismiss = { showSearch = false })
        if (showSettings) SettingsFullScreenOverlay(onDismiss = { showSettings = false })

        wireAction?.let { action ->
            ProtoWireOverlay(action = action, onDismiss = { wireAction = null })
        }

        messagesOverflowSheet?.let { sheetKind ->
            MessagesOverflowBottomSheet(
                kind = sheetKind,
                onDismissRequest = { messagesOverflowSheet = null },
                onMarkAllReadConfirm = {
                    messagesVm.markAllRead()
                    showMessage(allThreadsReadMessage)
                },
                onInboxDryRun = { showMessage(inboxDryRunMessage) },
            )
        }

        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                title = { Text(stringResource(R.string.dialog_delete_all_title)) },
                text = { Text(stringResource(R.string.dialog_delete_all_body)) },
                confirmButton = {
                    TextButton(onClick = {
                        phoneVm.clearCallLog()
                        showDeleteAllDialog = false
                        showMessage(recentsClearedMessage)
                    }) { Text(stringResource(R.string.action_delete_all)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }
    }
}
