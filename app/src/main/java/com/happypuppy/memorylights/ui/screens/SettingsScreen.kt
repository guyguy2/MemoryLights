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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.happypuppy.memorylights.R
import com.happypuppy.memorylights.domain.enums.SoundPack
import com.happypuppy.memorylights.domain.enums.ThemeMode
import com.happypuppy.memorylights.domain.model.Achievement
import com.happypuppy.memorylights.ui.theme.LocalGameAccentColors
import com.happypuppy.memorylights.ui.theme.MyApplicationTheme
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
    val accents = LocalGameAccentColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    fun toast(message: String) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    var showResetHighScoreDialog by rememberSaveable { mutableStateOf(false) }
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
                NavCard(
                    title = stringResource(R.string.settings_game_modes_title),
                    summary = stringResource(R.string.settings_game_modes_summary),
                    iconPainterResId = R.drawable.speed_24px,
                    iconTint = MaterialTheme.colorScheme.onSurface,
                    onClick = onGameModesClick
                )

                NavCard(
                    title = stringResource(R.string.settings_gameplay_title),
                    summary = stringResource(R.string.settings_gameplay_summary),
                    iconPainterResId = R.drawable.schedule_24px,
                    iconTint = MaterialTheme.colorScheme.onSurface,
                    onClick = onGameplayClick
                )

                NavCard(
                    title = stringResource(R.string.settings_sound_title),
                    summary = stringResource(
                        R.string.settings_sound_summary,
                        stringResource(currentSoundPack.displayNameRes)
                    ),
                    iconPainterResId = R.drawable.music_note_24px,
                    iconTint = MaterialTheme.colorScheme.onSurface,
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
                    iconTint = accents.highScore,
                    onClick = onStatisticsClicked
                )

                NavCard(
                    title = stringResource(R.string.settings_statistics_title),
                    summary = stringResource(R.string.settings_statistics_summary),
                    iconImageVector = Icons.Default.DateRange,
                    iconTint = accents.averageScore,
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
                            contentDescription = null,
                            tint = accents.resetWarning,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_reset_title),
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Text(
                                text = stringResource(R.string.settings_reset_summary),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            contentDescription = null,
                            tint = accents.highScore,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_rate_title),
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Text(
                                text = stringResource(R.string.settings_rate_summary),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_about_title),
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Text(
                                text = stringResource(R.string.settings_about_summary, versionInfo),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
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
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.reset_high_score_body),
                    color = MaterialTheme.colorScheme.onSurface,
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
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
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.about_dialog_description),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    Text(
                        text = stringResource(R.string.about_dialog_version),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = versionInfo,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.about_dialog_copyright),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.about_dialog_rights),
                    color = MaterialTheme.colorScheme.onSurface,
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
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
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
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                iconPainterResId != null -> Icon(
                    painter = painterResource(iconPainterResId),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = summary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardModifier = modifier
        .fillMaxWidth()
        .padding(bottom = 8.dp)
    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
    val cardShape = RoundedCornerShape(12.dp)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            colors = cardColors,
            shape = cardShape
        ) {
            content()
        }
    } else {
        Card(
            modifier = cardModifier,
            colors = cardColors,
            shape = cardShape
        ) {
            content()
        }
    }
}

@Composable
fun SoundPackOption(
    soundPack: SoundPack,
    isSelected: Boolean,
    onSelect: () -> Unit,
    isAvailable: Boolean = soundPack.isAvailable
) {
    val backgroundColor = if (isSelected && isAvailable) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        Color.Transparent
    }
    val contentAlpha = if (isAvailable) 1f else 0.45f
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(enabled = isAvailable) { onSelect() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // onClick = null so the RadioButton itself doesn't ripple — the Row
        // clickable handles selection (and ripples once over the full row),
        // avoiding the previous double-ripple on every pack-row tap (#59).
        RadioButton(
            selected = isSelected && isAvailable,
            onClick = null,
            enabled = isAvailable,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = onSurfaceVariant,
                disabledSelectedColor = onSurfaceVariant.copy(alpha = 0.45f),
                disabledUnselectedColor = onSurfaceVariant.copy(alpha = 0.45f)
            )
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(soundPack.displayNameRes),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = stringResource(soundPack.descriptionRes),
                color = onSurfaceVariant.copy(alpha = contentAlpha),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (!isAvailable) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = stringResource(R.string.sound_pack_coming_soon),
                    color = onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
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
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_theme_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.settings_theme_summary),
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
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = mode == themeMode,
                        onClick = { onThemeModeChange(mode) },
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

@PreviewLightDark
@PreviewFontScale
@PreviewScreenSizes
@Composable
private fun SettingsScreenPreview() {
    MyApplicationTheme(themeMode = ThemeMode.DARK) {
        SettingsScreen(
            currentSoundPack = SoundPack.RETRO,
            themeMode = ThemeMode.DARK,
            onThemeModeChange = {},
            onGameModesClick = {},
            onGameplayClick = {},
            onSoundAndHapticsClick = {},
            onStatisticsClicked = {},
            onResetHighScore = {},
            onBackPressed = {},
            unlockedAchievements = setOf(Achievement.FirstWin.name, Achievement.Level10.name)
        )
    }
}

@Preview(name = "AMOLED Settings", showBackground = true)
@Composable
private fun SettingsScreenAmoledPreview() {
    MyApplicationTheme(themeMode = ThemeMode.AMOLED) {
        SettingsScreen(
            currentSoundPack = SoundPack.STANDARD,
            themeMode = ThemeMode.AMOLED,
            onThemeModeChange = {},
            onGameModesClick = {},
            onGameplayClick = {},
            onSoundAndHapticsClick = {},
            onStatisticsClicked = {},
            onResetHighScore = {},
            onBackPressed = {}
        )
    }
}

@Preview(name = "AMOLED About Dialog", showBackground = true)
@Composable
private fun AboutDialogPreview() {
    MyApplicationTheme(themeMode = ThemeMode.AMOLED) {
        AboutDialog(
            versionInfo = "1.0.3 (4)",
            onDismiss = {}
        )
    }
}
