package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SenseiIndigo,
    onPrimary = Color.White,
    primaryContainer = SenseiPurple,
    onPrimaryContainer = Color.White,
    secondary = SenseiPink,
    onSecondary = Color.White,
    tertiary = SenseiGold,
    background = SenseiDarkBg,
    onBackground = Color.White,
    surface = SenseiSurface,
    onSurface = Color.White,
    surfaceVariant = SenseiCard,
    onSurfaceVariant = Color(0xFFA5B4FC)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
