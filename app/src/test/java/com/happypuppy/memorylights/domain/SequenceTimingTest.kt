package com.happypuppy.memorylights.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SequenceTimingTest {

    @Test
    fun `difficulty disabled returns base timing for any level`() {
        listOf(1, 5, 9, 100).forEach { level ->
            val (lit, pause) = calculateSequenceTiming(level, difficultyEnabled = false)
            assertEquals("level=$level lit", GameConstants.BASE_LIT_DURATION_MS, lit)
            assertEquals("level=$level pause", GameConstants.BASE_PAUSE_DURATION_MS, pause)
        }
    }

    @Test
    fun `level below 5 stays at base timing when difficulty enabled`() {
        listOf(1, 2, 3, 4).forEach { level ->
            val (lit, pause) = calculateSequenceTiming(level, difficultyEnabled = true)
            assertEquals("level=$level lit", GameConstants.BASE_LIT_DURATION_MS, lit)
            assertEquals("level=$level pause", GameConstants.BASE_PAUSE_DURATION_MS, pause)
        }
    }

    @Test
    fun `level 5 produces first reduction below base`() {
        val (lit, pause) = calculateSequenceTiming(level = 5, difficultyEnabled = true)
        assertEquals(510L, lit)
        assertEquals(340L, pause)
    }

    @Test
    fun `each level above baseline reduces timing further`() {
        // F5: smooth log curve, every level past 4 shaves a little more.
        var prevLit = GameConstants.BASE_LIT_DURATION_MS
        var prevPause = GameConstants.BASE_PAUSE_DURATION_MS
        for (level in 5..15) {
            val (lit, pause) = calculateSequenceTiming(level, difficultyEnabled = true)
            assert(lit < prevLit) { "level=$level lit $lit should be < prev $prevLit" }
            assert(pause < prevPause) { "level=$level pause $pause should be < prev $prevPause" }
            prevLit = lit
            prevPause = pause
        }
    }

    @Test
    fun `level 9 timing matches expected log curve value`() {
        val (lit, pause) = calculateSequenceTiming(level = 9, difficultyEnabled = true)
        assertEquals(369L, lit)
        assertEquals(246L, pause)
    }

    @Test
    fun `level 13 timing matches expected log curve value`() {
        val (lit, pause) = calculateSequenceTiming(level = 13, difficultyEnabled = true)
        assertEquals(303L, lit)
        assertEquals(202L, pause)
    }

    @Test
    fun `lit duration clamps at minimum on extreme levels`() {
        val (lit, pause) = calculateSequenceTiming(level = 100, difficultyEnabled = true)
        assertEquals(GameConstants.MIN_LIT_DURATION_MS, lit)
        assertEquals(GameConstants.MIN_PAUSE_DURATION_MS, pause)
    }
}
