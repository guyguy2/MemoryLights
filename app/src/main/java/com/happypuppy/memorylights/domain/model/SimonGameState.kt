package com.happypuppy.memorylights.domain.model

/**
 * Game states for the Simon game.
 *
 * Navigation between screens is tracked separately via [ScreenState] — `gameState`
 * always reflects the underlying game progression and is unaffected by opening
 * Settings or Statistics overlays.
 */
sealed class GameState {
    object WaitingToStart : GameState()
    object ShowingSequence : GameState()
    object PlayerRepeating : GameState()
    object GameOver : GameState()
}