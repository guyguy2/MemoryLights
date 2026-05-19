package com.happypuppy.memorylights.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.happypuppy.memorylights.domain.enums.SimonButton

/**
 * Colored-arc Canvas that wraps the central play disc. Renders either four
 * 90 degree arcs (classic Simon layout) or six 60 degree arcs (Memory
 * Lights+ mode).
 *
 * Previously inlined in `SimonGameScreen`; pulling it out drops ~100 lines
 * from the screen and concentrates the arc-drawing math (and the future
 * landing zone for #35 color-blind labels) in one place. The two code
 * paths are intentionally byte-equivalent to the previous inline blocks —
 * stroke width, sweep angles, start angles, and color order were preserved
 * verbatim.
 *
 * Arc order (6-button mode, clockwise from the 0-degree axis which points
 * right): RED, BLUE, ORANGE, YELLOW, PURPLE, GREEN. Arc start angles
 * (4-button mode): GREEN 180, RED 270, BLUE 0, YELLOW 90.
 *
 * @param memoryLightsPlusEnabled when true, draws six arcs; otherwise four.
 */
@Composable
fun CenterDiscCanvas(
    memoryLightsPlusEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = 8.dp.toPx()
        val arcCount = if (memoryLightsPlusEnabled) 6 else 4
        val sweepAngle = 360f / arcCount

        if (memoryLightsPlusEnabled) {
            // 6-button mode: 6 arcs (60 degrees each)
            val buttonOrder = listOf(
                SimonButton.RED,    // 0-60 degrees (top-right)
                SimonButton.BLUE,   // 60-120 degrees (bottom-right)
                SimonButton.ORANGE, // 120-180 degrees (bottom)
                SimonButton.YELLOW, // 180-240 degrees (bottom-left)
                SimonButton.PURPLE, // 240-300 degrees (top-left)
                SimonButton.GREEN   // 300-360 degrees (top)
            )

            buttonOrder.forEachIndexed { index, button ->
                drawArc(
                    color = button.color,
                    startAngle = index * sweepAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = size.copy(
                        width = size.width - strokeWidth,
                        height = size.height - strokeWidth
                    ),
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Butt
                    )
                )
            }
        } else {
            // 4-button mode: Classic 4 arcs (90 degrees each)

            // Draw green arc (top-left)
            drawArc(
                color = SimonButton.GREEN.color,
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = size.copy(
                    width = size.width - strokeWidth,
                    height = size.height - strokeWidth
                ),
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Butt
                )
            )

            // Draw red arc (top-right)
            drawArc(
                color = SimonButton.RED.color,
                startAngle = 270f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = size.copy(
                    width = size.width - strokeWidth,
                    height = size.height - strokeWidth
                ),
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Butt
                )
            )

            // Draw blue arc (bottom-right)
            drawArc(
                color = SimonButton.BLUE.color,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = size.copy(
                    width = size.width - strokeWidth,
                    height = size.height - strokeWidth
                ),
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Butt
                )
            )

            // Draw yellow arc (bottom-left)
            drawArc(
                color = SimonButton.YELLOW.color,
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = size.copy(
                    width = size.width - strokeWidth,
                    height = size.height - strokeWidth
                ),
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Butt
                )
            )
        }
    }
}
