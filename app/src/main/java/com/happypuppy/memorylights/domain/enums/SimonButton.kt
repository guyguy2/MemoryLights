package com.happypuppy.memorylights.domain.enums

import androidx.compose.ui.graphics.Color

/**
 * Represents the different buttons in the Simon game
 */
enum class SimonButton(val color: Color, val brightColor: Color) {
    GREEN(Color(0xFF00AA00), Color(0xFF00FF00)),
    RED(Color(0xFFFF6666), Color(0xFFFF9999)),
    YELLOW(Color(0xFFFFDD00), Color(0xFFFFFF00)),
    BLUE(Color(0xFF2288FF), Color(0xFF66AAFF)),
    PURPLE(Color(0xFF9C27B0), Color(0xFFBA68C8)),
    ORANGE(Color(0xFFFF9800), Color(0xFFFFB74D));

    /** Title-cased name for display + accessibility ("GREEN" → "Green"). */
    val displayName: String = name.lowercase().replaceFirstChar { it.uppercase() }

    companion object {
        fun getAvailableButtons(memoryLightsPlusEnabled: Boolean): List<SimonButton> {
            return if (memoryLightsPlusEnabled) {
                entries
            } else {
                listOf(GREEN, RED, YELLOW, BLUE)
            }
        }
    }
}