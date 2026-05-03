package com.happypuppy.memorylights.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ripple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import android.content.res.Configuration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.happypuppy.memorylights.R
import com.happypuppy.memorylights.domain.enums.SimonButton
import com.happypuppy.memorylights.domain.model.GameState
import com.happypuppy.memorylights.domain.model.ScreenState
import com.happypuppy.memorylights.domain.model.SimonGameUiState
import com.happypuppy.memorylights.ui.components.ParticleEffect
import com.happypuppy.memorylights.ui.components.SimonPanel
import com.happypuppy.memorylights.ui.viewmodels.SimonGameViewModel

// Extension function to capitalize first letter
private fun String.capitalize(): String {
    return if (this.isNotEmpty()) {
        this.substring(0, 1).uppercase() + this.substring(1)
    } else {
        this
    }
}

@Composable
fun MemoryLightsGame(viewModel: SimonGameViewModel) {
    // Collect UI state from ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Display appropriate screen based on screen state
    when (uiState.screenState) {
        is ScreenState.Settings -> {
            // Show Settings Screen
            val previousState = uiState.gameState // captured before navigating into settings
            SettingsScreen(
                currentSoundPack = uiState.currentSoundPack,
                difficultyEnabled = uiState.difficultyEnabled,
                memoryLightsPlusEnabled = uiState.memoryLightsPlusEnabled,
                highScore = uiState.currentHighScore,
                hasActiveGame = previousState is GameState.ShowingSequence ||
                        previousState is GameState.PlayerRepeating,
                onSoundPackSelected = { viewModel.setSoundPack(it) },
                onDifficultyToggled = { viewModel.setDifficultyEnabled(it) },
                onMemoryLightsPlusToggled = { viewModel.setMemoryLightsPlusEnabled(it) },
                onResetHighScore = { viewModel.resetHighScore() },
                onStatisticsClicked = { viewModel.showStatistics() },
                onBackPressed = { viewModel.exitSettings() }
            )
        }
        is ScreenState.Statistics -> {
            // Show Statistics Screen
            StatisticsScreen(
                statistics = uiState.statistics,
                currentHighScore = uiState.currentHighScore,
                onResetStatistics = { viewModel.resetStatistics() },
                onBackPressed = { viewModel.exitStatistics() }
            )
        }
        else -> {
            // Show Main Game Screen with updated callback signature
            SimonGameScreen(
                uiState = uiState,
                onButtonClick = { button, isPress -> viewModel.onButtonClick(button, isPress) },
                onSettingsClick = { viewModel.showSettings() },
                onStartNewGame = { viewModel.startNewGame() },
                onToggleSound = { viewModel.toggleSound() },
                onToggleVibration = { viewModel.toggleVibration() },
                onClearParticleEffects = { viewModel.clearParticleEffects() }
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
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit,
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
            else -> { /* No announcement for other states */ }
        }
    }

    // Announce new high score celebration
    LaunchedEffect(uiState.showHighScoreText) {
        if (uiState.showHighScoreText) {
            view.announceForAccessibility("New high score! Level ${uiState.level}.")
        }
    }

    // Function to handle both press and release events
    val handleButtonInteraction = { button: SimonButton, isPress: Boolean ->
        if (isPress) {
            // Update local state for immediate visual feedback
            localPressedButtons = localPressedButtons + (button to true)
        } else {
            // Release this button
            localPressedButtons = localPressedButtons - button
        }

        // Pass all events to ViewModel with isPress parameter
        onButtonClick(button, isPress)
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
            // Simon Says Game UI - Using more vertical space
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.95f)
                    .fillMaxWidth(0.9f)
                    .padding(top = 8.dp), // Reduced top padding
                contentAlignment = Alignment.Center
            ) {
                // Background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )

                // Simon Says colored panels - Dynamic layout based on mode and orientation
                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                
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
                                        colorName = button.name.lowercase().capitalize(),
                                        isLit = uiState.currentlyLit == button || uiState.allButtonsLit,
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
                                        colorName = button.name.lowercase().capitalize(),
                                        isLit = uiState.currentlyLit == button || uiState.allButtonsLit,
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
                                        colorName = button.name.lowercase().capitalize(),
                                        isLit = uiState.currentlyLit == button || uiState.allButtonsLit,
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
                                        colorName = button.name.lowercase().capitalize(),
                                        isLit = uiState.currentlyLit == button || uiState.allButtonsLit,
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
                                        colorName = button.name.lowercase().capitalize(),
                                        isLit = uiState.currentlyLit == button || uiState.allButtonsLit,
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
                                isLit = uiState.currentlyLit == SimonButton.GREEN || uiState.allButtonsLit,
                                userPressed = localPressedButtons[SimonButton.GREEN] == true,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(2.dp),
                                onTouchStateChanged = { isPressed ->
                                    handleButtonInteraction(SimonButton.GREEN, isPressed)
                                }
                            )

                            // Red panel (top-right)
                            SimonPanel(
                                color = SimonButton.RED.color,
                                colorName = "Red",
                                isLit = uiState.currentlyLit == SimonButton.RED || uiState.allButtonsLit,
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
                                isLit = uiState.currentlyLit == SimonButton.YELLOW || uiState.allButtonsLit,
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
                                isLit = uiState.currentlyLit == SimonButton.BLUE || uiState.allButtonsLit,
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

                // Center counter/button with FAB-style when in GameOver state
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .then(
                            if (uiState.gameState == GameState.GameOver) {
                                // When in GameOver state, add elevation and shadow
                                Modifier
                                    .shadow(
                                        elevation = 6.dp,
                                        shape = CircleShape,
                                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                    .clip(CircleShape)
                                    .background(Color(0xFF1D1D1D))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(),
                                        onClick = { onStartNewGame() }
                                    )
                            } else {
                                // Normal state
                                Modifier
                                    .background(Color.Black, RoundedCornerShape(60.dp))
                                    .zIndex(3f)
                                    .clickable(
                                        enabled = (uiState.gameState == GameState.GameOver),
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (uiState.gameState == GameState.GameOver) {
                                            onStartNewGame()
                                        }
                                    }
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
                                    text = uiState.currentSoundPack.name.lowercase().capitalize(),
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
                                Icon(
                                    painter = painterResource(R.drawable.play_arrow_24px),
                                    contentDescription = "Play Again",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
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
            
            // HIGH SCORE text overlay for new high score celebration
            if (uiState.showHighScoreText) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "HIGH SCORE!",
                        color = Color.Yellow,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .offset(y = if (uiState.memoryLightsPlusEnabled) (-200).dp else (-80).dp) // Position on top buttons in 6-button mode
                            .shadow(4.dp)
                    )
                }
            }
            
            // GAME OVER text overlay when game ends without high score
            if (uiState.showGameOverText) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "GAME OVER",
                        color = Color.Yellow,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .offset(y = if (uiState.memoryLightsPlusEnabled) (-200).dp else (-80).dp) // Position on top buttons in 6-button mode
                            .shadow(4.dp)
                    )
                }
            }
            
            // YOUR TURN text overlay when it's player's turn to repeat the sequence
            if (uiState.gameState == GameState.PlayerRepeating && uiState.showYourTurnText) {
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
                            .offset(y = if (uiState.memoryLightsPlusEnabled) (-200).dp else (-140).dp) // Position on top buttons in 6-button mode  
                            .shadow(4.dp)
                    )
                }
            }
            
            // Subtle interactive indicator for later rounds when YOUR TURN text is not shown
            if (uiState.gameState == GameState.PlayerRepeating && !uiState.showYourTurnText) {
                // Add a subtle pulse glow around the center circle to indicate interactivity
                Box(
                    modifier = Modifier
                        .size(130.dp) // Slightly larger than center circle
                        .background(
                            Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(65.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(110.dp)) // Match center circle size
                }
            }
            
            // Particle effect overlay for new high score celebration
            if (uiState.showHighScoreParticles) {
                ParticleEffect(
                    isActive = true,
                    modifier = Modifier.fillMaxSize(),
                    onComplete = { onClearParticleEffects?.invoke() }
                )
            }
        }
    }
}