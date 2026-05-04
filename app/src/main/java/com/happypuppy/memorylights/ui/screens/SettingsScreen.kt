package com.happypuppy.memorylights.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.happypuppy.memorylights.R
import com.happypuppy.memorylights.domain.enums.SoundPack
import com.happypuppy.memorylights.ui.theme.CardBackground
import com.happypuppy.memorylights.ui.theme.DialogBackground
import com.happypuppy.memorylights.ui.theme.SurfaceSelected

/**
 * Top-level Settings screen — a navigation hub since F18. Three chevron
 * rows lead to focused sub-screens (Game Modes / Gameplay / Sound &
 * Haptics); the global utilities (Statistics, Reset, Rate, About) live
 * directly here so they remain one tap away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentSoundPack: SoundPack,
    onGameModesClick: () -> Unit,
    onGameplayClick: () -> Unit,
    onSoundAndHapticsClick: () -> Unit,
    onStatisticsClicked: () -> Unit,
    onResetHighScore: () -> Unit,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current

    var showAboutDialog by remember { mutableStateOf(false) }
    var showResetHighScoreDialog by remember { mutableStateOf(false) }

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
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            NavCard(
                title = "Game Modes",
                summary = "Classic / Speed Blitz, Difficulty, Reverse, Practice, Audio-Only, Memory Lights+",
                iconPainterResId = R.drawable.speed_24px,
                iconTint = Color.White,
                onClick = onGameModesClick
            )

            NavCard(
                title = "Gameplay",
                summary = "Player timeout",
                iconPainterResId = R.drawable.schedule_24px,
                iconTint = Color.White,
                onClick = onGameplayClick
            )

            NavCard(
                title = "Sound & Haptics",
                summary = "Current pack: ${currentSoundPack.displayName}",
                iconPainterResId = R.drawable.music_note_24px,
                iconTint = Color.White,
                onClick = onSoundAndHapticsClick
            )

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                thickness = 1.dp,
                color = SurfaceSelected
            )

            NavCard(
                title = "Statistics",
                summary = "View your game progress and stats",
                iconImageVector = Icons.Default.DateRange,
                iconTint = Color(0xFF4CAF50),
                onClick = onStatisticsClicked
            )

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
                        tint = Color(0xFFFF9800),
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

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                thickness = 1.dp,
                color = SurfaceSelected
            )

            SettingsCard(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = "https://play.google.com/store/apps/details?id=com.happypuppy.memorylights".toUri()
                        setPackage("com.android.vending")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        // Fallback to web browser when the Play Store app isn't installed
                        // (e.g. side-loaded build, GMS-less device).
                        val fallbackIntent = Intent(
                            Intent.ACTION_VIEW,
                            "https://play.google.com/store/apps/details?id=com.happypuppy.memorylights".toUri()
                        )
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
                        tint = Color(0xFFFFC107),
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

            SettingsCard(onClick = { showAboutDialog = true }) {
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

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

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
            containerColor = DialogBackground,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

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
            containerColor = DialogBackground,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

/**
 * A row that navigates to a sub-screen. Either an `ImageVector` or a
 * drawable resource id is required for the leading icon — exactly one
 * should be non-null.
 */
@Composable
private fun NavCard(
    title: String,
    summary: String,
    iconTint: Color,
    onClick: () -> Unit,
    iconImageVector: ImageVector? = null,
    iconPainterResId: Int? = null
) {
    SettingsCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                iconImageVector != null -> Icon(
                    imageVector = iconImageVector,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                iconPainterResId != null -> Icon(
                    painter = painterResource(iconPainterResId),
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontSize = 16.sp)
                Text(text = summary, color = Color.Gray, fontSize = 14.sp)
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
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
            containerColor = CardBackground
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
        SurfaceSelected
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
        // onClick = null so the RadioButton itself doesn't ripple — the Row
        // clickable handles selection (and ripples once over the full row),
        // avoiding the previous double-ripple on every pack-row tap (#59).
        RadioButton(
            selected = isSelected,
            onClick = null,
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
