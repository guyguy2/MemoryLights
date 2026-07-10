package com.happypuppy.memorylights.domain.enums

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the #35 color-blind shape assignment. The mapping is the product
 * decision surface — UI drawing code is free to restyle glyphs, but each
 * button must keep a unique shape so the mode stays playable.
 */
class ColorBlindShapeTest {

    @Test
    fun `each button maps to a unique shape`() {
        val shapes = SimonButton.entries.map { ColorBlindShape.forButton(it) }
        assertEquals(
            "every SimonButton must have a distinct ColorBlindShape",
            SimonButton.entries.size,
            shapes.toSet().size
        )
    }

    @Test
    fun `canonical mapping is stable`() {
        assertEquals(ColorBlindShape.CIRCLE, ColorBlindShape.forButton(SimonButton.GREEN))
        assertEquals(ColorBlindShape.TRIANGLE, ColorBlindShape.forButton(SimonButton.RED))
        assertEquals(ColorBlindShape.SQUARE, ColorBlindShape.forButton(SimonButton.YELLOW))
        assertEquals(ColorBlindShape.CROSS, ColorBlindShape.forButton(SimonButton.BLUE))
        assertEquals(ColorBlindShape.DIAMOND, ColorBlindShape.forButton(SimonButton.PURPLE))
        assertEquals(ColorBlindShape.STRIPES, ColorBlindShape.forButton(SimonButton.ORANGE))
    }

    @Test
    fun `all six enum shapes are used`() {
        val used = SimonButton.entries.map { ColorBlindShape.forButton(it) }.toSet()
        assertEquals(ColorBlindShape.entries.toSet(), used)
    }
}
