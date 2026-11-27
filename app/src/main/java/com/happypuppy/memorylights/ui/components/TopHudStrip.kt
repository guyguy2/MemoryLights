package com.happypuppy.memorylights.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happypuppy.memorylights.R
import com.happypuppy.memorylights.domain.enums.GameMode
import com.happypuppy.memorylights.domain.model.GameState
import com.happypuppy.memorylights.ui.theme.LocalGameAccentColors

/**
 * Slim top metadata strip directly under `TopAppBar` for 4-tile mode (Wave 19 Option A).
 *
 * - Classic mode: shows `★ Best N` (when `currentHighScore > 0`) on the left and
 *   `♪ <Sound Pack>` on the right.
 * - Speed Blitz mode: integrates `Lvl X / targetLevel`, live elapsed timer (excluding
 *   paused time), `★ Best M:SS.s` (when `> 0`), and `♪ <Sound Pack>` into one single
 *   compact strip so there is never a stacked double-HUD.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SlimMetadataStrip(
    gameMode: GameMode,
    gameState: GameState,
    level: Int,
    targetLevel: Int,
    currentHighScore: Int,
    currentBestBlitzTimeMs: Long,
    blitzStartTimeMs: Long,
    blitzPauseStartedAtMs: Long,
    blitzElapsedMs: Long,
    @StringRes soundPackDisplayNameRes: Int,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val accents = LocalGameAccentColors.current

    if (gameMode == GameMode.SPEED_BLITZ) {
        val isActiveBlitz = gameState == GameState.ShowingSequence ||
            gameState == GameState.PlayerRepeating ||
            gameState == GameState.Paused
        val liveElapsedMs = if (isActiveBlitz) {
            rememberBlitzElapsedMs(
                startTimeMs = blitzStartTimeMs,
                isPaused = gameState == GameState.Paused,
                pauseStartedAtMs = blitzPauseStartedAtMs
            )
        } else {
            blitzElapsedMs
        }

        Surface(
            modifier = modifier
                .fillMaxWidth()
                .testTag(GameScreenTags.SLIM_METADATA_STRIP),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isActiveBlitz) {
                        Text(
                            text = stringResource(R.string.blitz_level_progress, level, targetLevel),
                            color = onSurface,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = stringResource(R.string.blitz_separator),
                            color = onSurface.copy(alpha = 0.45f),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = formatBlitzTime(liveElapsedMs),
                            color = onSurface,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            ),
                            maxLines = 1
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.game_mode_speed_blitz),
                            color = onSurface,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentBestBlitzTimeMs > 0L) {
                        Text(
                            text = stringResource(
                                R.string.hud_best_time_star,
                                formatBlitzTime(currentBestBlitzTimeMs)
                            ),
                            color = accents.highScore,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    SoundPackLabel(soundPackDisplayNameRes = soundPackDisplayNameRes)
                }
            }
        }
    } else {
        FlowRow(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .testTag(GameScreenTags.SLIM_METADATA_STRIP),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (currentHighScore > 0) {
                Text(
                    text = stringResource(R.string.hud_best_score_star, currentHighScore),
                    color = accents.highScore,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }
            SoundPackLabel(soundPackDisplayNameRes = soundPackDisplayNameRes)
        }
    }
}

/**
 * Full Top HUD Strip directly below `TopAppBar` for 6-tile Memory Lights+ mode (Wave 19 Option A).
 *
 * - Left: `ROUND` label + bold animated round number (or `BLITZ` + `Lvl X / 20` in Speed Blitz).
 * - Center: rounded status/step pill (`3 / 7` during `PlayerRepeating`, or Watch/Paused/Ready/etc.,
 *   paired with live Blitz timer when in Speed Blitz).
 * - Right: `★ Best N` (or `★ Best M:SS.s`) when `> 0` + `♪ <Sound Pack>`.
 * - Bottom edge: full-width `4.dp` horizontal green -> amber -> red countdown progress bar
 *   during `PlayerRepeating` (with persistent neutral track so HUD height never jumps).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FullTopHudStrip(
    gameMode: GameMode,
    gameState: GameState,
    level: Int,
    targetLevel: Int,
    sequenceSize: Int,
    playerSequenceSize: Int,
    currentHighScore: Int,
    currentBestBlitzTimeMs: Long,
    blitzStartTimeMs: Long,
    blitzPauseStartedAtMs: Long,
    blitzElapsedMs: Long,
    blitzWon: Boolean,
    soundsLoaded: Boolean,
    soundLoadError: String?,
    timeoutResetTick: Int,
    playerTimeoutMs: Long,
    @StringRes soundPackDisplayNameRes: Int,
    modifier: Modifier = Modifier
) {
    val isActiveBlitz = gameMode == GameMode.SPEED_BLITZ &&
        (gameState == GameState.ShowingSequence ||
            gameState == GameState.PlayerRepeating ||
            gameState == GameState.Paused)
    val liveElapsedMs = if (isActiveBlitz) {
        rememberBlitzElapsedMs(
            startTimeMs = blitzStartTimeMs,
            isPaused = gameState == GameState.Paused,
            pauseStartedAtMs = blitzPauseStartedAtMs
        )
    } else {
        blitzElapsedMs
    }

    val fontScale = LocalDensity.current.fontScale

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(GameScreenTags.FULL_TOP_HUD_STRIP),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = if (fontScale >= 1.5f) 6.dp else 8.dp)
            ) {
                val useStackedRows = maxWidth < 290.dp || (fontScale >= 1.5f && maxWidth < 460.dp)
                if (useStackedRows) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            HudRoundBlock(
                                gameMode = gameMode,
                                level = level,
                                targetLevel = targetLevel
                            )
                            HudBestAndPackBlock(
                                gameMode = gameMode,
                                currentHighScore = currentHighScore,
                                currentBestBlitzTimeMs = currentBestBlitzTimeMs,
                                soundPackDisplayNameRes = soundPackDisplayNameRes
                            )
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            HudStatusPill(
                                gameMode = gameMode,
                                gameState = gameState,
                                sequenceSize = sequenceSize,
                                playerSequenceSize = playerSequenceSize,
                                blitzElapsedDisplayMs = liveElapsedMs,
                                blitzWon = blitzWon,
                                soundsLoaded = soundsLoaded,
                                soundLoadError = soundLoadError
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HudRoundBlock(
                            gameMode = gameMode,
                            level = level,
                            targetLevel = targetLevel
                        )
                        HudStatusPill(
                            gameMode = gameMode,
                            gameState = gameState,
                            sequenceSize = sequenceSize,
                            playerSequenceSize = playerSequenceSize,
                            blitzElapsedDisplayMs = liveElapsedMs,
                            blitzWon = blitzWon,
                            soundsLoaded = soundsLoaded,
                            soundLoadError = soundLoadError
                        )
                        HudBestAndPackBlock(
                            gameMode = gameMode,
                            currentHighScore = currentHighScore,
                            currentBestBlitzTimeMs = currentBestBlitzTimeMs,
                            soundPackDisplayNameRes = soundPackDisplayNameRes
                        )
                    }
                }
            }

            // Bottom edge horizontal countdown bar (persistent track prevents height jump)
            HudCountdownBar(
                isPlayerTurn = gameState == GameState.PlayerRepeating,
                timeoutMs = playerTimeoutMs,
                resetTick = timeoutResetTick
            )
        }
    }
}

@Composable
private fun HudRoundBlock(
    gameMode: GameMode,
    level: Int,
    targetLevel: Int
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = stringResource(
                if (gameMode == GameMode.SPEED_BLITZ) R.string.hud_blitz_label
                else R.string.hud_round
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
            maxLines = 1
        )
        if (gameMode == GameMode.SPEED_BLITZ) {
            Text(
                text = stringResource(R.string.blitz_level_progress, level, targetLevel),
                color = onSurface,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1
            )
        } else {
            val reducedMotion = com.happypuppy.memorylights.ui.theme.LocalReducedMotion.current
            AnimatedContent(
                targetState = level,
                transitionSpec = {
                    if (reducedMotion) {
                        androidx.compose.animation.EnterTransition.None togetherWith
                            androidx.compose.animation.ExitTransition.None
                    } else {
                        val slideSpring = androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntOffset>(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                        )
                        (slideInVertically(animationSpec = slideSpring) { it } + androidx.compose.animation.fadeIn()) togetherWith
                            (slideOutVertically(animationSpec = slideSpring) { -it } + androidx.compose.animation.fadeOut())
                    }
                },
                label = "hud_round_animation"
            ) { animatedLevel ->
                Text(
                    text = animatedLevel.toString(),
                    color = onSurface,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HudStatusPill(
    gameMode: GameMode,
    gameState: GameState,
    sequenceSize: Int,
    playerSequenceSize: Int,
    blitzElapsedDisplayMs: Long,
    blitzWon: Boolean,
    soundsLoaded: Boolean,
    soundLoadError: String?
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val accents = LocalGameAccentColors.current
    val pillShape = androidx.compose.foundation.shape.CircleShape
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(onSurface.copy(alpha = 0.08f), pillShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, pillShape)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        when {
            soundLoadError != null -> {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = accents.countdownAmber,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = stringResource(R.string.hud_status_audio_error),
                    color = accents.countdownAmber,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            !soundsLoaded -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(13.dp),
                    color = onSurface,
                    strokeWidth = 2.dp
                )
                Text(
                    text = stringResource(R.string.game_loading),
                    color = onSurface.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            gameState == GameState.PlayerRepeating -> {
                Text(
                    text = stringResource(R.string.hud_step_progress, playerSequenceSize, sequenceSize),
                    color = onSurface,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    maxLines = 1
                )
                if (gameMode == GameMode.SPEED_BLITZ) {
                    Text(
                        text = stringResource(R.string.blitz_separator),
                        color = onSurface.copy(alpha = 0.45f),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = formatBlitzTime(blitzElapsedDisplayMs),
                        color = onSurface.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum"
                        ),
                        maxLines = 1
                    )
                }
            }

            gameState == GameState.ShowingSequence -> {
                Text(
                    text = stringResource(R.string.hud_status_watch),
                    color = onSurface.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
                if (gameMode == GameMode.SPEED_BLITZ) {
                    Text(
                        text = stringResource(R.string.blitz_separator),
                        color = onSurface.copy(alpha = 0.45f),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = formatBlitzTime(blitzElapsedDisplayMs),
                        color = onSurface.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum"
                        ),
                        maxLines = 1
                    )
                }
            }

            gameState == GameState.Paused -> {
                Text(
                    text = stringResource(R.string.hud_status_paused),
                    color = onSurface.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
                if (gameMode == GameMode.SPEED_BLITZ) {
                    Text(
                        text = stringResource(R.string.blitz_separator),
                        color = onSurface.copy(alpha = 0.45f),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = formatBlitzTime(blitzElapsedDisplayMs),
                        color = onSurface.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum"
                        ),
                        maxLines = 1
                    )
                }
            }

            gameState == GameState.WaitingToStart -> {
                Text(
                    text = stringResource(R.string.hud_status_ready),
                    color = onSurface.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
            }

            gameState == GameState.GameOver -> {
                Text(
                    text = stringResource(
                        if (blitzWon) R.string.hud_status_victory
                        else R.string.hud_status_game_over
                    ),
                    color = if (blitzWon) accents.victory else onSurface,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
                if (gameMode == GameMode.SPEED_BLITZ && blitzElapsedDisplayMs > 0L) {
                    Text(
                        text = stringResource(R.string.blitz_separator),
                        color = onSurface.copy(alpha = 0.45f),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = formatBlitzTime(blitzElapsedDisplayMs),
                        color = onSurface,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun HudBestAndPackBlock(
    gameMode: GameMode,
    currentHighScore: Int,
    currentBestBlitzTimeMs: Long,
    @StringRes soundPackDisplayNameRes: Int
) {
    val accents = LocalGameAccentColors.current
    Column(horizontalAlignment = Alignment.End) {
        if (gameMode == GameMode.SPEED_BLITZ) {
            if (currentBestBlitzTimeMs > 0L) {
                Text(
                    text = stringResource(
                        R.string.hud_best_time_star,
                        formatBlitzTime(currentBestBlitzTimeMs)
                    ),
                    color = accents.highScore,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else if (currentHighScore > 0) {
            Text(
                text = stringResource(R.string.hud_best_score_star, currentHighScore),
                color = accents.highScore,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        SoundPackLabel(soundPackDisplayNameRes = soundPackDisplayNameRes)
    }
}

@Composable
private fun SoundPackLabel(@StringRes soundPackDisplayNameRes: Int) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.music_note_24px),
            contentDescription = stringResource(R.string.game_sound_pack),
            tint = tint,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = stringResource(soundPackDisplayNameRes),
            color = tint,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HudCountdownBar(
    isPlayerTurn: Boolean,
    timeoutMs: Long,
    resetTick: Int
) {
    val timeoutFraction = remember { Animatable(1f) }
    LaunchedEffect(isPlayerTurn, resetTick, timeoutMs) {
        if (!isPlayerTurn) {
            timeoutFraction.snapTo(1f)
            return@LaunchedEffect
        }
        timeoutFraction.snapTo(1f)
        timeoutFraction.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = timeoutMs.toInt().coerceAtLeast(1),
                easing = LinearEasing
            )
        )
    }

    val cdLabel = stringResource(R.string.a11y_countdown_progress)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
            .testTag(GameScreenTags.COUNTDOWN_BAR)
            .then(
                if (isPlayerTurn) {
                    Modifier.semantics {
                        contentDescription = cdLabel
                        progressBarRangeInfo = ProgressBarRangeInfo(
                            current = timeoutFraction.value,
                            range = 0f..1f
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        if (isPlayerTurn) {
            val accents = LocalGameAccentColors.current
            Canvas(modifier = Modifier.fillMaxSize()) {
                val fraction = timeoutFraction.value.coerceIn(0f, 1f)
                if (fraction > 0f) {
                    drawRect(
                        color = countdownProgressColor(fraction, accents),
                        size = Size(width = size.width * fraction, height = size.height)
                    )
                }
            }
        }
    }
}
