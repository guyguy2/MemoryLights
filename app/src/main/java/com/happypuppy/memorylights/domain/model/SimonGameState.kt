package com.happypuppy.memorylights.domain.model

/**
 * Game states for the Simon game
 */
sealed class GameState {
    object WaitingToStart : GameState()
    object ShowingSequence : GameState()
    object PlayerRepeating : GameState()
    object GameOver : GameState()
    object Settings : GameState() // State for settings screen
}