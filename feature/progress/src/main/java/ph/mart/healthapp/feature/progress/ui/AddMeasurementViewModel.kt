package ph.mart.healthapp.feature.progress.ui

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.ProgressRepository

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

    private fun onSave(form: AddMeasurementForm) = intent {
        val part = form.part ?: return@intent
        progressRepository.upsertMeasurementEntry(MeasurementEntry(part = part, dateEpochDay = form.dateEpochDay, valueCm = form.valueCm))
        postSideEffect(AddMeasurementSideEffect.Saved)
    }
}
