package ph.mart.healthapp.feature.progress.ui.measurement

import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.data.progress.defaultValue
import ph.mart.healthapp.core.data.todayEpochDay

data class AddMeasurementUiState(
    val entriesByPart: Map<MeasurementPart, List<MeasurementEntry>> = emptyMap(),
)

/** [value] is stored units — centimetres, or percent for body fat. The opening figure comes from
 * the part rather than a constant here, because 80 is a waist and would be a lethal body fat. */
data class AddMeasurementForm(
    val part: MeasurementPart? = null,
    val dateEpochDay: Long = todayEpochDay(),
    val value: Double = MeasurementPart.Chest.defaultValue(),
)

sealed interface AddMeasurementEvent {
    data class OnSave(val form: AddMeasurementForm) : AddMeasurementEvent
}

sealed interface AddMeasurementSideEffect {
    data object Saved : AddMeasurementSideEffect
}
