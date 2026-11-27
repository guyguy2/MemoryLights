package com.happypuppy.memorylights.domain.enums

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `SYSTEM resolves to DARK when system dark theme is enabled`() {
        assertEquals(ThemeMode.DARK, ThemeMode.SYSTEM.resolve(systemInDarkTheme = true))
    }

    @Test
    fun `SYSTEM resolves to LIGHT when system dark theme is disabled`() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.SYSTEM.resolve(systemInDarkTheme = false))
    }

    @Test
    fun `explicit theme modes ignore system dark theme flag`() {
        assertEquals(ThemeMode.AMOLED, ThemeMode.AMOLED.resolve(systemInDarkTheme = false))
        assertEquals(ThemeMode.AMOLED, ThemeMode.AMOLED.resolve(systemInDarkTheme = true))
        assertEquals(ThemeMode.DARK, ThemeMode.DARK.resolve(systemInDarkTheme = false))
        assertEquals(ThemeMode.DARK, ThemeMode.DARK.resolve(systemInDarkTheme = true))
        assertEquals(ThemeMode.LIGHT, ThemeMode.LIGHT.resolve(systemInDarkTheme = false))
        assertEquals(ThemeMode.LIGHT, ThemeMode.LIGHT.resolve(systemInDarkTheme = true))
    }

    @Test
    fun `SYSTEM is listed first in ThemeMode entries`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.entries.first())
        assertEquals(
            listOf(ThemeMode.SYSTEM, ThemeMode.AMOLED, ThemeMode.DARK, ThemeMode.LIGHT),
            ThemeMode.entries
        )
    }
}
