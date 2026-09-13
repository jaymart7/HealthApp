package ph.mart.healthapp.feature.progress.ui.mood

import ph.mart.healthapp.core.data.mood.MoodDay

/**
 * Every logged day, oldest first — sparse, so the page hands the chart the window's bounds rather
 * than a slice, and a day with nothing tapped is absent rather than zero.
 *
 * [cycleTrackingOn] is `Profile.cycleTrackingOn`, carried for the sibling switcher — see
 * [ph.mart.healthapp.feature.progress.ui.sleep.SleepUiState], which explains it once for all five
 * Wellbeing pages.
 */
data class MoodUiState(
    val days: List<MoodDay> = emptyList(),
    val cycleTrackingOn: Boolean = false,
)
