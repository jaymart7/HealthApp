package ph.mart.healthapp.core.data.profile

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
