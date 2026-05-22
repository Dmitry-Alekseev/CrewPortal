package com.example.crewportal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = ThaiPurple,
    secondary = ThaiPurple,
    background = Color(0xFFF7F5FA),
    surface = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD7B7FF),
    secondary = Color(0xFFD7B7FF),
    background = Color(0xFF121018),
    surface = Color(0xFF211B2D)
)

@Composable
fun CrewPortalTheme(darkTheme: Boolean? = null, content: @Composable () -> Unit) {
    val colors = if (darkTheme ?: isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
