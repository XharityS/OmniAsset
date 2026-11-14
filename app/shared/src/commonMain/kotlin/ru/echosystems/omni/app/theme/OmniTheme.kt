package ru.echosystems.omni.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FC7FF), onPrimary = Color(0xFF003352),
    secondary = Color(0xFFB7C9E4), background = Color(0xFF0B111A),
    surface = Color(0xFF111A26), surfaceVariant = Color(0xFF1E2A39), error = Color(0xFFFFB4AB)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF00639A), onPrimary = Color.White,
    secondary = Color(0xFF4E6075), background = Color(0xFFF4F7FB),
    surface = Color.White, surfaceVariant = Color(0xFFE2EAF3), error = Color(0xFFBA1A1A)
)

@Composable
fun OmniTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
