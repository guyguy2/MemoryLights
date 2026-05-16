package com.happypuppy.memorylights.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Persistent player-turn cue — a soft pulsing halo around the center
 * disc, drawn for the entire `GameState.PlayerRepeating` phase.
 *
 * The "YOUR TURN" headline only shows for the first three rounds
 * (#15 accessibility decision — fewer interruptions for screen
 * readers). After that, this halo is the sole turn indicator, so it
 * animates (both alpha and scale, in lockstep on a 1.1s reverse
 * loop) to stay legible peripherally even at high levels (#60).
 *
 * Alpha sweeps 0.06 -> 0.18 and scale sweeps 1.00 -> 1.05 — soft
 * enough not to compete with the colored panels, just enough to
 * register as movement at the edge of vision.
 *
 * Extracted from `SimonGameScreen` (Wave 4) — size (130dp), corner
 * radius (65dp), animation curve (FastOutSlowInEasing, 1100ms), and
 * alpha/scale stops are byte-equivalent to the previous inline block.
 * The on-screen guard (`gameState == PlayerRepeating`) stays in the
 * screen as the gate.
 */
@Composable
fun PlayerTurnHalo(
    modifier: Modifier = Modifier
) {
    val turnTransition = rememberInfiniteTransition(label = "turnPulse")
    val turnAlpha by turnTransition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "turnAlpha"
    )
    val turnScale by turnTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "turnScale"
    )
    Box(
        modifier = modifier
            .size(130.dp)
            .graphicsLayer(scaleX = turnScale, scaleY = turnScale)
            .background(
                Color.White.copy(alpha = turnAlpha),
                RoundedCornerShape(65.dp)
            )
    )
}
