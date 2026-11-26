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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.happypuppy.memorylights.R
import com.happypuppy.memorylights.domain.enums.SoundPack
import com.happypuppy.memorylights.domain.enums.ThemeMode
import com.happypuppy.memorylights.domain.model.Achievement
import com.happypuppy.memorylights.util.AppVersionInfo
import kotlinx.coroutines.launch

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
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onGameModesClick: () -> Unit,
    onGameplayClick: () -> Unit,
    onSoundAndHapticsClick: () -> Unit,
    onStatisticsClicked: () -> Unit,
    onResetHighScore: () -> Unit,
    onBackPressed: () -> Unit,
    unlockedAchievements: Set<String> = emptySet()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    fun toast(message: String) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    var showResetHighScoreDialog by remember { mutableStateOf(false) }
    val versionInfo = remember(context) { AppVersionInfo.getFullVersionInfo(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
            NavCard(
                title = stringResource(R.string.settings_game_modes_title),
                summary = stringResource(R.string.settings_game_modes_summary),
                iconPainterResId = R.drawable.speed_24px,
                iconTint = Color.White,
                onClick = onGameModesClick
            )

            NavCard(
                title = stringResource(R.string.settings_gameplay_title),
                summary = stringResource(R.string.settings_gameplay_summary),
                iconPainterResId = R.drawable.schedule_24px,
                iconTint = Color.White,
                onClick = onGameplayClick
            )

            NavCard(
                title = stringResource(R.string.settings_sound_title),
                summary = stringResource(
                    R.string.settings_sound_summary,
                    stringResource(currentSoundPack.displayNameRes)
                ),
                iconPainterResId = R.drawable.music_note_24px,
                iconTint = Color.White,
                onClick = onSoundAndHapticsClick
            )

            ThemeCard(
                themeMode = themeMode,
                onThemeModeChange = { newMode ->
                    if (newMode != themeMode) {
                        onThemeModeChange(newMode)
                        toast(resources.getString(R.string.snack_theme, resources.getString(newMode.displayNameRes)))
                    }
                }
            )

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            )

            // F8 polish: surface unlocked-badge count without requiring a
            // Statistics-screen visit. Tapping leads to the same Statistics
            // route where the full Achievements list lives, so this is a
            // shortcut, not a duplicated surface.
            val totalAchievements = Achievement.entries.size
            val unlockedCount = Achievement.entries.count { it.name in unlockedAchievements }
            NavCard(
                title = stringResource(R.string.settings_achievements_title),
                summary = stringResource(
                    R.string.settings_achievements_summary,
                    unlockedCount,
                    totalAchievements
                ),
                iconImageVector = Icons.Default.Star,
                iconTint = Color(0xFFFFC107),
                onClick = onStatisticsClicked
            )

            NavCard(
                title = stringResource(R.string.settings_statistics_title),
                summary = stringResource(R.string.settings_statistics_summary),
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
                        contentDescription = stringResource(R.string.settings_reset_cd),
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_reset_title),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            text = stringResource(R.string.settings_reset_summary),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    TextButton(
                        onClick = { showResetHighScoreDialog = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.action_reset))
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHighest
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
                        contentDescription = stringResource(R.string.settings_rate_cd),
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_rate_title),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            text = stringResource(R.string.settings_rate_summary),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
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
                        contentDescription = stringResource(R.string.settings_about_cd),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_about_title),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            text = stringResource(R.string.settings_about_summary, versionInfo),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showAboutDialog) {
        AboutDialog(
            versionInfo = versionInfo,
            onDismiss = { showAboutDialog = false }
        )
    }

    if (showResetHighScoreDialog) {
        AlertDialog(
            onDismissRequest = { showResetHighScoreDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.reset_high_score_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.reset_high_score_body),
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
                    Text(stringResource(R.string.action_reset))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetHighScoreDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

@Composable
fun AboutDialog(
    versionInfo: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.about_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.about_dialog_description),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    Text(
                        text = stringResource(R.string.about_dialog_version),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = versionInfo,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.about_dialog_copyright),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.about_dialog_rights),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_ok),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
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
                Text(text = title, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                Text(text = summary, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
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
        MaterialTheme.colorScheme.surfaceContainerHighest
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
                text = stringResource(soundPack.displayNameRes),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = stringResource(soundPack.descriptionRes),
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Theme picker card (F12). Two FilterChips for AMOLED Black / Dark.
 * Mirrors the timeout chip-group pattern in GameplayScreen so users
 * recognise the segmented-control idiom.
 */
@Composable
private fun ThemeCard(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    SettingsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = stringResource(R.string.settings_theme_cd),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_theme_title),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.settings_theme_summary),
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
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = mode == themeMode,
                        onClick = { onThemeModeChange(mode) },
                        label = { Text(stringResource(mode.displayNameRes)) },
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
}
