package com.happypuppy.memorylights

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.happypuppy.memorylights.domain.enums.GameMode
import com.happypuppy.memorylights.domain.enums.SimonButton
import com.happypuppy.memorylights.domain.enums.SoundPack
import com.happypuppy.memorylights.domain.enums.ThemeMode
import com.happypuppy.memorylights.domain.model.Achievement
import com.happypuppy.memorylights.domain.model.GameState
import com.happypuppy.memorylights.domain.model.GameStatistics
import com.happypuppy.memorylights.domain.model.SimonGameUiState
import com.happypuppy.memorylights.ui.components.ParticleEffect
import com.happypuppy.memorylights.ui.screens.AboutDialog
import com.happypuppy.memorylights.ui.screens.GameModesScreen
import com.happypuppy.memorylights.ui.screens.GameplayScreen
import com.happypuppy.memorylights.ui.screens.ResetStatisticsDialog
import com.happypuppy.memorylights.ui.screens.SettingsScreen
import com.happypuppy.memorylights.ui.screens.SimonGameScreen
import com.happypuppy.memorylights.ui.screens.SoundAndHapticsScreen
import com.happypuppy.memorylights.ui.screens.StatisticsScreen
import com.happypuppy.memorylights.ui.theme.MyApplicationTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Permanent Compose Accessibility Test Framework (ATF) suite covering every
 * Memory Lights screen, dialog window, theme mode, and Wave A/B/C UX guards.
 */
