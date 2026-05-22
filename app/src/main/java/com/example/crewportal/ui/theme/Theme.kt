package com.example.crewportal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = ThaiPurple,
    onPrimary = Color.White,
    secondary = ThaiPurple,
    background = Color(0xFFF7F5FA),
    onBackground = Color(0xFF1D1B20),
    surface = Color.White,
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFF0EAF7),
    onSurfaceVariant = Color(0xFF5F6270)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE1C4FF),
    onPrimary = Color(0xFF1D1028),
    secondary = Color(0xFFE1C4FF),
    background = Color(0xFF100C16),
    onBackground = Color(0xFFF6EFFA),
    surface = Color(0xFF21182C),
    onSurface = Color(0xFFF6EFFA),
    surfaceVariant = Color(0xFF302540),
    onSurfaceVariant = Color(0xFFD7CDE0)
)

@Composable
fun CrewPortalTheme(darkTheme: Boolean? = null, content: @Composable () -> Unit) {
    val colors = if (darkTheme ?: isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
