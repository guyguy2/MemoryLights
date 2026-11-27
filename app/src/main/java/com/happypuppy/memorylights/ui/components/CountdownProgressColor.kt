package com.happypuppy.memorylights.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.happypuppy.memorylights.ui.theme.DefaultGameAccentColors
import com.happypuppy.memorylights.ui.theme.GameAccentColors

val CountdownGreen = DefaultGameAccentColors.countdownGreen
val CountdownAmber = DefaultGameAccentColors.countdownAmber
val CountdownRed = DefaultGameAccentColors.countdownRed

/**
 * Smooth 3-stop countdown urgency color:
 * - `1.0f -> 0.5f`: green -> amber
 * - `0.5f -> 0.0f`: amber -> red
 *
 * Uses theme-matched [accents] so both dark/AMOLED and light surfaces meet
 * WCAG non-text contrast (>= 3.0:1). Inputs outside `[0f, 1f]` are clamped.
 */
fun countdownProgressColor(
    fraction: Float,
    accents: GameAccentColors = DefaultGameAccentColors
): Color {
    val clamped = fraction.coerceIn(0f, 1f)
    return if (clamped >= 0.5f) {
        lerp(accents.countdownAmber, accents.countdownGreen, (clamped - 0.5f) * 2f)
    } else {
        lerp(accents.countdownRed, accents.countdownAmber, clamped * 2f)
    }
}

/**
 * Computes Speed Blitz elapsed milliseconds, freezing on [pauseStartedAtMs]
 * when paused and clamping negative deltas to `0L`.
 */
fun computeBlitzElapsedMs(
    startTimeMs: Long,
    nowMs: Long,
    pauseStartedAtMs: Long = 0L
): Long = when {
    startTimeMs <= 0L -> 0L
    pauseStartedAtMs > 0L -> (pauseStartedAtMs - startTimeMs).coerceAtLeast(0L)
    else -> (nowMs - startTimeMs).coerceAtLeast(0L)
}

/**
 * Live 10 Hz Speed Blitz elapsed clock that freezes while [isPaused] or
 * when [pauseStartedAtMs] is active.
 */
@Composable
fun rememberBlitzElapsedMs(
    startTimeMs: Long,
    isPaused: Boolean,
    pauseStartedAtMs: Long = 0L
): Long {
    var nowMs by remember(startTimeMs, isPaused) {
        mutableLongStateOf(System.currentTimeMillis())
    }
    LaunchedEffect(startTimeMs, isPaused) {
        if (startTimeMs <= 0L || isPaused) return@LaunchedEffect
        while (true) {
            nowMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(100L)
        }
    }
    return computeBlitzElapsedMs(
        startTimeMs = startTimeMs,
        nowMs = nowMs,
        pauseStartedAtMs = pauseStartedAtMs
    )
}
