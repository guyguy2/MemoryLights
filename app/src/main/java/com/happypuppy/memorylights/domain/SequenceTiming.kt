package com.happypuppy.memorylights.domain

import kotlin.math.ln

/**
 * Pure timing calculator for the Memory Lights sequence display.
 *
 * Returned pair is `(litDurationMs, pauseDurationMs)`.
 *
 * Difficulty curve (F5): when [difficultyEnabled] is true, levels 1–4 stay at base
 * timing. From level 5 onward the reduction follows a smooth logarithmic curve so
 * each level feels marginally faster than the last (instead of stair-stepping in
 * jumps every four levels). Output is clamped to [GameConstants.MIN_LIT_DURATION_MS]
 * and [GameConstants.MIN_PAUSE_DURATION_MS] so timing never collapses to zero.
 *
 * Pure + side-effect-free so it can be unit tested without standing up a ViewModel.
 */
fun calculateSequenceTiming(level: Int, difficultyEnabled: Boolean): Pair<Long, Long> {
    if (!difficultyEnabled || level < DIFFICULTY_BASELINE_LEVEL + 1) {
        return GameConstants.BASE_LIT_DURATION_MS to GameConstants.BASE_PAUSE_DURATION_MS
    }

    val effective = level - DIFFICULTY_BASELINE_LEVEL
    val ratio = ln((effective + 1).toDouble()) / LOG_DENOMINATOR
    val reductionFactor = 1.0 - DIFFICULTY_LOG_REDUCTION_MAX * ratio

    val litDuration = maxOf(
        GameConstants.MIN_LIT_DURATION_MS,
        (GameConstants.BASE_LIT_DURATION_MS * reductionFactor).toLong()
    )
    val pauseDuration = maxOf(
        GameConstants.MIN_PAUSE_DURATION_MS,
        (GameConstants.BASE_PAUSE_DURATION_MS * reductionFactor).toLong()
    )
    return litDuration to pauseDuration
}

// Last "easy" level — levels at or below this stay at base timing.
private const val DIFFICULTY_BASELINE_LEVEL = 4
// Effective-level ceiling: at this offset above baseline the log ratio reaches
// 1.0 and the floor clamp begins to dominate. Tuned so that the floor is hit
// roughly around level 29 (effective 25), giving a gentle ~25-level ramp.
private const val DIFFICULTY_LOG_RANGE = 25
// Maximum proportion of base timing the curve can shave off before clamping
// kicks in. 0.7 leaves the curve shy of zero so the min-clamp does the final
// pinning rather than the curve itself.
private const val DIFFICULTY_LOG_REDUCTION_MAX = 0.7
private val LOG_DENOMINATOR = ln((DIFFICULTY_LOG_RANGE + 1).toDouble())
