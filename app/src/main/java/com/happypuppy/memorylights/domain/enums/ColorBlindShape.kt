package com.happypuppy.memorylights.domain.enums

/**
 * Distinct geometric markers used by color-blind mode (#35).
 *
 * Each [SimonButton] maps to exactly one shape so the game stays playable
 * without hue discrimination. Mapping is pure domain data (no Compose)
 * so unit tests can lock the assignment without UI dependencies.
 *
 * Shapes chosen for maximum mutual distinguishability under typical
 * deuteranopia / protanopia / tritanopia confusion pairs (green-red,
 * blue-purple, yellow-orange).
 */
enum class ColorBlindShape {
    /** GREEN — filled circle. */
    CIRCLE,

    /** RED — upward triangle. */
    TRIANGLE,

    /** YELLOW — axis-aligned square. */
    SQUARE,

    /** BLUE — plus / cross. */
    CROSS,

    /** PURPLE — diamond (square rotated 45°). */
    DIAMOND,

    /** ORANGE — three horizontal bars. */
    STRIPES;

    companion object {
        fun forButton(button: SimonButton): ColorBlindShape = when (button) {
            SimonButton.GREEN -> CIRCLE
            SimonButton.RED -> TRIANGLE
            SimonButton.YELLOW -> SQUARE
            SimonButton.BLUE -> CROSS
            SimonButton.PURPLE -> DIAMOND
            SimonButton.ORANGE -> STRIPES
        }
    }
}
