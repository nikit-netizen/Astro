package com.astro.storm.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Unified App Theme Colors with Dark/Light Mode Support
 *
 * These colors provide a cohesive look throughout the app.
 * Dark mode uses a warm brown theme, light mode uses cream/beige tones.
 */
data class AppThemeColors(
    // Primary Background Colors
    val ScreenBackground: Color,
    val CardBackground: Color,
    val CardBackgroundElevated: Color,
    val SurfaceColor: Color,

    // Accent Colors
    val AccentPrimary: Color,
    val AccentSecondary: Color,
    val AccentGold: Color,
    val AccentTeal: Color,

    // Text Colors
    val TextPrimary: Color,
    val TextSecondary: Color,
    val TextMuted: Color,
    val TextSubtle: Color,

    // Border and Divider Colors
    val BorderColor: Color,
    val DividerColor: Color,

    // Interactive Element Colors
    val ChipBackground: Color,
    val ChipBackgroundSelected: Color,
    val ButtonBackground: Color,
    val ButtonText: Color,

    // Status Colors
    val SuccessColor: Color,
    val WarningColor: Color,
    val ErrorColor: Color,
    val InfoColor: Color,

    // Chart-Specific Colors
    val ChartBackground: Color,
    val ChartBorder: Color,

    // Planet Colors
    val PlanetSun: Color,
    val PlanetMoon: Color,
    val PlanetMars: Color,
    val PlanetMercury: Color,
    val PlanetJupiter: Color,
    val PlanetVenus: Color,
    val PlanetSaturn: Color,
    val PlanetRahu: Color,
    val PlanetKetu: Color,

    // Navigation Colors
    val NavBarBackground: Color,
    val NavItemSelected: Color,
    val NavItemUnselected: Color,
    val NavIndicator: Color,

    // Bottom Sheet Colors
    val BottomSheetBackground: Color,
    val BottomSheetHandle: Color,

    // Prediction Card Colors
    val PredictionCardToday: Color,
    val PredictionCardTomorrow: Color,
    val PredictionCardWeekly: Color,

    // Life Area Colors
    val LifeAreaCareer: Color,
    val LifeAreaLove: Color,
    val LifeAreaHealth: Color,
    val LifeAreaGrowth: Color,
    val LifeAreaFinance: Color,
    val LifeAreaSpiritual: Color,

    // Additional Colors for Light Theme
    val InputBackground: Color,
    val DialogBackground: Color,
    val ScrimColor: Color,

    // Is dark theme flag
    val isDark: Boolean
)

/**
 * Dark Theme Colors - Warm brown tones for nighttime viewing
 */
val DarkAppThemeColors = AppThemeColors(
    // Primary Background Colors
    ScreenBackground = Color(0xFF1C1410),
    CardBackground = Color(0xFF2A201A),
    CardBackgroundElevated = Color(0xFF352A22),
    SurfaceColor = Color(0xFF241C16),

    // Accent Colors
    AccentPrimary = Color(0xFFB8A99A),
    AccentSecondary = Color(0xFF8B7355),
    AccentGold = Color(0xFFD4AF37),
    AccentTeal = Color(0xFF4DB6AC),

    // Text Colors
    TextPrimary = Color(0xFFE8DFD6),
    TextSecondary = Color(0xFFB8A99A),
    TextMuted = Color(0xFF8A7A6A),
    TextSubtle = Color(0xFF6A5A4A),

    // Border and Divider Colors
    BorderColor = Color(0xFF4A3F38),
    DividerColor = Color(0xFF3A302A),

    // Interactive Element Colors
    ChipBackground = Color(0xFF3D322B),
    ChipBackgroundSelected = Color(0xFF4A3F38),
    ButtonBackground = Color(0xFFB8A99A),
    ButtonText = Color(0xFF1C1410),

    // Status Colors
    SuccessColor = Color(0xFF81C784),
    WarningColor = Color(0xFFFFB74D),
    ErrorColor = Color(0xFFCF6679),
    InfoColor = Color(0xFF64B5F6),

    // Chart-Specific Colors
    ChartBackground = Color(0xFF1A1512),
    ChartBorder = Color(0xFFB8A99A),

    // Planet Colors
    PlanetSun = Color(0xFFD2691E),
    PlanetMoon = Color(0xFFDC143C),
    PlanetMars = Color(0xFFDC143C),
    PlanetMercury = Color(0xFF228B22),
    PlanetJupiter = Color(0xFFDAA520),
    PlanetVenus = Color(0xFF9370DB),
    PlanetSaturn = Color(0xFF4169E1),
    PlanetRahu = Color(0xFF8B0000),
    PlanetKetu = Color(0xFF8B0000),

    // Navigation Colors
    NavBarBackground = Color(0xFF241C16),
    NavItemSelected = Color(0xFFB8A99A),
    NavItemUnselected = Color(0xFF6A5A4A),
    NavIndicator = Color(0xFF3D322B),

    // Bottom Sheet Colors
    BottomSheetBackground = Color(0xFF2A201A),
    BottomSheetHandle = Color(0xFF4A3F38),

    // Prediction Card Colors
    PredictionCardToday = Color(0xFF2D2520),
    PredictionCardTomorrow = Color(0xFF2A2520),
    PredictionCardWeekly = Color(0xFF282520),

    // Life Area Colors
    LifeAreaCareer = Color(0xFFFFB74D),
    LifeAreaLove = Color(0xFFE57373),
    LifeAreaHealth = Color(0xFF81C784),
    LifeAreaGrowth = Color(0xFF64B5F6),
    LifeAreaFinance = Color(0xFFFFD54F),
    LifeAreaSpiritual = Color(0xFFBA68C8),

    // Additional Colors
    InputBackground = Color(0xFF2A201A),
    DialogBackground = Color(0xFF2A201A),
    ScrimColor = Color(0x80000000),

    // Is dark theme flag
    isDark = true
)

