package com.happypuppy.memorylights.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Surface palette tokens kept as constants for the few callsites that
// can't read MaterialTheme.colorScheme (e.g. Brush gradients composed
// outside a @Composable scope). Most usages have migrated to
// MaterialTheme.colorScheme.surface*.
val CardBackground = Color(0xFF1D1D1D)
val DialogBackground = Color(0xFF1A1A1A)
val SurfaceContainer = Color(0xFF121212)
val SurfaceSelected = Color(0xFF303030)
val SurfaceContainerFade = Color(0xE6121212)

// Game-board pure-black canvas and high-contrast overlay scrims
val GameBoardBackdrop = Color(0xFF000000)

// Celebratory particle palette centralized in theme
val CelebrationParticleColors = listOf(
    Color(0xFFFFD700), // Gold
    Color(0xFFFFA500), // Orange
    Color(0xFFFF6B6B), // Red
    Color(0xFF4ECDC4), // Teal
    Color(0xFF45B7D1), // Blue
    Color(0xFF96CEB4), // Green
    Color(0xFFFECA57)  // Yellow
)

/**
 * Semantic accent colors for Statistics cards, Settings icons, and in-game
 * overlays. Every foreground accent here is verified against both AMOLED and
 * Dark `surfaceContainerHigh` in `ThemeContrastTest` (>= 4.5:1 WCAG AA).
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
