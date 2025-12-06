package com.happypuppy.memorylights.domain.model

data class GameStatistics(
    val gamesPlayed: Int = 0,
    val highScore: Int = 0,
    val totalScore: Int = 0,
    val bestStreak: Int = 0
) {
    val averageScore: Double
        get() = if (gamesPlayed > 0) totalScore.toDouble() / gamesPlayed else 0.0
}