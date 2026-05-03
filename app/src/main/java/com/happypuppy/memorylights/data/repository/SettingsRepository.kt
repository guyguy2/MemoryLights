package com.happypuppy.memorylights.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.happypuppy.memorylights.domain.enums.SoundPack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Repository interface for game settings persistence.
 */
interface SettingsRepository {
    fun getSoundPack(): SoundPack
    fun setSoundPack(pack: SoundPack)

    fun getHighScore4Button(): Int
    fun setHighScore4Button(score: Int)

    fun getHighScore6Button(): Int
    fun setHighScore6Button(score: Int)

    fun isVibrateEnabled(): Boolean
    fun setVibrateEnabled(enabled: Boolean)

    fun isSoundEnabled(): Boolean
    fun setSoundEnabled(enabled: Boolean)

    fun isDifficultyEnabled(): Boolean
    fun setDifficultyEnabled(enabled: Boolean)

    fun isMemoryLightsPlusEnabled(): Boolean
    fun setMemoryLightsPlusEnabled(enabled: Boolean)
}

private const val LEGACY_PREFS_NAME = "simon_game_prefs"

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, LEGACY_PREFS_NAME))
    }
)

/**
 * DataStore implementation of SettingsRepository.
 * Automatically migrates data from SharedPreferences on first access.
 */
class DataStoreSettingsRepository(context: Context) : SettingsRepository {

    companion object {
        private const val TAG = "SettingsRepository"

        private val KEY_SOUND_PACK = stringPreferencesKey("sound_pack")
        private val KEY_HIGH_SCORE_4_BUTTON = intPreferencesKey("high_score_4_button")
        private val KEY_HIGH_SCORE_6_BUTTON = intPreferencesKey("high_score_6_button")
        private val KEY_VIBRATE_ENABLED = booleanPreferencesKey("vibrate_enabled")
        private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val KEY_DIFFICULTY_ENABLED = booleanPreferencesKey("difficulty_enabled")
        private val KEY_MEMORY_LIGHTS_PLUS_ENABLED = booleanPreferencesKey("memory_lights_plus_enabled")
    }

    private val dataStore = context.settingsDataStore
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun getPreferences(): Preferences = runBlocking {
        dataStore.data.first()
    }

    private fun updatePreference(block: suspend (Preferences) -> Preferences) {
        scope.launch {
            dataStore.edit { prefs ->
                block(prefs)
            }
        }
    }

    override fun getSoundPack(): SoundPack {
        val name = getPreferences()[KEY_SOUND_PACK]
        return try {
            SoundPack.valueOf(name ?: SoundPack.STANDARD.name)
        } catch (e: Exception) {
            Log.w(TAG, "Invalid sound pack name: $name, defaulting to STANDARD")
            SoundPack.STANDARD
        }
    }

    override fun setSoundPack(pack: SoundPack) {
        scope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_SOUND_PACK] = pack.name
            }
        }
        Log.d(TAG, "Sound pack set to: ${pack.name}")
    }

    override fun getHighScore4Button(): Int {
        return getPreferences()[KEY_HIGH_SCORE_4_BUTTON] ?: 0
    }

    override fun setHighScore4Button(score: Int) {
        scope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_HIGH_SCORE_4_BUTTON] = score
            }
        }
        Log.d(TAG, "High score 4-button set to: $score")
    }

    override fun getHighScore6Button(): Int {
        return getPreferences()[KEY_HIGH_SCORE_6_BUTTON] ?: 0
    }

    override fun setHighScore6Button(score: Int) {
        scope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_HIGH_SCORE_6_BUTTON] = score
            }
        }
        Log.d(TAG, "High score 6-button set to: $score")
    }

    override fun isVibrateEnabled(): Boolean {
        return getPreferences()[KEY_VIBRATE_ENABLED] ?: true
    }

    override fun setVibrateEnabled(enabled: Boolean) {
        scope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_VIBRATE_ENABLED] = enabled
            }
        }
        Log.d(TAG, "Vibrate enabled set to: $enabled")
    }

    override fun isSoundEnabled(): Boolean {
        return getPreferences()[KEY_SOUND_ENABLED] ?: true
    }

    override fun setSoundEnabled(enabled: Boolean) {
        scope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_SOUND_ENABLED] = enabled
            }
        }
        Log.d(TAG, "Sound enabled set to: $enabled")
    }

    override fun isDifficultyEnabled(): Boolean {
        return getPreferences()[KEY_DIFFICULTY_ENABLED] ?: false
    }

    override fun setDifficultyEnabled(enabled: Boolean) {
        scope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_DIFFICULTY_ENABLED] = enabled
            }
        }
        Log.d(TAG, "Difficulty enabled set to: $enabled")
    }

    override fun isMemoryLightsPlusEnabled(): Boolean {
        return getPreferences()[KEY_MEMORY_LIGHTS_PLUS_ENABLED] ?: false
    }

    override fun setMemoryLightsPlusEnabled(enabled: Boolean) {
        scope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_MEMORY_LIGHTS_PLUS_ENABLED] = enabled
            }
        }
        Log.d(TAG, "Memory Lights+ enabled set to: $enabled")
    }
}
