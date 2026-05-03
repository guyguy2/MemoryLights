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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val LEGACY_PREFS_NAME = "simon_statistics"

private val Context.statisticsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "statistics",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, LEGACY_PREFS_NAME))
    }
)

/**
 * DataStore-backed statistics manager.
 *
 * Reads are exposed as a Flow so callers stay off the main thread.
 * Writes are fire-and-forget on an IO scope; persistence completes asynchronously.
 * Automatically migrates data from SharedPreferences on first access when constructed via [fromContext].
 */
class StatisticsManager(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    companion object {
        private val KEY_GAMES_PLAYED = intPreferencesKey("games_played")
        private val KEY_TOTAL_SCORE = intPreferencesKey("total_score")
        private val KEY_BEST_STREAK = intPreferencesKey("best_streak")

        fun fromContext(context: Context): StatisticsManager =
            StatisticsManager(context.statisticsDataStore)
    }

    val statisticsFlow: Flow<GameStatistics> = dataStore.data.map { prefs ->
        GameStatistics(
            gamesPlayed = prefs[KEY_GAMES_PLAYED] ?: 0,
            totalScore = prefs[KEY_TOTAL_SCORE] ?: 0,
            bestStreak = prefs[KEY_BEST_STREAK] ?: 0
        )
    }

    fun recordGameResult(score: Int, sequenceLength: Int) {
        scope.launch {
            dataStore.edit { prefs ->
                val gamesPlayed = prefs[KEY_GAMES_PLAYED] ?: 0
                val totalScore = prefs[KEY_TOTAL_SCORE] ?: 0
                val bestStreak = prefs[KEY_BEST_STREAK] ?: 0

                prefs[KEY_GAMES_PLAYED] = gamesPlayed + 1
                prefs[KEY_TOTAL_SCORE] = totalScore + score
                prefs[KEY_BEST_STREAK] = maxOf(bestStreak, sequenceLength)
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
