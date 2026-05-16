package com.happypuppy.memorylights.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Inactivity countdown ring drawn around the center disc during
 * `GameState.PlayerRepeating`. Drains linearly over [timeoutMs] and
 * resets every time [resetTick] changes (the ViewModel bumps that
 * counter on each player press).
 *
 * Color lerps from green (fresh) to red (almost out of time) so the
 * urgency reads peripherally — players watching the colored panels
 * still catch the warning without having to look at the disc.
 *
 * Re-keying the `LaunchedEffect` on [timeoutMs] lets a mid-turn slider
 * change in Settings -> Gameplay re-launch the animation at the new
 * duration instead of finishing the old one. Re-keying on [resetTick]
 * (rather than the player's sequence length) keeps the ring honest
 * during the all-buttons flash that follows a successful round.
 *
 * Extracted from `SimonGameScreen` (Wave 4) — ring size (124dp),
 * stroke width (4dp), start angle (-90°), sweep math, easing, and
 * color stops are byte-equivalent to the previous inline block.
 */
@Composable
fun TimeoutCountdownRing(
    timeoutMs: Long,
    resetTick: Int,
    modifier: Modifier = Modifier
) {
    val timeoutFraction = remember { Animatable(1f) }
    LaunchedEffect(resetTick, timeoutMs) {
        timeoutFraction.snapTo(1f)
        timeoutFraction.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = timeoutMs.toInt(),
                easing = LinearEasing
            )
        )
    }
    val ringColor = lerp(
        Color(0xFFE53935), // red — almost out of time
        Color(0xFF4CAF50), // green — fresh
        timeoutFraction.value
    )
    Canvas(
        modifier = modifier
            .size(124.dp)
            .zIndex(2f)
    ) {
        val stroke = 4.dp.toPx()
        drawArc(
            color = ringColor,
            startAngle = -90f,
            sweepAngle = 360f * timeoutFraction.value,
            useCenter = false,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(
                width = size.width - stroke,
                height = size.height - stroke
            ),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}
