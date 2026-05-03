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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Strongly-typed snapshot of all persisted game settings.
 */
data class AppSettings(
    val soundPack: SoundPack = SoundPack.STANDARD,
    val highScore4Button: Int = 0,
    val highScore6Button: Int = 0,
    val vibrateEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val difficultyEnabled: Boolean = false,
    val memoryLightsPlusEnabled: Boolean = false
)

/**
 * Repository interface for game settings persistence.
 *
 * Reads are exposed as a Flow so callers stay off the main thread.
 * Writes are fire-and-forget on an IO scope; persistence completes asynchronously.
 */
interface SettingsRepository {
    val settingsFlow: Flow<AppSettings>

    fun setSoundPack(pack: SoundPack)
    fun setHighScore4Button(score: Int)
    fun setHighScore6Button(score: Int)
    fun setVibrateEnabled(enabled: Boolean)
    fun setSoundEnabled(enabled: Boolean)
    fun setDifficultyEnabled(enabled: Boolean)
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

    override val settingsFlow: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            soundPack = readSoundPack(prefs),
            highScore4Button = prefs[KEY_HIGH_SCORE_4_BUTTON] ?: 0,
            highScore6Button = prefs[KEY_HIGH_SCORE_6_BUTTON] ?: 0,
            vibrateEnabled = prefs[KEY_VIBRATE_ENABLED] ?: true,
            soundEnabled = prefs[KEY_SOUND_ENABLED] ?: true,
            difficultyEnabled = prefs[KEY_DIFFICULTY_ENABLED] ?: false,
            memoryLightsPlusEnabled = prefs[KEY_MEMORY_LIGHTS_PLUS_ENABLED] ?: false
        )
    }

    private fun readSoundPack(prefs: Preferences): SoundPack {
        val name = prefs[KEY_SOUND_PACK] ?: return SoundPack.STANDARD
        return try {
            SoundPack.valueOf(name)
        } catch (_: IllegalArgumentException) {
            Log.w(TAG, "Invalid sound pack name: $name, defaulting to STANDARD")
            SoundPack.STANDARD
        }
    }

    override fun setSoundPack(pack: SoundPack) = write { it[KEY_SOUND_PACK] = pack.name }
    override fun setHighScore4Button(score: Int) = write { it[KEY_HIGH_SCORE_4_BUTTON] = score }
    override fun setHighScore6Button(score: Int) = write { it[KEY_HIGH_SCORE_6_BUTTON] = score }
    override fun setVibrateEnabled(enabled: Boolean) = write { it[KEY_VIBRATE_ENABLED] = enabled }
    override fun setSoundEnabled(enabled: Boolean) = write { it[KEY_SOUND_ENABLED] = enabled }
    override fun setDifficultyEnabled(enabled: Boolean) = write { it[KEY_DIFFICULTY_ENABLED] = enabled }
    override fun setMemoryLightsPlusEnabled(enabled: Boolean) = write { it[KEY_MEMORY_LIGHTS_PLUS_ENABLED] = enabled }

    private fun write(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        scope.launch {
            dataStore.edit(block)
        }
    }
}
