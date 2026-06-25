package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ─── Semantic Color Token Container ──────────────────────────────────────────

data class AppColors(
    val themeBackground: Color,
    val cardSurface: Color,
    val darkCardSurface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val darkCardTextPrimary: Color,
    val darkCardTextSecondary: Color,
    val iconBackground: Color,
    val darkCardIconBackground: Color,
    val primaryAccent: Color,
    val liquidAccentStart: Color,
    val liquidAccentEnd: Color,
    val chartGreen: Color,
    val chartOrange: Color,
    val chartBlue: Color,
    val chartPink: Color,
    val chartPurple: Color,
    val isDark: Boolean
)

// ─── CompositionLocal ────────────────────────────────────────────────────────

val LocalAppColors = staticCompositionLocalOf { ClassicLightPalette }

// ─── Classic Theme (Red Accent) ──────────────────────────────────────────────

val ClassicLightPalette = AppColors(
    themeBackground = Color(0xFFFFF5F2),
    cardSurface = Color(0xFFFFE0D5),
    darkCardSurface = Color(0xFF161E2F),
    textPrimary = Color(0xFF161E2F),
    textSecondary = Color(0xFF541A2E),
    darkCardTextPrimary = Color(0xFFFFA586),
    darkCardTextSecondary = Color(0xFF384358),
    iconBackground = Color(0xFFFFE0D5),
    darkCardIconBackground = Color(0xFF242F49),
    primaryAccent = Color(0xFFB51A28),
    liquidAccentStart = Color(0xFFB51A28),
    liquidAccentEnd = Color(0xFFB51A28),
    chartGreen = Color(0xFFB51A28),
    chartOrange = Color(0xFFFFA586),
    chartBlue = Color(0xFF384358),
    chartPink = Color(0xFF541A2E),
    chartPurple = Color(0xFF242F49),
    isDark = false
)

val ClassicDarkPalette = AppColors(
    themeBackground = Color(0xFF161E2F),
    cardSurface = Color(0xFF242F49),
    darkCardSurface = Color(0xFF541A2E),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB0B0B0),
    darkCardTextPrimary = Color(0xFFFFFFFF),
    darkCardTextSecondary = Color(0xFFCCCCCC),
    iconBackground = Color(0xFF242F49),
    darkCardIconBackground = Color(0xFF541A2E),
    primaryAccent = Color(0xFFB51A28),
    liquidAccentStart = Color(0xFFB51A28),
    liquidAccentEnd = Color(0xFFB51A28),
    chartGreen = Color(0xFFB51A28),
    chartOrange = Color(0xFFFFA586),
    chartBlue = Color(0xFF384358),
    chartPink = Color(0xFF541A2E),
    chartPurple = Color(0xFF242F49),
    isDark = true
)

// ─── Blue/Black Theme ────────────────────────────────────────────────────────

val BlueBlackLightPalette = AppColors(
    themeBackground = Color(0xFFC1E8FF),
    cardSurface = Color(0xFFE6F4FE),
    darkCardSurface = Color(0xFF021024),
    textPrimary = Color(0xFF021024),
    textSecondary = Color(0xFF052659),
    darkCardTextPrimary = Color(0xFFC1E8FF),
    darkCardTextSecondary = Color(0xFF7DA0CA),
    iconBackground = Color(0xFFE6F4FE),
    darkCardIconBackground = Color(0xFF052659),
    primaryAccent = Color(0xFF5483B3),
    liquidAccentStart = Color(0xFF7DA0CA),
    liquidAccentEnd = Color(0xFF5483B3),
    chartGreen = Color(0xFF5483B3),
    chartOrange = Color(0xFF021024),
    chartBlue = Color(0xFF5483B3),
    chartPink = Color(0xFF7DA0CA),
    chartPurple = Color(0xFFE6F4FE),
    isDark = false
)

