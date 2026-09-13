package ph.mart.healthapp.feature.progress.ui.nutrition

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.data.profile.nutrientTargets

/**
 * The Nutrition page's container — read-only, three flows, `WeightViewModel`'s shape.
 *
 * It is the first of the thirteen to copy a **fold** rather than a flow: the two target sets are
 * computed live off the profile, never stored, so this makes the same two `:core:data/profile`
 * calls `ProgressViewModel` makes. Reusing those functions rather than re-deriving is what stops
 * the overview's card and this page quoting different targets.
 */
class NutritionViewModel(
    foodRepository: FoodRepository,
    profileRepository: ProfileRepository,
) : ViewModel(), OrbitContainerHost<NutritionUiState, NutritionUiState, Nothing> {

    override val container = orbitContainer<NutritionUiState, Nothing>(NutritionUiState()) {
        observeNutrition(foodRepository, profileRepository)
    }

    private fun observeNutrition(
        foodRepository: FoodRepository,
        profileRepository: ProfileRepository,
    ) = intent {
        combine(
            foodRepository.observeDailyNutrition(),
            foodRepository.observeMealPhotos(),
            profileRepository.observeProfile(),
        ) { dailyNutrition, mealPhotos, profile ->
            NutritionUiState(
                dailyNutrition = dailyNutrition,
                mealPhotos = mealPhotos,
                targets = profile?.dailyTargets(),
                nutrientTargets = profile?.let { nutrientTargets(it, it.dailyTargets()) },
            )
        }.collect { newState -> reduce { newState } }
    }
}
