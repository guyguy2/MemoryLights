package com.happypuppy.memorylights.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.happypuppy.memorylights.R
import com.happypuppy.memorylights.domain.GameConstants
import com.happypuppy.memorylights.domain.enums.GameMode
import com.happypuppy.memorylights.domain.enums.SimonButton
import com.happypuppy.memorylights.domain.enums.SoundPack
import com.happypuppy.memorylights.domain.enums.ThemeMode
import com.happypuppy.memorylights.domain.model.Achievement
import com.happypuppy.memorylights.domain.model.GameState
import com.happypuppy.memorylights.domain.model.SimonGameUiState
import com.happypuppy.memorylights.ui.components.CenterDiscContent
import com.happypuppy.memorylights.ui.components.CenterDiscRing
import com.happypuppy.memorylights.ui.components.FullTopHudStrip
import com.happypuppy.memorylights.ui.components.GameScreenTags
import com.happypuppy.memorylights.ui.components.HeadlineOverlay
import com.happypuppy.memorylights.ui.components.ParticleEffect
import com.happypuppy.memorylights.ui.components.PausedOverlay
import com.happypuppy.memorylights.ui.components.ReplayShareButtons
import com.happypuppy.memorylights.ui.components.SimonButtonGrid
import com.happypuppy.memorylights.ui.components.SlimMetadataStrip
import com.happypuppy.memorylights.ui.components.consumeCircularTouch
import com.happypuppy.memorylights.ui.components.formatBlitzTime
import com.happypuppy.memorylights.ui.navigation.GameModesRoute
import com.happypuppy.memorylights.ui.navigation.GameRoute
import com.happypuppy.memorylights.ui.navigation.GameplayRoute
import com.happypuppy.memorylights.ui.navigation.SettingsRoute
import com.happypuppy.memorylights.ui.navigation.SoundAndHapticsRoute
import com.happypuppy.memorylights.ui.navigation.StatisticsRoute
import com.happypuppy.memorylights.ui.share.ScoreShareHelper
import com.happypuppy.memorylights.ui.theme.LocalGameAccentColors
import com.happypuppy.memorylights.ui.theme.LocalReducedMotion
import com.happypuppy.memorylights.ui.theme.MyApplicationTheme
import com.happypuppy.memorylights.ui.viewmodels.SimonGameViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow


