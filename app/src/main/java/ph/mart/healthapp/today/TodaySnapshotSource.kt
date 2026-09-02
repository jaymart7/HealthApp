package ph.mart.healthapp.today

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import ph.mart.healthapp.core.data.epochDayOf
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.exercise.budgetKcal
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.fasting.goalReachedMillis
import ph.mart.healthapp.core.data.food.DiaryTotals
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.dailyTotals
import ph.mart.healthapp.core.data.health.StepDay
import ph.mart.healthapp.core.data.health.StepsRepository
import ph.mart.healthapp.core.data.health.dayBurnedKcal
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.streak.loggedDays
import ph.mart.healthapp.core.data.streak.streakStats
import ph.mart.healthapp.core.data.todayFlow
import ph.mart.healthapp.core.data.water.WaterRepository
import ph.mart.healthapp.core.data.water.waterVolumeLabel
import ph.mart.healthapp.core.today.TodaySnapshot

/**
 * The one place `:core:data` becomes a [TodaySnapshot]. Three surfaces draw that snapshot — the
 * home-screen widget, the watch app and the watch tile — and they reach it through this file, so
 * none of them can compute the day differently from Home.
 *
 * It lives in `:app` beside `reminder/` and `widget/` for the reason those do: these are system
 * surfaces, not screens, and only `:app` sees both `:core:data` and every surface at once.
 */

/**
 * The day's burn folds in through `budgetKcal()` — the one place exercise touches the day's
 * budget — so no surface can drift from Home's ring. That is also why [exercise] and [steps]
 * arrive raw rather than pre-summed: `dayBurnedKcal()` is the single combining rule, and calling
 * it here means a caller cannot combine them differently. [profile] null means onboarding is
 * unfinished.
 */
fun todaySnapshot(
    profile: Profile?,
    totals: DiaryTotals,
    glasses: Int,
    exercise: List<ExerciseEntry>,
    steps: StepDay?,
    streakDays: Int,
    activeFast: FastSession? = null,
    nowMillis: Long = System.currentTimeMillis(),
    dateEpochDay: Long = epochDayOf(nowMillis),
): TodaySnapshot {
    if (profile == null) return TodaySnapshot(dateEpochDay = dateEpochDay, onboarding = true)
    val targets = profile.dailyTargets()
    return TodaySnapshot(
        dateEpochDay = dateEpochDay,
        consumedKcal = totals.calories,
        budgetKcal = budgetKcal(targets.calories, dayBurnedKcal(exercise, steps), profile.addExerciseToBudget),
        glasses = glasses,
        goalGlasses = profile.waterGoalGlasses,
        // Converted here rather than on the watch: the profile is the phone's, and shipping
        // `UnitSystem` to the wrist would mean shipping `:core:data` with it.
        waterLabel = waterVolumeLabel(glasses, profile.preferredUnit),
        streakDays = streakDays,
        steps = steps?.steps ?: 0,
        fastingUntilMillis = activeFast?.goalReachedMillis,
        fastingGoalReached = activeFast != null && nowMillis >= activeFast.goalReachedMillis,
        darkThemeOn = profile.darkThemeOn,
    )
}

/**
 * Today, live. Collected twice: by the widget's Glance session, and by [ph.mart.healthapp.FitPulseApplication]
 * to redraw that widget and push to the watch. It was two near-identical `combine` chains before
 * the watch arrived, which is exactly how two surfaces come to disagree.
 *
 * [todayFlow] is in the combine for the streak's sake. Every other input is a today-only
 * repository overload, and those already re-point themselves at midnight; `streakStats` takes the
 * day as an argument, so without this a session left open overnight would keep scoring the streak
 * against yesterday.
 */
fun todaySnapshotFlow(
    profileRepository: ProfileRepository,
    foodRepository: FoodRepository,
    waterRepository: WaterRepository,
    exerciseRepository: ExerciseRepository,
    progressRepository: ProgressRepository,
    stepsRepository: StepsRepository,
    fastingRepository: FastingRepository,
): Flow<TodaySnapshot> {
    val activeDays = combine(
        foodRepository.observeDailyNutrition(),
        waterRepository.observeLoggedDays(),
        progressRepository.observeWeightEntries(),
        exerciseRepository.observeLoggedDays(),
        ::loggedDays,
    )
    // Paired ahead of the combine, which is already at the arity the typed overloads stop at.
    return combine(
        profileRepository.observeProfile(),
        foodRepository.observeTodayEntries(),
        combine(waterRepository.observeToday(), fastingRepository.observeActive(), ::Pair),
        combine(exerciseRepository.observeTodayEntries(), stepsRepository.observeToday(), ::Pair),
        combine(activeDays, todayFlow(), ::Pair),
    ) { profile, entries, (glasses, activeFast), (exercise, steps), (days, today) ->
        todaySnapshot(
            profile = profile,
            totals = entries.dailyTotals(),
            glasses = glasses,
            exercise = exercise,
            steps = steps,
            streakDays = days.streakStats(today).current,
            activeFast = activeFast,
            dateEpochDay = today,
        )
    }
}
