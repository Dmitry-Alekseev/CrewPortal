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
    secondary = GoldAccent,
    background = Color(0xFFF5F2F8),
    onBackground = Color(0xFF17131D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17131D),
    surfaceVariant = Color(0xFFECE5F3),
    onSurfaceVariant = Color(0xFF52505A),
    tertiary = Color(0xFF7C4D00)
)

private val DarkColors = darkColorScheme(
    primary = GoldAccent,
    onPrimary = Color(0xFF171008),
    secondary = Color(0xFFB89CFF),
    background = Color(0xFF0B0D12),
    onBackground = Color(0xFFF4F1F8),
    surface = Color(0xFF161A22),
    onSurface = Color(0xFFF4F1F8),
    surfaceVariant = Color(0xFF222835),
    onSurfaceVariant = Color(0xFFD8D3E1),
    tertiary = Color(0xFFE9C86E)
)

@Composable
fun CrewPortalTheme(darkTheme: Boolean? = null, content: @Composable () -> Unit) {
    val colors = if (darkTheme ?: isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