@RunWith(AndroidJUnit4::class)
class ScreenAccessibilityTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        rule.enableAccessibilityChecks()
    }

    private fun checkAllWindows() {
        rule.waitForIdle()
        // Dialogs render in separate windows — check every root node.
        rule.onAllNodes(isRoot()).tryPerformAccessibilityChecks()
    }

    @Test
    fun fourTileGame_waitingAndActive_passesAccessibilityChecks() {
        rule.setContent {
            MyApplicationTheme(themeMode = ThemeMode.AMOLED) {
                SimonGameScreen(
                    uiState = SimonGameUiState(
                        gameState = GameState.WaitingToStart,
                        soundsLoaded = true,
                        highScore4Button = 12,
                        currentSoundPack = SoundPack.STANDARD
                    ),
                    onButtonClick = { _, _ -> },
                    onSettingsClick = {},
                    onStartNewGame = {},
                    onToggleSound = {},
                    onToggleVibration = {}
                )
            }
        }
        checkAllWindows()
    }

    @Test
    fun fourTileGame_gameOver_passesAccessibilityChecks() {
        rule.setContent {
            MyApplicationTheme(themeMode = ThemeMode.DARK) {
                SimonGameScreen(
                    uiState = SimonGameUiState(
                        gameState = GameState.GameOver,
                        soundsLoaded = true,
                        level = 6,
                        highScore4Button = 12,
                        sequence = listOf(SimonButton.GREEN, SimonButton.RED),
                        showGameOverText = true
                    ),
                    onButtonClick = { _, _ -> },
                    onSettingsClick = {},
                    onStartNewGame = {},
                    onToggleSound = {},
                    onToggleVibration = {}
                )
            }
        }
        checkAllWindows()
    }

    @Test
    fun sixTileGame_speedBlitzActive_passesAccessibilityChecks() {
        rule.setContent {
            MyApplicationTheme(themeMode = ThemeMode.AMOLED) {
                SimonGameScreen(
                    uiState = SimonGameUiState(
                        memoryLightsPlusEnabled = true,
                        gameMode = GameMode.SPEED_BLITZ,
                        gameState = GameState.PlayerRepeating,
                        soundsLoaded = true,
                        level = 7,
                        bestBlitzTime6ButtonMs = 24_800L,
                        blitzStartTimeMs = 1_000L,
                        blitzPauseStartedAtMs = 14_300L,
                        sequence = List(7) { SimonButton.GREEN },
                        playerSequence = List(3) { SimonButton.GREEN },
                        colorBlindModeEnabled = true
                    ),
                    onButtonClick = { _, _ -> },
                    onSettingsClick = {},
                    onStartNewGame = {},
                    onToggleSound = {},
                    onToggleVibration = {}
                )
            }
        }
        checkAllWindows()
    }

    @Test
    fun pausedOverlay_passesAccessibilityChecks() {
        rule.setContent {
            MyApplicationTheme(themeMode = ThemeMode.AMOLED) {
                SimonGameScreen(
                    uiState = SimonGameUiState(
                        gameState = GameState.Paused,
                        soundsLoaded = true,
                        level = 4,
                        sequence = List(4) { SimonButton.BLUE }
                    ),
                    onButtonClick = { _, _ -> },
                    onSettingsClick = {},
                    onStartNewGame = {},
                    onToggleSound = {},
                    onToggleVibration = {}
                )
            }
        }
        checkAllWindows()
    }

    @Test
    fun settingsScreen_andDialogs_passAccessibilityChecks() {
        rule.setContent {
            MyApplicationTheme(themeMode = ThemeMode.AMOLED) {
                SettingsScreen(
                    currentSoundPack = SoundPack.RETRO,
                    themeMode = ThemeMode.AMOLED,
                    onThemeModeChange = {},
                    onGameModesClick = {},
                    onGameplayClick = {},
                    onSoundAndHapticsClick = {},
                    onStatisticsClicked = {},
                    onResetHighScore = {},
                    onBackPressed = {},
                    unlockedAchievements = setOf(Achievement.FirstWin.name, Achievement.Level10.name)
                )
            }
        }
        checkAllWindows()

        // Open Reset dialog via inner Reset button and verify dialog window
        rule.onNodeWithText("Reset").performScrollTo().performClick()
        rule.onNodeWithText("Reset High Score").assertIsDisplayed()
        checkAllWindows()
        rule.onNodeWithText("Cancel").performClick()

        // Open About dialog and verify dialog window
        rule.onNodeWithText("About").performScrollTo().performClick()
        rule.onNodeWithText("About Memory Lights").assertIsDisplayed()
        checkAllWindows()
    }

    @Test
    fun aboutDialog_darkTheme_passesAccessibilityChecks() {
        rule.setContent {
            MyApplicationTheme(themeMode = ThemeMode.DARK) {
                AboutDialog(versionInfo = "1.0.3 (4)", onDismiss = {})
            }
        }
        checkAllWindows()
    }

    @Test
    fun gameModesScreen_andConfirmDialog_passAccessibilityChecks() {
        rule.setContent {
            MyApplicationTheme(themeMode = ThemeMode.DARK) {
                GameModesScreen(
                    difficultyEnabled = true,
                    reverseModeEnabled = false,
                    practiceModeEnabled = false,
                    audioOnlyModeEnabled = false,
                    memoryLightsPlusEnabled = false,
                    dailyChallengeEnabled = true,
                    dailyCompletedToday = true,
                    dailyBestLevel = 9,
                    gameMode = GameMode.CLASSIC,
                    hasActiveGame = true,
                    onDifficultyToggled = {},
                    onReverseModeToggled = {},
                    onPracticeModeToggled = {},
                    onAudioOnlyModeToggled = {},
                    onMemoryLightsPlusToggled = {},
                    onDailyChallengeToggled = {},
                    onGameModeSelected = {},
                    onBackPressed = {}
                )
            }
        }
        checkAllWindows()

        // Trigger mid-game switch confirmation dialog and check its window
        rule.onNodeWithText("Speed Blitz").performClick()
        rule.onNodeWithText("End current game?").assertIsDisplayed()
        checkAllWindows()
    }

    @Test
    fun gameplayScreen_passesAccessibilityChecks() {
        rule.setContent {
            MyApplicationTheme(themeMode = ThemeMode.AMOLED) {
                GameplayScreen(
                    playerTimeoutSeconds = 10,
                    colorBlindModeEnabled = true,
                    onPlayerTimeoutChanged = {},
                    onColorBlindModeToggled = {},
                    onBackPressed = {}
                )
            }
        }
        checkAllWindows()
    }

    @Test
    fun soundAndHapticsScreen_passesAccessibilityChecks() {
        rule.setContent {
            MyApplicationTheme(themeMode = ThemeMode.DARK) {
                SoundAndHapticsScreen(
                    currentSoundPack = SoundPack.STANDARD,
                    onSoundPackSelected = {},
                    onBackPressed = {}
                )
            }
        }
        checkAllWindows()
    }

    @Test
    fun statisticsScreen_empty_passesAccessibilityChecks() {
        rule.setContent {
            MyApplicationTheme(themeMode = ThemeMode.AMOLED) {
                StatisticsScreen(
                    statistics = GameStatistics(),
                    currentHighScore = 0,
                    unlockedAchievements = emptySet(),
                    onBackPressed = {}
                )
            }
        }
        checkAllWindows()
    }

    @Test
    fun statisticsScreen_populated_andResetDialog_passAccessibilityChecks_andNoCardClickTrap() {
        val restorationTester = StateRestorationTester(rule)
        restorationTester.setContent {
            MyApplicationTheme(themeMode = ThemeMode.DARK) {
                StatisticsScreen(
                    statistics = GameStatistics(
                        gamesPlayed = 24,
                        totalScore = 186,
                        bestStreak = 6
                    ),
                    currentHighScore = 15,
                    unlockedAchievements = setOf(
                        Achievement.FirstWin.name,
                        Achievement.Level10.name,
                        Achievement.Streak3.name
                    ),
                    onBackPressed = {}
                )
            }
        }
        checkAllWindows()

        // Tapping the Reset card title must NOT open the dialog (Wave A #4: no nested clickable trap).
        rule.onNodeWithText("Reset All Statistics").performScrollTo().performClick()
        rule.onNodeWithText("Reset Statistics").assertDoesNotExist()

        // Tapping the Reset button opens the confirmation dialog.
        rule.onNodeWithText("Reset").performScrollTo().performClick()
        rule.onNodeWithText("Reset Statistics").assertIsDisplayed()
        checkAllWindows()

        // Dialog state survives process recreation (Wave B #8: rememberSaveable).
        restorationTester.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Reset Statistics").assertIsDisplayed()
        checkAllWindows()
    }

    @Test
    fun resetStatisticsDialog_amoled_passesAccessibilityChecks() {
        rule.setContent {
            MyApplicationTheme(themeMode = ThemeMode.AMOLED) {
                ResetStatisticsDialog(onConfirm = {}, onDismiss = {})
            }
        }
        checkAllWindows()
    }

    @Test
    fun particleEffect_withReducedMotion_completesImmediately() {
        var completions = 0
        rule.setContent {
            MyApplicationTheme {
                ParticleEffect(
                    isActive = true,
                    reducedMotion = true,
                    onComplete = { completions++ }
                )
            }
        }
        rule.waitForIdle()
        assertEquals("Reduced motion should complete ParticleEffect immediately", 1, completions)
    }
}

