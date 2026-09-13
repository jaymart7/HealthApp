package ph.mart.healthapp.feature.progress.ui.measurement

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressRepository

/**
 * The Measurements page's container — read-only, `SleepViewModel`'s shape over three flows rather
 * than two. The weigh-ins ride along because the body-composition card is priced against the
 * newest one; only that one weight leaves the container, not the series, since nothing on this
 * page charts it.
 *
 * The writing stays with [AddMeasurementViewModel], which the sheet this page opens brings along.
 */
class MeasurementsViewModel(
    progressRepository: ProgressRepository,
    profileRepository: ProfileRepository,
) : ViewModel(), OrbitContainerHost<MeasurementsUiState, MeasurementsUiState, Nothing> {

    override val container = orbitContainer<MeasurementsUiState, Nothing>(MeasurementsUiState()) {
        observeMeasurements(progressRepository, profileRepository)
    }

    private fun observeMeasurements(
        progressRepository: ProgressRepository,
        profileRepository: ProfileRepository,
    ) = intent {
        combine(
            progressRepository.observeMeasurements(),
            progressRepository.observeWeightEntries(),
            profileRepository.observeProfile(),
        ) { measurements, weightEntries, profile ->
            MeasurementsUiState(
                measurements = measurements,
                latestWeightKg = weightEntries.maxByOrNull { it.dateEpochDay }?.weightKg,
                heightCm = profile?.heightCm,
                unit = profile?.preferredUnit ?: UnitSystem.Metric,
            )
        }.collect { newState -> reduce { newState } }
    }
}
