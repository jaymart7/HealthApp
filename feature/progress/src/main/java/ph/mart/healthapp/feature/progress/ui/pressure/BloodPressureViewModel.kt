package ph.mart.healthapp.feature.progress.ui.pressure

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository

/**
 * The Blood pressure page's container — read-only, `SleepViewModel`'s shape. The save and the
 * delete stay with [LogBloodPressureViewModel], which the page reaches for directly: both sit
 * under this route's `ViewModelStoreOwner`, so the list can delete the row it is showing without
 * this container knowing how to write.
 */
class BloodPressureViewModel(
    bloodPressureRepository: BloodPressureRepository,
    profileRepository: ProfileRepository,
) : ViewModel(), OrbitContainerHost<BloodPressureUiState, BloodPressureUiState, Nothing> {

    override val container = orbitContainer<BloodPressureUiState, Nothing>(BloodPressureUiState()) {
        observeReadings(bloodPressureRepository, profileRepository)
    }

    private fun observeReadings(
        bloodPressureRepository: BloodPressureRepository,
        profileRepository: ProfileRepository,
    ) = intent {
        combine(
            bloodPressureRepository.observeReadings(),
            profileRepository.observeProfile(),
        ) { readings, profile ->
            BloodPressureUiState(
                readings = readings,
                cycleTrackingOn = profile?.cycleTrackingOn == true,
            )
        }.collect { newState -> reduce { newState } }
    }
}
