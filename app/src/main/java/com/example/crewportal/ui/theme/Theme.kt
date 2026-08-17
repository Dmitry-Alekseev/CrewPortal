package com.example.crewportal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = CorporateBlue,
    onPrimary = Color.White,
    primaryContainer = CorporateBlueLight,
    onPrimaryContainer = Color(0xFF10263E),
    secondary = CorporateGraphite,
    onSecondary = Color.White,
    background = PortalBackground,
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
    background = PortalDarkBackground,
    onBackground = Color(0xFFE8EDF2),
    surface = Color(0xFF121A23),
    onSurface = Color(0xFFE8EDF2),
    surfaceVariant = Color(0xFF202633),
    onSurfaceVariant = Color(0xFFC3CCD5),
    tertiary = Color(0xFF8FB6D9),
    tertiaryContainer = Color(0xFF203B55)
)

private val PortalShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(13.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(34.dp)
)

private val PortalTypography = Typography(
    headlineLarge = Typography().headlineLarge.copy(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineMedium = Typography().headlineMedium.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    titleLarge = Typography().titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = Typography().bodyLarge.copy(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = Typography().bodyMedium.copy(lineHeight = 20.sp),
    labelLarge = Typography().labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
)

@Composable
fun CrewPortalTheme(darkTheme: Boolean? = null, content: @Composable () -> Unit) {
    val colors = if (darkTheme ?: isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        shapes = PortalShapes,
        typography = PortalTypography,
        content = content
    )
}
