package com.happypuppy.memorylights.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.happypuppy.memorylights.domain.enums.ColorBlindShape
import com.happypuppy.memorylights.domain.enums.SimonButton

/**
 * Colored-arc Canvas that wraps the central play disc. Renders either four
 * 90 degree arcs (classic Simon layout) or six 60 degree arcs (Memory
 * Lights+ mode).
 *
 * Previously inlined in `SimonGameScreen`; pulling it out drops ~100 lines
 * from the screen and concentrates the arc-drawing math (and #35 color-blind
 * per-arc shape glyphs) in one place. Stroke width, sweep angles, start
 * angles, and color order match the original inline blocks.
 *
 * Arc order (6-button mode, clockwise from the 0-degree axis which points
 * right): RED, BLUE, ORANGE, YELLOW, PURPLE, GREEN. Arc start angles
 * (4-button mode): GREEN 180, RED 270, BLUE 0, YELLOW 90.
 *
 * @param memoryLightsPlusEnabled when true, draws six arcs; otherwise four.
 * @param colorBlindModeEnabled when true, draws a small shape glyph at each
 *   arc midpoint so the disc rim mirrors the panel markers (#35).
 */
@Composable
fun CenterDiscCanvas(
    memoryLightsPlusEnabled: Boolean,
    colorBlindModeEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = 8.dp.toPx()
        val arcCount = if (memoryLightsPlusEnabled) 6 else 4
        val sweepAngle = 360f / arcCount
        val center = Offset(size.width / 2f, size.height / 2f)
        // Glyph sits just inside the stroke so it doesn't collide with the
        // level text in the disc center.
        val glyphRadius = (size.minDimension - strokeWidth) / 2f - strokeWidth * 0.6f
        val glyphHalf = (strokeWidth * 0.85f).coerceAtLeast(4.dp.toPx())

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
                val startAngle = index * sweepAngle
                drawArc(
                    color = button.color,
                    startAngle = startAngle,
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
                if (colorBlindModeEnabled) {
                    drawColorBlindShape(
                        shape = ColorBlindShape.forButton(button),
                        center = arcMidpoint(center, glyphRadius, startAngle, sweepAngle),
                        halfSize = glyphHalf
                    )
                }
            }
        } else {
            // 4-button mode: Classic 4 arcs (90 degrees each)
            val classicArcs = listOf(
                SimonButton.GREEN to 180f,
                SimonButton.RED to 270f,
                SimonButton.BLUE to 0f,
                SimonButton.YELLOW to 90f
            )

            classicArcs.forEach { (button, startAngle) ->
                drawArc(
                    color = button.color,
                    startAngle = startAngle,
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
                if (colorBlindModeEnabled) {
                    drawColorBlindShape(
                        shape = ColorBlindShape.forButton(button),
                        center = arcMidpoint(center, glyphRadius, startAngle, 90f),
                        halfSize = glyphHalf
                    )
                }
            }
        }
    }
}
