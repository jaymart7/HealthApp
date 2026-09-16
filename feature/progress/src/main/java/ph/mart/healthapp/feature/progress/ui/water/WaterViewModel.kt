package ph.mart.healthapp.feature.progress.ui.water

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.water.DEFAULT_WATER_GOAL_GLASSES
import ph.mart.healthapp.core.data.water.WaterRepository

/** The Water page's container — `FastingViewModel`'s shape, reading the profile for the goal line
 * and the unit label. Logging a glass is Home's card and the diary's row, each under its own. */
class WaterViewModel(
    waterRepository: WaterRepository,
    profileRepository: ProfileRepository,
) : ViewModel(), OrbitContainerHost<WaterUiState, WaterUiState, Nothing> {

    override val container = orbitContainer<WaterUiState, Nothing>(WaterUiState()) {
        observeWater(waterRepository, profileRepository)
    }

    private fun observeWater(
        waterRepository: WaterRepository,
        profileRepository: ProfileRepository,
    ) = intent {
        combine(
            waterRepository.observeDays(),
            profileRepository.observeProfile(),
        ) { days, profile ->
            WaterUiState(
                days = days,
                goalGlasses = profile?.waterGoalGlasses ?: DEFAULT_WATER_GOAL_GLASSES,
                unit = profile?.preferredUnit ?: UnitSystem.Metric,
            )
        }.collect { newState -> reduce { newState } }
    }
}
