package com.example.crewportal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = CorporateBlue,
    onPrimary = Color.White,
    primaryContainer = CorporateBlueLight,
    onPrimaryContainer = Color(0xFF10263E),
    secondary = CorporateGraphite,
    onSecondary = Color.White,
    background = Color(0xFFF4F6F8),
    onBackground = Color(0xFF17202A),
    surface = Color.White,
    onSurface = Color(0xFF17202A),
    surfaceVariant = CorporateNeutral,
    onSurfaceVariant = Color(0xFF4F5B67),
    tertiary = CorporateBlueAccent,
    tertiaryContainer = Color(0xFFDCE7F0)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C7E8),
    onPrimary = Color(0xFF0D1B2A),
    primaryContainer = Color(0xFF18314F),
    onPrimaryContainer = Color(0xFFDCE9F6),
    secondary = Color(0xFFB8C2CC),
    onSecondary = Color(0xFF18212A),
    background = Color(0xFF0B1118),
    onBackground = Color(0xFFE8EDF2),
    surface = Color(0xFF121A23),
    onSurface = Color(0xFFE8EDF2),
    surfaceVariant = Color(0xFF202633),
    onSurfaceVariant = Color(0xFFC3CCD5),
    tertiary = Color(0xFF8FB6D9),
    tertiaryContainer = Color(0xFF203B55)
)

@Composable
fun CrewPortalTheme(darkTheme: Boolean? = null, content: @Composable () -> Unit) {
    val colors = if (darkTheme ?: isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
