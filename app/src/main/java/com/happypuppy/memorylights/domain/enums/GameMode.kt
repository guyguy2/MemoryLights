package com.happypuppy.memorylights.domain.enums

/**
 * High-level run format. Orthogonal to the 4-button / 6-button toggle and
 * to the per-run modifiers (Difficulty / Reverse / Practice / Audio-Only).
 *
 * - [CLASSIC]: open-ended, sequence grows until the player misses.
 * - [SPEED_BLITZ]: race to a fixed target level (`BLITZ_TARGET_LEVEL`),
 *   shortest elapsed time wins; best time is persisted per button-count.
 */
enum class GameMode(val displayName: String) {
    CLASSIC("Classic"),
    SPEED_BLITZ("Speed Blitz")
}
