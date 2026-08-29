package ph.mart.healthapp.core.data.streak

import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.round1
import ph.mart.healthapp.core.data.progress.WeightEntry

/**
 * Consistency, derived — never stored. Nothing in this file writes anything: the streak is a fold
 * over data the food, water, and progress repositories already return, the same way
 * [ph.mart.healthapp.core.data.food.dailySeries] is a fold over the diary. That's what keeps a
 * backdated entry or a restored import honest — the streak recomputes rather than needing a
 * stored counter patched.
 *
 * It sits at its own domain root rather than under `food/` because it merges three domains, and
 * it has no `local/` or `di/` because there is nothing to persist and no repository to bind.
 */

/** Matches [ph.mart.healthapp.core.data.food.TREND_WINDOW_DAYS] — both day-series inputs already
 * reach exactly this far back, so the streak can never see a gap that is really just an unread
 * window. */
const val STREAK_WINDOW_DAYS = 365

data class StreakStats(val current: Int, val best: Int, val totalDaysLogged: Int)

/**
 * Every day the user did *anything* — logged food, drank a glass, or weighed in. [nutrition] is
 * the dense zero-filled series, so [DayNutrition.isLogged] is the only honest signal there; a
 * zero-calorie day means "didn't open the app", not "ate nothing".
 */
fun loggedDays(
    nutrition: List<DayNutrition>,
    waterDays: Set<Long>,
    weightEntries: List<WeightEntry>,
): Set<Long> = buildSet {
    nutrition.forEach { if (it.isLogged) add(it.dateEpochDay) }
    addAll(waterDays)
    weightEntries.forEach { add(it.dateEpochDay) }
}

/**
 * [StreakStats.current] counts back from today, or from **yesterday** when today is still empty —
 * without that grace day the card would read "0 days" every morning until breakfast is logged,
 * which is the one bug every streak app ships first.
 *
 * [StreakStats.best] is the longest run anywhere in the set, so breaking a streak never takes an
 * earned badge away.
 */
fun Set<Long>.streakStats(todayEpochDay: Long): StreakStats {
    if (isEmpty()) return StreakStats(current = 0, best = 0, totalDaysLogged = 0)

    val anchor = when {
        contains(todayEpochDay) -> todayEpochDay
        contains(todayEpochDay - 1) -> todayEpochDay - 1
        else -> null
    }
    var current = 0
    if (anchor != null) {
        var day = anchor
        while (contains(day)) {
            current++
            day--
        }
    }

    var best = 0
    var run = 0
    var previous: Long? = null
    for (day in sorted()) {
        run = if (previous != null && day == previous + 1) run + 1 else 1
        if (run > best) best = run
        previous = day
    }

    return StreakStats(current = current, best = best, totalDaysLogged = size)
}

enum class StreakBadge(val days: Int) {
    Three(3),
    Week(7),
    Fortnight(14),
    Month(30),
    Century(100),
}

/** Earned off [StreakStats.best], never [StreakStats.current] — a badge you've earned stays earned. */
fun StreakStats.earnedBadges(): Set<StreakBadge> =
    StreakBadge.entries.filterTo(mutableSetOf()) { best >= it.days }

/** The badge the *current* run is working toward. Null once the run has passed them all. */
fun StreakStats.nextBadge(): StreakBadge? = StreakBadge.entries.firstOrNull { current < it.days }

/** ponytail: kg-native thresholds, so imperial reads "4.4 lb" rather than a round "5 lb". Give
 * the enum a separate `lb` field if that ever grates. */
enum class WeightBadge(val kg: Double) {
    Two(2.0),
    Five(5.0),
    Ten(10.0),
}

/**
 * Kilograms moved **in the goal's direction** — Lose subtracts, Build adds, Maintain has no
 * direction and so has no milestone at all (null). A negative result is returned as-is: the user
 * moved the wrong way, and the caller decides not to show it rather than this pretending it's 0.
 *
 * The start is the earliest entry **by date**, never by insertion order, so backdating a past
 * weight can't rewrite the journey — same hazard `trendVsSevenDaysAgo` guards against. A lone
 * entry is the journey's end, not both ends, so [onboardingWeightKg] stands in as the start.
 */
fun weightProgressKg(
    entries: List<WeightEntry>,
    goal: Goal,
    onboardingWeightKg: Double,
): Double? {
    if (goal == Goal.Maintain) return null
    val sorted = entries.sortedBy { it.dateEpochDay }
    val current = sorted.lastOrNull()?.weightKg ?: return null
    val start = if (sorted.size > 1) sorted.first().weightKg else onboardingWeightKg
    return round1(if (goal == Goal.Lose) start - current else current - start)
}

/** The highest weight badge [progressKg] has reached, or null below the first threshold. */
fun earnedWeightBadge(progressKg: Double): WeightBadge? =
    WeightBadge.entries.lastOrNull { progressKg >= it.kg }
