package com.phoniq.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors =
    darkColorScheme(
        primary = PhoniqAccent,
        onPrimary = Color.White,
        background = PhoniqBackground,
        surface = PhoniqSurface,
        surfaceContainerLow = PhoniqSurfaceLow,
        onBackground = PhoniqOnBackground,
        onSurface = PhoniqOnBackground,
        secondary = PhoniqOnSurfaceMuted,
        onSecondary = PhoniqBackground,
        outline = PhoniqBorder,
        onSurfaceVariant = PhoniqOnSurfaceMuted,
    )

/** Light tokens aligned with `phoniq-mockup-v1.html` `#phoniq-screen.theme-light`. */
private val LightColors =
    lightColorScheme(
        primary = PhoniqAccent,
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
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
