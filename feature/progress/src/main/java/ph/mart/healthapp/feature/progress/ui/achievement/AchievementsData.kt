package ph.mart.healthapp.feature.progress.ui.achievement

import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.profile.UnitSystem

/**
 * The five figures [badgeGroups] takes, and the unit the weight tiers are quoted in — a **derived**
 * read model rather than a slice of anything.
 *
 * That is what makes this page's container worth having despite reading six repositories: none of
 * those series reaches the screen. A year of workouts becomes [workoutCount], the whole photo set
 * becomes [photoCount], four domains' logged days become [activeDays], and the profile becomes one
 * nullable [weightProgressKg] plus a unit. The page draws badges, not data.
 *
 * [activeDays] stays a set rather than a streak, because the streak is read against *today* at
 * draw time — a `StreakStats` folded when the flow was built would freeze at whatever day the app
 * was opened. `HomeViewModel` makes the same call for the same reason.
 */
data class AchievementsUiState(
    val activeDays: Set<Long> = emptySet(),
    val weightProgressKg: Double? = null,
    val workoutCount: Int = 0,
    val fasts: List<FastSession> = emptyList(),
    val photoCount: Int = 0,
    val unit: UnitSystem = UnitSystem.Metric,
)
