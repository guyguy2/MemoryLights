package com.happypuppy.memorylights.ui

import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.happypuppy.memorylights.BuildConfig
import com.happypuppy.memorylights.data.manager.SimonSoundManager
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
        private const val SPLASH_MAX_HOLD_MS = 2_000L
    }

    // Use Koin to inject ViewModel
    private val viewModel: SimonGameViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate. Keeps the system splash on screen until
        // initial persisted settings (including themeMode) resolve or the 2s cap fires.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        val splashDeadlineMs = SystemClock.elapsedRealtime() + SPLASH_MAX_HOLD_MS
        splashScreen.setKeepOnScreenCondition {
            !viewModel.uiState.value.settingsLoaded &&
                SystemClock.elapsedRealtime() < splashDeadlineMs
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        Log.d(TAG, "onCreate called")

        // Register the ViewModel as a lifecycle observer to handle pause/resume
        lifecycle.addObserver(viewModel)

        // Check for sound resources
        checkSoundResources()

        // Check audio settings
        val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        Log.d(TAG, "Audio volume: $currentVolume/$maxVolume")

        // System back button is handled by NavHost via the Activity's
        // OnBackPressedDispatcher: Statistics → Settings → Game pops the
        // back-stack automatically, and a back press on the Game route
        // falls through to default activity-finish behavior. The
        // ViewModel's onReturnToGame is fired by a LaunchedEffect on the
        // current destination inside MemoryLightsGame.

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            MyApplicationTheme(themeMode = uiState.themeMode) {
                MemoryLightsGame(viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")

        // Remove lifecycle observer
        lifecycle.removeObserver(viewModel)
    }

    private fun checkSoundResources() {
        if (!BuildConfig.DEBUG) return
        try {
            val soundPacks = SoundPack.entries.filter { it.isAvailable }
            val buttons = SimonButton.entries

            Log.d(TAG, "Checking sound resources:")

            soundPacks.forEach { soundPack ->
                val prefix = soundPack.resourcePrefix
                Log.d(TAG, "Checking ${getString(soundPack.displayNameRes)} ($prefix) sound pack:")

                buttons.forEach { button ->
                    checkResource("${prefix}_${button.name.lowercase()}_tone")
                }

                checkResource("${prefix}_error_tone")
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
        val resourceId = SimonSoundManager.getResourceId(resourceName)

        if (resourceId != 0) {
            Log.d(TAG, "✓ Resource found: $resourceName (ID: $resourceId)")
            try {
                resources.openRawResourceFd(resourceId).use { descriptor ->
                    Log.d(TAG, "  - File size: ${descriptor.length} bytes")
                }
            } catch (e: Exception) {
                Log.e(TAG, "  - Error opening resource: $e")
            }
        } else {
            Log.e(TAG, "✗ Resource missing: $resourceName")
        }
    }
}