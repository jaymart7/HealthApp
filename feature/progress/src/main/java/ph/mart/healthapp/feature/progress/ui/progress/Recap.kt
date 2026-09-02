package ph.mart.healthapp.feature.progress.ui.progress

import kotlin.math.abs
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.LiftRecord
import ph.mart.healthapp.core.data.exercise.StrengthTotals
import ph.mart.healthapp.core.data.exercise.personalRecords
import ph.mart.healthapp.core.data.exercise.strengthTotals
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.food.NutritionAverages
import ph.mart.healthapp.core.data.food.averages
import ph.mart.healthapp.core.data.health.DEFAULT_STEP_GOAL
import ph.mart.healthapp.core.data.health.StepAverages
import ph.mart.healthapp.core.data.health.StepDay
import ph.mart.healthapp.core.data.health.burnSeries
import ph.mart.healthapp.core.data.health.stepAverages
import ph.mart.healthapp.core.data.mood.MoodAverages
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.mood.moodAverages
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.WeightTrendDisplay
import ph.mart.healthapp.core.data.profile.trendVsSevenDaysAgo
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.progress.WeightEntry

/**
 * "How did this stretch go" — the one thing neither Home (today) nor the charts (one metric at a
 * time) answer. Derived, never stored: every field is a fold over what the repositories already
 * return, the same way [ph.mart.healthapp.core.data.streak.streakStats] is.
 *
 * Feature-local because Progress is the only screen that shows it; the inputs are all
 * `:core:data` types, so nothing here leaks a feature type back down.
 */

/**
 * The three windows a recap can cover, all rolling and ending today — never a calendar week or
 * month, which would report a half-empty Monday or a half-empty first of the month.
 *
 * Deliberately *not* [ph.mart.healthapp.core.data.progress.ChartRange]: that has no week, and its
 * `inRange` helpers anchor a weight series to the latest *entry* rather than to today, which is
 * exactly the thing a card headed "Last 30 days" must not do.
 *
 * [days] stops at a year because that is how far back the inputs themselves reach —
 * `observeDailyNutrition()` is a dense year and the logged-day sets are windowed to
 * [ph.mart.healthapp.core.data.streak.STREAK_WINDOW_DAYS].
 */
enum class RecapPeriod(val short: String, val label: String, val days: Int) {
    Week("Week", "Last 7 days", 7),
    Month("Month", "Last 30 days", 30),
    Year("Year", "Last 365 days", 365),
}

/** The window the Progress screen's own card always shows — the recap as it shipped. */
val DEFAULT_RECAP_PERIOD = RecapPeriod.Week

data class BestDay(val dateEpochDay: Long, val calories: Int)

/**
 * [daysLogged] uses the four-domain definition (food, water, weigh-in, or exercise) so this card
 * can never disagree with the streak about what a logged day is. [NutritionAverages.daysLogged]
 * inside [averages] is the narrower food-only count, and the two can differ — the card says so
 * rather than quietly reporting an average over a different denominator.
 *
 * Everything from [startWeightKg] down is drawn by the recap *screen* only; the card on the
 * Progress tab renders the six fields above them and nothing else, which is what keeps the
 * shipped weekly card unchanged.
 */
data class Recap(
    val period: RecapPeriod,
    val daysLogged: Int,
    val averages: NutritionAverages,
    val targets: DailyTargets?,
    /** Null when nothing was weighed inside the window — see [recap]. */
    val weightTrend: WeightTrendDisplay?,
    /** Null when nothing was felt-logged inside the window, for the same reason [weightTrend] is
     * dropped: a card headed "Last 7 days" must not report a number from outside them. */
    val moodAverages: MoodAverages?,
    val bestDay: BestDay?,
    /** The window's first and last weigh-in. Both null when nothing was weighed in it, and equal
     * when it holds exactly one — an arc needs two ends, and inventing the other would be a
     * delta nobody measured. */
    val startWeightKg: Double? = null,
    val endWeightKg: Double? = null,
    val strength: StrengthTotals = StrengthTotals(workouts = 0, sets = 0, volumeKg = 0.0),
    /** The most recently set record among the lifts trained in the window — [personalRecords] is
     * already ranked by estimated 1RM and ordered newest-first. */
    val topLift: LiftRecord? = null,
    val burnedKcal: Int = 0,
    val workouts: Int = 0,
    val steps: StepAverages = StepAverages(averageSteps = null, bestSteps = null, daysHitGoal = 0, days = 0),
    val photos: List<ProgressPhoto> = emptyList(),
) {
    /** Kilograms between the window's two ends, or null when it holds fewer than two weigh-ins. */
    val weightArcKg: Double?
        get() = if (startWeightKg != null && endWeightKg != null && startWeightKg != endWeightKg) {
            endWeightKg - startWeightKg
        } else {
            null
        }
}

