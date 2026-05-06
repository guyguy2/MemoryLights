package com.happypuppy.memorylights.domain.enums

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.happypuppy.memorylights.R

/**
 * Represents the different buttons in the Simon game
 */
enum class SimonButton(
    val color: Color,
    val brightColor: Color,
    @StringRes val displayNameRes: Int
) {
    GREEN(Color(0xFF00AA00), Color(0xFF00FF00), R.string.simon_green),
    RED(Color(0xFFFF6666), Color(0xFFFF9999), R.string.simon_red),
    YELLOW(Color(0xFFFFDD00), Color(0xFFFFFF00), R.string.simon_yellow),
    BLUE(Color(0xFF2288FF), Color(0xFF66AAFF), R.string.simon_blue),
    PURPLE(Color(0xFF9C27B0), Color(0xFFBA68C8), R.string.simon_purple),
    ORANGE(Color(0xFFFF9800), Color(0xFFFFB74D), R.string.simon_orange);

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
