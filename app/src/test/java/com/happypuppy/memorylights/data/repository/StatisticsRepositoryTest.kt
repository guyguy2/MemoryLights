package com.happypuppy.memorylights.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import com.happypuppy.memorylights.domain.model.GameStatistics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    /**
     * Audit item #24: a production-constructed repo (via [StatisticsRepository.fromContext])
     * owns its internal IO scope and must cancel it when [close] is called. We can't easily
     * build a `Context`-backed instance from a unit test, so we exercise the same code path
     * by hand-constructing an owned scope and verifying [close] cancels it. The test for the
     * injected-scope branch (caller retains ownership) lives in the existing tests above,
     * which use a `TestScope` that the `@After` cancels.
     */
    @Test
    fun `close cancels internally-owned scope`() {
        val ownedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repo = StatisticsRepository(
            dataStore = dataStore,
            scope = ownedScope,
            ownsScope = true
        )
        val job = ownedScope.coroutineContext[Job]!!
        assertTrue("scope is active before close", job.isActive)
        repo.close()
        assertFalse("scope is cancelled after close", job.isActive)
    }

    @Test
    fun `close is a no-op when scope is injected`() {
        // Default 2-arg constructor sets ownsScope = false — caller retains ownership.
        val injectedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val job = injectedScope.coroutineContext[Job]!!
        val repo = StatisticsRepository(dataStore = dataStore, scope = injectedScope)
        repo.close()
        assertTrue("injected scope is left alone by close", job.isActive)
        injectedScope.cancel()
    }
}
