package com.happypuppy.memorylights.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ripple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import android.content.res.Configuration
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
import com.happypuppy.memorylights.domain.model.GameState
import com.happypuppy.memorylights.domain.model.SimonGameUiState
import com.happypuppy.memorylights.ui.components.ParticleEffect
import com.happypuppy.memorylights.ui.components.SimonPanel
import com.happypuppy.memorylights.ui.navigation.GameModesRoute
import com.happypuppy.memorylights.ui.navigation.GameRoute
import com.happypuppy.memorylights.ui.navigation.GameplayRoute
import com.happypuppy.memorylights.ui.navigation.SettingsRoute
import com.happypuppy.memorylights.ui.navigation.SoundAndHapticsRoute
import com.happypuppy.memorylights.ui.navigation.StatisticsRoute
import com.happypuppy.memorylights.ui.theme.CardBackground
import com.happypuppy.memorylights.ui.viewmodels.SimonGameViewModel

// Bias-based alignment for the four center-anchored text overlays
// (HIGH SCORE, GAME OVER, SPEED UP, YOUR TURN). Each overlay sits above
// the center disc; the bias is roughly proportional to screen height so
// the text floats consistently across phone sizes without a magic dp
// offset (#31).
private val OverlayBias6Button = BiasAlignment(0f, -0.18f)
private val OverlayBias4Button = BiasAlignment(0f, -0.07f)
private val OverlayBias4ButtonTurn = BiasAlignment(0f, -0.13f)

/**
 * Format Speed Blitz elapsed time as `M:SS.s` — minutes, seconds, tenths.
 * Tenths help differentiate near-tied runs without the noise of full
 * milliseconds. Negative or zero inputs render as `0:00.0` so a stale
 * state never shows an unexpected `-` or rolls weirdly past midnight.
 */
