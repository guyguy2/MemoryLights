package com.happypuppy.memorylights.domain.enums

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimonButtonTest {

    @Test
    fun `getAvailableButtons returns 4 buttons when memoryLightsPlusEnabled is false`() {
        val buttons = SimonButton.getAvailableButtons(memoryLightsPlusEnabled = false)

        assertEquals(4, buttons.size)
        assertTrue(buttons.contains(SimonButton.GREEN))
        assertTrue(buttons.contains(SimonButton.RED))
        assertTrue(buttons.contains(SimonButton.YELLOW))
        assertTrue(buttons.contains(SimonButton.BLUE))
        assertFalse(buttons.contains(SimonButton.PURPLE))
        assertFalse(buttons.contains(SimonButton.ORANGE))
    }

    @Test
    fun `getAvailableButtons returns 6 buttons when memoryLightsPlusEnabled is true`() {
        val buttons = SimonButton.getAvailableButtons(memoryLightsPlusEnabled = true)

        assertEquals(6, buttons.size)
        assertTrue(buttons.contains(SimonButton.GREEN))
        assertTrue(buttons.contains(SimonButton.RED))
        assertTrue(buttons.contains(SimonButton.YELLOW))
        assertTrue(buttons.contains(SimonButton.BLUE))
        assertTrue(buttons.contains(SimonButton.PURPLE))
        assertTrue(buttons.contains(SimonButton.ORANGE))
    }

    @Test
    fun `4-button mode returns buttons in correct order`() {
        val buttons = SimonButton.getAvailableButtons(memoryLightsPlusEnabled = false)

        assertEquals(SimonButton.GREEN, buttons[0])
        assertEquals(SimonButton.RED, buttons[1])
        assertEquals(SimonButton.YELLOW, buttons[2])
        assertEquals(SimonButton.BLUE, buttons[3])
    }

    @Test
    fun `each button has unique index`() {
        val indices = SimonButton.entries.map { it.index }

        assertEquals(indices.distinct().size, indices.size)
    }

    @Test
    fun `button indices are sequential starting from 0`() {
        val indices = SimonButton.entries.map { it.index }.sorted()

        assertEquals(listOf(0, 1, 2, 3, 4, 5), indices)
    }
}
