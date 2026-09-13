package ph.mart.healthapp.feature.progress.ui.pressure

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureRepository

/**
 * The one place this feature writes blood pressure, and it carries the **delete** as well as the
 * save: the page, its list and its sheet sit under one `ViewModelStoreOwner`, so `koinViewModel()`
 * hands all three the same instance and a row can be removed from where it is shown.
 *
 * It stays the writer now that the page beside it has become a route with a read-only container of
 * its own (`BloodPressureViewModel`). The sheet has two open sites — the page, and the overview's
 * empty-card hint — so it is instantiated under two owners, which is harmless: both write through
 * the same repository, and neither holds state.
 */
class LogBloodPressureViewModel(
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
