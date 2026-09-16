package ph.mart.healthapp.feature.progress.ui.nutrition

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.weekBudget
import ph.mart.healthapp.core.data.health.StepsRepository
import ph.mart.healthapp.core.data.health.burnSeries
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.data.profile.nutrientTargets
import ph.mart.healthapp.core.data.todayEpochDay

/**
 * The Nutrition page's container — read-only, five flows, `WeightViewModel`'s shape.
 *
 * It is the first of the thirteen to copy a **fold** rather than a flow: the two target sets are
 * computed live off the profile, never stored, so this makes the same two `:core:data/profile`
 * calls `ProgressViewModel` makes. Reusing those functions rather than re-deriving is what stops
 * the overview's card and this page quoting different targets.
 *
 * The last two flows are movement, and they are here for one figure: the week's bank prices a past
 * day's budget through `burnSeries()`, exactly as Home does, so the two surfaces cannot report
 * different banks.
 */
class NutritionViewModel(
    foodRepository: FoodRepository,
    profileRepository: ProfileRepository,
    stepsRepository: StepsRepository,
    exerciseRepository: ExerciseRepository,
) : ViewModel(), OrbitContainerHost<NutritionUiState, NutritionUiState, Nothing> {

    override val container = orbitContainer<NutritionUiState, Nothing>(NutritionUiState()) {
        observeNutrition(foodRepository, profileRepository, stepsRepository, exerciseRepository)
    }

    private fun observeNutrition(
        foodRepository: FoodRepository,
        profileRepository: ProfileRepository,
        stepsRepository: StepsRepository,
        exerciseRepository: ExerciseRepository,
    ) = intent {
        combine(
            foodRepository.observeDailyNutrition(),
            foodRepository.observeMealPhotos(),
            profileRepository.observeProfile(),
            stepsRepository.observeDays(),
            exerciseRepository.observeRecentEntries(),
        ) { dailyNutrition, mealPhotos, profile, stepDays, exercise ->
            NutritionUiState(
                dailyNutrition = dailyNutrition,
                mealPhotos = mealPhotos,
                targets = profile?.dailyTargets(),
                nutrientTargets = profile?.let { nutrientTargets(it, it.dailyTargets()) },
                // Folded here beside the targets, and the clock read on every emission for
                // `HomeViewModel`'s reason: a page left open past midnight must not keep scoring
                // last week.
                weekBudget = profile?.let {
                    weekBudget(
                        nutrition = dailyNutrition,
                        burn = burnSeries(stepDays, exercise),
                        targets = it.dailyTargets(),
                        addExerciseToBudget = it.addExerciseToBudget,
                        todayEpochDay = todayEpochDay(),
                    )
                },
            )
        }.collect { newState -> reduce { newState } }
    }
}
