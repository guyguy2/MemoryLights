package com.happypuppy.memorylights.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic JVM WCAG 2.1 contrast guard for both AMOLED and Dark brand
 * schemes and all [GameAccentColors] stat/overlay/countdown roles.
 */
class ThemeContrastTest {

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val effective = foreground.compositeOver(background)
        val l1 = effective.luminance().toDouble() + 0.05
        val l2 = background.luminance().toDouble() + 0.05
        return maxOf(l1, l2) / minOf(l1, l2)
    }

    private fun assertTextContrast(label: String, fg: Color, bg: Color) {
        val ratio = contrastRatio(fg, bg)
        assertTrue(
            "$label contrast is %.2f:1 (need >= 4.5:1)".format(ratio),
            ratio >= 4.5
        )
    }

    private fun assertNonTextOrLargeContrast(label: String, fg: Color, bg: Color) {
        val ratio = contrastRatio(fg, bg)
        assertTrue(
            "$label contrast is %.2f:1 (need >= 3.0:1)".format(ratio),
            ratio >= 3.0
        )
    }

    private fun verifyScheme(name: String, s: ColorScheme) {
        // Primary text & surface pairings
        assertTextContrast("$name onSurface/surface", s.onSurface, s.surface)
        assertTextContrast("$name onSurface/surfaceContainerLow", s.onSurface, s.surfaceContainerLow)
        assertTextContrast("$name onSurface/surfaceContainer", s.onSurface, s.surfaceContainer)
        assertTextContrast("$name onSurface/surfaceContainerHigh", s.onSurface, s.surfaceContainerHigh)
        assertTextContrast("$name onSurface/surfaceContainerHighest", s.onSurface, s.surfaceContainerHighest)

        // Secondary / subtitle text pairings (replaced Color.Gray)
        assertTextContrast("$name onSurfaceVariant/surface", s.onSurfaceVariant, s.surface)
        assertTextContrast("$name onSurfaceVariant/surfaceContainerLow", s.onSurfaceVariant, s.surfaceContainerLow)
        assertTextContrast("$name onSurfaceVariant/surfaceContainerHigh", s.onSurfaceVariant, s.surfaceContainerHigh)
        assertTextContrast("$name onSurfaceVariant/surfaceContainerHighest", s.onSurfaceVariant, s.surfaceContainerHighest)

        // Action / button / chip pairings
        assertTextContrast("$name onPrimary/primary", s.onPrimary, s.primary)
        assertTextContrast("$name onPrimaryContainer/primaryContainer", s.onPrimaryContainer, s.primaryContainer)
        assertTextContrast("$name onSecondary/secondary", s.onSecondary, s.secondary)
        assertTextContrast("$name onSecondaryContainer/secondaryContainer", s.onSecondaryContainer, s.secondaryContainer)
        assertTextContrast("$name onError/error", s.onError, s.error)
        assertTextContrast("$name onErrorContainer/errorContainer", s.onErrorContainer, s.errorContainer)

        // Dialog/card button text on elevated surfaces
        assertTextContrast("$name primary/surfaceContainerHigh", s.primary, s.surfaceContainerHigh)
        assertTextContrast("$name error/surfaceContainerHigh", s.error, s.surfaceContainerHigh)

        // Non-text UI controls & outlines
        assertNonTextOrLargeContrast("$name outline/surface", s.outline, s.surface)
        assertNonTextOrLargeContrast("$name outline/surfaceContainerHigh", s.outline, s.surfaceContainerHigh)
        assertNonTextOrLargeContrast("$name outline/surfaceContainerHighest", s.outline, s.surfaceContainerHighest)
    }

    private fun verifyGameAccents(name: String, s: ColorScheme, accents: GameAccentColors) {
        val statSurfaces = listOf(
            "surface" to s.surface,
            "surfaceContainerHigh" to s.surfaceContainerHigh,
        )
        val textAccents = listOf(
            "gamesPlayed" to accents.gamesPlayed,
            "highScore" to accents.highScore,
            "averageScore" to accents.averageScore,
            "totalScore" to accents.totalScore,
            "bestStreak" to accents.bestStreak,
            "resetWarning" to accents.resetWarning,
            "victory" to accents.victory,
            "speedUp" to accents.speedUp,
            "headlineHighlight" to accents.headlineHighlight,
        )

        statSurfaces.forEach { (surfaceName, bg) ->
            textAccents.forEach { (accentName, fg) ->
                assertTextContrast("$name $accentName/$surfaceName", fg, bg)
            }
        }

        // Countdown urgency stops on theme surface & HUD track
        listOf(
            "countdownGreen" to accents.countdownGreen,
            "countdownAmber" to accents.countdownAmber,
            "countdownRed" to accents.countdownRed,
        ).forEach { (stopName, fg) ->
            assertNonTextOrLargeContrast("$name $stopName/surface", fg, s.surface)
            assertNonTextOrLargeContrast("$name $stopName/surfaceContainerLow", fg, s.surfaceContainerLow)
        }
    }

    @Test
    fun `amoled scheme meets WCAG 2_1 contrast`() {
        verifyScheme("AMOLED", AmoledColorScheme)
        verifyGameAccents("AMOLED", AmoledColorScheme, DefaultGameAccentColors)
    }

    @Test
    fun `dark scheme meets WCAG 2_1 contrast`() {
        verifyScheme("Dark", DarkMaterialColorScheme)
        verifyGameAccents("Dark", DarkMaterialColorScheme, DefaultGameAccentColors)
    }

    @Test
    fun `light scheme meets WCAG 2_1 contrast`() {
        verifyScheme("Light", LightMaterialColorScheme)
        verifyGameAccents("Light", LightMaterialColorScheme, LightGameAccentColors)
    }
}
