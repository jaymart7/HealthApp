package ph.mart.healthapp.feature.progress.ui.measurement

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.progress.range

class AddMeasurementViewModel(
    private val progressRepository: ProgressRepository,
) : ViewModel(), OrbitContainerHost<AddMeasurementUiState, AddMeasurementUiState, AddMeasurementSideEffect> {

    override val container = orbitContainer<AddMeasurementUiState, AddMeasurementSideEffect>(AddMeasurementUiState()) {
        observeMeasurements(progressRepository)
    }

    fun handleEvent(event: AddMeasurementEvent) {
        when (event) {
            is AddMeasurementEvent.OnSave -> onSave(event.form)
        }
    }

    private fun observeMeasurements(repo: ProgressRepository) = intent {
        repo.observeMeasurements().collect { entriesByPart -> reduce { state.copy(entriesByPart = entriesByPart) } }
    }

    /** The typed field is unclamped so a figure can be retyped digit by digit; the part's own range
     * is applied here instead. */
    private fun onSave(form: AddMeasurementForm) = intent {
        val part = form.part ?: return@intent
        val value = form.value.coerceIn(part.range())
        progressRepository.upsertMeasurementEntry(
            MeasurementEntry(part = part, dateEpochDay = form.dateEpochDay, value = value, minuteOfDay = form.minuteOfDay),
        )
        postSideEffect(AddMeasurementSideEffect.Saved)
    }
}