val BlueBlackDarkPalette = AppColors(
    themeBackground = Color(0xFF021024),
    cardSurface = Color(0xFF052659),
    darkCardSurface = Color(0xFF021024),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB0B0B0),
    darkCardTextPrimary = Color(0xFFFFFFFF),
    darkCardTextSecondary = Color(0xFFCCCCCC),
    iconBackground = Color(0xFF052659),
    darkCardIconBackground = Color(0xFF5483B3),
    primaryAccent = Color(0xFF5483B3),
    liquidAccentStart = Color(0xFF7DA0CA),
    liquidAccentEnd = Color(0xFF5483B3),
    chartGreen = Color(0xFF5483B3),
    chartOrange = Color(0xFFC1E8FF),
    chartBlue = Color(0xFF5483B3),
    chartPink = Color(0xFF7DA0CA),
    chartPurple = Color(0xFF052659),
    isDark = true
)


// ─── Green Theme ─────────────────────────────────────────────────────────────

val LightGreenLightPalette = AppColors(
    themeBackground = Color(0xFFF4F7F4),
    cardSurface = Color(0xFFD5DDDF),
    darkCardSurface = Color(0xFF1B2727),
    textPrimary = Color(0xFF1B2727),
    textSecondary = Color(0xFF3C5148),
    darkCardTextPrimary = Color(0xFFD5DDDF),
    darkCardTextSecondary = Color(0xFFB2C582),
    iconBackground = Color(0xFFD5DDDF),
    darkCardIconBackground = Color(0xFF3C5148),
    primaryAccent = Color(0xFF688E4E),
    liquidAccentStart = Color(0xFF688E4E),
    liquidAccentEnd = Color(0xFF688E4E),
    chartGreen = Color(0xFF688E4E),
    chartOrange = Color(0xFFB2C582),
    chartBlue = Color(0xFF3C5148),
    chartPink = Color(0xFFD5DDDF),
    chartPurple = Color(0xFF1B2727),
    isDark = false
)

val LightGreenDarkPalette = AppColors(
    themeBackground = Color(0xFF1B2727),
    cardSurface = Color(0xFF3C5148),
    darkCardSurface = Color(0xFF1B2727),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB0B0B0),
    darkCardTextPrimary = Color(0xFFFFFFFF),
    darkCardTextSecondary = Color(0xFFCCCCCC),
    iconBackground = Color(0xFF3C5148),
    darkCardIconBackground = Color(0xFF1B2727),
    primaryAccent = Color(0xFF688E4E),
    liquidAccentStart = Color(0xFF688E4E),
    liquidAccentEnd = Color(0xFF688E4E),
    chartGreen = Color(0xFF688E4E),
    chartOrange = Color(0xFFB2C582),
    chartBlue = Color(0xFF3C5148),
    chartPink = Color(0xFFD5DDDF),
    chartPurple = Color(0xFF1B2727),
    isDark = true
)

// ─── Sunset Theme (Replacing Light Yellow) ──────────────────────────────────

val LightYellowLightPalette = AppColors(
    themeBackground = Color(0xFFFFF5ED),
    cardSurface = Color(0xFFFFE0CC),
    darkCardSurface = Color(0xFF1D1A39),
    textPrimary = Color(0xFF1D1A39),
    textSecondary = Color(0xFF613159),
    darkCardTextPrimary = Color(0xFFFFFFFF),
    darkCardTextSecondary = Color(0xFFCCCCCC),
    iconBackground = Color(0xFFFFE0CC),
    darkCardIconBackground = Color(0xFF613159),
    primaryAccent = Color(0xFFA74D65),
    liquidAccentStart = Color(0xFFDF7862),
    liquidAccentEnd = Color(0xFFA74D65),
    chartGreen = Color(0xFFA74D65),
    chartOrange = Color(0xFFFDB45C),
    chartBlue = Color(0xFF613159),
    chartPink = Color(0xFFDF7862),
    chartPurple = Color(0xFF1D1A39),
    isDark = false
)

