package com.happypuppy.memorylights.domain.model

import com.happypuppy.memorylights.domain.enums.SimonButton
import com.happypuppy.memorylights.domain.enums.SoundPack

/**
 * Represents the state of the UI screens
 */
sealed class ScreenState {
    object Game : ScreenState()
    object Settings : ScreenState()
    object Statistics : ScreenState()
}

/**
 * Data class to represent game UI state
 */
data class SimonGameUiState(
    val gameState: GameState = GameState.WaitingToStart,
    val screenState: ScreenState = ScreenState.Game,
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
    val playerTimeoutSeconds: Int = 10 // Configurable inactivity timeout (5/10/15/30 sec). Drives both the timer and ring drain duration
) {
    // Computed property to get the current high score based on the mode
    val currentHighScore: Int
        get() = if (memoryLightsPlusEnabled) highScore6Button else highScore4Button

    val playerTimeoutMs: Long
        get() = playerTimeoutSeconds * 1000L
}