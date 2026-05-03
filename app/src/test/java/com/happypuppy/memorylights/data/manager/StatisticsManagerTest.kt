package com.happypuppy.memorylights.data.manager

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import com.happypuppy.memorylights.domain.model.GameStatistics
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
class StatisticsManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testScope: TestScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var manager: StatisticsManager

    @Before
    fun setUp() {
        testScope = TestScope(UnconfinedTestDispatcher())
        val file = File(tempFolder.root, "statistics.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { file }
        )
        manager = StatisticsManager(dataStore, scope = testScope)
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun `empty store emits zeroed statistics`() = runTest(testScope.testScheduler) {
        manager.statisticsFlow.test {
            assertEquals(GameStatistics(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `recordGameResult increments counters and tracks high score`() =
        runTest(testScope.testScheduler) {
            manager.statisticsFlow.test {
                awaitItem()
                manager.recordGameResult(score = 5, sequenceLength = 7)
                val a = awaitItem()
                assertEquals(1, a.gamesPlayed)
                assertEquals(5, a.highScore)
                assertEquals(5, a.totalScore)
                assertEquals(7, a.bestStreak)

                manager.recordGameResult(score = 3, sequenceLength = 4)
                val b = awaitItem()
                assertEquals(2, b.gamesPlayed)
                assertEquals(5, b.highScore) // not lowered
                assertEquals(8, b.totalScore)
                assertEquals(7, b.bestStreak) // not lowered

                manager.recordGameResult(score = 9, sequenceLength = 10)
                val c = awaitItem()
                assertEquals(3, c.gamesPlayed)
                assertEquals(9, c.highScore) // raised
                assertEquals(17, c.totalScore)
                assertEquals(10, c.bestStreak)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `resetStatistics clears all values`() = runTest(testScope.testScheduler) {
        manager.statisticsFlow.test {
            awaitItem()
            manager.recordGameResult(score = 8, sequenceLength = 9)
            assertEquals(8, awaitItem().highScore)

            manager.resetStatistics()
            val cleared = awaitItem()
            assertEquals(GameStatistics(), cleared)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `averageScore computes correctly across recordings`() =
        runTest(testScope.testScheduler) {
            manager.statisticsFlow.test {
                awaitItem()
                manager.recordGameResult(score = 10, sequenceLength = 5)
                awaitItem()
                manager.recordGameResult(score = 20, sequenceLength = 5)
                val state = awaitItem()
                assertEquals(15.0, state.averageScore, 0.001)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
