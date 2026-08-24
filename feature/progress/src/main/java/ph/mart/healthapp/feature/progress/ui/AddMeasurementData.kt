package ph.mart.healthapp.feature.progress.ui

import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.designsystem.component.todayEpochDay

data class AddMeasurementUiState(
    val entriesByPart: Map<MeasurementPart, List<MeasurementEntry>> = emptyMap(),
)

data class AddMeasurementForm(
    val part: MeasurementPart? = null,
    val dateEpochDay: Long = todayEpochDay(),
    val valueCm: Double = 80.0,
)

sealed interface AddMeasurementEvent {
    data class OnSave(val form: AddMeasurementForm) : AddMeasurementEvent
}

sealed interface AddMeasurementSideEffect {
    data object Saved : AddMeasurementSideEffect
}
