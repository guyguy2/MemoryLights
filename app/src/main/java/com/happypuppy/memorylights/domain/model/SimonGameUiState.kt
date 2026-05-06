package com.happypuppy.memorylights.domain.model

import com.happypuppy.memorylights.domain.enums.GameMode
import com.happypuppy.memorylights.domain.enums.SimonButton
import com.happypuppy.memorylights.domain.enums.SoundPack

/**
 * Data class to represent game UI state.
 *
 * Note: navigation between Game / Settings / Statistics is owned by the UI's
 * `NavController`, not this state — see `ui/navigation/Routes.kt`. The
 * ViewModel still tracks `gameState` (game-loop phase) but no longer carries a
 * screen-routing field.
 */
data class SimonGameUiState(
    val gameState: GameState = GameState.WaitingToStart,
    val level: Int = 1,
    val roundCount: Int = 0, // Track completed rounds for progressive UI features
    val sequence: List<SimonButton> = emptyList(),
    val playerSequence: List<SimonButton> = emptyList(),
    val currentlyLit: SimonButton? = null,
    val allButtonsLit: Boolean = false,  // Flag for when all buttons should light up
    val highScore4Button: Int = 0,
    val highScore6Button: Int = 0,
    val currentSoundPack: SoundPack = SoundPack.STANDARD,
    val vibrateEnabled: Boolean = true, // Whether button vibration is enabled
    val soundEnabled: Boolean = true, // Whether sound is enabled (mute/unmute)
    val difficultyEnabled: Boolean = false, // Whether progressive difficulty is enabled
    val memoryLightsPlusEnabled: Boolean = false, // Whether 6-button mode is enabled
    val showHighScoreParticles: Boolean = false, // Whether to show particle effects for new high score
    val showHighScoreText: Boolean = false, // Whether to show flashing HIGH SCORE text
    val showGameOverText: Boolean = false, // Whether to show flashing GAME OVER text
    val showYourTurnText: Boolean = false, // Whether to show YOUR TURN text overlay
    val showSpeedUpText: Boolean = false, // Whether to show transient SPEED UP! overlay (difficulty tier crossed)
    val statistics: GameStatistics = GameStatistics(), // Current game statistics
    val soundsLoaded: Boolean = false, // Whether all game sounds have finished loading
    val soundLoadError: String? = null, // Error message if sound loading failed
    val timeoutResetTick: Int = 0, // Increments each time the inactivity timer (re)starts; UI uses this to drive a countdown ring
    val playerTimeoutSeconds: Int = 10, // Configurable inactivity timeout (5/10/15/30 sec). Drives both the timer and ring drain duration
    val practiceModeEnabled: Boolean = false, // Practice mode (F15): wrong button replays sequence instead of ending game
    val reverseModeEnabled: Boolean = false, // Reverse mode (F1): player must repeat sequence in reverse order
    val audioOnlyModeEnabled: Boolean = false, // Audio-Only mode (F3): hide button colors during sequence playback
    val gameMode: GameMode = GameMode.CLASSIC, // Game mode (F4): Classic (open-ended) or Speed Blitz (sprint to BLITZ_TARGET_LEVEL)
    val blitzStartTimeMs: Long = 0L, // Wall-clock start of the current Speed Blitz run; 0 = not started
    val blitzElapsedMs: Long = 0L, // Final elapsed time when blitz run completes (won or lost); 0 while still in flight
    val blitzWon: Boolean = false, // True when the player completed BLITZ_TARGET_LEVEL levels — drives "VICTORY" overlay vs GAME OVER
    val bestBlitzTime4ButtonMs: Long = 0L, // Persisted best blitz time for 4-button mode; 0 = no completion yet
    val bestBlitzTime6ButtonMs: Long = 0L, // Persisted best blitz time for 6-button mode; 0 = no completion yet
    val dailyChallengeEnabled: Boolean = false, // Daily Challenge (F6): seed sequence with today's epoch day
    val dailyCompletedEpochDay: Long = 0L, // Epoch day of the last daily run played
    val dailyBestLevel: Int = 0 // Best level on dailyCompletedEpochDay; resets when day rolls over
) {
    // Computed property to get the current high score based on the mode
    val currentHighScore: Int
        get() = if (memoryLightsPlusEnabled) highScore6Button else highScore4Button

    val playerTimeoutMs: Long
        get() = playerTimeoutSeconds * 1000L

    /** Best blitz time for the current button-count, or 0 if none recorded. */
    val currentBestBlitzTimeMs: Long
        get() = if (memoryLightsPlusEnabled) bestBlitzTime6ButtonMs else bestBlitzTime4ButtonMs
}
