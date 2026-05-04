package com.happypuppy.memorylights.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happypuppy.memorylights.R
import com.happypuppy.memorylights.domain.enums.GameMode
import com.happypuppy.memorylights.ui.theme.SurfaceContainer
import kotlinx.coroutines.launch

/**
 * Game Modes settings sub-screen (F18). Hosts the four toggles that change
 * how a run plays: Difficulty (speed scaling), Reverse Mode (recall in
 * reverse), Practice Mode (wrong button replays sequence), and Memory
 * Lights+ (4-button → 6-button).
 *
 * Memory Lights+ retains the mid-game switch confirm-dialog because
 * flipping it ends the current run; the parent passes [hasActiveGame] so
 * the dialog only appears when there's something to lose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameModesScreen(
    difficultyEnabled: Boolean,
    reverseModeEnabled: Boolean,
    practiceModeEnabled: Boolean,
    audioOnlyModeEnabled: Boolean,
    memoryLightsPlusEnabled: Boolean,
    gameMode: GameMode,
    hasActiveGame: Boolean,
    onDifficultyToggled: (Boolean) -> Unit,
    onReverseModeToggled: (Boolean) -> Unit,
    onPracticeModeToggled: (Boolean) -> Unit,
    onAudioOnlyModeToggled: (Boolean) -> Unit,
    onMemoryLightsPlusToggled: (Boolean) -> Unit,
    onGameModeSelected: (GameMode) -> Unit,
    onBackPressed: () -> Unit
) {
    var pendingMemoryLightsPlusToggle by remember { mutableStateOf<Boolean?>(null) }
    var pendingGameModeChange by remember { mutableStateOf<GameMode?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    fun toast(message: String) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Modes") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Game mode picker — Classic vs Speed Blitz. Switching mid-game
            // ends the current run, so prompt before flipping if there's
            // something to lose. Mirrors the ML+ confirm-dialog flow.
            GameModeCard(
                selected = gameMode,
                onSelect = { newMode ->
                    if (newMode == gameMode) return@GameModeCard
                    if (hasActiveGame) {
                        pendingGameModeChange = newMode
                    } else {
                        onGameModeSelected(newMode)
                        toast("Mode: ${newMode.displayName}")
                    }
                }
            )

            val toggleDifficulty: (Boolean) -> Unit = { value ->
                onDifficultyToggled(value)
                toast("Difficulty: ${if (value) "On" else "Off"}")
            }
            ToggleCard(
                title = "Difficulty",
                description = "Increase speed as levels progress",
                iconResId = R.drawable.speed_24px,
                checked = difficultyEnabled,
                onToggle = toggleDifficulty
            )

            val toggleReverse: (Boolean) -> Unit = { value ->
                onReverseModeToggled(value)
                toast("Reverse Mode: ${if (value) "On" else "Off"}")
            }
            ToggleCard(
                title = "Reverse Mode",
                description = "Watch the sequence forward, then repeat it in reverse",
                iconResId = R.drawable.speed_24px,
                checked = reverseModeEnabled,
                onToggle = toggleReverse
            )

            val togglePractice: (Boolean) -> Unit = { value ->
                onPracticeModeToggled(value)
                toast("Practice Mode: ${if (value) "On" else "Off"}")
            }
            ToggleCard(
                title = "Practice Mode",
                description = "Wrong buttons replay the sequence instead of ending the run. High scores are not recorded while enabled.",
                iconResId = R.drawable.reset_score_24px,
                checked = practiceModeEnabled,
                onToggle = togglePractice
            )

            val toggleAudioOnly: (Boolean) -> Unit = { value ->
                onAudioOnlyModeToggled(value)
                toast("Audio-Only: ${if (value) "On" else "Off"}")
            }
            ToggleCard(
                title = "Audio-Only Mode",
                description = "Hide button colors during playback — recognize the sequence by sound alone",
                iconResId = R.drawable.music_note_24px,
                checked = audioOnlyModeEnabled,
                onToggle = toggleAudioOnly
            )

            val requestMemoryLightsPlusToggle: (Boolean) -> Unit = { newValue ->
                if (hasActiveGame) {
                    pendingMemoryLightsPlusToggle = newValue
                } else {
                    onMemoryLightsPlusToggled(newValue)
                    toast("Memory Lights+: ${if (newValue) "On" else "Off"}")
                }
            }
            ToggleCard(
                title = "Memory Lights+",
                description = "6 buttons",
                iconResId = R.drawable.play_arrow_24px,
                iconTint = Color(0xFF4CAF50),
                checked = memoryLightsPlusEnabled,
                onToggle = requestMemoryLightsPlusToggle
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    pendingGameModeChange?.let { newMode ->
        AlertDialog(
            onDismissRequest = { pendingGameModeChange = null },
            title = {
                Text(
                    text = "End current game?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Switching to ${newMode.displayName} will end your current run. Continue?",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onGameModeSelected(newMode)
                        snackbarHostState.currentSnackbarData?.dismiss()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Mode: ${newMode.displayName}",
                                duration = SnackbarDuration.Short
                            )
                        }
                        pendingGameModeChange = null
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
                    onClick = { pendingGameModeChange = null },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Cancel")
                }
            },
            containerColor = com.happypuppy.memorylights.ui.theme.DialogBackground,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

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
                        toast("Memory Lights+: ${if (newValue) "On" else "Off"}")
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
            containerColor = com.happypuppy.memorylights.ui.theme.DialogBackground,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

@Composable
private fun GameModeCard(
    selected: GameMode,
    onSelect: (GameMode) -> Unit
) {
    SettingsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.speed_24px),
                    contentDescription = "Game mode",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Game Mode",
                        color = Color.White,
                        fontSize = 16.sp
                    )

                    Text(
                        text = "Classic plays until you miss; Speed Blitz races to level 20",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameMode.entries.forEach { mode ->
                    FilterChip(
                        selected = mode == selected,
                        onClick = { onSelect(mode) },
                        label = { Text(mode.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SurfaceContainer,
                            labelColor = Color.Gray,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleCard(
    title: String,
    description: String,
    iconResId: Int,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    iconTint: Color = Color.White
) {
    SettingsCard(onClick = { onToggle(!checked) }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontSize = 16.sp)
                Text(text = description, color = Color.Gray, fontSize = 14.sp)
            }

            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.DarkGray
                )
            )
        }
    }
}
