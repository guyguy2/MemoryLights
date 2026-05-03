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
    fun `level 5 triggers first speed reduction`() {
        val (lit, pause) = calculateSequenceTiming(level = 5, difficultyEnabled = true)
        // 600 * 0.8 = 480, 400 * 0.8 = 320
        assertEquals(480L, lit)
        assertEquals(320L, pause)
    }

    @Test
    fun `levels 6 7 8 carry one speed reduction`() {
        listOf(6, 7, 8).forEach { level ->
            val (lit, pause) = calculateSequenceTiming(level, difficultyEnabled = true)
            assertEquals("level=$level lit", 480L, lit)
            assertEquals("level=$level pause", 320L, pause)
        }
    }

    @Test
    fun `level 9 triggers second speed reduction`() {
        val (lit, pause) = calculateSequenceTiming(level = 9, difficultyEnabled = true)
        // factor = 1 - 2*0.2 = 0.6
        assertEquals(360L, lit)
        assertEquals(240L, pause)
    }

    @Test
    fun `timing decreases monotonically across speed thresholds`() {
        val l5 = calculateSequenceTiming(5, difficultyEnabled = true)
        val l9 = calculateSequenceTiming(9, difficultyEnabled = true)
        val l13 = calculateSequenceTiming(13, difficultyEnabled = true)
        assert(l5.first > l9.first) { "L5 lit ${l5.first} should exceed L9 lit ${l9.first}" }
        assert(l9.first > l13.first) { "L9 lit ${l9.first} should exceed L13 lit ${l13.first}" }
        assert(l5.second > l9.second) { "L5 pause ${l5.second} should exceed L9 pause ${l9.second}" }
    }

    @Test
    fun `lit duration clamps at minimum on extreme levels`() {
        val (lit, pause) = calculateSequenceTiming(level = 100, difficultyEnabled = true)
        assertEquals(GameConstants.MIN_LIT_DURATION_MS, lit)
        assertEquals(GameConstants.MIN_PAUSE_DURATION_MS, pause)
    }
}
