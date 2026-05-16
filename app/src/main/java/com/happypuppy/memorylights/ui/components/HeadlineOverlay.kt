package com.happypuppy.memorylights.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * Bias-based alignment for the four center-anchored text overlays
 * (HIGH SCORE, GAME OVER, SPEED UP, YOUR TURN). Each overlay sits above
 * the center disc; the bias is roughly proportional to screen height so
 * the text floats consistently across phone sizes without a magic dp
 * offset (#31).
 *
 * Shared here so the screen and the overlay composable can both reach
 * them without duplicating the magic numbers.
 */
val OverlayBias6Button = BiasAlignment(0f, -0.18f)
val OverlayBias4Button = BiasAlignment(0f, -0.07f)
val OverlayBias4ButtonTurn = BiasAlignment(0f, -0.13f)

/**
 * Center-anchored, fade/scale-animated text overlay used for the four
 * headline states above the game disc: HIGH SCORE / VICTORY, GAME OVER,
 * SPEED UP, and YOUR TURN.
 *
 * The four call sites previously inlined the same
 * `AnimatedVisibility { Box(fillMax + Center) { Text(...) } }` block with
 * minor parameter differences. Consolidating here eliminates four copies
 * of the wrapper and concentrates the animation/layout tuning in one
 * place. The pattern is intentionally byte-equivalent to the previous
 * inline blocks — visibility, easing, scale, and alignment values were
 * preserved verbatim.
 *
 * @param initialScale starting scale for `scaleIn`/`scaleOut` (0.7 for
 *   the snappier HIGH SCORE / SPEED UP, 0.85 for GAME OVER, 0.9 for the
 *   subtler YOUR TURN nudge).
 * @param useScaleOut whether to apply `scaleOut` on exit. YOUR TURN
 *   exits with `fadeOut()` only because the next overlay (typically the
 *   level-advance halo) doesn't need a tight motion handoff.
 */
@Composable
fun HeadlineOverlay(
    visible: Boolean,
    text: String,
    color: Color,
    style: TextStyle,
    alignment: Alignment,
    initialScale: Float = 0.85f,
    useScaleOut: Boolean = true,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = initialScale),
        exit = if (useScaleOut) fadeOut() + scaleOut(targetScale = initialScale) else fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = color,
                style = style,
                modifier = Modifier
                    .align(alignment)
                    .shadow(4.dp)
            )
        }
    }
}
