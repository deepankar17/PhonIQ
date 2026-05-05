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
import androidx.compose.material.icons.filled.DeleteSweep
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Chat
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phoniq.app.R
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqBackground
import com.phoniq.app.ui.theme.PhoniqBorder
import com.phoniq.app.ui.theme.PhoniqBorderSoft
import com.phoniq.app.ui.theme.PhoniqOnBackground
import com.phoniq.app.ui.theme.PhoniqOnSurfaceMuted
import com.phoniq.app.ui.theme.PhoniqSecondary
import com.phoniq.app.ui.theme.PhoniqSurface
import com.phoniq.app.ui.theme.PhoniqSurfaceLow
import com.phoniq.app.ui.theme.PhoniqTextSecondaryMock
import java.util.Locale

private enum class SettingsPane { Root, Personalization, DataDevice }

private val ToggleTrackOff = Color(0xFF2A2A3A)
private val ChevronMuted = Color(0xFF444444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsFullScreenOverlay(onDismiss: () -> Unit) {
    var pane by remember { mutableStateOf(SettingsPane.Root) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = PhoniqBackground) {
            val barColors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = PhoniqSurfaceLow,
                    titleContentColor = PhoniqOnBackground,
                    navigationIconContentColor = PhoniqOnBackground,
                    actionIconContentColor = PhoniqOnBackground,
                )
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = PhoniqBackground,
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
                            )
                        SettingsPane.Personalization -> PersonalizationBody()
                        SettingsPane.DataDevice -> DataDeviceBody()
                    }
                }
            }
        }
    }
}