/**
 * Null when nothing at all was logged in the window: the caller omits the card rather than
 * rendering an all-zero recap on day one.
 *
 * [dailyNutrition] is the dense series that ends today, so its window is a plain tail slice — no
 * date math, same as the Nutrition tab's range slicing. Every *sparse* input is sliced by date
 * instead, because its last rows could reach back months.
 *
 * The weight delta reuses [trendVsSevenDaysAgo] (and its by-date backdating guard), but is
 * dropped when the newest weigh-in predates the window: that function anchors to the latest
 * *entry*, and a card headed "Last 7 days" must not report a delta between two entries from two
 * months ago. It stays a seven-day figure on every period — [weightArcKg] is the window's own
 * answer, and the screen labels which is which.
 */
fun recap(
    period: RecapPeriod,
    dailyNutrition: List<DayNutrition>,
    activeDays: Set<Long>,
    weightEntries: List<WeightEntry>,
    moodDays: List<MoodDay>,
    targets: DailyTargets?,
    todayEpochDay: Long,
    exerciseEntries: List<ExerciseEntry> = emptyList(),
    stepDays: List<StepDay> = emptyList(),
    stepGoal: Int = DEFAULT_STEP_GOAL,
    photos: List<ProgressPhoto> = emptyList(),
): Recap? {
    val windowStart = todayEpochDay - (period.days - 1)
    val window = windowStart..todayEpochDay
    val daysLogged = window.count { it in activeDays }
    if (daysLogged == 0) return null

    val nutrition = dailyNutrition.takeLast(period.days)
    val weighIns = weightEntries.filter { it.dateEpochDay in window }.sortedBy { it.dateEpochDay }
    val exercise = exerciseEntries.filter { it.dateEpochDay in window }
    val steps = stepDays.filter { it.dateEpochDay in window }
    val lastWeighIn = weightEntries.maxOfOrNull { it.dateEpochDay }

    return Recap(
        period = period,
        daysLogged = daysLogged,
        averages = nutrition.averages(),
        targets = targets,
        // The fallback weight is unreachable here: no entries means no weigh-in in the
        // window, which is already the null branch.
        weightTrend = if (lastWeighIn != null && lastWeighIn >= windowStart) {
            weightEntries.trendVsSevenDaysAgo(fallbackKg = 0.0)
        } else {
            null
        },
        moodAverages = moodDays
            .filter { it.dateEpochDay in window }
            .takeIf { it.isNotEmpty() }
            ?.moodAverages(),
        bestDay = targets?.let { nutrition.bestDay(it.calories) },
        startWeightKg = weighIns.firstOrNull()?.weightKg,
        endWeightKg = weighIns.lastOrNull()?.weightKg,
        strength = exercise.strengthTotals(),
        topLift = exercise.personalRecords().firstOrNull(),
        // Both halves are already windowed, so the series it folds is too — and pricing it here
        // rather than summing `exercise.burnedKcal` is what stops an imported walk being counted
        // twice against its own step total.
        burnedKcal = burnSeries(steps, exercise).sumOf { it.burnedKcal },
        workouts = exercise.size,
        steps = steps.stepAverages(stepGoal),
        photos = photos.filter { it.dateEpochDay in window },
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
