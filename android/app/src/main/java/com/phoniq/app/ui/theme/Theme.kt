package com.phoniq.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors =
    darkColorScheme(
        primary = PhoniqAccent,
        onPrimary = Color.White,
        background = PhoniqBackground,
        surface = PhoniqSurface,
        onBackground = PhoniqOnBackground,
        onSurface = PhoniqOnBackground,
        secondary = PhoniqOnSurfaceMuted,
        onSecondary = PhoniqBackground,
    )

@Composable
fun PhonIQTheme(content: @Composable () -> Unit) {
    // v0.1 shell: lock to mockup-style dark; system light/dark + Personalization later.
    MaterialTheme(colorScheme = DarkColors, content = content)
}
