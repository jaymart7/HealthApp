package ph.mart.healthapp.feature.progress.ui.weight

import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.todayEpochDay

data class LogWeightUiState(val entries: List<WeightEntry> = emptyList(), val preferredUnit: UnitSystem = UnitSystem.Metric)

data class LogWeightForm(
    val dateEpochDay: Long = todayEpochDay(),
    val weightKg: Double = 70.0,
    val note: String = "",
)

sealed interface LogWeightEvent {
    data class OnSave(val form: LogWeightForm) : LogWeightEvent
}

sealed interface LogWeightSideEffect {
    data class Loaded(val weightKg: Double) : LogWeightSideEffect
    data object Saved : LogWeightSideEffect
}
