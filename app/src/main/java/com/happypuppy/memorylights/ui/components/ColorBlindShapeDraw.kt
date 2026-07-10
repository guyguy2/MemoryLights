package com.happypuppy.memorylights.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.happypuppy.memorylights.domain.enums.ColorBlindShape
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-contrast fill + outline used for color-blind markers so shapes
 * remain readable on both dim and lit panel gradients.
 */
private val ShapeFill = Color.White.copy(alpha = 0.92f)
private val ShapeStroke = Color.Black.copy(alpha = 0.55f)

/**
 * Draws the geometric marker for [shape] centered at [center].
 *
 * [halfSize] is half the bounding-box edge length in pixels. A thin dark
 * outline is always drawn so the glyph survives bright yellow/orange fills.
 */
fun DrawScope.drawColorBlindShape(
    shape: ColorBlindShape,
    center: Offset,
    halfSize: Float
) {
    if (halfSize <= 0f) return
    val strokeWidth = (halfSize * 0.18f).coerceIn(1.5f, 3.5f)

    when (shape) {
        ColorBlindShape.CIRCLE -> {
            drawCircle(color = ShapeFill, radius = halfSize, center = center)
            drawCircle(
                color = ShapeStroke,
                radius = halfSize,
                center = center,
                style = Stroke(width = strokeWidth)
            )
        }

        ColorBlindShape.TRIANGLE -> {
            val path = Path().apply {
                moveTo(center.x, center.y - halfSize)
                lineTo(center.x + halfSize, center.y + halfSize * 0.85f)
                lineTo(center.x - halfSize, center.y + halfSize * 0.85f)
                close()
            }
            drawPath(path, color = ShapeFill, style = Fill)
            drawPath(path, color = ShapeStroke, style = Stroke(width = strokeWidth))
        }

        ColorBlindShape.SQUARE -> {
            val topLeft = Offset(center.x - halfSize, center.y - halfSize)
            val size = Size(halfSize * 2f, halfSize * 2f)
            drawRect(color = ShapeFill, topLeft = topLeft, size = size)
            drawRect(
                color = ShapeStroke,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = strokeWidth)
            )
        }

        ColorBlindShape.CROSS -> {
            val arm = halfSize * 0.32f
            // Vertical bar
            drawRect(
                color = ShapeFill,
                topLeft = Offset(center.x - arm, center.y - halfSize),
                size = Size(arm * 2f, halfSize * 2f)
            )
            // Horizontal bar
            drawRect(
                color = ShapeFill,
                topLeft = Offset(center.x - halfSize, center.y - arm),
                size = Size(halfSize * 2f, arm * 2f)
            )
            // Outline via four strokes for a crisp plus silhouette
            val path = Path().apply {
                // Outer path of a plus sign (12 vertices)
                val a = arm
                val h = halfSize
                moveTo(center.x - a, center.y - h)
                lineTo(center.x + a, center.y - h)
                lineTo(center.x + a, center.y - a)
                lineTo(center.x + h, center.y - a)
                lineTo(center.x + h, center.y + a)
                lineTo(center.x + a, center.y + a)
                lineTo(center.x + a, center.y + h)
                lineTo(center.x - a, center.y + h)
                lineTo(center.x - a, center.y + a)
                lineTo(center.x - h, center.y + a)
                lineTo(center.x - h, center.y - a)
                lineTo(center.x - a, center.y - a)
                close()
            }
            drawPath(path, color = ShapeStroke, style = Stroke(width = strokeWidth))
        }

        ColorBlindShape.DIAMOND -> {
            val path = Path().apply {
                moveTo(center.x, center.y - halfSize)
                lineTo(center.x + halfSize, center.y)
                lineTo(center.x, center.y + halfSize)
                lineTo(center.x - halfSize, center.y)
                close()
            }
            drawPath(path, color = ShapeFill, style = Fill)
            drawPath(path, color = ShapeStroke, style = Stroke(width = strokeWidth))
        }

        ColorBlindShape.STRIPES -> {
            val barHeight = halfSize * 0.28f
            val barHalfWidth = halfSize
            val gaps = listOf(-halfSize * 0.65f, 0f, halfSize * 0.65f)
            gaps.forEach { dy ->
                val topLeft = Offset(center.x - barHalfWidth, center.y + dy - barHeight / 2f)
                val size = Size(barHalfWidth * 2f, barHeight)
                drawRect(color = ShapeFill, topLeft = topLeft, size = size)
                drawRect(
                    color = ShapeStroke,
                    topLeft = topLeft,
                    size = size,
                    style = Stroke(width = strokeWidth * 0.7f)
                )
            }
        }
    }
}

/**
 * Arc mid-point in Canvas coordinates (0° = 3 o'clock, clockwise — matches
 * [androidx.compose.ui.graphics.drawscope.DrawScope.drawArc]).
 */
fun arcMidpoint(
    center: Offset,
    radius: Float,
    startAngleDeg: Float,
    sweepAngleDeg: Float
): Offset {
    val midDeg = startAngleDeg + sweepAngleDeg / 2f
    val rad = Math.toRadians(midDeg.toDouble())
    return Offset(
        x = center.x + radius * cos(rad).toFloat(),
        y = center.y + radius * sin(rad).toFloat()
    )
}

