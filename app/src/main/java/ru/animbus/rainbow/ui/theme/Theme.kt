package com.example.colorscience.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF1A2A6C),
    secondary = Color(0xFF2C3E50),
    background = Color(0xFF121212)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3498DB),
    secondary = Color(0xFF2C3E50),
    background = Color(0xFFF5F5F5)
)

@Composable
fun ColorScienceAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}