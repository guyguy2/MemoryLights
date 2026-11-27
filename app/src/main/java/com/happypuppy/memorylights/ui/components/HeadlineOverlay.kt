package com.happypuppy.memorylights.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.happypuppy.memorylights.ui.theme.GameBoardBackdrop

/**
 * Fade/scale-animated headline badge used for HIGH SCORE / BEST TIME,
 * GAME OVER / VICTORY, SPEED UP!, and YOUR TURN.
 *
 * Placed inside layout-aware overlay slots in `SimonGameScreen` (above the
 * 4-tile center disc or centered/upper-aligned in 6-tile mode) rather than
 * fixed screen-height biases so headlines never collide with the Top HUD
 * strip or 4-tile center disc across portrait, landscape, or `2.0x` font
 * scale (#72).
 */
@Composable
fun HeadlineOverlay(
    visible: Boolean,
    text: String,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier,
    initialScale: Float = 0.85f,
    useScaleOut: Boolean = true
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = initialScale),
        exit = if (useScaleOut) fadeOut() + scaleOut(targetScale = initialScale) else fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = GameBoardBackdrop.copy(alpha = 0.78f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
            shadowElevation = 6.dp
        ) {
            Text(
                text = text,
                color = color,
                style = style,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}
