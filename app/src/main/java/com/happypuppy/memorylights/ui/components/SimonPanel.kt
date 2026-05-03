package com.happypuppy.memorylights.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

@Composable
fun SimonPanel(
    color: Color,
    colorName: String,
    isLit: Boolean = false,
    userPressed: Boolean = false,
    modifier: Modifier = Modifier,
    onTouchStateChanged: (Boolean) -> Unit = {}
) {
    // Define the brighter color for the "light on" effect
    val brightColor = remember(color) {
        Color(
            red = (color.red + 0.3f).coerceAtMost(1f),
            green = (color.green + 0.3f).coerceAtMost(1f),
            blue = (color.blue + 0.3f).coerceAtMost(1f),
            alpha = 1f
        )
    }

    // Create highlight color for the top-left gradient
    val highlightColor = remember(color) {
        Color(
            red = (color.red + 0.1f).coerceAtMost(1f),
            green = (color.green + 0.1f).coerceAtMost(1f),
            blue = (color.blue + 0.1f).coerceAtMost(1f),
            alpha = 1f
        )
    }

    // Create shadow color for the bottom-right gradient
    val shadowColor = remember(color) {
        Color(
            red = (color.red * 0.85f),
            green = (color.green * 0.85f),
            blue = (color.blue * 0.85f),
            alpha = 1f
        )
    }

    // Create darker color for border/shadow
    val darkBorderColor = remember(color) {
        Color(
            red = color.red * 0.25f,
            green = color.green * 0.25f,
            blue = color.blue * 0.25f,
            alpha = 0.9f
        )
    }

    // Create inner shadow color
    val innerShadowColor = remember(color) {
        Color(
            red = color.red * 0.15f,
            green = color.green * 0.15f,
            blue = color.blue * 0.15f,
            alpha = 0.95f
        )
    }

    // Single animation progress for the button press (0f to 1f)
    val pressAnimation by animateFloatAsState(
        targetValue = if (userPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressAnimation"
    )

    // Calculate explicitly with max function to ensure non-negative values
    val baseBottomPadding = 6f
    val baseRightPadding = 6f
    val pressedAmount = 4f

    val bottomPaddingValue = maxOf(0f, baseBottomPadding - (pressAnimation * pressedAmount))
    val rightPaddingValue = maxOf(0f, baseRightPadding - (pressAnimation * pressedAmount))
    val topPaddingValue = pressAnimation * 3f
    val startPaddingValue = pressAnimation * 3f

    // Color and gradient setup based on button state
    val buttonColors = when {
        userPressed -> {
            // When pressed: darker gradient to simulate shadow
            listOf(shadowColor, brightColor, brightColor)
        }
        isLit -> {
            // When lit but not pressed: bright overall with slight gradient
            listOf(brightColor, brightColor, brightColor.copy(red = brightColor.red * 0.95f,
                green = brightColor.green * 0.95f,
                blue = brightColor.blue * 0.95f))
        }
        else -> {
            // Default state: standard gradient
            listOf(highlightColor, color, shadowColor)
        }
    }

    // *** Use an offset instead of padding ***
    val offsetX = startPaddingValue
    val offsetY = topPaddingValue

    // Accessibility state description
    val stateDesc = when {
        userPressed -> "pressed"
        isLit -> "illuminated"
        else -> "off"
    }

    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp) // Minimum touch target size per Material Design
            .clip(RoundedCornerShape(16.dp))
            .semantics {
                contentDescription = "$colorName button"
                stateDescription = stateDesc
                role = Role.Button
            }
    ) {
        // Bottom layer - darker border/shadow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(darkBorderColor)
        )

        // Middle layer - main button with gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Use fixed padding for bottom and right
                .padding(
                    bottom = 6.dp,
                    end = 6.dp
                )
                // Use offset for the "pressed" effect instead of top/start padding
                .offset(x = offsetX.dp, y = offsetY.dp)
                // Reduce size to account for the offset
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = buttonColors,
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            // Notify pressed state
                            onTouchStateChanged(true)

                            // Wait for the finger to be released
                            val released = tryAwaitRelease()

                            // Notify released state
                            if (released) {
                                onTouchStateChanged(false)
                            }
                        }
                    )
                }
        )

        // Inner shadows and highlights
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Calculate inner shadow visibility (only when pressed)
            val shadowAlpha = pressAnimation * 0.8f

            // Inner shadows (when pressed)
            if (shadowAlpha > 0.01f) {
                // Draw top shadow line
                drawLine(
                    color = innerShadowColor.copy(alpha = shadowAlpha),
                    start = Offset(15f + offsetX, 10f + offsetY),
                    end = Offset(size.width - 15f, 10f + offsetY),
                    strokeWidth = 2.5f
                )

                // Draw left shadow line
                drawLine(
                    color = innerShadowColor.copy(alpha = shadowAlpha),
                    start = Offset(10f + offsetX, 15f + offsetY),
                    end = Offset(10f + offsetX, size.height - 15f),
                    strokeWidth = 2.5f
                )
            }

            // Light reflections (only when not pressed)
            val reflectionAlpha = (1f - pressAnimation) * 0.2f

            if (reflectionAlpha > 0.01f && !isLit) {
                // Draw top light reflection
                drawLine(
                    color = Color.White.copy(alpha = reflectionAlpha),
                    start = Offset(12f, 6f),
                    end = Offset(size.width - 25f, 6f),
                    strokeWidth = 2f
                )

                // Draw left light reflection
                drawLine(
                    color = Color.White.copy(alpha = reflectionAlpha * 0.75f),
                    start = Offset(6f, 12f),
                    end = Offset(6f, size.height - 25f),
                    strokeWidth = 2f
                )
            }
        }
    }
}