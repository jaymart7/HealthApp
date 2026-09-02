package ph.mart.healthapp.core.data.profile

import kotlin.math.roundToInt
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.food.averages
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.progress.slopeKgPerDay

/**
 * "Was the formula right about me?" — the one question [calculateDailyTargets] can't answer about
 * its own output. Mifflin–St Jeor is a population average multiplied by an activity level the user
 * picked for themselves; for any given person it is routinely a couple of hundred kcal out. What
 * the app holds is the correction: four weeks of logged intake and a weigh-in series, which
 * together *measure* maintenance instead of estimating it.
 *
 * Derived, never stored — no table, no schema, the same shape as
 * [ph.mart.healthapp.core.data.progress.goalProjection] and `streakStats`. Applying the answer
 * writes [Profile.calorieOverrideKcal], the column a manual target already uses, so there is no
 * second kind of pinned target to reconcile and "Reset to calculated" already undoes it.
 *
 * It lives in this package rather than beside `goalProjection` because what it produces is a
 * calorie target: the goal adjustments and both clamps it needs are [DailyTargets]'.
 */

/** Four whole weeks, deliberately not the projection's 30 days: a 30-day window holds four extra
 * weekdays, and weekend intake is systematically higher than midweek intake. A mean over 28 days
 * weights every day of the week equally. */
const val CHECKIN_WINDOW_DAYS = 28L

/** Half the window. The estimate divides by what was *logged*, so it assumes the unlogged days
 * looked like the logged ones — an assumption that stops being defensible below this. */
const val MIN_CHECKIN_LOGGED_DAYS = 14

/** [MIN_CHECKIN_WEIGH_INS] over [MIN_CHECKIN_SPAN_DAYS], for
 * [ph.mart.healthapp.core.data.progress.MIN_PROJECTION_SPAN_DAYS]' reasoning: a rate fitted to two
 * mornings two days apart is a rate about hydration. */
const val MIN_CHECKIN_WEIGH_INS = 4
const val MIN_CHECKIN_SPAN_DAYS = 14L

/** A check-in anchored at today must not be driven off a three-week-old weigh-in — the same seven
 * days, for the same reason, as [ph.mart.healthapp.core.data.progress.PROJECTION_STALE_DAYS]. */
const val CHECKIN_STALE_DAYS = 7L

/** The energy in a kilogram of body mass, the standard figure. Approximate by nature — it is why
 * the check-in reports a target to adjust to and not a promise. */
const val KCAL_PER_KG = 7700.0

/** A window that is dense enough to pass every guard above can still produce nonsense from a fast
 * water swing. Outside this the arithmetic is refused rather than recommended. */
val MAINTENANCE_SANITY_KCAL = 1000..6000

/** Under this the adjustment is inside the noise of the estimate itself, so the screen reports the
 * measurement and offers no button. Applying drives the delta to zero, which is what lets the call
 * to action clear itself with nothing persisted anywhere. */
const val MIN_MEANINGFUL_DELTA_KCAL = 75

/**
 * The measurement, when there is enough data to make one. Null [estimate] is the ordinary case
 * early on: the counts above it are still reported, so the screen can say what is missing rather
 * than showing an empty page.
 */
data class EnergyCheckIn(
    val windowDays: Int,
    /** Days with food logged inside the window — the denominator [avgIntakeKcal] was divided by. */
    val daysLogged: Int,
    val weighIns: Int,
    val avgIntakeKcal: Int,
    /** What [Profile.dailyTargets] says right now, so the comparison is always against the figure
     * the user actually sees on Home. */
    val currentTargetKcal: Int,
    val estimate: EnergyEstimate?,
)

/**
 * [maintenanceKcal] is total expenditure — it already contains whatever the user trained, because
 * it was measured from what their body actually did. That is why applying it while
 * [Profile.addExerciseToBudget] is on counts a workout twice, and why the screen says so.
 *
 * [deltaKcal] is signed against the target in force: positive means the formula has been
 * underfeeding them.
 */
