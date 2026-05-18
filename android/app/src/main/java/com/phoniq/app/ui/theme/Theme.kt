package com.phoniq.app.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.phoniq.app.util.PersonalizationStore

/** When true, message bubbles use tighter vertical spacing; when false, spacing is roomier. */
val LocalDenseThreads = compositionLocalOf { true }

/** Active personalization theme tile (AMOLED, Deep Dark, Samsung, …). Used for Samsung One UI–style phone chrome. */
val LocalThemePreset = compositionLocalOf { PersonalizationStore.THEME_DEEP_DARK }

/** Contact list / call / message avatar clip (round, squircle, star, …). */
val LocalContactAvatarStyle = compositionLocalOf { PersonalizationStore.CONTACT_AVATAR_ROUND }

/** Dialpad and other controls: when false, suppress haptic feedback. */
val LocalHapticsEnabled = compositionLocalOf { true }

/** In-call active phase: show elapsed call duration when true. */
val LocalShowInCallTimer = compositionLocalOf { true }

/** Messages / search: show RCS capability pills when true. */
val LocalRcsBadgesEnabled = compositionLocalOf { true }

/** Money tab: mask digits in amounts for screenshots / shared devices. */
val LocalBlurMoneyAmounts = compositionLocalOf { false }

/**
 * Material 3 shape scale for components (cards, bars, sheets). See
 * [Design for Android](https://developer.android.com/design/ui).
 */
private val PhonIQShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

private fun shapesForPreset(preset: String): Shapes =
    when (preset) {
        PersonalizationStore.THEME_MATERIAL3 ->
            Shapes(
                extraSmall = RoundedCornerShape(6.dp),
                small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(24.dp),
                extraLarge = RoundedCornerShape(28.dp),
            )
        PersonalizationStore.THEME_PHONE_STYLE ->
            Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(14.dp),
                large = RoundedCornerShape(18.dp),
                extraLarge = RoundedCornerShape(22.dp),
            )
        else -> PhonIQShapes
    }

/** Default Material 3 type scale; font family applied in [PhonIQTheme]. */
private val PhonIQTypographyBase = Typography()

private data class DarkPalette(val background: Color, val surface: Color, val surfaceLow: Color)