/**
 * Light Theme Colors - Cream/beige tones for daytime viewing
 */
val LightAppThemeColors = AppThemeColors(
    // Primary Background Colors
    ScreenBackground = Color(0xFFF5F2ED),
    CardBackground = Color(0xFFFFFFFF),
    CardBackgroundElevated = Color(0xFFFAF8F5),
    SurfaceColor = Color(0xFFFEFCF9),

    // Accent Colors
    AccentPrimary = Color(0xFF6B5D4D),
    AccentSecondary = Color(0xFF8B7355),
    AccentGold = Color(0xFFB8860B),
    AccentTeal = Color(0xFF008B8B),

    // Text Colors
    TextPrimary = Color(0xFF2C2418),
    TextSecondary = Color(0xFF5A4D3D),
    TextMuted = Color(0xFF7A6D5D),
    TextSubtle = Color(0xFFA99D8D),

    // Border and Divider Colors
    BorderColor = Color(0xFFD4C8B8),
    DividerColor = Color(0xFFE8DFD6),

    // Interactive Element Colors
    ChipBackground = Color(0xFFEDE7DF),
    ChipBackgroundSelected = Color(0xFFD4C8B8),
    ButtonBackground = Color(0xFF6B5D4D),
    ButtonText = Color(0xFFFFFFFF),

    // Status Colors
    SuccessColor = Color(0xFF2E7D32),
    WarningColor = Color(0xFFED6C02),
    ErrorColor = Color(0xFFD32F2F),
    InfoColor = Color(0xFF0288D1),

    // Chart-Specific Colors
    ChartBackground = Color(0xFFFAF8F5),
    ChartBorder = Color(0xFF6B5D4D),

    // Planet Colors (slightly adjusted for light mode visibility)
    PlanetSun = Color(0xFFCD6600),
    PlanetMoon = Color(0xFFB22222),
    PlanetMars = Color(0xFFB22222),
    PlanetMercury = Color(0xFF006400),
    PlanetJupiter = Color(0xFFB8860B),
    PlanetVenus = Color(0xFF7B68EE),
    PlanetSaturn = Color(0xFF3A5FCD),
    PlanetRahu = Color(0xFF8B0000),
    PlanetKetu = Color(0xFF8B0000),

    // Navigation Colors
    NavBarBackground = Color(0xFFFFFFFF),
    NavItemSelected = Color(0xFF6B5D4D),
    NavItemUnselected = Color(0xFFA99D8D),
    NavIndicator = Color(0xFFEDE7DF),

    // Bottom Sheet Colors
    BottomSheetBackground = Color(0xFFFFFFFF),
    BottomSheetHandle = Color(0xFFD4C8B8),

    // Prediction Card Colors
    PredictionCardToday = Color(0xFFFFFFFF),
    PredictionCardTomorrow = Color(0xFFFAF8F5),
    PredictionCardWeekly = Color(0xFFF5F2ED),

    // Life Area Colors (adjusted for light mode)
    LifeAreaCareer = Color(0xFFED6C02),
    LifeAreaLove = Color(0xFFC62828),
    LifeAreaHealth = Color(0xFF2E7D32),
    LifeAreaGrowth = Color(0xFF0277BD),
    LifeAreaFinance = Color(0xFFF9A825),
    LifeAreaSpiritual = Color(0xFF7B1FA2),

    // Additional Colors
    InputBackground = Color(0xFFFAF8F5),
    DialogBackground = Color(0xFFFFFFFF),
    ScrimColor = Color(0x40000000),

    // Is dark theme flag
    isDark = false
)

/**
 * CompositionLocal for accessing theme colors
 */
val LocalAppThemeColors = staticCompositionLocalOf { DarkAppThemeColors }

