package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = IdeCyan,
    onPrimary = Color(0xFF003548),
    primaryContainer = Color(0xFF004D67),
    onPrimaryContainer = Color(0xFFBEE9FF),
    secondary = IdePurple,
    onSecondary = Color(0xFF381E72),
    secondaryContainer = Color(0xFF4F378B),
    onSecondaryContainer = Color(0xFFE8DDFF),
    tertiary = IdeGreen,
    onTertiary = Color(0xFF003915),
    background = IdeBackgroundDark,
    onBackground = IdeTextPrimary,
    surface = IdeSurfaceDark,
    onSurface = IdeTextPrimary,
    surfaceVariant = IdeSurfaceVariantDark,
    onSurfaceVariant = IdeTextSecondary,
    outline = IdeBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBAE6FD),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDD6FE),
    onSecondaryContainer = Color(0xFF5B21B6),
    tertiary = Color(0xFF16A34A),
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun DroidIdeTheme(
    darkTheme: Boolean = true, // Default to sleek developer dark mode
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
