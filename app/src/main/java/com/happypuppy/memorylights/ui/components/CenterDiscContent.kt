package com.happypuppy.memorylights.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happypuppy.memorylights.R
import com.happypuppy.memorylights.domain.enums.GameMode
import com.happypuppy.memorylights.domain.model.GameState
import com.happypuppy.memorylights.ui.theme.LocalGameAccentColors
import com.happypuppy.memorylights.ui.theme.LocalReducedMotion

/**
 * Format Speed Blitz elapsed time as `M:SS.s` — minutes, seconds, tenths.
 * Tenths help differentiate near-tied runs without the noise of full
 * milliseconds. Negative or zero inputs render as `0:00.0` so a stale
 * state never shows an unexpected `-` or rolls weirdly past midnight.
 *
 * Shared by [CenterDiscContent] (GameOver blitz summary) and [TopHudStrip]
 * (live timer). Public so all call sites and unit tests share one formatter.
 */
fun formatBlitzTime(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val minutes = safe / 60_000L
    val seconds = (safe % 60_000L) / 1_000L
    val tenths = (safe % 1_000L) / 100L
    return "%d:%02d.%d".format(minutes, seconds, tenths)
}

/**
 * Decluttered inner content for the 4-tile focal disc (Wave 19 Option A).
 *
 * High score and sound pack metadata moved to `SlimMetadataStrip` above the
 * grid so this disc carries only the immediate focal signal:
 * - Active play: animated Round number + `playerSequenceSize / sequenceSize`
 *   during `PlayerRepeating`.
 * - `WaitingToStart`: Play icon + concise "Tap to start" (no "1", pack, or best).
 * - `GameOver`: final score (or Blitz time) + subtle best + Play Again icon.
 * - Loading / `soundLoadError`: crisp spinner or warning icon + concise label.
 *
 * Font scale inside the fixed-diameter disc is bounded (`<= 1.25f`) so `2.0x`
 * system font scaling never clips or collides inside the circle (#72).
 */
@Composable
fun CenterDiscContent(
    gameState: GameState,
    gameMode: GameMode,
    level: Int,
    currentHighScore: Int,
    blitzElapsedMs: Long,
    currentBestBlitzTimeMs: Long,
    soundLoadError: String?,
    soundsLoaded: Boolean,
    sequenceSize: Int,
    playerSequenceSize: Int,
    modifier: Modifier = Modifier
) {
    val baseDensity = LocalDensity.current
    val clampedDensity = Density(
        density = baseDensity.density,
        fontScale = baseDensity.fontScale.coerceAtMost(1.25f)
    )
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val accents = LocalGameAccentColors.current
    val reducedMotion = LocalReducedMotion.current

    CompositionLocalProvider(LocalDensity provides clampedDensity) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    soundLoadError != null -> {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = stringResource(R.string.game_sound_load_failed),
                            tint = accents.countdownAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.game_sound_load_failed),
                            color = accents.countdownAmber,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    !soundsLoaded -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            color = onSurface,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.game_loading),
                            color = onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    gameState == GameState.GameOver -> {
                        if (gameMode == GameMode.SPEED_BLITZ && blitzElapsedMs > 0L) {
                            Text(
                                text = formatBlitzTime(blitzElapsedMs),
                                color = onSurface,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                autoSize = TextAutoSize.StepBased(
                                    minFontSize = 12.sp,
                                    maxFontSize = 18.sp,
                                    stepSize = 1.sp
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (currentBestBlitzTimeMs > 0L) {
                                Text(
                                    text = stringResource(
                                        R.string.game_best_time,
                                        formatBlitzTime(currentBestBlitzTimeMs)
                                    ),
                                    color = accents.highScore,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFeatureSettings = "tnum"
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Text(
                                text = level.toString(),
                                color = onSurface,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFeatureSettings = "tnum"
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                            if (currentHighScore > 0) {
                                Text(
                                    text = stringResource(R.string.game_best_score, currentHighScore),
                                    color = accents.highScore,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFeatureSettings = "tnum"
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(1.dp))
                        Icon(
                            painter = painterResource(R.drawable.play_arrow_24px),
                            contentDescription = stringResource(R.string.game_play_again),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    gameState == GameState.WaitingToStart -> {
                        Icon(
                            painter = painterResource(R.drawable.play_arrow_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.game_tap_to_start),
                            color = onSurface.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    else -> {
                        AnimatedContent(
                            targetState = level,
                            transitionSpec = {
                                if (reducedMotion) {
                                    EnterTransition.None togetherWith ExitTransition.None
                                } else {
                                    val slideSpring = spring<IntOffset>(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                    (slideInVertically(animationSpec = slideSpring) { height -> height } + fadeIn()) togetherWith
                                        (slideOutVertically(animationSpec = slideSpring) { height -> -height } + fadeOut())
                                }
                            },
                            label = "level_animation"
                        ) { animatedLevel ->
                            Text(
                                text = animatedLevel.toString(),
                                color = onSurface,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontFeatureSettings = "tnum"
                                ),
                                autoSize = TextAutoSize.StepBased(
                                    minFontSize = 14.sp,
                                    maxFontSize = 32.sp,
                                    stepSize = 1.sp
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (gameState == GameState.PlayerRepeating && sequenceSize > 0) {
                            Text(
                                text = stringResource(
                                    R.string.hud_step_progress,
                                    playerSequenceSize,
                                    sequenceSize
                                ),
                                color = onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFeatureSettings = "tnum"
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}
