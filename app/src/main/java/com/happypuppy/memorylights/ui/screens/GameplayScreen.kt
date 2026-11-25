package com.happypuppy.memorylights.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.happypuppy.memorylights.R
import kotlinx.coroutines.launch

/**
 * Gameplay settings sub-screen (F18). Hosts pacing and accessibility
 * controls that tune *how* a run feels but don't change its rules:
 * Player Timeout and Color-Blind Mode (#35).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameplayScreen(
    playerTimeoutSeconds: Int,
    colorBlindModeEnabled: Boolean,
    onPlayerTimeoutChanged: (Int) -> Unit,
    onColorBlindModeToggled: (Boolean) -> Unit,
    onBackPressed: () -> Unit
) {
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
                title = { Text(stringResource(R.string.gameplay_title)) },
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
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.schedule_24px),
                            contentDescription = stringResource(R.string.gameplay_timeout_cd),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.gameplay_timeout_title),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Text(
                                text = stringResource(R.string.gameplay_timeout_summary),
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 30).forEach { seconds ->
                            FilterChip(
                                selected = playerTimeoutSeconds == seconds,
                                onClick = {
                                    if (playerTimeoutSeconds != seconds) {
                                        onPlayerTimeoutChanged(seconds)
                                        toast(resources.getString(R.string.snack_timeout_seconds, seconds))
                                    }
                                },
                                label = { Text(stringResource(R.string.gameplay_timeout_chip, seconds)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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

            Spacer(modifier = Modifier.height(12.dp))

            // #35 Color-blind mode — pure presentation toggle, safe mid-run.
            SettingsCard(onClick = {
                val next = !colorBlindModeEnabled
                onColorBlindModeToggled(next)
                toast(
                    resources.getString(
                        if (next) R.string.snack_color_blind_on else R.string.snack_color_blind_off
                    )
                )
            }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = stringResource(R.string.toggle_color_blind_title),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.toggle_color_blind_title),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.toggle_color_blind_summary),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Switch(
                        checked = colorBlindModeEnabled,
                        onCheckedChange = { value ->
                            onColorBlindModeToggled(value)
                            toast(
                                resources.getString(
                                    if (value) R.string.snack_color_blind_on else R.string.snack_color_blind_off
                                )
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
