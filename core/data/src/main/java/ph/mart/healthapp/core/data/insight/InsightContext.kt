package ph.mart.healthapp.core.data.insight

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.food.DiaryTotals
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.dailyTotals
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.data.profile.trendVsSevenDaysAgo
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.streak.loggedDays
import ph.mart.healthapp.core.data.streak.streakStats
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.data.water.WaterRepository

/**
 * The one place an [InsightRequest] is assembled.
 *
 * Home builds it from state it has already combined for its cards, while the coach has no such
 * state and combines the flows below — but both land here, so the two features can never disagree
 * about what the model is told. `dailyTargets()` and `trendVsSevenDaysAgo()` are the same
 * derivations the cards draw with, so the answer is priced off exactly the numbers on screen.
 */
fun insightRequest(
    profile: Profile,
    totals: DiaryTotals,
    weightEntries: List<WeightEntry>,
    waterGlasses: Int,
    waterGoalGlasses: Int,
    streakDays: Int,
): InsightRequest {
    val targets = profile.dailyTargets()
    val trend = weightEntries.trendVsSevenDaysAgo(fallbackKg = profile.weightKg)
    return InsightRequest(
        goal = profile.goal,
        caloriesConsumed = totals.calories,
        caloriesTarget = targets.calories,
        proteinG = totals.proteinG,
        proteinTargetG = targets.proteinG,
        carbsG = totals.carbsG,
        carbsTargetG = targets.carbsG,
        fatG = totals.fatG,
        fatTargetG = targets.fatG,
        waterGlasses = waterGlasses,
        waterGoalGlasses = waterGoalGlasses,
        streakDays = streakDays,
        // Null rather than 0.0 with nothing to compare against — see [InsightRequest].
        weightDeltaKg = trend.deltaKg.takeIf { trend.hasPrior },
    )
}

/**
 * Today's payload as a flow, for a caller that isn't already holding the day.
 *
 * Two nested combines because the typed `combine` overloads stop at five and this needs seven —
 * the same shape `HomeViewModel.observeHome` uses. The streak's four series are folded with the
 * existing [loggedDays]/[streakStats], and `todayEpochDay()` is read on every emission rather than
 * once at construction so a session left open across midnight doesn't freeze the run.
 *
 * Null until the profile lands: there is no target to be over or under without one.
 */
fun observeInsightRequest(
    profileRepository: ProfileRepository,
    foodRepository: FoodRepository,
    progressRepository: ProgressRepository,
    waterRepository: WaterRepository,
    exerciseRepository: ExerciseRepository,
): Flow<InsightRequest?> {
    val streakDays = combine(
        foodRepository.observeDailyNutrition(),
        waterRepository.observeLoggedDays(),
        progressRepository.observeWeightEntries(),
        exerciseRepository.observeLoggedDays(),
        ::loggedDays,
    )
    return combine(
        profileRepository.observeProfile(),
        foodRepository.observeTodayEntries(),
        progressRepository.observeWeightEntries(),
        waterRepository.observeToday(),
        streakDays,
    ) { profile, entries, weightEntries, waterGlasses, days ->
        profile?.let {
            insightRequest(
                profile = it,
                totals = entries.dailyTotals(),
                weightEntries = weightEntries,
                waterGlasses = waterGlasses,
                waterGoalGlasses = it.waterGoalGlasses,
                streakDays = days.streakStats(todayEpochDay()).current,
            )
        }
    }
}