private fun darkPaletteForPreset(preset: String, useAmoledBlack: Boolean): DarkPalette =
    when {
        useAmoledBlack ->
            DarkPalette(
                background = Color.Black,
                surface = Color(0xFF0A0A0A),
                surfaceLow = Color(0xFF050505),
            )
        preset == PersonalizationStore.THEME_DARK_NAVY ->
            DarkPalette(
                background = Color(0xFF080F1A),
                surface = Color(0xFF121E35),
                surfaceLow = Color(0xFF0C1628),
            )
        preset == PersonalizationStore.THEME_FOREST ->
            DarkPalette(
                background = Color(0xFF081208),
                surface = Color(0xFF152515),
                surfaceLow = Color(0xFF0A180A),
            )
        preset == PersonalizationStore.THEME_WINE ->
            DarkPalette(
                background = Color(0xFF120808),
                surface = Color(0xFF251515),
                surfaceLow = Color(0xFF180C0C),
            )
        preset == PersonalizationStore.THEME_SAMSUNG ->
            DarkPalette(
                background = Color(0xFF0C0D10),
                surface = Color(0xFF1E2229),
                surfaceLow = Color(0xFF14161C),
            )
        preset == PersonalizationStore.THEME_DAILY_UI_DIAL ->
            DarkPalette(
                background = Color(0xFF0A0812),
                surface = Color(0xFF161022),
                surfaceLow = Color(0xFF100C18),
            )
        preset == PersonalizationStore.THEME_NEO_MIRROR ->
            DarkPalette(
                background = Color(0xFF020306),
                surface = Color(0xFF0A1018),
                surfaceLow = Color(0xFF06090E),
            )
        preset == PersonalizationStore.THEME_DIALER_360 ->
            DarkPalette(
                background = Color(0xFF0C1629),
                surface = Color(0xFF152238),
                surfaceLow = Color(0xFF101B2E),
            )
        preset == PersonalizationStore.THEME_NOTHING_DIAL ->
            DarkPalette(
                background = Color(0xFF000000),
                surface = Color(0xFF141414),
                surfaceLow = Color(0xFF0A0A0A),
            )
        preset == PersonalizationStore.THEME_GLASS_DIAL ->
            DarkPalette(
                background = Color(0xFF282C3E),
                surface = Color(0xFF363B52),
                surfaceLow = Color(0xFF2F3448),
            )
        preset == PersonalizationStore.THEME_AI_TRANSLATOR ->
            DarkPalette(
                background = Color(0xFF141022),
                surface = Color(0xFF1E1A32),
                surfaceLow = Color(0xFF18122A),
            )
        preset == PersonalizationStore.THEME_SAAS_WIDGET ->
            DarkPalette(
                background = Color(0xFF1F2937),
                surface = Color(0xFF2D3748),
                surfaceLow = Color(0xFF252D3A),
            )
        preset == PersonalizationStore.THEME_MESSAGE_APP ->
            DarkPalette(
                background = Color(0xFF231F2E),
                surface = Color(0xFF322B40),
                surfaceLow = Color(0xFF2A2435),
            )
        preset == PersonalizationStore.THEME_MODERN_MESSAGING ->
            DarkPalette(
                background = Color(0xFF0D1117),
                surface = Color(0xFF161B22),
                surfaceLow = Color(0xFF12171E),
            )
        preset == PersonalizationStore.THEME_CONVERSATION_FLOW ->
            DarkPalette(
                background = Color(0xFF1A1D26),
                surface = Color(0xFF252A36),
                surfaceLow = Color(0xFF20242F),
            )
        preset == PersonalizationStore.THEME_MICRO_MOTION ->
            DarkPalette(
                background = Color(0xFF151018),
                surface = Color(0xFF252030),
                surfaceLow = Color(0xFF1E1828),
            )
        preset == PersonalizationStore.THEME_TEAL_TIDE ->
            DarkPalette(
                background = Color(0xFF0C1418),
                surface = Color(0xFF152A32),
                surfaceLow = Color(0xFF101C24),
            )
        preset == PersonalizationStore.THEME_INDIGO_LINE ->
            DarkPalette(
                background = Color(0xFF13141C),
                surface = Color(0xFF1C1E2E),
                surfaceLow = Color(0xFF17182A),
            )
        preset == PersonalizationStore.THEME_SKY_PANEL ->
            DarkPalette(
                background = Color(0xFF141416),
                surface = Color(0xFF1F1F24),
                surfaceLow = Color(0xFF1A1A1F),
            )
        preset == PersonalizationStore.THEME_VIOLET_STUDIO ->
            DarkPalette(
                background = Color(0xFF1A1025),
                surface = Color(0xFF261B35),
                surfaceLow = Color(0xFF20182E),
            )
        preset == PersonalizationStore.THEME_PHONE_STYLE ->
            DarkPalette(
                background = Color(0xFF000000),
                surface = Color(0xFF1C1C1E),
                surfaceLow = Color(0xFF2C2C2E),
            )
        preset == PersonalizationStore.THEME_MATERIAL3 ->
            DarkPalette(
                background = Color(0xFF10131A),
                surface = Color(0xFF1B1F2A),
                surfaceLow = Color(0xFF252A38),
            )
        else ->
            DarkPalette(
                background = PhoniqBackground,
                surface = PhoniqSurface,
                surfaceLow = PhoniqSurfaceLow,
            )
    }

internal fun fontFamilyForPersonalization(key: String): FontFamily =
    when (key) {
        "Mono" -> FontFamily.Monospace
        "Serif" -> FontFamily.Serif
        "System" -> FontFamily.SansSerif
        else -> FontFamily.Default
    }

internal fun fontTierMultiplier(tier: String): Float =
    when (tier) {
        "Small" -> 0.92f
        "Large" -> 1.08f
        "XL" -> 1.15f
        else -> 1f
    }

