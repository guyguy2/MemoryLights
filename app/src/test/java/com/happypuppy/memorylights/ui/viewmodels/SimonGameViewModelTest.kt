package com.happypuppy.memorylights.ui.viewmodels

import androidx.lifecycle.LifecycleOwner
import com.happypuppy.memorylights.data.manager.SimonSoundManager
import com.happypuppy.memorylights.data.manager.StatisticsManager
import com.happypuppy.memorylights.data.repository.AppSettings
import com.happypuppy.memorylights.data.repository.SettingsRepository
import com.happypuppy.memorylights.domain.GameConstants
import com.happypuppy.memorylights.domain.enums.SimonButton
import com.happypuppy.memorylights.domain.model.GameState
import com.happypuppy.memorylights.domain.model.GameStatistics
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    private lateinit var statisticsManager: StatisticsManager
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var settingsFlow: MutableStateFlow<AppSettings>
    private lateinit var statisticsFlow: MutableStateFlow<GameStatistics>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        soundManager = mockk(relaxed = true)
        every { soundManager.areSoundsLoaded() } returns true
        every { soundManager.getLoadError() } returns null

        statisticsManager = mockk(relaxed = true)
        statisticsFlow = MutableStateFlow(GameStatistics())
        every { statisticsManager.statisticsFlow } returns statisticsFlow

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
        return SimonGameViewModel(soundManager, statisticsManager, settingsRepository)
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
        verify { statisticsManager.recordGameResult(1, any()) }
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
        verify { statisticsManager.resetStatistics() }
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
}

