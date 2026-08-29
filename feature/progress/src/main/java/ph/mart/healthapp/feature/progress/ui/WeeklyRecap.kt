package ph.mart.healthapp.feature.progress.ui

import kotlin.math.abs
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.food.NutritionAverages
import ph.mart.healthapp.core.data.food.averages
import ph.mart.healthapp.core.data.mood.MoodAverages
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.mood.moodAverages
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.WeightTrendDisplay
import ph.mart.healthapp.core.data.profile.trendVsSevenDaysAgo
import ph.mart.healthapp.core.data.progress.WeightEntry

/**
 * "How did this week go" — the one thing neither Home (today) nor the charts (1M–1Y) answer.
 * Derived, never stored: every field is a fold over what the repositories already return, the
 * same way [ph.mart.healthapp.core.data.streak.streakStats] is.
 *
 * Feature-local because Progress is the only screen that shows it; the inputs are all
 * `:core:data` types, so nothing here leaks a feature type back down.
 */

/** Rolling, ending today — never a calendar week, which would report a half-empty Monday. */
const val RECAP_WINDOW_DAYS = 7

data class BestDay(val dateEpochDay: Long, val calories: Int)

/**
 * [daysLogged] uses the four-domain definition (food, water, weigh-in, or exercise) so this card
 * can never disagree with the streak about what a logged day is. [NutritionAverages.daysLogged]
 * inside [averages] is the narrower food-only count, and the two can differ — the card says so
 * rather than quietly reporting an average over a different denominator.
 */
data class WeeklyRecap(
    val daysLogged: Int,
    val averages: NutritionAverages,
    val targets: DailyTargets?,
    /** Null when nothing was weighed inside the window — see [weeklyRecap]. */
    val weightTrend: WeightTrendDisplay?,
    /** Null when nothing was felt-logged inside the window, for the same reason [weightTrend] is
     * dropped: a card headed "Last 7 days" must not report a number from outside them. */
    val moodAverages: MoodAverages?,
    val bestDay: BestDay?,
)

/**
 * Null when nothing at all was logged in the window: the screen omits the card rather than
 * rendering an all-zero recap on day one.
 *
 * [dailyNutrition] is the dense series that ends today, so the window is a plain tail slice —
 * no date math, same as the Nutrition tab's range slicing.
 *
 * The weight delta reuses [trendVsSevenDaysAgo] (and its by-date backdating guard), but is
 * dropped when the newest weigh-in predates the window: that function anchors to the latest
 * *entry*, and a card headed "Last 7 days" must not report a delta between two entries from two
 * months ago.
 */
fun weeklyRecap(
    dailyNutrition: List<DayNutrition>,
    activeDays: Set<Long>,
    weightEntries: List<WeightEntry>,
    moodDays: List<MoodDay>,
    targets: DailyTargets?,
    todayEpochDay: Long,
): WeeklyRecap? {
    val windowStart = todayEpochDay - (RECAP_WINDOW_DAYS - 1)
    val daysLogged = (windowStart..todayEpochDay).count { it in activeDays }
    if (daysLogged == 0) return null

    val window = dailyNutrition.takeLast(RECAP_WINDOW_DAYS)
    val lastWeighIn = weightEntries.maxOfOrNull { it.dateEpochDay }
    return WeeklyRecap(
        daysLogged = daysLogged,
        averages = window.averages(),
        targets = targets,
        // The fallback weight is unreachable here: no entries means no weigh-in in the
        // window, which is already the null branch.
        weightTrend = if (lastWeighIn != null && lastWeighIn >= windowStart) {
            weightEntries.trendVsSevenDaysAgo(fallbackKg = 0.0)
        } else {
            null
        },
        // Sliced by date, not by tail: the mood series is sparse, so its last seven rows could
        // reach back months.
        moodAverages = moodDays
            .filter { it.dateEpochDay in windowStart..todayEpochDay }
            .takeIf { it.isNotEmpty() }
            ?.moodAverages(),
        bestDay = targets?.let { window.bestDay(it.calories) },
    )
}

/** Closest to the calorie target among logged days; ties break to the more recent day. There is
 * deliberately no worst day — naming one is a shaming pattern this app avoids everywhere else. */
private fun List<DayNutrition>.bestDay(targetCalories: Int): BestDay? =
    filter { it.isLogged }
        .minWithOrNull(
            compareBy<DayNutrition> { abs(it.calories - targetCalories) }
                .thenByDescending { it.dateEpochDay },
        )
        ?.let { BestDay(dateEpochDay = it.dateEpochDay, calories = it.calories) }