val LightYellowDarkPalette = AppColors(
    themeBackground = Color(0xFF1D1A39),
    cardSurface = Color(0xFF613159),
    darkCardSurface = Color(0xFF1D1A39),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB0B0B0),
    darkCardTextPrimary = Color(0xFFFFFFFF),
    darkCardTextSecondary = Color(0xFFCCCCCC),
    iconBackground = Color(0xFF613159),
    darkCardIconBackground = Color(0xFF1D1A39),
    primaryAccent = Color(0xFFDF7862),
    liquidAccentStart = Color(0xFFFDB45C),
    liquidAccentEnd = Color(0xFFDF7862),
    chartGreen = Color(0xFFDF7862),
    chartOrange = Color(0xFFFDB45C),
    chartBlue = Color(0xFF613159),
    chartPink = Color(0xFFA74D65),
    chartPurple = Color(0xFF1D1A39),
    isDark = true
)

// ─── Purple Theme (Replacing Light Blue) ─────────────────────────────────────

val PurpleLightPalette = AppColors(
    themeBackground = Color(0xFFFDFBFE),
    cardSurface = Color(0xFFE7DBEF),
    darkCardSurface = Color(0xFF49225B),
    textPrimary = Color(0xFF49225B),
    textSecondary = Color(0xFF6E3482),
    darkCardTextPrimary = Color(0xFFF5EBFA),
    darkCardTextSecondary = Color(0xFFE7DBEF),
    iconBackground = Color(0xFFE7DBEF),
    darkCardIconBackground = Color(0xFF6E3482),
    primaryAccent = Color(0xFFA56ABD),
    liquidAccentStart = Color(0xFFA56ABD),
    liquidAccentEnd = Color(0xFFA56ABD),
    chartGreen = Color(0xFFA56ABD),
    chartOrange = Color(0xFFE7DBEF),
    chartBlue = Color(0xFF6E3482),
    chartPink = Color(0xFFF5EBFA),
    chartPurple = Color(0xFF49225B),
    isDark = false
)

val PurpleDarkPalette = AppColors(
    themeBackground = Color(0xFF49225B),
    cardSurface = Color(0xFF6E3482),
    darkCardSurface = Color(0xFF49225B),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB0B0B0),
    darkCardTextPrimary = Color(0xFFFFFFFF),
    darkCardTextSecondary = Color(0xFFCCCCCC),
    iconBackground = Color(0xFF6E3482),
    darkCardIconBackground = Color(0xFF49225B),
    primaryAccent = Color(0xFFA56ABD),
    liquidAccentStart = Color(0xFFA56ABD),
    liquidAccentEnd = Color(0xFFA56ABD),
    chartGreen = Color(0xFFA56ABD),
    chartOrange = Color(0xFFE7DBEF),
    chartBlue = Color(0xFF6E3482),
    chartPink = Color(0xFFF5EBFA),
    chartPurple = Color(0xFF49225B),
    isDark = true
)

// ─── Pink Theme ──────────────────────────────────────────────────────────────

val PinkLightPalette = AppColors(
    themeBackground = Color(0xFFF7F5F8),
    cardSurface = Color(0xFFDCD7D5),
    darkCardSurface = Color(0xFF4B3F6E),
    textPrimary = Color(0xFF4B3F6E),
    textSecondary = Color(0xFF6C5F8D),
    darkCardTextPrimary = Color(0xFFDCD7D5),
    darkCardTextSecondary = Color(0xFFBA96C1),
    iconBackground = Color(0xFFDCD7D5),
    darkCardIconBackground = Color(0xFF6C5F8D),
    primaryAccent = Color(0xFFBA96C1),
    liquidAccentStart = Color(0xFFBA96C1),
    liquidAccentEnd = Color(0xFFBA96C1),
    chartGreen = Color(0xFFBA96C1),
    chartOrange = Color(0xFF9C8CB9),
    chartBlue = Color(0xFF6C5F8D),
    chartPink = Color(0xFFDCD7D5),
    chartPurple = Color(0xFF4B3F6E),
    isDark = false
)

