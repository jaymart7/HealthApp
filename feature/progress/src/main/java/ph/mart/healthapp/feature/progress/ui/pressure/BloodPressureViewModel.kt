package ph.mart.healthapp.feature.progress.ui.pressure

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureRepository

/**
 * The pressure flow's only container, and the one place this feature writes blood pressure.
 *
 * It carries the delete as well as the save because both belong to the same flow and the tab and
 * its sheet sit under one `ViewModelStoreOwner`, so `koinViewModel()` hands them the same instance.
 * `ProgressViewModel` stays the read-only container its KDoc says it is.
 *
 * There is no state of its own: the readings are already on `ProgressUiState`, and duplicating
 * them here would give the tab and its list two sources that could disagree.
 */
class BloodPressureViewModel(
    private val repository: BloodPressureRepository,
) : ViewModel(), OrbitContainerHost<Unit, Unit, BloodPressureSideEffect> {

    override val container = orbitContainer<Unit, BloodPressureSideEffect>(Unit)

    fun handleEvent(event: BloodPressureEvent) {
        when (event) {
            is BloodPressureEvent.OnSave -> onSave(event.form)
            is BloodPressureEvent.OnDelete -> intent { repository.deleteReading(event.id) }
        }
    }

    /** The clock is read here rather than in the form, so a sheet left open across an hour still
     * stamps the reading with the moment Save was tapped. */
    private fun onSave(form: BloodPressureForm) = intent {
        if (!form.isValid) return@intent
        repository.addReading(form.toReading(System.currentTimeMillis()))
        postSideEffect(BloodPressureSideEffect.Saved)
    }
}
