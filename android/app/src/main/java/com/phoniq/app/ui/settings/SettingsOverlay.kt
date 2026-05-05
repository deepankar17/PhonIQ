package com.phoniq.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phoniq.app.R
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqOnSurfaceMuted
import com.phoniq.app.ui.theme.PhoniqSurface

private enum class SettingsPane { Root, Personalization, DataDevice }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsFullScreenOverlay(onDismiss: () -> Unit) {
    var pane by remember { mutableStateOf(SettingsPane.Root) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    when (pane) {
                        SettingsPane.Root ->
                            TopAppBar(
                                title = { Text(stringResource(R.string.settings_title)) },
                                actions = {
                                    IconButton(onClick = onDismiss) {
                                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close))
                                    }
                                },
                            )
                        SettingsPane.Personalization ->
                            TopAppBar(
                                title = { Text(stringResource(R.string.settings_personalization_title)) },
                                navigationIcon = {
                                    IconButton(onClick = { pane = SettingsPane.Root }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                                    }
                                },
                            )
                        SettingsPane.DataDevice ->
                            TopAppBar(
                                title = { Text(stringResource(R.string.settings_data_device_title)) },
                                navigationIcon = {
                                    IconButton(onClick = { pane = SettingsPane.Root }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
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
                        SettingsPane.Root -> RootBody(onPersonalization = { pane = SettingsPane.Personalization }, onDataDevice = { pane = SettingsPane.DataDevice })
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
    SectionLabel(stringResource(R.string.settings_group_general))
    Surface(onClick = onPersonalization, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_row_personalization)) },
            supportingContent = { Text(stringResource(R.string.settings_row_personalization_sub)) },
            trailingContent = { Text("›", style = MaterialTheme.typography.bodyLarge) },
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
    SectionLabel(stringResource(R.string.settings_group_data), topPad = 8.dp)
    Surface(onClick = onDataDevice, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_row_data_device)) },
            supportingContent = { Text(stringResource(R.string.settings_row_data_device_sub)) },
            trailingContent = { Text("›", style = MaterialTheme.typography.bodyLarge) },
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
    Surface(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_row_widgets)) },
            supportingContent = { Text(stringResource(R.string.settings_row_widgets_sub)) },
            trailingContent = { Text("›", style = MaterialTheme.typography.bodyLarge) },
        )
    }
}

