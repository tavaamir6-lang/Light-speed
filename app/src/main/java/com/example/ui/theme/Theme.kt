package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonEmerald,
    onPrimary = CyberBlack,
    primaryContainer = CyberDarkSurface,
    onPrimaryContainer = NeonEmerald,
    secondary = NeonCyan,
    onSecondary = CyberBlack,
    secondaryContainer = CyberCardSurface,
    onSecondaryContainer = NeonCyan,
    tertiary = ElectricViolet,
    background = CyberBlack,
    onBackground = TextPrimary,
    surface = CyberDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberCardSurface,
    onSurfaceVariant = TextSecondary,
    outline = CyberCardBorder,
    error = DangerRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
