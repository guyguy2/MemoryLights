package com.happypuppy.memorylights.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.happypuppy.memorylights.R
import com.happypuppy.memorylights.domain.enums.GameMode
import com.happypuppy.memorylights.ui.theme.LocalGameAccentColors
import kotlinx.coroutines.launch

/**
 * Game Modes settings sub-screen (F18). Hosts the toggles that change
 * how a run plays: Difficulty (speed scaling), Reverse Mode (recall in
 * reverse), Practice Mode (wrong button replays sequence), Audio-Only,
 * Daily Challenge, and Memory Lights+ (4-button → 6-button).
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
    dailyChallengeEnabled: Boolean,
    dailyCompletedToday: Boolean,
    dailyBestLevel: Int,
    gameMode: GameMode,
    hasActiveGame: Boolean,
    onDifficultyToggled: (Boolean) -> Unit,
    onReverseModeToggled: (Boolean) -> Unit,
    onPracticeModeToggled: (Boolean) -> Unit,
    onAudioOnlyModeToggled: (Boolean) -> Unit,
    onMemoryLightsPlusToggled: (Boolean) -> Unit,
    onDailyChallengeToggled: (Boolean) -> Unit,
    onGameModeSelected: (GameMode) -> Unit,
    onBackPressed: () -> Unit
) {
    var pendingMemoryLightsPlusToggle by remember { mutableStateOf<Boolean?>(null) }
    var pendingGameModeChange by remember { mutableStateOf<GameMode?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val resources = LocalResources.current
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
                title = { Text(stringResource(R.string.game_modes_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
                            toast(resources.getString(R.string.snack_mode, resources.getString(newMode.displayNameRes)))
                        }
                    }
                )

                val toggleDifficulty: (Boolean) -> Unit = { value ->
                    onDifficultyToggled(value)
                    toast(resources.getString(if (value) R.string.snack_difficulty_on else R.string.snack_difficulty_off))
                }
                ToggleCard(
                    title = stringResource(R.string.toggle_difficulty_title),
                    description = stringResource(R.string.toggle_difficulty_summary),
                    iconResId = R.drawable.speed_24px,
                    checked = difficultyEnabled,
                    onToggle = toggleDifficulty
                )

                val toggleReverse: (Boolean) -> Unit = { value ->
                    onReverseModeToggled(value)
                    toast(resources.getString(if (value) R.string.snack_reverse_on else R.string.snack_reverse_off))
                }
                ToggleCard(
                    title = stringResource(R.string.toggle_reverse_title),
                    description = stringResource(R.string.toggle_reverse_summary),
                    iconResId = R.drawable.speed_24px,
                    checked = reverseModeEnabled,
                    onToggle = toggleReverse
                )

                val togglePractice: (Boolean) -> Unit = { value ->
                    onPracticeModeToggled(value)
                    toast(resources.getString(if (value) R.string.snack_practice_on else R.string.snack_practice_off))
                }
                ToggleCard(
                    title = stringResource(R.string.toggle_practice_title),
                    description = stringResource(R.string.toggle_practice_summary),
                    iconResId = R.drawable.reset_score_24px,
                    checked = practiceModeEnabled,
                    onToggle = togglePractice
                )

                val toggleAudioOnly: (Boolean) -> Unit = { value ->
                    onAudioOnlyModeToggled(value)
                    toast(resources.getString(if (value) R.string.snack_audio_only_on else R.string.snack_audio_only_off))
                }
                ToggleCard(
                    title = stringResource(R.string.toggle_audio_only_title),
                    description = stringResource(R.string.toggle_audio_only_summary),
                    iconResId = R.drawable.music_note_24px,
                    checked = audioOnlyModeEnabled,
                    onToggle = toggleAudioOnly
                )

                val toggleDaily: (Boolean) -> Unit = { value ->
                    onDailyChallengeToggled(value)
                    toast(resources.getString(if (value) R.string.snack_daily_on else R.string.snack_daily_off))
                }
                // Subtitle: when on AND a run was already played today, lead with
                // completion + high score so the player sees progress inline.
                val dailyDescription = if (dailyChallengeEnabled && dailyCompletedToday && dailyBestLevel > 0) {
                    stringResource(
                        R.string.toggle_daily_challenge_completed_with_score,
                        stringResource(R.string.toggle_daily_challenge_completed),
                        stringResource(R.string.statistic_high_score),
                        dailyBestLevel
                    )
                } else {
                    stringResource(R.string.toggle_daily_challenge_summary)
                }
                ToggleCard(
                    title = stringResource(R.string.toggle_daily_challenge_title),
                    description = dailyDescription,
                    iconResId = R.drawable.schedule_24px,
                    checked = dailyChallengeEnabled,
                    onToggle = toggleDaily
                )

                val requestMemoryLightsPlusToggle: (Boolean) -> Unit = { newValue ->
                    if (hasActiveGame) {
                        pendingMemoryLightsPlusToggle = newValue
                    } else {
                        onMemoryLightsPlusToggled(newValue)
                        toast(resources.getString(if (newValue) R.string.snack_memory_lights_plus_on else R.string.snack_memory_lights_plus_off))
                    }
                }
                ToggleCard(
                    title = stringResource(R.string.toggle_memory_lights_plus_title),
                    description = stringResource(R.string.toggle_memory_lights_plus_summary),
                    iconResId = R.drawable.play_arrow_24px,
                    iconTint = LocalGameAccentColors.current.averageScore,
                    checked = memoryLightsPlusEnabled,
                    onToggle = requestMemoryLightsPlusToggle
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    pendingGameModeChange?.let { newMode ->
        AlertDialog(
            onDismissRequest = { pendingGameModeChange = null },
            title = {
                Text(
                    text = stringResource(R.string.confirm_end_game_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.confirm_end_game_mode,
                        stringResource(newMode.displayNameRes)
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onGameModeSelected(newMode)
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val msg = resources.getString(R.string.snack_mode, resources.getString(newMode.displayNameRes))
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = msg,
                                duration = SnackbarDuration.Short
                            )
                        }
                        pendingGameModeChange = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.action_switch))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingGameModeChange = null },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    pendingMemoryLightsPlusToggle?.let { newValue ->
        AlertDialog(
            onDismissRequest = { pendingMemoryLightsPlusToggle = null },
            title = {
                Text(
                    text = stringResource(R.string.confirm_end_game_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.confirm_end_game_generic),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onMemoryLightsPlusToggled(newValue)
                        toast(resources.getString(if (newValue) R.string.snack_memory_lights_plus_on else R.string.snack_memory_lights_plus_off))
                        pendingMemoryLightsPlusToggle = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.action_switch))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingMemoryLightsPlusToggle = null },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
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
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.game_modes_card_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = stringResource(R.string.game_modes_card_summary),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
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
                        label = { Text(stringResource(mode.displayNameRes)) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
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
    iconTint: Color = MaterialTheme.colorScheme.onSurface
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val hapticsEnabled = com.happypuppy.memorylights.ui.theme.LocalHapticsEnabled.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    role = Role.Switch,
                    onValueChange = { next ->
                        if (hapticsEnabled) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm)
                        }
                        onToggle(next)
                    }
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
        }
    }
}
