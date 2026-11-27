package com.happypuppy.memorylights.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.happypuppy.memorylights.R

/**
 * Bottom-aligned GameOver action cluster: a prominent filled primary "New Game"
 * CTA alongside Replay (Classic mode) and Share buttons, visible only on GameOver
 * with a non-empty sequence.
 *
 * In portrait / tall viewports (`maxHeight >= 480.dp` and non-landscape),
 * "New Game" sits vertically above the Replay + Share row for clear hierarchy.
 * In landscape or compact-height viewports (`maxHeight < 480.dp`), all visible
 * buttons collapse into a single horizontal [FlowRow] with tighter bottom padding
 * so they clear the `110.dp` center disc without overlapping "Play Again".
 *
 * `navigationBarsPadding()` keeps the action cluster clear of the system
 * navigation/gesture bar without pushing the background game panels off
 * edge-to-edge.
 *
 * Animations: fades + 0.9 scale-in on entry, plain fadeOut on exit.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReplayShareButtons(
    visible: Boolean,
    onNewGame: () -> Unit,
    onReplay: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    showReplay: Boolean = true
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        val orientation = LocalConfiguration.current.orientation
        val isLandscape = remember(orientation) {
            orientation == Configuration.ORIENTATION_LANDSCAPE
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            val useCompactRow = isLandscape || maxHeight < 480.dp
            if (useCompactRow) {
                FlowRow(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    NewGameActionButton(onClick = onNewGame)
                    if (showReplay) {
                        ReplayActionButton(onClick = onReplay)
                    }
                    ShareActionButton(onClick = onShare)
                }
            } else {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NewGameActionButton(onClick = onNewGame)

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (showReplay) {
                            ReplayActionButton(onClick = onReplay)
                        }
                        ShareActionButton(onClick = onShare)
                    }
                }
            }
        }
    }
}

@Composable
private fun NewGameActionButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.play_arrow_24px),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.game_new_game),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun ReplayActionButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Black.copy(alpha = 0.72f),
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.game_replay),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun ShareActionButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Black.copy(alpha = 0.72f),
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.game_share),
            style = MaterialTheme.typography.labelLarge
        )
    }
}