@Composable
fun MemoryLightsGame(viewModel: SimonGameViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    // Whenever the back-stack lands back on the Game route, restore the
    // game phase that was active before the player navigated away. The
    // ViewModel captures `previousGameState` at the moment of [showSettings]
    // so this side effect is safe to fire whenever GameRoute reappears
    // (initial composition, return from Settings, return from Statistics).
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    LaunchedEffect(currentDestination) {
        if (currentDestination?.hasRoute<GameRoute>() == true) {
            viewModel.onReturnToGame()
        }
    }

    // Snapshot the active game phase at the moment the user opens Settings
    // — needed by the mid-game-mode-switch confirmation dialog. Updated on
    // each navigation entry into Settings via the LaunchedEffect below.
    var gameStateAtSettingsEntry by remember { mutableStateOf(uiState.gameState) }

    NavHost(navController = navController, startDestination = GameRoute) {
        composable<GameRoute> {
            SimonGameScreen(
                uiState = uiState,
                onButtonClick = { button, isPress -> viewModel.onButtonClick(button, isPress) },
                onSettingsClick = {
                    // showSettings may land a pending game over first, so read
                    // the state after it (and from the VM, not the composed snapshot).
                    viewModel.showSettings()
                    gameStateAtSettingsEntry = viewModel.uiState.value.gameState
                    navController.navigate(SettingsRoute)
                },
                onStartNewGame = { viewModel.startNewGame() },
                onReplayLastSequence = { viewModel.replayLastSequence() },
                onToggleSound = { viewModel.toggleSound() },
                onToggleVibration = { viewModel.toggleVibration() },
                onPauseGame = { viewModel.pauseGame() },
                onResumeGame = { viewModel.resumeGame() },
                onClearParticleEffects = { viewModel.clearParticleEffects() },
                unlockEvents = viewModel.unlockEvents
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                currentSoundPack = uiState.currentSoundPack,
                themeMode = uiState.themeMode,
                onThemeModeChange = { viewModel.setThemeMode(it) },
                onGameModesClick = { navController.navigate(GameModesRoute) },
                onGameplayClick = { navController.navigate(GameplayRoute) },
                onSoundAndHapticsClick = { navController.navigate(SoundAndHapticsRoute) },
                onResetHighScore = { viewModel.resetHighScore() },
                onStatisticsClicked = {
                    viewModel.showStatistics()
                    navController.navigate(StatisticsRoute)
                },
                onBackPressed = { navController.popBackStack() },
                unlockedAchievements = uiState.unlockedAchievements
            )
        }
        composable<GameModesRoute> {
            GameModesScreen(
                difficultyEnabled = uiState.difficultyEnabled,
                reverseModeEnabled = uiState.reverseModeEnabled,
                practiceModeEnabled = uiState.practiceModeEnabled,
                audioOnlyModeEnabled = uiState.audioOnlyModeEnabled,
                memoryLightsPlusEnabled = uiState.memoryLightsPlusEnabled,
                dailyChallengeEnabled = uiState.dailyChallengeEnabled,
                dailyCompletedToday = uiState.dailyCompletedEpochDay == java.time.LocalDate.now().toEpochDay(),
                dailyBestLevel = uiState.dailyBestLevel,
                gameMode = uiState.gameMode,
                hasActiveGame = gameStateAtSettingsEntry is GameState.ShowingSequence ||
                        gameStateAtSettingsEntry is GameState.PlayerRepeating ||
                        gameStateAtSettingsEntry is GameState.Paused,
                onDifficultyToggled = { viewModel.setDifficultyEnabled(it) },
                onReverseModeToggled = { viewModel.setReverseModeEnabled(it) },
                onPracticeModeToggled = { viewModel.setPracticeModeEnabled(it) },
                onAudioOnlyModeToggled = { viewModel.setAudioOnlyModeEnabled(it) },
                onMemoryLightsPlusToggled = { viewModel.setMemoryLightsPlusEnabled(it) },
                onDailyChallengeToggled = { viewModel.setDailyChallengeEnabled(it) },
                onGameModeSelected = { viewModel.setGameMode(it) },
                onBackPressed = { navController.popBackStack() }
            )
        }
        composable<GameplayRoute> {
            GameplayScreen(
                playerTimeoutSeconds = uiState.playerTimeoutSeconds,
                colorBlindModeEnabled = uiState.colorBlindModeEnabled,
                onPlayerTimeoutChanged = { viewModel.setPlayerTimeoutSeconds(it) },
                onColorBlindModeToggled = { viewModel.setColorBlindModeEnabled(it) },
                onBackPressed = { navController.popBackStack() }
            )
        }
        composable<SoundAndHapticsRoute> {
            SoundAndHapticsScreen(
                currentSoundPack = uiState.currentSoundPack,
                onSoundPackSelected = { viewModel.setSoundPack(it) },
                onBackPressed = { navController.popBackStack() }
            )
        }
        composable<StatisticsRoute> {
            StatisticsScreen(
                statistics = uiState.statistics,
                currentHighScore = uiState.currentHighScore,
                unlockedAchievements = uiState.unlockedAchievements,
                onResetStatistics = { viewModel.resetStatistics() },
                onBackPressed = {
                    viewModel.exitStatistics()
                    navController.popBackStack()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimonGameScreen(
    uiState: SimonGameUiState,
    onButtonClick: (SimonButton, Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    onStartNewGame: () -> Unit,
    onReplayLastSequence: () -> Unit = {},
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit,
    onPauseGame: () -> Unit = {},
    onResumeGame: () -> Unit = {},
    onClearParticleEffects: (() -> Unit)? = null,
    unlockEvents: Flow<List<Achievement>> = emptyFlow()
) {
    // Single source of truth for which buttons are physically pressed (for UI feedback only)
    var localPressedButtons by remember { mutableStateOf(mapOf<SimonButton, Boolean>()) }

    val context = LocalContext.current
    val resources = LocalResources.current
    val onSurface = MaterialTheme.colorScheme.onSurface
    val accents = LocalGameAccentColors.current
    val reducedMotion = LocalReducedMotion.current

    // Polite live-region text for TalkBack announcements (replaces deprecated announceForAccessibility, #70).
    var liveAnnouncement by remember { mutableStateOf("") }

    // Announce level changes (skip initial level 1 on game start)
    var previousLevel by remember { mutableIntStateOf(0) }
    LaunchedEffect(uiState.level) {
        if (previousLevel > 0 && uiState.level > previousLevel) {
            liveAnnouncement = resources.getString(R.string.a11y_level_announce, uiState.level)
        }
        previousLevel = uiState.level
    }

    // Announce game state changes
    LaunchedEffect(uiState.gameState) {
        when (uiState.gameState) {
            GameState.PlayerRepeating -> {
                liveAnnouncement = resources.getString(R.string.a11y_your_turn)
            }
            GameState.GameOver -> {
                liveAnnouncement = resources.getString(R.string.a11y_game_over_level, uiState.level)
            }
            GameState.Paused -> {
                liveAnnouncement = resources.getString(R.string.a11y_paused)
            }
            GameState.ShowingSequence,
            GameState.WaitingToStart -> {
                liveAnnouncement = ""
            }
        }
    }

    // Announce new high score celebration
    LaunchedEffect(uiState.showHighScoreText) {
        if (uiState.showHighScoreText) {
            liveAnnouncement = resources.getString(R.string.a11y_high_score_celebrate, uiState.level)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(unlockEvents) {
        unlockEvents.collect { newlyUnlocked ->
            if (newlyUnlocked.isEmpty()) return@collect
            val message = if (newlyUnlocked.size == 1) {
                resources.getString(
                    R.string.achievement_unlocked_toast,
                    resources.getString(newlyUnlocked.first().titleRes)
                )
            } else {
                resources.getString(
                    R.string.achievement_unlocked_toast_multi,
                    newlyUnlocked.size
                )
            }
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    // Audio-Only Mode (F3): swallow currentlyLit during the watch phase so
    // the panels (and 4-tile reactive disc ring) stay dark while the sound plays.
    val displayLit: SimonButton? = if (
        uiState.audioOnlyModeEnabled && uiState.gameState == GameState.ShowingSequence
    ) null else uiState.currentlyLit

    val handleButtonInteraction = remember(onButtonClick) {
        { button: SimonButton, isPress: Boolean ->
            if (isPress) {
                localPressedButtons = localPressedButtons + (button to true)
            } else {
                localPressedButtons = localPressedButtons - button
            }
            onButtonClick(button, isPress)
        }
    }

    val isIdleTappable = uiState.gameState == GameState.GameOver ||
        uiState.gameState == GameState.WaitingToStart
    val ctaScale = if (reducedMotion || !isIdleTappable) {
        1f
    } else {
        val pulseTransition = rememberInfiniteTransition(label = "ctaPulse")
        val animatedScale by pulseTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ctaScalePulse"
        )
        animatedScale
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    // Pause button — leftmost slot and always rendered so
                    // Mute, Vibration, and Settings never shift across turns.
                    val canPause = uiState.gameState == GameState.PlayerRepeating
                    IconButton(
                        onClick = onPauseGame,
                        enabled = canPause
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.pause_24px),
                            contentDescription = stringResource(R.string.action_pause),
                            tint = if (canPause) onSurface else onSurface.copy(alpha = 0.35f),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Mute/Unmute button
                    IconButton(onClick = onToggleSound) {
                        Icon(
                            painter = painterResource(
                                id = if (uiState.soundEnabled)
                                    R.drawable.volume_up_24px
                                else
                                    R.drawable.volume_off_24px
                            ),
                            contentDescription = stringResource(
                                if (uiState.soundEnabled) R.string.game_action_mute
                                else R.string.game_action_unmute
                            ),
                            tint = onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Vibration toggle button
                    IconButton(onClick = onToggleVibration) {
                        Icon(
                            painter = painterResource(
                                id = if (uiState.vibrateEnabled)
                                    R.drawable.vibration_24px
                                else
                                    R.drawable.vibration_off_24px
                            ),
                            contentDescription = stringResource(
                                if (uiState.vibrateEnabled) R.string.game_action_disable_vibration
                                else R.string.game_action_enable_vibration
                            ),
                            tint = onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Settings button
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.action_settings),
                            tint = onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = onSurface,
                    actionIconContentColor = onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            contentAlignment = Alignment.Center
        ) {
            if (liveAnnouncement.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(1.dp)
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = liveAnnouncement
                        }
                )
            }
            // Option A layout: width-capped Column with Top HUD/Metadata Strip + Play Board.
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight(0.97f)
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 500.dp)
                    .padding(top = 4.dp, bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.memoryLightsPlusEnabled) {
                    FullTopHudStrip(
                        gameMode = uiState.gameMode,
                        gameState = uiState.gameState,
                        level = uiState.level,
                        targetLevel = GameConstants.BLITZ_TARGET_LEVEL,
                        sequenceSize = uiState.sequence.size,
                        playerSequenceSize = uiState.playerSequence.size,
                        currentHighScore = uiState.currentHighScore,
                        currentBestBlitzTimeMs = uiState.currentBestBlitzTimeMs,
                        blitzStartTimeMs = uiState.blitzStartTimeMs,
                        blitzPauseStartedAtMs = uiState.blitzPauseStartedAtMs,
                        blitzElapsedMs = uiState.blitzElapsedMs,
                        blitzWon = uiState.blitzWon,
                        soundsLoaded = uiState.soundsLoaded,
                        soundLoadError = uiState.soundLoadError,
                        timeoutResetTick = uiState.timeoutResetTick,
                        playerTimeoutMs = uiState.playerTimeoutMs,
                        soundPackDisplayNameRes = uiState.currentSoundPack.displayNameRes
                    )
                } else {
                    SlimMetadataStrip(
                        gameMode = uiState.gameMode,
                        gameState = uiState.gameState,
                        level = uiState.level,
                        targetLevel = GameConstants.BLITZ_TARGET_LEVEL,
                        currentHighScore = uiState.currentHighScore,
                        currentBestBlitzTimeMs = uiState.currentBestBlitzTimeMs,
                        blitzStartTimeMs = uiState.blitzStartTimeMs,
                        blitzPauseStartedAtMs = uiState.blitzPauseStartedAtMs,
                        blitzElapsedMs = uiState.blitzElapsedMs,
                        soundPackDisplayNameRes = uiState.currentSoundPack.displayNameRes
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Play Board slot
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    SimonButtonGrid(
                        memoryLightsPlusEnabled = uiState.memoryLightsPlusEnabled,
                        litButton = displayLit,
                        allButtonsLit = uiState.allButtonsLit,
                        pressedButtons = localPressedButtons,
                        colorBlindModeEnabled = uiState.colorBlindModeEnabled,
                        onButtonInteraction = handleButtonInteraction,
                        modifier = Modifier.testTag(GameScreenTags.SIMON_BUTTON_GRID)
                    )

                    if (!uiState.memoryLightsPlusEnabled) {
                        // 4-tile decluttered focal disc (~104.dp) with themed inset gutter,
                        // unified reactive ring, and active-play circular touch absorption.
                        val discSurface = MaterialTheme.colorScheme.surfaceContainerLow
                        val canTapDiscToStart = isIdleTappable &&
                            (uiState.soundsLoaded || uiState.soundLoadError != null)
                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .zIndex(3f)
                                .testTag(GameScreenTags.CENTER_DISC)
                                .graphicsLayer(
                                    scaleX = if (canTapDiscToStart) ctaScale else 1f,
                                    scaleY = if (canTapDiscToStart) ctaScale else 1f
                                )
                                .then(
                                    if (canTapDiscToStart) {
                                        Modifier
                                            .shadow(
                                                elevation = 8.dp,
                                                shape = CircleShape,
                                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                            )
                                            .clip(CircleShape)
                                            .background(discSurface)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple(),
                                                onClickLabel = stringResource(
                                                    if (uiState.gameState == GameState.GameOver) R.string.game_new_game
                                                    else R.string.game_tap_to_start
                                                ),
                                                role = Role.Button,
                                                onClick = { onStartNewGame() }
                                            )
                                    } else {
                                        Modifier
                                            .clip(CircleShape)
                                            .background(discSurface)
                                            .consumeCircularTouch()
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            CenterDiscRing(
                                isPlayerTurn = uiState.gameState == GameState.PlayerRepeating,
                                timeoutMs = uiState.playerTimeoutMs,
                                resetTick = uiState.timeoutResetTick,
                                litButton = displayLit
                            )

                            CenterDiscContent(
                                gameState = uiState.gameState,
                                gameMode = uiState.gameMode,
                                level = uiState.level,
                                currentHighScore = uiState.currentHighScore,
                                blitzElapsedMs = uiState.blitzElapsedMs,
                                currentBestBlitzTimeMs = uiState.currentBestBlitzTimeMs,
                                soundLoadError = uiState.soundLoadError,
                                soundsLoaded = uiState.soundsLoaded,
                                sequenceSize = uiState.sequence.size,
                                playerSequenceSize = uiState.playerSequence.size
                            )
                        }
                    } else if (uiState.gameState == GameState.WaitingToStart) {
                        // 6-tile mode has NO center disc; show accessible Start CTA / status pill.
                        SixTileWaitingAffordance(
                            soundsLoaded = uiState.soundsLoaded,
                            soundLoadError = uiState.soundLoadError,
                            ctaScale = ctaScale,
                            onStartNewGame = onStartNewGame,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .zIndex(4f)
                        )
                    }

                    // Collision-free headline overlays inside the Play Board Box.
                    val isSixTileGameOver = uiState.memoryLightsPlusEnabled && uiState.gameState == GameState.GameOver
                    val overlaySlotModifier = if (!uiState.memoryLightsPlusEnabled) {
                        // Strictly in upper half above the 104.dp center disc (56.dp bottom clearance >= 52.dp radius)
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f)
                            .padding(bottom = 56.dp, start = 8.dp, end = 8.dp)
                    } else if (isSixTileGameOver) {
                        // Upper region above GameOver summary + ReplayShareButtons
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(0.42f)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    } else {
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    }

                    BoxWithConstraints(
                        modifier = overlaySlotModifier
                            .zIndex(5f)
                            .testTag(GameScreenTags.HEADLINE_OVERLAY_CONTAINER),
                        contentAlignment = if (isSixTileGameOver) Alignment.TopCenter else Alignment.Center
                    ) {
                        val compactOverlay = maxHeight < 96.dp
                        val headlineStyle = when {
                            isSixTileGameOver && maxHeight < 120.dp -> MaterialTheme.typography.titleMedium
                            compactOverlay -> MaterialTheme.typography.titleLarge
                            else -> MaterialTheme.typography.headlineLarge
                        }
                        val speedUpStyle = if (compactOverlay) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.headlineMedium
                        }
                        val turnStyle = if (compactOverlay) {
                            MaterialTheme.typography.titleSmall
                        } else {
                            MaterialTheme.typography.headlineSmall
                        }
                        val overlayHeadlineColor = if (uiState.blitzWon) accents.victory else accents.headlineHighlight

                        Box(contentAlignment = Alignment.Center) {
                            val isGameOver = uiState.gameState == GameState.GameOver
                            HeadlineOverlay(
                                visible = isGameOver && uiState.showHighScoreText,
                                text = stringResource(
                                    if (uiState.blitzWon) R.string.overlay_best_time
                                    else R.string.overlay_high_score
                                ),
                                color = overlayHeadlineColor,
                                style = headlineStyle,
                                initialScale = 0.7f
                            )

                            HeadlineOverlay(
                                visible = isGameOver && uiState.showGameOverText,
                                text = stringResource(
                                    if (uiState.blitzWon) R.string.overlay_victory
                                    else R.string.overlay_game_over
                                ),
                                color = overlayHeadlineColor,
                                style = headlineStyle,
                                initialScale = 0.85f
                            )

                            HeadlineOverlay(
                                visible = !isGameOver && uiState.showSpeedUpText,
                                text = stringResource(R.string.overlay_speed_up),
                                color = accents.speedUp,
                                style = speedUpStyle,
                                initialScale = 0.7f
                            )

                            HeadlineOverlay(
                                visible = uiState.gameState == GameState.PlayerRepeating &&
                                    uiState.showYourTurnText &&
                                    !uiState.showSpeedUpText,
                                text = stringResource(R.string.overlay_your_turn),
                                color = onSurface,
                                style = turnStyle,
                                initialScale = 0.9f,
                                useScaleOut = false
                            )
                        }
                    }
                }
            }

            // Particle effect overlay for new high score celebration
            if (uiState.showHighScoreParticles) {
                ParticleEffect(
                    isActive = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(7f),
                    onComplete = { onClearParticleEffects?.invoke() }
                )
            }

            // Game Over action cluster + 6-tile compact score/best summary
            ReplayShareButtons(
                visible = uiState.gameState == GameState.GameOver &&
                        uiState.sequence.isNotEmpty(),
                showReplay = uiState.gameMode != GameMode.SPEED_BLITZ,
                onNewGame = onStartNewGame,
                onReplay = onReplayLastSequence,
                onShare = {
                    ScoreShareHelper.shareScore(
                        context = context,
                        level = uiState.level,
                        bestScore = uiState.currentHighScore,
                        is6ButtonMode = uiState.memoryLightsPlusEnabled
                    )
                },
                modifier = Modifier.zIndex(6f),
                topContent = if (uiState.memoryLightsPlusEnabled) {
                    {
                        SixTileGameOverSummary(
                            gameMode = uiState.gameMode,
                            level = uiState.level,
                            currentHighScore = uiState.currentHighScore,
                            blitzElapsedMs = uiState.blitzElapsedMs,
                            currentBestBlitzTimeMs = uiState.currentBestBlitzTimeMs
                        )
                    }
                } else null
            )

            // Paused overlay (F14)
            PausedOverlay(
                visible = uiState.gameState == GameState.Paused,
                onResume = onResumeGame
            )
        }
    }
}

@Composable
private fun SixTileWaitingAffordance(
    soundsLoaded: Boolean,
    soundLoadError: String?,
    ctaScale: Float,
    onStartNewGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val accents = LocalGameAccentColors.current
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (soundLoadError != null) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, accents.countdownAmber.copy(alpha = 0.6f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = stringResource(R.string.game_sound_load_failed),
                        tint = accents.countdownAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.game_sound_load_failed),
                            color = accents.countdownAmber,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.game_audio_fallback),
                            color = onSurface.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (!soundsLoaded && soundLoadError == null) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = onSurface,
                        strokeWidth = 2.5.dp
                    )
                    Text(
                        text = stringResource(R.string.game_loading),
                        color = onSurface,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        } else {
            Button(
                onClick = onStartNewGame,
                shape = MaterialTheme.shapes.extraLarge,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .defaultMinSize(minWidth = 148.dp, minHeight = 52.dp)
                    .testTag(GameScreenTags.SIX_TILE_START_CTA)
                    .graphicsLayer(scaleX = ctaScale, scaleY = ctaScale)
            ) {
                Icon(
                    painter = painterResource(R.drawable.play_arrow_24px),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.game_start),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SixTileGameOverSummary(
    gameMode: GameMode,
    level: Int,
    currentHighScore: Int,
    blitzElapsedMs: Long,
    currentBestBlitzTimeMs: Long
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val accents = LocalGameAccentColors.current
    val isBlitzWithTime = gameMode == GameMode.SPEED_BLITZ && blitzElapsedMs > 0L
    val scoreText = if (isBlitzWithTime) {
        formatBlitzTime(blitzElapsedMs)
    } else {
        stringResource(R.string.game_round_score, level)
    }
    val bestText = when {
        gameMode == GameMode.SPEED_BLITZ && currentBestBlitzTimeMs > 0L ->
            stringResource(R.string.game_best_time, formatBlitzTime(currentBestBlitzTimeMs))
        gameMode == GameMode.CLASSIC && currentHighScore > 0 ->
            stringResource(R.string.game_best_score, currentHighScore)
        else -> null
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.testTag(GameScreenTags.SIX_TILE_GAME_OVER_SUMMARY)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = scoreText,
                color = onSurface,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (bestText != null) {
                Text(
                    text = stringResource(R.string.blitz_separator),
                    color = onSurface.copy(alpha = 0.45f),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = bestText,
                    color = accents.highScore,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@PreviewLightDark
@PreviewFontScale
@PreviewScreenSizes
@Composable
private fun SimonGameScreenFourTilePreview() {
    MyApplicationTheme(
        themeMode = if (isSystemInDarkTheme()) ThemeMode.AMOLED else ThemeMode.LIGHT
    ) {
        SimonGameScreen(
            uiState = SimonGameUiState(
                gameState = GameState.PlayerRepeating,
                soundsLoaded = true,
                level = 5,
                highScore4Button = 14,
                currentSoundPack = SoundPack.STANDARD,
                sequence = listOf(
                    SimonButton.GREEN,
                    SimonButton.RED,
                    SimonButton.BLUE,
                    SimonButton.YELLOW,
                    SimonButton.GREEN
                ),
                playerSequence = listOf(SimonButton.GREEN, SimonButton.RED)
            ),
            onButtonClick = { _, _ -> },
            onSettingsClick = {},
            onStartNewGame = {},
            onToggleSound = {},
            onToggleVibration = {}
        )
    }
}

@PreviewLightDark
@PreviewFontScale
@PreviewScreenSizes
@Composable
private fun SimonGameScreenSixTilePreview() {
    MyApplicationTheme(
        themeMode = if (isSystemInDarkTheme()) ThemeMode.DARK else ThemeMode.LIGHT
    ) {
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
                currentSoundPack = SoundPack.RETRO,
                sequence = List(7) { SimonButton.GREEN },
                playerSequence = List(3) { SimonButton.GREEN }
            ),
            onButtonClick = { _, _ -> },
            onSettingsClick = {},
            onStartNewGame = {},
            onToggleSound = {},
            onToggleVibration = {}
        )
    }
}

@Preview(name = "4-Tile Waiting AMOLED", showBackground = true)
@Composable
private fun SimonGameScreenWaitingPreview() {
    MyApplicationTheme(themeMode = ThemeMode.AMOLED) {
        SimonGameScreen(
            uiState = SimonGameUiState(
                gameState = GameState.WaitingToStart,
                soundsLoaded = true,
                highScore4Button = 12,
                currentSoundPack = SoundPack.ELECTRONIC
            ),
            onButtonClick = { _, _ -> },
            onSettingsClick = {},
            onStartNewGame = {},
            onToggleSound = {},
            onToggleVibration = {}
        )
    }
}