private fun Typography.withFontFamily(ff: FontFamily): Typography =
    Typography(
        displayLarge = displayLarge.copy(fontFamily = ff),
        displayMedium = displayMedium.copy(fontFamily = ff),
        displaySmall = displaySmall.copy(fontFamily = ff),
        headlineLarge = headlineLarge.copy(fontFamily = ff),
        headlineMedium = headlineMedium.copy(fontFamily = ff),
        headlineSmall = headlineSmall.copy(fontFamily = ff),
        titleLarge = titleLarge.copy(fontFamily = ff),
        titleMedium = titleMedium.copy(fontFamily = ff),
        titleSmall = titleSmall.copy(fontFamily = ff),
        bodyLarge = bodyLarge.copy(fontFamily = ff),
        bodyMedium = bodyMedium.copy(fontFamily = ff),
        bodySmall = bodySmall.copy(fontFamily = ff),
        labelLarge = labelLarge.copy(fontFamily = ff),
        labelMedium = labelMedium.copy(fontFamily = ff),
        labelSmall = labelSmall.copy(fontFamily = ff),
    )

private fun darkSchemeFor(
    accent: Color,
    palette: DarkPalette,
) =
    darkColorScheme(
        primary = accent,
        onPrimary = Color.White,
        background = palette.background,
        surface = palette.surface,
        surfaceContainerLow = palette.surfaceLow,
        onBackground = PhoniqOnBackground,
        onSurface = PhoniqOnBackground,
        secondary = PhoniqOnSurfaceMuted,
        onSecondary = palette.background,
        outline = PhoniqBorder,
        onSurfaceVariant = PhoniqOnSurfaceMuted,
    )

private fun lightSchemeFor(accent: Color) =
    lightColorScheme(
        primary = accent,
        onPrimary = Color.White,
        background = Color(0xFFF5F5FA),
        surface = Color(0xFFEEEEF5),
        surfaceContainerLow = Color(0xFFEEEEF5),
        onBackground = Color(0xFF1A1A24),
        onSurface = Color(0xFF1A1A24),
        secondary = Color(0xFF5C5C6E),
        onSecondary = Color.White,
        outline = Color(0xFFE0E0EA),
        onSurfaceVariant = Color(0xFF5C5C6E),
    )

@Composable
fun PhonIQTheme(
    darkTheme: Boolean = true,
    themePreset: String = PersonalizationStore.THEME_DEEP_DARK,
    accentArgb: Long = PersonalizationStore.DEFAULT_ACCENT_ARGB,
    useAmoledBlack: Boolean = false,
    denseThreads: Boolean = true,
    materialYou: Boolean = false,
    fontFamilyName: String = "Roboto",
    fontSizeTier: String = "Normal",
    hapticsEnabled: Boolean = true,
    showInCallTimer: Boolean = true,
    rcsBadgesEnabled: Boolean = true,
    blurMoneyAmounts: Boolean = false,
    contactAvatarStyle: String = PersonalizationStore.CONTACT_AVATAR_ROUND,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val accent = Color(accentArgb)
    val useAmoled = useAmoledBlack && darkTheme
    val palette = darkPaletteForPreset(themePreset, useAmoled)
    val colors =
        if (materialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val base =
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            val onPrimary = if (accent.luminance() > 0.5f) Color.Black else Color.White
            base.copy(primary = accent, onPrimary = onPrimary)
        } else if (darkTheme) {
            darkSchemeFor(accent, palette)
        } else {
            lightSchemeFor(accent)
        }

    val typography =
        remember(fontFamilyName) {
            PhonIQTypographyBase.withFontFamily(fontFamilyForPersonalization(fontFamilyName))
        }

    val shapes = remember(themePreset) { shapesForPreset(themePreset) }

    val density = LocalDensity.current
    val tierMul = fontTierMultiplier(fontSizeTier)
    val themedDensity =
        remember(density, tierMul) {
            Density(density.density, density.fontScale * tierMul)
        }

    CompositionLocalProvider(
        LocalDensity provides themedDensity,
        LocalThemePreset provides themePreset,
        LocalDenseThreads provides denseThreads,
        LocalHapticsEnabled provides hapticsEnabled,
        LocalShowInCallTimer provides showInCallTimer,
        LocalRcsBadgesEnabled provides rcsBadgesEnabled,
        LocalBlurMoneyAmounts provides blurMoneyAmounts,
        LocalContactAvatarStyle provides contactAvatarStyle,
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = typography,
            shapes = shapes,
            content = content,
        )
    }
}
