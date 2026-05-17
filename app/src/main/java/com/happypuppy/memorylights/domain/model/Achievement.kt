package com.happypuppy.memorylights.domain.model

/**
 * Local achievements (F8). Each entry is a self-contained badge unlocked when the
 * paired predicate in [evaluate] fires. Achievement state is persisted as a
 * `Set<String>` of [Achievement.name] values in `SettingsRepository` so the
 * data layer never imports a UI-facing enum.
 *
 * Achievements are intentionally derived from existing game events — there's no
 * separate event bus. The ViewModel calls [evaluate] in [updateGameOverState] /
 * [handleBlitzWin] with the current snapshot, and any newly-unlocked IDs are
 * written through the settings repo.
 *
 * Ordering in this enum drives display order on the Statistics screen.
 */
enum class Achievement(
    val titleRes: Int,
    val descriptionRes: Int
) {
    FirstWin(
        titleRes = com.happypuppy.memorylights.R.string.achievement_first_win_title,
        descriptionRes = com.happypuppy.memorylights.R.string.achievement_first_win_desc
    ),
    Level10(
        titleRes = com.happypuppy.memorylights.R.string.achievement_level_10_title,
        descriptionRes = com.happypuppy.memorylights.R.string.achievement_level_10_desc
    ),
    Level25(
        titleRes = com.happypuppy.memorylights.R.string.achievement_level_25_title,
        descriptionRes = com.happypuppy.memorylights.R.string.achievement_level_25_desc
    ),
    Streak3(
        titleRes = com.happypuppy.memorylights.R.string.achievement_streak_3_title,
        descriptionRes = com.happypuppy.memorylights.R.string.achievement_streak_3_desc
    ),
    Streak7(
        titleRes = com.happypuppy.memorylights.R.string.achievement_streak_7_title,
        descriptionRes = com.happypuppy.memorylights.R.string.achievement_streak_7_desc
    ),
    BlitzChampion(
        titleRes = com.happypuppy.memorylights.R.string.achievement_blitz_champion_title,
        descriptionRes = com.happypuppy.memorylights.R.string.achievement_blitz_champion_desc
    ),
    Speedrun30s(
        titleRes = com.happypuppy.memorylights.R.string.achievement_speedrun_30s_title,
        descriptionRes = com.happypuppy.memorylights.R.string.achievement_speedrun_30s_desc
    ),
    DailyDone(
        titleRes = com.happypuppy.memorylights.R.string.achievement_daily_done_title,
        descriptionRes = com.happypuppy.memorylights.R.string.achievement_daily_done_desc
    );

    companion object {
        /**
         * Compute the full set of achievements that should be unlocked given the
         * supplied snapshot. Callers compare against the current persisted set to
         * find newly-earned entries. Pure function — no side effects.
         *
         * @param level Level reached on the just-finished game (0 if not a game-over event).
         * @param gamesPlayed Total finished games **including** the current one.
         * @param bestStreak Best win streak after recording the current result.
         * @param blitzWon True if the current event is a completed Speed Blitz run.
         * @param blitzElapsedMs Elapsed time of the just-finished blitz run; ignored unless blitzWon.
         * @param dailyCompleted True if a Daily Challenge run was just recorded.
         */
        fun evaluate(
            level: Int,
            gamesPlayed: Int,
            bestStreak: Int,
            blitzWon: Boolean,
            blitzElapsedMs: Long,
            dailyCompleted: Boolean
        ): Set<Achievement> = buildSet {
            if (gamesPlayed >= 1) add(FirstWin)
            if (level >= 10) add(Level10)
            if (level >= 25) add(Level25)
            if (bestStreak >= 3) add(Streak3)
            if (bestStreak >= 7) add(Streak7)
            if (blitzWon) add(BlitzChampion)
            if (blitzWon && blitzElapsedMs in 1..30_000L) add(Speedrun30s)
            if (dailyCompleted) add(DailyDone)
        }

        /** Returns [Achievement] values whose [name] is present in [ids]; unknown ids are silently dropped. */
        fun fromIds(ids: Set<String>): Set<Achievement> {
            if (ids.isEmpty()) return emptySet()
            val byName = entries.associateBy { it.name }
            return ids.mapNotNullTo(mutableSetOf()) { byName[it] }
        }
    }
}
