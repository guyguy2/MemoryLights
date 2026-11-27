package com.happypuppy.memorylights.ui.components

import androidx.compose.ui.graphics.lerp
import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownProgressColorTest {

    @Test
    fun `full fraction returns green`() {
        assertEquals(CountdownGreen, countdownProgressColor(1.0f))
    }

    @Test
    fun `midpoint fraction returns amber`() {
        assertEquals(CountdownAmber, countdownProgressColor(0.5f))
    }

    @Test
    fun `zero fraction returns red`() {
        assertEquals(CountdownRed, countdownProgressColor(0.0f))
    }

    @Test
    fun `fractions outside zero to one clamp to endpoints`() {
        assertEquals(CountdownRed, countdownProgressColor(-0.5f))
        assertEquals(CountdownGreen, countdownProgressColor(1.5f))
    }

    @Test
    fun `quarter fractions interpolate smoothly between stops`() {
        assertEquals(lerp(CountdownAmber, CountdownGreen, 0.5f), countdownProgressColor(0.75f))
        assertEquals(lerp(CountdownRed, CountdownAmber, 0.5f), countdownProgressColor(0.25f))
    }

    @Test
    fun `computeBlitzElapsedMs excludes paused time and clamps negatives`() {
        assertEquals(0L, computeBlitzElapsedMs(startTimeMs = 0L, nowMs = 5_000L))
        assertEquals(4_200L, computeBlitzElapsedMs(startTimeMs = 1_000L, nowMs = 5_200L))
        assertEquals(
            2_500L,
            computeBlitzElapsedMs(startTimeMs = 1_000L, nowMs = 9_999L, pauseStartedAtMs = 3_500L)
        )
        assertEquals(0L, computeBlitzElapsedMs(startTimeMs = 5_000L, nowMs = 2_000L))
    }
}
