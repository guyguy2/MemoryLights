package com.happypuppy.memorylights.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenStateTest {

    @Test
    fun `Game screen state is identified correctly`() {
        val state: ScreenState = ScreenState.Game

        assertTrue(state is ScreenState.Game)
        assertFalse(state is ScreenState.Settings)
        assertFalse(state is ScreenState.Statistics)
    }

    @Test
    fun `Settings screen state is identified correctly`() {
        val state: ScreenState = ScreenState.Settings

        assertFalse(state is ScreenState.Game)
        assertTrue(state is ScreenState.Settings)
        assertFalse(state is ScreenState.Statistics)
    }

    @Test
    fun `Statistics screen state is identified correctly`() {
        val state: ScreenState = ScreenState.Statistics

        assertFalse(state is ScreenState.Game)
        assertFalse(state is ScreenState.Settings)
        assertTrue(state is ScreenState.Statistics)
    }

    @Test
    fun `when expression covers all screen states`() {
        val states = listOf(
            ScreenState.Game,
            ScreenState.Settings,
            ScreenState.Statistics
        )

        states.forEach { state ->
            val name = when (state) {
                ScreenState.Game -> "game"
                ScreenState.Settings -> "settings"
                ScreenState.Statistics -> "statistics"
            }
            assertTrue(name.isNotEmpty())
        }
    }
}
