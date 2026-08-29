package ph.mart.healthapp.widget

import ph.mart.healthapp.core.data.exercise.budgetKcal
import ph.mart.healthapp.core.data.food.DiaryTotals
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.data.water.DEFAULT_WATER_GOAL_GLASSES

/**
 * Everything the widget draws, and nothing it doesn't. Derived, never stored — same rule as
 * [ph.mart.healthapp.feature.home.ui.HomeUiState], for the same reason: a widget holding its own
 * copy of the calorie target is a widget that will one day disagree with Home.
 *
 * All the arithmetic lives in [todayWidgetState] rather than in the composable, because Glance
 * composables can only be exercised on a device and this can be unit-tested.
 */
data class TodayWidgetState(
    val consumedKcal: Int = 0,
    val budgetKcal: Int = 0,
    val glasses: Int = 0,
    val goalGlasses: Int = DEFAULT_WATER_GOAL_GLASSES,
    val unit: UnitSystem = UnitSystem.Metric,
    val streakDays: Int = 0,
    /** Null means follow the device, exactly as `Profile.darkThemeOn` does. */
    val darkThemeOn: Boolean? = null,
    /** No profile row yet — the user hasn't finished onboarding, so there are no targets to show. */
    val onboarding: Boolean = false,
)

/** Signed: negative once the day is over budget, which the widget says out loud rather than
 * clamping to zero. */
val TodayWidgetState.remainingKcal: Int get() = budgetKcal - consumedKcal

/** The bar's fill, 0f..1f. Clamped — unlike [remainingKcal] — because a bar can't overflow, and
 * a zero budget would otherwise divide by zero. */
val TodayWidgetState.progress: Float
    get() = if (budgetKcal > 0) (consumedKcal.toFloat() / budgetKcal).coerceIn(0f, 1f) else 0f

/** The count the +1 button writes. Capped at the goal, which is also where the button stops
 * being offered — the control never silently no-ops. */
val TodayWidgetState.glassesAfterAdd: Int get() = (glasses + 1).coerceAtMost(goalGlasses)

val TodayWidgetState.waterGoalReached: Boolean get() = glasses >= goalGlasses

/**
 * [burnedKcal] folds in through `budgetKcal()` — the one place exercise touches the day's budget —
 * so the widget and Home's ring cannot drift apart. [profile] null means onboarding is unfinished.
 */
fun todayWidgetState(
    profile: Profile?,
    totals: DiaryTotals,
    glasses: Int,
    burnedKcal: Int,
    streakDays: Int,
): TodayWidgetState {
    if (profile == null) return TodayWidgetState(onboarding = true)
    val targets = profile.dailyTargets()
    return TodayWidgetState(
        consumedKcal = totals.calories,
        budgetKcal = budgetKcal(targets.calories, burnedKcal, profile.addExerciseToBudget),
        glasses = glasses,
        goalGlasses = profile.waterGoalGlasses,
        unit = profile.preferredUnit,
        streakDays = streakDays,
        darkThemeOn = profile.darkThemeOn,
    )
}