data class EnergyEstimate(
    val maintenanceKcal: Int,
    val recommendedKcal: Int,
    val deltaKcal: Int,
    /** The fitted trend the measurement came from, signed (negative = losing). */
    val kgPerWeek: Double,
    /** The raw recommendation fell under the Mifflin–St Jeor safety floor and was raised to it —
     * [calculateDailyTargets]' own clamp, reported rather than applied silently. */
    val clampedToFloor: Boolean,
)

/**
 * Null when the window holds no logged food at all — there is then nothing to report, not even a
 * count, the call [ph.mart.healthapp.core.data.progress.goalProjection] makes on an empty series.
 *
 * The measurement is one line: intake minus the energy the weight change accounts for. Losing
 * weight is a negative slope, which *adds* the deficit back onto what was eaten.
 */
fun energyCheckIn(
    dailyNutrition: List<DayNutrition>,
    weightEntries: List<WeightEntry>,
    profile: Profile,
    todayEpochDay: Long,
): EnergyCheckIn? {
    // Sliced against today rather than against the latest entry: a page headed "last 28 days" has
    // to mean the last 28 days, the rule `recap` follows and `List<WeightEntry>.inRange` does not.
    val window = (todayEpochDay - (CHECKIN_WINDOW_DAYS - 1))..todayEpochDay
    val intake = dailyNutrition.filter { it.dateEpochDay in window }.averages()
    if (intake.daysLogged == 0) return null

    val weighIns = weightEntries.filter { it.dateEpochDay in window }
    val targets = profile.dailyTargets()
    return EnergyCheckIn(
        windowDays = CHECKIN_WINDOW_DAYS.toInt(),
        daysLogged = intake.daysLogged,
        weighIns = weighIns.size,
        avgIntakeKcal = intake.calories,
        currentTargetKcal = targets.calories,
        estimate = estimate(
            avgIntakeKcal = intake.calories,
            daysLogged = intake.daysLogged,
            weighIns = weighIns,
            goal = profile.goal,
            targets = targets,
            todayEpochDay = todayEpochDay,
        ),
    )
}

/** Every guard in one place; any one of them failing means the honest answer is "not yet". */
private fun estimate(
    avgIntakeKcal: Int,
    daysLogged: Int,
    weighIns: List<WeightEntry>,
    goal: Goal,
    targets: DailyTargets,
    todayEpochDay: Long,
): EnergyEstimate? {
    if (daysLogged < MIN_CHECKIN_LOGGED_DAYS) return null
    if (weighIns.size < MIN_CHECKIN_WEIGH_INS) return null

    val latest = weighIns.maxBy { it.dateEpochDay }
    val oldest = weighIns.minBy { it.dateEpochDay }
    if (latest.dateEpochDay - oldest.dateEpochDay < MIN_CHECKIN_SPAN_DAYS) return null
    if (latest.dateEpochDay < todayEpochDay - CHECKIN_STALE_DAYS) return null

    // The projection's own least-squares fit, reused rather than repeated: one fitting method
    // means the check-in and the goal projection can never disagree about the rate.
    val slope = weighIns.slopeKgPerDay() ?: return null
    val maintenance = (avgIntakeKcal - slope * KCAL_PER_KG).roundToInt()
    if (maintenance !in MAINTENANCE_SANITY_KCAL) return null

    val raw = maintenance + (GOAL_ADJUSTMENT_KCAL[goal] ?: 0)
    val recommended = raw.coerceAtLeast(targets.floor).coerceIn(CALORIE_TARGET_KCAL)
    return EnergyEstimate(
        maintenanceKcal = maintenance,
        recommendedKcal = recommended,
        deltaKcal = recommended - targets.calories,
        kgPerWeek = slope * 7,
        clampedToFloor = raw < targets.floor,
    )
}
