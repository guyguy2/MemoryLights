package com.happypuppy.memorylights.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateTest {

    @Test
    fun `WaitingToStart is identified correctly`() {
        val state: GameState = GameState.WaitingToStart

        assertTrue(state is GameState.WaitingToStart)
        assertFalse(state is GameState.ShowingSequence)
        assertFalse(state is GameState.PlayerRepeating)
        assertFalse(state is GameState.GameOver)
        assertFalse(state is GameState.Settings)
    }

    @Test
    fun `ShowingSequence is identified correctly`() {
        val state: GameState = GameState.ShowingSequence

        assertFalse(state is GameState.WaitingToStart)
        assertTrue(state is GameState.ShowingSequence)
        assertFalse(state is GameState.PlayerRepeating)
        assertFalse(state is GameState.GameOver)
        assertFalse(state is GameState.Settings)
    }

    @Test
    fun `PlayerRepeating is identified correctly`() {
        val state: GameState = GameState.PlayerRepeating

        assertFalse(state is GameState.WaitingToStart)
        assertFalse(state is GameState.ShowingSequence)
        assertTrue(state is GameState.PlayerRepeating)
        assertFalse(state is GameState.GameOver)
        assertFalse(state is GameState.Settings)
    }

    @Test
    fun `GameOver is identified correctly`() {
        val state: GameState = GameState.GameOver

        assertFalse(state is GameState.WaitingToStart)
        assertFalse(state is GameState.ShowingSequence)
        assertFalse(state is GameState.PlayerRepeating)
        assertTrue(state is GameState.GameOver)
        assertFalse(state is GameState.Settings)
    }

    @Test
    fun `Settings is identified correctly`() {
        val state: GameState = GameState.Settings

        assertFalse(state is GameState.WaitingToStart)
        assertFalse(state is GameState.ShowingSequence)
        assertFalse(state is GameState.PlayerRepeating)
        assertFalse(state is GameState.GameOver)
        assertTrue(state is GameState.Settings)
    }

    @Test
    fun `when expression covers all states`() {
        val states = listOf(
            GameState.WaitingToStart,
            GameState.ShowingSequence,
            GameState.PlayerRepeating,
            GameState.GameOver,
            GameState.Settings
        )

        states.forEach { state ->
            // This will fail to compile if a state is missing from the when
            val name = when (state) {
                GameState.WaitingToStart -> "waiting"
                GameState.ShowingSequence -> "showing"
                GameState.PlayerRepeating -> "repeating"
                GameState.GameOver -> "over"
                GameState.Settings -> "settings"
            }
            assertTrue(name.isNotEmpty())
        }
    }
}
