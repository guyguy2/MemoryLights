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
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.happypuppy.memorylights.domain.enums.GameMode
import com.happypuppy.memorylights.domain.enums.SoundPack
import com.happypuppy.memorylights.domain.enums.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    val difficultyEnabled: Boolean = true,
    val memoryLightsPlusEnabled: Boolean = false,
    val playerTimeoutSeconds: Int = 10,
    val practiceModeEnabled: Boolean = false,
    val reverseModeEnabled: Boolean = false,
    val audioOnlyModeEnabled: Boolean = false,
    /** #35: geometric shape overlays on buttons/arcs. Default off preserves classic look. */
    val colorBlindModeEnabled: Boolean = false,
    val gameMode: GameMode = GameMode.CLASSIC,
    val bestBlitzTime4ButtonMs: Long = 0L,
    val bestBlitzTime6ButtonMs: Long = 0L,
    val dailyChallengeEnabled: Boolean = false,
    /** Epoch day (`LocalDate.toEpochDay()`) of the last daily run completed; 0 = never. */
    val dailyCompletedEpochDay: Long = 0L,
    /** Best level reached on `dailyCompletedEpochDay`'s daily run. Resets when the day rolls over. */
    val dailyBestLevel: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /**
     * F8: persisted set of unlocked achievement IDs (matching `Achievement.name`).
     * Stored as a string set so the data layer never has to import the
     * domain-facing enum. Unknown IDs in this set are silently ignored at read
     * time (forward-compat for removed achievements).
     */
    val unlockedAchievements: Set<String> = emptySet()
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
    fun setColorBlindModeEnabled(enabled: Boolean)
    fun setGameMode(mode: GameMode)
    fun setBestBlitzTime4ButtonMs(ms: Long)
    fun setBestBlitzTime6ButtonMs(ms: Long)
    fun setDailyChallengeEnabled(enabled: Boolean)
    fun setDailyCompletion(epochDay: Long, bestLevel: Int)
    fun setThemeMode(mode: ThemeMode)
    fun setUnlockedAchievements(ids: Set<String>)

    /**
     * Cancels any background scope the repository owns. Implementations that
     * use an injected scope (e.g. tests) must leave the caller's scope alone.
     */
    fun close()
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
 *
 * Scope ownership: when constructed without an explicit [scope] argument, the repo
 * owns the internal `Dispatchers.IO` scope and [close] will cancel it. When a scope
 * is injected (e.g. tests passing a `TestScope`), the caller retains ownership and
 * [close] is a no-op for that scope; the caller is responsible for cancellation.
 */
class DataStoreSettingsRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope,
    private val ownsScope: Boolean
) : SettingsRepository {

    /**
     * Test-friendly constructor. Caller retains ownership of the supplied [scope]
     * and is responsible for cancelling it.
     */
    constructor(
        dataStore: DataStore<Preferences>,
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    ) : this(dataStore, scope, ownsScope = false)

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
        private val KEY_COLOR_BLIND_MODE_ENABLED = booleanPreferencesKey("color_blind_mode_enabled")
        private val KEY_GAME_MODE = stringPreferencesKey("game_mode")
        private val KEY_BEST_BLITZ_TIME_4_BUTTON_MS = longPreferencesKey("best_blitz_time_4_button_ms")
        private val KEY_BEST_BLITZ_TIME_6_BUTTON_MS = longPreferencesKey("best_blitz_time_6_button_ms")
        private val KEY_DAILY_CHALLENGE_ENABLED = booleanPreferencesKey("daily_challenge_enabled")
        private val KEY_DAILY_COMPLETED_EPOCH_DAY = longPreferencesKey("daily_completed_epoch_day")
        private val KEY_DAILY_BEST_LEVEL = intPreferencesKey("daily_best_level")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_UNLOCKED_ACHIEVEMENTS = stringSetPreferencesKey("unlocked_achievements")

        fun fromContext(context: Context): DataStoreSettingsRepository =
            DataStoreSettingsRepository(
                dataStore = context.settingsDataStore,
                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
                ownsScope = true
            )
    }

    override val settingsFlow: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            soundPack = readSoundPack(prefs),
            highScore4Button = prefs[KEY_HIGH_SCORE_4_BUTTON] ?: 0,
            highScore6Button = prefs[KEY_HIGH_SCORE_6_BUTTON] ?: 0,
            vibrateEnabled = prefs[KEY_VIBRATE_ENABLED] ?: true,
            soundEnabled = prefs[KEY_SOUND_ENABLED] ?: true,
            difficultyEnabled = prefs[KEY_DIFFICULTY_ENABLED] ?: true,
            memoryLightsPlusEnabled = prefs[KEY_MEMORY_LIGHTS_PLUS_ENABLED] ?: false,
            playerTimeoutSeconds = prefs[KEY_PLAYER_TIMEOUT_SECONDS] ?: 10,
            practiceModeEnabled = prefs[KEY_PRACTICE_MODE_ENABLED] ?: false,
            reverseModeEnabled = prefs[KEY_REVERSE_MODE_ENABLED] ?: false,
            audioOnlyModeEnabled = prefs[KEY_AUDIO_ONLY_MODE_ENABLED] ?: false,
            colorBlindModeEnabled = prefs[KEY_COLOR_BLIND_MODE_ENABLED] ?: false,
            gameMode = readGameMode(prefs),
            bestBlitzTime4ButtonMs = prefs[KEY_BEST_BLITZ_TIME_4_BUTTON_MS] ?: 0L,
            bestBlitzTime6ButtonMs = prefs[KEY_BEST_BLITZ_TIME_6_BUTTON_MS] ?: 0L,
            dailyChallengeEnabled = prefs[KEY_DAILY_CHALLENGE_ENABLED] ?: false,
            dailyCompletedEpochDay = prefs[KEY_DAILY_COMPLETED_EPOCH_DAY] ?: 0L,
            dailyBestLevel = prefs[KEY_DAILY_BEST_LEVEL] ?: 0,
            themeMode = readThemeMode(prefs),
            unlockedAchievements = prefs[KEY_UNLOCKED_ACHIEVEMENTS] ?: emptySet()
        )
    }

    private fun readThemeMode(prefs: Preferences): ThemeMode {
        val name = prefs[KEY_THEME_MODE] ?: return ThemeMode.SYSTEM
        return try {
            ThemeMode.valueOf(name)
        } catch (_: IllegalArgumentException) {
            Log.w(TAG, "Invalid theme mode name: $name, defaulting to SYSTEM")
            ThemeMode.SYSTEM
        }
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

    /** Availability predicate for SoundPack. Indirected so tests can verify fallback behavior. */
    internal var isSoundPackAvailable: (SoundPack) -> Boolean = { it.isAvailable }

    private fun readSoundPack(prefs: Preferences): SoundPack {
        val name = prefs[KEY_SOUND_PACK] ?: return SoundPack.STANDARD
        return try {
            val pack = SoundPack.valueOf(name)
            if (isSoundPackAvailable(pack)) {
                pack
            } else {
                Log.w(TAG, "Sound pack $name is unavailable, defaulting to STANDARD")
                SoundPack.STANDARD
            }
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
    override fun setColorBlindModeEnabled(enabled: Boolean) = write { it[KEY_COLOR_BLIND_MODE_ENABLED] = enabled }
    override fun setGameMode(mode: GameMode) = write { it[KEY_GAME_MODE] = mode.name }
    override fun setBestBlitzTime4ButtonMs(ms: Long) = write { it[KEY_BEST_BLITZ_TIME_4_BUTTON_MS] = ms }
    override fun setBestBlitzTime6ButtonMs(ms: Long) = write { it[KEY_BEST_BLITZ_TIME_6_BUTTON_MS] = ms }
    override fun setDailyChallengeEnabled(enabled: Boolean) = write { it[KEY_DAILY_CHALLENGE_ENABLED] = enabled }
    override fun setDailyCompletion(epochDay: Long, bestLevel: Int) = write {
        it[KEY_DAILY_COMPLETED_EPOCH_DAY] = epochDay
        it[KEY_DAILY_BEST_LEVEL] = bestLevel
    }
    override fun setThemeMode(mode: ThemeMode) = write { it[KEY_THEME_MODE] = mode.name }
    override fun setUnlockedAchievements(ids: Set<String>) = write {
        // Defensive copy: DataStore stores its own Set, but copying here keeps the
        // repo independent of the caller's collection lifetime.
        it[KEY_UNLOCKED_ACHIEVEMENTS] = ids.toSet()
    }

    private fun write(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        scope.launch {
            dataStore.edit(block)
        }
    }

    override fun close() {
        if (ownsScope) {
            scope.cancel()
        }
    }
}
