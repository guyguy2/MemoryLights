package com.happypuppy.memorylights.domain.enums

import androidx.annotation.StringRes
import com.happypuppy.memorylights.R

/**
 * App-wide surface palette mode (F12).
 *
 * - [AMOLED]: pure-black backgrounds — best on OLED panels, default and
 *   matches the historic Memory Lights aesthetic.
 * - [DARK]: Material 3 tinted dark surfaces — softer contrast, nicer on
 *   LCD panels.
 * - [LIGHT]: Material 3 bright tonal surfaces with WCAG-verified deepened
 *   accents and dark system-bar icon polarity.
 */
enum class ThemeMode(@StringRes val displayNameRes: Int) {
    AMOLED(R.string.theme_mode_amoled),
    DARK(R.string.theme_mode_dark),
    LIGHT(R.string.theme_mode_light)
}
