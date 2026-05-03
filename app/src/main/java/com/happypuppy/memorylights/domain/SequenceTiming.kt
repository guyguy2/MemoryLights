package com.happypuppy.memorylights.domain

/**
 * Pure timing calculator for the Memory Lights sequence display.
 *
 * Returned pair is `(litDurationMs, pauseDurationMs)`.
 *
 * Difficulty curve: when [difficultyEnabled] is true, every 4 levels starting at level 5
 * (i.e. 5, 9, 13, ...) the timing is reduced by [GameConstants.DIFFICULTY_REDUCTION_PERCENT].
 * Levels between thresholds carry the most-recent reduction. Output is clamped to
 * [GameConstants.MIN_LIT_DURATION_MS] / [GameConstants.MIN_PAUSE_DURATION_MS].
 *
 * Pure + side-effect-free so it can be unit tested without standing up a ViewModel.
 */
fun calculateSequenceTiming(level: Int, difficultyEnabled: Boolean): Pair<Long, Long> {
    if (!difficultyEnabled) {
        return GameConstants.BASE_LIT_DURATION_MS to GameConstants.BASE_PAUSE_DURATION_MS
    }

    val speedIncreases = if (level >= 5) (level - 1) / GameConstants.DIFFICULTY_INTERVAL else 0
    val reductionFactor = 1.0 - speedIncreases * GameConstants.DIFFICULTY_REDUCTION_PERCENT

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
