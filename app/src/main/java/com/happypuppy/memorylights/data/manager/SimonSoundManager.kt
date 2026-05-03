package com.happypuppy.memorylights.data.manager

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.happypuppy.memorylights.R
import com.happypuppy.memorylights.domain.GameConstants
import com.happypuppy.memorylights.domain.enums.SimonButton
import com.happypuppy.memorylights.domain.enums.SoundPack
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages sound effects for the Memory Lights game with support for multiple sound packs.
 * Features:
 * - Lazy loading: Only loads current sound pack at startup, others on demand
 * - Memory-aware: Releases unused sound packs when memory is low
 * - Audio focus: Properly handles audio focus for integration with other apps
 * - Graceful degradation: Falls back to haptic feedback when sounds fail
 * - Volume control: Supports master volume adjustment
 */
class SimonSoundManager(private val context: Context) : ComponentCallbacks2 {

    companion object {
        private const val TAG = "SimonSoundManager"
    }

    private val soundPool: SoundPool
    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    // Audio focus handling
    private var hasAudioFocus = false
    private val audioFocusRequest: AudioFocusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()
    }
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "Audio focus lost permanently")
                hasAudioFocus = false
                pauseSounds()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "Audio focus lost transiently")
                hasAudioFocus = false
                pauseSounds()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "Audio focus lost - ducking")
                // Lower volume instead of pausing for short interruptions
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus gained")
                hasAudioFocus = true
                resumeSounds()
            }
        }
    }

    // Audio attributes for game sounds
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    // Flag to track if sounds are paused when app is in background
    private var isPaused = false

    // Flag to track if sounds are muted
    private var isMuted = false

    // Master volume control (0.0 to 1.0)
    private var masterVolume = GameConstants.DEFAULT_MASTER_VOLUME

    // Vibration settings and control
    private var vibrateEnabled = true
    private val vibrator: Vibrator by lazy {
        Log.d(TAG, "Initializing vibrator")
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    }

    // Maps to store sound IDs for different sound packs
    private val soundPackMap = mutableMapOf<SoundPack, Map<SimonButton, Int>>()
    private val errorSoundMap = mutableMapOf<SoundPack, Int>()

    // Track which sound packs are currently loaded
    private val loadedSoundPacks = mutableSetOf<SoundPack>()

    // Currently active sound pack
    private var currentSoundPack = SoundPack.STANDARD

    // Track load status
    private val loadStatusMap = ConcurrentHashMap<Int, Boolean>()

    // Track currently playing sound streams to be able to stop them
    private var activeStreamId: Int = 0

    // Callback for sound loading completion
    private var onSoundsLoadedCallback: ((Boolean, String?) -> Unit)? = null

    // Track total sounds to load and loaded count for current loading operation
    private var totalSoundsToLoad = 0
    private var soundsLoadedCount = 0
    private var loadError: String? = null

    // Track if initial loading is complete
    private var initialLoadComplete = false

    init {
        Log.d(TAG, "Initializing SimonSoundManager")

        // Register for memory callbacks
        context.applicationContext.registerComponentCallbacks(this)

        // List all raw resources to verify what's available
        listAllRawResources()

        // Create a SoundPool optimized for short game sounds
        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        // Set up load listener to track sound loading status
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            val success = status == 0
            loadStatusMap[sampleId] = success
            Log.d(TAG, "Sound loaded: ID=$sampleId, Success=$success")

            soundsLoadedCount++
            if (!success) {
                loadError = "Failed to load sound ID: $sampleId"
            }

            // Check if all sounds are loaded for current operation
            if (soundsLoadedCount >= totalSoundsToLoad && totalSoundsToLoad > 0) {
                Log.d(TAG, "Sound pack loaded: $soundsLoadedCount/$totalSoundsToLoad, error=$loadError")
                if (!initialLoadComplete) {
                    initialLoadComplete = true
                    onSoundsLoadedCallback?.invoke(loadError == null, loadError)
                }
            }
        }

        // Lazy loading: Only load the default/current sound pack at startup
        loadCurrentSoundPackOnly()
    }

    /**
     * Set a callback to be notified when all sounds are loaded
     * @param callback Called with (success, errorMessage) when loading completes
     */
    fun setOnSoundsLoadedListener(callback: (Boolean, String?) -> Unit) {
        onSoundsLoadedCallback = callback
        // If sounds are already loaded, call immediately
        if (soundsLoadedCount >= totalSoundsToLoad && totalSoundsToLoad > 0) {
            callback(loadError == null, loadError)
        }
    }

    /**
     * Check if all sounds are loaded
     */
    fun areSoundsLoaded(): Boolean {
        return soundsLoadedCount >= totalSoundsToLoad && totalSoundsToLoad > 0
    }

    /**
     * Get sound loading error message if any
     */
    fun getLoadError(): String? {
        return loadError
    }

    /**
     * Get the context
     * Used by ViewModel to access application context
     */
    fun getContext(): Context {
        return context.applicationContext
    }

    /**
     * List all raw resources to help debug resource loading issues
     */
    private fun listAllRawResources() {
        try {
            Log.d(TAG, "=== LISTING ALL RAW RESOURCES ===")
            val fields = R.raw::class.java.fields
            if (fields.isEmpty()) {
                Log.e(TAG, "No raw resources found in R.raw!")
            } else {
                for (field in fields) {
                    val resourceName = field.name
                    val resourceId = field.getInt(null)
                    Log.d(TAG, "Raw resource: $resourceName (ID: $resourceId)")
                }
            }
            Log.d(TAG, "=== END OF RAW RESOURCES LIST ===")
        } catch (e: Exception) {
            Log.e(TAG, "Error listing raw resources", e)
        }
    }

    /**
     * Load only the current sound pack at startup (lazy loading)
     */
    private fun loadCurrentSoundPackOnly() {
        try {
            Log.d(TAG, "Lazy loading: Loading only current sound pack: ${currentSoundPack.name}")

            // Calculate total sounds to load for just one pack: buttons + error
            totalSoundsToLoad = SimonButton.entries.size + 1
            soundsLoadedCount = 0
            loadError = null
            Log.d(TAG, "Total sounds to load: $totalSoundsToLoad")

            // Load only the current sound pack
            loadSoundPack(currentSoundPack)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading sounds", e)
            loadError = e.message
        }
    }

    /**
     * Ensure a sound pack is loaded, loading it on-demand if necessary
     * @return true if the sound pack is ready to use
     */
    private fun ensureSoundPackLoaded(soundPack: SoundPack): Boolean {
        if (loadedSoundPacks.contains(soundPack)) {
            return true
        }

        Log.d(TAG, "On-demand loading sound pack: ${soundPack.name}")

        // Check if this pack shares resources with an already loaded pack
        val existingPack = loadedSoundPacks.find { it.resourcePrefix == soundPack.resourcePrefix }
        if (existingPack != null) {
            Log.d(TAG, "Sound pack ${soundPack.name} uses same resources as ${existingPack.name}, reusing")
            soundPackMap[soundPack] = soundPackMap[existingPack] ?: emptyMap()
            errorSoundMap[soundPack] = errorSoundMap[existingPack] ?: 0
            loadedSoundPacks.add(soundPack)
            return true
        }

        // Load the sound pack
        loadSoundPack(soundPack)

        // Note: Loading is asynchronous, so we return false and the caller should retry
        // For simplicity in this implementation, we load synchronously by returning after adding to set
        return loadedSoundPacks.contains(soundPack)
    }

    /**
     * Load sounds for a specific sound pack
     */
    private fun loadSoundPack(soundPack: SoundPack) {
        val prefix = soundPack.resourcePrefix
        Log.d(TAG, "Loading sound pack: ${soundPack.name} with prefix: $prefix")

        try {
            // Map to store button sounds for this pack
            val buttonSoundMap = mutableMapOf<SimonButton, Int>()

            // Load sound for each button using the proper prefix
            SimonButton.entries.forEach { button ->
                val resourceName = "${prefix}_${button.name.lowercase()}_tone"
                Log.d(TAG, "🔄 Loading sound for ${button.name}: $resourceName")

                val resourceId = getResourceId(resourceName)

                if (resourceId != 0) {
                    val soundId = soundPool.load(context, resourceId, 1)
                    buttonSoundMap[button] = soundId
                    Log.d(TAG, "✅ Successfully loaded $resourceName sound: ID=$soundId")
                    
                    // Extra logging for PURPLE and ORANGE buttons
                    if (button == SimonButton.PURPLE || button == SimonButton.ORANGE) {
                        Log.i(TAG, "🟣🟠 SPECIAL LOAD: ${button.name} -> ResourceID=$resourceId, SoundID=$soundId")
                    }
                } else {
                    Log.e(TAG, "❌ Resource not found: $resourceName")

                    // Try to debug why resource wasn't found
                    debugResourceNotFound(resourceName)
                    
                    // For PURPLE and ORANGE buttons, try to load a fallback resource
                    if (button == SimonButton.PURPLE || button == SimonButton.ORANGE) {
                        Log.w(TAG, "🔧 FALLBACK LOAD: Trying to load GREEN sound for ${button.name}")
                        val fallbackResourceName = "${prefix}_green_tone"
                        val fallbackResourceId = getResourceId(fallbackResourceName)
                        if (fallbackResourceId != 0) {
                            val fallbackSoundId = soundPool.load(context, fallbackResourceId, 1)
                            buttonSoundMap[button] = fallbackSoundId
                            Log.w(TAG, "⚠️ Loaded fallback sound for ${button.name}: ID=$fallbackSoundId")
                        }
                    }
                }
            }

            // Load error sound for this pack
            val errorResourceName = "${prefix}_error_tone"
            Log.d(TAG, "Looking for error resource: $errorResourceName")

            val errorResourceId = getResourceId(errorResourceName)

            if (errorResourceId != 0) {
                val errorSoundId = soundPool.load(context, errorResourceId, 1)
                errorSoundMap[soundPack] = errorSoundId
                Log.d(TAG, "✓ Loaded $errorResourceName sound: ID=$errorSoundId")
            } else {
                Log.e(TAG, "✗ Error resource not found: $errorResourceName")

                // Try to debug why resource wasn't found
                debugResourceNotFound(errorResourceName)
            }

            // Add the button sound map to the pack map
            soundPackMap[soundPack] = buttonSoundMap

            // Mark this sound pack as loaded
            loadedSoundPacks.add(soundPack)

            // Log summary of loaded sounds for this pack
            logSoundPackSummary(soundPack, buttonSoundMap)

        } catch (e: Exception) {
            Log.e(TAG, "Error loading $prefix sounds", e)
            e.printStackTrace()
        }
    }

    /**
     * Log summary of loaded sounds for a sound pack
     */
    private fun logSoundPackSummary(soundPack: SoundPack, buttonSoundMap: Map<SimonButton, Int>) {
        Log.d(TAG, "=== ${soundPack.name} SOUND PACK SUMMARY ===")
        Log.d(TAG, "Buttons loaded: ${buttonSoundMap.size}/${SimonButton.entries.size}")
        buttonSoundMap.forEach { (button, soundId) ->
            Log.d(TAG, "  - ${button.name}: SoundID=$soundId")
        }
        val errorSoundId = errorSoundMap[soundPack]
        if (errorSoundId != null) {
            Log.d(TAG, "  - ERROR: SoundID=$errorSoundId")
        } else {
            Log.d(TAG, "  - ERROR: Not loaded")
        }
        Log.d(TAG, "===================================")
    }

    /**
     * Try to debug why a resource wasn't found
     */
    private fun debugResourceNotFound(resourceName: String) {
        Log.e(TAG, "Debug - Attempting to find similar resources:")

        // Check for resources with similar names
        val fields = R.raw::class.java.fields
        var foundSimilar = false

        for (field in fields) {
            val name = field.name
            if (name.contains(resourceName) || resourceName.contains(name)) {
                foundSimilar = true
                Log.e(TAG, "  - Similar resource found: $name")
            }
        }

        if (!foundSimilar) {
            Log.e(TAG, "  - No similar resources found. Check file names in res/raw folder.")
        }

        // Extract prefix information from the resource name
        val parts = resourceName.split("_")
        if (parts.isNotEmpty()) {
            val extractedPrefix = parts[0]
            Log.e(TAG, "  - Resource prefix from name: $extractedPrefix")
            Log.e(TAG, "  - Check if this matches any SoundPack.resourcePrefix value")

            // Log all available resource prefixes for comparison
            SoundPack.entries.forEach { pack ->
                Log.e(TAG, "  - Available prefix: '${pack.resourcePrefix}' from ${pack.name}")
            }
        }

        Log.e(TAG, "  - Ensure your file is named exactly: $resourceName.wav in res/raw folder")
    }

    /**
     * Get the resource ID using direct references instead of dynamic lookup
     */
    private fun getResourceId(resourceName: String): Int {
        Log.d(TAG, "🔍 Looking up resource ID for: $resourceName")
        
        // Use direct resource references to avoid reflection
        val resourceId = when (resourceName) {
            // Standard sound pack - all 6 buttons
            "standard_green_tone" -> R.raw.standard_green_tone
            "standard_red_tone" -> R.raw.standard_red_tone
            "standard_yellow_tone" -> R.raw.standard_yellow_tone
            "standard_blue_tone" -> R.raw.standard_blue_tone
            "standard_purple_tone" -> R.raw.standard_purple_tone
            "standard_orange_tone" -> R.raw.standard_orange_tone
            "standard_error_tone" -> R.raw.standard_error_tone

            // Funny sound pack - all 6 buttons
            "funny_green_tone" -> R.raw.funny_green_tone
            "funny_red_tone" -> R.raw.funny_red_tone
            "funny_yellow_tone" -> R.raw.funny_yellow_tone
            "funny_blue_tone" -> R.raw.funny_blue_tone
            "funny_purple_tone" -> R.raw.funny_purple_tone
            "funny_orange_tone" -> R.raw.funny_orange_tone
            "funny_error_tone" -> R.raw.funny_error_tone

            // Electronic sound pack - all 6 buttons
            "electronic_green_tone" -> R.raw.electronic_green_tone
            "electronic_red_tone" -> R.raw.electronic_red_tone
            "electronic_yellow_tone" -> R.raw.electronic_yellow_tone
            "electronic_blue_tone" -> R.raw.electronic_blue_tone
            "electronic_purple_tone" -> R.raw.electronic_purple_tone
            "electronic_orange_tone" -> R.raw.electronic_orange_tone
            "electronic_error_tone" -> R.raw.electronic_error_tone

            // Retro sound pack - all 6 buttons
            "retro_green_tone" -> R.raw.retro_green_tone
            "retro_red_tone" -> R.raw.retro_red_tone
            "retro_yellow_tone" -> R.raw.retro_yellow_tone
            "retro_blue_tone" -> R.raw.retro_blue_tone
            "retro_purple_tone" -> R.raw.retro_purple_tone
            "retro_orange_tone" -> R.raw.retro_orange_tone
            "retro_error_tone" -> R.raw.retro_error_tone

            // For all other resources, return 0 (not found)
            else -> {
                Log.w(TAG, "Resource not found in direct mapping: $resourceName")
                0
            }
        }
        
        Log.d(TAG, "🔍 Resource ID lookup result: $resourceName -> $resourceId")
        return resourceId
    }

    /**
     * Request audio focus before playing sounds
     * @return true if audio focus was granted
     */
    fun requestAudioFocus(): Boolean {
        val result = audioManager.requestAudioFocus(audioFocusRequest)
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        Log.d(TAG, "Audio focus request result: $result, hasAudioFocus=$hasAudioFocus")
        return hasAudioFocus
    }

    /**
     * Abandon audio focus when done playing
     */
    fun abandonAudioFocus() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
        hasAudioFocus = false
        Log.d(TAG, "Audio focus abandoned")
    }

    /**
     * Pause all sounds
     * Called when app goes to background
     */
    fun pauseSounds() {
        Log.d(TAG, "Pausing all sounds")
        isPaused = true

        // Stop any currently playing sounds
        stopAllSounds()

        // Auto-pause SoundPool
        soundPool.autoPause()

        // Abandon audio focus
        abandonAudioFocus()
    }

    /**
     * Resume sounds
     * Called when app returns to foreground
     */
    fun resumeSounds() {
        Log.d(TAG, "Resuming sounds")
        isPaused = false

        // Auto-resume SoundPool
        soundPool.autoResume()

        // Request audio focus
        requestAudioFocus()
    }

    /**
     * Change the active sound pack
     * This method ensures the new pack is loaded on-demand if necessary
     */
    fun setSoundPack(soundPack: SoundPack) {
        Log.d(TAG, "Changing sound pack to: ${soundPack.name}")

        // Ensure the sound pack is loaded before switching
        ensureSoundPackLoaded(soundPack)

        currentSoundPack = soundPack
    }

    /**
     * Set whether vibration is enabled
     */
    fun setVibrationEnabled(enabled: Boolean) {
        Log.d(TAG, "Setting vibration enabled: $enabled")
        vibrateEnabled = enabled

        // Test vibration immediately if enabling (only if not paused)
        if (enabled && !isPaused) {
            try {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Log.d(TAG, "Testing vibration after enabling")
                    vibrate()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to test vibration", e)
            }
        }
    }

    /**
     * Get current vibration setting
     */
    fun isVibrationEnabled(): Boolean {
        return vibrateEnabled
    }

    /**
     * Check if the vibrator can actually vibrate
     */
    private fun hasVibrator(): Boolean {
        return vibrator.hasVibrator()
    }

    /**
     * Trigger a short vibration for button press
     */
    private fun vibrate() {
        if (!vibrateEnabled || isPaused) {
            Log.d(TAG, "Vibration disabled or app paused, skipping")
            return
        }

        if (!hasVibrator()) {
            Log.d(TAG, "Device does not have vibration capability")
            return
        }

        try {
            Log.d(TAG, "Triggering vibration")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(effect)
                Log.d(TAG, "Vibrated using modern API")
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
                Log.d(TAG, "Vibrated using legacy API")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate", e)
            e.printStackTrace()
        }
    }

    /**
     * Set whether sounds are muted
     * 
     * @param muted True to mute sounds, false to unmute
     */
    fun setSoundMuted(muted: Boolean) {
        Log.d(TAG, "Setting sound muted: $muted")
        isMuted = muted
    }
    
    /**
     * Get whether sounds are currently muted
     */
    fun isSoundMuted(): Boolean {
        return isMuted
    }
    
    /**
     * Play the sound associated with a specific Simon button using the current sound pack.
     * Implements graceful degradation - if sound fails, haptic feedback is still provided.
     *
     * @param button The button to play sound for
     * @param isPlayerPressed Whether this sound is from a player pressing a button (for vibration)
     */
    fun playSound(button: SimonButton, isPlayerPressed: Boolean = false) {
        Log.d(TAG, "🔊 Request to play sound for button: $button with sound pack: ${currentSoundPack.name}, player pressed: $isPlayerPressed")

        // Handle vibration first, independently from sound state (graceful degradation)
        if (isPlayerPressed) {
            Log.d(TAG, "📳 Player pressed button, triggering vibration")
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                vibrate()
            }
        }

        // Don't play sound if paused or muted, but vibration was already handled above
        if (isPaused || isMuted) {
            Log.d(TAG, "Sounds are ${if (isPaused) "paused" else "muted"}, not playing sound (vibration was handled separately)")
            return
        }

        // Ensure current sound pack is loaded
        if (!loadedSoundPacks.contains(currentSoundPack)) {
            Log.w(TAG, "Sound pack ${currentSoundPack.name} not loaded, attempting to load")
            ensureSoundPackLoaded(currentSoundPack)
        }

        // Stop any currently playing sound
        stopAllSounds()

        val soundMap = soundPackMap[currentSoundPack]
        if (soundMap == null) {
            Log.e(TAG, "❌ Sound map not found for sound pack: ${currentSoundPack.name}")
            Log.e(TAG, "Available sound packs: ${soundPackMap.keys.joinToString { it.name }}")
            // Graceful degradation: vibration was already handled above
            return
        }

        val soundId = soundMap[button]
        if (soundId == null) {
            Log.w(TAG, "Sound not available for $button in ${currentSoundPack.name}, using haptic feedback only")

            // FALLBACK: Try to use a working sound as substitute
            val fallbackSoundId = soundMap[SimonButton.GREEN]
            if (fallbackSoundId != null) {
                Log.i(TAG, "Using GREEN button sound as fallback for ${button.name}")
                playSoundWithVolume(fallbackSoundId)
                return
            }
            // Graceful degradation: vibration was already handled above
            return
        }

        val loadStatus = loadStatusMap[soundId] ?: false
        Log.d(TAG, "🎵 Playing ${currentSoundPack.name} sound for button $button (ID=$soundId, Loaded=$loadStatus)")

        if (!loadStatus) {
            Log.w(TAG, "Sound hasn't finished loading: $soundId, using haptic feedback only")
            // Graceful degradation: vibration was already handled above
            return
        }

        playSoundWithVolume(soundId)
    }

    /**
     * Play a sound with the current master volume setting
     */
    private fun playSoundWithVolume(soundId: Int, volumeMultiplier: Float = 1.0f) {
        val volume = masterVolume * volumeMultiplier
        val clampedVolume = volume.coerceIn(GameConstants.MIN_VOLUME, GameConstants.MAX_VOLUME)

        val playId = soundPool.play(soundId, clampedVolume, clampedVolume, 1, 0, 1.0f)
        if (playId == 0) {
            Log.e(TAG, "❌ Failed to play sound (playId=0)")
        } else {
            Log.d(TAG, "✓ Playing sound (playId=$playId, volume=$clampedVolume)")
            activeStreamId = playId
        }
    }

    /**
     * Stop all currently playing sounds
     */
    fun stopAllSounds() {
        if (activeStreamId != 0) {
            Log.d(TAG, "Stopping active sound stream: $activeStreamId")
            soundPool.stop(activeStreamId)
            activeStreamId = 0
        }
    }

    /**
     * Play the error sound for when player makes a mistake using the current sound pack.
     * Implements graceful degradation - if sound fails, haptic feedback is still provided.
     */
    fun playErrorSound() {
        Log.d(TAG, "Request to play error sound with sound pack: ${currentSoundPack.name}")

        // Handle error vibration first, independently from sound state (graceful degradation)
        if (vibrateEnabled && !isPaused) {
            try {
                Log.d(TAG, "📳 Triggering error vibration")
                // Create a pattern for error: vibrate-pause-vibrate
                val timings = longArrayOf(0, GameConstants.VIBRATION_DURATION_MS, 100, GameConstants.VIBRATION_DURATION_MS)
                val amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to vibrate for error", e)
            }
        }

        // Don't play sound if paused or muted, but vibration was already handled above
        if (isPaused || isMuted) {
            Log.d(TAG, "Sounds are ${if (isPaused) "paused" else "muted"}, not playing error sound (vibration was handled separately)")
            return
        }

        // Ensure current sound pack is loaded
        if (!loadedSoundPacks.contains(currentSoundPack)) {
            Log.w(TAG, "Sound pack ${currentSoundPack.name} not loaded, attempting to load")
            ensureSoundPackLoaded(currentSoundPack)
        }

        // Stop any currently playing sound
        stopAllSounds()

        val errorSoundId = errorSoundMap[currentSoundPack]
        if (errorSoundId == null) {
            Log.w(TAG, "Error sound not available for ${currentSoundPack.name}, using haptic feedback only")
            // Graceful degradation: vibration was already handled above
            return
        }

        val loadStatus = loadStatusMap[errorSoundId] ?: false
        Log.d(TAG, "Playing ${currentSoundPack.name} error sound (ID=$errorSoundId, Loaded=$loadStatus)")

        if (!loadStatus) {
            Log.w(TAG, "Error sound hasn't finished loading, using haptic feedback only")
            // Graceful degradation: vibration was already handled above
            return
        }

        // Play error sound with slightly higher volume
        playSoundWithVolume(errorSoundId, GameConstants.ERROR_SOUND_VOLUME_BOOST)
    }

    /**
     * Release resources when no longer needed
     */
    fun release() {
        Log.d(TAG, "Releasing SoundPool resources")
        stopAllSounds()
        abandonAudioFocus()
        context.applicationContext.unregisterComponentCallbacks(this)
        soundPool.release()
    }

    // --- Volume Control Methods ---

    /**
     * Set the master volume for all sounds
     * @param volume Volume level from 0.0 (silent) to 1.0 (full volume)
     */
    fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(GameConstants.MIN_VOLUME, GameConstants.MAX_VOLUME)
        Log.d(TAG, "Master volume set to: $masterVolume")
    }

    /**
     * Get the current master volume
     * @return Volume level from 0.0 to 1.0
     */
    fun getMasterVolume(): Float {
        return masterVolume
    }

    // --- ComponentCallbacks2 Implementation (Memory-aware loading) ---

    /**
     * Called when the system needs to reclaim memory.
     * Releases unused sound packs to free memory.
     */
    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        Log.d(TAG, "onTrimMemory called with level: $level")

        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> {
                Log.d(TAG, "Memory running moderate - no action needed")
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                Log.d(TAG, "Memory running low - releasing unused sound packs")
                releaseUnusedSoundPacks()
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                Log.d(TAG, "Critical memory situation - releasing all unused sound packs")
                releaseUnusedSoundPacks()
            }
            else -> {
                Log.d(TAG, "Unknown trim memory level: $level")
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // No action needed for configuration changes
        Log.d(TAG, "Configuration changed")
    }

    @Deprecated("Deprecated in ComponentCallbacks2", ReplaceWith("onTrimMemory(level)"))
    override fun onLowMemory() {
        Log.d(TAG, "onLowMemory called - releasing unused sound packs")
        releaseUnusedSoundPacks()
    }

    /**
     * Release sound packs that are not currently in use to free memory.
     * Always keeps the current sound pack loaded.
     */
    private fun releaseUnusedSoundPacks() {
        val packsToRelease = loadedSoundPacks.filter { it != currentSoundPack }

        if (packsToRelease.isEmpty()) {
            Log.d(TAG, "No unused sound packs to release")
            return
        }

        Log.d(TAG, "Releasing ${packsToRelease.size} unused sound packs: ${packsToRelease.joinToString { it.name }}")

        for (soundPack in packsToRelease) {
            // Remove from tracking
            loadedSoundPacks.remove(soundPack)

            // Unload the sounds
            soundPackMap[soundPack]?.values?.forEach { soundId ->
                soundPool.unload(soundId)
                loadStatusMap.remove(soundId)
            }
            soundPackMap.remove(soundPack)

            errorSoundMap[soundPack]?.let { errorSoundId ->
                soundPool.unload(errorSoundId)
                loadStatusMap.remove(errorSoundId)
            }
            errorSoundMap.remove(soundPack)

            Log.d(TAG, "Released sound pack: ${soundPack.name}")
        }
    }

    /**
     * Get the number of currently loaded sound packs
     * Useful for debugging and testing memory management
     */
    fun getLoadedSoundPackCount(): Int {
        return loadedSoundPacks.size
    }

    /**
     * Get list of currently loaded sound packs
     * Useful for debugging and testing memory management
     */
    fun getLoadedSoundPacks(): Set<SoundPack> {
        return loadedSoundPacks.toSet()
    }
}