package com.happypuppy.memorylights

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.happypuppy.memorylights.domain.enums.GameMode
import com.happypuppy.memorylights.domain.enums.SimonButton
import com.happypuppy.memorylights.domain.enums.SoundPack
import com.happypuppy.memorylights.domain.model.GameState
import com.happypuppy.memorylights.domain.model.SimonGameUiState
import com.happypuppy.memorylights.ui.screens.SimonGameScreen
import com.happypuppy.memorylights.ui.screens.SoundPackOption
import com.happypuppy.memorylights.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class Wave18UiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun topAppBar_pauseButtonIsLeftmostAndStationaryAcrossStates() {
        var uiState by mutableStateOf(
            SimonGameUiState(
                gameState = GameState.WaitingToStart,
                soundsLoaded = true,
                sequence = listOf(SimonButton.GREEN)
            )
        )

        composeRule.setContent {
            MyApplicationTheme {
                SimonGameScreen(
                    uiState = uiState,
                    onButtonClick = { _, _ -> },
                    onSettingsClick = {},
                    onStartNewGame = {},
                    onToggleSound = {},
                    onToggleVibration = {}
                )
            }
        }

        val pause = composeRule.onNodeWithContentDescription("Pause")
        val mute = composeRule.onNodeWithContentDescription("Mute")
        val vibrate = composeRule.onNodeWithContentDescription("Disable Vibration")
        val settings = composeRule.onNodeWithContentDescription("Settings")

        pause.assertIsDisplayed().assertIsNotEnabled()
        val pauseLeft = pause.getUnclippedBoundsInRoot().left
        val muteLeft = mute.getUnclippedBoundsInRoot().left
        val vibrateLeft = vibrate.getUnclippedBoundsInRoot().left
        val settingsLeft = settings.getUnclippedBoundsInRoot().left

        assertTrue("Pause should be leftmost in actions", pauseLeft < muteLeft)
        assertTrue(muteLeft < vibrateLeft)
        assertTrue(vibrateLeft < settingsLeft)

        listOf(
            GameState.ShowingSequence to false,
            GameState.PlayerRepeating to true,
            GameState.Paused to false,
            GameState.GameOver to false
        ).forEach { (state, expectEnabled) ->
            composeRule.runOnIdle {
                uiState = uiState.copy(gameState = state)
            }
            composeRule.waitForIdle()
            if (expectEnabled) {
                pause.assertIsDisplayed().assertIsEnabled()
            } else {
                pause.assertIsDisplayed().assertIsNotEnabled()
            }
            assertEquals("Mute shifted in $state", muteLeft, mute.getUnclippedBoundsInRoot().left)
            assertEquals("Vibrate shifted in $state", vibrateLeft, vibrate.getUnclippedBoundsInRoot().left)
            assertEquals("Settings shifted in $state", settingsLeft, settings.getUnclippedBoundsInRoot().left)
        }
    }

    @Test
    fun gameOver_displaysProminentNewGameAboveReplayAndShare() {
        var newGameClicks = 0
        composeRule.setContent {
            MyApplicationTheme {
                SimonGameScreen(
                    uiState = SimonGameUiState(
                        gameState = GameState.GameOver,
                        soundsLoaded = true,
                        level = 4,
                        sequence = listOf(SimonButton.GREEN, SimonButton.RED)
                    ),
                    onButtonClick = { _, _ -> },
                    onSettingsClick = {},
                    onStartNewGame = { newGameClicks++ },
                    onToggleSound = {},
                    onToggleVibration = {}
                )
            }
        }

        val newGameNode = composeRule.onNodeWithText("New Game")
        val replayNode = composeRule.onNodeWithText("Replay")
        val shareNode = composeRule.onNodeWithText("Share")

        newGameNode.assertIsDisplayed()
        replayNode.assertIsDisplayed()
        shareNode.assertIsDisplayed()

        assertTrue(
            "New Game button should sit above Replay/Share row",
            newGameNode.getUnclippedBoundsInRoot().bottom <= replayNode.getUnclippedBoundsInRoot().top
        )

        newGameNode.performClick()
        assertEquals(1, newGameClicks)

        // Center disc shortcut also remains clickable on Game Over
        composeRule.onNodeWithContentDescription("Play Again").performClick()
        assertEquals(2, newGameClicks)
    }

    @Test
    fun soundPackOption_comingSoon_isVisibleDisabledAndShowsBadge() {
        var selected = false
        composeRule.setContent {
            MyApplicationTheme {
                SoundPackOption(
                    soundPack = SoundPack.FUNNY,
                    isSelected = false,
                    onSelect = { selected = true },
                    isAvailable = false
                )
            }
        }

        composeRule.onNodeWithText("Funny").assertIsDisplayed()
        composeRule.onNodeWithText("Humorous sound effects").assertIsDisplayed()
        composeRule.onNodeWithText("Coming soon").assertIsDisplayed()
        composeRule.onNodeWithText("Funny").performClick()
        assertFalse("Disabled Coming soon pack should not trigger onSelect", selected)
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    fun gameOver_compactLandscapeHeight_buttonsSitBelowCenterDiscWithoutOverlap() {
        composeRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(800.dp, 350.dp))
            ) {
                MyApplicationTheme {
                    Box(
                        modifier = Modifier
                            .size(width = 800.dp, height = 350.dp)
                            .consumeWindowInsets(WindowInsets.systemBars)
                    ) {
                        SimonGameScreen(
                            uiState = SimonGameUiState(
                                gameState = GameState.GameOver,
                                soundsLoaded = true,
                                level = 4,
                                highScore4Button = 4,
                                sequence = listOf(SimonButton.GREEN, SimonButton.RED)
                            ),
                            onButtonClick = { _, _ -> },
                            onSettingsClick = {},
                            onStartNewGame = {},
                            onToggleSound = {},
                            onToggleVibration = {}
                        )
                    }
                }
            }
        }

        val newGameNode = composeRule.onNodeWithText("New Game")
        val replayNode = composeRule.onNodeWithText("Replay")
        val shareNode = composeRule.onNodeWithText("Share")
        val playAgainNode = composeRule.onNodeWithContentDescription("Play Again")
        val bestScoreNode = composeRule.onNodeWithText("Best 4")

        newGameNode.assertIsDisplayed()
        replayNode.assertIsDisplayed()
        shareNode.assertIsDisplayed()
        playAgainNode.assertIsDisplayed()
        bestScoreNode.assertIsDisplayed()

        val newGameBounds = newGameNode.getUnclippedBoundsInRoot()
        val playAgainBounds = playAgainNode.getUnclippedBoundsInRoot()
        val bestScoreBounds = bestScoreNode.getUnclippedBoundsInRoot()
        val replayBounds = replayNode.getUnclippedBoundsInRoot()

        assertTrue(
            "New Game (top=${newGameBounds.top}) must sit below Play Again (bottom=${playAgainBounds.bottom})",
            newGameBounds.top >= playAgainBounds.bottom
        )
        assertTrue(
            "New Game (top=${newGameBounds.top}) must sit below Best 4 (bottom=${bestScoreBounds.bottom})",
            newGameBounds.top >= bestScoreBounds.bottom
        )
        assertTrue(
            "New Game and Replay should sit on the same horizontal row in compact height",
            newGameBounds.right <= replayBounds.left
        )
    }

    @Test
    fun gameOver_speedBlitz_hidesReplayButton() {
        composeRule.setContent {
            MyApplicationTheme {
                SimonGameScreen(
                    uiState = SimonGameUiState(
                        gameState = GameState.GameOver,
                        gameMode = GameMode.SPEED_BLITZ,
                        soundsLoaded = true,
                        level = 5,
                        blitzElapsedMs = 8_400L,
                        sequence = listOf(SimonButton.GREEN, SimonButton.RED)
                    ),
                    onButtonClick = { _, _ -> },
                    onSettingsClick = {},
                    onStartNewGame = {},
                    onToggleSound = {},
                    onToggleVibration = {}
                )
            }
        }

        composeRule.onNodeWithText("New Game").assertIsDisplayed()
        composeRule.onNodeWithText("Share").assertIsDisplayed()
        composeRule.onNodeWithText("Replay").assertDoesNotExist()
    }

    @Test
    fun topAppBar_titleRemainsSingleLineAt2xFontScale() {
        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 2.0f)
            ) {
                MyApplicationTheme {
                    Box(modifier = Modifier.size(width = 360.dp, height = 640.dp)) {
                        SimonGameScreen(
                            uiState = SimonGameUiState(
                                gameState = GameState.WaitingToStart,
                                soundsLoaded = true,
                                sequence = listOf(SimonButton.GREEN)
                            ),
                            onButtonClick = { _, _ -> },
                            onSettingsClick = {},
                            onStartNewGame = {},
                            onToggleSound = {},
                            onToggleVibration = {}
                        )
                    }
                }
            }
        }

        val titleBounds = composeRule.onNodeWithText("Memory Lights").getUnclippedBoundsInRoot()
        val titleHeight = titleBounds.bottom - titleBounds.top
        assertTrue(
            "Title height ($titleHeight) at 2.0x font scale should stay single-line inside 64.dp TopAppBar",
            titleHeight <= 64.dp
        )
    }
}
