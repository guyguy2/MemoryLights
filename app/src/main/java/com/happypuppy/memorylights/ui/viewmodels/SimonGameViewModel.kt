package com.happypuppy.memorylights.ui.viewmodels

import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happypuppy.memorylights.data.manager.SimonSoundManager
import com.happypuppy.memorylights.data.repository.StatisticsRepository
import com.happypuppy.memorylights.data.repository.SettingsRepository
import com.happypuppy.memorylights.domain.GameConstants
import com.happypuppy.memorylights.domain.calculateSequenceTiming
import com.happypuppy.memorylights.domain.enums.GameMode
import com.happypuppy.memorylights.domain.enums.SimonButton
import com.happypuppy.memorylights.domain.model.GameStatistics
import com.happypuppy.memorylights.domain.enums.SoundPack
import com.happypuppy.memorylights.domain.model.GameState
import com.happypuppy.memorylights.domain.model.SimonGameUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Memory Lights game logic with lifecycle awareness
 */
class SimonGameViewModel(
    private val soundManager: SimonSoundManager,
    private val statisticsRepository: StatisticsRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel(), DefaultLifecycleObserver {

    companion object {
        private const val TAG = "SimonGameViewModel"
    }

    // Flag to track if startup animation has been played in this session
    private var hasPlayedStartupAnimation = false

    // Job to track the timeout timer
    private var timeoutJob: Job? = null

    // Track if the app is currently in foreground
    private var isAppInForeground = true

    // Track if the game was active before going to background
    private var wasGameActiveBeforeBackground = false

    // Track the game state before going to background
    private var gameStateBeforeBackground: GameState = GameState.WaitingToStart

    // Private and public state flows
    private val _uiState = MutableStateFlow(SimonGameUiState())
    val uiState: StateFlow<SimonGameUiState> = _uiState.asStateFlow()

    // Buttons currently held by the player. Used to debounce simultaneous touches —
    // not exposed in UiState because the UI tracks its own visual press state.
    // Main-thread only: mutated exclusively by [onButtonClick] (annotated @MainThread)
    // and viewModelScope launches default to Dispatchers.Main.immediate.
    private val activePresses = mutableSetOf<SimonButton>()

    // Cursor for the sound-pack preview tone. Advances each time the player
    // taps a pack so they audition multiple colors rather than only GREEN
    // every time (#63). Wraps around the available button list.
    private var previewToneIndex = 0

    init {
        Log.d(TAG, "Initializing SimonGameViewModel")

        // Set up sound loading callback
        soundManager.setOnSoundsLoadedListener { success, error ->
            Log.d(TAG, "Sounds loaded callback: success=$success, error=$error")
            _uiState.update { it.copy(
                soundsLoaded = success,
                soundLoadError = error
            )}
        }

        // Check if sounds are already loaded (in case callback was missed)
        if (soundManager.areSoundsLoaded()) {
            _uiState.update { it.copy(
                soundsLoaded = true,
                soundLoadError = soundManager.getLoadError()
            )}
        }

        // Continuously sync statistics from repository so any write is reflected in UI
        statisticsRepository.statisticsFlow
            .onEach { stats -> _uiState.update { it.copy(statistics = stats) } }
            .launchIn(viewModelScope)

        // Load initial settings off the main thread, then start the game.
        viewModelScope.launch {
            loadInitialSettings()

            // Play startup animation once when app starts, then start the game
            playStartupAnimation {
                hasPlayedStartupAnimation = true
                initializeNewGame()
            }
        }
    }

    /**
     * Load the first emission of saved settings into UI state and the sound manager.
     * Suspends until DataStore returns the persisted snapshot, so this MUST be called
     * from a coroutine — do not block the main thread.
     */
    private suspend fun loadInitialSettings() {
        val settings = settingsRepository.settingsFlow.first()

        Log.d(TAG, "Loaded settings - Sound Pack: ${settings.soundPack}, High Score 4-button: ${settings.highScore4Button}, High Score 6-button: ${settings.highScore6Button}, Vibrate: ${settings.vibrateEnabled}, Sound Enabled: ${settings.soundEnabled}, Difficulty: ${settings.difficultyEnabled}, Memory Lights+: ${settings.memoryLightsPlusEnabled}")

        // Update sound manager with saved settings
        soundManager.setSoundPack(settings.soundPack)
        soundManager.setVibrationEnabled(settings.vibrateEnabled)
        soundManager.setSoundMuted(!settings.soundEnabled)

        // Update UI state with saved settings
        _uiState.update { it.copy(
            currentSoundPack = settings.soundPack,
            highScore4Button = settings.highScore4Button,
            highScore6Button = settings.highScore6Button,
            vibrateEnabled = settings.vibrateEnabled,
            soundEnabled = settings.soundEnabled,
            difficultyEnabled = settings.difficultyEnabled,
            memoryLightsPlusEnabled = settings.memoryLightsPlusEnabled,
            playerTimeoutSeconds = settings.playerTimeoutSeconds,
            practiceModeEnabled = settings.practiceModeEnabled,
            reverseModeEnabled = settings.reverseModeEnabled,
            audioOnlyModeEnabled = settings.audioOnlyModeEnabled,
            gameMode = settings.gameMode,
            bestBlitzTime4ButtonMs = settings.bestBlitzTime4ButtonMs,
            bestBlitzTime6ButtonMs = settings.bestBlitzTime6ButtonMs
        )}
    }


    // Track previous game state before entering settings
    private var previousGameState: GameState = GameState.WaitingToStart

    // Track active coroutine jobs that need to be paused/resumed
    private var activeSequenceJob: Job? = null
    private var gameOverTextAnimationJob: Job? = null
    private var highScoreTextAnimationJob: Job? = null
    private var speedUpTextJob: Job? = null

    /**
     * Called by the UI right before navigating to the Settings route.
     * Captures the current game state so [onReturnToGame] can restore it,
     * cancels in-flight animations, and pauses the inactivity timer.
     * Navigation itself is owned by the UI's `NavController`.
     */
    fun showSettings() {
        Log.d(TAG, "Game pausing for settings navigation")

        previousGameState = _uiState.value.gameState

        cancelTimeoutTimer()
        activeSequenceJob?.cancel()
        activeSequenceJob = null
    }

    /**
     * Called by the UI right before navigating to the Statistics route.
     * Always reached from Settings, so the original [previousGameState]
     * captured by [showSettings] is preserved.
     */
    fun showStatistics() {
        Log.d(TAG, "Game pausing for statistics navigation")

        cancelTimeoutTimer()
        activeSequenceJob?.cancel()
        activeSequenceJob = null
    }

    /**
     * Called by the UI when navigating back from Statistics to Settings.
     * No-op today — kept as an extension point so the back-navigation
     * call site stays symmetric with [showStatistics].
     */
    fun exitStatistics() {
        Log.d(TAG, "Returning from statistics to settings")
    }

    /**
     * Called by the UI when navigation lands back on the Game route after
     * a Settings (or Settings → Statistics) detour. Restores the game
     * phase that was active before the player opened Settings.
     */
    fun onReturnToGame() {
        Log.d(TAG, "Resuming game after navigation back")

        // Only resume if app is in foreground
        if (!isAppInForeground) {
            Log.d(TAG, "App is in background, not resuming game from settings yet")
            return
        }

        when (previousGameState) {
            // If we were showing sequence when settings was opened, restart sequence display
            is GameState.ShowingSequence -> {
                Log.d(TAG, "Resuming from ShowingSequence state - restarting sequence")
                _uiState.update { it.copy(gameState = GameState.WaitingToStart) }
                showSequence()
            }

            // If player was repeating a sequence, let them continue
            is GameState.PlayerRepeating -> {
                Log.d(TAG, "Resuming from PlayerRepeating state")
                _uiState.update { it.copy(gameState = GameState.PlayerRepeating) }
                resetTimeoutTimer()
            }

            // If we were in game over state, restore it
            is GameState.GameOver -> {
                Log.d(TAG, "Restoring GameOver state after returning from settings")
                _uiState.update { it.copy(gameState = GameState.GameOver) }
            }

            // If we were in a transitional state, just start a new game
            is GameState.WaitingToStart -> {
                Log.d(TAG, "Starting new game after returning from settings")

                if (_uiState.value.sequence.isEmpty()) {
                    startNewGame()
                } else {
                    _uiState.update { it.copy(gameState = GameState.PlayerRepeating) }
                    resetTimeoutTimer()
                }
            }

            // If we were paused when settings opened, stay paused on return.
            is GameState.Paused -> {
                Log.d(TAG, "Restoring Paused state after returning from settings")
                _uiState.update { it.copy(gameState = GameState.Paused) }
            }
        }

        // Reset the previous state
        previousGameState = GameState.WaitingToStart
    }

    /**
     * Change sound pack and play a short preview tone so the player can audition it.
     * Called from the Settings screen on selection.
     */
    fun setSoundPack(soundPack: SoundPack) {
        Log.d(TAG, "Changing sound pack to: ${soundPack.name}")

        soundManager.setSoundPack(soundPack)
        _uiState.update { it.copy(currentSoundPack = soundPack) }

        // Cycle the preview tone across the available buttons so the player
        // hears the pack's full character, not always GREEN.
        val availableButtons = SimonButton.getAvailableButtons(_uiState.value.memoryLightsPlusEnabled)
        val previewButton = availableButtons[previewToneIndex % availableButtons.size]
        previewToneIndex = (previewToneIndex + 1) % availableButtons.size
        soundManager.playSound(previewButton)

        settingsRepository.setSoundPack(soundPack)
    }

    /**
     * Toggle vibration setting
     */
    fun setVibrationEnabled(enabled: Boolean) {
        Log.d(TAG, "Setting vibration enabled: $enabled")
        soundManager.setVibrationEnabled(enabled)
        _uiState.update { it.copy(vibrateEnabled = enabled) }
        settingsRepository.setVibrateEnabled(enabled)
    }

    /**
     * Toggle sound enabled/disabled (mute/unmute)
     */
    fun toggleSound() {
        val newSoundEnabled = !_uiState.value.soundEnabled
        Log.d(TAG, "Toggling sound enabled: $newSoundEnabled")
        soundManager.setSoundMuted(!newSoundEnabled)
        _uiState.update { it.copy(soundEnabled = newSoundEnabled) }
        settingsRepository.setSoundEnabled(newSoundEnabled)
    }

    /**
     * Toggle vibration enabled/disabled
     */
    fun toggleVibration() {
        val newVibrateEnabled = !_uiState.value.vibrateEnabled
        Log.d(TAG, "Toggling vibration enabled: $newVibrateEnabled")
        soundManager.setVibrationEnabled(newVibrateEnabled)
        _uiState.update { it.copy(vibrateEnabled = newVibrateEnabled) }
        settingsRepository.setVibrateEnabled(newVibrateEnabled)
    }

    /**
     * Pause the player's turn. No-op unless the game is in [GameState.PlayerRepeating].
     *
     * Cancels the inactivity timer; the countdown ring disappears (it only renders
     * during PlayerRepeating). On resume, the timer restarts with the full configured
     * duration — pause is not "freeze remaining time," it's "give me a break." This
     * is generous, but pause is a rare event and the simpler model is easier to
     * reason about. See F14 in specs/improvements.md.
     */
    fun pauseGame() {
        if (_uiState.value.gameState != GameState.PlayerRepeating) {
            Log.d(TAG, "pauseGame called outside PlayerRepeating (state=${_uiState.value.gameState}) — ignoring")
            return
        }
        Log.d(TAG, "Pausing game")
        cancelTimeoutTimer()
        _uiState.update { it.copy(
            gameState = GameState.Paused,
            currentlyLit = null,
            showYourTurnText = false,
            showSpeedUpText = false
        )}
    }

    /**
     * Resume from [GameState.Paused] back to [GameState.PlayerRepeating]. Restarts
     * the inactivity timer fresh (full duration); the ring re-renders and re-drains
     * via the existing `timeoutResetTick` bump in [startTimeoutTimer].
     */
    fun resumeGame() {
        if (_uiState.value.gameState != GameState.Paused) {
            Log.d(TAG, "resumeGame called when not Paused (state=${_uiState.value.gameState}) — ignoring")
            return
        }
        if (!isAppInForeground) {
            Log.d(TAG, "resumeGame called while app in background — ignoring")
            return
        }
        Log.d(TAG, "Resuming game")
        _uiState.update { it.copy(gameState = GameState.PlayerRepeating) }
        startTimeoutTimer()
    }

    /**
     * Set the inactivity timeout (5/10/15/30 sec). If the player is currently
     * repeating a sequence, the running timer is restarted at the new duration
     * so the change feels immediate (and the ring re-drains).
     */
    fun setPlayerTimeoutSeconds(seconds: Int) {
        Log.d(TAG, "Setting player timeout: $seconds sec")
        _uiState.update { it.copy(playerTimeoutSeconds = seconds) }
        settingsRepository.setPlayerTimeoutSeconds(seconds)
        if (_uiState.value.gameState == GameState.PlayerRepeating) {
            resetTimeoutTimer()
        }
    }

    /**
     * Toggle Practice Mode (F15). When enabled, a wrong button does not end
     * the game — the sequence replays from the start so the player can try
     * again. High scores never advance in practice mode because the game-
     * over → recordGameResult path is never taken.
     */
    fun setPracticeModeEnabled(enabled: Boolean) {
        Log.d(TAG, "Setting practice mode enabled: $enabled")
        _uiState.update { it.copy(practiceModeEnabled = enabled) }
        settingsRepository.setPracticeModeEnabled(enabled)
    }

    /**
     * Toggle Reverse Mode (F1). When enabled, the player must repeat the
     * sequence in reverse order — the watch-then-recall puzzle inverts.
     * The display still plays forward; only the comparison flips.
     */
    fun setReverseModeEnabled(enabled: Boolean) {
        Log.d(TAG, "Setting reverse mode enabled: $enabled")
        _uiState.update { it.copy(reverseModeEnabled = enabled) }
        settingsRepository.setReverseModeEnabled(enabled)
    }

    /**
     * Toggle Audio-Only Mode (F3). When enabled, the watch phase plays only
     * sounds — buttons stay dark — so the player must recognize the pack's
     * tones. Player presses still light up normally for tactile feedback.
     */
    fun setAudioOnlyModeEnabled(enabled: Boolean) {
        Log.d(TAG, "Setting audio-only mode enabled: $enabled")
        _uiState.update { it.copy(audioOnlyModeEnabled = enabled) }
        settingsRepository.setAudioOnlyModeEnabled(enabled)
    }

    /**
     * Switch between Classic and Speed Blitz (F4). Always resets the current
     * run because the rules change; persists the new mode.
     */
    fun setGameMode(mode: GameMode) {
        if (_uiState.value.gameMode == mode) return
        Log.d(TAG, "Setting game mode: $mode")

        cancelTimeoutTimer()
        activeSequenceJob?.cancel()
        activeSequenceJob = null
        gameOverTextAnimationJob?.cancel()
        gameOverTextAnimationJob = null
        highScoreTextAnimationJob?.cancel()
        highScoreTextAnimationJob = null
        speedUpTextJob?.cancel()
        speedUpTextJob = null

        _uiState.update { it.copy(
            gameMode = mode,
            gameState = GameState.WaitingToStart,
            level = 1,
            roundCount = 0,
            sequence = emptyList(),
            playerSequence = emptyList(),
            currentlyLit = null,
            allButtonsLit = false,
            showYourTurnText = false,
            showSpeedUpText = false,
            showHighScoreParticles = false,
            showHighScoreText = false,
            showGameOverText = false,
            blitzStartTimeMs = 0L,
            blitzElapsedMs = 0L,
            blitzWon = false
        )}

        settingsRepository.setGameMode(mode)

        if (isAppInForeground) {
            generateNextSequence()
            showSequence()
        }
    }

    /**
     * Toggle difficulty setting
     */
    fun setDifficultyEnabled(enabled: Boolean) {
        Log.d(TAG, "Setting difficulty enabled: $enabled")
        _uiState.update { it.copy(difficultyEnabled = enabled) }
        settingsRepository.setDifficultyEnabled(enabled)
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
            speedUpTextJob?.cancel()
            speedUpTextJob = null

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
                showSpeedUpText = false,
                showHighScoreParticles = false,
                showHighScoreText = false,
                showGameOverText = false,
                blitzStartTimeMs = 0L,
                blitzElapsedMs = 0L,
                blitzWon = false
            )}
            
            settingsRepository.setMemoryLightsPlusEnabled(enabled)

            // Start a new game with the new mode if app is in foreground
            if (isAppInForeground) {
                generateNextSequence()
                showSequence()
            }
        }
    }
    
    private fun currentSequenceTiming(): Pair<Long, Long> {
        val state = _uiState.value
        val timing = calculateSequenceTiming(state.level, state.difficultyEnabled)
        Log.d(TAG, "Level ${state.level}, timing: ${timing.first}ms lit, ${timing.second}ms pause")
        return timing
    }
    
    /**
     * Reset the high score for the current mode and all statistics to 0
     */
    fun resetHighScore() {
        Log.d(TAG, "Resetting high score for ${if (_uiState.value.memoryLightsPlusEnabled) "6-button" else "4-button"} mode and all statistics")

        // Reset all statistics in the StatisticsRepository (statisticsFlow collector
        // updates uiState.statistics asynchronously)
        statisticsRepository.resetStatistics()

        // Optimistically zero the per-mode high score so the UI reflects the reset
        // without waiting for DataStore to round-trip.
        if (_uiState.value.memoryLightsPlusEnabled) {
            _uiState.update { it.copy(highScore6Button = 0) }
            settingsRepository.setHighScore6Button(0)
        } else {
            _uiState.update { it.copy(highScore4Button = 0) }
            settingsRepository.setHighScore4Button(0)
        }
    }

    /**
     * Get current game statistics from the cached UI state snapshot.
     */
    fun getStatistics(): GameStatistics = _uiState.value.statistics

    /**
     * Reset all game statistics. The statisticsFlow collector updates UI state
     * once DataStore has cleared.
     */
    fun resetStatistics() {
        Log.d(TAG, "Resetting all game statistics")
        statisticsRepository.resetStatistics()
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
                showYourTurnText = false,
                blitzStartTimeMs = 0L,
                blitzElapsedMs = 0L,
                blitzWon = false
            )
        }

        // Start the game by generating and showing the first sequence
        // Only if app is in foreground
        if (isAppInForeground) {
            generateNextSequence()
            showSequence()
        }
    }

    /**
     * Replay the same sequence the player just lost on, without resetting
     * level / sequence — gives them a "practice this run again" option that
     * preserves the current `sequence` and `level`. If they beat it, the
     * existing `advanceToNextLevel` path takes over and the run grows
     * normally from there. No-op if there is no sequence yet (e.g. before
     * the very first round).
     */
    fun replayLastSequence() {
        val state = _uiState.value
        if (state.sequence.isEmpty()) {
            Log.d(TAG, "replayLastSequence called with empty sequence — ignoring")
            return
        }
        Log.d(TAG, "Replaying last sequence (${state.sequence.size} buttons, level ${state.level})")

        gameOverTextAnimationJob?.cancel()
        gameOverTextAnimationJob = null
        highScoreTextAnimationJob?.cancel()
        highScoreTextAnimationJob = null
        speedUpTextJob?.cancel()
        speedUpTextJob = null
        cancelTimeoutTimer()

        _uiState.update {
            it.copy(
                gameState = GameState.ShowingSequence,
                playerSequence = emptyList(),
                currentlyLit = null,
                allButtonsLit = false,
                showHighScoreParticles = false,
                showHighScoreText = false,
                showGameOverText = false,
                showYourTurnText = false,
                showSpeedUpText = false
            )
        }
        showSequence()
    }

    // Start a new game - public method called by UI
    fun startNewGame() {
        Log.d(TAG, "Starting new game")
        // Cancel any ongoing game over or high score text animations
        gameOverTextAnimationJob?.cancel()
        gameOverTextAnimationJob = null
        highScoreTextAnimationJob?.cancel()
        highScoreTextAnimationJob = null
        speedUpTextJob?.cancel()
        speedUpTextJob = null

        // Clear particle effects, high score text, game over text, and YOUR TURN text when starting new game
        _uiState.update { it.copy(
            showHighScoreParticles = false,
            showHighScoreText = false,
            showGameOverText = false,
            showYourTurnText = false,
            showSpeedUpText = false,
            blitzStartTimeMs = 0L,
            blitzElapsedMs = 0L,
            blitzWon = false
        ) }
        // No startup animation on manual game restart
        initializeNewGame()
    }
    
    // Clear particle effects (called when animation completes)
    fun clearParticleEffects() {
        Log.d(TAG, "Clearing particle effects")
        _uiState.update { it.copy(showHighScoreParticles = false) }
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
            delay(GameConstants.STARTUP_INITIAL_DELAY_MS)
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
                delay(GameConstants.STARTUP_BUTTON_LIGHT_MS)
                _uiState.update { it.copy(currentlyLit = null) }
                delay(GameConstants.STARTUP_PAUSE_MS)
            }

            // Slight pause before starting the game
            delay(GameConstants.STARTUP_INITIAL_DELAY_MS)

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
        val (litDuration, pauseDuration) = currentSequenceTiming()

        // Capture the blitz start moment lazily on the first sequence display of
        // a fresh blitz run (level 1 + no recorded start). Done here rather than
        // in initializeNewGame so the startup animation isn't included in the
        // recorded time.
        val shouldStartBlitzTimer = _uiState.value.gameMode == GameMode.SPEED_BLITZ &&
                _uiState.value.level == 1 &&
                _uiState.value.blitzStartTimeMs == 0L

        _uiState.update {
            it.copy(
                gameState = GameState.ShowingSequence,
                blitzStartTimeMs = if (shouldStartBlitzTimer) System.currentTimeMillis() else it.blitzStartTimeMs
            )
        }

        // Cancel any existing sequence job
        activeSequenceJob?.cancel()

        // Start a new sequence job and save the reference
        activeSequenceJob = viewModelScope.launch {
            delay(GameConstants.SEQUENCE_START_DELAY_MS) // Brief pause before showing sequence

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
    @MainThread
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
            val isFirstButtonPressed = activePresses.isEmpty()
            activePresses.add(button)

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
                    delay(GameConstants.BUTTON_SOUND_DURATION_MS) // Match sound duration
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
            activePresses.remove(button)
        }
    }

    private fun checkSequenceMatch(playerSequence: List<SimonButton>) {
        val index = playerSequence.size - 1
        val sequence = _uiState.value.sequence

        // Safety check: make sure index is valid for both sequences
        if (index < 0 || index >= sequence.size) {
            Log.e(TAG, "Invalid index $index: player sequence=${playerSequence.size}, game sequence=${sequence.size}")
            handleWrongButton("Game over - player entered too many buttons")
            return
        }

        // Reverse mode (F1) inverts the expected order: player watches the
        // sequence forward but must press buttons in reverse. The display
        // path is untouched — only the per-step comparison flips.
        val expected = if (_uiState.value.reverseModeEnabled) {
            sequence[sequence.size - 1 - index]
        } else {
            sequence[index]
        }

        if (playerSequence[index] != expected) {
            Log.d(TAG, "Wrong button selected! Expected: $expected, Got: ${playerSequence[index]}")
            handleWrongButton("Wrong button pressed")
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

    // Handle game over state - Simplified version that calls the main implementation
    private fun handleGameOver() {
        handleGameOver("Wrong button pressed")
    }

    /**
     * Player completed BLITZ_TARGET_LEVEL levels in Speed Blitz mode.
     * Compute elapsed time, persist a new best if applicable, and land on
     * GameOver with `blitzWon=true` so the UI shows the victory variant.
     * Statistics flows are intentionally not touched — blitz uses its own
     * best-time slot and isn't part of the classic gamesPlayed/avg score.
     */
    private fun handleBlitzWin() {
        val state = _uiState.value
        val startMs = state.blitzStartTimeMs
        val elapsedMs = if (startMs > 0L) System.currentTimeMillis() - startMs else 0L
        Log.d(TAG, "Blitz win! Elapsed ${elapsedMs}ms (${if (state.memoryLightsPlusEnabled) "6-button" else "4-button"})")

        cancelTimeoutTimer()
        activeSequenceJob?.cancel()
        activeSequenceJob = null

        // A new best beats the prior time, OR sets it for the first time when
        // the slot is still 0L. Time-based best: lower is better.
        val priorBest = state.currentBestBlitzTimeMs
        val isNewBest = priorBest == 0L || elapsedMs < priorBest
        val newBest = if (isNewBest) elapsedMs else priorBest

        _uiState.update { it.copy(
            gameState = GameState.GameOver,
            blitzWon = true,
            blitzElapsedMs = elapsedMs,
            currentlyLit = null,
            allButtonsLit = false,
            showHighScoreParticles = isNewBest,
            showHighScoreText = isNewBest,
            showGameOverText = !isNewBest,
            bestBlitzTime4ButtonMs = if (isNewBest && !it.memoryLightsPlusEnabled) newBest else it.bestBlitzTime4ButtonMs,
            bestBlitzTime6ButtonMs = if (isNewBest && it.memoryLightsPlusEnabled) newBest else it.bestBlitzTime6ButtonMs
        )}

        if (isNewBest) {
            if (state.memoryLightsPlusEnabled) {
                settingsRepository.setBestBlitzTime6ButtonMs(newBest)
            } else {
                settingsRepository.setBestBlitzTime4ButtonMs(newBest)
            }
            startHighScoreTextAnimation()
        } else {
            startGameOverTextAnimation()
        }
    }

    /**
     * Wrong-button branch shared by [checkSequenceMatch]'s "out of bounds" and
     * "mismatch" paths. In normal play this routes to [handleGameOver]; in
     * practice mode (F15) it plays the error tone, briefly waits, clears the
     * player's progress, and replays the same sequence so the player can try
     * again without losing their level.
     */
    private fun handleWrongButton(reason: String) {
        if (_uiState.value.practiceModeEnabled) {
            Log.d(TAG, "Practice mode: $reason — replaying sequence instead of game over")
            cancelTimeoutTimer()
            soundManager.playErrorSound()
            viewModelScope.launch {
                delay(GameConstants.BUTTON_SOUND_DURATION_MS)
                if (!isAppInForeground) return@launch
                _uiState.update { it.copy(
                    playerSequence = emptyList(),
                    currentlyLit = null,
                    showYourTurnText = false
                )}
                showSequence()
            }
            return
        }
        viewModelScope.launch {
            delay(GameConstants.BUTTON_SOUND_DURATION_MS)
            handleGameOver(reason)
        }
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
                delay(GameConstants.GAME_OVER_FLASH_DURATION_MS)

                // Turn all buttons off
                _uiState.update { it.copy(allButtonsLit = false) }
                delay(GameConstants.GAME_OVER_FLASH_DURATION_MS)
            }

            // Clear the job reference when done
            activeSequenceJob = null
        }
    }

    // Advance to the next level
    private fun advanceToNextLevel() {
        val newLevel = _uiState.value.level + 1
        Log.d(TAG, "Advancing to level $newLevel")

        // Speed Blitz finish line: the player just recalled a sequence of
        // BLITZ_TARGET_LEVEL buttons correctly. Skip the normal level-up path
        // and route to the blitz win handler so we record elapsed time + best.
        if (_uiState.value.gameMode == GameMode.SPEED_BLITZ &&
            newLevel > GameConstants.BLITZ_TARGET_LEVEL
        ) {
            handleBlitzWin()
            return
        }

        // Detect crossing into a faster difficulty tier so the UI can flash
        // a "Speed Up!" cue. Mirrors the threshold in calculateSequenceTiming:
        // every DIFFICULTY_INTERVAL levels starting at level 5 (5, 9, 13, ...).
        val crossedSpeedTier = _uiState.value.difficultyEnabled &&
                newLevel >= 5 &&
                (newLevel - 1) % GameConstants.DIFFICULTY_INTERVAL == 0

        _uiState.update { it.copy(
            level = newLevel,
            roundCount = it.roundCount + 1,
            gameState = GameState.ShowingSequence,
            playerSequence = emptyList(),
            showSpeedUpText = crossedSpeedTier
        ) }

        if (crossedSpeedTier) {
            speedUpTextJob?.cancel()
            speedUpTextJob = viewModelScope.launch {
                delay(GameConstants.SPEED_UP_TEXT_DISPLAY_MS)
                _uiState.update { it.copy(showSpeedUpText = false) }
                speedUpTextJob = null
            }
        }

        // Generate new sequence that includes the previous one
        generateNextSequence()

        // Cancel any existing sequence job
        activeSequenceJob?.cancel()

        // Brief delay before showing new sequence
        activeSequenceJob = viewModelScope.launch {
            delay(GameConstants.LEVEL_ADVANCE_DELAY_MS)

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
        val timeoutMs = _uiState.value.playerTimeoutMs
        Log.d(TAG, "Starting player inactivity timeout timer (${timeoutMs / 1000} seconds)")

        // Don't start timer if app is in background
        if (!isAppInForeground) {
            Log.d(TAG, "App is in background, not starting timeout timer")
            return
        }

        // Cancel any existing timer first
        cancelTimeoutTimer()

        // Bump the tick so the UI knows to (re)start its countdown ring animation.
        _uiState.update { it.copy(timeoutResetTick = it.timeoutResetTick + 1) }

        // Start a new timer
        timeoutJob = viewModelScope.launch {
            delay(timeoutMs)

            // Check if app is still in foreground
            if (!isAppInForeground) {
                Log.d(TAG, "App is in background when timeout occurred, ignoring")
                return@launch
            }

            // If this code executes, the timeout has occurred
            Log.d(TAG, "Player timeout! No button pressed for ${timeoutMs / 1000} seconds")

            // Ensure we're still in PlayerRepeating state (could have changed during the delay)
            if (_uiState.value.gameState == GameState.PlayerRepeating) {
                // Play timeout sound and end game
                soundManager.playErrorSound()
                viewModelScope.launch {
                    delay(GameConstants.BUTTON_SOUND_DURATION_MS)
                    // Handle game over due to timeout
                    handleGameOver("Timeout - no button pressed for ${timeoutMs / 1000} seconds")
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
        val state = _uiState.value
        val currentLevel = state.level
        val isBlitz = state.gameMode == GameMode.SPEED_BLITZ

        // In Speed Blitz a loss doesn't touch the classic high-score slot —
        // best is time-based and only awarded on completion. We still record
        // generic statistics (gamesPlayed / totalScore) so the player's
        // overall play history isn't blank.
        val currentHighScore = state.currentHighScore
        val isNewHighScore = !isBlitz && currentLevel > currentHighScore
        val newHighScore = if (isNewHighScore) currentLevel else currentHighScore
        val blitzElapsedMs = if (isBlitz && state.blitzStartTimeMs > 0L) {
            System.currentTimeMillis() - state.blitzStartTimeMs
        } else 0L

        Log.d(TAG, "$reason at level $currentLevel (mode: ${state.gameMode}, high score: $currentHighScore for ${if (state.memoryLightsPlusEnabled) "6-button" else "4-button"} mode${if (isBlitz) ", blitz elapsed ${blitzElapsedMs}ms" else ""})")

        // Don't do game over animation if app is in background
        if (!isAppInForeground) {
            Log.d(TAG, "App is in background, skipping game over animation")
            // Just update the game state immediately
            updateGameOverState(currentLevel, newHighScore, isNewHighScore, blitzElapsedMs)
            return
        }

        // Play error sound when game is over
        soundManager.playErrorSound()

        // Flash all buttons sequence to indicate game over
        flashAllButtons()

        // Update game state after the flashing animation
        viewModelScope.launch {
            // Wait for flash animation to complete (in flashAllButtons)
            delay(GameConstants.GAME_OVER_ANIMATION_WAIT_MS)

            updateGameOverState(currentLevel, newHighScore, isNewHighScore, blitzElapsedMs)
        }
    }

    // Helper function to update the game state after game over
    private fun updateGameOverState(
        currentLevel: Int,
        newHighScore: Int,
        isNewHighScore: Boolean,
        blitzElapsedMs: Long = 0L
    ) {
        _uiState.update { currentState ->
            if (currentState.memoryLightsPlusEnabled) {
                currentState.copy(
                    gameState = GameState.GameOver,
                    highScore6Button = newHighScore,
                    showHighScoreParticles = isNewHighScore,
                    showHighScoreText = isNewHighScore,
                    showGameOverText = !isNewHighScore, // Show GAME OVER when NOT a new high score
                    blitzElapsedMs = if (blitzElapsedMs > 0L) blitzElapsedMs else currentState.blitzElapsedMs,
                    blitzWon = false
                )
            } else {
                currentState.copy(
                    gameState = GameState.GameOver,
                    highScore4Button = newHighScore,
                    showHighScoreParticles = isNewHighScore,
                    showHighScoreText = isNewHighScore,
                    showGameOverText = !isNewHighScore, // Show GAME OVER when NOT a new high score
                    blitzElapsedMs = if (blitzElapsedMs > 0L) blitzElapsedMs else currentState.blitzElapsedMs,
                    blitzWon = false
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

        // Record game result in statistics. The statisticsFlow collector updates
        // uiState.statistics once DataStore has persisted the new values.
        statisticsRepository.recordGameResult(currentLevel, _uiState.value.sequence.size)

        // Persist the new high score for the active mode (per-key write).
        if (isNewHighScore) {
            if (_uiState.value.memoryLightsPlusEnabled) {
                settingsRepository.setHighScore6Button(newHighScore)
            } else {
                settingsRepository.setHighScore4Button(newHighScore)
            }
        }
    }
    
    // Start the HIGH SCORE text flash animation
    private fun startHighScoreTextAnimation() {
        Log.d(TAG, "Starting HIGH SCORE text flash animation")
        
        // Cancel any existing animation
        highScoreTextAnimationJob?.cancel()
        
        highScoreTextAnimationJob = viewModelScope.launch {
            // Flash the text
            repeat(GameConstants.HIGH_SCORE_FLASH_COUNT) {
                delay(GameConstants.HIGH_SCORE_FLASH_INTERVAL_MS)
                _uiState.update { it.copy(showHighScoreText = false) }
                delay(GameConstants.HIGH_SCORE_FLASH_INTERVAL_MS)
                _uiState.update { it.copy(showHighScoreText = true) }
            }

            // Keep text visible
            delay(GameConstants.HIGH_SCORE_DISPLAY_MS)
            
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
            // Keep text visible without flashing
            delay(GameConstants.GAME_OVER_TEXT_DISPLAY_MS)
            
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
                is GameState.GameOver, is GameState.WaitingToStart, is GameState.Paused -> {
                    // No special handling needed — Paused stays Paused until the
                    // player taps resume.
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
        cancelTimeoutTimer() // Make sure to cancel any timers

        // Cancel any active animations or sequences
        activeSequenceJob?.cancel()
        activeSequenceJob = null
        gameOverTextAnimationJob?.cancel()
        gameOverTextAnimationJob = null
        highScoreTextAnimationJob?.cancel()
        highScoreTextAnimationJob = null
        speedUpTextJob?.cancel()
        speedUpTextJob = null

        soundManager.release()
    }
}