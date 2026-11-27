package com.happypuppy.memorylights.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.happypuppy.memorylights.R
import com.happypuppy.memorylights.ui.theme.LocalReducedMotion

/**
 * Full-screen paused overlay (F14). Dims the play area, shows a centered
 * play-arrow icon with "Paused" headline + hint, and resumes the game when
 * the player taps anywhere on the dim backdrop.
 *
 * Z-ordered at 10f so the player cannot accidentally hit a colored panel
 * underneath. Tap target has no ripple — the overlay itself is the
 * affordance, and a ripple flash would be noisy on a transient backdrop.
 *
 * Accessibility: the full backdrop carries a `contentDescription` so
 * TalkBack announces "Game paused. Tap to resume." when the overlay
 * appears.
 *
 * Extracted from `SimonGameScreen` (Wave 4) — animation curves, colors,
 * sizes, and z-order are byte-equivalent to the previous inline block.
 */
@Composable
fun PausedOverlay(
    visible: Boolean,
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pauseOverlayCd = stringResource(R.string.a11y_pause_overlay)
    val reducedMotion = LocalReducedMotion.current
    AnimatedVisibility(
        visible = visible,
        enter = if (reducedMotion) EnterTransition.None else fadeIn(),
        exit = if (reducedMotion) ExitTransition.None else fadeOut(),
        modifier = modifier
            .fillMaxSize()
            .zIndex(10f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onResume
                )
                .semantics {
                    contentDescription = pauseOverlayCd
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(R.drawable.play_arrow_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(96.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.game_paused),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.game_paused_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
