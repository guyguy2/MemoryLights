package com.happypuppy.memorylights.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins every Material 3 role of [AmoledColorScheme] and [DarkMaterialColorScheme],
 * guards monotonic tonal surface-container elevation, and validates typography
 * line-height and dark-mode font-weight legibility.
 */
class BrandColorSchemeTest {

    private fun roles(s: ColorScheme): Map<String, Int> = mapOf(
        "primary" to s.primary.toArgb(),
        "onPrimary" to s.onPrimary.toArgb(),
        "primaryContainer" to s.primaryContainer.toArgb(),
        "onPrimaryContainer" to s.onPrimaryContainer.toArgb(),
        "inversePrimary" to s.inversePrimary.toArgb(),
        "secondary" to s.secondary.toArgb(),
        "onSecondary" to s.onSecondary.toArgb(),
        "secondaryContainer" to s.secondaryContainer.toArgb(),
        "onSecondaryContainer" to s.onSecondaryContainer.toArgb(),
        "tertiary" to s.tertiary.toArgb(),
        "onTertiary" to s.onTertiary.toArgb(),
        "tertiaryContainer" to s.tertiaryContainer.toArgb(),
        "onTertiaryContainer" to s.onTertiaryContainer.toArgb(),
        "background" to s.background.toArgb(),
        "onBackground" to s.onBackground.toArgb(),
        "surface" to s.surface.toArgb(),
        "onSurface" to s.onSurface.toArgb(),
        "surfaceVariant" to s.surfaceVariant.toArgb(),
        "onSurfaceVariant" to s.onSurfaceVariant.toArgb(),
        "surfaceTint" to s.surfaceTint.toArgb(),
        "inverseSurface" to s.inverseSurface.toArgb(),
        "inverseOnSurface" to s.inverseOnSurface.toArgb(),
        "error" to s.error.toArgb(),
        "onError" to s.onError.toArgb(),
        "errorContainer" to s.errorContainer.toArgb(),
        "onErrorContainer" to s.onErrorContainer.toArgb(),
        "outline" to s.outline.toArgb(),
        "outlineVariant" to s.outlineVariant.toArgb(),
        "scrim" to s.scrim.toArgb(),
        "surfaceBright" to s.surfaceBright.toArgb(),
        "surfaceDim" to s.surfaceDim.toArgb(),
        "surfaceContainer" to s.surfaceContainer.toArgb(),
        "surfaceContainerHigh" to s.surfaceContainerHigh.toArgb(),
        "surfaceContainerHighest" to s.surfaceContainerHighest.toArgb(),
        "surfaceContainerLow" to s.surfaceContainerLow.toArgb(),
        "surfaceContainerLowest" to s.surfaceContainerLowest.toArgb(),
    )

    private val expectedAmoled = mapOf(
        "primary" to 0xFFD0BCFF.toInt(),
        "onPrimary" to 0xFF381E72.toInt(),
        "primaryContainer" to 0xFF4F378B.toInt(),
        "onPrimaryContainer" to 0xFFEADDFF.toInt(),
        "inversePrimary" to 0xFF6750A4.toInt(),
        "secondary" to 0xFFCCC2DC.toInt(),
        "onSecondary" to 0xFF332D41.toInt(),
        "secondaryContainer" to 0xFF4A4458.toInt(),
        "onSecondaryContainer" to 0xFFE8DEF8.toInt(),
        "tertiary" to 0xFFEFB8C8.toInt(),
        "onTertiary" to 0xFF492532.toInt(),
        "tertiaryContainer" to 0xFF633B48.toInt(),
        "onTertiaryContainer" to 0xFFFFD8E4.toInt(),
        "background" to 0xFF000000.toInt(),
        "onBackground" to 0xFFE6E0E9.toInt(),
        "surface" to 0xFF000000.toInt(),
        "onSurface" to 0xFFE6E0E9.toInt(),
        "surfaceVariant" to 0xFF49454F.toInt(),
        "onSurfaceVariant" to 0xFFCAC4D0.toInt(),
        "surfaceTint" to 0xFFD0BCFF.toInt(),
        "inverseSurface" to 0xFFE6E0E9.toInt(),
        "inverseOnSurface" to 0xFF322F35.toInt(),
        "error" to 0xFFF2B8B5.toInt(),
        "onError" to 0xFF601410.toInt(),
        "errorContainer" to 0xFF8C1D18.toInt(),
        "onErrorContainer" to 0xFFF9DEDC.toInt(),
        "outline" to 0xFF938F99.toInt(),
        "outlineVariant" to 0xFF49454F.toInt(),
        "scrim" to 0xFF000000.toInt(),
        "surfaceBright" to 0xFF3B383E.toInt(),
        "surfaceDim" to 0xFF141218.toInt(),
        "surfaceContainer" to 0xFF1A1A1A.toInt(),
        "surfaceContainerHigh" to 0xFF1D1D1D.toInt(),
        "surfaceContainerHighest" to 0xFF303030.toInt(),
        "surfaceContainerLow" to 0xFF121212.toInt(),
        "surfaceContainerLowest" to 0xFF000000.toInt(),
    )

