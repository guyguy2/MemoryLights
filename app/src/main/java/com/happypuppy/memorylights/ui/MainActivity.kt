package com.happypuppy.memorylights.ui

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.DefaultLifecycleObserver
import com.happypuppy.memorylights.R
import com.happypuppy.memorylights.domain.enums.SimonButton
import com.happypuppy.memorylights.domain.enums.SoundPack
import com.happypuppy.memorylights.domain.model.GameState
import com.happypuppy.memorylights.ui.screens.MemoryLightsGame
import com.happypuppy.memorylights.ui.theme.MyApplicationTheme
import com.happypuppy.memorylights.ui.viewmodels.SimonGameViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    // Use Koin to inject ViewModel
    private val viewModel: SimonGameViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.d(TAG, "onCreate called")

        // Register the ViewModel as a lifecycle observer to handle pause/resume
        lifecycle.addObserver(viewModel as DefaultLifecycleObserver)

        // Check for sound resources
        checkSoundResources()

        // Check audio settings
        val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        Log.d(TAG, "Audio volume: $currentVolume/$maxVolume")

        // Setup back press handling using OnBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Check if we're in the settings screen
                if (viewModel.uiState.value.gameState is GameState.Settings) {
                    // Go back to the game instead of exiting the app
                    viewModel.exitSettings()
                } else {
                    // Normal back behavior for other screens
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        setContent {
            MyApplicationTheme {
                // No need for a Surface here since our Scaffold components handle the surface
                MemoryLightsGame(viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")

        // Remove lifecycle observer
        lifecycle.removeObserver(viewModel as DefaultLifecycleObserver)
    }

    private fun checkSoundResources() {
        try {
            // Get all sound packs
            val soundPacks = SoundPack.entries
            val buttons = SimonButton.entries

            Log.d(TAG, "Checking sound resources:")

            // Check resources for each sound pack
            soundPacks.forEach { soundPack ->
                val prefix = soundPack.resourcePrefix
                Log.d(TAG, "Checking ${soundPack.displayName} (${prefix}) sound pack:")

                // Check button sounds
                buttons.forEach { button ->
                    val resourceName = "${prefix}_${button.name.lowercase()}_tone"
                    checkResource(resourceName)
                }

                // Check error sound
                val errorResourceName = "${prefix}_error_tone"
                checkResource(errorResourceName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking resources", e)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Log the configuration change
        val orientation = when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> "Landscape"
            Configuration.ORIENTATION_PORTRAIT -> "Portrait"
            else -> "Undefined"
        }
        Log.d(TAG, "Configuration changed: orientation=$orientation")

        // If the game is in PlayerRepeating state, reset the timeout timer
        // This prevents timeout issues during orientation changes
        if (viewModel.uiState.value.gameState is GameState.PlayerRepeating) {
            Log.d(TAG, "Resetting timeout timer after orientation change")
            viewModel.resetTimeoutTimer()
        }
    }

    private fun checkResource(resourceName: String) {
        // Map resource names to direct resource IDs
        val resourceId = when (resourceName) {
            // Standard sound pack
            "standard_green_tone" -> R.raw.standard_green_tone
            "standard_red_tone" -> R.raw.standard_red_tone
            "standard_yellow_tone" -> R.raw.standard_yellow_tone
            "standard_blue_tone" -> R.raw.standard_blue_tone
            "standard_error_tone" -> R.raw.standard_error_tone

            // Funny sound pack - using standard sounds until funny ones are added
            "funny_green_tone", "funny_red_tone", "funny_yellow_tone",
            "funny_blue_tone", "funny_error_tone" -> {
                // Log that we're using standard sounds as fallback
                Log.d(TAG, "  - Using standard sounds for: $resourceName")
                0 // Return 0 to indicate it's not directly available but handled
            }

            // For all other resources
            else -> 0
        }

        if (resourceId != 0) {
            // Resource exists
            Log.d(TAG, "✓ Resource found: $resourceName (ID: $resourceId)")

            // Try to open the resource to verify it's accessible
            try {
                val descriptor = resources.openRawResourceFd(resourceId)
                Log.d(TAG, "  - File size: ${descriptor.length} bytes")
                descriptor.close()
            } catch (e: Exception) {
                Log.e(TAG, "  - Error opening resource: $e")
            }
        } else if (resourceName.startsWith("funny_")) {
            // Resource not found but we have fallbacks for funny sounds
            Log.d(TAG, "✓ (Fallback) Will use standard sounds for: $resourceName")
        } else {
            // Resource not found and no fallback
            Log.e(TAG, "✗ Resource missing: $resourceName")
        }
    }
}