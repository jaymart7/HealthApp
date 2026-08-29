package ph.mart.healthapp.feature.home.ui

import kotlin.math.abs
import ph.mart.healthapp.core.data.food.DiaryTotals
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.streak.StreakStats
import ph.mart.healthapp.core.data.water.DEFAULT_WATER_GOAL_GLASSES

/**
 * Read model. Every field traces back to a repository interface: [profile] from
 * `ProfileRepository`, [totals] from `FoodRepository`, [weightEntries] and [lastPhotoEpochDay]
 * from `ProgressRepository`, [waterGlasses] from `WaterRepository`. Targets are never stored here — they're derived
 * from [profile] at read time via `dailyTargets()`, so they can't drift from the inputs that
 * produce them. [streak] and [weightProgressKg] are derived the same way, from all three
 * repositories at once — nothing about consistency is stored.
 */
data class HomeUiState(
    val profile: Profile? = null,
    val totals: DiaryTotals = DiaryTotals(0, 0, 0, 0),
    val foodEntryCount: Int = 0,
    val weightEntries: List<WeightEntry> = emptyList(),
    val lastPhotoEpochDay: Long? = null,
    val waterGlasses: Int = 0,
    val waterGoalGlasses: Int = DEFAULT_WATER_GOAL_GLASSES,
    val streak: StreakStats = StreakStats(current = 0, best = 0, totalDaysLogged = 0),
    /** Null when the goal is Maintain (no direction to move) or nothing has been weighed yet. */
    val weightProgressKg: Double? = null,
)

/** The one thing Home writes: today's glass count. Everything else on the screen is read-only. */
sealed interface HomeEvent {
    data class OnSetWaterGlasses(val glasses: Int) : HomeEvent
}

/** Day one = nothing logged anywhere yet. The profile alone doesn't count — it always exists by
 * the time Home is reachable. */
val HomeUiState.isDayOne: Boolean
    get() = foodEntryCount == 0 && weightEntries.isEmpty() && lastPhotoEpochDay == null &&
        waterGlasses == 0

/** [hasPrior] false means there's no entry 7+ days back to compare against — the card shows
 * "No prior data" rather than a false 0.0 delta. */
data class WeightTrendDisplay(val currentKg: Double, val deltaKg: Double, val hasPrior: Boolean)

/** Below this the arrow renders as a neutral dash — the delta is real but too small to call a
 * direction. Display-only: the *color* still comes from the shared `goalRelativeTrend()`. */
const val TREND_ARROW_DEADBAND_KG = 0.2

/**
 * Latest weight vs. the newest entry at least 7 days older — by **date**, never by insertion
 * order, so backdating a past weight can't produce a bogus trend. [fallbackKg] (the onboarding
 * weight from the profile) stands in when nothing has been logged yet.
 */
fun List<WeightEntry>.trendVsSevenDaysAgo(fallbackKg: Double): WeightTrendDisplay {
    val latest = maxByOrNull { it.dateEpochDay }
        ?: return WeightTrendDisplay(currentKg = fallbackKg, deltaKg = 0.0, hasPrior = false)
    val prior = filter { it.dateEpochDay <= latest.dateEpochDay - 7 }.maxByOrNull { it.dateEpochDay }
        ?: return WeightTrendDisplay(currentKg = latest.weightKg, deltaKg = 0.0, hasPrior = false)
    return WeightTrendDisplay(
        currentKg = latest.weightKg,
        deltaKg = latest.weightKg - prior.weightKg,
        hasPrior = true,
    )
}

/** Days between the most recent photo and today. Null when there are no photos at all. */
fun daysSincePhoto(lastPhotoEpochDay: Long?, todayEpochDay: Long): Long? =
    lastPhotoEpochDay?.let { (todayEpochDay - it).coerceAtLeast(0) }

/**
 * The one insight line, derived from the day's real numbers rather than a hardcoded string.
 * First matching rule wins; null hides the card entirely. Deliberately not a model call — a
 * Gemini-backed insight is a separate piece of work, not Phase 7 assembly.
 */
fun insightFor(totals: DiaryTotals, targets: DailyTargets, trend: WeightTrendDisplay): String? = when {
    totals.calories > targets.calories ->
        "You're ${totals.calories - targets.calories} kcal over today's target."
    targets.proteinG > 0 && totals.calories > 0 && totals.proteinG < targets.proteinG * 0.6 ->
        "You're ${targets.proteinG - totals.proteinG}g short on protein today."
    trend.hasPrior && abs(trend.deltaKg) >= TREND_ARROW_DEADBAND_KG ->
        "${formatDelta(trend.deltaKg)} kg over the last week — keep it steady."
    else -> null
}

/** Signed, one decimal, tabular-friendly — e.g. "-0.6", "+1.2". */
fun formatDelta(deltaKg: Double): String = "%+.1f".format(deltaKg)

/** Greeting copy is the prototype's verbatim, keyed off the local hour. */
fun greetingFor(hour: Int): String {
    val part = when {
        hour < 12 -> "morning"
        hour < 18 -> "afternoon"
        else -> "evening"
    }
    val line = when {
        hour < 12 -> "Ready for breakfast?"
        hour < 18 -> "How's the day going?"
        else -> "Almost there for today."
    }
    return "Good $part! $line"
}
