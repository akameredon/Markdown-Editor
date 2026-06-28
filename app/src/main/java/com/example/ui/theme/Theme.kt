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

private val DarkColorScheme =
  darkColorScheme(
    primary = ImmersivePrimary,
    onPrimary = ImmersiveOnPrimary,
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = ImmersiveSecondary,
    onSecondary = Color(0xFF1A1C1E),
    secondaryContainer = ImmersiveSurfaceVariant,
    onSecondaryContainer = ImmersiveOnBackground,
    tertiary = ImmersiveTertiary,
    onTertiary = ImmersiveOnTertiary,
    background = ImmersiveBackground,
    onBackground = ImmersiveOnBackground,
    surface = ImmersiveSurface,
    onSurface = ImmersiveOnSurface,
    surfaceVariant = ImmersiveSurfaceVariant,
    onSurfaceVariant = ImmersiveOnSurfaceVariant,
    outline = ImmersiveOutline
  )

private val LightColorScheme = DarkColorScheme // Keep it consistent for an immersive markdown environment

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark mode to align with the "Immersive UI" deep dark/charcoal design
  dynamicColor: Boolean = false, // Disable dynamic colors to ensure the bespoke "Immersive UI" palette is strictly applied
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