@Composable
private fun RootBody(onPersonalization: () -> Unit, onDataDevice: () -> Unit) {
    SettingsSectionLabel(stringResource(R.string.settings_group_general), topPaddingExtra = 4.dp)
    SettingsCard {
        SettingsNavRow(
            label = stringResource(R.string.settings_row_personalization),
            sub = stringResource(R.string.settings_row_personalization_sub),
            icon = Icons.Default.Palette,
            iconBrush = Brush.linearGradient(colors = listOf(PhoniqAccent, PhoniqSecondary)),
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
        HorizontalDivider(color = PhoniqBorderSoft, thickness = 1.dp)
        SettingsNavRow(
            label = stringResource(R.string.settings_row_widgets),
            sub = stringResource(R.string.settings_row_widgets_sub),
            icon = Icons.Default.Apps,
            iconBrush =
                Brush.linearGradient(
                    colors = listOf(Color(0xFF95A5A6), Color(0xFF7F8C8D)),
                ),
            onClick = {},
        )
    }
}

@Composable
private fun PersonalizationBody() {
    var selectedTheme by remember { mutableStateOf("Deep Dark") }
    val themes = listOf("AMOLED", "Deep Dark", "Dark Navy", "Forest", "Wine", "Light")

    val accentColors =
        listOf(
            Color(0xFF6C63FF),
            Color(0xFF00D4AA),
            Color(0xFF2196F3),
            Color(0xFFE91E63),
            Color(0xFFFF9800),
            Color(0xFF4CAF50),
            Color(0xFFF44336),
            Color(0xFF9C27B0),
            Color(0xFF00BCD4),
            Color(0xFFFFEB3B),
        )
    var selectedAccent by remember { mutableStateOf(0) }

    var selectedDialpad by remember { mutableStateOf("Classic") }
    val dialpadStyles = listOf("Classic", "Rounded", "Minimal", "iOS-like", "Material 3")

    var selectedFont by remember { mutableStateOf("Roboto") }
    val fonts = listOf("Roboto", "Nunito", "Mono", "Serif", "System")

    var selectedSize by remember { mutableStateOf("Normal") }
    val fontSizes = listOf("Small", "Normal", "Large", "XL")

    var amoledDark by remember { mutableStateOf(false) }
    var autoDayNight by remember { mutableStateOf(false) }
    var materialYou by remember { mutableStateOf(false) }
    var haptics by remember { mutableStateOf(true) }

    var showCallTimer by remember { mutableStateOf(true) }
    var verifiedBadge by remember { mutableStateOf(true) }

    var otpAutoCopy by remember { mutableStateOf(true) }
    var rcsEnabled by remember { mutableStateOf(true) }
    var denseThreads by remember { mutableStateOf(true) }

    var overBudgetAlert by remember { mutableStateOf(true) }

    var appLock by remember { mutableStateOf(false) }
    var blurAmounts by remember { mutableStateOf(false) }
    var stealthMode by remember { mutableStateOf(false) }

    Column(Modifier.padding(bottom = 32.dp)) {
        SettingsSectionLabel(stringResource(R.string.perso_theme_section), topPaddingExtra = 4.dp)
        SettingsCard {
            ChipRow(options = themes, selected = selectedTheme, onSelect = { selectedTheme = it })
        }

        SettingsSectionLabel(stringResource(R.string.perso_accent_section))
        SettingsCard {
            AccentSwatchRow(colors = accentColors, selected = selectedAccent, onSelect = { selectedAccent = it })
        }

        SettingsSectionLabel(stringResource(R.string.perso_dialpad_section))
        SettingsCard {
            ChipRow(options = dialpadStyles, selected = selectedDialpad, onSelect = { selectedDialpad = it })
        }

        SettingsSectionLabel(stringResource(R.string.perso_font_family_section))
        SettingsCard {
            ChipRow(options = fonts, selected = selectedFont, onSelect = { selectedFont = it })
        }

        SettingsSectionLabel(stringResource(R.string.perso_font_size_section))
        SettingsCard {
            ChipRow(options = fontSizes, selected = selectedSize, onSelect = { selectedSize = it })
        }

        SettingsSectionLabel(stringResource(R.string.perso_display_section))
        SettingsCard {
            SettingsToggleRow(
                stringResource(R.string.perso_amoled_dark),
                stringResource(R.string.perso_amoled_dark_sub),
                Icons.Default.DarkMode,
                PhoniqAccent,
                amoledDark,
            ) { amoledDark = it }
            HorizontalDivider(color = PhoniqBorderSoft, thickness = 1.dp)
            SettingsToggleRow(
                stringResource(R.string.perso_auto_theme),
                stringResource(R.string.perso_auto_theme_sub),
                Icons.Default.LightMode,
                Color(0xFFF5A623),
                autoDayNight,
            ) { autoDayNight = it }
            HorizontalDivider(color = PhoniqBorderSoft, thickness = 1.dp)
            SettingsToggleRow(
                stringResource(R.string.perso_material_you),
                stringResource(R.string.perso_material_you_sub),
                Icons.Default.Palette,
                PhoniqSecondary,
                materialYou,
            ) { materialYou = it }
            HorizontalDivider(color = PhoniqBorderSoft, thickness = 1.dp)
            SettingsToggleRow(
                stringResource(R.string.perso_haptics),
                stringResource(R.string.perso_haptics_sub),
                Icons.Default.Vibration,
                Color(0xFF4499FF),
                haptics,
            ) { haptics = it }
        }

        SettingsSectionLabel(stringResource(R.string.perso_call_section))
        SettingsCard {
            SettingsToggleRow(
                stringResource(R.string.perso_call_timer),
                stringResource(R.string.perso_call_timer_sub),
                Icons.Default.Timer,
                Color(0xFF00C472),
                showCallTimer,
            ) { showCallTimer = it }
            HorizontalDivider(color = PhoniqBorderSoft, thickness = 1.dp)
            SettingsToggleRow(
                stringResource(R.string.perso_verified_badge),
                stringResource(R.string.perso_verified_badge_sub),
                Icons.Default.VerifiedUser,
                Color(0xFFFF9F43),
                verifiedBadge,
            ) { verifiedBadge = it }
        }

        SettingsSectionLabel(stringResource(R.string.perso_messages_section))
        SettingsCard {
            SettingsToggleRow(
                stringResource(R.string.perso_otp_autocopy),
                stringResource(R.string.perso_otp_autocopy_sub),
                Icons.Default.Key,
                Color(0xFFA855F7),
                otpAutoCopy,
            ) { otpAutoCopy = it }
            HorizontalDivider(color = PhoniqBorderSoft, thickness = 1.dp)
            SettingsToggleRow(
                stringResource(R.string.perso_rcs),
                stringResource(R.string.perso_rcs_sub),
                Icons.Default.Chat,
                Color(0xFF00A884),
                rcsEnabled,
            ) { rcsEnabled = it }
            HorizontalDivider(color = PhoniqBorderSoft, thickness = 1.dp)
            SettingsToggleRow(
                stringResource(R.string.personalization_dense_threads),
                stringResource(R.string.personalization_dense_threads_sub),
                Icons.Default.ViewCompact,
                Color(0xFF4499FF),
                denseThreads,
            ) { denseThreads = it }
        }

        SettingsSectionLabel(stringResource(R.string.perso_money_section))
        SettingsCard {
            SettingsToggleRow(
                stringResource(R.string.perso_over_budget),
                stringResource(R.string.perso_over_budget_sub),
                Icons.Default.Notifications,
                Color(0xFFE74C3C),
                overBudgetAlert,
            ) { overBudgetAlert = it }
        }

        SettingsSectionLabel(stringResource(R.string.perso_privacy_section))
        SettingsCard {
            SettingsToggleRow(
                stringResource(R.string.perso_app_lock),
                stringResource(R.string.perso_app_lock_sub),
                Icons.Default.Lock,
                PhoniqAccent,
                appLock,
            ) { appLock = it }
            HorizontalDivider(color = PhoniqBorderSoft, thickness = 1.dp)
            SettingsToggleRow(
                stringResource(R.string.perso_blur_amounts),
                stringResource(R.string.perso_blur_amounts_sub),
                Icons.Default.VisibilityOff,
                Color(0xFFFF6B9D),
                blurAmounts,
            ) { blurAmounts = it }
            HorizontalDivider(color = PhoniqBorderSoft, thickness = 1.dp)
            SettingsToggleRow(
                stringResource(R.string.perso_stealth_mode),
                stringResource(R.string.perso_stealth_mode_sub),
                Icons.Default.Security,
                Color(0xFFE74C3C),
                stealthMode,
            ) { stealthMode = it }
        }

        Text(
            stringResource(R.string.personalization_note),
            style = MaterialTheme.typography.bodySmall,
            color = PhoniqTextSecondaryMock,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun DataDeviceBody() {
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
        SettingsActionItem(label = "Backup now", sub = "Save all PhonIQ data to Drive")
        SettingsDivider()
        SettingsActionItem(label = "Restore from Drive", sub = "Pick a backup file to restore")
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
                color = PhoniqOnBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "100% offline · No Internet permission.\nAll data stays on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = PhoniqTextSecondaryMock,
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
        color = PhoniqAccent,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = PhoniqSurface,
        border = BorderStroke(1.dp, PhoniqBorder),
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
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
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
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = PhoniqOnBackground,
                )
                Text(
                    sub,
                    fontSize = 11.sp,
                    color = PhoniqTextSecondaryMock,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            Text("›", fontSize = 14.sp, color = ChevronMuted)
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
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = PhoniqOnBackground,
            )
            Text(
                sub,
                fontSize = 11.sp,
                color = PhoniqTextSecondaryMock,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        MockPhoniqToggle(checked, onChange)
    }
}

@Composable
private fun MockPhoniqToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val thumbOffset by animateDpAsState(if (checked) 18.dp else 0.dp, label = "toggleThumb")
    Box(
        modifier =
            Modifier
                .size(width = 42.dp, height = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (checked) PhoniqAccent else ToggleTrackOff)
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

@Composable
private fun ChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
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
                color = if (isSelected) PhoniqAccent.copy(alpha = 0.12f) else Color.Transparent,
                modifier =
                    Modifier.border(
                        width = 1.5.dp,
                        color = if (isSelected) PhoniqAccent else PhoniqBorderSoft.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(20.dp),
                    ),
            ) {
                Text(
                    opt,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) PhoniqAccent else PhoniqOnSurfaceMuted,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun AccentSwatchRow(colors: List<Color>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        colors.forEachIndexed { index, color ->
            Box(
                modifier =
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (selected == index) {
                                Modifier.border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                if (selected == index) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
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
        color = PhoniqBorder.copy(alpha = 0.5f),
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
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PhoniqOnBackground)
            Text(sub, fontSize = 11.sp, color = PhoniqTextSecondaryMock, modifier = Modifier.padding(top = 1.dp))
        }
        MockPhoniqToggle(checked, onChange)
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
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PhoniqOnBackground)
                Text(sub, fontSize = 11.sp, color = PhoniqTextSecondaryMock, modifier = Modifier.padding(top = 1.dp))
            }
            Text("›", fontSize = 14.sp, color = ChevronMuted)
        }
    }
}
