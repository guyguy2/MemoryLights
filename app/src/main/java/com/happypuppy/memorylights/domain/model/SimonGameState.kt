package com.happypuppy.memorylights.domain.model

/**
 * Game states for the Simon game.
 *
 * Navigation between screens is owned by the UI's `NavController` (see
 * `ui/navigation/Routes.kt`) — `gameState` always reflects the underlying
 * game progression and is unaffected by opening Settings or Statistics.
 */
sealed class GameState {
    object WaitingToStart : GameState()
    object ShowingSequence : GameState()
    object PlayerRepeating : GameState()
    /**
     * Player explicitly paused mid-turn (F14). Distinct from app backgrounding,
     * which keeps gameState as PlayerRepeating but freezes the timer via the
     * isAppInForeground flag. From Paused, only [SimonGameViewModel.resumeGame]
     * (or starting a new game) returns to PlayerRepeating.
     */
    object Paused : GameState()
    object GameOver : GameState()
}