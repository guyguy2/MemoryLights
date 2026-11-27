package com.happypuppy.memorylights.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.happypuppy.memorylights.ui.theme.LocalReducedMotion

/**
 * Spring-animated headline badge used for HIGH SCORE / BEST TIME,
 * GAME OVER / VICTORY, SPEED UP!, and YOUR TURN.
 *
 * Placed inside layout-aware overlay slots in `SimonGameScreen` (above the
 * 4-tile center disc or centered/upper-aligned in 6-tile mode) so headlines
 * never collide with the Top HUD strip or 4-tile center disc across portrait,
 * landscape, or `2.0x` font scale (#72). Honors [LocalReducedMotion].
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
    val reducedMotion = LocalReducedMotion.current
    val enter = if (reducedMotion) {
        EnterTransition.None
    } else {
        fadeIn() + scaleIn(
            initialScale = initialScale,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }
    val exit = if (reducedMotion) {
        ExitTransition.None
    } else if (useScaleOut) {
        fadeOut() + scaleOut(
            targetScale = initialScale,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    } else {
        fadeOut()
    }

    AnimatedVisibility(
        visible = visible,
        enter = enter,
        exit = exit,
        modifier = modifier
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
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
