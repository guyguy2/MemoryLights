package com.happypuppy.memorylights.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementTest {

    private fun eval(
        level: Int = 0,
        gamesPlayed: Int = 0,
        bestStreak: Int = 0,
        blitzWon: Boolean = false,
        blitzElapsedMs: Long = 0L,
        dailyCompleted: Boolean = false
    ) = Achievement.evaluate(level, gamesPlayed, bestStreak, blitzWon, blitzElapsedMs, dailyCompleted)

    @Test
    fun `empty snapshot unlocks nothing`() {
        assertEquals(emptySet<Achievement>(), eval())
    }

    @Test
    fun `first finished game unlocks FirstWin`() {
        val unlocked = eval(level = 1, gamesPlayed = 1)
        assertTrue(Achievement.FirstWin in unlocked)
    }

    @Test
    fun `level thresholds unlock cumulatively`() {
        // Level 10 unlocks Level10 only
        val at10 = eval(level = 10, gamesPlayed = 1)
        assertTrue(Achievement.Level10 in at10)
        assertTrue(Achievement.Level25 !in at10)

        // Level 25 unlocks both
        val at25 = eval(level = 25, gamesPlayed = 1)
        assertTrue(Achievement.Level10 in at25)
        assertTrue(Achievement.Level25 in at25)
    }

    @Test
    fun `streak thresholds unlock cumulatively`() {
        val s3 = eval(level = 1, gamesPlayed = 1, bestStreak = 3)
        assertTrue(Achievement.Streak3 in s3)
        assertTrue(Achievement.Streak7 !in s3)

        val s7 = eval(level = 1, gamesPlayed = 1, bestStreak = 7)
        assertTrue(Achievement.Streak3 in s7)
        assertTrue(Achievement.Streak7 in s7)
    }

    @Test
    fun `blitz win unlocks BlitzChampion`() {
        val unlocked = eval(level = 20, gamesPlayed = 1, blitzWon = true, blitzElapsedMs = 90_000L)
        assertTrue(Achievement.BlitzChampion in unlocked)
        // 90s > 30s, Speedrun30s must NOT unlock
        assertTrue(Achievement.Speedrun30s !in unlocked)
    }

    @Test
    fun `blitz win under 30 seconds unlocks Speedrun30s`() {
        val unlocked = eval(level = 20, gamesPlayed = 1, blitzWon = true, blitzElapsedMs = 25_000L)
        assertTrue(Achievement.BlitzChampion in unlocked)
        assertTrue(Achievement.Speedrun30s in unlocked)
    }

    @Test
    fun `zero elapsed blitz does not unlock Speedrun30s`() {
        // 0ms is the "unknown" sentinel from blitzStartTimeMs being 0L — must not
        // accidentally award the speedrun badge.
        val unlocked = eval(level = 20, gamesPlayed = 1, blitzWon = true, blitzElapsedMs = 0L)
        assertTrue(Achievement.BlitzChampion in unlocked)
        assertTrue(Achievement.Speedrun30s !in unlocked)
    }

    @Test
    fun `blitz speedrun badge requires blitzWon true`() {
        // Elapsed under 30s but not a win — must not unlock.
        val unlocked = eval(level = 5, gamesPlayed = 1, blitzWon = false, blitzElapsedMs = 10_000L)
        assertTrue(Achievement.Speedrun30s !in unlocked)
    }

    @Test
    fun `daily completion unlocks DailyDone`() {
        val unlocked = eval(level = 1, gamesPlayed = 1, dailyCompleted = true)
        assertTrue(Achievement.DailyDone in unlocked)
    }

    @Test
    fun `fromIds rebuilds enums and drops unknown ids`() {
        val ids = setOf(Achievement.FirstWin.name, "RemovedLegacyAchievement", Achievement.Streak3.name)
        val result = Achievement.fromIds(ids)
        assertEquals(setOf(Achievement.FirstWin, Achievement.Streak3), result)
    }

    @Test
    fun `fromIds on empty set returns empty`() {
        assertEquals(emptySet<Achievement>(), Achievement.fromIds(emptySet()))
    }
}
