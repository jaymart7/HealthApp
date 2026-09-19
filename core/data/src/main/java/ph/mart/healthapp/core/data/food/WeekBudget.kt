package ph.mart.healthapp.core.data.food

import ph.mart.healthapp.core.data.DAYS_IN_WEEK
import ph.mart.healthapp.core.data.exercise.budgetKcal
import ph.mart.healthapp.core.data.exercise.weekStart
import ph.mart.healthapp.core.data.health.BurnDay
import ph.mart.healthapp.core.data.profile.DailyTargets

/**
 * The week's calorie bank — what the days already behind you left over, or spent in advance.
 *
 * Derived, never stored: the third fold in this file's neighbourhood, after [dailySeries] and
 * [averages], and the `streak/` and `trainingWeek()` shape one domain over. No table, no
 * repository, no schema — so a backdated meal or a restored import recomputes rather than needing
 * a counter patched.
 *
 * Four rules it exists to keep, each of them easy to "fix" back into a bug:
 *
 * - **Monday to Sunday**, via [weekStart] — the week [trainingWeek] already scores, not a rolling
 *   seven days. A bank you can spend needs an end.
 * - **Today is never in it.** The bank is the week's *closed* days; today is what the calorie ring
 *   is for, and folding a half-eaten day in would make the figure swing all afternoon.
 * - **An unlogged day is skipped, not banked.** Counting a day nobody opened the app on as "2,000
 *   under" invents a credit the user never earned — [averages]' reason for dividing by logged days
 *   only. [daysCounted] against [daysClosed] is what lets the card say so.
 * - **Burn folds in exactly as it does on the day**, through [budgetKcal] and the user's own
 *   `addExerciseToBudget` switch. Anything else and this and the calorie ring above it disagree on
 *   one screen.
 *
 * The one figure it deliberately does *not* produce is a new target. [perDayKcal] is a report, and
 * it is clamped at [DailyTargets.floor] with [belowFloor] set when the clamp bit — the same
 * warn-don't-block floor the onboarding and the target editor keep.
 */
data class WeekBudget(
    /** Positive under, negative over, across [daysCounted]. */
    val bankedKcal: Int,
    val daysCounted: Int,
    /** Days of this week already behind today — 0 on a Monday, 6 on a Sunday. */
    val daysClosed: Int,
    /** Days left to spend it on, today included. Never 0. */
    val daysLeft: Int,
    /** What [daysLeft] days would each hold to finish the week even. */
    val perDayKcal: Int,
    /** Whether [perDayKcal] is the floor rather than the arithmetic. */
    val belowFloor: Boolean,
) {
    /** Nothing behind today has been logged — a fresh Monday, or a week nobody logged. */
    val isEmpty: Boolean get() = daysCounted == 0
}

/**
 * [nutrition] is the dense series; only this week's closed days are read out of it. [targets] is
 * the target **as it stands now**, applied to every day of the week — this app historises no
 * targets, the caveat `stepAverages()` carries for the step goal.
 */
fun weekBudget(
    nutrition: List<DayNutrition>,
    burn: List<BurnDay>,
    targets: DailyTargets,
    addExerciseToBudget: Boolean,
    todayEpochDay: Long,
): WeekBudget {
    val start = weekStart(todayEpochDay)
    val closed = (start until todayEpochDay)
    val burnByDay = burn.associateBy { it.dateEpochDay }
    val logged = nutrition.filter { it.dateEpochDay in closed && it.isLogged }

    val banked = logged.sumOf { day ->
        val burned = burnByDay[day.dateEpochDay]?.burnedKcal ?: 0
        budgetKcal(targets.calories, burned, addExerciseToBudget) - day.calories
    }

    val daysClosed = (todayEpochDay - start).toInt()
    val daysLeft = DAYS_IN_WEEK - daysClosed
    val even = (banked + daysLeft * targets.calories) / daysLeft

    return WeekBudget(
        bankedKcal = banked,
        daysCounted = logged.size,
        daysClosed = daysClosed,
        daysLeft = daysLeft,
        perDayKcal = even.coerceAtLeast(targets.floor),
        belowFloor = even < targets.floor,
    )
}
