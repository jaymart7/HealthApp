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
import ph.mart.healthapp.core.data.supplement.SupplementRepository
import ph.mart.healthapp.core.data.todayEpochDay

/**
 * The Nutrition page's container — read-only, six flows in five slots, `WeightViewModel`'s shape.
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
    supplementRepository: SupplementRepository,
) : ViewModel(), OrbitContainerHost<NutritionUiState, NutritionUiState, Nothing> {

    override val container = orbitContainer<NutritionUiState, Nothing>(NutritionUiState()) {
        observeNutrition(
            foodRepository, profileRepository, stepsRepository, exerciseRepository,
            supplementRepository,
        )
    }

    private fun observeNutrition(
        foodRepository: FoodRepository,
        profileRepository: ProfileRepository,
        stepsRepository: StepsRepository,
        exerciseRepository: ExerciseRepository,
        supplementRepository: SupplementRepository,
    ) = intent {
        combine(
            foodRepository.observeDailyNutrition(),
            // Meal photos and the supplement figures pair up: this combine was already at the
            // arity its typed overloads stop at, and a Pair costs nothing where a sixth flow
            // would cost the whole shape — the diary's trick, one tab over.
            combine(
                foodRepository.observeMealPhotos(),
                supplementRepository.observeNutrientsByDay(),
                ::Pair,
            ),
            profileRepository.observeProfile(),
            stepsRepository.observeDays(),
            exerciseRepository.observeRecentEntries(),
        ) { dailyNutrition, (mealPhotos, supplementNutrients), profile, stepDays, exercise ->
            NutritionUiState(
                dailyNutrition = dailyNutrition,
                mealPhotos = mealPhotos,
                supplementNutrients = supplementNutrients,
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
