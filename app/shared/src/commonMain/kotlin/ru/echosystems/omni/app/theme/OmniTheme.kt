package ru.echosystems.omni.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC5BCFF), onPrimary = Color(0xFF2C235C),
    primaryContainer = Color(0xFF433A76), onPrimaryContainer = Color(0xFFE6DFFF),
    secondary = Color(0xFFCBC3DE), background = Color(0xFF17151F),
    surface = Color(0xFF201D2A), surfaceVariant = Color(0xFF2C283B),
    onSurfaceVariant = Color(0xFFCDC5DB), outlineVariant = Color(0xFF494357),
    error = Color(0xFFFFB4AB)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5346A0), onPrimary = Color.White,
    primaryContainer = Color(0xFFDBD4FF), onPrimaryContainer = Color(0xFF23184E),
    secondary = Color(0xFF5D5674), background = Color(0xFFF7F5FC),
    surface = Color(0xFFFEFCFF), onSurface = Color(0xFF201C2B),
    surfaceVariant = Color(0xFFECE8FA), onSurfaceVariant = Color(0xFF494258),
    outlineVariant = Color(0xFFDCD5EB), error = Color(0xFFBA1A1A)
)

@Composable
fun OmniTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
