package com.phoniq.app

import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.outlined.CurrencyRupee
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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
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
import com.phoniq.app.ui.messages.MessagesOverflowBottomSheet
import com.phoniq.app.ui.messages.MessagesOverflowSheetKind
import com.phoniq.app.ui.messages.MessagesScreen
import com.phoniq.app.ui.messages.MessagesViewModel
import com.phoniq.app.ui.money.MoneyExportBottomSheet
import com.phoniq.app.ui.money.MoneyScreen
import com.phoniq.app.ui.money.MoneyViewModel
import com.phoniq.app.ui.reminders.RemindersOverlay
import com.phoniq.app.ui.reminders.RemindersViewModel
import com.phoniq.app.ui.permission.CORE_PERMISSIONS
import com.phoniq.app.ui.permission.OPTIONAL_PERMISSIONS
import com.phoniq.app.ui.permission.PermissionBanner
import com.phoniq.app.ui.permission.PermissionScreen
import com.phoniq.app.ui.permission.allCorePermissionsGranted
import com.phoniq.app.ui.phone.AfterCallSheet
import com.phoniq.app.ui.phone.EditContactOverlay
import com.phoniq.app.ui.phone.FullScreenDialpadOverlay
import com.phoniq.app.ui.phone.InCallScreen
import com.phoniq.app.ui.phone.InCallUiPhase
import com.phoniq.app.ui.phone.IncomingCallScreen
import com.phoniq.app.ui.phone.PhoneScreen
import com.phoniq.app.ui.phone.PhoneViewModel
import com.phoniq.app.data.model.ContactPhoneEntry
import com.phoniq.app.util.PersonalizationStore
import com.phoniq.app.notification.NotificationPermissionHelper
import com.phoniq.app.notification.SmsIncomingNotifier
import com.phoniq.app.telecom.CallOverlayActivity
import com.phoniq.app.telecom.CallState
import com.phoniq.app.telecom.CallRecordingPreferences
import com.phoniq.app.telecom.CallStateRepository
import com.phoniq.app.telecom.IncomingCallNotification
import com.phoniq.app.ui.settings.SettingsFullScreenOverlay
import com.phoniq.app.data.model.MessageThreadCategory
import com.phoniq.app.data.model.DuplicateContactGroup
import com.phoniq.app.data.model.buildDuplicateContactGroups
import com.phoniq.app.ui.shell.CallRecordingLibraryOverlay
import com.phoniq.app.ui.shell.CommunicationInsightsOverlay
import com.phoniq.app.ui.shell.GlobalSearchOverlay
import com.phoniq.app.ui.shell.MergeContactsOverlay
import com.phoniq.app.ui.shell.PhonIQTopBar
import com.phoniq.app.ui.shell.ShellMenuAction
import com.phoniq.app.ui.shell.WhoIsThisOverlay
import com.phoniq.app.ui.shell.mainTabFromRoute
import com.phoniq.app.ui.theme.PhonIQTheme
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.util.LocalDatabaseExport
import com.phoniq.app.util.contactIdFromPickUri
import com.phoniq.app.util.findMessageThreadIdForNumber
import com.phoniq.app.util.openBlockedNumbersSettings
import com.phoniq.app.util.sanitizeForTelDial
import com.phoniq.app.util.placeOrDial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Snapshot used to seed the in-app contact editor (add when `deviceContactId == 0L`, else edit). */
private data class EditingContactInit(
    val deviceContactId: Long,
    val name: String,
    val phones: List<ContactPhoneEntry>,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumeLaunchIntent(intent)
        enableEdgeToEdge()
        setContent {
            val act = this@MainActivity
            val init = remember { PersonalizationStore.load(act) }
            var accentArgb by rememberSaveable { mutableStateOf(init.accentArgb) }
            var amoledBlack by rememberSaveable { mutableStateOf(init.amoledBlack) }
            var materialYou by rememberSaveable { mutableStateOf(init.materialYou) }
            var denseThreads by rememberSaveable { mutableStateOf(init.denseThreads) }
            var dialpadStyle by rememberSaveable { mutableStateOf(init.dialpadStyle) }
            var answerCallStyle by rememberSaveable { mutableStateOf(init.answerCallStyle) }
            var themePreset by rememberSaveable { mutableStateOf(init.themePreset) }
            var followSystemTheme by rememberSaveable { mutableStateOf(init.followSystemTheme) }
            var useDarkTheme by rememberSaveable { mutableStateOf(init.darkTheme) }
            var fontFamilyName by rememberSaveable { mutableStateOf(init.fontFamily) }
            var fontSizeTier by rememberSaveable { mutableStateOf(init.fontSizeTier) }
            var hapticsEnabled by rememberSaveable { mutableStateOf(init.hapticsEnabled) }
            var showInCallTimer by rememberSaveable { mutableStateOf(init.showInCallTimer) }
            var verifiedCallerBadge by rememberSaveable { mutableStateOf(init.verifiedCallerBadge) }
            var otpAutoCopy by rememberSaveable { mutableStateOf(init.otpAutoCopy) }
            var rcsUiEnabled by rememberSaveable { mutableStateOf(init.rcsUiEnabled) }
            var overBudgetAlerts by rememberSaveable { mutableStateOf(init.overBudgetAlerts) }
            var blurMoneyAmounts by rememberSaveable { mutableStateOf(init.blurMoneyAmounts) }
            var appLockEnabled by rememberSaveable { mutableStateOf(init.appLockEnabled) }
            var stealthMode by rememberSaveable { mutableStateOf(init.stealthMode) }
            var contactAvatarStyle by rememberSaveable { mutableStateOf(init.contactAvatarStyle) }

            val systemDark = isSystemInDarkTheme()
            val effectiveDark = if (followSystemTheme) systemDark else useDarkTheme
            val effectiveAmoled = !followSystemTheme && amoledBlack

            PhonIQTheme(
                darkTheme = effectiveDark,
                themePreset = themePreset,
                accentArgb = accentArgb,
                useAmoledBlack = effectiveAmoled,
                denseThreads = denseThreads,
                materialYou = materialYou,
                fontFamilyName = fontFamilyName,
                fontSizeTier = fontSizeTier,
                hapticsEnabled = hapticsEnabled,
                showInCallTimer = showInCallTimer,
                rcsBadgesEnabled = rcsUiEnabled,
                blurMoneyAmounts = blurMoneyAmounts,
                contactAvatarStyle = contactAvatarStyle,
            ) {
                PhonIQRoot(
                    onDarkThemePreference = {
                        useDarkTheme = it
                        PersonalizationStore.update(act) { s -> s.copy(darkTheme = it) }
                    },
                    onAccentArgbChanged = {
                        accentArgb = it
                        PersonalizationStore.update(act) { s -> s.copy(accentArgb = it) }
                    },
                    onAmoledBlackChanged = {
                        amoledBlack = it
                        PersonalizationStore.update(act) { s -> s.copy(amoledBlack = it) }
                    },
                    onMaterialYouChanged = { materialYou = it },
                    onDenseThreadsChanged = {
                        denseThreads = it
                        PersonalizationStore.update(act) { s -> s.copy(denseThreads = it) }
                    },
                    onDialpadStyleChanged = { dialpadStyle = it },
                    onAnswerCallStyleChanged = { answerCallStyle = it },
                    onFollowSystemThemeChanged = { followSystemTheme = it },
                    onThemePresetChanged = { themePreset = it },
                    onFontFamilyChanged = { fontFamilyName = it },
                    onFontSizeTierChanged = { fontSizeTier = it },
                    onHapticsChanged = { hapticsEnabled = it },
                    onShowInCallTimerChanged = { showInCallTimer = it },
                    onVerifiedCallerBadgeChanged = { verifiedCallerBadge = it },
                    onOtpAutoCopyChanged = { otpAutoCopy = it },
                    onRcsUiChanged = { rcsUiEnabled = it },
                    onOverBudgetAlertsChanged = { overBudgetAlerts = it },
                    onBlurMoneyAmountsChanged = { blurMoneyAmounts = it },
                    onAppLockChanged = { appLockEnabled = it },
                    onStealthModeChanged = { stealthMode = it },
                    onContactAvatarStyleChanged = {
                        contactAvatarStyle = it
                        PersonalizationStore.update(act) { s -> s.copy(contactAvatarStyle = it) }
                    },
                    dialpadStyle = dialpadStyle,
                    answerCallStyle = answerCallStyle,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeLaunchIntent(intent)
    }

    private fun consumeLaunchIntent(i: Intent?) {
        if (i == null) return

        if (i.getBooleanExtra(SmsIncomingNotifier.EXTRA_OPEN_MONEY_TAB, false)) {
            val modeStr = i.getStringExtra(SmsIncomingNotifier.EXTRA_MONEY_NOTIF_MODE).orEmpty()
            val mode =
                when (modeStr) {
                    "stats" -> MoneyNotifMode.STATS
                    "split" -> MoneyNotifMode.SPLIT
                    else -> MoneyNotifMode.DEFAULT
                }
            val splitAmt =
                if (i.hasExtra(SmsIncomingNotifier.EXTRA_TXN_SPLIT_AMOUNT)) {
                    i.getDoubleExtra(SmsIncomingNotifier.EXTRA_TXN_SPLIT_AMOUNT, 0.0).takeIf { it > 0 }
                } else {
                    null
                }
            val merch = i.getStringExtra(SmsIncomingNotifier.EXTRA_TXN_SPLIT_MERCHANT)
            PhonIQLaunchRouter.offerMoneyFromTxnNotification(
                MoneyNotifExtras(mode = mode, splitAmount = splitAmt, splitMerchant = merch),
            )
            return
        }

        i.getStringExtra(SmsIncomingNotifier.EXTRA_OPEN_THREAD_ID)?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
            val tid = if (raw.startsWith("sms_")) raw else "sms_$raw"
            PhonIQLaunchRouter.offerOpenMessageThread(tid)
            return
        }

        if (i.getBooleanExtra(IncomingCallNotification.EXTRA_SHOW_CALL, false)) {
            CallOverlayActivity.launch(this)
            return
        }

        val uri = i.data ?: return
        val action = i.action ?: return
        if (uri.scheme == "phoniq") {
            PhonIQLaunchRouter.offerMainTabRoute(uri.host.orEmpty())
            return
        }
        // As default dialer, implicit DIAL / tel VIEW is delivered to MainActivity; route in-app.
        if (uri.scheme == "tel") {
            val raw = Uri.decode(uri.schemeSpecificPart ?: "")
            val n = sanitizeForTelDial(raw)
            if (n.isNotEmpty() && n != "+") {
                when (action) {
                    Intent.ACTION_DIAL,
                    Intent.ACTION_VIEW,
                    Intent.ACTION_CALL,
                    -> {
                        PhonIQLaunchRouter.offerDialDigits(n)
                        return
                    }
                }
            }
        }
        if (action != Intent.ACTION_SENDTO && action != Intent.ACTION_VIEW) return
        if (uri.scheme != "sms" && uri.scheme != "smsto") return
        val raw = uri.schemeSpecificPart?.trim() ?: ""
        val dest = raw.substringBefore(",").substringBefore(";").trim()
        if (dest.isNotEmpty()) {
            PhonIQLaunchRouter.offerSmsComposeDestination(dest)
        } else {
            PhonIQLaunchRouter.offerBlankSmsCompose()
        }
    }
}

// ---------------------------------------------------------------------------
// Root: permission gate → shell
// ---------------------------------------------------------------------------

@Composable
private fun PhonIQRoot(
    onDarkThemePreference: (Boolean) -> Unit = {},
    onAccentArgbChanged: (Long) -> Unit = {},
    onAmoledBlackChanged: (Boolean) -> Unit = {},
    onMaterialYouChanged: (Boolean) -> Unit = {},
    onDenseThreadsChanged: (Boolean) -> Unit = {},
    onDialpadStyleChanged: (String) -> Unit = {},
    onAnswerCallStyleChanged: (String) -> Unit = {},
    onFollowSystemThemeChanged: (Boolean) -> Unit = {},
    onThemePresetChanged: (String) -> Unit = {},
    onFontFamilyChanged: (String) -> Unit = {},
    onFontSizeTierChanged: (String) -> Unit = {},
    onHapticsChanged: (Boolean) -> Unit = {},
    onShowInCallTimerChanged: (Boolean) -> Unit = {},
    onVerifiedCallerBadgeChanged: (Boolean) -> Unit = {},
    onOtpAutoCopyChanged: (Boolean) -> Unit = {},
    onRcsUiChanged: (Boolean) -> Unit = {},
    onOverBudgetAlertsChanged: (Boolean) -> Unit = {},
    onBlurMoneyAmountsChanged: (Boolean) -> Unit = {},
    onAppLockChanged: (Boolean) -> Unit = {},
    onStealthModeChanged: (Boolean) -> Unit = {},
    onContactAvatarStyleChanged: (String) -> Unit = {},
    dialpadStyle: String,
    answerCallStyle: String,
) {
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

    // Default dialer, then default SMS role (Android Q+)
    val defaultSmsLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { /* optional: snackbar if user denies */ }

    fun requestDefaultSmsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(RoleManager::class.java)
            if (rm != null && !rm.isRoleHeld(RoleManager.ROLE_SMS)) {
                val intent = rm.createRequestRoleIntent(RoleManager.ROLE_SMS)
                defaultSmsLauncher.launch(intent)
            }
        }
    }

    val defaultDialerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { requestDefaultSmsIfNeeded() }

    fun requestDefaultDialer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(RoleManager::class.java)
            if (rm != null && !rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
                val intent = rm.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                defaultDialerLauncher.launch(intent)
            } else {
                requestDefaultSmsIfNeeded()
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
                onDarkThemePreference = onDarkThemePreference,
                onAccentArgbChanged = onAccentArgbChanged,
                onAmoledBlackChanged = onAmoledBlackChanged,
                onMaterialYouChanged = onMaterialYouChanged,
                onDenseThreadsChanged = onDenseThreadsChanged,
                onDialpadStyleChanged = onDialpadStyleChanged,
                onAnswerCallStyleChanged = onAnswerCallStyleChanged,
                onFollowSystemThemeChanged = onFollowSystemThemeChanged,
                onThemePresetChanged = onThemePresetChanged,
                onFontFamilyChanged = onFontFamilyChanged,
                onFontSizeTierChanged = onFontSizeTierChanged,
                onHapticsChanged = onHapticsChanged,
                onShowInCallTimerChanged = onShowInCallTimerChanged,
                onVerifiedCallerBadgeChanged = onVerifiedCallerBadgeChanged,
                onOtpAutoCopyChanged = onOtpAutoCopyChanged,
                onRcsUiChanged = onRcsUiChanged,
                onOverBudgetAlertsChanged = onOverBudgetAlertsChanged,
                onBlurMoneyAmountsChanged = onBlurMoneyAmountsChanged,
                onAppLockChanged = onAppLockChanged,
                onStealthModeChanged = onStealthModeChanged,
                onContactAvatarStyleChanged = onContactAvatarStyleChanged,
                dialpadStyle = dialpadStyle,
                answerCallStyle = answerCallStyle,
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
    onDarkThemePreference: (Boolean) -> Unit = {},
    onAccentArgbChanged: (Long) -> Unit = {},
    onAmoledBlackChanged: (Boolean) -> Unit = {},
    onMaterialYouChanged: (Boolean) -> Unit = {},
    onDenseThreadsChanged: (Boolean) -> Unit = {},
    onDialpadStyleChanged: (String) -> Unit = {},
    onAnswerCallStyleChanged: (String) -> Unit = {},
    onFollowSystemThemeChanged: (Boolean) -> Unit = {},
    onThemePresetChanged: (String) -> Unit = {},
    onFontFamilyChanged: (String) -> Unit = {},
    onFontSizeTierChanged: (String) -> Unit = {},
    onHapticsChanged: (Boolean) -> Unit = {},
    onShowInCallTimerChanged: (Boolean) -> Unit = {},
    onVerifiedCallerBadgeChanged: (Boolean) -> Unit = {},
    onOtpAutoCopyChanged: (Boolean) -> Unit = {},
    onRcsUiChanged: (Boolean) -> Unit = {},
    onOverBudgetAlertsChanged: (Boolean) -> Unit = {},
    onBlurMoneyAmountsChanged: (Boolean) -> Unit = {},
    onAppLockChanged: (Boolean) -> Unit = {},
    onStealthModeChanged: (Boolean) -> Unit = {},
    onContactAvatarStyleChanged: (String) -> Unit = {},
    dialpadStyle: String,
    answerCallStyle: String,
) {
    val context = LocalContext.current
    val app = context.applicationContext as PhonIQApp

    // ViewModels via factories
    val messagesVm: MessagesViewModel = viewModel(factory = MessagesViewModel.Factory(app.smsRepository, app.callLogRepository, app))
    val phoneVm: PhoneViewModel =
        viewModel(
            factory =
                PhoneViewModel.Factory(
                    app.callLogRepository,
                    app.contactsRepository,
                    app.smsRepository,
                ),
        )
    val moneyVm: MoneyViewModel =
        viewModel(
            factory =
                MoneyViewModel.Factory(
                    app.transactionRepository,
                    LocalContext.current.applicationContext,
                    app.smsRepository,
                ),
        )
    val remindersVm: RemindersViewModel = viewModel(factory = RemindersViewModel.Factory())

    val lifecycleOwner = LocalLifecycleOwner.current

    // Collect live data from ViewModels — STARTED-bound so inactive tabs recompose less while backgrounded.
    val recentCalls by phoneVm.recentCalls.collectAsStateWithLifecycle(lifecycleOwner)
    val frequentQuickCalls by phoneVm.frequentQuickCalls.collectAsStateWithLifecycle(lifecycleOwner)
    val messageThreads by messagesVm.messageThreads.collectAsStateWithLifecycle(lifecycleOwner)
    val moneyRealSummary by moneyVm.derivedSummary.collectAsStateWithLifecycle(lifecycleOwner)
    val moneyRealCategories by moneyVm.derivedCategories.collectAsStateWithLifecycle(lifecycleOwner)
    val moneyRealTransactions by moneyVm.derivedRecentTransactions.collectAsStateWithLifecycle(lifecycleOwner)
    val moneyBudgetStatuses by moneyVm.budgetStatuses.collectAsStateWithLifecycle(lifecycleOwner)
    val moneyAccountBalances by moneyVm.accountBalances.collectAsStateWithLifecycle(lifecycleOwner)
    val moneyMonthlySpends by moneyVm.monthlySpends.collectAsStateWithLifecycle(lifecycleOwner)
    val moneySelectedMonth by moneyVm.selectedMonthState.collectAsStateWithLifecycle(lifecycleOwner)
    val moneyMonthBounds by moneyVm.monthPickerBounds.collectAsStateWithLifecycle(lifecycleOwner)
    val communicationInsights by phoneVm.communicationInsights.collectAsStateWithLifecycle(lifecycleOwner)
    val whoIsThisSnapshot by phoneVm.whoIsThisSnapshot.collectAsStateWithLifecycle(lifecycleOwner)
    val whoIsThisInput by phoneVm.whoIsThisInput.collectAsStateWithLifecycle(lifecycleOwner)
    val allContacts by phoneVm.allContacts.collectAsStateWithLifecycle(lifecycleOwner)
    val duplicateContactGroups = remember(allContacts) { buildDuplicateContactGroups(allContacts) }
    val billThreadsForReminders =
        remember(messageThreads) { messageThreads.filter { MessageThreadCategory.Bill in it.categories } }
    val moneyIntelligence by moneyVm.moneyIntelligence.collectAsStateWithLifecycle(lifecycleOwner)
    val moneyReminderLines by moneyVm.moneyReminderLines.collectAsStateWithLifecycle(lifecycleOwner)
    val salaryFySummary by moneyVm.salaryFySummary.collectAsStateWithLifecycle(lifecycleOwner)
    val upcomingBillHints by moneyVm.upcomingBillHints.collectAsStateWithLifecycle(lifecycleOwner)
    val investmentHighlights by moneyVm.investmentHighlightTransactions.collectAsStateWithLifecycle(lifecycleOwner)
    val reminderRows by remindersVm.reminders.collectAsStateWithLifecycle(lifecycleOwner)
    val remindBeforeDays by remindersVm.remindBeforeDays.collectAsStateWithLifecycle(lifecycleOwner)

    val navController = rememberNavController()
    val tabs = listOf(MainTab.Phone, MainTab.Messages, MainTab.Money)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var backgroundSyncRunning by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showReminders by remember { mutableStateOf(false) }
    var showWidgetsInfoDialog by remember { mutableStateOf(false) }
    var pendingOpenMessageThreadId by remember { mutableStateOf<String?>(null) }
    val callNoteSavedMessage = stringResource(R.string.snackbar_call_note_saved)
    val favoriteAddedMessage = stringResource(R.string.snackbar_favorite_starred)
    val favoriteFailedMessage = stringResource(R.string.snackbar_favorite_failed)
    val favoritePickFailedMessage = stringResource(R.string.snackbar_favorite_pick_failed)
    var pendingComposeAddress by remember { mutableStateOf<String?>(null) }
    var pendingOpenBlankComposer by remember { mutableStateOf(false) }
    var showRecordingLibrary by remember { mutableStateOf(false) }
    var showCommunicationInsights by remember { mutableStateOf(false) }
    var showAfterCallHelpDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var messagesOverflowSheet by remember { mutableStateOf<MessagesOverflowSheetKind?>(null) }
    var showFullScreenDialpad by remember { mutableStateOf(false) }
    var fullscreenDialpadInitialDigits by remember { mutableStateOf("") }
    var showContactMerge by remember { mutableStateOf(false) }
    /** When non-null, merge overlay shows only these groups (bulk-merge); null = full duplicate scan. */
    var mergeOverlayGroups by remember { mutableStateOf<List<DuplicateContactGroup>?>(null) }
    var showMoneyExportSheet by remember { mutableStateOf(false) }
    var showCloudBackupInfoDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    val recentsClearedMessage = stringResource(R.string.snackbar_recents_cleared)
    val allThreadsReadMessage = stringResource(R.string.snackbar_all_threads_read)
    val inboxDryRunMessage = stringResource(R.string.snackbar_inbox_dry_run)
    val recordingStartedMessage = stringResource(R.string.toast_incall_recording_started)
    val recordingStoppedMessage = stringResource(R.string.toast_incall_recording_stopped)

    val navBarTopLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    fun showMessage(msg: String) { scope.launch { snackbarHostState.showSnackbar(msg) } }

    val exportDbLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream"),
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val ok =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            context.contentResolver.openOutputStream(uri)?.use { out ->
                                LocalDatabaseExport.copyDatabaseToStream(app, out).getOrThrow()
                            } ?: error("openOutputStream")
                        }.isSuccess
                    }
                showMessage(
                    context.getString(
                        if (ok) {
                            R.string.snackbar_db_export_ok
                        } else {
                            R.string.snackbar_db_export_fail
                        },
                    ),
                )
            }
        }

    val restoreOpenLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            pendingRestoreUri = uri
            showRestoreConfirmDialog = true
        }

    val pickFavoriteContact =
        rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val id =
                contactIdFromPickUri(context.contentResolver, uri) ?: run {
                    showMessage(favoritePickFailedMessage)
                    return@rememberLauncherForActivityResult
                }
            phoneVm.starDeviceContact(id) { ok ->
                showMessage(if (ok) favoriteAddedMessage else favoriteFailedMessage)
            }
        }

    fun runDeviceSync() {
        if (!permissionsGranted || backgroundSyncRunning) return
        backgroundSyncRunning = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    messagesVm.refreshFromDevice()
                    phoneVm.refreshFromDevice()
                }
            } catch (_: Exception) {
                showMessage(context.getString(R.string.snackbar_device_sync_failed))
            } finally {
                backgroundSyncRunning = false
            }
        }
    }

    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) runDeviceSync()
    }

    LaunchedEffect(lifecycleOwner, permissionsGranted) {
        if (!permissionsGranted) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            phoneVm.refreshCallLogFromDevice()
        }
    }

    var editingContact by remember {
        mutableStateOf<EditingContactInit?>(null)
    }

    fun openInsertContact(displayName: String?, phoneNumber: String?) {
        val seedNumber = phoneNumber?.trim().orEmpty()
        val seedPhones =
            if (seedNumber.isEmpty()) emptyList()
            else listOf(ContactPhoneEntry(number = seedNumber, label = null))
        editingContact =
            EditingContactInit(
                deviceContactId = 0L,
                name = displayName?.trim().orEmpty(),
                phones = seedPhones,
            )
    }

    fun openEditContact(deviceContactId: Long, displayName: String, phones: List<ContactPhoneEntry>) {
        editingContact =
            EditingContactInit(
                deviceContactId = deviceContactId,
                name = displayName,
                phones = phones,
            )
    }

    fun navigateToMessagesTab() {
        navController.navigate(MainTab.Messages.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val moneyToolLedgerHint = stringResource(R.string.money_tool_ledger_hint)

    val pendingSmsNav by PhonIQLaunchRouter.pendingSmsAddress.collectAsStateWithLifecycle(lifecycleOwner)
    LaunchedEffect(pendingSmsNav) {
        val addr = pendingSmsNav ?: return@LaunchedEffect
        PhonIQLaunchRouter.consumeSmsAddress()
        pendingComposeAddress = addr
        navController.navigate(MainTab.Messages.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val pendingBlankSmsCompose by PhonIQLaunchRouter.pendingBlankSmsCompose.collectAsStateWithLifecycle(lifecycleOwner)
    LaunchedEffect(pendingBlankSmsCompose) {
        if (!pendingBlankSmsCompose) return@LaunchedEffect
        PhonIQLaunchRouter.consumeBlankSmsCompose()
        pendingOpenBlankComposer = true
        navController.navigate(MainTab.Messages.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val pendingDialDigits by PhonIQLaunchRouter.pendingDialDigits.collectAsStateWithLifecycle(lifecycleOwner)
    LaunchedEffect(pendingDialDigits) {
        val d = pendingDialDigits ?: return@LaunchedEffect
        PhonIQLaunchRouter.consumeDialDigits()
        fullscreenDialpadInitialDigits = d
        showFullScreenDialpad = true
        navController.navigate(MainTab.Phone.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val pendingTabRoute by PhonIQLaunchRouter.pendingMainTabRoute.collectAsStateWithLifecycle(lifecycleOwner)
    LaunchedEffect(pendingTabRoute, permissionsGranted) {
        val route = pendingTabRoute ?: return@LaunchedEffect
        if (!permissionsGranted) return@LaunchedEffect
        PhonIQLaunchRouter.consumeMainTabRoute()
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val pendingThreadFromNotif by PhonIQLaunchRouter.pendingOpenMessageThreadId.collectAsStateWithLifecycle(lifecycleOwner)
    LaunchedEffect(pendingThreadFromNotif, permissionsGranted) {
        val tid = pendingThreadFromNotif ?: return@LaunchedEffect
        if (!permissionsGranted) return@LaunchedEffect
        PhonIQLaunchRouter.consumeOpenMessageThreadId()
        pendingOpenMessageThreadId = tid
        navigateToMessagesTab()
    }

    var fullScreenIntentHintShown by rememberSaveable { mutableStateOf(false) }
    val fullScreenIntentHint = stringResource(R.string.snackbar_enable_full_screen_calls)
    LaunchedEffect(permissionsGranted) {
        if (!permissionsGranted || fullScreenIntentHintShown) return@LaunchedEffect
        if (!NotificationPermissionHelper.canUseFullScreenIntent(context)) {
            fullScreenIntentHintShown = true
            showMessage(fullScreenIntentHint)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column {
                    PhonIQTopBar(
                        currentTab = mainTabFromRoute(currentRoute),
                        onSearchClick = { showSearch = true },
                        onMenuAction = { action ->
                            fun goMoney() {
                                navController.navigate(MainTab.Money.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            fun goMessages() {
                                navController.navigate(MainTab.Messages.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            when (action) {
                                ShellMenuAction.Settings -> showSettings = true
                                ShellMenuAction.PhoneDeleteAllCalls -> showDeleteAllDialog = true
                                ShellMenuAction.MessagesMarkAllRead -> {
                                    messagesVm.markAllRead()
                                    messagesOverflowSheet = MessagesOverflowSheetKind.MarkAllRead
                                }
                                ShellMenuAction.MessagesInboxCleaner ->
                                    messagesOverflowSheet = MessagesOverflowSheetKind.InboxCleaner
                                ShellMenuAction.PhoneCommunicationInsights -> {
                                    showCommunicationInsights = true
                                }
                                ShellMenuAction.PhoneWhoIsThis -> phoneVm.openWhoIsThis(null)
                                ShellMenuAction.PhoneMergeContacts -> {
                                    mergeOverlayGroups = null
                                    showContactMerge = true
                                }
                                ShellMenuAction.PhoneAfterCall -> showAfterCallHelpDialog = true
                                ShellMenuAction.PhoneRecording -> showRecordingLibrary = true
                                ShellMenuAction.MessagesBillHygiene -> {
                                    messagesOverflowSheet = MessagesOverflowSheetKind.BillHygiene
                                }
                                ShellMenuAction.MessagesOtpCenter -> {
                                    goMessages()
                                }
                                ShellMenuAction.MessagesReminders -> {
                                    goMessages()
                                    showReminders = true
                                }
                                ShellMenuAction.MoneyExport -> showMoneyExportSheet = true
                                ShellMenuAction.MoneyBudget -> goMoney()
                                ShellMenuAction.MoneyBillDue,
                                ShellMenuAction.MoneyRecurring,
                                ShellMenuAction.MoneySalaryYearly,
                                ShellMenuAction.MoneyInvestments,
                                -> {
                                    goMoney()
                                    if (!moneyIntelligence.hasAny) {
                                        showMessage(moneyToolLedgerHint)
                                    }
                                }
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
                                color = navBarTopLineColor,
                                strokeWidth = stroke,
                                start = Offset(0f, stroke * 0.5f),
                                end = Offset(size.width, stroke * 0.5f),
                            )
                        },
                    containerColor = MaterialTheme.colorScheme.surface,
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
                                    MainTab.Phone -> Icon(Icons.Default.Call, contentDescription = stringResource(R.string.cd_nav_phone))
                                    MainTab.Messages ->
                                        Icon(Icons.AutoMirrored.Filled.Message, contentDescription = stringResource(R.string.cd_nav_messages))
                                    MainTab.Money ->
                                        Icon(Icons.Outlined.CurrencyRupee, contentDescription = stringResource(R.string.cd_nav_money))
                                }
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                            selected = selected,
                            onClick = {
                                if (tab == MainTab.Messages || tab == MainTab.Money) {
                                    showFullScreenDialpad = false
                                }
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
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                composable(MainTab.Phone.route) {
                    PhoneScreen(
                        recents = recentCalls,
                        frequentQuickCalls = frequentQuickCalls,
                        phoneViewModel = phoneVm,
                        onUserMessage = { showMessage(it) },
                        onAddContact = { name, number -> openInsertContact(name, number) },
                        onEditContact = { id, name, phones -> openEditContact(id, name, phones) },
                        onPickFavoriteContact = { pickFavoriteContact.launch(null) },
                        onOpenDialpadFullScreen = {
                            fullscreenDialpadInitialDigits = ""
                            showFullScreenDialpad = true
                        },
                        onBulkMergeContacts = { deviceIds ->
                            val filtered =
                                duplicateContactGroups.filter { g ->
                                    g.contacts.any { c -> c.deviceContactId in deviceIds }
                                }
                            if (filtered.isEmpty()) {
                                showMessage(context.getString(R.string.bulk_merge_none))
                            } else {
                                mergeOverlayGroups = filtered
                                showContactMerge = true
                            }
                        },
                    )
                }
                composable(MainTab.Messages.route) {
                    MessagesScreen(
                        threads = messageThreads,
                        messagesViewModel = messagesVm,
                        pendingOpenThreadId = pendingOpenMessageThreadId,
                        onConsumePendingOpenThread = { pendingOpenMessageThreadId = null },
                        pendingComposeAddress = pendingComposeAddress,
                        onConsumePendingCompose = { pendingComposeAddress = null },
                        pendingOpenBlankComposer = pendingOpenBlankComposer,
                        onConsumePendingOpenBlankComposer = { pendingOpenBlankComposer = false },
                        onNavigateToMoney = {
                            navController.navigate(MainTab.Money.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onThreadAction = { showMessage(it) },
                    )
                }
                composable(MainTab.Money.route) {
                    MoneyScreen(
                        onUserMessage = { showMessage(it) },
                        onMoneyTool = { action ->
                            when (action) {
                                ShellMenuAction.MoneyExport -> showMoneyExportSheet = true
                                ShellMenuAction.MoneyBillDue,
                                ShellMenuAction.MoneyRecurring,
                                ShellMenuAction.MoneySalaryYearly,
                                ShellMenuAction.MoneyInvestments,
                                -> {
                                    if (!moneyIntelligence.hasAny) {
                                        showMessage(moneyToolLedgerHint)
                                    }
                                }
                                else -> Unit
                            }
                        },
                        selectedMonth = moneySelectedMonth,
                        monthPickerBounds = moneyMonthBounds,
                        onMonthYearChange = { y, m -> moneyVm.selectMonth(y, m) },
                        realSummary = moneyRealSummary,
                        realCategories = moneyRealCategories,
                        realTransactions = moneyRealTransactions,
                        budgetStatuses = moneyBudgetStatuses,
                        onSetBudget = { cat, limit -> moneyVm.setBudget(cat, limit) },
                        accountBalances = moneyAccountBalances,
                        monthlySpends = moneyMonthlySpends,
                        moneyIntelligence = moneyIntelligence,
                        moneyReminderLines = moneyReminderLines,
                        salaryFySummary = salaryFySummary,
                        upcomingBillHints = upcomingBillHints,
                        investmentHighlights = investmentHighlights,
                    )
                }
            }
        }

        // In-call overlay — slides up over everything when a call is active/ringing
        val activeCall by CallStateRepository.callInfo.collectAsStateWithLifecycle(lifecycleOwner)
        val isCallRecording by CallStateRepository.isCallRecording.collectAsStateWithLifecycle(lifecycleOwner)

        // Track last call for the after-call sheet
        var lastCall by remember { mutableStateOf<com.phoniq.app.telecom.ActiveCallInfo?>(null) }
        var callDurationSecs by remember { mutableStateOf(0) }
        var showAfterCallSheet by remember { mutableStateOf(false) }
        var previousActiveCall by remember { mutableStateOf<com.phoniq.app.telecom.ActiveCallInfo?>(null) }

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
                    phoneVm.refreshCallLogFromDevice()
                }
                else -> Unit
            }
        }

        // [hangUp]/[reject] clear callInfo without a DISCONNECTED frame; still resync call log.
        LaunchedEffect(activeCall) {
            val prev = previousActiveCall
            if (prev != null && activeCall == null) {
                phoneVm.scheduleCallLogResyncAfterCall()
            }
            previousActiveCall = activeCall
        }

        AnimatedVisibility(
            visible = activeCall != null && activeCall!!.state != CallState.DISCONNECTED,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            activeCall?.let { info ->
                when (info.state) {
                    CallState.RINGING ->
                        IncomingCallScreen(
                            callerName = info.callerName,
                            callerNumber = info.callerNumber,
                            deviceContactId = info.deviceContactId,
                            dialpadStyle = dialpadStyle,
                            answerCallStyle = answerCallStyle,
                            onAnswer = { CallStateRepository.answer() },
                            onDecline = { CallStateRepository.reject() },
                            onUserMessage = { showMessage(it) },
                        )
                    CallState.DIALING ->
                        InCallScreen(
                            callerName = info.callerName,
                            callerNumber = info.callerNumber,
                            deviceContactId = info.deviceContactId,
                            phase = InCallUiPhase.Dialing,
                            dialpadStyle = dialpadStyle,
                            answerCallStyle = answerCallStyle,
                            isCallRecordingActive = false,
                            canControlCallRecording = false,
                            onToggleCallRecording = {},
                            onHangUp = { CallStateRepository.hangUp() },
                            onUserMessage = { showMessage(it) },
                        )
                    else ->
                InCallScreen(
                    callerName = info.callerName,
                    callerNumber = info.callerNumber,
                    deviceContactId = info.deviceContactId,
                    phase = InCallUiPhase.Active,
                            dialpadStyle = dialpadStyle,
                            answerCallStyle = answerCallStyle,
                            spamRiskLabel = null,
                            isCallRecordingActive = isCallRecording,
                            canControlCallRecording = true,
                            onToggleCallRecording = {
                                if (!CallRecordingPreferences.isEnabled(context)) {
                                    showMessage(context.getString(R.string.toast_incall_record_pref_off))
                                } else if (isCallRecording) {
                                    CallStateRepository.requestStopCallRecording()
                                    showMessage(recordingStoppedMessage)
                                } else {
                                    CallStateRepository.requestStartCallRecording()
                                    showMessage(recordingStartedMessage)
                                }
                            },
                    onHangUp = { CallStateRepository.hangUp() },
                            onUserMessage = { showMessage(it) },
                        )
                }
            }
        }

        // After-call sheet
        val lastRecordingPath by CallStateRepository.lastRecordingPath.collectAsStateWithLifecycle(lifecycleOwner)
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
                onUserMessage = { showMessage(it) },
                onAddContact = {
                    showAfterCallSheet = false
                    openInsertContact(lastCall!!.callerName, lastCall!!.callerNumber)
                },
                onSaveCallNote = { note ->
                    phoneVm.saveCallNote(lastCall!!.callerNumber, note)
                    showMessage(callNoteSavedMessage)
                },
                onFavorite = {
                    val num = lastCall!!.callerNumber
                    phoneVm.starContactForPhoneNumber(num) { ok ->
                        showMessage(
                            context.getString(
                                if (ok) {
                                    R.string.snackbar_favorite_starred
                                } else {
                                    R.string.after_call_favorite_no_match
                                },
                            ),
                        )
                    }
                },
                onWhoIsThis = {
                    showAfterCallSheet = false
                    phoneVm.openWhoIsThis(lastCall!!.callerNumber)
                },
                onBlock = {
                    if (context.openBlockedNumbersSettings()) {
                        showMessage(context.getString(R.string.after_call_blocked_numbers_screen))
                    } else {
                        showMessage(context.getString(R.string.after_call_blocked_numbers_unavailable))
                    }
                },
            )
        }

        editingContact?.let { init ->
            EditContactOverlay(
                initialDeviceContactId = init.deviceContactId,
                initialName = init.name,
                initialPhones = init.phones,
                phoneViewModel = phoneVm,
                onDismiss = { editingContact = null },
                onSaved = { _ ->
                    editingContact = null
                    phoneVm.enqueueContactsRefresh()
                },
                onUserMessage = { showMessage(it) },
            )
        }

        if (showFullScreenDialpad) {
            FullScreenDialpadOverlay(
                initialDigits = fullscreenDialpadInitialDigits,
                contacts = allContacts,
                recentCalls = recentCalls,
                dialpadStyle = dialpadStyle,
                onDismiss = {
                    showFullScreenDialpad = false
                    fullscreenDialpadInitialDigits = ""
                },
                onAddContact = { digits ->
                    showFullScreenDialpad = false
                    fullscreenDialpadInitialDigits = ""
                    openInsertContact(null, digits)
                },
            )
        }

        if (showSearch) {
            GlobalSearchOverlay(
                onDismiss = { showSearch = false },
                recentCalls = recentCalls,
                messageThreads = messageThreads,
                moneyTransactions = moneyRealTransactions,
                onDialNumber = { num ->
                    showSearch = false
                    if (!context.placeOrDial(num, null)) {
                        showMessage(context.getString(R.string.toast_dial_failed))
                    }
                },
                onOpenMessageThread = { threadId ->
                    pendingOpenMessageThreadId = threadId
                    showSearch = false
                    if (currentRoute != MainTab.Messages.route) {
                        navController.navigate(MainTab.Messages.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onGoToMoney = {
                    showSearch = false
                    navController.navigate(MainTab.Money.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
        if (showReminders) {
            RemindersOverlay(
                reminders = reminderRows,
                remindBeforeDays = remindBeforeDays,
                onRemindBeforeDaysChange = { remindersVm.setRemindBeforeDays(it) },
                onDismiss = { showReminders = false },
                onAdd = { title, dueAt -> remindersVm.addReminder(title, dueAt) },
                onSetDone = { id, done -> remindersVm.setDone(id, done) },
                onSnooze = { id, add -> remindersVm.snooze(id, add) },
                onImportBillSms = { remindersVm.importFromBillThreads(billThreadsForReminders) },
                onOpenThread = { threadId ->
                    pendingOpenMessageThreadId = threadId
                    showReminders = false
                    if (currentRoute != MainTab.Messages.route) {
                        navController.navigate(MainTab.Messages.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onUserMessage = { showMessage(it) },
            )
        }

        if (showSettings) {
            SettingsFullScreenOverlay(
                onDismiss = { showSettings = false },
                onDarkThemePreference = onDarkThemePreference,
                onAccentArgbChanged = onAccentArgbChanged,
                onAmoledBlackChanged = onAmoledBlackChanged,
                onMaterialYouChanged = onMaterialYouChanged,
                onDenseThreadsChanged = onDenseThreadsChanged,
                onDialpadStyleChanged = onDialpadStyleChanged,
                onAnswerCallStyleChanged = onAnswerCallStyleChanged,
                onFollowSystemThemeChanged = onFollowSystemThemeChanged,
                onThemePresetChanged = onThemePresetChanged,
                onFontFamilyChanged = onFontFamilyChanged,
                onFontSizeTierChanged = onFontSizeTierChanged,
                onHapticsChanged = onHapticsChanged,
                onShowInCallTimerChanged = onShowInCallTimerChanged,
                onVerifiedCallerBadgeChanged = onVerifiedCallerBadgeChanged,
                onOtpAutoCopyChanged = onOtpAutoCopyChanged,
                onRcsUiChanged = onRcsUiChanged,
                onOverBudgetAlertsChanged = onOverBudgetAlertsChanged,
                onBlurMoneyAmountsChanged = onBlurMoneyAmountsChanged,
                onAppLockChanged = onAppLockChanged,
                onStealthModeChanged = onStealthModeChanged,
                onContactAvatarStyleChanged = onContactAvatarStyleChanged,
                onExportLocalDatabase = {
                    exportDbLauncher.launch(
                        "phoniq-backup-${System.currentTimeMillis()}.db",
                    )
                },
                onRestoreLocalDatabase = {
                    restoreOpenLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                },
                onInformCloudBackup = { showCloudBackupInfoDialog = true },
                onWidgetsInfo = { showWidgetsInfoDialog = true },
            )
        }

        if (showRecordingLibrary) {
            CallRecordingLibraryOverlay(
                onDismiss = { showRecordingLibrary = false },
                onUserMessage = { showMessage(it) },
            )
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
                billThreads =
                    if (sheetKind == MessagesOverflowSheetKind.BillHygiene) {
                        messageThreads.filter { MessageThreadCategory.Bill in it.categories }
                    } else {
                        emptyList()
                    },
                onOpenBillThread = { threadId ->
                    pendingOpenMessageThreadId = threadId
                    navigateToMessagesTab()
                },
                onBillFocusFilter = {
                    navigateToMessagesTab()
                },
            )
        }

        if (showMoneyExportSheet) {
            MoneyExportBottomSheet(
                onDismissRequest = { showMoneyExportSheet = false },
                onExportCsv = {
                    moneyVm.exportTransactionsCsv { showMessage(it) }
                },
                onExportPdf = {
                    moneyVm.exportTransactionsPdf { showMessage(it) }
                },
            )
        }

        if (showWidgetsInfoDialog) {
            AlertDialog(
                onDismissRequest = { showWidgetsInfoDialog = false },
                title = { Text(stringResource(R.string.dialog_widgets_title)) },
                text = { Text(stringResource(R.string.dialog_widgets_body)) },
                confirmButton = {
                    TextButton(onClick = { showWidgetsInfoDialog = false }) {
                        Text(stringResource(R.string.action_got_it))
                    }
                },
            )
        }

        if (showCloudBackupInfoDialog) {
            AlertDialog(
                onDismissRequest = { showCloudBackupInfoDialog = false },
                title = { Text(stringResource(R.string.dialog_cloud_backup_title)) },
                text = { Text(stringResource(R.string.dialog_cloud_backup_body)) },
                confirmButton = {
                    TextButton(onClick = { showCloudBackupInfoDialog = false }) {
                        Text(stringResource(R.string.action_got_it))
                    }
                },
            )
        }

        val restoreUriSnapshot = pendingRestoreUri
        if (showRestoreConfirmDialog && restoreUriSnapshot != null) {
            AlertDialog(
                onDismissRequest = {
                    showRestoreConfirmDialog = false
                    pendingRestoreUri = null
                },
                title = { Text(stringResource(R.string.restore_db_title)) },
                text = { Text(stringResource(R.string.restore_db_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showRestoreConfirmDialog = false
                            val u = restoreUriSnapshot
                            pendingRestoreUri = null
                            scope.launch {
                                val ok =
                                    withContext(Dispatchers.IO) {
                                        LocalDatabaseExport.restoreDatabaseFromUri(app, context.contentResolver, u)
                                            .isSuccess
                                    }
                                if (ok) {
                                    Process.killProcess(Process.myPid())
                                } else {
                                    showMessage(context.getString(R.string.restore_db_fail))
                                }
                            }
                        },
                    ) {
                        Text(stringResource(R.string.restore_db_confirm))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showRestoreConfirmDialog = false
                            pendingRestoreUri = null
                        },
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
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

        if (showCommunicationInsights) {
            CommunicationInsightsOverlay(
                insights = communicationInsights,
                onDismiss = { showCommunicationInsights = false },
            )
        }

        whoIsThisSnapshot?.let { snap ->
            WhoIsThisOverlay(
                snapshot = snap,
                numberField = whoIsThisInput,
                onNumberChange = { phoneVm.setWhoIsThisInput(it) },
                onDismiss = { phoneVm.dismissWhoIsThis() },
                onOpenMessageThread = {
                    val tid = findMessageThreadIdForNumber(messageThreads, whoIsThisInput)
                    if (tid != null) {
                        phoneVm.dismissWhoIsThis()
                        pendingOpenMessageThreadId = tid
                        if (currentRoute != MainTab.Messages.route) {
                            navController.navigate(MainTab.Messages.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    } else {
                        showMessage(context.getString(R.string.after_call_who_no_thread))
                    }
                },
            )
        }

        if (showContactMerge) {
            MergeContactsOverlay(
                groups = mergeOverlayGroups ?: duplicateContactGroups,
                onDismiss = {
                    showContactMerge = false
                    mergeOverlayGroups = null
                },
                onUserMessage = { showMessage(it) },
            )
        }

        if (showAfterCallHelpDialog) {
            AlertDialog(
                onDismissRequest = { showAfterCallHelpDialog = false },
                title = { Text(stringResource(R.string.after_call_help_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.after_call_help_body))
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                showAfterCallHelpDialog = false
                                if (context.openBlockedNumbersSettings()) {
                                    showMessage(context.getString(R.string.after_call_blocked_numbers_screen))
                                } else {
                                    showMessage(context.getString(R.string.after_call_blocked_numbers_unavailable))
                                }
                            },
                        ) { Text(stringResource(R.string.after_call_help_blocked)) }
                        TextButton(
                            onClick = {
                                showAfterCallHelpDialog = false
                                navigateToMessagesTab()
                            },
                        ) { Text(stringResource(R.string.after_call_help_messages)) }
                        TextButton(
                            onClick = {
                                showAfterCallHelpDialog = false
                                openInsertContact(null, null)
                            },
                        ) { Text(stringResource(R.string.after_call_help_new_contact)) }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAfterCallHelpDialog = false }) {
                        Text(stringResource(R.string.action_close))
                    }
                },
            )
        }
    }
}
