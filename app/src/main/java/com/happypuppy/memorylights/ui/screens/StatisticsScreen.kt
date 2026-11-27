package com.happypuppy.memorylights.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.happypuppy.memorylights.R
import com.happypuppy.memorylights.domain.enums.ThemeMode
import com.happypuppy.memorylights.domain.model.Achievement
import com.happypuppy.memorylights.domain.model.GameStatistics
import com.happypuppy.memorylights.ui.theme.LocalGameAccentColors
import com.happypuppy.memorylights.ui.theme.MyApplicationTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    statistics: GameStatistics,
    currentHighScore: Int,
    unlockedAchievements: Set<String> = emptySet(),
    onResetStatistics: () -> Unit = {},
    onBackPressed: () -> Unit
) {
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    val accents = LocalGameAccentColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics_title)) },
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
                    .padding(horizontal = 16.dp)
            ) {
                // Main Statistics Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.statistics_section_heading),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() }
                    )
                }

                // Statistics Cards. Each row gets a distinct icon so the eye can
                // anchor on the metric without reading every label (#57).
                StatisticCard(
                    title = stringResource(R.string.statistic_games_played),
                    value = statistics.gamesPlayed.toString(),
                    icon = Icons.Default.PlayArrow,
                    color = accents.gamesPlayed
                )

                StatisticCard(
                    title = stringResource(R.string.statistic_high_score),
                    value = currentHighScore.toString(),
                    icon = Icons.Default.Star,
                    color = accents.highScore
                )

                StatisticCard(
                    title = stringResource(R.string.statistic_average_score),
                    value = stringResource(R.string.statistics_average_format, statistics.averageScore),
                    icon = Icons.AutoMirrored.Filled.List,
                    color = accents.averageScore
                )

                StatisticCard(
                    title = stringResource(R.string.statistic_total_score),
                    value = statistics.totalScore.toString(),
                    icon = Icons.Default.AddCircle,
                    color = accents.totalScore
                )

                StatisticCard(
                    title = stringResource(R.string.statistic_best_streak),
                    value = statistics.bestStreak.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = accents.bestStreak
                )

                // Achievements Section (F8)
                AchievementsSection(unlockedAchievements = unlockedAchievements)

                // Reset Section
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                )

                // Only the inner Reset button triggers the destructive dialog so
                // there is no nested clickable trap or duplicate TalkBack stop.
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = accents.bestStreak,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.statistics_reset_title),
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Text(
                                text = stringResource(R.string.statistics_reset_summary),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        TextButton(
                            onClick = { showResetDialog = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.action_reset))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Reset confirmation dialog
    if (showResetDialog) {
        ResetStatisticsDialog(
            onConfirm = {
                onResetStatistics()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }
}

@Composable
internal fun ResetStatisticsDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.statistics_reset_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = stringResource(R.string.statistics_reset_dialog_body),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.action_reset))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
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

@Composable
fun StatisticCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Text(
                text = value,
                color = color,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum")
            )
        }
    }
}

/**
 * F8: render the unlocked-achievement count + a row per badge. Locked badges
 * appear subdued so the player sees what's still available. Order follows
 * [Achievement] enum declaration.
 */
@Composable
private fun AchievementsSection(unlockedAchievements: Set<String>) {
    val all = Achievement.entries
    val unlockedCount = all.count { it.name in unlockedAchievements }
    val accents = LocalGameAccentColors.current

    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) { heading() }
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = accents.highScore,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(R.string.achievements_section_heading),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(R.string.achievements_unlocked_format, unlockedCount, all.size),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum")
        )
    }

    if (unlockedCount == 0) {
        Text(
            text = stringResource(R.string.achievements_empty),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    } else {
        Spacer(modifier = Modifier.height(4.dp))
    }

    all.forEach { achievement ->
        AchievementRow(
            achievement = achievement,
            unlocked = achievement.name in unlockedAchievements
        )
    }
}

@Composable
private fun AchievementRow(achievement: Achievement, unlocked: Boolean) {
    val title = stringResource(achievement.titleRes)
    val description = stringResource(achievement.descriptionRes)
    val rowContentDescription = stringResource(
        if (unlocked) R.string.achievement_cd_unlocked else R.string.achievement_cd_locked,
        title,
        description
    )
    val icon = if (unlocked) Icons.Default.CheckCircle else Icons.Default.Lock
    val tint = if (unlocked) {
        LocalGameAccentColors.current.highScore
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val textColor = if (unlocked) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = rowContentDescription
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@PreviewLightDark
@PreviewFontScale
@PreviewScreenSizes
@Composable
private fun StatisticsScreenPopulatedPreview() {
    MyApplicationTheme(
        themeMode = if (androidx.compose.foundation.isSystemInDarkTheme()) ThemeMode.DARK else ThemeMode.LIGHT
    ) {
        StatisticsScreen(
            statistics = GameStatistics(
                gamesPlayed = 24,
                totalScore = 186,
                bestStreak = 6
            ),
            currentHighScore = 15,
            unlockedAchievements = setOf(
                Achievement.FirstWin.name,
                Achievement.Level10.name,
                Achievement.Streak3.name
            ),
            onBackPressed = {}
        )
    }
}

@Preview(name = "AMOLED Empty", showBackground = true)
@Composable
private fun StatisticsScreenEmptyAmoledPreview() {
    MyApplicationTheme(themeMode = ThemeMode.AMOLED) {
        StatisticsScreen(
            statistics = GameStatistics(),
            currentHighScore = 0,
            unlockedAchievements = emptySet(),
            onBackPressed = {}
        )
    }
}

@Preview(name = "AMOLED Reset Dialog", showBackground = true)
@Composable
private fun ResetStatisticsDialogPreview() {
    MyApplicationTheme(themeMode = ThemeMode.AMOLED) {
        ResetStatisticsDialog(onConfirm = {}, onDismiss = {})
    }
}