    private val expectedDark = mapOf(
        "primary" to 0xFFD0BCFF.toInt(),
        "onPrimary" to 0xFF381E72.toInt(),
        "primaryContainer" to 0xFF4F378B.toInt(),
        "onPrimaryContainer" to 0xFFEADDFF.toInt(),
        "inversePrimary" to 0xFF6750A4.toInt(),
        "secondary" to 0xFFCCC2DC.toInt(),
        "onSecondary" to 0xFF332D41.toInt(),
        "secondaryContainer" to 0xFF4A4458.toInt(),
        "onSecondaryContainer" to 0xFFE8DEF8.toInt(),
        "tertiary" to 0xFFEFB8C8.toInt(),
        "onTertiary" to 0xFF492532.toInt(),
        "tertiaryContainer" to 0xFF633B48.toInt(),
        "onTertiaryContainer" to 0xFFFFD8E4.toInt(),
        "background" to 0xFF141218.toInt(),
        "onBackground" to 0xFFE6E0E9.toInt(),
        "surface" to 0xFF141218.toInt(),
        "onSurface" to 0xFFE6E0E9.toInt(),
        "surfaceVariant" to 0xFF49454F.toInt(),
        "onSurfaceVariant" to 0xFFCAC4D0.toInt(),
        "surfaceTint" to 0xFFD0BCFF.toInt(),
        "inverseSurface" to 0xFFE6E0E9.toInt(),
        "inverseOnSurface" to 0xFF322F35.toInt(),
        "error" to 0xFFF2B8B5.toInt(),
        "onError" to 0xFF601410.toInt(),
        "errorContainer" to 0xFF8C1D18.toInt(),
        "onErrorContainer" to 0xFFF9DEDC.toInt(),
        "outline" to 0xFF938F99.toInt(),
        "outlineVariant" to 0xFF49454F.toInt(),
        "scrim" to 0xFF000000.toInt(),
        "surfaceBright" to 0xFF3B383E.toInt(),
        "surfaceDim" to 0xFF141218.toInt(),
        "surfaceContainer" to 0xFF211F26.toInt(),
        "surfaceContainerHigh" to 0xFF2B2930.toInt(),
        "surfaceContainerHighest" to 0xFF36343B.toInt(),
        "surfaceContainerLow" to 0xFF1D1B20.toInt(),
        "surfaceContainerLowest" to 0xFF0F0D13.toInt(),
    )

    private val expectedLight = mapOf(
        "primary" to 0xFF5B3F98.toInt(),
        "onPrimary" to 0xFFFFFFFF.toInt(),
        "primaryContainer" to 0xFFEADDFF.toInt(),
        "onPrimaryContainer" to 0xFF21005D.toInt(),
        "inversePrimary" to 0xFFD0BCFF.toInt(),
        "secondary" to 0xFF625B71.toInt(),
        "onSecondary" to 0xFFFFFFFF.toInt(),
        "secondaryContainer" to 0xFFE8DEF8.toInt(),
        "onSecondaryContainer" to 0xFF1D192B.toInt(),
        "tertiary" to 0xFF7D5260.toInt(),
        "onTertiary" to 0xFFFFFFFF.toInt(),
        "tertiaryContainer" to 0xFFFFD8E4.toInt(),
        "onTertiaryContainer" to 0xFF31111D.toInt(),
        "background" to 0xFFFDF8FF.toInt(),
        "onBackground" to 0xFF1D1B20.toInt(),
        "surface" to 0xFFFDF8FF.toInt(),
        "onSurface" to 0xFF1D1B20.toInt(),
        "surfaceVariant" to 0xFFE7E0EC.toInt(),
        "onSurfaceVariant" to 0xFF49454F.toInt(),
        "surfaceTint" to 0xFF5B3F98.toInt(),
        "inverseSurface" to 0xFF322F35.toInt(),
        "inverseOnSurface" to 0xFFF5EFF7.toInt(),
        "error" to 0xFFB3261E.toInt(),
        "onError" to 0xFFFFFFFF.toInt(),
        "errorContainer" to 0xFFF9DEDC.toInt(),
        "onErrorContainer" to 0xFF410E0B.toInt(),
        "outline" to 0xFF676170.toInt(),
        "outlineVariant" to 0xFFCAC4D0.toInt(),
        "scrim" to 0xFF000000.toInt(),
        "surfaceBright" to 0xFFFEF7FF.toInt(),
        "surfaceDim" to 0xFFDED8E1.toInt(),
        "surfaceContainer" to 0xFFF3EDF7.toInt(),
        "surfaceContainerHigh" to 0xFFECE6F0.toInt(),
        "surfaceContainerHighest" to 0xFFE6E0E9.toInt(),
        "surfaceContainerLow" to 0xFFF7F2FA.toInt(),
        "surfaceContainerLowest" to 0xFFFFFFFF.toInt(),
    )

