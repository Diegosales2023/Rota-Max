package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    secondary = BrandBlueLight,
    onSecondary = Color.White,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = StatusSkipped,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme by default to match Circuit's signature dark UI
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // We enforce our custom premium dark theme to maintain the exact look of Circuit
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
