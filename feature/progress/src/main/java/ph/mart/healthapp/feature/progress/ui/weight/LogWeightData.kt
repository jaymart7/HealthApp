package ph.mart.healthapp.feature.progress.ui.weight

import ph.mart.healthapp.core.data.nowMinuteOfDay
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.todayEpochDay

data class LogWeightUiState(val entries: List<WeightEntry> = emptyList(), val preferredUnit: UnitSystem = UnitSystem.Metric)

/** [minuteOfDay] opens at now, and matters more here than anywhere else it is asked: a body is a
 * kilo heavier in the evening than it was that morning, and the Weight page's `weighInTimeSplit`
 * is what reads this field back. */
data class LogWeightForm(
    val dateEpochDay: Long = todayEpochDay(),
    val weightKg: Double = 70.0,
    val note: String = "",
    val minuteOfDay: Int = nowMinuteOfDay(),
)

sealed interface LogWeightEvent {
    data class OnSave(val form: LogWeightForm) : LogWeightEvent

    /** The day's whole weigh-in, gone. Only a hand-typed row offers it — see
     *  [isImported][ph.mart.healthapp.core.data.progress.isImported]. */
    data class OnDelete(val dateEpochDay: Long) : LogWeightEvent
}

sealed interface LogWeightSideEffect {
    data class Loaded(val weightKg: Double) : LogWeightSideEffect

    /** Written or removed — either way the sheet is done and closes. */
    data object Saved : LogWeightSideEffect
}
