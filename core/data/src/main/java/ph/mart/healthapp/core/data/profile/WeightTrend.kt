package ph.mart.healthapp.core.data.profile

import ph.mart.healthapp.core.data.progress.WeightEntry

/** Semantic only — no color here (core:data stays framework-free). UI maps this to
 * onSurfaceVariant/primary/error per CLAUDE.md's "never default to green-for-loss/red-for-gain"
 * rule. Shared by Progress's WeightStatRow/MeasurementRow now and Home's WeightMetricCard later
 * (Phase 7) — one source of truth, not a color decision forked per screen. */
enum class TrendDirection { Neutral, OnTrack, OffTrack }

/** [deltaKg] is "current minus prior" — negative means the value went down. [Goal.Maintain] stays
 * [TrendDirection.Neutral] regardless of direction; there's no defined "how much drift is too
 * much" threshold to call it off-track. */
fun goalRelativeTrend(goal: Goal?, deltaKg: Double): TrendDirection = when {
    deltaKg == 0.0 || goal == null || goal == Goal.Maintain -> TrendDirection.Neutral
    goal == Goal.Lose -> if (deltaKg < 0) TrendDirection.OnTrack else TrendDirection.OffTrack
    else -> if (deltaKg > 0) TrendDirection.OnTrack else TrendDirection.OffTrack
}

/** [hasPrior] false means there's no entry 7+ days back to compare against — the caller shows
 * "No prior data" rather than a false 0.0 delta. */
data class WeightTrendDisplay(val currentKg: Double, val deltaKg: Double, val hasPrior: Boolean)

/** Below this the arrow renders as a neutral dash — the delta is real but too small to call a
 * direction. Display-only: the *color* still comes from [goalRelativeTrend]. */
const val TREND_ARROW_DEADBAND_KG = 0.2

/**
 * Latest weight vs. the newest entry at least 7 days older — by **date**, never by insertion
 * order, so backdating a past weight can't produce a bogus trend. [fallbackKg] (the onboarding
 * weight from the profile) stands in when nothing has been logged yet.
 *
 * Note the anchor is the latest *entry*, not today: a series that stops a month ago still
 * reports the delta across its own last week. Callers that mean "this week" (Progress's weekly
 * recap) check the latest date themselves.
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
