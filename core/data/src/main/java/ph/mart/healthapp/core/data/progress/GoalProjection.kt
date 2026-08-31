package ph.mart.healthapp.core.data.progress

import kotlin.math.abs
import kotlin.math.ceil
import ph.mart.healthapp.core.data.profile.Goal

/**
 * "At this rate, when do I get there?" — the one thing the goal line on the chart and the
 * "Goal remaining" cell never answer. Derived, never stored: a fold over the weight entries
 * the screen already holds, the same way `streakStats` is — no table, no repository, no schema.
 *
 * It lives here rather than in a feature because three screens show it now: Progress's Weight
 * tab, Progress's weekly recap, and Home's weight card. `:feature:*` modules never import each
 * other, so `:core:data` is the only place all three can reach.
 */

/** Long enough that a single bad weigh-in can't steer the fit, short enough to reflect what the
 * user is doing *now* rather than a diet they abandoned in spring. */
const val PROJECTION_WINDOW_DAYS = 30L

const val MIN_PROJECTION_ENTRIES = 3

/** Two weigh-ins two days apart fit a slope of several kg/week. Refuse to project until the
 * window is wide enough for the rate to mean something. */
const val MIN_PROJECTION_SPAN_DAYS = 14L

/** A projection anchored at today must not be driven off a three-week-old weigh-in. Same seven
 * days, for the same reason, as the weekly recap's blank weight cell — declared here rather than
 * shared with it, because the recap's window is feature-local and means something else. */
const val PROJECTION_STALE_DAYS = 7L

/** Below this the trend is flat: the arithmetic would still hand back a date, but it would be
 * years out and would swing wildly on the next entry. */
const val PROJECTION_FLAT_KG_PER_WEEK = 0.1

/** Past a year the date is noise dressed as precision. */
const val MAX_PROJECTION_DAYS = 365L

/**
 * [kgPerWeek] is signed (negative = losing) and always reported, even when there is no date —
 * knowing the rate is useful precisely when the projection can't be made.
 *
 * [targetEpochDay] is null whenever no honest date exists: the trend is flat, it points away
 * from the goal, or it lands beyond [MAX_PROJECTION_DAYS]. The card says so in one neutral
 * line rather than hiding — the user should be able to tell "no date" from "no data".
 */
data class GoalProjection(
    val goalWeightKg: Double,
    val kgPerWeek: Double,
    val targetEpochDay: Long?,
    val reached: Boolean,
)

/**
 * Null means the card is omitted entirely — there is nothing truthful to say yet, the same
 * call the weekly recap makes on an empty week.
 *
 * The rate is a least-squares slope over the window rather than first-vs-last: every point
 * counts, so one dehydrated morning can't flip the sign. It is order-independent by
 * construction, which is what makes backdating safe here (the property [
 * ph.mart.healthapp.core.data.profile.trendVsSevenDaysAgo] gets by comparing dates).
 *
 * The current weight is the latest *entry*, not the fitted value at today, so this and the
 * "Goal remaining" cell above it on the Weight tab can never disagree about where the user is.
 */
fun goalProjection(
    weightEntries: List<WeightEntry>,
    goalWeightKg: Double?,
    goal: Goal?,
    todayEpochDay: Long,
): GoalProjection? {
    if (goalWeightKg == null || goal == null || goal == Goal.Maintain) return null

    val window = weightEntries.filter { it.dateEpochDay in (todayEpochDay - PROJECTION_WINDOW_DAYS)..todayEpochDay }
    if (window.size < MIN_PROJECTION_ENTRIES) return null

    val latest = window.maxBy { it.dateEpochDay }
    val oldest = window.minBy { it.dateEpochDay }
    if (latest.dateEpochDay - oldest.dateEpochDay < MIN_PROJECTION_SPAN_DAYS) return null
    if (latest.dateEpochDay < todayEpochDay - PROJECTION_STALE_DAYS) return null

    val slope = window.slopeKgPerDay() ?: return null
    val kgPerWeek = slope * 7
    val remaining = goalWeightKg - latest.weightKg
    val reached = if (goal == Goal.Lose) latest.weightKg <= goalWeightKg else latest.weightKg >= goalWeightKg

    // Opposite signs means the trend points away from the goal; equal signs guarantee days > 0.
    val days = if (reached || abs(kgPerWeek) < PROJECTION_FLAT_KG_PER_WEEK || remaining * slope <= 0) {
        null
    } else {
        ceil(remaining / slope).toLong().takeIf { it <= MAX_PROJECTION_DAYS }
    }

    return GoalProjection(
        goalWeightKg = goalWeightKg,
        kgPerWeek = kgPerWeek,
        targetEpochDay = days?.let { todayEpochDay + it },
        reached = reached,
    )
}

/** Least-squares slope of weight over day, x centred on the mean day. Null when every entry
 * shares one date — impossible past the span guard, but division is division. */
private fun List<WeightEntry>.slopeKgPerDay(): Double? {
    val meanDay = sumOf { it.dateEpochDay.toDouble() } / size
    val meanKg = sumOf { it.weightKg } / size
    val denominator = sumOf { (it.dateEpochDay - meanDay) * (it.dateEpochDay - meanDay) }
    if (denominator == 0.0) return null
    return sumOf { (it.dateEpochDay - meanDay) * (it.weightKg - meanKg) } / denominator
}
