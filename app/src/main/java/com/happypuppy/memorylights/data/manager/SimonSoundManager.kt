package com.happypuppy.memorylights.data.manager

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.happypuppy.memorylights.R
import com.happypuppy.memorylights.domain.enums.SimonButton
import com.happypuppy.memorylights.domain.enums.SoundPack
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages sound effects for the Memory Lights game with support for multiple sound packs
 * Enhanced with debug logging and lifecycle awareness
 */
class SimonSoundManager(private val context: Context) {

    private val TAG = "SimonSoundManager"

    private val soundPool: SoundPool

    // Flag to track if sounds are paused when app is in background
    private var isPaused = false
    
    // Flag to track if sounds are muted
    private var isMuted = false

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

    // Currently active sound pack
    private var currentSoundPack = SoundPack.STANDARD

    // Track load status
    private val loadStatusMap = ConcurrentHashMap<Int, Boolean>()

    // Track currently playing sound streams to be able to stop them
    private var activeStreamId: Int = 0

    init {
        Log.d(TAG, "Initializing SimonSoundManager")

        // List all raw resources to verify what's available
        listAllRawResources()

        // Configure audio attributes for game sounds
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

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
        }

        // Load all sound packs
        loadAllSounds()
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
     * Load sounds for all available sound packs
     */
    private fun loadAllSounds() {
        try {
            // Log available sound packs
            Log.d(TAG, "Available sound packs: ${SoundPack.entries.joinToString { it.name }}")

            // Load each sound pack
            for (soundPack in SoundPack.entries) {
                loadSoundPack(soundPack)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading sounds", e)
        }
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
    }

    /**
     * Change the active sound pack
     */
    fun setSoundPack(soundPack: SoundPack) {
        Log.d(TAG, "Changing sound pack to: ${soundPack.name}")
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
     * Play the sound associated with a specific Simon button using the current sound pack
     *
     * @param button The button to play sound for
     * @param isPlayerPressed Whether this sound is from a player pressing a button (for vibration)
     */
    fun playSound(button: SimonButton, isPlayerPressed: Boolean = false) {
        Log.d(TAG, "🔊 Request to play sound for button: $button with sound pack: ${currentSoundPack.name}, player pressed: $isPlayerPressed")

        // Handle vibration first, independently from sound state
        if (isPlayerPressed) {
            Log.d(TAG, "📳 Player pressed button, triggering vibration")
            // Force this to run on the main thread - it might be getting lost otherwise
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                vibrate()
            }
        }

        // Don't play sound if paused or muted, but vibration was already handled above
        if (isPaused || isMuted) {
            Log.d(TAG, "Sounds are ${if (isPaused) "paused" else "muted"}, not playing sound (vibration was handled separately)")
            return
        }

        // Stop any currently playing sound
        stopAllSounds()

        val soundMap = soundPackMap[currentSoundPack]
        if (soundMap == null) {
            Log.e(TAG, "❌ Sound map not found for sound pack: ${currentSoundPack.name}")
            // Log all available sound packs for debugging
            Log.e(TAG, "Available sound packs: ${soundPackMap.keys.joinToString { it.name }}")
            return
        }

        val soundId = soundMap[button]
        if (soundId == null) {
            Log.e(TAG, "❌ Sound ID not found for button: $button in sound pack: ${currentSoundPack.name}")
            // Log all available buttons in the sound map for debugging
            Log.e(TAG, "Available buttons in ${currentSoundPack.name}: ${soundMap.keys.joinToString { it.name }}")
            // Extra debugging - show what resource name we're looking for
            val prefix = currentSoundPack.resourcePrefix
            val expectedResourceName = "${prefix}_${button.name.lowercase()}_tone"
            Log.e(TAG, "Expected resource name: $expectedResourceName")
            val resourceId = getResourceId(expectedResourceName)
            Log.e(TAG, "Resource ID lookup result: $resourceId")
            
            // FALLBACK: Try to use a working sound as substitute (for PURPLE/ORANGE buttons only)
            if (button == SimonButton.PURPLE || button == SimonButton.ORANGE) {
                Log.w(TAG, "🔧 FALLBACK: Attempting to use GREEN button sound for ${button.name}")
                val fallbackSoundId = soundMap[SimonButton.GREEN]
                if (fallbackSoundId != null) {
                    Log.i(TAG, "✓ Using GREEN button sound as fallback for ${button.name}")
                    // Play the fallback sound
                    val playId = soundPool.play(fallbackSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
                    if (playId != 0) {
                        activeStreamId = playId
                        Log.i(TAG, "✓ Fallback sound playing successfully")
                    }
                    return
                }
            }
            return
        }

        val loadStatus = loadStatusMap[soundId] ?: false
        Log.d(TAG, "🎵 Playing ${currentSoundPack.name} sound for button $button (ID=$soundId, Loaded=$loadStatus)")

        if (!loadStatus) {
            Log.w(TAG, "⚠️ Attempting to play sound that hasn't finished loading: $soundId")
        }

        // Check audio state before playing
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        Log.d(TAG, "🔊 Current audio state: Volume=${currentVolume}/${maxVolume}")

        // Special logging for PURPLE and ORANGE buttons to debug the issue
        if (button == SimonButton.PURPLE || button == SimonButton.ORANGE) {
            Log.i(TAG, "🟣🟠 DEBUGGING ${button.name} BUTTON:")
            Log.i(TAG, "  - SoundID: $soundId")
            Log.i(TAG, "  - LoadStatus: $loadStatus")
            Log.i(TAG, "  - Sound Pack: ${currentSoundPack.name}")
            Log.i(TAG, "  - Resource Prefix: ${currentSoundPack.resourcePrefix}")
            Log.i(TAG, "  - Expected Resource: ${currentSoundPack.resourcePrefix}_${button.name.lowercase()}_tone")
        }

        // Play sound with default priority, no loop, normal rate
        val playId = soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
        if (playId == 0) {
            Log.e(TAG, "❌ Failed to play sound for button $button (playId=0)")
            // Extra logging for PURPLE/ORANGE failures
            if (button == SimonButton.PURPLE || button == SimonButton.ORANGE) {
                Log.e(TAG, "🚨 ${button.name} BUTTON SOUND FAILED TO PLAY! SoundID=$soundId, LoadStatus=$loadStatus")
            }
        } else {
            Log.d(TAG, "✓ Successfully playing sound for button $button (playId=$playId)")
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
     * Play the error sound for when player makes a mistake using the current sound pack
     */
    fun playErrorSound() {
        Log.d(TAG, "Request to play error sound with sound pack: ${currentSoundPack.name}")

        // Handle error vibration first, independently from sound state
        if (vibrateEnabled && !isPaused) {
            try {
                Log.d(TAG, "📳 Triggering error vibration")
                // Create a pattern for error: vibrate-pause-vibrate
                val timings = longArrayOf(0, 100, 100, 100)
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

        // Stop any currently playing sound
        stopAllSounds()

        val errorSoundId = errorSoundMap[currentSoundPack]
        if (errorSoundId == null) {
            Log.e(TAG, "❌ Error sound ID not found for sound pack: ${currentSoundPack.name}")
            return
        }

        val loadStatus = loadStatusMap[errorSoundId] ?: false
        Log.d(TAG, "Playing ${currentSoundPack.name} error sound (ID=$errorSoundId, Loaded=$loadStatus)")

        if (!loadStatus) {
            Log.w(TAG, "⚠️ Attempting to play error sound that hasn't finished loading")
        }

        // Check audio state before playing
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        Log.d(TAG, "Current audio state: Volume=$currentVolume")

        // Play error sound with slightly higher priority, no loop, normal rate
        val playId = soundPool.play(errorSoundId, 1.2f, 1.2f, 2, 0, 1.0f)
        if (playId == 0) {
            Log.e(TAG, "Failed to play error sound (playId=0)")
        } else {
            Log.d(TAG, "✓ Successfully playing error sound (playId=$playId)")
            activeStreamId = playId
        }
    }

    /**
     * Release resources when no longer needed
     */
    fun release() {
        Log.d(TAG, "Releasing SoundPool resources")
        stopAllSounds()
        soundPool.release()
    }
}