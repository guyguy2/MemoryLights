package com.happypuppy.memorylights.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.happypuppy.memorylights.domain.enums.ThemeMode
import com.happypuppy.memorylights.ui.components.isReducedMotionEnabled

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.AMOLED,
    hapticsEnabled: Boolean = true,
    reducedMotionOverride: Boolean? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val reducedMotion = reducedMotionOverride ?: remember(context) { isReducedMotionEnabled(context) }

    val colorScheme = when (themeMode) {
        ThemeMode.AMOLED -> AmoledColorScheme
        ThemeMode.DARK -> DarkMaterialColorScheme
        ThemeMode.LIGHT -> LightMaterialColorScheme
    }
    val accentColors = when (themeMode) {
        ThemeMode.LIGHT -> LightGameAccentColors
        ThemeMode.AMOLED, ThemeMode.DARK -> DefaultGameAccentColors
    }
    val particleColors = when (themeMode) {
        ThemeMode.LIGHT -> LightCelebrationParticleColors
        ThemeMode.AMOLED, ThemeMode.DARK -> CelebrationParticleColors
    }

    CompositionLocalProvider(
        LocalGameAccentColors provides accentColors,
        LocalCelebrationParticleColors provides particleColors,
        LocalReducedMotion provides reducedMotion,
        LocalHapticsEnabled provides hapticsEnabled,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