/**
 * Adaptive wide-landscape accessibility and 840.dp width-cap checks at true
 * 1:1 physical display density (avoids ForcedSize density downscaling).
 */
@RunWith(AndroidJUnit4::class)
class AdaptiveScreenAccessibilityTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        rule.enableAccessibilityChecks()
    }

    @After
    fun restoreOrientation() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<ComponentActivity>()
                .forEach { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun switchToWideLandscape() {
        rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val deadlineMs = SystemClock.elapsedRealtime() + 10_000L
        var isWideLandscape = false
        while (!isWideLandscape && SystemClock.elapsedRealtime() < deadlineMs) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val resumed = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .firstOrNull { it is ComponentActivity }
                if (resumed != null) {
                    val config = resumed.resources.configuration
                    isWideLandscape =
                        config.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                            config.screenWidthDp >= 600
                }
            }
            if (!isWideLandscape) SystemClock.sleep(50L)
        }
        assertTrue("Expected wide landscape width >= 600dp", isWideLandscape)
    }

    @Test
    fun wideLandscapeSettings_passesAccessibilityChecks_andCapsContentWidth() {
        switchToWideLandscape()
        rule.setContent {
            MyApplicationTheme(themeMode = ThemeMode.DARK) {
                SettingsScreen(
                    currentSoundPack = SoundPack.STANDARD,
                    themeMode = ThemeMode.DARK,
                    onThemeModeChange = {},
                    onGameModesClick = {},
                    onGameplayClick = {},
                    onSoundAndHapticsClick = {},
                    onStatisticsClicked = {},
                    onResetHighScore = {},
                    onBackPressed = {}
                )
            }
        }
        rule.waitForIdle()
        val cardBounds = rule.onNodeWithText("Game Modes").getUnclippedBoundsInRoot()
        val cardWidth = cardBounds.right - cardBounds.left
        assertTrue(
            "Settings content width ($cardWidth) must stay capped at <= 840.dp",
            cardWidth <= 840.dp
        )
        rule.onAllNodes(isRoot()).tryPerformAccessibilityChecks()
    }
}
