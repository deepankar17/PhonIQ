package com.phoniq.app.ui.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phoniq.app.util.PersonalizationStore
import com.phoniq.app.util.ThemeUiBindings
import com.phoniq.app.util.contactIdsWithPhoto
import com.phoniq.app.R
import com.phoniq.app.telecom.CallRecordingPreferences
import com.phoniq.app.ui.phone.PersonalizationDialpadStylePreview
import com.phoniq.app.ui.phone.AnswerCallStylePreview
import com.phoniq.app.ui.components.ContactPhotoAvatar
import com.phoniq.app.ui.theme.LocalContactAvatarStyle
import com.phoniq.app.ui.theme.LocalThemePreset
import com.phoniq.app.ui.theme.fontFamilyForPersonalization
import com.phoniq.app.ui.theme.fontTierMultiplier
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class SettingsPane { Root, Personalization, DataDevice }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsFullScreenOverlay(
    onDismiss: () -> Unit,
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
    onExportLocalDatabase: () -> Unit = {},
    onRestoreLocalDatabase: () -> Unit = {},
    onInformCloudBackup: () -> Unit = {},
    onWidgetsInfo: () -> Unit = {},
) {
    var pane by remember { mutableStateOf(SettingsPane.Root) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val scheme = MaterialTheme.colorScheme
        Surface(modifier = Modifier.fillMaxSize(), color = scheme.background) {
            val barColors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.surfaceContainerLow,
                    titleContentColor = scheme.onSurface,
                    navigationIconContentColor = scheme.onSurface,
                    actionIconContentColor = scheme.onSurface,
                )
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = scheme.background,
                topBar = {
                    when (pane) {
                        SettingsPane.Root ->
                            TopAppBar(
                                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                                colors = barColors,
                                actions = {
                                    IconButton(onClick = onDismiss) {
                                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close))
                                    }
                                },
                            )
                        SettingsPane.Personalization ->
                            TopAppBar(
                                title = {
                                    Text(
                                        stringResource(R.string.settings_personalization_title),
                                        fontWeight = FontWeight.Bold,
                                    )
                                },
                                colors = barColors,
                                navigationIcon = {
                                    IconButton(onClick = { pane = SettingsPane.Root }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            stringResource(R.string.cd_back),
                                        )
                                    }
                                },
                            )
                        SettingsPane.DataDevice ->
                            TopAppBar(
                                title = {
                                    Text(
                                        stringResource(R.string.settings_data_device_title),
                                        fontWeight = FontWeight.Bold,
                                    )
                                },
                                colors = barColors,
                                navigationIcon = {
                                    IconButton(onClick = { pane = SettingsPane.Root }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            stringResource(R.string.cd_back),
                                        )
                                    }
                                },
                            )
                    }
                },
            ) { padding ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
                ) {
                    when (pane) {
                        SettingsPane.Root ->
                            RootBody(
                                onPersonalization = { pane = SettingsPane.Personalization },
                                onDataDevice = { pane = SettingsPane.DataDevice },
                                onWidgetsInfo = onWidgetsInfo,
                            )
                        SettingsPane.Personalization ->
                            PersonalizationBody(
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
                            )
                        SettingsPane.DataDevice ->
                            DataDeviceBody(
                                onExportLocalDatabase = onExportLocalDatabase,
                                onRestoreLocalDatabase = onRestoreLocalDatabase,
                                onInformCloudBackup = onInformCloudBackup,
                            )
                    }
                }
            }
        }
    }
}

@Composable
private fun RootBody(
    onPersonalization: () -> Unit,
    onDataDevice: () -> Unit,
    onWidgetsInfo: () -> Unit,
) {
    SettingsSectionLabel(stringResource(R.string.settings_group_general), topPaddingExtra = 4.dp)
    SettingsCard {
        SettingsNavRow(
            label = stringResource(R.string.settings_row_personalization),
            sub = stringResource(R.string.settings_row_personalization_sub),
            icon = Icons.Default.Palette,
            iconBrush =
                Brush.linearGradient(
                    colors =
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                        ),
                ),
            onClick = onPersonalization,
        )
    }
    SettingsSectionLabel(stringResource(R.string.settings_group_data))
    SettingsCard {
        SettingsNavRow(
            label = stringResource(R.string.settings_row_data_device),
            sub = stringResource(R.string.settings_row_data_device_sub),
            icon = Icons.Default.Folder,
            iconBrush =
                Brush.linearGradient(
                    colors = listOf(Color(0xFF607D8B), Color(0xFF455A64)),
                ),
            onClick = onDataDevice,
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            thickness = 1.dp,
        )
        SettingsNavRow(
            label = stringResource(R.string.settings_row_widgets),
            sub = stringResource(R.string.settings_row_widgets_sub),
            icon = Icons.Default.Apps,
            iconBrush =
                Brush.linearGradient(
                    colors = listOf(Color(0xFF95A5A6), Color(0xFF7F8C8D)),
                ),
            onClick = onWidgetsInfo,
        )
    }
}

