package com.happypuppy.memorylights.data.repository

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
class StatisticsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testScope: TestScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: StatisticsRepository

    @Before
    fun setUp() {
        testScope = TestScope(UnconfinedTestDispatcher())
        val file = File(tempFolder.root, "statistics.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { file }
        )
        repository = StatisticsRepository(dataStore, scope = testScope)
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun `empty store emits zeroed statistics`() = runTest(testScope.testScheduler) {
        repository.statisticsFlow.test {
            assertEquals(GameStatistics(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `recordGameResult accumulates totals and tracks best streak`() =
        runTest(testScope.testScheduler) {
            repository.statisticsFlow.test {
                awaitItem()
                // First game: score 5 > 0 baseline, streak becomes 1.
                repository.recordGameResult(score = 5, sequenceLength = 7)
                val a = awaitItem()
                assertEquals(1, a.gamesPlayed)
                assertEquals(5, a.totalScore)
                assertEquals(1, a.bestStreak)

                // Second game: score 7 > previous 5, streak grows to 2.
                repository.recordGameResult(score = 7, sequenceLength = 9)
                val b = awaitItem()
                assertEquals(2, b.gamesPlayed)
                assertEquals(12, b.totalScore)
                assertEquals(2, b.bestStreak)

                // Third game: score 3 < previous 7, streak resets to 0,
                // bestStreak holds at 2.
                repository.recordGameResult(score = 3, sequenceLength = 5)
                val c = awaitItem()
                assertEquals(3, c.gamesPlayed)
                assertEquals(15, c.totalScore)
                assertEquals(2, c.bestStreak)

                // Fourth game: score 9 > previous 3, new run begins at 1.
                repository.recordGameResult(score = 9, sequenceLength = 11)
                val d = awaitItem()
                assertEquals(4, d.gamesPlayed)
                assertEquals(24, d.totalScore)
                assertEquals(2, d.bestStreak)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `streak counts consecutive level improvements not raw sequence length`() =
        runTest(testScope.testScheduler) {
            repository.statisticsFlow.test {
                awaitItem()
                // Three consecutive improvements should yield streak 3.
                repository.recordGameResult(score = 2, sequenceLength = 2)
                assertEquals(1, awaitItem().bestStreak)
                repository.recordGameResult(score = 5, sequenceLength = 5)
                assertEquals(2, awaitItem().bestStreak)
                repository.recordGameResult(score = 8, sequenceLength = 8)
                assertEquals(3, awaitItem().bestStreak)

                // Equal score (no improvement) breaks the run; bestStreak frozen.
                repository.recordGameResult(score = 8, sequenceLength = 8)
                assertEquals(3, awaitItem().bestStreak)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `resetStatistics clears all values`() = runTest(testScope.testScheduler) {
        repository.statisticsFlow.test {
            awaitItem()
            repository.recordGameResult(score = 8, sequenceLength = 9)
            assertEquals(8, awaitItem().totalScore)

            repository.resetStatistics()
            val cleared = awaitItem()
            assertEquals(GameStatistics(), cleared)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `averageScore computes correctly across recordings`() =
        runTest(testScope.testScheduler) {
            repository.statisticsFlow.test {
                awaitItem()
                repository.recordGameResult(score = 10, sequenceLength = 5)
                awaitItem()
                repository.recordGameResult(score = 20, sequenceLength = 5)
                val state = awaitItem()
                assertEquals(15.0, state.averageScore, 0.001)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
