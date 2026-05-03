package com.happypuppy.memorylights.domain.model

import com.happypuppy.memorylights.domain.enums.SimonButton
import com.happypuppy.memorylights.domain.enums.SoundPack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test


class SimonGameUiStateTest {

    @Test
    fun `default state has sensible values`() {
        val state = SimonGameUiState()

        assertEquals(GameState.WaitingToStart, state.gameState)
        assertEquals(1, state.level)
        assertEquals(0, state.roundCount)
        assertTrue(state.sequence.isEmpty())
        assertTrue(state.playerSequence.isEmpty())
        assertNull(state.currentlyLit)
        assertFalse(state.allButtonsLit)
        assertEquals(0, state.highScore4Button)
        assertEquals(0, state.highScore6Button)
        assertEquals(SoundPack.STANDARD, state.currentSoundPack)
        assertTrue(state.vibrateEnabled)
        assertTrue(state.soundEnabled)
        assertFalse(state.difficultyEnabled)
        assertFalse(state.memoryLightsPlusEnabled)
        assertFalse(state.showHighScoreParticles)
        assertFalse(state.showHighScoreText)
        assertFalse(state.showGameOverText)
        assertFalse(state.showYourTurnText)
        assertFalse(state.soundsLoaded)
        assertNull(state.soundLoadError)
    }

    @Test
    fun `currentHighScore returns 4-button score when memoryLightsPlusEnabled is false`() {
        val state = SimonGameUiState(
            highScore4Button = 10,
            highScore6Button = 20,
            memoryLightsPlusEnabled = false
        )

        assertEquals(10, state.currentHighScore)
    }

    @Test
    fun `currentHighScore returns 6-button score when memoryLightsPlusEnabled is true`() {
        val state = SimonGameUiState(
            highScore4Button = 10,
            highScore6Button = 20,
            memoryLightsPlusEnabled = true
        )

        assertEquals(20, state.currentHighScore)
    }

    @Test
    fun `copy creates new state with updated values`() {
        val original = SimonGameUiState()
        val updated = original.copy(level = 5, roundCount = 4)

        assertEquals(1, original.level)
        assertEquals(5, updated.level)
        assertEquals(0, original.roundCount)
        assertEquals(4, updated.roundCount)
    }

    @Test
    fun `sequence list is immutable`() {
        val originalSequence = listOf(SimonButton.GREEN, SimonButton.RED)
        val state = SimonGameUiState(sequence = originalSequence)

        assertEquals(2, state.sequence.size)
        assertEquals(SimonButton.GREEN, state.sequence[0])
        assertEquals(SimonButton.RED, state.sequence[1])
    }
}
