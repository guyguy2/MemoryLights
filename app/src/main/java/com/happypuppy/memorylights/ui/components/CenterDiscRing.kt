package com.happypuppy.memorylights.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.happypuppy.memorylights.domain.enums.SimonButton

/**
 * Unified single reactive border ring for the 4-tile center disc (Wave 19 Option A).
 *
 * Replaces the previous 3 competing rings (`CenterDiscCanvas`, `TimeoutCountdownRing`,
 * `PlayerTurnHalo`) with one cohesive ring inset cleanly inside the dark outer gutter:
 * - Idle / `ShowingSequence` / `Paused` / `GameOver`: subtle neutral track that gently
 *   tints and glows with [litButton]'s color when a tile lights up (stays neutral in
 *   Audio-Only mode because `displayLit` is already `null`).
 * - `PlayerRepeating`: smoothly drains a green -> amber -> red countdown arc over
 *   [timeoutMs], re-keyed on [resetTick] and [timeoutMs].
 */
@Composable
fun CenterDiscRing(
    isPlayerTurn: Boolean,
    timeoutMs: Long,
    resetTick: Int,
    litButton: SimonButton?,
    modifier: Modifier = Modifier
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

    val neutralRingColor = Color.White.copy(alpha = 0.16f)
    val reactiveTargetColor = litButton?.color ?: neutralRingColor
    val reactiveColor by animateColorAsState(
        targetValue = reactiveTargetColor,
        animationSpec = tween(durationMillis = 120),
        label = "discRingReactiveColor"
    )
    val reactiveGlowAlpha by animateFloatAsState(
        targetValue = if (!isPlayerTurn && litButton != null) 0.35f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "discRingGlowAlpha"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag(GameScreenTags.CENTER_DISC_RING)
            .then(
                if (isPlayerTurn) {
                    Modifier.semantics {
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
        val gutterInset = 4.dp.toPx()
        val strokeWidth = 3.5.dp.toPx()
        val arcInset = gutterInset + strokeWidth / 2f
        val arcSize = Size(
            width = (size.width - arcInset * 2f).coerceAtLeast(0f),
            height = (size.height - arcInset * 2f).coerceAtLeast(0f)
        )
        val arcTopLeft = Offset(arcInset, arcInset)

        // Subtle neutral track
        drawArc(
            color = Color.White.copy(alpha = 0.14f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth)
        )

        if (isPlayerTurn) {
            val fraction = timeoutFraction.value.coerceIn(0f, 1f)
            if (fraction > 0f) {
                drawArc(
                    color = countdownProgressColor(fraction),
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        } else if (litButton != null || reactiveGlowAlpha > 0.01f) {
            if (reactiveGlowAlpha > 0.01f) {
                drawArc(
                    color = reactiveColor.copy(alpha = reactiveGlowAlpha),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth * 2.1f)
                )
            }
            drawArc(
                color = reactiveColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

/**
 * Consumes pointer events within the circular bounds of the center disc so
 * stray taps during active play never pass through to `SimonPanel` underneath.
 */
fun Modifier.consumeCircularTouch(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = minOf(size.width, size.height) / 2f
            val anyInside = event.changes.any { change ->
                (change.position - center).getDistance() <= radius
            }
            if (anyInside) {
                event.changes.forEach { it.consume() }
            }
        }
    }
}
