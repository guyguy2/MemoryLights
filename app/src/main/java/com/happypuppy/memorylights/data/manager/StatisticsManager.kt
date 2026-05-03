package com.happypuppy.memorylights.data.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.happypuppy.memorylights.domain.model.GameStatistics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private const val LEGACY_PREFS_NAME = "simon_statistics"

private val Context.statisticsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "statistics",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, LEGACY_PREFS_NAME))
    }
)

/**
 * DataStore-backed statistics manager.
 * Automatically migrates data from SharedPreferences on first access.
 */
class StatisticsManager(context: Context) {

    companion object {
        private val KEY_GAMES_PLAYED = intPreferencesKey("games_played")
        private val KEY_HIGH_SCORE = intPreferencesKey("high_score")
        private val KEY_TOTAL_SCORE = intPreferencesKey("total_score")
        private val KEY_BEST_STREAK = intPreferencesKey("best_streak")
    }

    private val dataStore = context.statisticsDataStore
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun getPreferences(): Preferences = runBlocking {
        dataStore.data.first()
    }

    fun getStatistics(): GameStatistics {
        val prefs = getPreferences()
        return GameStatistics(
            gamesPlayed = prefs[KEY_GAMES_PLAYED] ?: 0,
            highScore = prefs[KEY_HIGH_SCORE] ?: 0,
            totalScore = prefs[KEY_TOTAL_SCORE] ?: 0,
            bestStreak = prefs[KEY_BEST_STREAK] ?: 0
        )
    }

    fun recordGameResult(score: Int, sequenceLength: Int) {
        val currentStats = getStatistics()

        scope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_GAMES_PLAYED] = currentStats.gamesPlayed + 1
                prefs[KEY_HIGH_SCORE] = maxOf(currentStats.highScore, score)
                prefs[KEY_TOTAL_SCORE] = currentStats.totalScore + score
                prefs[KEY_BEST_STREAK] = maxOf(currentStats.bestStreak, sequenceLength)
            }
        }
    }

    fun resetStatistics() {
        scope.launch {
            dataStore.edit { prefs ->
                prefs.clear()
            }
        }
    }
}