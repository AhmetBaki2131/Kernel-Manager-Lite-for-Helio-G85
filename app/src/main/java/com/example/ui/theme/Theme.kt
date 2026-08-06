package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ImmersivePrimary,
    onPrimary = ImmersiveOnPrimary,
    primaryContainer = ImmersivePrimaryContainer,
    onPrimaryContainer = ImmersiveOnPrimaryContainer,
    secondary = ActiveRootGreenText,
    onSecondary = Color(0xFF003915),
    secondaryContainer = ActiveRootGreenBg,
    onSecondaryContainer = ActiveRootGreenText,
    tertiary = Color(0xFFC0C6DC),
    background = ImmersiveBackground,
    onBackground = ImmersiveTextPrimary,
    surface = ImmersiveSurface,
    onSurface = ImmersiveTextPrimary,
    surfaceVariant = ImmersiveSurfaceVariant,
    onSurfaceVariant = ImmersiveTextSecondary,
    outline = ImmersiveBorder
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF005AC1),
    onPrimary = Color.White,
    primaryContainer = ImmersivePrimaryContainer,
    onPrimaryContainer = ImmersiveOnPrimaryContainer,
    secondary = Color(0xFF006C35),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB4EBB0),
    onSecondaryContainer = Color(0xFF00210B),
    tertiary = Color(0xFF535F70),
    background = Color(0xFFF9F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color.White,
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF74777F)
)

@Composable
fun KernelManagerTheme(
    darkTheme: Boolean = true, // Default to Dark / Immersive UI theme
    dynamicColor: Boolean = false, // Use our handcrafted Immersive UI palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
