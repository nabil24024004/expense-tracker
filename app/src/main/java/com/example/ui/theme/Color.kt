package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Dynamic theme color configurations
data class AppColors(
    val background: Color,
    val cardSurface: Color,
    val darkCardSurface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val darkCardTextPrimary: Color,
    val darkCardTextSecondary: Color,
    val iconBackground: Color,
    val darkCardIconBackground: Color,
    val chartOrange: Color,
    val chartPurple: Color
)

val LightAppColors = AppColors(
    background = Color(0xFFF3F3F1),
    cardSurface = Color(0xFFE5E5E2),
    darkCardSurface = Color(0xFF020203),
    textPrimary = Color(0xFF020203),
    textSecondary = Color(0xFF767677),
    darkCardTextPrimary = Color(0xFFF3F3F1),
    darkCardTextSecondary = Color(0xFFA1A1A4),
    iconBackground = Color(0xFFE5E5E2),
    darkCardIconBackground = Color(0xFF2E2E33),
    chartOrange = Color(0xFF020203),
    chartPurple = Color(0xFFE5E5E2)
)

val DarkAppColors = AppColors(
    background = Color(0xFF0C0C0E),            // Charcoal black variant background
    cardSurface = Color(0xFF18181B),           // Dark gray card surface
    darkCardSurface = Color(0xFF242428),       // Slightly lighter dark gray surface
    textPrimary = Color(0xFFFFFFFF),           // Pure white text for dark mode
    textSecondary = Color(0xFFA1A1A4),         // Clear readable secondary gray text
    darkCardTextPrimary = Color(0xFFFFFFFF),   // White text on dark cards
    darkCardTextSecondary = Color(0xFFC7C7CC), // Muted white text on dark cards
    iconBackground = Color(0xFF242428),        // Muted dark icon backgrounds
    darkCardIconBackground = Color(0xFF323238),// Lighter dark icon backgrounds
    chartOrange = Color(0xFFFFFFFF),           // Adapt chart orange color to white in dark mode
    chartPurple = Color(0xFF18181B)            // Adapt chart purple color to dark gray in dark mode
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

val ThemeBackground: Color
    @Composable
    get() = LocalAppColors.current.background

val CardSurface: Color
    @Composable
    get() = LocalAppColors.current.cardSurface

val DarkCardSurface: Color
    @Composable
    get() = LocalAppColors.current.darkCardSurface

val TextPrimary: Color
    @Composable
    get() = LocalAppColors.current.textPrimary

val TextSecondary: Color
    @Composable
    get() = LocalAppColors.current.textSecondary

val DarkCardTextPrimary: Color
    @Composable
    get() = LocalAppColors.current.darkCardTextPrimary

val DarkCardTextSecondary: Color
    @Composable
    get() = LocalAppColors.current.darkCardTextSecondary

val IconBackground: Color
    @Composable
    get() = LocalAppColors.current.iconBackground

val DarkCardIconBackground: Color
    @Composable
    get() = LocalAppColors.current.darkCardIconBackground

val LiquidAccentStart = Color(0xFFEA3B35)
val LiquidAccentEnd = Color(0xFFEA3B35)

val ChartGreen = Color(0xFFEA3B35)
val ChartOrange: Color
    @Composable
    get() = LocalAppColors.current.chartOrange

val ChartBlue = Color(0xFFEA3B35)
val ChartPink = Color(0xFF767677)

val ChartPurple: Color
    @Composable
    get() = LocalAppColors.current.chartPurple

val PrimaryAccent = Color(0xFFEA3B35)

