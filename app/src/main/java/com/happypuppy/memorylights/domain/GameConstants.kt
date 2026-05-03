package com.happypuppy.memorylights.domain

/**
 * Constants for game timing and animation values
 */
object GameConstants {
    // Player timeout duration
    const val PLAYER_TIMEOUT_MS = 10_000L

    // Base sequence timing
    const val BASE_LIT_DURATION_MS = 600L
    const val BASE_PAUSE_DURATION_MS = 400L

    // Minimum sequence timing (for difficulty scaling)
    const val MIN_LIT_DURATION_MS = 200L
    const val MIN_PAUSE_DURATION_MS = 150L

    // Animation durations
    const val BUTTON_SOUND_DURATION_MS = 300L
    const val STARTUP_BUTTON_LIGHT_MS = 300L
    const val STARTUP_PAUSE_MS = 150L
    const val STARTUP_INITIAL_DELAY_MS = 500L
    const val SEQUENCE_START_DELAY_MS = 500L
    const val LEVEL_ADVANCE_DELAY_MS = 1000L
    const val GAME_OVER_FLASH_DURATION_MS = 300L
    const val GAME_OVER_ANIMATION_WAIT_MS = 2000L

    // High score animation
    const val HIGH_SCORE_FLASH_INTERVAL_MS = 250L
    const val HIGH_SCORE_FLASH_COUNT = 6
    const val HIGH_SCORE_DISPLAY_MS = 2000L
    const val GAME_OVER_TEXT_DISPLAY_MS = 5000L

    // Difficulty scaling
    const val DIFFICULTY_REDUCTION_PERCENT = 0.2
    const val DIFFICULTY_INTERVAL = 4 // Speed increases every 4 rounds starting at round 5
    const val SPEED_UP_TEXT_DISPLAY_MS = 1200L

    // Sound settings
    const val DEFAULT_MASTER_VOLUME = 1.0f
    const val MIN_VOLUME = 0.0f
    const val MAX_VOLUME = 1.0f
    const val VIBRATION_DURATION_MS = 100L
}
