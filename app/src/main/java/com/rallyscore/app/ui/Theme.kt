package com.rallyscore.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.rallyscore.app.settings.ThemeMode

data class Palette(
    val isDark: Boolean,
    val background: Color,
    val panel: Color,
    val panelBorder: Color,
    val digits: Color,
    val muted: Color,
    val icon: Color,
    val accent: Color,
    val win: Color,
)

// Pure black so AMOLED pixels are fully off; digits are a soft off-white to reduce wear.
val DarkPalette = Palette(
    isDark = true,
    background = Color.Black,
    panel = Color.Black,
    panelBorder = Color(0xFF1A1C20),
    digits = Color(0xFFD9DEE6),
    muted = Color(0xFF7A808A),
    icon = Color(0xFF9AA1AB),
    accent = Color(0xFF7FB2FF),
    win = Color(0xFF6EE7A0),
)

val LightPalette = Palette(
    isDark = false,
    background = Color(0xFFF2F3F5),
    panel = Color.White,
    panelBorder = Color(0xFFDADDE2),
    digits = Color(0xFF15171A),
    muted = Color(0xFF5F6670),
    icon = Color(0xFF3D434B),
    accent = Color(0xFF2563EB),
    win = Color(0xFF15803D),
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

@Composable
fun RallyTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val scheme = if (dark) {
        darkColorScheme(
            primary = Color(0xFF7FB2FF),
            background = Color.Black,
            surface = Color.Black,
        )
    } else {
        lightColorScheme(primary = Color(0xFF2563EB))
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalPalette provides if (dark) DarkPalette else LightPalette, content = content)
    }
}
