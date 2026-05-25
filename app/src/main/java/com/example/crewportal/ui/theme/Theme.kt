package com.example.crewportal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF4A246F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9D9FF),
    onPrimaryContainer = Color(0xFF221034),
    secondary = GoldAccent,
    onSecondary = Color(0xFF211400),
    background = Color(0xFFF7F4EF),
    onBackground = Color(0xFF16121C),
    surface = Color(0xFFFFFCF7),
    onSurface = Color(0xFF16121C),
    surfaceVariant = Color(0xFFEDE7DD),
    onSurfaceVariant = Color(0xFF554E5F),
    tertiary = Color(0xFF006D77),
    tertiaryContainer = Color(0xFFD7F4F2)
)

private val DarkColors = darkColorScheme(
    primary = GoldAccent,
    onPrimary = Color(0xFF1F1600),
    primaryContainer = Color(0xFF3C2E13),
    onPrimaryContainer = Color(0xFFFFE4A3),
    secondary = Color(0xFFBFA6FF),
    onSecondary = Color(0xFF1F1236),
    background = Color(0xFF080A0F),
    onBackground = Color(0xFFF2EEF7),
    surface = Color(0xFF121721),
    onSurface = Color(0xFFF2EEF7),
    surfaceVariant = Color(0xFF202633),
    onSurfaceVariant = Color(0xFFD8D2E2),
    tertiary = Color(0xFF8EE8DD),
    tertiaryContainer = Color(0xFF163A3D)
)

@Composable
fun CrewPortalTheme(darkTheme: Boolean? = null, content: @Composable () -> Unit) {
    val colors = if (darkTheme ?: isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