    private fun assertScheme(expected: Map<String, Int>, actual: Map<String, Int>) {
        assertEquals(expected.keys, actual.keys)
        for ((role, argb) in expected) {
            assertEquals(
                "%s expected #%08X but was #%08X".format(role, argb, actual.getValue(role)),
                argb,
                actual.getValue(role)
            )
        }
    }

    @Test
    fun `amoled scheme keeps every role`() = assertScheme(expectedAmoled, roles(AmoledColorScheme))

    @Test
    fun `dark scheme keeps every role`() = assertScheme(expectedDark, roles(DarkMaterialColorScheme))

    @Test
    fun `light scheme keeps every role`() = assertScheme(expectedLight, roles(LightMaterialColorScheme))

    @Test
    fun `surface containers step monotonically in luminance across all themes`() {
        listOf(
            "AMOLED" to AmoledColorScheme,
            "Dark" to DarkMaterialColorScheme
        ).forEach { (name, scheme) ->
            val ladder = listOf(
                scheme.surfaceContainerLowest,
                scheme.surfaceContainerLow,
                scheme.surfaceContainer,
                scheme.surfaceContainerHigh,
                scheme.surfaceContainerHighest,
            ).map { it.luminance() }
            assertTrue(
                "$name surface containers must brighten monotonically: $ladder",
                ladder.zipWithNext().all { (a, b) -> b > a }
            )
            assertTrue(
                "$name dialog container (surfaceContainerHigh) must be lighter than surface",
                scheme.surfaceContainerHigh.luminance() > scheme.surface.luminance()
            )
        }

        val lightLadder = listOf(
            LightMaterialColorScheme.surfaceContainerLowest,
            LightMaterialColorScheme.surfaceContainerLow,
            LightMaterialColorScheme.surfaceContainer,
            LightMaterialColorScheme.surfaceContainerHigh,
            LightMaterialColorScheme.surfaceContainerHighest,
        ).map { it.luminance() }
        assertTrue(
            "Light surface containers must step down monotonically in luminance: $lightLadder",
            lightLadder.zipWithNext().all { (a, b) -> b < a }
        )
        assertTrue(
            "Light dialog container (surfaceContainerHigh) must separate from surface",
            LightMaterialColorScheme.surfaceContainerHigh.luminance() < LightMaterialColorScheme.surface.luminance()
        )
    }

    @Test
    fun `typography defines custom fonts, valid line heights, tracking, and readable weights`() {
        val displayStyles = listOf(
            "displaySmall" to Typography.displaySmall,
            "headlineLarge" to Typography.headlineLarge,
            "headlineMedium" to Typography.headlineMedium,
            "headlineSmall" to Typography.headlineSmall,
            "titleLarge" to Typography.titleLarge,
        )
        val bodyStyles = listOf(
            "titleMedium" to Typography.titleMedium,
            "titleSmall" to Typography.titleSmall,
            "bodyLarge" to Typography.bodyLarge,
            "bodyMedium" to Typography.bodyMedium,
            "bodySmall" to Typography.bodySmall,
            "labelLarge" to Typography.labelLarge,
            "labelMedium" to Typography.labelMedium,
            "labelSmall" to Typography.labelSmall,
        )
        displayStyles.forEach { (name, style) ->
            assertEquals("$name should use DisplayFontFamily", DisplayFontFamily, style.fontFamily)
            assertNotEquals("$name should not fall back to FontFamily.Default", FontFamily.Default, style.fontFamily)
        }
        bodyStyles.forEach { (name, style) ->
            assertEquals("$name should use BodyFontFamily", BodyFontFamily, style.fontFamily)
            assertNotEquals("$name should not fall back to FontFamily.Default", FontFamily.Default, style.fontFamily)
        }
        (displayStyles + bodyStyles).forEach { (name, style) ->
            assertTrue("$name must define explicit lineHeight in sp", style.lineHeight.isSp)
            assertTrue("$name must define explicit letterSpacing in sp", style.letterSpacing.isSp)
            assertTrue(
                "$name lineHeight (${style.lineHeight}) should be >= 1.15x fontSize (${style.fontSize})",
                style.lineHeight.value >= style.fontSize.value * 1.15f
            )
        }
        listOf(
            Typography.bodyLarge,
            Typography.bodyMedium,
            Typography.bodySmall,
            Typography.labelLarge,
            Typography.labelMedium,
            Typography.labelSmall,
        ).forEach { style ->
            val weight = style.fontWeight?.weight ?: FontWeight.Normal.weight
            assertTrue("Dark-mode body/label weight ($weight) should be >= 400", weight >= 400)
        }
    }
}
