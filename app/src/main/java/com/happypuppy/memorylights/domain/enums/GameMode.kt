package com.happypuppy.memorylights.domain.enums

import androidx.annotation.StringRes
import com.happypuppy.memorylights.R

/**
 * High-level run format. Orthogonal to the 4-button / 6-button toggle and
 * to the per-run modifiers (Difficulty / Reverse / Practice / Audio-Only).
 *
 * - [CLASSIC]: open-ended, sequence grows until the player misses.
 * - [SPEED_BLITZ]: race to a fixed target level (`BLITZ_TARGET_LEVEL`),
 *   shortest elapsed time wins; best time is persisted per button-count.
 */
enum class GameMode(@StringRes val displayNameRes: Int) {
    CLASSIC(R.string.game_mode_classic),
    SPEED_BLITZ(R.string.game_mode_speed_blitz)
}
