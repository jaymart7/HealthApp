package ph.mart.healthapp.feature.progress.ui.pressure

import ph.mart.healthapp.core.data.bloodpressure.BloodPressureReading

/**
 * Every reading, oldest first — the only series on this tab that is per *reading* rather than per
 * day, so one day can hold several. The page folds it with `byDay()` before charting it.
 */
data class BloodPressureUiState(
    val readings: List<BloodPressureReading> = emptyList(),
)
