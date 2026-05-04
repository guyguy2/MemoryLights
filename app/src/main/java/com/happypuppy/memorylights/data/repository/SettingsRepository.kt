package com.happypuppy.memorylights.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.happypuppy.memorylights.domain.enums.GameMode
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
    val memoryLightsPlusEnabled: Boolean = false,
    val playerTimeoutSeconds: Int = 10,
    val practiceModeEnabled: Boolean = false,
    val reverseModeEnabled: Boolean = false,
    val audioOnlyModeEnabled: Boolean = false,
    val gameMode: GameMode = GameMode.CLASSIC,
    val bestBlitzTime4ButtonMs: Long = 0L,
    val bestBlitzTime6ButtonMs: Long = 0L
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
    fun setPlayerTimeoutSeconds(seconds: Int)
    fun setPracticeModeEnabled(enabled: Boolean)
    fun setReverseModeEnabled(enabled: Boolean)
    fun setAudioOnlyModeEnabled(enabled: Boolean)
    fun setGameMode(mode: GameMode)
    fun setBestBlitzTime4ButtonMs(ms: Long)
    fun setBestBlitzTime6ButtonMs(ms: Long)
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
 * Automatically migrates data from SharedPreferences on first access when constructed via [fromContext].
 */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : SettingsRepository {

    companion object {
        private const val TAG = "SettingsRepository"

        private val KEY_SOUND_PACK = stringPreferencesKey("sound_pack")
        private val KEY_HIGH_SCORE_4_BUTTON = intPreferencesKey("high_score_4_button")
        private val KEY_HIGH_SCORE_6_BUTTON = intPreferencesKey("high_score_6_button")
        private val KEY_VIBRATE_ENABLED = booleanPreferencesKey("vibrate_enabled")
        private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val KEY_DIFFICULTY_ENABLED = booleanPreferencesKey("difficulty_enabled")
        private val KEY_MEMORY_LIGHTS_PLUS_ENABLED = booleanPreferencesKey("memory_lights_plus_enabled")
        private val KEY_PLAYER_TIMEOUT_SECONDS = intPreferencesKey("player_timeout_seconds")
        private val KEY_PRACTICE_MODE_ENABLED = booleanPreferencesKey("practice_mode_enabled")
        private val KEY_REVERSE_MODE_ENABLED = booleanPreferencesKey("reverse_mode_enabled")
        private val KEY_AUDIO_ONLY_MODE_ENABLED = booleanPreferencesKey("audio_only_mode_enabled")
        private val KEY_GAME_MODE = stringPreferencesKey("game_mode")
        private val KEY_BEST_BLITZ_TIME_4_BUTTON_MS = longPreferencesKey("best_blitz_time_4_button_ms")
        private val KEY_BEST_BLITZ_TIME_6_BUTTON_MS = longPreferencesKey("best_blitz_time_6_button_ms")

        fun fromContext(context: Context): DataStoreSettingsRepository =
            DataStoreSettingsRepository(context.settingsDataStore)
    }

    override val settingsFlow: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            soundPack = readSoundPack(prefs),
            highScore4Button = prefs[KEY_HIGH_SCORE_4_BUTTON] ?: 0,
            highScore6Button = prefs[KEY_HIGH_SCORE_6_BUTTON] ?: 0,
            vibrateEnabled = prefs[KEY_VIBRATE_ENABLED] ?: true,
            soundEnabled = prefs[KEY_SOUND_ENABLED] ?: true,
            difficultyEnabled = prefs[KEY_DIFFICULTY_ENABLED] ?: false,
            memoryLightsPlusEnabled = prefs[KEY_MEMORY_LIGHTS_PLUS_ENABLED] ?: false,
            playerTimeoutSeconds = prefs[KEY_PLAYER_TIMEOUT_SECONDS] ?: 10,
            practiceModeEnabled = prefs[KEY_PRACTICE_MODE_ENABLED] ?: false,
            reverseModeEnabled = prefs[KEY_REVERSE_MODE_ENABLED] ?: false,
            audioOnlyModeEnabled = prefs[KEY_AUDIO_ONLY_MODE_ENABLED] ?: false,
            gameMode = readGameMode(prefs),
            bestBlitzTime4ButtonMs = prefs[KEY_BEST_BLITZ_TIME_4_BUTTON_MS] ?: 0L,
            bestBlitzTime6ButtonMs = prefs[KEY_BEST_BLITZ_TIME_6_BUTTON_MS] ?: 0L
        )
    }

    private fun readGameMode(prefs: Preferences): GameMode {
        val name = prefs[KEY_GAME_MODE] ?: return GameMode.CLASSIC
        return try {
            GameMode.valueOf(name)
        } catch (_: IllegalArgumentException) {
            Log.w(TAG, "Invalid game mode name: $name, defaulting to CLASSIC")
            GameMode.CLASSIC
        }
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
    override fun setPlayerTimeoutSeconds(seconds: Int) = write { it[KEY_PLAYER_TIMEOUT_SECONDS] = seconds }
    override fun setPracticeModeEnabled(enabled: Boolean) = write { it[KEY_PRACTICE_MODE_ENABLED] = enabled }
    override fun setReverseModeEnabled(enabled: Boolean) = write { it[KEY_REVERSE_MODE_ENABLED] = enabled }
    override fun setAudioOnlyModeEnabled(enabled: Boolean) = write { it[KEY_AUDIO_ONLY_MODE_ENABLED] = enabled }
    override fun setGameMode(mode: GameMode) = write { it[KEY_GAME_MODE] = mode.name }
    override fun setBestBlitzTime4ButtonMs(ms: Long) = write { it[KEY_BEST_BLITZ_TIME_4_BUTTON_MS] = ms }
    override fun setBestBlitzTime6ButtonMs(ms: Long) = write { it[KEY_BEST_BLITZ_TIME_6_BUTTON_MS] = ms }

    private fun write(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        scope.launch {
            dataStore.edit(block)
        }
    }
}
