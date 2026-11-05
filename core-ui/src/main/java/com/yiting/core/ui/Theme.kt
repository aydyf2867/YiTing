package com.yiting.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val YiTingRed = Color(0xFFFA2D48)

private val LightColors = lightColorScheme(
    primary = YiTingRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE1E5),
    onPrimaryContainer = Color(0xFF4F0010),
    secondary = Color(0xFF77565A),
    background = Color.White,
    surface = Color.White,
    onBackground = Color(0xFF201A1B),
    onSurface = Color(0xFF201A1B)
)

private val DarkColors = darkColorScheme(
    primary = YiTingRed,
    onPrimary = Color.White,
    background = Color(0xFF1A1113),
    surface = Color(0xFF1A1113),
    onBackground = Color(0xFFF0DEDF),
    onSurface = Color(0xFFF0DEDF)
)

val YiTingTypography = Typography(
    displayLarge = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold),
    displayMedium = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 20.sp),
    bodyMedium = TextStyle(fontSize = 20.sp),
    bodySmall = TextStyle(fontSize = 20.sp),
    labelLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 18.sp),
    labelSmall = TextStyle(fontSize = 18.sp)
)

@Composable
fun YiTingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = YiTingTypography,
        content = content
    )
}
