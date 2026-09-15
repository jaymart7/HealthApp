package ph.mart.healthapp.feature.progress.ui.pressure

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureRepository

/**
 * The Blood pressure page's container — read-only, `SleepViewModel`'s shape. The save and the
 * delete stay with [LogBloodPressureViewModel], which the page reaches for directly: both sit
 * under this route's `ViewModelStoreOwner`, so the list can delete the row it is showing without
 * this container knowing how to write.
 */
class BloodPressureViewModel(
    bloodPressureRepository: BloodPressureRepository,
) : ViewModel(), OrbitContainerHost<BloodPressureUiState, BloodPressureUiState, Nothing> {

    override val container = orbitContainer<BloodPressureUiState, Nothing>(BloodPressureUiState()) {
        observeReadings(bloodPressureRepository)
    }

    private fun observeReadings(bloodPressureRepository: BloodPressureRepository) = intent {
        bloodPressureRepository.observeReadings().collect { readings ->
            reduce { BloodPressureUiState(readings = readings) }
        }
    }
}
