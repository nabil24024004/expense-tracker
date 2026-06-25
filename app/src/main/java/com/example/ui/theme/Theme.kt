package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun MyApplicationTheme(
    themeSelection: String = "Classic",
    isDarkMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    val appColors = resolveAppColors(themeSelection, isDarkMode)

    val colorScheme = if (appColors.isDark) {
        darkColorScheme(
            primary = appColors.primaryAccent,
            secondary = appColors.textSecondary,
            tertiary = appColors.cardSurface,
            background = appColors.themeBackground,
            surface = appColors.cardSurface,
            error = Color(0xFFEA3B35),
            onPrimary = Color.White,
            onBackground = appColors.textPrimary,
            onSurface = appColors.textPrimary
        )
    } else {
        lightColorScheme(
            primary = appColors.primaryAccent,
            secondary = appColors.textSecondary,
            tertiary = appColors.cardSurface,
            background = appColors.themeBackground,
            surface = appColors.cardSurface,
            error = Color(0xFFEA3B35),
            onPrimary = Color.White,
            onBackground = appColors.textPrimary,
            onSurface = appColors.textPrimary
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !appColors.isDark
            }
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}
