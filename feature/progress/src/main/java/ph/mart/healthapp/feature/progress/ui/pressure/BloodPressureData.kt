package ph.mart.healthapp.feature.progress.ui.pressure

import ph.mart.healthapp.core.data.bloodpressure.BloodPressureReading

/**
 * Every reading, oldest first — the only series on this tab that is per *reading* rather than per
 * day, so one day can hold several. The page folds it with `byDay()` before charting it.
 *
 * [cycleTrackingOn] is `Profile.cycleTrackingOn`, carried for the sibling switcher — see
 * [ph.mart.healthapp.feature.progress.ui.sleep.SleepUiState], which explains it once for all the
 * Wellbeing pages. The Cycle page is the one member that does not need it: it is excluded from its
 * own switcher anyway.
 */
data class BloodPressureUiState(
    val readings: List<BloodPressureReading> = emptyList(),
    val cycleTrackingOn: Boolean = false,
)
