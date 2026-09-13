package ph.mart.healthapp.feature.progress.ui.weight

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.progress.ProgressRepository

/**
 * The Weight page's container — read-only over three flows, `MeasurementsViewModel`'s shape.
 *
 * The nutrition flow is the odd one: nothing here charts calories, but the energy check-in is
 * measured from what was eaten against what the scale did, so the page cannot fold it without
 * them. Writing a target is [ph.mart.healthapp.feature.progress.ui.energy.EnergyCheckInViewModel]'s
 * job and logging a weigh-in is [LogWeightViewModel]'s; both sit under this route's owner.
 */
class WeightViewModel(
    progressRepository: ProgressRepository,
    foodRepository: FoodRepository,
    profileRepository: ProfileRepository,
) : ViewModel(), OrbitContainerHost<WeightUiState, WeightUiState, Nothing> {

    override val container = orbitContainer<WeightUiState, Nothing>(WeightUiState()) {
        observeWeight(progressRepository, foodRepository, profileRepository)
    }

    private fun observeWeight(
        progressRepository: ProgressRepository,
        foodRepository: FoodRepository,
        profileRepository: ProfileRepository,
    ) = intent {
        combine(
            progressRepository.observeWeightEntries(),
            foodRepository.observeDailyNutrition(),
            profileRepository.observeProfile(),
        ) { entries, dailyNutrition, profile ->
            WeightUiState(entries = entries, dailyNutrition = dailyNutrition, profile = profile)
        }.collect { newState -> reduce { newState } }
    }
}
