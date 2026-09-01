package ph.mart.healthapp.feature.progress.ui.achievement

import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.fasting.durationMinutes
import ph.mart.healthapp.core.data.streak.StreakBadge
import ph.mart.healthapp.core.data.streak.StreakStats
import ph.mart.healthapp.core.data.streak.WeightBadge

/**
 * "What have I earned, and what's next" — the question neither Home's streak card (one family,
 * five dots) nor the charts answer. Derived, never stored: every figure is a fold over what
 * [ph.mart.healthapp.feature.progress.ui.progress.ProgressViewModel] already combines, the same
 * way [ph.mart.healthapp.feature.progress.ui.progress.weeklyRecap] is.
 *
 * Nothing here persists anything, which is what keeps CLAUDE.md's no-celebration rule intact: a
 * toast when a badge lights up would need an "already celebrated" flag, and there is none to add.
 *
 * Feature-local because Progress is the only screen that shows it and every input is a
 * `:core:data` type, so nothing leaks a feature type downward — `weeklyRecap`'s reasoning. It
 * moves down to `:core:data` the day a second feature needs it, the way `insightFor()` did.
 */

/** The two shipped families read their thresholds off the enums Home already draws, never a
 * retyped copy, so the tab and the streak card can't disagree about what a badge is worth. */
private val STREAK_TIERS = StreakBadge.entries.map { it.days }
private val WEIGHT_TIERS = WeightBadge.entries.map { it.kg.toInt() }

/** ponytail: days-logged and workouts ride windowed inputs — `observeDailyNutrition()` is a
 * dense year and `observeRecentEntries()` a rolling one — so a badge can go dark after a year
 * away. That is the exposure the shipped streak badge already carries, which is why both stop
 * well inside a year (50 workouts is one a week). Upgrade path: a `SELECT COUNT(*)` flow on
 * `ExerciseEntryDao` and `FoodEntryDao`. */
private val DAYS_LOGGED_TIERS = listOf(10, 50, 100)
private val WORKOUT_TIERS = listOf(10, 25, 50)

private val FAST_TIERS = listOf(1, 10, 50)

/** The goal range's ends (see `FAST_GOAL_HOURS`): the shortest thing this app calls a fast, and
 * the longest it will schedule one for. */
private val LONGEST_FAST_TIERS = listOf(16, 24)

private val PHOTO_TIERS = listOf(3, 10, 25)

enum class BadgeFamily { Streak, DaysLogged, WeightMoved, Workouts, Fasts, LongestFast, Photos }

/** [tiers] ascending, [current] in the same unit — days, kilograms, hours or a plain count,
 * whichever the family counts in. The card formats; this only counts. */
data class BadgeGroup(val family: BadgeFamily, val tiers: List<Int>, val current: Int) {
    val earnedCount: Int get() = tiers.count { current >= it }

    /** The threshold the user is working toward, or null once every tier is earned. */
    val next: Int? get() = tiers.firstOrNull { current < it }
}

/**
 * Every family, in display order. [weightProgressKg] null drops the weight family entirely rather
 * than showing it at zero — a Maintain goal has no direction to move, the same call the streak
 * card makes when it hides its weight line.
 *
 * The streak family scores off [StreakStats.best], never `current`: breaking a streak has never
 * un-earned a badge in this app and must not start here.
 */
fun badgeGroups(
    streak: StreakStats,
    weightProgressKg: Double?,
    workoutCount: Int,
    fasts: List<FastSession>,
    photoCount: Int,
): List<BadgeGroup> = listOfNotNull(
    BadgeGroup(BadgeFamily.Streak, STREAK_TIERS, streak.best),
    BadgeGroup(BadgeFamily.DaysLogged, DAYS_LOGGED_TIERS, streak.totalDaysLogged),
    weightProgressKg?.let {
        // Floored, so 4.9 kg has honestly not reached the 5 kg tier. Negative — moved the wrong
        // way — reads as zero here; the streak card's line hides itself instead, but a badge row
        // that vanished on a bad week would be a badge row nobody could trust.
        BadgeGroup(BadgeFamily.WeightMoved, WEIGHT_TIERS, it.toInt().coerceAtLeast(0))
    },
    BadgeGroup(BadgeFamily.Workouts, WORKOUT_TIERS, workoutCount),
    BadgeGroup(BadgeFamily.Fasts, FAST_TIERS, fasts.size),
    // `durationMinutes` only reads its argument for a running fast, and `observeSessions()`
    // returns completed ones, so the clock never enters this.
    BadgeGroup(BadgeFamily.LongestFast, LONGEST_FAST_TIERS, fasts.maxOfOrNull { it.durationMinutes(0) / 60 } ?: 0),
    BadgeGroup(BadgeFamily.Photos, PHOTO_TIERS, photoCount),
)
