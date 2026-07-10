package com.happypuppy.memorylights.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happypuppy.memorylights.R
import com.happypuppy.memorylights.domain.enums.GameMode
import com.happypuppy.memorylights.domain.model.GameState

/**
 * Format Speed Blitz elapsed time as `M:SS.s` — minutes, seconds, tenths.
 * Tenths help differentiate near-tied runs without the noise of full
 * milliseconds. Negative or zero inputs render as `0:00.0` so a stale
 * state never shows an unexpected `-` or rolls weirdly past midnight.
 *
 * Shared by [CenterDiscContent] (GameOver blitz summary) and `BlitzHud`
 * (live timer). Public so the screen's private HUD can call the same
 * formatter without a second copy.
 */
fun formatBlitzTime(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val minutes = safe / 60_000L
    val seconds = (safe % 60_000L) / 1_000L
    val tenths = (safe % 1_000L) / 100L
    return "%d:%02d.%d".format(minutes, seconds, tenths)
}

/**
 * Inner content of the center play disc: sound-pack chip, level / loading /
 * game-over summary column, and high-score footer.
 *
 * Extracted from `SimonGameScreen` (Wave 9) so the screen stays focused on
 * layout scaffolding and overlays. Branching is byte-equivalent to the
 * prior inline block — same strings, sizes, colors, and AnimatedContent
 * slide for the in-progress level number.
 *
 * @param soundPackDisplayNameRes string resource for the current pack name
 * @param sequenceSize length of the full sequence (for N / M progress)
 * @param playerSequenceSize presses completed so far this turn
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
    @StringRes soundPackDisplayNameRes: Int,
    sequenceSize: Int,
    playerSequenceSize: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Sound pack indicator - only show when NOT in GameOver state
        if (gameState != GameState.GameOver) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.music_note_24px),
                    contentDescription = stringResource(R.string.game_sound_pack),
                    tint = Color.Gray,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = stringResource(soundPackDisplayNameRes),
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Center content (level number, play button, or loading indicator)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (soundLoadError != null) {
                // Surface the load error so the player isn't left
                // staring at a silent indefinite spinner.
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = stringResource(R.string.game_sound_load_failed),
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.game_sound_load_failed),
                    color = Color(0xFFFFB300),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = stringResource(R.string.game_audio_fallback),
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                )
            } else if (!soundsLoaded) {
                // Show loading indicator while sounds are loading
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.game_loading),
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall
                )
            } else if (gameState == GameState.GameOver) {
                // Score summary so the game-over moment lingers
                // long enough for the player to register it.
                // In Speed Blitz the headline is the elapsed
                // time; level is implicit (always 20 on win,
                // wherever the player died on loss).
                if (gameMode == GameMode.SPEED_BLITZ &&
                    blitzElapsedMs > 0L
                ) {
                    Text(
                        text = formatBlitzTime(blitzElapsedMs),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (currentBestBlitzTimeMs > 0L) {
                        Text(
                            text = stringResource(
                                R.string.game_best_time,
                                formatBlitzTime(currentBestBlitzTimeMs)
                            ),
                            color = Color(0xFFFFC107),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                        )
                    }
                } else {
                    Text(
                        text = level.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp)
                    )
                    if (currentHighScore > 0) {
                        Text(
                            text = stringResource(R.string.game_best_score, currentHighScore),
                            color = Color(0xFFFFC107),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                        )
                    }
                }
                Icon(
                    painter = painterResource(R.drawable.play_arrow_24px),
                    contentDescription = stringResource(R.string.game_play_again),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else if (gameState == GameState.WaitingToStart) {
                // Tap-to-start affordance — play icon + label invite
                // the player to begin instead of staring at a "1".
                Text(
                    text = level.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp)
                )
                Icon(
                    painter = painterResource(R.drawable.play_arrow_24px),
                    contentDescription = stringResource(R.string.game_tap_to_start),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.game_tap_to_start),
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                )
            } else {
                // Animated level display with smooth transitions
                AnimatedContent(
                    targetState = level,
                    transitionSpec = {
                        slideInVertically(
                            initialOffsetY = { height -> height }
                        ) togetherWith slideOutVertically(
                            targetOffsetY = { height -> -height }
                        )
                    },
                    label = "level_animation"
                ) { animatedLevel ->
                    Text(
                        text = animatedLevel.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.displaySmall
                    )
                }
                // Sequence-progress indicator under the level number
                // during the player's turn. Players lose count of
                // long sequences without a visible step counter (#64).
                if (gameState == GameState.PlayerRepeating &&
                    sequenceSize > 0
                ) {
                    Text(
                        text = "$playerSequenceSize / $sequenceSize",
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // High score display at bottom - only show when NOT in GameOver state
        if (currentHighScore > 0 && gameState != GameState.GameOver) {
            Text(
                text = stringResource(R.string.game_high_score_inline, currentHighScore),
                color = Color.Gray,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
            )
        }
    }
}
