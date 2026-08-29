package ph.mart.healthapp.feature.progress.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressRepository

/** Read-only container — nothing on the Progress tab itself writes data (see [ProgressUiState]),
 * so there's no handleEvent/Event pair here, unlike the other screens in this feature. */
class ProgressViewModel(
    progressRepository: ProgressRepository,
    profileRepository: ProfileRepository,
    foodRepository: FoodRepository,
) : ViewModel(), OrbitContainerHost<ProgressUiState, ProgressUiState, Nothing> {

    override val container = orbitContainer<ProgressUiState, Nothing>(ProgressUiState()) {
        observeProgress(progressRepository, profileRepository, foodRepository)
    }

    private fun observeProgress(
        progressRepository: ProgressRepository,
        profileRepository: ProfileRepository,
        foodRepository: FoodRepository,
    ) = intent {
        combine(
            progressRepository.observeWeightEntries(),
            progressRepository.observeMeasurements(),
            progressRepository.observePhotos(),
            profileRepository.observeProfile(),
            foodRepository.observeDailyNutrition(),
        ) { weightEntries, measurements, photos, profile, dailyNutrition ->
            ProgressUiState(
                weightEntries = weightEntries,
                measurements = measurements,
                photos = photos,
                goalWeightKg = profile?.targetWeightKg,
                goal = profile?.goal,
                preferredUnit = profile?.preferredUnit ?: UnitSystem.Metric,
                dailyNutrition = dailyNutrition,
                // Computed live off the profile, same as every other place targets are shown.
                targets = profile?.dailyTargets(),
            )
        }.collect { newState -> reduce { newState } }
    }
}