val PinkDarkPalette = AppColors(
    themeBackground = Color(0xFF4B3F6E),
    cardSurface = Color(0xFF6C5F8D),
    darkCardSurface = Color(0xFF4B3F6E),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB0B0B0),
    darkCardTextPrimary = Color(0xFFFFFFFF),
    darkCardTextSecondary = Color(0xFFCCCCCC),
    iconBackground = Color(0xFF6C5F8D),
    darkCardIconBackground = Color(0xFF4B3F6E),
    primaryAccent = Color(0xFF9C8CB9),
    liquidAccentStart = Color(0xFF9C8CB9),
    liquidAccentEnd = Color(0xFF9C8CB9),
    chartGreen = Color(0xFF9C8CB9),
    chartOrange = Color(0xFFBA96C1),
    chartBlue = Color(0xFF6C5F8D),
    chartPink = Color(0xFFDCD7D5),
    chartPurple = Color(0xFF4B3F6E),
    isDark = true
)

// ─── Helper to resolve palette ───────────────────────────────────────────────

fun resolveAppColors(themeName: String, isDark: Boolean): AppColors {
    return when (themeName) {
        "Ocean Blue", "Blue/Black" -> if (isDark) BlueBlackDarkPalette else BlueBlackLightPalette
        "Green Tea", "Green", "Light Green" -> if (isDark) LightGreenDarkPalette else LightGreenLightPalette
        "Sunset", "Light Yellow" -> if (isDark) LightYellowDarkPalette else LightYellowLightPalette
        "Grapefruit", "Purple", "Light Blue" -> if (isDark) PurpleDarkPalette else PurpleLightPalette
        "Bubblegum", "Pink" -> if (isDark) PinkDarkPalette else PinkLightPalette
        else -> if (isDark) ClassicDarkPalette else ClassicLightPalette // "Classic" default
    }
}

// ─── Reactive Accessors (backward-compatible with existing UI code) ──────────
// These replace the old static `val` declarations so existing composable code
// referencing e.g. `ThemeBackground` will resolve to the current palette at
// composition time without any further refactoring.

val ThemeBackground: Color @Composable get() = LocalAppColors.current.themeBackground
val CardSurface: Color @Composable get() = LocalAppColors.current.cardSurface
val DarkCardSurface: Color @Composable get() = LocalAppColors.current.darkCardSurface

val TextPrimary: Color @Composable get() = LocalAppColors.current.textPrimary
val TextSecondary: Color @Composable get() = LocalAppColors.current.textSecondary
val DarkCardTextPrimary: Color @Composable get() = LocalAppColors.current.darkCardTextPrimary
val DarkCardTextSecondary: Color @Composable get() = LocalAppColors.current.darkCardTextSecondary

val IconBackground: Color @Composable get() = LocalAppColors.current.iconBackground
val DarkCardIconBackground: Color @Composable get() = LocalAppColors.current.darkCardIconBackground

val LiquidAccentStart: Color @Composable get() = LocalAppColors.current.liquidAccentStart
val LiquidAccentEnd: Color @Composable get() = LocalAppColors.current.liquidAccentEnd

val ChartGreen: Color @Composable get() = LocalAppColors.current.chartGreen
val ChartOrange: Color @Composable get() = LocalAppColors.current.chartOrange
val ChartBlue: Color @Composable get() = LocalAppColors.current.chartBlue
val ChartPink: Color @Composable get() = LocalAppColors.current.chartPink
val ChartPurple: Color @Composable get() = LocalAppColors.current.chartPurple
val PrimaryAccent: Color @Composable get() = LocalAppColors.current.primaryAccent

// Legacy compat – unused Purple / Pink from original boilerplate
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