private fun formatBlitzTime(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val minutes = safe / 60_000L
    val seconds = (safe % 60_000L) / 1_000L
    val tenths = (safe % 1_000L) / 100L
    return "%d:%02d.%d".format(minutes, seconds, tenths)
}

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
                    gameStateAtSettingsEntry = uiState.gameState
                    viewModel.showSettings()
                    navController.navigate(SettingsRoute)
                },
                onStartNewGame = { viewModel.startNewGame() },
                onReplayLastSequence = { viewModel.replayLastSequence() },
                onToggleSound = { viewModel.toggleSound() },
                onToggleVibration = { viewModel.toggleVibration() },
                onPauseGame = { viewModel.pauseGame() },
                onResumeGame = { viewModel.resumeGame() },
                onClearParticleEffects = { viewModel.clearParticleEffects() }
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                currentSoundPack = uiState.currentSoundPack,
                onGameModesClick = { navController.navigate(GameModesRoute) },
                onGameplayClick = { navController.navigate(GameplayRoute) },
                onSoundAndHapticsClick = { navController.navigate(SoundAndHapticsRoute) },
                onResetHighScore = { viewModel.resetHighScore() },
                onStatisticsClicked = {
                    viewModel.showStatistics()
                    navController.navigate(StatisticsRoute)
                },
                onBackPressed = { navController.popBackStack() }
            )
        }
        composable<GameModesRoute> {
            GameModesScreen(
                difficultyEnabled = uiState.difficultyEnabled,
                reverseModeEnabled = uiState.reverseModeEnabled,
                practiceModeEnabled = uiState.practiceModeEnabled,
                audioOnlyModeEnabled = uiState.audioOnlyModeEnabled,
                memoryLightsPlusEnabled = uiState.memoryLightsPlusEnabled,
                gameMode = uiState.gameMode,
                hasActiveGame = gameStateAtSettingsEntry is GameState.ShowingSequence ||
                        gameStateAtSettingsEntry is GameState.PlayerRepeating ||
                        gameStateAtSettingsEntry is GameState.Paused,
                onDifficultyToggled = { viewModel.setDifficultyEnabled(it) },
                onReverseModeToggled = { viewModel.setReverseModeEnabled(it) },
                onPracticeModeToggled = { viewModel.setPracticeModeEnabled(it) },
                onAudioOnlyModeToggled = { viewModel.setAudioOnlyModeEnabled(it) },
                onMemoryLightsPlusToggled = { viewModel.setMemoryLightsPlusEnabled(it) },
                onGameModeSelected = { viewModel.setGameMode(it) },
                onBackPressed = { navController.popBackStack() }
            )
        }
        composable<GameplayRoute> {
            GameplayScreen(
                playerTimeoutSeconds = uiState.playerTimeoutSeconds,
                onPlayerTimeoutChanged = { viewModel.setPlayerTimeoutSeconds(it) },
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
    onClearParticleEffects: (() -> Unit)? = null
) {
    // Single source of truth for which buttons are physically pressed (for UI feedback only)
    var localPressedButtons by remember { mutableStateOf(mapOf<SimonButton, Boolean>()) }

    // Screen reader announcements for accessibility
    val view = LocalView.current

    // Derived state for available buttons - only recomputes when memoryLightsPlusEnabled changes
    val availableButtons by remember(uiState.memoryLightsPlusEnabled) {
        derivedStateOf { SimonButton.getAvailableButtons(uiState.memoryLightsPlusEnabled) }
    }

    // Announce level changes (skip initial level 1 on game start)
    var previousLevel by remember { mutableIntStateOf(0) }
    LaunchedEffect(uiState.level) {
        if (previousLevel > 0 && uiState.level > previousLevel) {
            view.announceForAccessibility("Level ${uiState.level}")
        }
        previousLevel = uiState.level
    }

    // Announce game state changes
    LaunchedEffect(uiState.gameState) {
        when (uiState.gameState) {
            GameState.PlayerRepeating -> {
                view.announceForAccessibility("Your turn. Repeat the sequence.")
            }
            GameState.GameOver -> {
                view.announceForAccessibility("Game over. You reached level ${uiState.level}.")
            }
            GameState.Paused -> {
                view.announceForAccessibility("Game paused.")
            }
            else -> { /* No announcement for other states */ }
        }
    }

    // Announce new high score celebration
    LaunchedEffect(uiState.showHighScoreText) {
        if (uiState.showHighScoreText) {
            view.announceForAccessibility("New high score! Level ${uiState.level}.")
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
                title = { Text("Memory Lights") },
                actions = {
                    // Mute/Unmute button
                    IconButton(onClick = onToggleSound) {
                        Icon(
                            painter = painterResource(
                                id = if (uiState.soundEnabled) 
                                    R.drawable.volume_up_24px 
                                else 
                                    R.drawable.volume_off_24px
                            ),
                            contentDescription = if (uiState.soundEnabled) "Mute" else "Unmute",
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
                            contentDescription = if (uiState.vibrateEnabled) "Disable Vibration" else "Enable Vibration",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Pause button — only meaningful while the player is repeating.
                    // Hidden in every other state so the action bar doesn't lie
                    // about what's available.
                    if (uiState.gameState == GameState.PlayerRepeating) {
                        IconButton(onClick = onPauseGame) {
                            Icon(
                                painter = painterResource(R.drawable.pause_24px),
                                contentDescription = "Pause",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Settings button
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
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

                // Simon Says colored panels - Dynamic layout based on mode and orientation.
                // remember keyed on the orientation int so the boolean is stable
                // across unrelated Configuration changes (font scale, locale,
                // density). Full panel extraction tracked separately as #17.
                val orientation = LocalConfiguration.current.orientation
                val isLandscape = remember(orientation) {
                    orientation == Configuration.ORIENTATION_LANDSCAPE
                }
                
                if (uiState.memoryLightsPlusEnabled) {
                    // 6-button mode: Layout changes based on orientation
                    if (isLandscape) {
                        // Landscape: 2x3 grid layout (2 rows, 3 columns)
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Top row: Green, Red, Yellow
                            Row(modifier = Modifier.weight(1f)) {
                                listOf(SimonButton.GREEN, SimonButton.RED, SimonButton.YELLOW).forEach { button ->
                                    SimonPanel(
                                        color = button.color,
                                        colorName = button.displayName,
                                        isLit = displayLit == button || uiState.allButtonsLit,
                                        userPressed = localPressedButtons[button] == true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(2.dp),
                                        onTouchStateChanged = { isPressed ->
                                            handleButtonInteraction(button, isPressed)
                                        }
                                    )
                                }
                            }

                            // Bottom row: Blue, Purple, Orange
                            Row(modifier = Modifier.weight(1f)) {
                                listOf(SimonButton.BLUE, SimonButton.PURPLE, SimonButton.ORANGE).forEach { button ->
                                    SimonPanel(
                                        color = button.color,
                                        colorName = button.displayName,
                                        isLit = displayLit == button || uiState.allButtonsLit,
                                        userPressed = localPressedButtons[button] == true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(2.dp),
                                        onTouchStateChanged = { isPressed ->
                                            handleButtonInteraction(button, isPressed)
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // Portrait: 3x2 grid layout (3 rows, 2 columns) - original layout
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Top row: Green, Red
                            Row(modifier = Modifier.weight(1f)) {
                                listOf(SimonButton.GREEN, SimonButton.RED).forEach { button ->
                                    SimonPanel(
                                        color = button.color,
                                        colorName = button.displayName,
                                        isLit = displayLit == button || uiState.allButtonsLit,
                                        userPressed = localPressedButtons[button] == true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(2.dp),
                                        onTouchStateChanged = { isPressed ->
                                            handleButtonInteraction(button, isPressed)
                                        }
                                    )
                                }
                            }

                            // Middle row: Yellow, Blue
                            Row(modifier = Modifier.weight(1f)) {
                                listOf(SimonButton.YELLOW, SimonButton.BLUE).forEach { button ->
                                    SimonPanel(
                                        color = button.color,
                                        colorName = button.displayName,
                                        isLit = displayLit == button || uiState.allButtonsLit,
                                        userPressed = localPressedButtons[button] == true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(2.dp),
                                        onTouchStateChanged = { isPressed ->
                                            handleButtonInteraction(button, isPressed)
                                        }
                                    )
                                }
                            }

                            // Bottom row: Purple, Orange
                            Row(modifier = Modifier.weight(1f)) {
                                listOf(SimonButton.PURPLE, SimonButton.ORANGE).forEach { button ->
                                    SimonPanel(
                                        color = button.color,
                                        colorName = button.displayName,
                                        isLit = displayLit == button || uiState.allButtonsLit,
                                        userPressed = localPressedButtons[button] == true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(2.dp),
                                        onTouchStateChanged = { isPressed ->
                                            handleButtonInteraction(button, isPressed)
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // 4-button mode: 2x2 grid layout (classic mode)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(modifier = Modifier.weight(1f)) {
                            // Green panel (top-left)
                            SimonPanel(
                                color = SimonButton.GREEN.color,
                                colorName = "Green",
                                isLit = displayLit == SimonButton.GREEN || uiState.allButtonsLit,
                                userPressed = localPressedButtons[SimonButton.GREEN] == true,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(4.dp),
                                onTouchStateChanged = { isPressed ->
                                    handleButtonInteraction(SimonButton.GREEN, isPressed)
                                }
                            )

                            // Red panel (top-right)
                            SimonPanel(
                                color = SimonButton.RED.color,
                                colorName = "Red",
                                isLit = displayLit == SimonButton.RED || uiState.allButtonsLit,
                                userPressed = localPressedButtons[SimonButton.RED] == true,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(4.dp),
                                onTouchStateChanged = { isPressed ->
                                    handleButtonInteraction(SimonButton.RED, isPressed)
                                }
                            )
                        }

                        Row(modifier = Modifier.weight(1f)) {
                            // Yellow panel (bottom-left)
                            SimonPanel(
                                color = SimonButton.YELLOW.color,
                                colorName = "Yellow",
                                isLit = displayLit == SimonButton.YELLOW || uiState.allButtonsLit,
                                userPressed = localPressedButtons[SimonButton.YELLOW] == true,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(4.dp),
                                onTouchStateChanged = { isPressed ->
                                    handleButtonInteraction(SimonButton.YELLOW, isPressed)
                                }
                            )

                            // Blue panel (bottom-right)
                            SimonPanel(
                                color = SimonButton.BLUE.color,
                                colorName = "Blue",
                                isLit = displayLit == SimonButton.BLUE || uiState.allButtonsLit,
                                userPressed = localPressedButtons[SimonButton.BLUE] == true,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(4.dp),
                                onTouchStateChanged = { isPressed ->
                                    handleButtonInteraction(SimonButton.BLUE, isPressed)
                                }
                            )
                        }
                    }
                }

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
                                    .background(CardBackground)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(),
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
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 8.dp.toPx()
                        val sweepAngle = 360f / availableButtons.size
                        
                        if (uiState.memoryLightsPlusEnabled) {
                            // 6-button mode: 6 arcs (60 degrees each)
                            val buttonOrder = listOf(
                                SimonButton.RED,    // 0-60 degrees (top-right)
                                SimonButton.BLUE,   // 60-120 degrees (bottom-right)
                                SimonButton.ORANGE, // 120-180 degrees (bottom)
                                SimonButton.YELLOW, // 180-240 degrees (bottom-left)
                                SimonButton.PURPLE, // 240-300 degrees (top-left)
                                SimonButton.GREEN   // 300-360 degrees (top)
                            )
                            
                            buttonOrder.forEachIndexed { index, button ->
                                drawArc(
                                    color = button.color,
                                    startAngle = index * sweepAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                    size = size.copy(
                                        width = size.width - strokeWidth,
                                        height = size.height - strokeWidth
                                    ),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = strokeWidth,
                                        cap = StrokeCap.Butt
                                    )
                                )
                            }
                        } else {
                            // 4-button mode: Classic 4 arcs (90 degrees each)
                            
                            // Draw green arc (top-left)
                            drawArc(
                                color = SimonButton.GREEN.color,
                                startAngle = 180f,
                                sweepAngle = 90f,
                                useCenter = false,
                                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                size = size.copy(
                                    width = size.width - strokeWidth,
                                    height = size.height - strokeWidth
                                ),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Butt
                                )
                            )

                            // Draw red arc (top-right)
                            drawArc(
                                color = SimonButton.RED.color,
                                startAngle = 270f,
                                sweepAngle = 90f,
                                useCenter = false,
                                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                size = size.copy(
                                    width = size.width - strokeWidth,
                                    height = size.height - strokeWidth
                                ),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Butt
                                )
                            )

                            // Draw blue arc (bottom-right)
                            drawArc(
                                color = SimonButton.BLUE.color,
                                startAngle = 0f,
                                sweepAngle = 90f,
                                useCenter = false,
                                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                size = size.copy(
                                    width = size.width - strokeWidth,
                                    height = size.height - strokeWidth
                                ),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Butt
                                )
                            )

                            // Draw yellow arc (bottom-left)
                            drawArc(
                                color = SimonButton.YELLOW.color,
                                startAngle = 90f,
                                sweepAngle = 90f,
                                useCenter = false,
                                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                size = size.copy(
                                    width = size.width - strokeWidth,
                                    height = size.height - strokeWidth
                                ),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Butt
                                )
                            )
                        }
                    }

                    // Create a centered inner content Box
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Sound pack indicator - only show when NOT in GameOver state
                        if (uiState.gameState != GameState.GameOver) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 10.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.music_note_24px),
                                    contentDescription = "Sound Pack",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = uiState.currentSoundPack.displayName,
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Center content (level number, play button, or loading indicator)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (uiState.soundLoadError != null) {
                                // Surface the load error so the player isn't left
                                // staring at a silent indefinite spinner.
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Sound load failed",
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Sound load failed",
                                    color = Color(0xFFFFB300),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Game playable without audio",
                                    color = Color.Gray,
                                    fontSize = 9.sp
                                )
                            } else if (!uiState.soundsLoaded) {
                                // Show loading indicator while sounds are loading
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Loading...",
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            } else if (uiState.gameState == GameState.GameOver) {
                                // Score summary so the game-over moment lingers
                                // long enough for the player to register it.
                                // In Speed Blitz the headline is the elapsed
                                // time; level is implicit (always 20 on win,
                                // wherever the player died on loss).
                                if (uiState.gameMode == GameMode.SPEED_BLITZ &&
                                    uiState.blitzElapsedMs > 0L
                                ) {
                                    Text(
                                        text = formatBlitzTime(uiState.blitzElapsedMs),
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (uiState.currentBestBlitzTimeMs > 0L) {
                                        Text(
                                            text = "Best ${formatBlitzTime(uiState.currentBestBlitzTimeMs)}",
                                            color = Color(0xFFFFC107),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                } else {
                                    Text(
                                        text = uiState.level.toString(),
                                        color = Color.White,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (uiState.currentHighScore > 0) {
                                        Text(
                                            text = "Best ${uiState.currentHighScore}",
                                            color = Color(0xFFFFC107),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Icon(
                                    painter = painterResource(R.drawable.play_arrow_24px),
                                    contentDescription = "Play Again",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else if (uiState.gameState == GameState.WaitingToStart) {
                                // Tap-to-start affordance — play icon + label invite
                                // the player to begin instead of staring at a "1".
                                Text(
                                    text = uiState.level.toString(),
                                    color = Color.White,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    painter = painterResource(R.drawable.play_arrow_24px),
                                    contentDescription = "Tap to start",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Tap to start",
                                    color = Color.Gray,
                                    fontSize = 9.sp
                                )
                            } else {
                                // Animated level display with smooth transitions
                                AnimatedContent(
                                    targetState = uiState.level,
                                    transitionSpec = {
                                        slideInVertically(
                                            initialOffsetY = { height -> height }
                                        ) togetherWith slideOutVertically(
                                            targetOffsetY = { height -> -height }
                                        )
                                    },
                                    label = "level_animation"
                                ) { level ->
                                    Text(
                                        text = level.toString(),
                                        color = Color.White,
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                // Sequence-progress indicator under the level number
                                // during the player's turn. Players lose count of
                                // long sequences without a visible step counter (#64).
                                if (uiState.gameState == GameState.PlayerRepeating &&
                                    uiState.sequence.isNotEmpty()
                                ) {
                                    Text(
                                        text = "${uiState.playerSequence.size} / ${uiState.sequence.size}",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // High score display at bottom - only show when NOT in GameOver state
                        if (uiState.currentHighScore > 0 && uiState.gameState != GameState.GameOver) {
                            Text(
                                text = "High: ${uiState.currentHighScore}",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 10.dp)
                            )
                        }
                    }
                }
            }
            
            // Inactivity countdown ring around the center disc — drains over
            // uiState.playerTimeoutMs during the player's turn and resets on
            // each press. Color shifts from green → red as time runs out so
            // the signal is readable peripherally. Re-keying on
            // playerTimeoutMs lets a mid-turn slider change re-launch the
            // animation at the new duration.
            if (uiState.gameState == GameState.PlayerRepeating) {
                val timeoutFraction = remember { Animatable(1f) }
                LaunchedEffect(uiState.timeoutResetTick, uiState.playerTimeoutMs) {
                    timeoutFraction.snapTo(1f)
                    timeoutFraction.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = uiState.playerTimeoutMs.toInt(),
                            easing = LinearEasing
                        )
                    )
                }
                val ringColor = lerp(
                    Color(0xFFE53935), // red — almost out of time
                    Color(0xFF4CAF50), // green — fresh
                    timeoutFraction.value
                )
                Canvas(
                    modifier = Modifier
                        .size(124.dp)
                        .zIndex(2f)
                ) {
                    val stroke = 4.dp.toPx()
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * timeoutFraction.value,
                        useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(
                            width = size.width - stroke,
                            height = size.height - stroke
                        ),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }

            // Headline overlay text — switches between HIGH SCORE / VICTORY /
            // GAME OVER depending on whether the run was a Speed Blitz win
            // and whether it set a new best.
            AnimatedVisibility(
                visible = uiState.showHighScoreText,
                enter = fadeIn() + scaleIn(initialScale = 0.7f),
                exit = fadeOut() + scaleOut(targetScale = 0.7f),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.blitzWon) "BEST TIME!" else "HIGH SCORE!",
                        color = if (uiState.blitzWon) Color(0xFF66BB6A) else Color.Yellow,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(if (uiState.memoryLightsPlusEnabled) OverlayBias6Button else OverlayBias4Button)
                            .shadow(4.dp)
                    )
                }
            }

            // GAME OVER / VICTORY (blitz win without a new best) overlay.
            AnimatedVisibility(
                visible = uiState.showGameOverText,
                enter = fadeIn() + scaleIn(initialScale = 0.85f),
                exit = fadeOut() + scaleOut(targetScale = 0.85f),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.blitzWon) "VICTORY!" else "GAME OVER",
                        color = if (uiState.blitzWon) Color(0xFF66BB6A) else Color.Yellow,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(if (uiState.memoryLightsPlusEnabled) OverlayBias6Button else OverlayBias4Button)
                            .shadow(4.dp)
                    )
                }
            }

            // SPEED UP! transient overlay when difficulty crosses a faster tier
            // (level 5, 9, 13, ... with difficulty enabled). Clears itself via
            // SPEED_UP_TEXT_DISPLAY_MS. Surfaces a change that was previously
            // invisible to the player (#62).
            AnimatedVisibility(
                visible = uiState.showSpeedUpText,
                enter = fadeIn() + scaleIn(initialScale = 0.7f),
                exit = fadeOut() + scaleOut(targetScale = 0.7f),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SPEED UP!",
                        color = Color(0xFFFF7043),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(if (uiState.memoryLightsPlusEnabled) OverlayBias6Button else OverlayBias4Button)
                            .shadow(4.dp)
                    )
                }
            }

            // YOUR TURN text overlay when it's player's turn to repeat the sequence
            AnimatedVisibility(
                visible = uiState.gameState == GameState.PlayerRepeating && uiState.showYourTurnText,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "YOUR TURN",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(if (uiState.memoryLightsPlusEnabled) OverlayBias6Button else OverlayBias4ButtonTurn)
                            .shadow(4.dp)
                    )
                }
            }
            
            // Persistent player-turn cue: a pulsing halo around the center disc
            // for the entire PlayerRepeating phase. The "YOUR TURN" text only
            // shows for the first 3 rounds (#15 a11y) — after that, this halo
            // is the sole turn indicator, so it animates to stay legible
            // peripherally even at high levels (#60).
            if (uiState.gameState == GameState.PlayerRepeating) {
                val turnTransition = rememberInfiniteTransition(label = "turnPulse")
                val turnAlpha by turnTransition.animateFloat(
                    initialValue = 0.06f,
                    targetValue = 0.18f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "turnAlpha"
                )
                val turnScale by turnTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "turnScale"
                )
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .graphicsLayer(scaleX = turnScale, scaleY = turnScale)
                        .background(
                            Color.White.copy(alpha = turnAlpha),
                            RoundedCornerShape(65.dp)
                        )
                )
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
            AnimatedVisibility(
                visible = uiState.gameState == GameState.GameOver &&
                        uiState.sequence.isNotEmpty(),
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                val context = LocalContext.current
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(
                        modifier = Modifier.padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onReplayLastSequence,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Replay", fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                ScoreShareHelper.shareScore(
                                    context = context,
                                    level = uiState.level,
                                    bestScore = uiState.currentHighScore,
                                    is6ButtonMode = uiState.memoryLightsPlusEnabled
                                )
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share", fontSize = 13.sp)
                        }
                    }
                }
            }

            // Paused overlay (F14). Tap anywhere on the dim backdrop to resume.
            // Z-ordered above all game content so the player can't accidentally
            // hit a colored panel while paused.
            AnimatedVisibility(
                visible = uiState.gameState == GameState.Paused,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onResumeGame
                        )
                        .semantics {
                            contentDescription = "Game paused. Tap to resume."
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play_arrow_24px),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(96.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "PAUSED",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap to resume",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }
            }

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
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .zIndex(4f)
                )
            }
        }
    }
}

/**
 * Top-of-screen HUD shown only in Speed Blitz: progress (X / target) and a
 * live elapsed-time readout that ticks at 10 Hz so the tenths digit moves
 * visibly. The clock is read from `System.currentTimeMillis()` rather than
 * tracked in UiState to keep the ViewModel from emitting at 10 Hz.
 */
@Composable
private fun BlitzHud(
    level: Int,
    targetLevel: Int,
    bestTimeMs: Long,
    startTimeMs: Long,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startTimeMs, isPaused) {
        if (startTimeMs <= 0L || isPaused) return@LaunchedEffect
        while (true) {
            nowMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(100L)
        }
    }
    val elapsed = if (startTimeMs > 0L) (nowMs - startTimeMs).coerceAtLeast(0L) else 0L

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.55f),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = "Lvl $level / $targetLevel",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "•",
            color = Color.Gray,
            fontSize = 13.sp
        )
        Text(
            text = formatBlitzTime(elapsed),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        if (bestTimeMs > 0L) {
            Text(
                text = "•",
                color = Color.Gray,
                fontSize = 13.sp
            )
            Text(
                text = "Best ${formatBlitzTime(bestTimeMs)}",
                color = Color(0xFFFFC107),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}