/**
 * App Theme object for accessing current theme colors
 *
 * This object provides backward-compatible access to theme colors.
 * It delegates to the current theme from CompositionLocal when accessed
 * inside a Composable, or falls back to dark theme colors for static access.
 */
object AppTheme {
    // Current theme accessor for Composables
    val current: AppThemeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppThemeColors.current

    // Static fallback colors for non-Composable contexts
    // These use the dark theme as the default (existing behavior)
    val ScreenBackground: Color get() = DarkAppThemeColors.ScreenBackground
    val CardBackground: Color get() = DarkAppThemeColors.CardBackground
    val CardBackgroundElevated: Color get() = DarkAppThemeColors.CardBackgroundElevated
    val SurfaceColor: Color get() = DarkAppThemeColors.SurfaceColor

    val AccentPrimary: Color get() = DarkAppThemeColors.AccentPrimary
    val AccentSecondary: Color get() = DarkAppThemeColors.AccentSecondary
    val AccentGold: Color get() = DarkAppThemeColors.AccentGold
    val AccentTeal: Color get() = DarkAppThemeColors.AccentTeal

    val TextPrimary: Color get() = DarkAppThemeColors.TextPrimary
    val TextSecondary: Color get() = DarkAppThemeColors.TextSecondary
    val TextMuted: Color get() = DarkAppThemeColors.TextMuted
    val TextSubtle: Color get() = DarkAppThemeColors.TextSubtle

    val BorderColor: Color get() = DarkAppThemeColors.BorderColor
    val DividerColor: Color get() = DarkAppThemeColors.DividerColor

    val ChipBackground: Color get() = DarkAppThemeColors.ChipBackground
    val ChipBackgroundSelected: Color get() = DarkAppThemeColors.ChipBackgroundSelected
    val ButtonBackground: Color get() = DarkAppThemeColors.ButtonBackground
    val ButtonText: Color get() = DarkAppThemeColors.ButtonText

    val SuccessColor: Color get() = DarkAppThemeColors.SuccessColor
    val WarningColor: Color get() = DarkAppThemeColors.WarningColor
    val ErrorColor: Color get() = DarkAppThemeColors.ErrorColor
    val InfoColor: Color get() = DarkAppThemeColors.InfoColor

    val ChartBackground: Color get() = DarkAppThemeColors.ChartBackground
    val ChartBorder: Color get() = DarkAppThemeColors.ChartBorder

    val PlanetSun: Color get() = DarkAppThemeColors.PlanetSun
    val PlanetMoon: Color get() = DarkAppThemeColors.PlanetMoon
    val PlanetMars: Color get() = DarkAppThemeColors.PlanetMars
    val PlanetMercury: Color get() = DarkAppThemeColors.PlanetMercury
    val PlanetJupiter: Color get() = DarkAppThemeColors.PlanetJupiter
    val PlanetVenus: Color get() = DarkAppThemeColors.PlanetVenus
    val PlanetSaturn: Color get() = DarkAppThemeColors.PlanetSaturn
    val PlanetRahu: Color get() = DarkAppThemeColors.PlanetRahu
    val PlanetKetu: Color get() = DarkAppThemeColors.PlanetKetu

    val NavBarBackground: Color get() = DarkAppThemeColors.NavBarBackground
    val NavItemSelected: Color get() = DarkAppThemeColors.NavItemSelected
    val NavItemUnselected: Color get() = DarkAppThemeColors.NavItemUnselected
    val NavIndicator: Color get() = DarkAppThemeColors.NavIndicator

    val BottomSheetBackground: Color get() = DarkAppThemeColors.BottomSheetBackground
    val BottomSheetHandle: Color get() = DarkAppThemeColors.BottomSheetHandle

    val PredictionCardToday: Color get() = DarkAppThemeColors.PredictionCardToday
    val PredictionCardTomorrow: Color get() = DarkAppThemeColors.PredictionCardTomorrow
    val PredictionCardWeekly: Color get() = DarkAppThemeColors.PredictionCardWeekly

    val LifeAreaCareer: Color get() = DarkAppThemeColors.LifeAreaCareer
    val LifeAreaLove: Color get() = DarkAppThemeColors.LifeAreaLove
    val LifeAreaHealth: Color get() = DarkAppThemeColors.LifeAreaHealth
    val LifeAreaGrowth: Color get() = DarkAppThemeColors.LifeAreaGrowth
    val LifeAreaFinance: Color get() = DarkAppThemeColors.LifeAreaFinance
    val LifeAreaSpiritual: Color get() = DarkAppThemeColors.LifeAreaSpiritual
}

/**
 * Provider composable for theme colors
 */
@Composable
fun ProvideAppThemeColors(
    colors: AppThemeColors,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAppThemeColors provides colors) {
        content()
    }
}
