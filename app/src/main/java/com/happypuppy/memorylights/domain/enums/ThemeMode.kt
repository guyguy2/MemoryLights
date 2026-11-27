package com.happypuppy.memorylights.domain.enums

import androidx.annotation.StringRes
import com.happypuppy.memorylights.R

/**
 * App-wide surface palette mode (F12).
 *
 * - [SYSTEM]: follows the device's system dark/light theme setting (default),
 *   resolving to [DARK] in system dark mode and [LIGHT] in system light mode.
 * - [AMOLED]: pure-black backgrounds — best on OLED panels and matches the
 *   historic Memory Lights aesthetic.
 * - [DARK]: Material 3 tinted dark surfaces — softer contrast, nicer on
 *   LCD panels.
 * - [LIGHT]: Material 3 bright tonal surfaces with WCAG-verified deepened
 *   accents and dark system-bar icon polarity.
 */
enum class ThemeMode(@StringRes val displayNameRes: Int) {
    SYSTEM(R.string.theme_mode_system),
    AMOLED(R.string.theme_mode_amoled),
    DARK(R.string.theme_mode_dark),
    LIGHT(R.string.theme_mode_light);

    /**
     * Resolves [SYSTEM] against the current OS dark-theme flag into a concrete
     * surface palette mode ([DARK] or [LIGHT]). Explicit modes pass through unchanged.
     */
    fun resolve(systemInDarkTheme: Boolean): ThemeMode = when (this) {
        SYSTEM -> if (systemInDarkTheme) DARK else LIGHT
        AMOLED, DARK, LIGHT -> this
    }
}