@Composable
private fun PersonalizationBody(
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
) {
    val context = LocalContext.current
    val initial = remember { PersonalizationStore.load(context) }
    val accentArgbList =
        remember {
            listOf(
                0xFF6C63FFL,
                0xFF00D4AAL,
                0xFF2196F3L,
                0xFFE91E63L,
                0xFFFF9800L,
                0xFF4CAF50L,
                0xFFF44336L,
                0xFF9C27B0L,
                0xFF00BCD4L,
                0xFFFFEB3BL,
            )
        }
    val accentColors = remember { accentArgbList.map { Color(it) } }

    var selectedTheme by remember { mutableStateOf(initial.themePreset) }
    var selectedAccent by remember {
        mutableIntStateOf(
            accentArgbList.indexOf(initial.accentArgb).let { if (it < 0) 0 else it },
        )
    }

    var selectedDialpad by remember { mutableStateOf(initial.dialpadStyle) }
    val dialpadStyles = listOf("Classic", "Rounded", "Minimal", "iOS-like", "Material 3")

    var selectedAnswer by remember { mutableStateOf(initial.answerCallStyle) }
    val answerStyles =
        listOf(
            PersonalizationStore.ANSWER_STYLE_CLASSIC,
            PersonalizationStore.ANSWER_STYLE_GLASS,
            PersonalizationStore.ANSWER_STYLE_SAMSUNG_LIQUID,
        )

    var selectedFont by remember { mutableStateOf(initial.fontFamily) }
    val fonts = listOf("Roboto", "Nunito", "Mono", "Serif", "System")

    var selectedSize by remember { mutableStateOf(initial.fontSizeTier) }
    val fontSizes = listOf("Small", "Normal", "Large", "XL")

    var amoledDark by remember { mutableStateOf(initial.amoledBlack) }
    var autoDayNight by remember { mutableStateOf(initial.followSystemTheme) }
    var materialYou by remember { mutableStateOf(initial.materialYou) }
    var haptics by remember { mutableStateOf(initial.hapticsEnabled) }

    var showCallTimer by remember { mutableStateOf(initial.showInCallTimer) }
    var callRecordingEnabled by remember { mutableStateOf(CallRecordingPreferences.isEnabled(context)) }
    var verifiedBadge by remember { mutableStateOf(initial.verifiedCallerBadge) }

    var otpAutoCopy by remember { mutableStateOf(initial.otpAutoCopy) }
    var rcsEnabled by remember { mutableStateOf(initial.rcsUiEnabled) }
    var denseThreads by remember { mutableStateOf(initial.denseThreads) }

    var overBudgetAlert by remember { mutableStateOf(initial.overBudgetAlerts) }

    var appLock by remember { mutableStateOf(initial.appLockEnabled) }
    var blurAmounts by remember { mutableStateOf(initial.blurMoneyAmounts) }
    var stealthMode by remember { mutableStateOf(initial.stealthMode) }

    val callUiBundled = ThemeUiBindings.themeUsesDedicatedCallPack(selectedTheme)
    val answerChipSelection =
        if (callUiBundled) {
            ThemeUiBindings.defaultAnswerCallStyleForTheme(selectedTheme)
        } else {
            selectedAnswer
        }

    Column(Modifier.padding(bottom = 32.dp)) {
        SettingsSectionLabel(stringResource(R.string.perso_theme_section), topPaddingExtra = 4.dp)
        SettingsCard {
            ThemeTilesRow(
                selected = selectedTheme,
                onSelect = { name ->
                    selectedTheme = name
                    autoDayNight = false
                    val dark = name != PersonalizationStore.THEME_LIGHT
                    val amoled = name == PersonalizationStore.THEME_AMOLED
                    amoledDark = amoled
                    selectedDialpad = ThemeUiBindings.defaultDialpadStorageForTheme(name)
                    selectedAnswer = ThemeUiBindings.defaultAnswerCallStyleForTheme(name)
                    PersonalizationStore.update(context) { prev ->
                        ThemeUiBindings.applyBundledDefaultsForTheme(prev, name).copy(
                            followSystemTheme = false,
                            darkTheme = dark,
                            amoledBlack = amoled,
                        )
                    }
                    onFollowSystemThemeChanged(false)
                    onThemePresetChanged(name)
                    onDialpadStyleChanged(selectedDialpad)
                    onAnswerCallStyleChanged(selectedAnswer)
                    onDarkThemePreference(dark)
                    onAmoledBlackChanged(amoled)
                },
            )
        }

        SettingsSectionLabel(stringResource(R.string.perso_dialpad_section))
        SettingsCard {
            if (callUiBundled) {
                Text(
                    stringResource(R.string.perso_call_style_locked_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
            ChipRow(
                options = dialpadStyles,
                selected = selectedDialpad,
                enabled = !callUiBundled,
                onSelect = {
                    selectedDialpad = it
                    PersonalizationStore.update(context) { s -> s.copy(dialpadStyle = it) }
                    onDialpadStyleChanged(it)
                },
            )
            CompositionLocalProvider(LocalThemePreset provides selectedTheme) {
                PersonalizationDialpadStylePreview(dialpadStyle = selectedDialpad)
            }
        }

        SettingsSectionLabel(stringResource(R.string.perso_answer_style_section))
        SettingsCard {
            if (callUiBundled) {
                Text(
                    stringResource(R.string.perso_call_style_locked_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
            ChipRow(
                options = answerStyles,
                selected = answerChipSelection,
                enabled = !callUiBundled,
                onSelect = {
                    selectedAnswer = it
                    PersonalizationStore.update(context) { s -> s.copy(answerCallStyle = it) }
                    onAnswerCallStyleChanged(it)
                },
            )
            AnswerCallStylePreview(answerCallStyle = answerChipSelection)
        }

        SettingsSectionLabel(stringResource(R.string.perso_contact_avatar_section))
        SettingsCard {
            Text(
                stringResource(R.string.perso_contact_avatar_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
            var selectedAvatarStyle by remember { mutableStateOf(initial.contactAvatarStyle) }
            val avatarStyles =
                listOf(
                    PersonalizationStore.CONTACT_AVATAR_ROUND,
                    PersonalizationStore.CONTACT_AVATAR_SQUARE,
                    PersonalizationStore.CONTACT_AVATAR_SQUIRCLE,
                    PersonalizationStore.CONTACT_AVATAR_STAR,
                    PersonalizationStore.CONTACT_AVATAR_TEARDROP,
                    PersonalizationStore.CONTACT_AVATAR_EXTRA,
                )
            ChipRow(
                options = avatarStyles,
                selected = selectedAvatarStyle,
                onSelect = {
                    selectedAvatarStyle = it
                    PersonalizationStore.update(context) { s -> s.copy(contactAvatarStyle = it) }
                    onContactAvatarStyleChanged(it)
                },
            )
            var photoPreviewIds by remember { mutableStateOf<List<Long>>(emptyList()) }
            LaunchedEffect(Unit) {
                photoPreviewIds = withContext(Dispatchers.IO) { context.contactIdsWithPhoto(5) }
            }
            CompositionLocalProvider(LocalContactAvatarStyle provides selectedAvatarStyle) {
                if (photoPreviewIds.isEmpty()) {
                    Text(
                        stringResource(R.string.perso_contact_avatar_preview_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                } else {
                    val gPrev0 = MaterialTheme.colorScheme.primary
                    val gPrev1 = MaterialTheme.colorScheme.secondary
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        photoPreviewIds.forEach { cid ->
                            ContactPhotoAvatar(
                                deviceContactId = cid,
                                initials = "•",
                                gradientStart = gPrev0,
                                gradientEnd = gPrev1,
                                size = 48.dp,
                                fontSize = 18.sp,
                            )
                        }
                    }
                }
            }
        }

        SettingsSectionLabel(stringResource(R.string.perso_accent_section))
        SettingsCard {
            AccentSwatchRow(
                colors = accentColors,
                selected = selectedAccent,
                onSelect = { idx ->
                    selectedAccent = idx
                    val argb = accentArgbList[idx]
                    PersonalizationStore.update(context) { it.copy(accentArgb = argb) }
                    onAccentArgbChanged(argb)
                },
            )
        }

        SettingsSectionLabel(stringResource(R.string.perso_font_family_section))
        SettingsCard {
            FontFamilyChipRow(
                options = fonts,
                selected = selectedFont,
                onSelect = {
                    selectedFont = it
                    PersonalizationStore.update(context) { s -> s.copy(fontFamily = it) }
                    onFontFamilyChanged(it)
                },
            )
        }

        SettingsSectionLabel(stringResource(R.string.perso_font_size_section))
        SettingsCard {
            FontSizeTierChipRow(
                options = fontSizes,
                selected = selectedSize,
                selectedFontKey = selectedFont,
                onSelect = {
                    selectedSize = it
                    PersonalizationStore.update(context) { s -> s.copy(fontSizeTier = it) }
                    onFontSizeTierChanged(it)
                },
            )
        }

        SettingsSectionLabel(stringResource(R.string.perso_display_section))
        SettingsCard {
            SettingsToggleRow(
                stringResource(R.string.perso_amoled_dark),
                stringResource(R.string.perso_amoled_dark_sub),
                Icons.Default.DarkMode,
                MaterialTheme.colorScheme.primary,
                amoledDark,
            ) { v ->
                amoledDark = v
                if (v && selectedTheme != PersonalizationStore.THEME_LIGHT) {
                    selectedTheme = PersonalizationStore.THEME_AMOLED
                } else if (!v && selectedTheme == PersonalizationStore.THEME_AMOLED) {
                    selectedTheme = PersonalizationStore.THEME_DEEP_DARK
                }
                PersonalizationStore.update(context) {
                    it.copy(
                        amoledBlack = v,
                        darkTheme = selectedTheme != PersonalizationStore.THEME_LIGHT,
                        themePreset = selectedTheme,
                    )
                }
                onAmoledBlackChanged(v)
                onThemePresetChanged(selectedTheme)
                onDarkThemePreference(selectedTheme != PersonalizationStore.THEME_LIGHT)
            }
            HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            thickness = 1.dp,
        )
            SettingsToggleRow(
                stringResource(R.string.perso_auto_theme),
                stringResource(R.string.perso_auto_theme_sub),
                Icons.Default.LightMode,
                Color(0xFFF5A623),
                autoDayNight,
            ) { v ->
                autoDayNight = v
                PersonalizationStore.update(context) { it.copy(followSystemTheme = v) }
                onFollowSystemThemeChanged(v)
            }
            HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            thickness = 1.dp,
        )
            SettingsToggleRow(
                stringResource(R.string.perso_material_you),
                stringResource(R.string.perso_material_you_sub),
                Icons.Default.Palette,
                MaterialTheme.colorScheme.secondary,
                materialYou,
            ) { v ->
                materialYou = v
                PersonalizationStore.update(context) { it.copy(materialYou = v) }
                onMaterialYouChanged(v)
            }
            HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            thickness = 1.dp,
        )
            SettingsToggleRow(
                stringResource(R.string.perso_haptics),
                stringResource(R.string.perso_haptics_sub),
                Icons.Default.Vibration,
                Color(0xFF4499FF),
                haptics,
            ) { v ->
                haptics = v
                PersonalizationStore.update(context) { s -> s.copy(hapticsEnabled = v) }
                onHapticsChanged(v)
            }
        }

        SettingsSectionLabel(stringResource(R.string.perso_call_section))
        SettingsCard {
            SettingsToggleRow(
                stringResource(R.string.perso_call_timer),
                stringResource(R.string.perso_call_timer_sub),
                Icons.Default.Timer,
                Color(0xFF00C472),
                showCallTimer,
            ) { v ->
                showCallTimer = v
                PersonalizationStore.update(context) { s -> s.copy(showInCallTimer = v) }
                onShowInCallTimerChanged(v)
            }
            HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            thickness = 1.dp,
        )
            SettingsToggleRow(
                stringResource(R.string.perso_call_recording),
                stringResource(R.string.perso_call_recording_sub),
                Icons.Default.FiberManualRecord,
                Color(0xFFE53935),
                callRecordingEnabled,
            ) { next ->
                callRecordingEnabled = next
                CallRecordingPreferences.setEnabled(context, next)
            }
            Text(
                stringResource(R.string.perso_incoming_full_screen_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
            HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            thickness = 1.dp,
        )
            SettingsToggleRow(
                stringResource(R.string.perso_verified_badge),
                stringResource(R.string.perso_verified_badge_sub),
                Icons.Default.VerifiedUser,
                Color(0xFFFF9F43),
                verifiedBadge,
            ) { v ->
                verifiedBadge = v
                PersonalizationStore.update(context) { s -> s.copy(verifiedCallerBadge = v) }
                onVerifiedCallerBadgeChanged(v)
            }
        }

        SettingsSectionLabel(stringResource(R.string.perso_messages_section))
        SettingsCard {
            SettingsToggleRow(
                stringResource(R.string.perso_otp_autocopy),
                stringResource(R.string.perso_otp_autocopy_sub),
                Icons.Default.Key,
                Color(0xFFA855F7),
                otpAutoCopy,
            ) { v ->
                otpAutoCopy = v
                PersonalizationStore.update(context) { s -> s.copy(otpAutoCopy = v) }
                onOtpAutoCopyChanged(v)
            }
            HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            thickness = 1.dp,
        )
            SettingsToggleRow(
                stringResource(R.string.perso_rcs),
                stringResource(R.string.perso_rcs_sub),
                Icons.AutoMirrored.Filled.Chat,
                Color(0xFF00A884),
                rcsEnabled,
            ) { v ->
                rcsEnabled = v
                PersonalizationStore.update(context) { s -> s.copy(rcsUiEnabled = v) }
                onRcsUiChanged(v)
            }
            HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            thickness = 1.dp,
        )
            SettingsToggleRow(
                stringResource(R.string.personalization_dense_threads),
                stringResource(R.string.personalization_dense_threads_sub),
                Icons.Default.ViewCompact,
                Color(0xFF4499FF),
                denseThreads,
            ) { v ->
                denseThreads = v
                PersonalizationStore.update(context) { s -> s.copy(denseThreads = v) }
                onDenseThreadsChanged(v)
            }
        }

        SettingsSectionLabel(stringResource(R.string.perso_money_section))
        SettingsCard {
            SettingsToggleRow(
                stringResource(R.string.perso_over_budget),
                stringResource(R.string.perso_over_budget_sub),
                Icons.Default.Notifications,
                Color(0xFFE74C3C),
                overBudgetAlert,
            ) { v ->
                overBudgetAlert = v
                PersonalizationStore.update(context) { s -> s.copy(overBudgetAlerts = v) }
                onOverBudgetAlertsChanged(v)
            }
        }

        SettingsSectionLabel(stringResource(R.string.perso_privacy_section))
        SettingsCard {
            SettingsToggleRow(
                stringResource(R.string.perso_app_lock),
                stringResource(R.string.perso_app_lock_sub),
                Icons.Default.Lock,
                MaterialTheme.colorScheme.primary,
                appLock,
            ) { v ->
                appLock = v
                PersonalizationStore.update(context) { s -> s.copy(appLockEnabled = v) }
                onAppLockChanged(v)
            }
            HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            thickness = 1.dp,
        )
            SettingsToggleRow(
                stringResource(R.string.perso_blur_amounts),
                stringResource(R.string.perso_blur_amounts_sub),
                Icons.Default.VisibilityOff,
                Color(0xFFFF6B9D),
                blurAmounts,
            ) { v ->
                blurAmounts = v
                PersonalizationStore.update(context) { s -> s.copy(blurMoneyAmounts = v) }
                onBlurMoneyAmountsChanged(v)
            }
            HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            thickness = 1.dp,
        )
            SettingsToggleRow(
                stringResource(R.string.perso_stealth_mode),
                stringResource(R.string.perso_stealth_mode_sub),
                Icons.Default.Security,
                Color(0xFFE74C3C),
                stealthMode,
            ) { v ->
                stealthMode = v
                PersonalizationStore.update(context) { s -> s.copy(stealthMode = v) }
                onStealthModeChanged(v)
            }
        }

            Text(
                stringResource(R.string.personalization_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun DataDeviceBody(
    onExportLocalDatabase: () -> Unit,
    onRestoreLocalDatabase: () -> Unit,
    onInformCloudBackup: () -> Unit,
) {
    var autoBackup by remember { mutableStateOf(false) }
    var exportIncludeAmounts by remember { mutableStateOf(true) }

    // ── Backup & Restore ──────────────────────────────────────────────────
    SettingsSectionLabel("Backup & Restore", topPaddingExtra = 4.dp)
    SettingsCard {
        SettingsToggleItem(
            label = "Auto-backup to Google Drive",
            sub = "Manually triggered — no background sync",
            checked = autoBackup,
            onChange = { autoBackup = it },
        )
        SettingsDivider()
        SettingsActionItem(
            label = stringResource(R.string.settings_export_db_title),
            sub = stringResource(R.string.settings_export_db_sub),
            onClick = onExportLocalDatabase,
        )
        SettingsDivider()
        SettingsActionItem(
            label = stringResource(R.string.settings_restore_db_title),
            sub = stringResource(R.string.settings_restore_db_sub),
            onClick = onRestoreLocalDatabase,
        )
        SettingsDivider()
        SettingsActionItem(
            label = stringResource(R.string.settings_backup_now),
            sub = stringResource(R.string.settings_backup_now_sub),
            onClick = onInformCloudBackup,
        )
        SettingsDivider()
        SettingsActionItem(
            label = stringResource(R.string.settings_restore_drive),
            sub = stringResource(R.string.settings_restore_drive_sub),
            onClick = onInformCloudBackup,
        )
    }

    SettingsSectionLabel(stringResource(R.string.settings_section_mms), topPaddingExtra = 4.dp)
    SettingsCard {
            Text(
                stringResource(R.string.settings_mms_scope_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }

    // ── Export ────────────────────────────────────────────────────────────
    SettingsSectionLabel("Export")
    SettingsCard {
        SettingsToggleItem(
            label = "Include transaction amounts",
            sub = "Toggle off for a privacy-safe CSV share",
            checked = exportIncludeAmounts,
            onChange = { exportIncludeAmounts = it },
        )
        SettingsDivider()
        SettingsActionItem(label = "Export SMS transactions (CSV)", sub = "Save parsed transactions to Downloads")
        SettingsDivider()
        SettingsActionItem(label = "Export transactions (PDF)", sub = "Summary + full list as a PDF in Downloads")
        SettingsDivider()
        SettingsActionItem(label = "Export call log (CSV)", sub = "Save call history to Downloads")
    }

    // ── Storage & Reset ───────────────────────────────────────────────────
    SettingsSectionLabel("Storage & Reset")
    SettingsCard {
        SettingsActionItem(label = "Clear OTP history", sub = "Delete all stored OTP logs")
        SettingsDivider()
        SettingsActionItem(label = "Clear spam list", sub = "Remove all local spam number entries")
    }

    // ── About ─────────────────────────────────────────────────────────────
    SettingsSectionLabel("About")
    SettingsCard {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                "PhonIQ v1.0.0-alpha",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.settings_about_offline_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(Modifier.height(32.dp))
}

@Composable
private fun SettingsSectionLabel(text: String, topPaddingExtra: Dp = 0.dp) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        modifier =
            Modifier.padding(
                start = 16.dp,
                top = 12.dp + topPaddingExtra,
                end = 16.dp,
                bottom = 6.dp,
            ),
        style =
            MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold,
            ),
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsNavRow(
    label: String,
    sub: String,
    icon: ImageVector,
    iconBrush: Brush,
    onClick: () -> Unit,
) {
    val rowContentDescription =
        remember(label, sub) {
            "${label.takeIf { it.isNotBlank() } ?: ""}. ${sub.takeIf { it.isNotBlank() } ?: ""}".trim()
        }
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = rowContentDescription
                },
        color = Color.Transparent,
        shape = RoundedCornerShape(0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBrush),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            Text(
                "›",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    sub: String,
    icon: ImageVector,
    iconBg: Color,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val toggleState =
        stringResource(if (checked) R.string.cd_toggle_state_on else R.string.cd_toggle_state_off)
    val rowContentDesc =
        remember(label, sub, toggleState) {
            "$label. $sub. $toggleState"
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = rowContentDesc
                },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        PhoniqAccentSwitch(checked, onChange)
    }
}

@Composable
private fun PhoniqAccentSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val thumbOffset by animateDpAsState(if (checked) 18.dp else 0.dp, label = "toggleThumb")
    val trackOff = scheme.surfaceVariant.copy(alpha = 0.55f)
    Box(
        modifier =
            Modifier
                .size(width = 42.dp, height = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (checked) scheme.primary else trackOff)
                .clickable { onCheckedChange(!checked) },
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 3.dp)
                .offset(x = thumbOffset)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

private data class ThemeTileSpec(
    val nameKey: String,
    val previewBg: Color,
    val accentBar: Color,
    val row1: Color,
    val row2: Color,
    val labelBg: Color,
    val labelFg: Color,
)

private fun themeTileSpecs(primary: Color): List<ThemeTileSpec> =
    listOf(
        ThemeTileSpec(
            "AMOLED",
            Color(0xFF000000),
            primary,
            Color(0xFF222222),
            Color(0xFF1A1A2A),
            Color(0xFF000000),
            Color(0xFF888888),
        ),
        ThemeTileSpec(
            "Deep Dark",
            Color(0xFF0A0A0F),
            primary,
            Color(0xFF252535),
            Color(0xFF1E1E30),
            Color(0xFF0A0A0F),
            Color(0xFF888888),
        ),
        ThemeTileSpec(
            "Dark Navy",
            Color(0xFF080F1A),
            Color(0xFF4499FF),
            Color(0xFF152035),
            Color(0xFF1A2840),
            Color(0xFF080F1A),
            Color(0xFF888888),
        ),
        ThemeTileSpec(
            "Forest",
            Color(0xFF081208),
            Color(0xFF00C472),
            Color(0xFF152515),
            Color(0xFF1A2A1A),
            Color(0xFF081208),
            Color(0xFF888888),
        ),
        ThemeTileSpec(
            "Wine",
            Color(0xFF120808),
            Color(0xFFFF6B9D),
            Color(0xFF251515),
            Color(0xFF2A1A1A),
            Color(0xFF120808),
            Color(0xFF888888),
        ),
        ThemeTileSpec(
            "Samsung",
            Color(0xFF101218),
            Color(0xFF3D9BFF),
            Color(0xFF252A34),
            Color(0xFF1E232B),
            Color(0xFF101218),
            Color(0xFFBBBBCC),
        ),
        ThemeTileSpec(
            "Daily Dial",
            Color(0xFF0A0812),
            Color(0xFF8B5CF6),
            Color(0xFF1E1830),
            Color(0xFF251A3D),
            Color(0xFF0A0812),
            Color(0xFFAAA0CC),
        ),
        ThemeTileSpec(
            "Neo Mirror",
            Color(0xFF020306),
            Color(0xFF00E8F9),
            Color(0xFF0C141C),
            Color(0xFF101820),
            Color(0xFF020306),
            Color(0xFF8EC4CC),
        ),
        ThemeTileSpec(
            "Dialer 360",
            Color(0xFF0C1629),
            Color(0xFF3B82F6),
            Color(0xFF1A2840),
            Color(0xFF243652),
            Color(0xFF0C1629),
            Color(0xFFB8C5DC),
        ),
        ThemeTileSpec(
            "Nothing Dial",
            Color(0xFF000000),
            Color(0xFFFF3B30),
            Color(0xFF1C1C1C),
            Color(0xFF252525),
            Color(0xFF000000),
            Color(0xFFB0B0B0),
        ),
        ThemeTileSpec(
            "Glass Dial",
            Color(0xFF2D3142),
            Color(0xFF8AB4F8),
            Color(0xFF3D4358),
            Color(0xFF4A5168),
            Color(0xFF2D3142),
            Color(0xFFD0D8F0),
        ),
        ThemeTileSpec(
            "AI Translator",
            Color(0xFF1A1030),
            Color(0xFF2DD4BF),
            Color(0xFF2A2148),
            Color(0xFF35285C),
            Color(0xFF1A1030),
            Color(0xFFC4B8E8),
        ),
        ThemeTileSpec(
            "SaaS Widget",
            Color(0xFF1F2937),
            Color(0xFF60A5FA),
            Color(0xFF374151),
            Color(0xFF4B5563),
            Color(0xFF1F2937),
            Color(0xFFCBD5E1),
        ),
        ThemeTileSpec(
            "Plum Inbox",
            Color(0xFF231F2E),
            Color(0xFFF472B6),
            Color(0xFF3D3448),
            Color(0xFF4A4058),
            Color(0xFF231F2E),
            Color(0xFFE9D5FF),
        ),
        ThemeTileSpec(
            "Carbon Thread",
            Color(0xFF0D1117),
            Color(0xFF39D98A),
            Color(0xFF21262D),
            Color(0xFF30363D),
            Color(0xFF0D1117),
            Color(0xFF8B949E),
        ),
        ThemeTileSpec(
            "Blue Thread",
            Color(0xFF1A1D26),
            Color(0xFF5BA4F8),
            Color(0xFF323746),
            Color(0xFF3D4352),
            Color(0xFF1A1D26),
            Color(0xFF9BA4B5),
        ),
        ThemeTileSpec(
            "Coral Dusk",
            Color(0xFF151018),
            Color(0xFFFF8A65),
            Color(0xFF353040),
            Color(0xFF45405A),
            Color(0xFF151018),
            Color(0xFFE8D5F0),
        ),
        ThemeTileSpec(
            "Teal Tide",
            Color(0xFF0C1418),
            Color(0xFF2DD4BF),
            Color(0xFF1A3040),
            Color(0xFF244854),
            Color(0xFF0C1418),
            Color(0xFFB2DFDB),
        ),
        ThemeTileSpec(
            "Indigo Line",
            Color(0xFF13141C),
            Color(0xFF818CF8),
            Color(0xFF252440),
            Color(0xFF2E2F48),
            Color(0xFF13141C),
            Color(0xFFC7D2FE),
        ),
        ThemeTileSpec(
            "Sky Panel",
            Color(0xFF141416),
            Color(0xFF38BDF8),
            Color(0xFF2A2A30),
            Color(0xFF35353D),
            Color(0xFF141416),
            Color(0xFF94A3B8),
        ),
        ThemeTileSpec(
            "Violet Studio",
            Color(0xFF1A1025),
            Color(0xFFE879F9),
            Color(0xFF352848),
            Color(0xFF403058),
            Color(0xFF1A1025),
            Color(0xFFF5D0FE),
        ),
        ThemeTileSpec(
            "Phone Style",
            Color(0xFF000000),
            Color(0xFF0A84FF),
            Color(0xFF1C1C1E),
            Color(0xFF2C2C2E),
            Color(0xFF000000),
            Color(0xFFEBEBF5),
        ),
        ThemeTileSpec(
            "Material 3",
            Color(0xFF10131A),
            Color(0xFFD0BCFF),
            Color(0xFF1B1F2A),
            Color(0xFF2A3142),
            Color(0xFF10131A),
            Color(0xFFE8E1F5),
        ),
        ThemeTileSpec(
            "Light",
            Color(0xFFF5F5FA),
            primary,
            Color(0xFFDDDDDD),
            Color(0xFFE8E8F0),
            Color(0xFFF5F5FA),
            Color(0xFF555555),
        ),
    )

@Composable
private fun ThemeTilesRow(
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val specs = themeTileSpecs(MaterialTheme.colorScheme.primary)
        for (spec in specs) {
            ThemeTileCard(spec = spec, selected = spec.nameKey == selected, onClick = { onSelect(spec.nameKey) })
        }
    }
}

@Composable
private fun ThemeTileCard(
    spec: ThemeTileSpec,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border =
            BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color =
                    if (selected) {
                        scheme.primary
                    } else {
                        scheme.outlineVariant.copy(alpha = 0.85f)
                    },
            ),
    ) {
        Column(
            modifier = Modifier.width(76.dp).padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(spec.previewBg)
                        .padding(5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.62f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(spec.accentBar),
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.92f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(spec.row1),
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.72f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(spec.row2),
                )
            }
            Text(
                spec.nameKey,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = spec.labelFg,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(spec.labelBg)
                        .padding(vertical = 4.dp, horizontal = 2.dp),
            )
        }
    }
}

@Composable
private fun ChipRow(
    options: List<String>,
    selected: String,
    enabled: Boolean = true,
    onSelect: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { opt ->
            val isSelected = opt == selected
            Surface(
                onClick = { if (enabled) onSelect(opt) },
                enabled = enabled,
                shape = RoundedCornerShape(20.dp),
                color =
                    when {
                        !enabled -> scheme.surfaceVariant.copy(alpha = 0.35f)
                        isSelected -> scheme.primary.copy(alpha = 0.12f)
                        else -> Color.Transparent
                    },
                modifier =
                    Modifier.border(
                        width = 1.5.dp,
                        color =
                            when {
                                !enabled -> scheme.outlineVariant.copy(alpha = 0.35f)
                                isSelected -> scheme.primary
                                else -> scheme.outlineVariant.copy(alpha = 0.85f)
                            },
                        shape = RoundedCornerShape(20.dp),
                    ),
            ) {
                Text(
                    opt,
                    style = MaterialTheme.typography.labelMedium,
                    color =
                        when {
                            !enabled -> scheme.onSurface.copy(alpha = 0.38f)
                            isSelected -> scheme.primary
                            else -> scheme.onSurfaceVariant
                        },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun FontFamilyChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { opt ->
            val isSelected = opt == selected
            Surface(
                onClick = { onSelect(opt) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) scheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                modifier =
                    Modifier.border(
                        width = 1.5.dp,
                        color =
                            if (isSelected) {
                                scheme.primary
                            } else {
                                scheme.outlineVariant.copy(alpha = 0.85f)
                            },
                        shape = RoundedCornerShape(20.dp),
                    ),
            ) {
                Text(
                    opt,
                    style =
                        MaterialTheme.typography.labelMedium.copy(
                            fontFamily = fontFamilyForPersonalization(opt),
                        ),
                    color = if (isSelected) scheme.primary else scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun FontSizeTierChipRow(
    options: List<String>,
    selected: String,
    selectedFontKey: String,
    onSelect: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val fontFamily = fontFamilyForPersonalization(selectedFontKey)
    val baseSp = MaterialTheme.typography.labelMedium.fontSize.value
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { opt ->
            val isSelected = opt == selected
            val mul = fontTierMultiplier(opt)
            Surface(
                onClick = { onSelect(opt) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) scheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                modifier =
                    Modifier.border(
                        width = 1.5.dp,
                        color =
                            if (isSelected) {
                                scheme.primary
                            } else {
                                scheme.outlineVariant.copy(alpha = 0.85f)
                            },
                        shape = RoundedCornerShape(20.dp),
                    ),
            ) {
                Text(
                    opt,
                    style =
                        MaterialTheme.typography.labelMedium.copy(
                            fontFamily = fontFamily,
                            fontSize = (baseSp * mul).sp,
                        ),
                    color = if (isSelected) scheme.primary else scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun AccentSwatchRow(colors: List<Color>, selected: Int, onSelect: (Int) -> Unit) {
    val ring = MaterialTheme.colorScheme.primary
    Row(
        modifier =
            Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val selectedAccentLabel = stringResource(R.string.cd_selected_accent_swatch)
        colors.forEachIndexed { index, color ->
            Box(
                modifier =
                    Modifier
                        .size(30.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                            if (selected == index) {
                                Modifier.border(2.dp, ring, CircleShape)
                            } else {
                                Modifier
                            },
                    )
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                if (selected == index) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = selectedAccentLabel,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

// ── Lightweight helpers used by DataDeviceBody ─────────────────────────────

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 14.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        thickness = 0.5.dp,
    )
}

@Composable
private fun SettingsToggleItem(label: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        PhoniqAccentSwitch(checked, onChange)
    }
}

@Composable
private fun SettingsActionItem(label: String, sub: String, onClick: () -> Unit = {}) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            Text(
                "›",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            )
        }
    }
}
