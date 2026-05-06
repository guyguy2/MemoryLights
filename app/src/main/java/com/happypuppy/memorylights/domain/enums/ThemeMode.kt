package com.happypuppy.memorylights.domain.enums

import androidx.annotation.StringRes
import com.happypuppy.memorylights.R

/**
 * App-wide surface palette mode (F12). Game-screen text colors and the
 * colored panels stay constant; only the surrounding chrome (cards,
 * dialogs, sub-screen backgrounds) shifts.
 *
 * - [AMOLED]: pure-black backgrounds — best on OLED panels, default and
 *   matches the historic Memory Lights aesthetic.
 * - [DARK]: Material 3 tinted dark surfaces — softer contrast, nicer on
 *   LCD panels.
 *
 * A real Light variant is deferred to v1.1: the game screen has dozens
 * of hardcoded `Color.White` / `Color.Gray` text calls that need a
 * theme-aware audit before light-on-light becomes legible.
 */
enum class ThemeMode(@StringRes val displayNameRes: Int) {
    AMOLED(R.string.theme_mode_amoled),
    DARK(R.string.theme_mode_dark)
}
