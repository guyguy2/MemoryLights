package com.happypuppy.memorylights.ui.theme

import androidx.compose.material3.darkColorScheme
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
