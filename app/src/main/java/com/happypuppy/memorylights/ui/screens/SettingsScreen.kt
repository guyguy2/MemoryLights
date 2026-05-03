// Updated SettingsScreen.kt file
package com.happypuppy.memorylights.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happypuppy.memorylights.R
import com.happypuppy.memorylights.domain.enums.SoundPack
import kotlinx.coroutines.launch
import androidx.core.net.toUri

/**
 * Settings screen for the Memory Lights game
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentSoundPack: SoundPack,
    difficultyEnabled: Boolean = false,
    memoryLightsPlusEnabled: Boolean = false,
    highScore: Int = 0,
    hasActiveGame: Boolean = false,
    onSoundPackSelected: (SoundPack) -> Unit,
    onDifficultyToggled: (Boolean) -> Unit = {},
    onMemoryLightsPlusToggled: (Boolean) -> Unit = {},
    onResetHighScore: () -> Unit = {},
    onStatisticsClicked: () -> Unit = {},
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current

    // Dialog visibility states
    var showAboutDialog by remember { mutableStateOf(false) }
    var showResetHighScoreDialog by remember { mutableStateOf(false) }
    var pendingMemoryLightsPlusToggle by remember { mutableStateOf<Boolean?>(null) }

    // LazyListState for the sound pack list
    val soundPackListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Check if the list can scroll down
    val canScrollDown by remember {
        derivedStateOf {
            soundPackListState.canScrollForward
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // SOUND PACKS SECTION
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.music_note_24px),
                    contentDescription = "Sound Packs",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Sound Packs",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Sound pack selection with scroll indicators
            Box(
                modifier = Modifier
                    .height(280.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF121212))
            ) {
                // Main list of sound packs
                LazyColumn(
                    state = soundPackListState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(SoundPack.entries.toList()) { soundPack ->
                        SoundPackOption(
                            soundPack = soundPack,
                            isSelected = soundPack == currentSoundPack,
                            onSelect = { onSoundPackSelected(soundPack) }
                        )
                    }

                    // Add empty item to ensure last item isn't hidden behind the scroll indicator
                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }

                // Scroll indicator at the bottom
                if (canScrollDown) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xE6121212)
                                    )
                                )
                            )
                            .clickable {
                                coroutineScope.launch {
                                    // Scroll down when clicked
                                    soundPackListState.animateScrollBy(100f)
                                }
                            }
                            .padding(vertical = 8.dp)
                            .align(Alignment.BottomCenter),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "More options",
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "More sound options",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Fade overlay at the top when scrolled
                val showTopFade by remember {
                    derivedStateOf {
                        soundPackListState.firstVisibleItemIndex > 0 ||
                                soundPackListState.firstVisibleItemScrollOffset > 0
                    }
                }

                if (showTopFade) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xE6121212),
                                        Color.Transparent
                                    )
                                )
                            )
                            .align(Alignment.TopCenter)
                    )
                }
            }

            
            // Difficulty setting card
            SettingsCard(
                onClick = { onDifficultyToggled(!difficultyEnabled) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.speed_24px),
                        contentDescription = "Difficulty",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Difficulty",
                            color = Color.White,
                            fontSize = 16.sp
                        )

                        Text(
                            text = "Increase speed as levels progress",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }

                    Switch(
                        checked = difficultyEnabled,
                        onCheckedChange = onDifficultyToggled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }
            }
            
            // Memory Lights+ setting card
            val requestMemoryLightsPlusToggle: (Boolean) -> Unit = { newValue ->
                if (hasActiveGame) {
                    pendingMemoryLightsPlusToggle = newValue
                } else {
                    onMemoryLightsPlusToggled(newValue)
                }
            }
            SettingsCard(
                onClick = { requestMemoryLightsPlusToggle(!memoryLightsPlusEnabled) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play_arrow_24px),
                        contentDescription = "Memory Lights+",
                        tint = Color(0xFF4CAF50), // Green color for premium feature
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Memory Lights+",
                            color = Color.White,
                            fontSize = 16.sp
                        )

                        Text(
                            text = "6 buttons",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }

                    Switch(
                        checked = memoryLightsPlusEnabled,
                        onCheckedChange = requestMemoryLightsPlusToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }
            }

            // Statistics card
            SettingsCard(
                onClick = onStatisticsClicked
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Statistics",
                        tint = Color(0xFF4CAF50), // Green color
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Statistics",
                            color = Color.White,
                            fontSize = 16.sp
                        )

                        Text(
                            text = "View your game progress and stats",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            // Reset Score & Statistics card. Only the inner Reset button triggers
            // the destructive dialog so users can scroll/scan the card without
            // accidentally firing it.
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.counter_0_24px),
                        contentDescription = "Reset Score and Statistics",
                        tint = Color(0xFFFF9800), // Orange color
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reset Score & Statistics",
                            color = Color.White,
                            fontSize = 16.sp
                        )

                        Text(
                            text = "Clears high score, games played, and all statistics",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }

                    TextButton(
                        onClick = { showResetHighScoreDialog = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Reset")
                    }
                }
            }

            // RATE APP SECTION
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                thickness = 1.dp,
                color = Color(0xFF303030)
            )

            // Rate app card
            SettingsCard(
                onClick = { 
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data =
                            "https://play.google.com/store/apps/details?id=com.happypuppy.memorylights".toUri()
                        setPackage("com.android.vending")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        // Fallback to web browser if Play Store not available
                        val fallbackIntent = Intent(Intent.ACTION_VIEW,
                            "https://play.google.com/store/apps/details?id=com.happypuppy.memorylights".toUri())
                        context.startActivity(fallbackIntent)
                    }
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rate App",
                        tint = Color(0xFFFFC107), // Amber/gold color
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Rate this App",
                            color = Color.White,
                            fontSize = 16.sp
                        )

                        Text(
                            text = "Enjoying Memory Lights? Let us know!",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // About card
            SettingsCard(
                onClick = { showAboutDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "About",
                            color = Color.White,
                            fontSize = 16.sp
                        )

                        Text(
                            text = "Version 1.0.0 • © 2025",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Bottom spacing to ensure everything is visible
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // About dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text(
                    text = "About Memory Lights",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = "Memory Lights is a classic electronic memory game that challenges players to repeat sequences of lights and sounds.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Each round adds a new step to the sequence, testing your memory limits as you progress through increasingly difficult patterns.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Version: 1.0.0",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "© 2025 Memory Lights Game",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(
                        text = "Close",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            containerColor = Color(0xFF1A1A1A),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
    
    // Reset High Score confirmation dialog
    if (showResetHighScoreDialog) {
        AlertDialog(
            onDismissRequest = { showResetHighScoreDialog = false },
            title = {
                Text(
                    text = "Reset High Score",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to reset your high score and all statistics? This will reset games played, total score, and best streak. This action cannot be undone.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        onResetHighScore()
                        showResetHighScoreDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetHighScoreDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF1A1A1A),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    // Memory Lights+ mode-switch confirmation when toggled mid-game.
    pendingMemoryLightsPlusToggle?.let { newValue ->
        AlertDialog(
            onDismissRequest = { pendingMemoryLightsPlusToggle = null },
            title = {
                Text(
                    text = "End current game?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Switching modes will end your current run. Continue?",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onMemoryLightsPlusToggled(newValue)
                        pendingMemoryLightsPlusToggle = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Switch")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingMemoryLightsPlusToggle = null },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF1A1A1A),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

@Composable
fun SettingsCard(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1D1D1D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        content()
    }
}

@Composable
fun SoundPackOption(
    soundPack: SoundPack,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        Color(0xFF303030)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onSelect() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = soundPack.displayName,
                color = Color.White,
                fontSize = 16.sp
            )

            Text(
                text = soundPack.description,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}