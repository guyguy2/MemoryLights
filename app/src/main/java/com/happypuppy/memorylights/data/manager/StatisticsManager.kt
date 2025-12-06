package com.happypuppy.memorylights.data.manager

import android.content.Context
import android.content.SharedPreferences
import com.happypuppy.memorylights.domain.model.GameStatistics

class StatisticsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "simon_statistics"
        private const val KEY_GAMES_PLAYED = "games_played"
        private const val KEY_HIGH_SCORE = "high_score"
        private const val KEY_TOTAL_SCORE = "total_score"
        private const val KEY_BEST_STREAK = "best_streak"
    }

    fun getStatistics(): GameStatistics {
        return GameStatistics(
            gamesPlayed = prefs.getInt(KEY_GAMES_PLAYED, 0),
            highScore = prefs.getInt(KEY_HIGH_SCORE, 0),
            totalScore = prefs.getInt(KEY_TOTAL_SCORE, 0),
            bestStreak = prefs.getInt(KEY_BEST_STREAK, 0)
        )
    }

    fun recordGameResult(score: Int, sequenceLength: Int) {
        val currentStats = getStatistics()
        
        prefs.edit().apply {
            putInt(KEY_GAMES_PLAYED, currentStats.gamesPlayed + 1)
            putInt(KEY_HIGH_SCORE, maxOf(currentStats.highScore, score))
            putInt(KEY_TOTAL_SCORE, currentStats.totalScore + score)
            putInt(KEY_BEST_STREAK, maxOf(currentStats.bestStreak, sequenceLength))
            apply()
        }
    }

    fun resetStatistics() {
        prefs.edit().clear().apply()
    }
}