@Composable
private fun PersonalizationBody() {
    // App Theme
    var selectedTheme by remember { mutableStateOf("Deep Dark") }
    val themes = listOf("AMOLED", "Deep Dark", "Dark Navy", "Forest", "Wine", "Light")

    // Accent Color
    val accentColors = listOf(
        Color(0xFF6C63FF), Color(0xFF00D4AA), Color(0xFF2196F3), Color(0xFFE91E63),
        Color(0xFFFF9800), Color(0xFF4CAF50), Color(0xFFF44336), Color(0xFF9C27B0),
        Color(0xFF00BCD4), Color(0xFFFFEB3B),
    )
    var selectedAccent by remember { mutableStateOf(0) }

    // Dialpad style
    var selectedDialpad by remember { mutableStateOf("Classic") }
    val dialpadStyles = listOf("Classic", "Rounded", "Minimal", "iOS-like", "Material 3")

    // Font family
    var selectedFont by remember { mutableStateOf("Roboto") }
    val fonts = listOf("Roboto", "Nunito", "Mono", "Serif", "System")

    // Font size
    var selectedSize by remember { mutableStateOf("Normal") }
    val fontSizes = listOf("Small", "Normal", "Large", "XL")

    // Display toggles
    var amoledDark by remember { mutableStateOf(false) }
    var autoDayNight by remember { mutableStateOf(false) }
    var materialYou by remember { mutableStateOf(false) }
    var haptics by remember { mutableStateOf(true) }

    // Call screen
    var showCallTimer by remember { mutableStateOf(true) }
    var verifiedBadge by remember { mutableStateOf(true) }

    // Messages
    var otpAutoCopy by remember { mutableStateOf(true) }
    var rcsEnabled by remember { mutableStateOf(true) }
    var denseThreads by remember { mutableStateOf(true) }

    // Money
    var overBudgetAlert by remember { mutableStateOf(true) }

    // Privacy
    var appLock by remember { mutableStateOf(false) }
    var blurAmounts by remember { mutableStateOf(false) }
    var stealthMode by remember { mutableStateOf(false) }

    Column(Modifier.padding(bottom = 32.dp)) {
        // ── App Theme ──────────────────────────────────────────────
        SectionLabel(stringResource(R.string.perso_theme_section))
        ChipRow(options = themes, selected = selectedTheme, onSelect = { selectedTheme = it })
        HorizontalDivider()

        // ── Accent Color ──────────────────────────────────────────
        SectionLabel(stringResource(R.string.perso_accent_section))
        AccentSwatchRow(colors = accentColors, selected = selectedAccent, onSelect = { selectedAccent = it })
        HorizontalDivider()

        // ── Dialpad Style ─────────────────────────────────────────
        SectionLabel(stringResource(R.string.perso_dialpad_section))
        ChipRow(options = dialpadStyles, selected = selectedDialpad, onSelect = { selectedDialpad = it })
        HorizontalDivider()

        // ── Font Family ───────────────────────────────────────────
        SectionLabel(stringResource(R.string.perso_font_family_section))
        ChipRow(options = fonts, selected = selectedFont, onSelect = { selectedFont = it })
        HorizontalDivider()

        // ── Font Size ─────────────────────────────────────────────
        SectionLabel(stringResource(R.string.perso_font_size_section))
        ChipRow(options = fontSizes, selected = selectedSize, onSelect = { selectedSize = it })
        HorizontalDivider()

        // ── Display ───────────────────────────────────────────────
        SectionLabel(stringResource(R.string.perso_display_section))
        ToggleRow(stringResource(R.string.perso_amoled_dark), stringResource(R.string.perso_amoled_dark_sub), amoledDark) { amoledDark = it }
        ToggleRow(stringResource(R.string.perso_auto_theme), stringResource(R.string.perso_auto_theme_sub), autoDayNight) { autoDayNight = it }
        ToggleRow(stringResource(R.string.perso_material_you), stringResource(R.string.perso_material_you_sub), materialYou) { materialYou = it }
        ToggleRow(stringResource(R.string.perso_haptics), stringResource(R.string.perso_haptics_sub), haptics) { haptics = it }
        HorizontalDivider()

        // ── Call Screen ───────────────────────────────────────────
        SectionLabel(stringResource(R.string.perso_call_section))
        ToggleRow(stringResource(R.string.perso_call_timer), stringResource(R.string.perso_call_timer_sub), showCallTimer) { showCallTimer = it }
        ToggleRow(stringResource(R.string.perso_verified_badge), stringResource(R.string.perso_verified_badge_sub), verifiedBadge) { verifiedBadge = it }
        HorizontalDivider()

        // ── Messages ──────────────────────────────────────────────
        SectionLabel(stringResource(R.string.perso_messages_section))
        ToggleRow(stringResource(R.string.perso_otp_autocopy), stringResource(R.string.perso_otp_autocopy_sub), otpAutoCopy) { otpAutoCopy = it }
        ToggleRow(stringResource(R.string.perso_rcs), stringResource(R.string.perso_rcs_sub), rcsEnabled) { rcsEnabled = it }
        ToggleRow(stringResource(R.string.personalization_dense_threads), stringResource(R.string.personalization_dense_threads_sub), denseThreads) { denseThreads = it }
        HorizontalDivider()

        // ── Money Manager ─────────────────────────────────────────
        SectionLabel(stringResource(R.string.perso_money_section))
        ToggleRow(stringResource(R.string.perso_over_budget), stringResource(R.string.perso_over_budget_sub), overBudgetAlert) { overBudgetAlert = it }
        HorizontalDivider()

        // ── Privacy & Security ────────────────────────────────────
        SectionLabel(stringResource(R.string.perso_privacy_section))
        ToggleRow(stringResource(R.string.perso_app_lock), stringResource(R.string.perso_app_lock_sub), appLock) { appLock = it }
        ToggleRow(stringResource(R.string.perso_blur_amounts), stringResource(R.string.perso_blur_amounts_sub), blurAmounts) { blurAmounts = it }
        ToggleRow(stringResource(R.string.perso_stealth_mode), stringResource(R.string.perso_stealth_mode_sub), stealthMode) { stealthMode = it }

        Text(
            stringResource(R.string.personalization_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun DataDeviceBody() {
    Column(Modifier.padding(16.dp)) {
        Text(stringResource(R.string.data_device_backup_intro), style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(R.string.data_device_backup_wire),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

// ── Helper composables ──────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String, topPad: androidx.compose.ui.unit.Dp = 4.dp) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = PhoniqAccent,
        modifier = Modifier.padding(start = 16.dp, top = topPad + 8.dp, bottom = 4.dp),
        letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp),
    )
}

@Composable
private fun ChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { opt ->
            val isSelected = opt == selected
            Surface(
                onClick = { onSelect(opt) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) PhoniqAccent.copy(alpha = 0.12f) else Color.Transparent,
                modifier = Modifier.border(
                    width = 1.5.dp,
                    color = if (isSelected) PhoniqAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
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
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        colors.forEachIndexed { index, color ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (selected == index) Modifier.border(2.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                        else Modifier
                    )
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                if (selected == index) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(sub, style = MaterialTheme.typography.bodySmall) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
    )
}
