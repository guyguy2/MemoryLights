package com.happypuppy.memorylights.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF5B3F98)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)

// Pure-black backdrop kept for dark game-board reference and contrast tests
val GameBoardBackdrop = Color(0xFF000000)

// Celebratory particle palettes centralized in theme (dark and light variants)
val CelebrationParticleColors = listOf(
    Color(0xFFFFD700), // Gold
    Color(0xFFFFA500), // Orange
    Color(0xFFFF6B6B), // Red
    Color(0xFF4ECDC4), // Teal
    Color(0xFF45B7D1), // Blue
    Color(0xFF96CEB4), // Green
    Color(0xFFFECA57)  // Yellow
)

val LightCelebrationParticleColors = listOf(
    Color(0xFFD48800), // Deep Gold
    Color(0xFFE65100), // Deep Orange
    Color(0xFFD32F2F), // Crimson
    Color(0xFF00796B), // Deep Teal
    Color(0xFF1565C0), // Cobalt Blue
    Color(0xFF2E7D32), // Forest Green
    Color(0xFF7B1FA2)  // Royal Purple
)

val LocalCelebrationParticleColors = staticCompositionLocalOf { CelebrationParticleColors }
val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalHapticsEnabled = staticCompositionLocalOf { true }

/**
 * Semantic accent colors for Statistics cards, Settings icons, and in-game
 * overlays. Every foreground accent here is verified against its matching
 * theme `surface` and `surfaceContainerHigh` in `ThemeContrastTest`
 * (>= 4.5:1 WCAG AA text; >= 3.0:1 non-text countdown stops).
 */
@Immutable
data class GameAccentColors(
    val gamesPlayed: Color,
    val highScore: Color,
    val averageScore: Color,
    val totalScore: Color,
    val bestStreak: Color,
    val resetWarning: Color,
    val victory: Color,
    val speedUp: Color,
    val headlineHighlight: Color,
    val countdownGreen: Color,
    val countdownAmber: Color,
    val countdownRed: Color,
)

val DefaultGameAccentColors = GameAccentColors(
    gamesPlayed = Color(0xFF42A5F5),
    highScore = Color(0xFFFFC107),
    averageScore = Color(0xFF4CAF50),
    totalScore = Color(0xFFCE93D8),
    bestStreak = Color(0xFFFF7043),
    resetWarning = Color(0xFFFF9800),
    victory = Color(0xFF66BB6A),
    speedUp = Color(0xFFFF7043),
    headlineHighlight = Color(0xFFFFFF00),
    countdownGreen = Color(0xFF4CAF50),
    countdownAmber = Color(0xFFFFB300),
    countdownRed = Color(0xFFE53935),
)

val LightGameAccentColors = GameAccentColors(
    gamesPlayed = Color(0xFF0D47A1),
    highScore = Color(0xFF8C5300),
    averageScore = Color(0xFF1B5E20),
    totalScore = Color(0xFF6A1B9A),
    bestStreak = Color(0xFFB7330A),
    resetWarning = Color(0xFF9C4200),
    victory = Color(0xFF1B5E20),
    speedUp = Color(0xFFB7330A),
    headlineHighlight = Color(0xFF7A4900),
    countdownGreen = Color(0xFF2E7D32),
    countdownAmber = Color(0xFF9A5B00),
    countdownRed = Color(0xFFC62828),
)

val LocalGameAccentColors = staticCompositionLocalOf { DefaultGameAccentColors }

// AMOLED variant — pure black background. Default; matches the
// historic Memory Lights aesthetic and saves OLED pixels.
val AmoledColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF121212),
    surfaceContainer = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF1D1D1D),
    surfaceContainerHighest = Color(0xFF303030),
)

// Material 3 dark variant — softer tinted-gray surfaces, friendlier on
// LCD panels where pure black appears slightly washed-out.
val DarkMaterialColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF141218),
    surface = Color(0xFF141218),
    surfaceContainerLowest = Color(0xFF0F0D13),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B),
)

// Material 3 light variant — crisp lavender-tinted tonal ladder with
// monotonic luminance step-down and WCAG AA verified roles.
val LightMaterialColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = PurpleGrey40,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Pink40,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFDF8FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFDF8FF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF676170),
    outlineVariant = Color(0xFFCAC4D0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9),
)
