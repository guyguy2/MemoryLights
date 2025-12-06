package com.happypuppy.memorylights.domain.enums

import androidx.compose.ui.graphics.Color

/**
 * Represents the different buttons in the Simon game
 */
enum class SimonButton(val color: Color, val brightColor: Color, val index: Int) {
    GREEN(Color(0xFF00AA00), Color(0xFF00FF00), 0),
    RED(Color(0xFFFF6666), Color(0xFFFF9999), 1),
    YELLOW(Color(0xFFFFDD00), Color(0xFFFFFF00), 2),
    BLUE(Color(0xFF2288FF), Color(0xFF66AAFF), 3),
    PURPLE(Color(0xFF9C27B0), Color(0xFFBA68C8), 4),
    ORANGE(Color(0xFFFF9800), Color(0xFFFFB74D), 5);

    companion object {
        fun getAvailableButtons(memoryLightsPlusEnabled: Boolean): List<SimonButton> {
            return if (memoryLightsPlusEnabled) {
                values().toList()
            } else {
                listOf(GREEN, RED, YELLOW, BLUE)
            }
        }
    }
}