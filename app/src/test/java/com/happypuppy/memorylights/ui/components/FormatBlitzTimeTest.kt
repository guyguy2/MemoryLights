package com.happypuppy.memorylights.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the Speed Blitz `M:SS.s` formatter shared by CenterDiscContent (GameOver)
 * and BlitzHud (live timer). Negative inputs must not render a minus sign.
 */
class FormatBlitzTimeTest {

    @Test
    fun `zero and negative clamp to zero display`() {
        assertEquals("0:00.0", formatBlitzTime(0L))
        assertEquals("0:00.0", formatBlitzTime(-1L))
        assertEquals("0:00.0", formatBlitzTime(-60_000L))
    }

    @Test
    fun `tenths are floor of remaining milliseconds`() {
        assertEquals("0:00.0", formatBlitzTime(99L))
        assertEquals("0:00.1", formatBlitzTime(100L))
        assertEquals("0:00.9", formatBlitzTime(999L))
        assertEquals("0:01.0", formatBlitzTime(1_000L))
        assertEquals("0:01.5", formatBlitzTime(1_599L))
    }

    @Test
    fun `seconds pad to two digits and minutes roll over`() {
        assertEquals("0:09.0", formatBlitzTime(9_000L))
        assertEquals("0:59.9", formatBlitzTime(59_999L))
        assertEquals("1:00.0", formatBlitzTime(60_000L))
        assertEquals("1:02.3", formatBlitzTime(62_300L))
        assertEquals("12:34.5", formatBlitzTime(12 * 60_000L + 34_500L))
    }
}
