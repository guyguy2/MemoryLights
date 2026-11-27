package com.happypuppy.memorylights.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ripple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.happypuppy.memorylights.ui.share.ScoreShareHelper
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.happypuppy.memorylights.R
import com.happypuppy.memorylights.domain.GameConstants
import com.happypuppy.memorylights.domain.enums.GameMode
import com.happypuppy.memorylights.domain.enums.SimonButton
import com.happypuppy.memorylights.domain.model.Achievement
import com.happypuppy.memorylights.domain.model.GameState
import com.happypuppy.memorylights.domain.model.SimonGameUiState
import com.happypuppy.memorylights.ui.components.BlitzHud
import com.happypuppy.memorylights.ui.components.CenterDiscCanvas
import com.happypuppy.memorylights.ui.components.CenterDiscContent
import com.happypuppy.memorylights.ui.components.HeadlineOverlay
import com.happypuppy.memorylights.ui.components.OverlayBias4Button
import com.happypuppy.memorylights.ui.components.OverlayBias4ButtonTurn
import com.happypuppy.memorylights.ui.components.OverlayBias6Button
import com.happypuppy.memorylights.ui.components.ParticleEffect
import com.happypuppy.memorylights.ui.components.PausedOverlay
import com.happypuppy.memorylights.ui.components.PlayerTurnHalo
import com.happypuppy.memorylights.ui.components.ReplayShareButtons
import com.happypuppy.memorylights.ui.components.SimonButtonGrid
import com.happypuppy.memorylights.ui.components.TimeoutCountdownRing
import com.happypuppy.memorylights.ui.navigation.GameModesRoute
import com.happypuppy.memorylights.ui.navigation.GameRoute
import com.happypuppy.memorylights.ui.navigation.GameplayRoute
import com.happypuppy.memorylights.ui.navigation.SettingsRoute
import com.happypuppy.memorylights.ui.navigation.SoundAndHapticsRoute
import com.happypuppy.memorylights.ui.navigation.StatisticsRoute
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

    // Screen reader announcements for accessibility
    val view = LocalView.current

    val context = LocalContext.current
    val resources = LocalResources.current

    // Announce level changes (skip initial level 1 on game start)
    var previousLevel by remember { mutableIntStateOf(0) }
    LaunchedEffect(uiState.level) {
        if (previousLevel > 0 && uiState.level > previousLevel) {
            view.announceForAccessibility(resources.getString(R.string.a11y_level_announce, uiState.level))
        }
        previousLevel = uiState.level
    }

    // Announce game state changes
    LaunchedEffect(uiState.gameState) {
        when (uiState.gameState) {
            GameState.PlayerRepeating -> {
                view.announceForAccessibility(resources.getString(R.string.a11y_your_turn))
            }
            GameState.GameOver -> {
                view.announceForAccessibility(resources.getString(R.string.a11y_game_over_level, uiState.level))
            }
            GameState.Paused -> {
                view.announceForAccessibility(resources.getString(R.string.a11y_paused))
            }
            else -> { /* No announcement for other states */ }
        }
    }

    // Announce new high score celebration
    LaunchedEffect(uiState.showHighScoreText) {
        if (uiState.showHighScoreText) {
            view.announceForAccessibility(resources.getString(R.string.a11y_high_score_celebrate, uiState.level))
        }
    }

    // F8 polish: one-shot snackbar when a new achievement unlocks during a
    // session. Channel-backed flow so re-collecting on rotation doesn't
    // re-fire stale toasts; multi-unlock game-overs (e.g. FirstWin + Level10
    // on the very first deep run) collapse into one "N achievements
    // unlocked" message rather than queuing N snackbars.
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
            // Dismiss any in-flight snackbar so back-to-back unlocks don't
            // queue up; the most recent badge wins the screen.
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    // Audio-Only Mode (F3): swallow currentlyLit during the watch phase so
    // the panels stay dark while the sound plays. Player presses still light
    // up — that branch goes through `currentlyLit` while gameState is
    // PlayerRepeating, which this guard does not touch. The all-buttons
    // game-over flash is also unaffected.
    val displayLit: SimonButton? = if (
        uiState.audioOnlyModeEnabled && uiState.gameState == GameState.ShowingSequence
    ) null else uiState.currentlyLit

    // Function to handle both press and release events. Memoized so SimonPanel
    // children don't get a fresh lambda — and a fresh recomposition — on every
    // parent update.
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
                    // Enabled only while the player is repeating; dimmed otherwise.
                    val canPause = uiState.gameState == GameState.PlayerRepeating
                    IconButton(
                        onClick = onPauseGame,
                        enabled = canPause
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.pause_24px),
                            contentDescription = stringResource(R.string.action_pause),
                            tint = if (canPause) Color.White else Color.White.copy(alpha = 0.35f),
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
                            tint = Color.White,
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
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Settings button
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.action_settings),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        // Game content with minimal padding to maximize button size
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 0.dp,
                    start = 0.dp,
                    end = 0.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            // Simon Says Game UI - Using more vertical space. The width cap
            // keeps the panels from stretching uncomfortably wide on tablets
            // and large foldables (the disc + arc geometry was tuned for
            // phone proportions).
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.95f)
                    .fillMaxWidth(0.9f)
                    .widthIn(max = 500.dp)
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )

                // Simon Says colored panels — layout/orientation handling is
                // owned by `SimonButtonGrid` so this screen stays focused on
                // the overlays, the center disc, and game-state plumbing (#17).
                SimonButtonGrid(
                    memoryLightsPlusEnabled = uiState.memoryLightsPlusEnabled,
                    litButton = displayLit,
                    allButtonsLit = uiState.allButtonsLit,
                    pressedButtons = localPressedButtons,
                    colorBlindModeEnabled = uiState.colorBlindModeEnabled,
                    onButtonInteraction = handleButtonInteraction
                )

                // Center counter/button. Tappable in idle states (GameOver,
                // WaitingToStart) to start / restart a game; passive otherwise.
                val isIdleTappable = uiState.gameState == GameState.GameOver ||
                        uiState.gameState == GameState.WaitingToStart
                // Subtle scale pulse when the disc is tappable but the player
                // hasn't started yet — telegraphs the affordance.
                val pulseTransition = rememberInfiniteTransition(label = "ctaPulse")
                val ctaScale by pulseTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.06f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "ctaScalePulse"
                )
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .graphicsLayer(
                            scaleX = if (isIdleTappable) ctaScale else 1f,
                            scaleY = if (isIdleTappable) ctaScale else 1f
                        )
                        .then(
                            if (isIdleTappable) {
                                Modifier
                                    .shadow(
                                        elevation = 6.dp,
                                        shape = CircleShape,
                                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                    .clip(CircleShape)
                                    // Center disc stays near-black on both AMOLED and DARK so
                                    // the colored panels keep their contrast pop regardless of theme.
                                    .background(Color.Black)
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
                                    .background(Color.Black, RoundedCornerShape(60.dp))
                                    .zIndex(3f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Colored arcs - Dynamic based on mode
                    CenterDiscCanvas(
                        memoryLightsPlusEnabled = uiState.memoryLightsPlusEnabled,
                        colorBlindModeEnabled = uiState.colorBlindModeEnabled
                    )

                    // Sound pack chip + level / loading / game-over summary.
                    // Extracted (Wave 9) so this screen owns layout scaffolding
                    // and overlays only.
                    CenterDiscContent(
                        gameState = uiState.gameState,
                        gameMode = uiState.gameMode,
                        level = uiState.level,
                        currentHighScore = uiState.currentHighScore,
                        blitzElapsedMs = uiState.blitzElapsedMs,
                        currentBestBlitzTimeMs = uiState.currentBestBlitzTimeMs,
                        soundLoadError = uiState.soundLoadError,
                        soundsLoaded = uiState.soundsLoaded,
                        soundPackDisplayNameRes = uiState.currentSoundPack.displayNameRes,
                        sequenceSize = uiState.sequence.size,
                        playerSequenceSize = uiState.playerSequence.size
                    )
                }
            }
            
            // Inactivity countdown ring around the center disc — drains over
            // uiState.playerTimeoutMs during the player's turn and resets on
            // each press. Color shifts from green to red as time runs out so
            // the signal is readable peripherally. Re-keying on playerTimeoutMs
            // lets a mid-turn slider change re-launch the animation at the new
            // duration. Extracted to a dedicated component (Wave 4).
            if (uiState.gameState == GameState.PlayerRepeating) {
                TimeoutCountdownRing(
                    timeoutMs = uiState.playerTimeoutMs,
                    resetTick = uiState.timeoutResetTick
                )
            }

            // Center-anchored headline overlays. Each is a fade/scale text
            // popup above the game disc, consolidated into a single
            // `HeadlineOverlay` composable (#65). HIGH SCORE / GAME OVER
            // both swap copy + color when the run is a Speed Blitz win.
            val overlayAlign = if (uiState.memoryLightsPlusEnabled) OverlayBias6Button else OverlayBias4Button
            val overlayHeadlineColor = if (uiState.blitzWon) Color(0xFF66BB6A) else Color.Yellow

            // HIGH SCORE / BEST TIME — snappy 0.7 scale-in/out.
            HeadlineOverlay(
                visible = uiState.showHighScoreText,
                text = stringResource(
                    if (uiState.blitzWon) R.string.overlay_best_time
                    else R.string.overlay_high_score
                ),
                color = overlayHeadlineColor,
                style = MaterialTheme.typography.headlineLarge,
                alignment = overlayAlign,
                initialScale = 0.7f
            )

            // GAME OVER / VICTORY — slightly slower 0.85 scale matches the
            // weightier end-of-run beat.
            HeadlineOverlay(
                visible = uiState.showGameOverText,
                text = stringResource(
                    if (uiState.blitzWon) R.string.overlay_victory
                    else R.string.overlay_game_over
                ),
                color = overlayHeadlineColor,
                style = MaterialTheme.typography.headlineLarge,
                alignment = overlayAlign,
                initialScale = 0.85f
            )

            // SPEED UP! transient overlay when difficulty crosses a faster
            // tier (level 5, 9, 13, ... with difficulty enabled). Clears
            // itself via SPEED_UP_TEXT_DISPLAY_MS. Surfaces a change that
            // was previously invisible to the player (#62).
            HeadlineOverlay(
                visible = uiState.showSpeedUpText,
                text = stringResource(R.string.overlay_speed_up),
                color = Color(0xFFFF7043),
                style = MaterialTheme.typography.headlineMedium,
                alignment = overlayAlign,
                initialScale = 0.7f
            )

            // YOUR TURN — softer 0.9 scale-in, plain fadeOut (no scaleOut)
            // so the handoff to the gameplay halo doesn't pop. Uses a
            // different 4-button bias to clear the level number.
            HeadlineOverlay(
                visible = uiState.gameState == GameState.PlayerRepeating && uiState.showYourTurnText,
                text = stringResource(R.string.overlay_your_turn),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                alignment = if (uiState.memoryLightsPlusEnabled) OverlayBias6Button else OverlayBias4ButtonTurn,
                initialScale = 0.9f,
                useScaleOut = false
            )
            
            // Persistent player-turn cue: a pulsing halo around the center disc
            // for the entire PlayerRepeating phase. The "YOUR TURN" text only
            // shows for the first 3 rounds (#15 a11y) — after that, this halo
            // is the sole turn indicator, so it animates to stay legible
            // peripherally even at high levels (#60). Extracted to a
            // dedicated component (Wave 4).
            if (uiState.gameState == GameState.PlayerRepeating) {
                PlayerTurnHalo()
            }
            
            // Particle effect overlay for new high score celebration
            if (uiState.showHighScoreParticles) {
                ParticleEffect(
                    isActive = true,
                    modifier = Modifier.fillMaxSize(),
                    onComplete = { onClearParticleEffects?.invoke() }
                )
            }

            // Replay-last-sequence + share affordances, visible only on GameOver.
            // The disc itself starts a fresh run; Replay lets the player retry
            // the same sequence (highest-leverage retention loop), Share fires
            // an Android share sheet with a 1080x1080 score-card PNG (F10).
            // Extracted to a dedicated component (Wave 4) — the share-helper
            // wiring stays here so the component doesn't need to know about
            // context, level, high-score, or 6-button mode.
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
                }
            )

            // Paused overlay (F14). Tap anywhere on the dim backdrop to resume.
            // Z-ordered above all game content so the player can't accidentally
            // hit a colored panel while paused. Extracted to a dedicated
            // component (Wave 4) — same dim color, icon, copy, and z-order.
            PausedOverlay(
                visible = uiState.gameState == GameState.Paused,
                onResume = onResumeGame
            )

            // Speed Blitz HUD (F4) — sits at the top of the play area in any
            // active blitz state, showing progress to BLITZ_TARGET_LEVEL and
            // the live elapsed timer. Hidden in Classic mode and on the
            // GameOver screen (the disc shows the final time there).
            if (uiState.gameMode == GameMode.SPEED_BLITZ &&
                (uiState.gameState == GameState.ShowingSequence ||
                        uiState.gameState == GameState.PlayerRepeating ||
                        uiState.gameState == GameState.Paused)
            ) {
                BlitzHud(
                    level = uiState.level,
                    targetLevel = GameConstants.BLITZ_TARGET_LEVEL,
                    bestTimeMs = uiState.currentBestBlitzTimeMs,
                    startTimeMs = uiState.blitzStartTimeMs,
                    isPaused = uiState.gameState == GameState.Paused,
                    pauseStartedAtMs = uiState.blitzPauseStartedAtMs,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .zIndex(4f)
                )
            }
        }
    }
}