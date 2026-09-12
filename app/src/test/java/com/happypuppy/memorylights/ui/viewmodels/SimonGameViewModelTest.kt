package com.happypuppy.memorylights.ui.viewmodels

import androidx.lifecycle.LifecycleOwner
import com.happypuppy.memorylights.data.manager.SimonSoundManager
import com.happypuppy.memorylights.data.repository.StatisticsRepository
import com.happypuppy.memorylights.data.repository.AppSettings
import com.happypuppy.memorylights.data.repository.SettingsRepository
import com.happypuppy.memorylights.domain.GameConstants
import com.happypuppy.memorylights.domain.enums.GameMode
import com.happypuppy.memorylights.domain.enums.SimonButton
import com.happypuppy.memorylights.domain.model.Achievement
import com.happypuppy.memorylights.domain.model.GameState
import com.happypuppy.memorylights.domain.model.GameStatistics
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SimonGameViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var soundManager: SimonSoundManager
    private lateinit var statisticsRepository: StatisticsRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var settingsFlow: MutableStateFlow<AppSettings>
    private lateinit var statisticsFlow: MutableStateFlow<GameStatistics>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        soundManager = mockk(relaxed = true)
        every { soundManager.areSoundsLoaded() } returns true
        every { soundManager.getLoadError() } returns null

        statisticsRepository = mockk(relaxed = true)
        statisticsFlow = MutableStateFlow(GameStatistics())
        every { statisticsRepository.statisticsFlow } returns statisticsFlow

        settingsRepository = mockk(relaxed = true)
        settingsFlow = MutableStateFlow(AppSettings())
        every { settingsRepository.settingsFlow } returns settingsFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(initial: AppSettings = AppSettings()): SimonGameViewModel {
        settingsFlow.value = initial
        return SimonGameViewModel(soundManager, statisticsRepository, settingsRepository)
    }

    /**
     * Drains the startup animation and the first showSequence so the VM lands in
     * PlayerRepeating with a 1-button sequence — without firing the 10s player timeout.
     */
    private fun kotlinx.coroutines.test.TestScope.advanceToPlayerTurn() {
        // Startup: 500 + 4*(300+150) + 500 = ~2800ms; first showSequence: 500 + (600+400) = ~1500ms
        // Allow 5s — well past sequence display, well before 10s timeout
        advanceTimeBy(5_000)
        runCurrent()
    }

    @Test
    fun `correct button advances to next level`() = runTest(testDispatcher.scheduler) {
        val vm = createViewModel()
        advanceToPlayerTurn()

        val state1 = vm.uiState.value
        assertEquals(GameState.PlayerRepeating, state1.gameState)
        assertEquals(1, state1.level)
        assertEquals(1, state1.sequence.size)

        // Tap the correct button — should advance to level 2
        vm.onButtonClick(state1.sequence[0], isPress = true)
        // LEVEL_ADVANCE_DELAY (1000) + showSequence (500 + 2*1000) = ~3500ms
        advanceTimeBy(4_000)
        runCurrent()

        val state2 = vm.uiState.value
        assertEquals(2, state2.level)
        assertEquals(2, state2.sequence.size)
        assertEquals(GameState.PlayerRepeating, state2.gameState)
    }

    @Test
    fun `wrong button triggers game over`() = runTest(testDispatcher.scheduler) {
        val vm = createViewModel()
        advanceToPlayerTurn()

        val available = SimonButton.getAvailableButtons(memoryLightsPlusEnabled = false)
        val correct = vm.uiState.value.sequence[0]
        val wrong = available.first { it != correct }

        vm.onButtonClick(wrong, isPress = true)
        // checkSequenceMatch delay (300) + handleGameOver wait (2000)
        advanceTimeBy(3_000)
        runCurrent()

        assertEquals(GameState.GameOver, vm.uiState.value.gameState)
    }

    @Test
    fun `inactivity timeout triggers game over`() = runTest(testDispatcher.scheduler) {
        val vm = createViewModel()
        advanceToPlayerTurn()
        assertEquals(GameState.PlayerRepeating, vm.uiState.value.gameState)

        // PLAYER_TIMEOUT_MS=10000 + BUTTON_SOUND_DURATION_MS=300 + GAME_OVER_ANIMATION_WAIT_MS=2000
        advanceTimeBy(GameConstants.PLAYER_TIMEOUT_MS + 3_000)
        advanceUntilIdle()

        assertEquals(GameState.GameOver, vm.uiState.value.gameState)
        verify { soundManager.playErrorSound() }
    }

    @Test
    fun `losing at level 1 sets high score to 1 in 4-button mode`() = runTest(testDispatcher.scheduler) {
        val vm = createViewModel(AppSettings(highScore4Button = 0))
        advanceToPlayerTurn()

        val available = SimonButton.getAvailableButtons(memoryLightsPlusEnabled = false)
        val wrong = available.first { it != vm.uiState.value.sequence[0] }
        vm.onButtonClick(wrong, isPress = true)
        advanceTimeBy(3_000)
        runCurrent()

        assertEquals(1, vm.uiState.value.highScore4Button)
        assertTrue(vm.uiState.value.showHighScoreText || vm.uiState.value.showHighScoreParticles)
        verify { settingsRepository.setHighScore4Button(1) }
        verify { statisticsRepository.recordGameResult(1, any()) }
    }

    @Test
    fun `losing below existing high score does not lower it`() = runTest(testDispatcher.scheduler) {
        val vm = createViewModel(AppSettings(highScore4Button = 5))
        advanceToPlayerTurn()
        assertEquals(5, vm.uiState.value.highScore4Button)

        val available = SimonButton.getAvailableButtons(memoryLightsPlusEnabled = false)
        val wrong = available.first { it != vm.uiState.value.sequence[0] }
        vm.onButtonClick(wrong, isPress = true)
        advanceTimeBy(3_000)
        runCurrent()

        assertEquals(GameState.GameOver, vm.uiState.value.gameState)
        // High score must not regress
        assertEquals(5, vm.uiState.value.highScore4Button)
        // Not flagged as a new high score
        assertEquals(false, vm.uiState.value.showHighScoreText)
        // saveSettings is gated on isNewHighScore inside updateGameOverState — should not fire
        verify(exactly = 0) { settingsRepository.setHighScore4Button(any()) }
    }

    @Test
    fun `resetHighScore zeroes current mode and clears statistics`() = runTest(testDispatcher.scheduler) {
        val vm = createViewModel(AppSettings(highScore4Button = 12, highScore6Button = 8))
        advanceToPlayerTurn()
        assertEquals(12, vm.uiState.value.highScore4Button)

        vm.resetHighScore()
        runCurrent()

        assertEquals(0, vm.uiState.value.highScore4Button)
        // 6-button score untouched (current mode is 4-button)
        assertEquals(8, vm.uiState.value.highScore6Button)
        verify { statisticsRepository.resetStatistics() }
        verify { settingsRepository.setHighScore4Button(0) }
    }

    @Test
    fun `onPause cancels active animations and pauses sounds`() = runTest(testDispatcher.scheduler) {
        val vm = createViewModel()
        advanceToPlayerTurn()
        val owner = mockk<LifecycleOwner>(relaxed = true)

        // Force a lit button so we can verify onPause clears it
        val target = vm.uiState.value.sequence[0]
        vm.onButtonClick(target, isPress = true)
        runCurrent()
        assertNotEquals(null, vm.uiState.value.currentlyLit)

        vm.onPause(owner)
        runCurrent()

        assertNull(vm.uiState.value.currentlyLit)
        assertEquals(false, vm.uiState.value.allButtonsLit)
        verify { soundManager.pauseSounds() }
    }

    @Test
    fun `onResume restores PlayerRepeating state and resumes sounds`() = runTest(testDispatcher.scheduler) {
        val vm = createViewModel()
        advanceToPlayerTurn()
        val owner = mockk<LifecycleOwner>(relaxed = true)

        vm.onPause(owner)
        runCurrent()
        vm.onResume(owner)
        runCurrent()

        assertEquals(GameState.PlayerRepeating, vm.uiState.value.gameState)
        verify { soundManager.resumeSounds() }
    }

    @Test
    fun `setMemoryLightsPlusEnabled true switches to 6-button mode and resets game`() =
        runTest(testDispatcher.scheduler) {
            val vm = createViewModel()
            advanceToPlayerTurn()
            assertEquals(false, vm.uiState.value.memoryLightsPlusEnabled)

            vm.setMemoryLightsPlusEnabled(true)
            advanceTimeBy(2_000)
            runCurrent()

            val state = vm.uiState.value
            assertTrue(state.memoryLightsPlusEnabled)
            assertEquals(1, state.level)
            verify { settingsRepository.setMemoryLightsPlusEnabled(true) }
        }

    @Test
    fun `setAudioOnlyModeEnabled updates state and persists`() =
        runTest(testDispatcher.scheduler) {
            val vm = createViewModel()
            advanceToPlayerTurn()
            assertEquals(false, vm.uiState.value.audioOnlyModeEnabled)

            vm.setAudioOnlyModeEnabled(true)
            runCurrent()

            assertEquals(true, vm.uiState.value.audioOnlyModeEnabled)
            verify { settingsRepository.setAudioOnlyModeEnabled(true) }
        }

    @Test
    fun `setColorBlindModeEnabled updates state and persists without resetting run`() =
        runTest(testDispatcher.scheduler) {
            val vm = createViewModel()
            advanceToPlayerTurn()
            val levelBefore = vm.uiState.value.level
            assertEquals(false, vm.uiState.value.colorBlindModeEnabled)

            vm.setColorBlindModeEnabled(true)
            runCurrent()

            assertEquals(true, vm.uiState.value.colorBlindModeEnabled)
            assertEquals(levelBefore, vm.uiState.value.level)
            verify { settingsRepository.setColorBlindModeEnabled(true) }
        }

    @Test
    fun `setGameMode to Speed Blitz resets the run`() = runTest(testDispatcher.scheduler) {
        val vm = createViewModel()
        advanceToPlayerTurn()
        assertEquals(GameMode.CLASSIC, vm.uiState.value.gameMode)

        // Advance one level so we have non-trivial state to reset
        vm.onButtonClick(vm.uiState.value.sequence[0], isPress = true)
        advanceTimeBy(4_000)
        runCurrent()
        assertEquals(2, vm.uiState.value.level)

        vm.setGameMode(GameMode.SPEED_BLITZ)
        advanceTimeBy(2_000)
        runCurrent()

        val state = vm.uiState.value
        assertEquals(GameMode.SPEED_BLITZ, state.gameMode)
        assertEquals(1, state.level)
        verify { settingsRepository.setGameMode(GameMode.SPEED_BLITZ) }
    }

    @Test
    fun `setGameMode no-op when already in that mode`() = runTest(testDispatcher.scheduler) {
        val vm = createViewModel(AppSettings(gameMode = GameMode.SPEED_BLITZ))
        advanceToPlayerTurn()
        assertEquals(GameMode.SPEED_BLITZ, vm.uiState.value.gameMode)

        // Reaching PlayerRepeating already required one persist call during
        // load; reset by clearing recorded calls then issuing the no-op.
        vm.setGameMode(GameMode.SPEED_BLITZ)
        runCurrent()

        // Still blitz, no extra persist
        assertEquals(GameMode.SPEED_BLITZ, vm.uiState.value.gameMode)
        verify(exactly = 0) { settingsRepository.setGameMode(any()) }
    }

    @Test
    fun `daily challenge produces deterministic sequence for fixed epoch day`() =
        runTest(testDispatcher.scheduler) {
            // Two ViewModels initialized with daily mode on, both pinned to the
            // same epoch day, must produce identical first sequences. That's
            // the whole point of F6: same seed → same buttons for everyone.
            val day = 19_852L

            val vm1 = createViewModel(AppSettings(dailyChallengeEnabled = true))
            vm1.currentEpochDay = { day }
            advanceToPlayerTurn()
            val seq1 = vm1.uiState.value.sequence
            assertEquals(1, seq1.size)

            val vm2 = createViewModel(AppSettings(dailyChallengeEnabled = true))
            vm2.currentEpochDay = { day }
            advanceToPlayerTurn()
            val seq2 = vm2.uiState.value.sequence

            assertEquals(seq1, seq2)
        }

    @Test
    fun `daily completion records today's epoch day and best level on game over`() =
        runTest(testDispatcher.scheduler) {
            val day = 19_852L
            val vm = createViewModel(AppSettings(dailyChallengeEnabled = true))
            vm.currentEpochDay = { day }
            advanceToPlayerTurn()

            val available = SimonButton.getAvailableButtons(memoryLightsPlusEnabled = false)
            val wrong = available.first { it != vm.uiState.value.sequence[0] }
            vm.onButtonClick(wrong, isPress = true)
            advanceTimeBy(3_000)
            runCurrent()

            assertEquals(GameState.GameOver, vm.uiState.value.gameState)
            assertEquals(day, vm.uiState.value.dailyCompletedEpochDay)
            assertEquals(1, vm.uiState.value.dailyBestLevel)
            verify { settingsRepository.setDailyCompletion(day, 1) }
        }

    @Test
    fun `game over at level 1 unlocks FirstWin and persists set`() =
        runTest(testDispatcher.scheduler) {
            val vm = createViewModel()
            advanceToPlayerTurn()
            assertEquals(emptySet<String>(), vm.uiState.value.unlockedAchievements)

            val available = SimonButton.getAvailableButtons(memoryLightsPlusEnabled = false)
            val wrong = available.first { it != vm.uiState.value.sequence[0] }
            vm.onButtonClick(wrong, isPress = true)
            advanceTimeBy(3_000)
            runCurrent()

            assertEquals(GameState.GameOver, vm.uiState.value.gameState)
            assertTrue("FirstWin" in vm.uiState.value.unlockedAchievements)
            verify { settingsRepository.setUnlockedAchievements(match { "FirstWin" in it }) }
        }

    @Test
    fun `daily completion game over unlocks both FirstWin and DailyDone`() =
        runTest(testDispatcher.scheduler) {
            val vm = createViewModel(AppSettings(dailyChallengeEnabled = true))
            vm.currentEpochDay = { 19_852L }
            advanceToPlayerTurn()

            val available = SimonButton.getAvailableButtons(memoryLightsPlusEnabled = false)
            val wrong = available.first { it != vm.uiState.value.sequence[0] }
            vm.onButtonClick(wrong, isPress = true)
            advanceTimeBy(3_000)
            runCurrent()

            val unlocked = vm.uiState.value.unlockedAchievements
            assertTrue("FirstWin in unlocked", "FirstWin" in unlocked)
            assertTrue("DailyDone in unlocked", "DailyDone" in unlocked)
        }

    @Test
    fun `unlocked achievements load from settings on init`() =
        runTest(testDispatcher.scheduler) {
            val vm = createViewModel(AppSettings(unlockedAchievements = setOf("FirstWin", "Level10")))
            advanceToPlayerTurn()

            assertEquals(setOf("FirstWin", "Level10"), vm.uiState.value.unlockedAchievements)
        }

    @Test
    fun `re-unlock with no new achievements does not persist`() =
        runTest(testDispatcher.scheduler) {
            // Start with FirstWin already unlocked. A game-over at level 1 would
            // try to unlock FirstWin again — but since it's already in the set,
            // setUnlockedAchievements must NOT be called (no-op fast path).
            val vm = createViewModel(AppSettings(unlockedAchievements = setOf("FirstWin")))
            advanceToPlayerTurn()

            val available = SimonButton.getAvailableButtons(memoryLightsPlusEnabled = false)
            val wrong = available.first { it != vm.uiState.value.sequence[0] }
            vm.onButtonClick(wrong, isPress = true)
            advanceTimeBy(3_000)
            runCurrent()

            // Still just FirstWin — no new unlock happened
            assertEquals(setOf("FirstWin"), vm.uiState.value.unlockedAchievements)
            verify(exactly = 0) { settingsRepository.setUnlockedAchievements(any()) }
        }

    @Test
    fun `unlockEvents emits newly unlocked achievements in enum order`() =
        runTest(testDispatcher.scheduler) {
            // Start with everything except FirstWin already unlocked so the
            // game-over at level 1 unlocks exactly one new badge — easy to
            // assert against without timing-dependency on later batches.
            val preUnlocked = Achievement.entries
                .filterNot { it == Achievement.FirstWin }
                .map { it.name }
                .toSet()
            val vm = createViewModel(AppSettings(unlockedAchievements = preUnlocked))
            advanceToPlayerTurn()

            val collected = mutableListOf<List<Achievement>>()
            val collectorJob = backgroundScope.launch(testDispatcher) {
                vm.unlockEvents.collect { collected.add(it) }
            }

            val available = SimonButton.getAvailableButtons(memoryLightsPlusEnabled = false)
            val wrong = available.first { it != vm.uiState.value.sequence[0] }
            vm.onButtonClick(wrong, isPress = true)
            advanceTimeBy(3_000)
            runCurrent()

            assertEquals(GameState.GameOver, vm.uiState.value.gameState)
            assertEquals(1, collected.size)
            assertEquals(listOf(Achievement.FirstWin), collected.first())

            collectorJob.cancel()
        }

    @Test
    fun `unlockEvents does not emit when already-unlocked badge re-evaluates`() =
        runTest(testDispatcher.scheduler) {
            // FirstWin already unlocked — game-over at level 1 re-evaluates but
            // adds nothing new. The channel must stay silent (no transient
            // toast for badges the player already has).
            val vm = createViewModel(AppSettings(unlockedAchievements = setOf("FirstWin")))
            advanceToPlayerTurn()

            val collected = mutableListOf<List<Achievement>>()
            val collectorJob = backgroundScope.launch(testDispatcher) {
                vm.unlockEvents.collect { collected.add(it) }
            }

            val available = SimonButton.getAvailableButtons(memoryLightsPlusEnabled = false)
            val wrong = available.first { it != vm.uiState.value.sequence[0] }
            vm.onButtonClick(wrong, isPress = true)
            advanceTimeBy(3_000)
            runCurrent()

            assertEquals(GameState.GameOver, vm.uiState.value.gameState)
            // No emission — re-unlock fast path skipped the trySend.
            assertEquals(0, collected.size)

            collectorJob.cancel()
        }

    @Test
    fun `practice mode wrong button replays sequence instead of game over`() =
        runTest(testDispatcher.scheduler) {
            val vm = createViewModel(AppSettings(practiceModeEnabled = true, highScore4Button = 0))
            advanceToPlayerTurn()
            assertEquals(GameState.PlayerRepeating, vm.uiState.value.gameState)

            val available = SimonButton.getAvailableButtons(memoryLightsPlusEnabled = false)
            val correct = vm.uiState.value.sequence[0]
            val wrong = available.first { it != correct }

            vm.onButtonClick(wrong, isPress = true)
            // wrong-button delay (300) + showSequence redraw (500 + 600 + 400) = ~1800ms
            advanceTimeBy(2_500)
            runCurrent()

            // Game must NOT be over; sequence replays so we land back in PlayerRepeating
            assertEquals(GameState.PlayerRepeating, vm.uiState.value.gameState)
            // High score must not advance — practice mode never reaches handleGameOver
            assertEquals(0, vm.uiState.value.highScore4Button)
            verify(exactly = 0) { settingsRepository.setHighScore4Button(any()) }
            verify(exactly = 0) { statisticsRepository.recordGameResult(any(), any()) }
        }

    /**
     * Press + release so multi-step input clears [activePresses]. Without a
     * release, later presses are treated as multi-touch and game logic is
     * ignored (see onButtonClick isFirstButtonPressed gate).
     */
    private fun SimonGameViewModel.tap(button: SimonButton) {
        onButtonClick(button, isPress = true)
        onButtonClick(button, isPress = false)
    }

    @Test
    fun `reverse mode level-2 accepts reverse order and rejects forward`() =
        runTest(testDispatcher.scheduler) {
            val vm = createViewModel(AppSettings(reverseModeEnabled = true))
            advanceToPlayerTurn()

            // Level 1 is a single button — reverse is identity. Clear it.
            vm.tap(vm.uiState.value.sequence[0])
            // LEVEL_ADVANCE_DELAY + showSequence for 2 buttons ≈ 1000 + 500 + 2*1000
            advanceTimeBy(4_000)
            runCurrent()

            assertEquals(GameState.PlayerRepeating, vm.uiState.value.gameState)
            assertEquals(2, vm.uiState.value.level)
            assertEquals(2, vm.uiState.value.sequence.size)

            val seq = vm.uiState.value.sequence
            // Reverse expects last-first. When the two steps differ, forward
            // order (seq[0] first) is a hard fail; if they match, any other
            // color is the wrong first press.
            val badFirst = if (seq[0] != seq[1]) {
                seq[0]
            } else {
                SimonButton.getAvailableButtons(memoryLightsPlusEnabled = false)
                    .first { it != seq.last() }
            }
            vm.tap(badFirst)
            advanceTimeBy(3_000)
            runCurrent()
            assertEquals(GameState.GameOver, vm.uiState.value.gameState)
        }

    @Test
    fun `reverse mode level-2 reverse presses advance to level 3`() =
        runTest(testDispatcher.scheduler) {
            val vm = createViewModel(AppSettings(reverseModeEnabled = true))
            advanceToPlayerTurn()

            vm.tap(vm.uiState.value.sequence[0])
            advanceTimeBy(4_000)
            runCurrent()

            val seq = vm.uiState.value.sequence
            assertEquals(2, seq.size)
            // Reverse recall: last button first, then earlier ones.
            vm.tap(seq[1])
            vm.tap(seq[0])
            // LEVEL_ADVANCE_DELAY (1000) + showSequence for 3 buttons
            // (500 + 3*(600+400)) ≈ 4500ms — allow 6s.
            advanceTimeBy(6_000)
            runCurrent()

            assertEquals(GameState.PlayerRepeating, vm.uiState.value.gameState)
            assertEquals(3, vm.uiState.value.level)
            assertEquals(3, vm.uiState.value.sequence.size)
        }

    @Test
    fun `pauseGame only works during PlayerRepeating and resume restores it`() =
        runTest(testDispatcher.scheduler) {
            val vm = createViewModel()

            // No-op before the first turn.
            vm.pauseGame()
            assertNotEquals(GameState.Paused, vm.uiState.value.gameState)

            advanceToPlayerTurn()
            assertEquals(GameState.PlayerRepeating, vm.uiState.value.gameState)

            vm.pauseGame()
            assertEquals(GameState.Paused, vm.uiState.value.gameState)
            assertNull(vm.uiState.value.currentlyLit)
            assertEquals(false, vm.uiState.value.showYourTurnText)

            // Double-pause is a no-op.
            vm.pauseGame()
            assertEquals(GameState.Paused, vm.uiState.value.gameState)

            vm.resumeGame()
            assertEquals(GameState.PlayerRepeating, vm.uiState.value.gameState)

            // resume when not paused is a no-op.
            vm.resumeGame()
            assertEquals(GameState.PlayerRepeating, vm.uiState.value.gameState)
        }

    @Test
    fun `replayLastSequence keeps level and sequence after game over`() =
        runTest(testDispatcher.scheduler) {
            val vm = createViewModel(AppSettings(highScore4Button = 99))
            advanceToPlayerTurn()

            // Grow to level 2 so the sequence is multi-button and distinctive.
            vm.tap(vm.uiState.value.sequence[0])
            advanceTimeBy(4_000)
            runCurrent()
            assertEquals(2, vm.uiState.value.level)
            val lostSequence = vm.uiState.value.sequence
            assertEquals(2, lostSequence.size)

            val available = SimonButton.getAvailableButtons(memoryLightsPlusEnabled = false)
            val wrong = available.first { it != lostSequence[0] }
            vm.tap(wrong)
            advanceTimeBy(3_000)
            runCurrent()
            assertEquals(GameState.GameOver, vm.uiState.value.gameState)

            vm.replayLastSequence()
            // showSequence: SEQUENCE_START_DELAY + 2*(lit+pause) ≈ 500 + 2*1000
            advanceTimeBy(3_000)
            runCurrent()

            val after = vm.uiState.value
            assertEquals(GameState.PlayerRepeating, after.gameState)
            assertEquals(2, after.level)
            assertEquals(lostSequence, after.sequence)
            assertTrue(after.playerSequence.isEmpty())
        }

    @Test
    fun `blitz loss does not update classic high score`() =
        runTest(testDispatcher.scheduler) {
            val vm = createViewModel(
                AppSettings(gameMode = GameMode.SPEED_BLITZ, highScore4Button = 5)
            )
            advanceToPlayerTurn()

            val available = SimonButton.getAvailableButtons(memoryLightsPlusEnabled = false)
            val wrong = available.first { it != vm.uiState.value.sequence[0] }
            vm.tap(wrong)
            advanceTimeBy(3_000)
            runCurrent()

            assertEquals(GameState.GameOver, vm.uiState.value.gameState)
            assertEquals(false, vm.uiState.value.blitzWon)
            assertEquals(5, vm.uiState.value.highScore4Button)
            verify(exactly = 0) { settingsRepository.setHighScore4Button(any()) }
            // Stats still record so play history isn't blank.
            verify { statisticsRepository.recordGameResult(1, any()) }
        }

    @Test
    fun `press after a miss cannot advance the level`() =
        runTest(testDispatcher.scheduler) {
            val vm = createViewModel()
            advanceToPlayerTurn()

            // Grow to level 2 so a wrong-then-matching pair of presses exists.
            vm.tap(vm.uiState.value.sequence[0])
            advanceTimeBy(4_000)
            runCurrent()
            val seq = vm.uiState.value.sequence
            assertEquals(2, seq.size)

            val available = SimonButton.getAvailableButtons(memoryLightsPlusEnabled = false)
            val wrong = available.first { it != seq[0] }
            vm.tap(wrong)
            // Still inside the error-tone delay: gameState is PlayerRepeating but
            // the turn is over. This press matches seq[1] and must not complete
            // the sequence.
            vm.tap(seq[1])
            advanceTimeBy(3_000)
            runCurrent()

            assertEquals(GameState.GameOver, vm.uiState.value.gameState)
            assertEquals(2, vm.uiState.value.level)
            verify(exactly = 1) { statisticsRepository.recordGameResult(2, any()) }
        }

    @Test
    fun `press during game-over flash does not record a second game over`() =
        runTest(testDispatcher.scheduler) {
            val vm = createViewModel()
            advanceToPlayerTurn()

            val available = SimonButton.getAvailableButtons(memoryLightsPlusEnabled = false)
            val correct = vm.uiState.value.sequence[0]
            val wrong = available.first { it != correct }
            vm.tap(wrong)
            // Past the error-tone delay (300) and into the 2s flash animation,
            // during which gameState is still PlayerRepeating.
            advanceTimeBy(600)
            runCurrent()
            assertEquals(GameState.PlayerRepeating, vm.uiState.value.gameState)

            vm.tap(correct)
            advanceTimeBy(3_000)
            runCurrent()

            assertEquals(GameState.GameOver, vm.uiState.value.gameState)
            assertEquals(1, vm.uiState.value.level)
            verify(exactly = 1) { statisticsRepository.recordGameResult(any(), any()) }
        }

    @Test
    fun `practice mode ignores presses until the sequence replays`() =
        runTest(testDispatcher.scheduler) {
            val vm = createViewModel(AppSettings(practiceModeEnabled = true))
            advanceToPlayerTurn()

            val available = SimonButton.getAvailableButtons(memoryLightsPlusEnabled = false)
            val correct = vm.uiState.value.sequence[0]
            val wrong = available.first { it != correct }
            vm.tap(wrong)
            // Inside the error-tone delay; must not count as a second miss.
            vm.tap(correct)
            advanceTimeBy(2_500)
            runCurrent()

            assertEquals(GameState.PlayerRepeating, vm.uiState.value.gameState)
            verify(exactly = 1) { soundManager.playErrorSound() }
        }
}
