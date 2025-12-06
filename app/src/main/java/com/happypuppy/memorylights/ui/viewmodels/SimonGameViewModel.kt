package com.happypuppy.memorylights.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happypuppy.memorylights.data.manager.SimonSoundManager
import com.happypuppy.memorylights.data.manager.StatisticsManager
import com.happypuppy.memorylights.domain.enums.SimonButton
import com.happypuppy.memorylights.domain.model.GameStatistics
import com.happypuppy.memorylights.domain.enums.SoundPack
import com.happypuppy.memorylights.domain.model.GameState
import com.happypuppy.memorylights.domain.model.ScreenState
import com.happypuppy.memorylights.domain.model.SimonGameUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * ViewModel for the Memory Lights game logic with lifecycle awareness
 */
class SimonGameViewModel(
    // Inject SimonSoundManager through constructor
    private val soundManager: SimonSoundManager,
    private val statisticsManager: StatisticsManager
) : ViewModel(), DefaultLifecycleObserver {

    // Get the application context from Koin
    private val appContext = soundManager.getContext()

    private val TAG = "SimonGameViewModel"

    // Flag to track if startup animation has been played in this session
    private var hasPlayedStartupAnimation = false

    // Timeout duration for player's turn (10 seconds)
    private val playerTimeoutDuration = 10000L // 10 seconds in milliseconds

    // Job to track the timeout timer
    private var timeoutJob: Job? = null

    // Track if the app is currently in foreground
    private var isAppInForeground = true

    // Track if the game was active before going to background
    private var wasGameActiveBeforeBackground = false

    // Track the game state before going to background
    private var gameStateBeforeBackground: GameState = GameState.WaitingToStart

    // SharedPreferences for storing settings
    private val preferences = appContext.getSharedPreferences(
        "simon_game_prefs", Context.MODE_PRIVATE
    )

    // Private and public state flows
    private val _uiState = MutableStateFlow(SimonGameUiState())
    val uiState: StateFlow<SimonGameUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "Initializing SimonGameViewModel")

        // Load settings from preferences
        loadSettings()

        // Play startup animation once when app starts, then start the game
        playStartupAnimation {
            hasPlayedStartupAnimation = true
            initializeNewGame()
        }
    }

    /**
     * Load saved settings from SharedPreferences
     */
    private fun loadSettings() {
        val savedSoundPackName = preferences.getString("sound_pack", SoundPack.STANDARD.name)
        val savedSoundPack = try {
            SoundPack.valueOf(savedSoundPackName ?: SoundPack.STANDARD.name)
        } catch (e: Exception) {
            SoundPack.STANDARD
        }

        val savedHighScore4Button = preferences.getInt("high_score_4_button", 0)
        val savedHighScore6Button = preferences.getInt("high_score_6_button", 0)

        // Load vibration setting (default to true)
        val savedVibrateEnabled = preferences.getBoolean("vibrate_enabled", true)
        
        // Load sound enabled setting (default to true)
        val savedSoundEnabled = preferences.getBoolean("sound_enabled", true)
        
        // Load difficulty setting (default to false)
        val savedDifficultyEnabled = preferences.getBoolean("difficulty_enabled", false)
        
        // Load Memory Lights+ setting (default to false)
        val savedMemoryLightsPlusEnabled = preferences.getBoolean("memory_lights_plus_enabled", false)
        
        // Load current statistics
        val currentStatistics = statisticsManager.getStatistics()

        Log.d(TAG, "Loaded settings - Sound Pack: $savedSoundPack, High Score 4-button: $savedHighScore4Button, High Score 6-button: $savedHighScore6Button, Vibrate: $savedVibrateEnabled, Sound Enabled: $savedSoundEnabled, Difficulty: $savedDifficultyEnabled, Memory Lights+: $savedMemoryLightsPlusEnabled")

        // Update sound manager with saved sound pack, vibration setting, and sound enabled state
        soundManager.setSoundPack(savedSoundPack)
        soundManager.setVibrationEnabled(savedVibrateEnabled)
        soundManager.setSoundMuted(!savedSoundEnabled)

        // Update UI state with saved settings
        _uiState.update { it.copy(
            currentSoundPack = savedSoundPack,
            highScore4Button = savedHighScore4Button,
            highScore6Button = savedHighScore6Button,
            vibrateEnabled = savedVibrateEnabled,
            soundEnabled = savedSoundEnabled,
            difficultyEnabled = savedDifficultyEnabled,
            memoryLightsPlusEnabled = savedMemoryLightsPlusEnabled,
            statistics = currentStatistics
        )}
    }

    /**
     * Save settings to SharedPreferences
     */
    private fun saveSettings() {
        preferences.edit {
            putString("sound_pack", _uiState.value.currentSoundPack.name)
                .putInt("high_score_4_button", _uiState.value.highScore4Button)
                .putInt("high_score_6_button", _uiState.value.highScore6Button)
                .putBoolean("vibrate_enabled", _uiState.value.vibrateEnabled)
                .putBoolean("sound_enabled", _uiState.value.soundEnabled)
                .putBoolean("difficulty_enabled", _uiState.value.difficultyEnabled)
                .putBoolean("memory_lights_plus_enabled", _uiState.value.memoryLightsPlusEnabled)
        }

        Log.d(TAG, "Saved settings - Sound Pack: ${_uiState.value.currentSoundPack.name}, " +
                "High Score 4-button: ${_uiState.value.highScore4Button}, " +
                "High Score 6-button: ${_uiState.value.highScore6Button}, " +
                "Vibrate: ${_uiState.value.vibrateEnabled}, " +
                "Sound: ${_uiState.value.soundEnabled}, " +
                "Difficulty: ${_uiState.value.difficultyEnabled}, " +
                "Memory Lights+: ${_uiState.value.memoryLightsPlusEnabled}")
    }

    // Track previous game state before entering settings
    private var previousGameState: GameState = GameState.WaitingToStart

    // Track active coroutine jobs that need to be paused/resumed
    private var activeSequenceJob: Job? = null
    private var gameOverTextAnimationJob: Job? = null
    private var highScoreTextAnimationJob: Job? = null

    /**
     * Switch to settings screen
     */
    fun showSettings() {
        Log.d(TAG, "Switching to settings screen")

        // Store current state to restore when returning from settings
        previousGameState = _uiState.value.gameState

        // Cancel any active timeout timer when going to settings
        cancelTimeoutTimer()

        // Cancel any active sequence jobs
        activeSequenceJob?.cancel()
        activeSequenceJob = null


        // Switch to settings screen
        _uiState.update { it.copy(
            gameState = GameState.Settings,
            screenState = ScreenState.Settings
        ) }
    }
    
    /**
     * Switch to statistics screen
     */
    fun showStatistics() {
        Log.d(TAG, "Switching to statistics screen")
        
        // Don't override previousGameState if we're already in Settings - 
        // this preserves the original game state from when we first entered settings
        if (_uiState.value.gameState != GameState.Settings) {
            previousGameState = _uiState.value.gameState
        }
        
        // Cancel any active timeout timer when going to statistics
        cancelTimeoutTimer()
        
        // Cancel any active sequence jobs
        activeSequenceJob?.cancel()
        activeSequenceJob = null
        
        // Switch to statistics screen
        _uiState.update { it.copy(
            gameState = GameState.Settings, // Keep game state as Settings for lifecycle
            screenState = ScreenState.Statistics
        ) }
    }

    /**
     * Return from statistics to settings
     */
    fun exitStatistics() {
        Log.d(TAG, "Exiting statistics screen, returning to settings")
        
        _uiState.update { it.copy(
            gameState = GameState.Settings,
            screenState = ScreenState.Settings
        ) }
    }

    /**
     * Return from settings to game
     */
    fun exitSettings() {
        Log.d(TAG, "Exiting settings screen")

        // Only resume if app is in foreground
        if (!isAppInForeground) {
            Log.d(TAG, "App is in background, not resuming game from settings yet")
            return
        }

        when (previousGameState) {
            // If we were showing sequence when settings was opened, restart sequence display
            is GameState.ShowingSequence -> {
                Log.d(TAG, "Resuming from ShowingSequence state - restarting sequence")
                // First update state
                _uiState.update { it.copy(
                    gameState = GameState.WaitingToStart,
                    screenState = ScreenState.Game
                ) }
                // Then restart sequence display
                showSequence()
            }

            // If player was repeating a sequence, let them continue
            is GameState.PlayerRepeating -> {
                Log.d(TAG, "Resuming from PlayerRepeating state")
                _uiState.update { it.copy(
                    gameState = GameState.PlayerRepeating,
                    screenState = ScreenState.Game
                ) }
                // Reset the timeout timer when returning to the game
                resetTimeoutTimer()
            }

            // If we were in game over state, restore it
            is GameState.GameOver -> {
                Log.d(TAG, "Restoring GameOver state after returning from settings")
                _uiState.update { it.copy(
                    gameState = GameState.GameOver,
                    screenState = ScreenState.Game
                ) }
            }
            
            // If we were in a transitional state, just start a new game
            is GameState.Settings,
            is GameState.WaitingToStart -> {
                Log.d(TAG, "Starting new game after returning from settings")
                // Update screen state first
                _uiState.update { it.copy(screenState = ScreenState.Game) }
                
                // If there's no sequence yet, start a new game
                if (_uiState.value.sequence.isEmpty()) {
                    startNewGame()
                } else {
                    // If there was a sequence, go to player repeating state
                    _uiState.update { it.copy(gameState = GameState.PlayerRepeating) }
                    resetTimeoutTimer()
                }
            }
        }

        // Reset the previous state
        previousGameState = GameState.WaitingToStart
    }

    /**
     * Change sound pack
     */
    fun setSoundPack(soundPack: SoundPack) {
        Log.d(TAG, "Changing sound pack to: ${soundPack.name}")

        // Update sound manager
        soundManager.setSoundPack(soundPack)

        // Update UI state
        _uiState.update { it.copy(currentSoundPack = soundPack) }

        // Save to preferences
        saveSettings()
    }

    /**
     * Toggle vibration setting
     */
    fun setVibrationEnabled(enabled: Boolean) {
        Log.d(TAG, "Setting vibration enabled: $enabled")

        // Update sound manager
        soundManager.setVibrationEnabled(enabled)

        // Update UI state
        _uiState.update { it.copy(vibrateEnabled = enabled) }

        // Save to preferences
        saveSettings()
    }
    
    /**
     * Toggle sound enabled/disabled (mute/unmute)
     */
    fun toggleSound() {
        val newSoundEnabled = !_uiState.value.soundEnabled
        Log.d(TAG, "Toggling sound enabled: $newSoundEnabled")
        
        // Update sound manager (note the inverse relationship - soundEnabled=true means not muted)
        soundManager.setSoundMuted(!newSoundEnabled)
        
        // Update UI state
        _uiState.update { it.copy(soundEnabled = newSoundEnabled) }
        
        // Save to preferences
        saveSettings()
    }
    
    /**
     * Toggle vibration enabled/disabled
     */
    fun toggleVibration() {
        val newVibrateEnabled = !_uiState.value.vibrateEnabled
        Log.d(TAG, "Toggling vibration enabled: $newVibrateEnabled")
        
        // Update sound manager
        soundManager.setVibrationEnabled(newVibrateEnabled)
        
        // Update UI state
        _uiState.update { it.copy(vibrateEnabled = newVibrateEnabled) }
        
        // Save to preferences
        saveSettings()
    }
    
    /**
     * Toggle difficulty setting
     */
    fun setDifficultyEnabled(enabled: Boolean) {
        Log.d(TAG, "Setting difficulty enabled: $enabled")
        
        // Update UI state
        _uiState.update { it.copy(difficultyEnabled = enabled) }
        
        // Save to preferences
        saveSettings()
    }
    
    /**
     * Toggle Memory Lights+ setting (6-button mode)
     */
    fun setMemoryLightsPlusEnabled(enabled: Boolean) {
        Log.d(TAG, "Setting Memory Lights+ enabled: $enabled")
        
        // If the mode is changing, reset the current game
        if (_uiState.value.memoryLightsPlusEnabled != enabled) {
            Log.d(TAG, "Mode changed, resetting current game")
            
            // Cancel any active timers and animations
            cancelTimeoutTimer()
            activeSequenceJob?.cancel()
            activeSequenceJob = null
            gameOverTextAnimationJob?.cancel()
            gameOverTextAnimationJob = null
            highScoreTextAnimationJob?.cancel()
            highScoreTextAnimationJob = null
            
            // Update UI state with new mode and reset game
            _uiState.update { it.copy(
                memoryLightsPlusEnabled = enabled,
                gameState = GameState.WaitingToStart,
                level = 1,
                roundCount = 0,
                sequence = emptyList(),
                playerSequence = emptyList(),
                currentlyLit = null,
                allButtonsLit = false,
                showYourTurnText = false,
                showHighScoreParticles = false,
                showHighScoreText = false,
                showGameOverText = false
            )}
            
            // Save to preferences
            saveSettings()
            
            // Start a new game with the new mode if app is in foreground
            if (isAppInForeground) {
                generateNextSequence()
                showSequence()
            }
        }
    }
    
    /**
     * Calculate timing for sequence display based on difficulty setting and level
     * Base timing: 600ms button lit, 400ms pause between buttons
     * If difficulty enabled: reduce by 20% on rounds 5, 9, 13, etc.
     */
    private fun calculateSequenceTiming(): Pair<Long, Long> {
        val currentLevel = _uiState.value.level
        val difficultyEnabled = _uiState.value.difficultyEnabled
        
        // Base timing values
        val baseLitDuration = 600L
        val basePauseDuration = 400L
        
        if (!difficultyEnabled) {
            return Pair(baseLitDuration, basePauseDuration)
        }
        
        // Calculate speed increases - 20% reduction on rounds 5, 9, 13, etc.
        // This means rounds where (level - 1) % 4 == 0 and level >= 5
        val speedIncreases = if (currentLevel >= 5 && (currentLevel - 1) % 4 == 0) {
            (currentLevel - 1) / 4
        } else if (currentLevel > 5) {
            (currentLevel - 1) / 4
        } else {
            0
        }
        
        // Each speed increase reduces timing by 20%
        val reductionFactor = 1.0 - (speedIncreases * 0.2)
        
        // Ensure minimum timing (don't go below 200ms for lit, 150ms for pause)
        val litDuration = maxOf(200L, (baseLitDuration * reductionFactor).toLong())
        val pauseDuration = maxOf(150L, (basePauseDuration * reductionFactor).toLong())
        
        Log.d(TAG, "Level $currentLevel, Speed increases: $speedIncreases, Timing: ${litDuration}ms lit, ${pauseDuration}ms pause")
        
        return Pair(litDuration, pauseDuration)
    }
    
    /**
     * Reset the high score for the current mode and all statistics to 0
     */
    fun resetHighScore() {
        Log.d(TAG, "Resetting high score for ${if (_uiState.value.memoryLightsPlusEnabled) "6-button" else "4-button"} mode and all statistics")
        
        // Reset all statistics in the StatisticsManager
        statisticsManager.resetStatistics()
        
        // Get the reset statistics
        val resetStatistics = statisticsManager.getStatistics()
        
        // Update UI state with reset high score and statistics for current mode
        _uiState.update { currentState ->
            if (currentState.memoryLightsPlusEnabled) {
                currentState.copy(
                    highScore6Button = 0,
                    statistics = resetStatistics
                )
            } else {
                currentState.copy(
                    highScore4Button = 0,
                    statistics = resetStatistics
                )
            }
        }
        
        // Save to preferences
        saveSettings()
    }
    
    /**
     * Get current game statistics
     */
    fun getStatistics(): GameStatistics {
        return statisticsManager.getStatistics()
    }
    
    /**
     * Reset all game statistics
     */
    fun resetStatistics() {
        Log.d(TAG, "Resetting all game statistics")
        statisticsManager.resetStatistics()
        
        // Update UI state with reset statistics
        val resetStatistics = statisticsManager.getStatistics()
        _uiState.update { it.copy(statistics = resetStatistics) }
    }

    // Initialize game state for a new game without animation
    private fun initializeNewGame() {
        Log.d(TAG, "Initializing new game state")

        // Cancel any active timers to prevent unexpected behavior
        cancelTimeoutTimer()

        _uiState.update { currentState ->
            currentState.copy(
                gameState = GameState.WaitingToStart,
                level = 1,
                roundCount = 0,
                sequence = emptyList(),
                playerSequence = emptyList(),
                currentlyLit = null,
                allButtonsLit = false,
                showYourTurnText = false
            )
        }

        // Start the game by generating and showing the first sequence
        // Only if app is in foreground
        if (isAppInForeground) {
            generateNextSequence()
            showSequence()
        }
    }

    // Start a new game - public method called by UI
    fun startNewGame() {
        Log.d(TAG, "Starting new game")
        // Cancel any ongoing game over or high score text animations
        gameOverTextAnimationJob?.cancel()
        gameOverTextAnimationJob = null
        highScoreTextAnimationJob?.cancel()
        highScoreTextAnimationJob = null
        
        // Clear particle effects, high score text, game over text, and YOUR TURN text when starting new game
        _uiState.update { it.copy(showHighScoreParticles = false, showHighScoreText = false, showGameOverText = false, showYourTurnText = false) }
        // No startup animation on manual game restart
        initializeNewGame()
    }
    
    // Clear particle effects (called when animation completes)
    fun clearParticleEffects() {
        Log.d(TAG, "Clearing particle effects")
        _uiState.update { it.copy(showHighScoreParticles = false) }
    }
    
    
    // Trigger particle effects for testing
    fun triggerParticleEffects() {
        Log.d(TAG, "Triggering particle effects for testing")
        _uiState.update { it.copy(showHighScoreParticles = true) }
    }

    // Play a startup animation by lighting up each button in sequence
    private fun playStartupAnimation(onComplete: () -> Unit) {
        Log.d(TAG, "Playing startup animation")

        // Don't play animation if app is in background
        if (!isAppInForeground) {
            Log.d(TAG, "App is in background, skipping startup animation")
            onComplete()
            return
        }

        // The buttons in order for the startup animation
        val buttonsInOrder = SimonButton.getAvailableButtons(_uiState.value.memoryLightsPlusEnabled)

        // Cancel any existing animations
        activeSequenceJob?.cancel()

        // Start and track new animation
        activeSequenceJob = viewModelScope.launch {
            delay(500)
            // Flash each button in order
            buttonsInOrder.forEach { button ->
                // Check if app is still in foreground before each step
                if (!isAppInForeground) {
                    Log.d(TAG, "App went to background during startup animation, cancelling")
                    // Use this instead of cancel()
                    activeSequenceJob?.cancel()
                    return@launch
                }

                Log.d(TAG, "Startup animation: lighting up $button")
                _uiState.update { it.copy(currentlyLit = button) }
                soundManager.playSound(button)
                delay(300)
                _uiState.update { it.copy(currentlyLit = null) }
                delay(150)
            }

            // Slight pause before starting the game
            delay(500)

            // Call the completion handler
            onComplete()

            // Clear the active job reference
            activeSequenceJob = null
        }
    }

    // Add a new button to the sequence
    private fun generateNextSequence() {
        val availableButtons = SimonButton.getAvailableButtons(_uiState.value.memoryLightsPlusEnabled)
        val newButton = availableButtons.random()
        Log.d(TAG, "Adding new button to sequence: $newButton (mode: ${if(_uiState.value.memoryLightsPlusEnabled) "6-button" else "4-button"})")

        _uiState.update { currentState ->
            currentState.copy(
                sequence = currentState.sequence + newButton
            )
        }
        Log.d(TAG, "Sequence is now: ${_uiState.value.sequence}")
    }

    // Display the sequence to the player
    private fun showSequence() {
        Log.d(TAG, "Showing sequence of length: ${_uiState.value.sequence.size}")

        // Don't show sequence if app is in background
        if (!isAppInForeground) {
            Log.d(TAG, "App is in background, not showing sequence now")
            return
        }

        // Calculate dynamic timing based on difficulty setting and level
        val (litDuration, pauseDuration) = calculateSequenceTiming()

        _uiState.update { it.copy(gameState = GameState.ShowingSequence) }

        // Cancel any existing sequence job
        activeSequenceJob?.cancel()

        // Start a new sequence job and save the reference
        activeSequenceJob = viewModelScope.launch {
            delay(500) // Brief pause before showing sequence

            // Light up each button in the sequence and play sound
            _uiState.value.sequence.forEachIndexed { index, button ->
                // Check if app is still in foreground
                if (!isAppInForeground) {
                    Log.d(TAG, "App went to background while showing sequence, cancelling")
                    // Use this instead of cancel()
                    activeSequenceJob?.cancel()
                    return@launch
                }

                Log.d(TAG, "Showing sequence item $index: $button")

                // Update UI state to light the button
                _uiState.update { it.copy(currentlyLit = button) }

                // Play button sound
                soundManager.playSound(button)

                // Keep lit for sound duration
                delay(litDuration)

                // Turn off light
                _uiState.update { it.copy(currentlyLit = null) }

                // Pause between buttons
                delay(pauseDuration)
            }

            // After showing sequence, let player repeat it
            Log.d(TAG, "Finished showing sequence, now player's turn")
            val shouldShowYourTurn = _uiState.value.roundCount < 3 // Show for first 3 rounds (0-2)
            _uiState.update {
                it.copy(
                    gameState = GameState.PlayerRepeating,
                    playerSequence = emptyList(),
                    showYourTurnText = shouldShowYourTurn
                )
            }

            // Start the inactivity timeout timer
            startTimeoutTimer()

            // Clear the active job reference since it's completed
            activeSequenceJob = null
        }
    }

    // Handle player button presses
    fun onButtonClick(button: SimonButton, isPress: Boolean) {
        Log.d(TAG, "Button ${if (isPress) "press" else "release"}: $button")

        // Ignore button presses if app is in background
        if (!isAppInForeground) {
            Log.d(TAG, "App is in background, ignoring button press")
            return
        }

        if (isPress) {
            // Handle press event

            // Ignore new presses when not in player repeating state
            if (_uiState.value.gameState != GameState.PlayerRepeating) {
                Log.d(TAG, "Button press ignored - not in PlayerRepeating state: ${_uiState.value.gameState}")
                return
            }

            // Check if this is the first button pressed in the current interaction
            val isFirstButtonPressed = _uiState.value.activeButtonPresses.isEmpty()

            // Update active button presses map
            val activeButtons = _uiState.value.activeButtonPresses.toMutableMap()
            activeButtons[button] = true
            _uiState.update { it.copy(activeButtonPresses = activeButtons) }

            // Only process game logic and play sound for the first button press
            if (isFirstButtonPressed) {
                // Reset the timeout timer since the player has acted
                resetTimeoutTimer()

                // Add button to player's sequence
                val updatedPlayerSequence = _uiState.value.playerSequence + button
                _uiState.update { it.copy(
                    playerSequence = updatedPlayerSequence,
                    // Set the button as lit
                    currentlyLit = button,
                    // Hide YOUR TURN text when player starts pressing buttons
                    showYourTurnText = false
                )}

                // Play the button sound
                soundManager.playSound(button, isPlayerPressed = true)

                // Schedule turning off the light after a delay
                viewModelScope.launch {
                    delay(300) // Match sound duration
                    // Only clear if this button is still the current one lit
                    if (_uiState.value.currentlyLit == button) {
                        _uiState.update { it.copy(currentlyLit = null) }
                    }
                }

                // Now check the sequence
                checkSequenceMatch(updatedPlayerSequence)
            }
        } else {
            // Handle button release event
            val activeButtons = _uiState.value.activeButtonPresses.toMutableMap()
            activeButtons.remove(button)
            _uiState.update { it.copy(activeButtonPresses = activeButtons) }
        }
    }

    private fun checkSequenceMatch(playerSequence: List<SimonButton>) {
        val index = playerSequence.size - 1

        // Safety check: make sure index is valid for both sequences
        if (index < 0 || index >= _uiState.value.sequence.size) {
            Log.e(TAG, "Invalid index $index: player sequence=${playerSequence.size}, game sequence=${_uiState.value.sequence.size}")

            // Player has clicked beyond the expected sequence length
            // This is a game over condition - the player made an error by entering too many buttons
            viewModelScope.launch {
                delay(300)
                handleGameOver("Game over - player entered too many buttons")
            }
            return
        }

        if (playerSequence[index] != _uiState.value.sequence[index]) {
            // Wrong button
            Log.d(TAG, "Wrong button selected! Expected: ${_uiState.value.sequence[index]}, Got: ${playerSequence[index]}")

            // Brief delay before game over to allow button sound to play
            viewModelScope.launch {
                delay(300)
                // Wrong button - game over
                handleGameOver()
            }
            return
        }

        // Correct button
        Log.d(TAG, "Correct button! ${playerSequence.size}/${_uiState.value.sequence.size} steps completed")

        // Check if player completed the entire sequence
        if (playerSequence.size == _uiState.value.sequence.size) {
            Log.d(TAG, "Player completed the entire sequence! Advancing to next level")
            // Cancel timeout timer as level is complete
            cancelTimeoutTimer()

            // Move to next level
            advanceToNextLevel()
        }
    }

    // Handle button release events
    fun onButtonRelease(button: SimonButton) {
        // Remove button from active presses
        val activeButtons = _uiState.value.activeButtonPresses.toMutableMap()
        activeButtons.remove(button)

        _uiState.update { it.copy(activeButtonPresses = activeButtons) }
    }

    // Handle game over state - Simplified version that calls the main implementation
    private fun handleGameOver() {
        handleGameOver("Wrong button pressed")
    }

    // Flash all buttons to indicate game over
    private fun flashAllButtons() {
        Log.d(TAG, "Flashing all buttons for game over animation")

        // Don't flash if app is in background
        if (!isAppInForeground) {
            Log.d(TAG, "App is in background, skipping game over animation")
            return
        }

        // Cancel any existing sequence animation
        activeSequenceJob?.cancel()

        // Start and track the flashing animation
        activeSequenceJob = viewModelScope.launch {
            // Flash all buttons 3 times
            repeat(3) { flashCount ->
                // Check if app is still in foreground
                if (!isAppInForeground) {
                    Log.d(TAG, "App went to background during game over animation, cancelling")
                    // Use this instead of cancel()
                    activeSequenceJob?.cancel()
                    return@launch
                }

                Log.d(TAG, "Flash sequence $flashCount")

                // Turn all buttons on
                _uiState.update { it.copy(allButtonsLit = true) }
                delay(300)

                // Turn all buttons off
                _uiState.update { it.copy(allButtonsLit = false) }
                delay(300)
            }

            // Clear the job reference when done
            activeSequenceJob = null
        }
    }

    // Advance to the next level
    private fun advanceToNextLevel() {
        Log.d(TAG, "Advancing to level ${_uiState.value.level + 1}")
        _uiState.update { it.copy(
            level = it.level + 1,
            roundCount = it.roundCount + 1,
            gameState = GameState.ShowingSequence,
            playerSequence = emptyList()
        ) }

        // Generate new sequence that includes the previous one
        generateNextSequence()

        // Cancel any existing sequence job
        activeSequenceJob?.cancel()

        // Brief delay before showing new sequence
        activeSequenceJob = viewModelScope.launch {
            delay(1000)

            // Check if app is still in foreground
            if (isAppInForeground) {
                showSequence()
            } else {
                Log.d(TAG, "App went to background before showing new sequence")
            }

            // Clear job reference after showSequence (which sets its own reference)
            activeSequenceJob = null
        }
    }

    // Start a timer that will end the game if the player doesn't act within the timeout period
    private fun startTimeoutTimer() {
        Log.d(TAG, "Starting player inactivity timeout timer (${playerTimeoutDuration/1000} seconds)")

        // Don't start timer if app is in background
        if (!isAppInForeground) {
            Log.d(TAG, "App is in background, not starting timeout timer")
            return
        }

        // Cancel any existing timer first
        cancelTimeoutTimer()

        // Start a new timer
        timeoutJob = viewModelScope.launch {
            delay(playerTimeoutDuration)

            // Check if app is still in foreground
            if (!isAppInForeground) {
                Log.d(TAG, "App is in background when timeout occurred, ignoring")
                return@launch
            }

            // If this code executes, the timeout has occurred
            Log.d(TAG, "Player timeout! No button pressed for ${playerTimeoutDuration/1000} seconds")

            // Ensure we're still in PlayerRepeating state (could have changed during the delay)
            if (_uiState.value.gameState == GameState.PlayerRepeating) {
                // Play timout sound and end game
                soundManager.playErrorSound()
                viewModelScope.launch {
                    delay(300)
                    // Handle game over due to timeout
                    handleGameOver("Timeout - no button pressed for ${playerTimeoutDuration/1000} seconds")
                }
            }
        }
    }

    // Reset the timeout timer (when a player presses a button)
    // Public to allow resetting from Activity during configuration changes
    fun resetTimeoutTimer() {
        // Only reset if app is in foreground
        if (isAppInForeground) {
            Log.d(TAG, "Resetting player inactivity timeout timer")
            startTimeoutTimer() // Cancel and start a new timeout
        }
    }

    // Cancel the timeout timer
    private fun cancelTimeoutTimer() {
        timeoutJob?.cancel()
        timeoutJob = null
    }

    // Handle game over state with an optional reason
    private fun handleGameOver(reason: String = "Game over") {
        // Cancel any running timeout timer
        cancelTimeoutTimer()
        val currentLevel = _uiState.value.level
        val currentHighScore = _uiState.value.currentHighScore
        Log.d(TAG, "$reason at level $currentLevel (high score: $currentHighScore for ${if (_uiState.value.memoryLightsPlusEnabled) "6-button" else "4-button"} mode)")

        // Don't do game over animation if app is in background
        if (!isAppInForeground) {
            Log.d(TAG, "App is in background, skipping game over animation")
            // Just update the game state immediately
            updateGameOverState(currentLevel, currentHighScore, false)
            return
        }

        // Check if this is a new high score
        val isNewHighScore = currentLevel > currentHighScore
        val newHighScore = if (isNewHighScore) currentLevel else currentHighScore

        // Play error sound when game is over
        soundManager.playErrorSound()

        // Flash all buttons sequence to indicate game over
        flashAllButtons()

        // Update game state after the flashing animation
        viewModelScope.launch {
            // Wait for flash animation to complete (in flashAllButtons)
            delay(2000)

            updateGameOverState(currentLevel, newHighScore, isNewHighScore)
        }
    }

    // Helper function to update the game state after game over
    private fun updateGameOverState(currentLevel: Int, newHighScore: Int, isNewHighScore: Boolean) {
        _uiState.update { currentState ->
            if (currentState.memoryLightsPlusEnabled) {
                currentState.copy(
                    gameState = GameState.GameOver,
                    highScore6Button = newHighScore,
                    showHighScoreParticles = isNewHighScore,
                    showHighScoreText = isNewHighScore,
                    showGameOverText = !isNewHighScore // Show GAME OVER when NOT a new high score
                )
            } else {
                currentState.copy(
                    gameState = GameState.GameOver,
                    highScore4Button = newHighScore,
                    showHighScoreParticles = isNewHighScore,
                    showHighScoreText = isNewHighScore,
                    showGameOverText = !isNewHighScore // Show GAME OVER when NOT a new high score
                )
            }
        }

        // If it's a new high score, start text flash animation
        if (isNewHighScore) {
            startHighScoreTextAnimation()
        } else {
            // If it's not a new high score, start GAME OVER text animation
            startGameOverTextAnimation()
        }

        // Record game result in statistics
        statisticsManager.recordGameResult(currentLevel, _uiState.value.sequence.size)
        
        // Update UI state with refreshed statistics
        val updatedStatistics = statisticsManager.getStatistics()
        _uiState.update { it.copy(statistics = updatedStatistics) }
        
        // Save settings if it's a new high score (always save when there's a change)
        if (isNewHighScore) {
            saveSettings()
        }
    }
    
    // Start the HIGH SCORE text flash animation
    private fun startHighScoreTextAnimation() {
        Log.d(TAG, "Starting HIGH SCORE text flash animation")
        
        // Cancel any existing animation
        highScoreTextAnimationJob?.cancel()
        
        highScoreTextAnimationJob = viewModelScope.launch {
            // Flash the text for 3 seconds
            repeat(6) { // 6 flashes over 3 seconds
                delay(250) // Flash on for 250ms
                _uiState.update { it.copy(showHighScoreText = false) }
                delay(250) // Flash off for 250ms  
                _uiState.update { it.copy(showHighScoreText = true) }
            }
            
            // Keep text visible for another 2 seconds
            delay(2000)
            
            // Hide the text
            _uiState.update { it.copy(showHighScoreText = false) }
            
            // Clear job reference when done
            highScoreTextAnimationJob = null
        }
    }
    
    // Show solid GAME OVER text (no animation)
    private fun startGameOverTextAnimation() {
        Log.d(TAG, "Showing solid GAME OVER text")
        
        // Cancel any existing animation
        gameOverTextAnimationJob?.cancel()
        
        gameOverTextAnimationJob = viewModelScope.launch {
            // Keep text visible for 5 seconds without flashing
            delay(5000)
            
            // Hide the text
            _uiState.update { it.copy(showGameOverText = false) }
            
            // Clear job reference when done
            gameOverTextAnimationJob = null
        }
    }

    // Lifecycle methods

    // Called when app goes to foreground
    override fun onResume(owner: LifecycleOwner) {
        Log.d(TAG, "onResume - App coming to foreground")
        isAppInForeground = true

        // Resume sounds
        soundManager.resumeSounds()

        // Check if we need to resume the game
        if (wasGameActiveBeforeBackground) {
            Log.d(TAG, "Resuming game from previous state: $gameStateBeforeBackground")
            wasGameActiveBeforeBackground = false

            when (gameStateBeforeBackground) {
                is GameState.ShowingSequence -> {
                    // Restart showing sequence
                    showSequence()
                }
                is GameState.PlayerRepeating -> {
                    // Return to player's turn and restart timeout
                    _uiState.update { it.copy(gameState = GameState.PlayerRepeating) }
                    resetTimeoutTimer()
                }
                is GameState.GameOver, is GameState.WaitingToStart -> {
                    // No special handling needed
                }
                is GameState.Settings -> {
                    // Stay in settings
                }
            }
        }
    }

    // Called when app goes to background
    override fun onPause(owner: LifecycleOwner) {
        Log.d(TAG, "onPause - App going to background")
        isAppInForeground = false

        // Save current game state to restore later
        gameStateBeforeBackground = _uiState.value.gameState
        wasGameActiveBeforeBackground = _uiState.value.gameState is GameState.ShowingSequence ||
                _uiState.value.gameState is GameState.PlayerRepeating

        // Cancel any active animations
        activeSequenceJob?.cancel()
        activeSequenceJob = null
        gameOverTextAnimationJob?.cancel()
        gameOverTextAnimationJob = null
        highScoreTextAnimationJob?.cancel()
        highScoreTextAnimationJob = null

        // Cancel timeout timer
        cancelTimeoutTimer()

        // Pause all sounds
        soundManager.pauseSounds()

        // Turn off any lit buttons
        _uiState.update { it.copy(
            currentlyLit = null,
            allButtonsLit = false
        )}
    }

    // Clean up resources when ViewModel is cleared
    override fun onCleared() {
        Log.d(TAG, "ViewModel cleared, releasing sound resources")
        super.onCleared()
        saveSettings()
        cancelTimeoutTimer() // Make sure to cancel any timers

        // Cancel any active animations or sequences
        activeSequenceJob?.cancel()
        activeSequenceJob = null
        gameOverTextAnimationJob?.cancel()
        gameOverTextAnimationJob = null
        highScoreTextAnimationJob?.cancel()
        highScoreTextAnimationJob = null

        soundManager.release()
    }
}