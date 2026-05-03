package com.happypuppy.memorylights.domain

import org.junit.Assert.assertTrue
import org.junit.Test

class GameConstantsTest {

    @Test
    fun `player timeout is reasonable duration`() {
        assertTrue(
            "Player timeout should be at least 5 seconds",
            GameConstants.PLAYER_TIMEOUT_MS >= 5000L
        )
        assertTrue(
            "Player timeout should be at most 30 seconds",
            GameConstants.PLAYER_TIMEOUT_MS <= 30000L
        )
    }

    @Test
    fun `base timing values are greater than minimum values`() {
        assertTrue(
            "Base lit duration should be greater than minimum",
            GameConstants.BASE_LIT_DURATION_MS > GameConstants.MIN_LIT_DURATION_MS
        )
        assertTrue(
            "Base pause duration should be greater than minimum",
            GameConstants.BASE_PAUSE_DURATION_MS > GameConstants.MIN_PAUSE_DURATION_MS
        )
    }

    @Test
    fun `minimum timing values are positive`() {
        assertTrue(
            "Minimum lit duration should be positive",
            GameConstants.MIN_LIT_DURATION_MS > 0
        )
        assertTrue(
            "Minimum pause duration should be positive",
            GameConstants.MIN_PAUSE_DURATION_MS > 0
        )
    }

    @Test
    fun `difficulty reduction is reasonable percentage`() {
        assertTrue(
            "Difficulty reduction should be positive",
            GameConstants.DIFFICULTY_REDUCTION_PERCENT > 0
        )
        assertTrue(
            "Difficulty reduction should be less than 100%",
            GameConstants.DIFFICULTY_REDUCTION_PERCENT < 1.0
        )
    }

    @Test
    fun `difficulty interval is positive`() {
        assertTrue(
            "Difficulty interval should be positive",
            GameConstants.DIFFICULTY_INTERVAL > 0
        )
    }

    @Test
    fun `animation durations are positive`() {
        assertTrue(GameConstants.BUTTON_SOUND_DURATION_MS > 0)
        assertTrue(GameConstants.STARTUP_BUTTON_LIGHT_MS > 0)
        assertTrue(GameConstants.STARTUP_PAUSE_MS > 0)
        assertTrue(GameConstants.STARTUP_INITIAL_DELAY_MS > 0)
        assertTrue(GameConstants.SEQUENCE_START_DELAY_MS > 0)
        assertTrue(GameConstants.LEVEL_ADVANCE_DELAY_MS > 0)
        assertTrue(GameConstants.GAME_OVER_FLASH_DURATION_MS > 0)
        assertTrue(GameConstants.GAME_OVER_ANIMATION_WAIT_MS > 0)
    }

    @Test
    fun `high score animation values are sensible`() {
        assertTrue(GameConstants.HIGH_SCORE_FLASH_INTERVAL_MS > 0)
        assertTrue(GameConstants.HIGH_SCORE_FLASH_COUNT > 0)
        assertTrue(GameConstants.HIGH_SCORE_DISPLAY_MS > 0)
        assertTrue(GameConstants.GAME_OVER_TEXT_DISPLAY_MS > 0)
    }
}
