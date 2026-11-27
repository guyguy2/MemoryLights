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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.happypuppy.memorylights.domain.enums.GameMode
import com.happypuppy.memorylights.domain.enums.SimonButton
import com.happypuppy.memorylights.domain.enums.SoundPack
import com.happypuppy.memorylights.domain.model.GameState
import com.happypuppy.memorylights.domain.model.SimonGameUiState
import com.happypuppy.memorylights.ui.components.GameScreenTags
import com.happypuppy.memorylights.ui.screens.SimonGameScreen
import com.happypuppy.memorylights.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class Wave19UiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun fourTile_waitingToStart_hidesRoundOnePackAndBestInsideDisc_andShowsMetadataStrip() {
        var started = 0
        composeRule.setContent {
            MyApplicationTheme {
                SimonGameScreen(
                    uiState = SimonGameUiState(
                        gameState = GameState.WaitingToStart,
                        soundsLoaded = true,
                        highScore4Button = 12,
                        currentSoundPack = SoundPack.RETRO,
                        sequence = listOf(SimonButton.GREEN)
                    ),
                    onButtonClick = { _, _ -> },
                    onSettingsClick = {},
                    onStartNewGame = { started++ },
                    onToggleSound = {},
                    onToggleVibration = {}
                )
            }
        }

        val strip = composeRule.onNodeWithTag(GameScreenTags.SLIM_METADATA_STRIP)
        val grid = composeRule.onNodeWithTag(GameScreenTags.SIMON_BUTTON_GRID)
        val disc = composeRule.onNodeWithTag(GameScreenTags.CENTER_DISC)

        strip.assertIsDisplayed()
        grid.assertIsDisplayed()
        disc.assertIsDisplayed()
        composeRule.onNodeWithTag(GameScreenTags.CENTER_DISC_RING, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(GameScreenTags.FULL_TOP_HUD_STRIP).assertDoesNotExist()

        composeRule.onNodeWithText("★ Best 12").assertIsDisplayed()
        composeRule.onNodeWithText("Retro Gaming").assertIsDisplayed()
        composeRule.onNodeWithText("Tap to start").assertIsDisplayed()
        composeRule.onNodeWithText("1").assertDoesNotExist()
        composeRule.onNodeWithText("High: 12").assertDoesNotExist()

        assertTrue(
            "Slim metadata strip should sit above the 4-tile grid",
            strip.getUnclippedBoundsInRoot().bottom <= grid.getUnclippedBoundsInRoot().top
        )

        disc.performClick()
        assertEquals(1, started)
    }

    @Test
    fun fourTile_playerRepeating_showsStepCounterAndBlocksTouchPassThrough() {
        var uiState by mutableStateOf(
            SimonGameUiState(
                gameState = GameState.ShowingSequence,
                soundsLoaded = true,
                level = 3,
                sequence = listOf(SimonButton.GREEN, SimonButton.RED, SimonButton.YELLOW),
                playerSequence = emptyList()
            )
        )
        var buttonCalls = 0

        composeRule.setContent {
            MyApplicationTheme {
                SimonGameScreen(
                    uiState = uiState,
                    onButtonClick = { _, _ -> buttonCalls++ },
                    onSettingsClick = {},
                    onStartNewGame = {},
                    onToggleSound = {},
                    onToggleVibration = {}
                )
            }
        }

        val disc = composeRule.onNodeWithTag(GameScreenTags.CENTER_DISC)
        disc.assertIsDisplayed()
        composeRule.onNodeWithText("3").assertIsDisplayed()

        // Tap inside center disc during ShowingSequence: must be absorbed
        disc.performTouchInput { click(center) }
        composeRule.waitForIdle()
        assertEquals("Center disc must absorb taps during ShowingSequence", 0, buttonCalls)

        // Switch to PlayerRepeating
        composeRule.runOnIdle {
            uiState = uiState.copy(
                gameState = GameState.PlayerRepeating,
                playerSequence = listOf(SimonButton.GREEN)
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("3").assertIsDisplayed()
        composeRule.onNodeWithText("1 / 3").assertIsDisplayed()
        composeRule.onNodeWithTag(GameScreenTags.CENTER_DISC_RING).assert(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)
        )

        // Tap inside center disc during PlayerRepeating: must still be absorbed
        disc.performTouchInput { click(center) }
        composeRule.waitForIdle()
        assertEquals("Center disc must absorb taps during PlayerRepeating", 0, buttonCalls)

        // Tapping an unobstructed corner of Green button still works
        composeRule.onNodeWithContentDescription("Green button").performTouchInput {
            click(Offset(20f, 20f))
        }
        composeRule.waitForIdle()
        assertTrue("Tapping SimonPanel outside center disc should fire onButtonClick", buttonCalls > 0)
    }

    @Test
    fun fourTile_speedBlitz_integratesSingleHudWithoutStackedBars() {
        composeRule.setContent {
            MyApplicationTheme {
                SimonGameScreen(
                    uiState = SimonGameUiState(
                        gameState = GameState.PlayerRepeating,
                        gameMode = GameMode.SPEED_BLITZ,
                        soundsLoaded = true,
                        level = 5,
                        bestBlitzTime4ButtonMs = 18_400L,
                        blitzStartTimeMs = System.currentTimeMillis() - 5_000L,
                        sequence = List(5) { SimonButton.GREEN },
                        playerSequence = List(2) { SimonButton.GREEN },
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

        val strip = composeRule.onNodeWithTag(GameScreenTags.SLIM_METADATA_STRIP)
        val grid = composeRule.onNodeWithTag(GameScreenTags.SIMON_BUTTON_GRID)

        strip.assertIsDisplayed()
        composeRule.onNodeWithTag(GameScreenTags.FULL_TOP_HUD_STRIP).assertDoesNotExist()
        composeRule.onNodeWithText("Lvl 5 / 20").assertIsDisplayed()
        composeRule.onNodeWithText("★ Best 0:18.4").assertIsDisplayed()
        composeRule.onNodeWithText("Standard").assertIsDisplayed()
        composeRule.onNodeWithText("2 / 5").assertIsDisplayed()

        assertTrue(
            "Single integrated Blitz strip must sit cleanly above the 4-tile grid",
            strip.getUnclippedBoundsInRoot().bottom <= grid.getUnclippedBoundsInRoot().top
        )
    }

    @Test
    fun sixTile_removesCenterDisc_andRendersFullTopHudStrip() {
        var uiState by mutableStateOf(
            SimonGameUiState(
                memoryLightsPlusEnabled = true,
                gameState = GameState.WaitingToStart,
                soundsLoaded = true,
                highScore6Button = 12,
                currentSoundPack = SoundPack.RETRO,
                sequence = listOf(SimonButton.GREEN)
            )
        )
        var started = 0

        composeRule.setContent {
            MyApplicationTheme {
                SimonGameScreen(
                    uiState = uiState,
                    onButtonClick = { _, _ -> },
                    onSettingsClick = {},
                    onStartNewGame = { started++ },
                    onToggleSound = {},
                    onToggleVibration = {}
                )
            }
        }

        composeRule.onNodeWithTag(GameScreenTags.CENTER_DISC).assertDoesNotExist()
        composeRule.onNodeWithTag(GameScreenTags.SLIM_METADATA_STRIP).assertDoesNotExist()

        val hud = composeRule.onNodeWithTag(GameScreenTags.FULL_TOP_HUD_STRIP)
        val grid = composeRule.onNodeWithTag(GameScreenTags.SIMON_BUTTON_GRID)
        val startCta = composeRule.onNodeWithTag(GameScreenTags.SIX_TILE_START_CTA)

        hud.assertIsDisplayed()
        composeRule.onNodeWithText("ROUND").assertIsDisplayed()
        composeRule.onNodeWithText("1").assertIsDisplayed()
        composeRule.onNodeWithText("Ready").assertIsDisplayed()
        composeRule.onNodeWithText("★ Best 12").assertIsDisplayed()
        composeRule.onNodeWithText("Retro Gaming").assertIsDisplayed()
        composeRule.onNodeWithTag(GameScreenTags.COUNTDOWN_BAR).assertIsDisplayed()

        startCta.assertIsDisplayed().performClick()
        assertEquals(1, started)

        // Active play: mid-turn Round 7 step 3 / 7 matching Option A mockup
        composeRule.runOnIdle {
            uiState = uiState.copy(
                gameState = GameState.PlayerRepeating,
                level = 7,
                sequence = List(7) { SimonButton.GREEN },
                playerSequence = List(3) { SimonButton.GREEN }
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(GameScreenTags.CENTER_DISC).assertDoesNotExist()
        composeRule.onNodeWithTag(GameScreenTags.SIX_TILE_START_CTA).assertDoesNotExist()
        composeRule.onNodeWithText("7").assertIsDisplayed()
        composeRule.onNodeWithText("3 / 7").assertIsDisplayed()
        composeRule.onNodeWithTag(GameScreenTags.COUNTDOWN_BAR).assert(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)
        )
        listOf("Green", "Red", "Yellow", "Blue", "Purple", "Orange").forEach { name ->
            composeRule.onNodeWithContentDescription("$name button").assertIsDisplayed()
        }
        assertTrue(
            "Full Top HUD Strip must sit cleanly above the 6-tile grid",
            hud.getUnclippedBoundsInRoot().bottom <= grid.getUnclippedBoundsInRoot().top
        )

        // Speed Blitz in 6-tile integrates inside the same single Full Top HUD Strip
        composeRule.runOnIdle {
            uiState = uiState.copy(
                gameMode = GameMode.SPEED_BLITZ,
                gameState = GameState.Paused,
                level = 5,
                bestBlitzTime6ButtonMs = 18_400L,
                blitzStartTimeMs = 1_000L,
                blitzPauseStartedAtMs = 6_200L
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("BLITZ").assertIsDisplayed()
        composeRule.onNodeWithText("Lvl 5 / 20").assertIsDisplayed()
        composeRule.onNodeWithText("★ Best 0:18.4").assertIsDisplayed()
    }

    @Test
    fun sixTile_gameOver_showsSummaryAndWave18ButtonsWithoutOverlap() {
        composeRule.setContent {
            MyApplicationTheme {
                SimonGameScreen(
                    uiState = SimonGameUiState(
                        memoryLightsPlusEnabled = true,
                        gameState = GameState.GameOver,
                        soundsLoaded = true,
                        level = 4,
                        highScore6Button = 4,
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

        composeRule.onNodeWithTag(GameScreenTags.CENTER_DISC).assertDoesNotExist()
        val hud = composeRule.onNodeWithTag(GameScreenTags.FULL_TOP_HUD_STRIP)
        val grid = composeRule.onNodeWithTag(GameScreenTags.SIMON_BUTTON_GRID)
        val summary = composeRule.onNodeWithTag(GameScreenTags.SIX_TILE_GAME_OVER_SUMMARY)
        val newGame = composeRule.onNodeWithText("New Game")
        val replay = composeRule.onNodeWithText("Replay")
        val share = composeRule.onNodeWithText("Share")

        summary.assertIsDisplayed()
        composeRule.onNodeWithText("Round 4").assertIsDisplayed()
        composeRule.onNodeWithText("Best 4").assertIsDisplayed()
        newGame.assertIsDisplayed()
        replay.assertIsDisplayed()
        share.assertIsDisplayed()

        assertTrue(
            "HUD must sit above 6-tile grid",
            hud.getUnclippedBoundsInRoot().bottom <= grid.getUnclippedBoundsInRoot().top
        )
        assertTrue(
            "6-tile GameOver summary must sit above New Game",
            summary.getUnclippedBoundsInRoot().bottom <= newGame.getUnclippedBoundsInRoot().top
        )
        assertTrue(
            "New Game must sit above Replay",
            newGame.getUnclippedBoundsInRoot().bottom <= replay.getUnclippedBoundsInRoot().top
        )
    }

    @Test
    fun portraitCompactLandscapeAndTablet2xFontScale_noCollisions() {
        val geometries = listOf(
            DpSize(360.dp, 640.dp),
            DpSize(800.dp, 350.dp),
            DpSize(720.dp, 1024.dp)
        )

        var currentSize by mutableStateOf(geometries.first())
        var currentUiState by mutableStateOf(
            SimonGameUiState(
                memoryLightsPlusEnabled = false,
                gameState = GameState.WaitingToStart,
                soundsLoaded = true,
                highScore4Button = 12,
                highScore6Button = 12,
                sequence = listOf(SimonButton.GREEN, SimonButton.RED)
            )
        )

        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 2.0f)
            ) {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(currentSize)
                ) {
                    MyApplicationTheme {
                        @OptIn(ExperimentalLayoutApi::class)
                        Box(
                            modifier = Modifier
                                .size(currentSize)
                                .consumeWindowInsets(WindowInsets.systemBars)
                        ) {
                            SimonGameScreen(
                                uiState = currentUiState,
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
        }

        geometries.forEach { size ->
            // 1) 4-tile WaitingToStart: Tap to start fits inside center disc (#72)
            composeRule.runOnIdle {
                currentSize = size
                currentUiState = currentUiState.copy(
                    memoryLightsPlusEnabled = false,
                    gameState = GameState.WaitingToStart,
                    showYourTurnText = false,
                    showGameOverText = false
                )
            }
            composeRule.waitForIdle()

            val slimBounds = composeRule.onNodeWithTag(GameScreenTags.SLIM_METADATA_STRIP).getUnclippedBoundsInRoot()
            val gridBounds4 = composeRule.onNodeWithTag(GameScreenTags.SIMON_BUTTON_GRID).getUnclippedBoundsInRoot()
            val discBounds = composeRule.onNodeWithTag(GameScreenTags.CENTER_DISC).getUnclippedBoundsInRoot()
            val tapBounds = composeRule.onNodeWithText("Tap to start").getUnclippedBoundsInRoot()

            assertTrue("[$size 4-tile] strip above grid", slimBounds.bottom <= gridBounds4.top)
            assertTrue(
                "[$size 4-tile] Tap to start stays inside center disc at 2.0x fontScale",
                tapBounds.top >= discBounds.top && tapBounds.bottom <= discBounds.bottom
            )

            // 2) 4-tile PlayerRepeating with YOUR TURN overlay: clears both strip and disc (#72)
            composeRule.runOnIdle {
                currentUiState = currentUiState.copy(
                    gameState = GameState.PlayerRepeating,
                    level = 3,
                    playerSequence = listOf(SimonButton.GREEN),
                    showYourTurnText = true
                )
            }
            composeRule.waitForIdle()

            val yourTurnBounds = composeRule.onNodeWithText("YOUR TURN").getUnclippedBoundsInRoot()
            val discPlayBounds = composeRule.onNodeWithTag(GameScreenTags.CENTER_DISC).getUnclippedBoundsInRoot()
            val stripPlayBounds = composeRule.onNodeWithTag(GameScreenTags.SLIM_METADATA_STRIP).getUnclippedBoundsInRoot()

            assertTrue(
                "[$size 4-tile] YOUR TURN (${yourTurnBounds.top}..${yourTurnBounds.bottom}) must clear strip (${stripPlayBounds.bottom}) and center disc (${discPlayBounds.top})",
                yourTurnBounds.top >= stripPlayBounds.bottom && yourTurnBounds.bottom <= discPlayBounds.top
            )

            // 3) 6-tile GameOver with GAME OVER overlay: clears HUD above and summary/New Game below
            composeRule.runOnIdle {
                currentUiState = currentUiState.copy(
                    memoryLightsPlusEnabled = true,
                    gameState = GameState.GameOver,
                    level = 4,
                    showYourTurnText = false,
                    showGameOverText = true
                )
            }
            composeRule.waitForIdle()

            val fullHudBounds = composeRule.onNodeWithTag(GameScreenTags.FULL_TOP_HUD_STRIP).getUnclippedBoundsInRoot()
            val gridBounds6 = composeRule.onNodeWithTag(GameScreenTags.SIMON_BUTTON_GRID).getUnclippedBoundsInRoot()
            val gameOverBounds = composeRule.onNodeWithText("GAME OVER").getUnclippedBoundsInRoot()
            val summaryBounds = composeRule.onNodeWithTag(GameScreenTags.SIX_TILE_GAME_OVER_SUMMARY).getUnclippedBoundsInRoot()
            val newGameBounds = composeRule.onNodeWithText("New Game").getUnclippedBoundsInRoot()

            assertTrue("[$size 6-tile] HUD above grid", fullHudBounds.bottom <= gridBounds6.top)
            assertTrue(
                "[$size 6-tile] GAME OVER clears HUD above and summary below",
                gameOverBounds.top >= fullHudBounds.bottom && gameOverBounds.bottom <= summaryBounds.top
            )
            assertTrue(
                "[$size 6-tile] summary sits above New Game",
                summaryBounds.bottom <= newGameBounds.top
            )
        }
    }
}
