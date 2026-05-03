package com.happypuppy.memorylights.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import com.happypuppy.memorylights.domain.enums.SoundPack
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testScope: TestScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repo: DataStoreSettingsRepository

    @Before
    fun setUp() {
        testScope = TestScope(UnconfinedTestDispatcher())
        val file = File(tempFolder.root, "settings.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { file }
        )
        repo = DataStoreSettingsRepository(dataStore, scope = testScope)
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun `empty store emits default settings`() = runTest(testScope.testScheduler) {
        repo.settingsFlow.test {
            val first = awaitItem()
            assertEquals(AppSettings(), first)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setSoundPack persists and re-emits`() = runTest(testScope.testScheduler) {
        repo.settingsFlow.test {
            assertEquals(SoundPack.STANDARD, awaitItem().soundPack)
            repo.setSoundPack(SoundPack.RETRO)
            assertEquals(SoundPack.RETRO, awaitItem().soundPack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setHighScore4Button persists`() = runTest(testScope.testScheduler) {
        repo.settingsFlow.test {
            assertEquals(0, awaitItem().highScore4Button)
            repo.setHighScore4Button(42)
            assertEquals(42, awaitItem().highScore4Button)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setHighScore6Button does not affect 4-button score`() = runTest(testScope.testScheduler) {
        repo.settingsFlow.test {
            awaitItem()
            repo.setHighScore4Button(10)
            assertEquals(10, awaitItem().highScore4Button)
            repo.setHighScore6Button(20)
            val state = awaitItem()
            assertEquals(10, state.highScore4Button)
            assertEquals(20, state.highScore6Button)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multiple toggles persist independently`() = runTest(testScope.testScheduler) {
        repo.settingsFlow.test {
            awaitItem()
            repo.setVibrateEnabled(false)
            assertEquals(false, awaitItem().vibrateEnabled)
            repo.setSoundEnabled(false)
            val a = awaitItem()
            assertEquals(false, a.vibrateEnabled)
            assertEquals(false, a.soundEnabled)
            repo.setDifficultyEnabled(true)
            val b = awaitItem()
            assertEquals(true, b.difficultyEnabled)
            assertEquals(false, b.vibrateEnabled) // unchanged
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setMemoryLightsPlusEnabled toggles 6-button mode`() = runTest(testScope.testScheduler) {
        repo.settingsFlow.test {
            assertEquals(false, awaitItem().memoryLightsPlusEnabled)
            repo.setMemoryLightsPlusEnabled(true)
            assertEquals(true, awaitItem().memoryLightsPlusEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
