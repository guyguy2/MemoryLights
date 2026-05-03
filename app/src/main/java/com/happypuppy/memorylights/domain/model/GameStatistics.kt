package com.happypuppy.memorylights.domain.model

/**
 * Aggregated game-history counters. The per-mode high score is owned by
 * [com.happypuppy.memorylights.data.repository.SettingsRepository] (4-button vs
 * 6-button) — this class deliberately does not duplicate it.
 */
data class GameStatistics(
    val gamesPlayed: Int = 0,
    val totalScore: Int = 0,
    val bestStreak: Int = 0
) {
    val averageScore: Double
        get() = if (gamesPlayed > 0) totalScore.toDouble() / gamesPlayed else 0.0
}