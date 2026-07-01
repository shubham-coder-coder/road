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

private val LightColorScheme =
  lightColorScheme(
    primary = PurpleMain,
    secondary = PurpleNavIndicator,
    tertiary = BlueGpsBg,
    background = CreamBg,
    surface = Color.White,
    onBackground = CharcoalText,
    onSurface = CharcoalText,
    onPrimary = Color.White,
    onSecondary = CharcoalDark,
    surfaceVariant = PurpleNavBg,
    outline = BorderGray
  )

private val DarkColorScheme = LightColorScheme // Keep light-theme aesthetic for consistent professional polish

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Enforce our custom theme colors
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
