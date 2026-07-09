package com.neosparkx.expensetracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.toArgb
import android.app.Activity
import androidx.core.view.WindowCompat

import androidx.compose.runtime.CompositionLocalProvider

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryAccent,
    secondary = LightAppColors.textSecondary,
    tertiary = LightAppColors.cardSurface,
    background = LightAppColors.background,
    surface = LightAppColors.cardSurface,
    error = Color(0xFFEA3B35),
    onPrimary = Color.White,
    onBackground = LightAppColors.textPrimary,
    onSurface = LightAppColors.textPrimary
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryAccent,
    secondary = DarkAppColors.textSecondary,
    tertiary = DarkAppColors.cardSurface,
    background = DarkAppColors.background,
    surface = DarkAppColors.cardSurface,
    error = Color(0xFFEA3B35),
    onPrimary = Color.White,
    onBackground = DarkAppColors.textPrimary,
    onSurface = DarkAppColors.textPrimary
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        window.statusBarColor = Color.Transparent.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
      }
    }
  }

  val colors = if (darkTheme) DarkAppColors else LightAppColors
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  CompositionLocalProvider(LocalAppColors provides colors) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}


