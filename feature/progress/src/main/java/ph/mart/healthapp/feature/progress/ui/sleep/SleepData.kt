package ph.mart.healthapp.feature.progress.ui.sleep

import ph.mart.healthapp.core.data.health.SleepNight

/**
 * Every imported night, oldest first — the series this page charts, read straight from the
 * repository rather than sliced off `ProgressUiState`, which is what lets the page be a route.
 *
 * [cycleTrackingOn] is not the page's own data. It is `Profile.cycleTrackingOn`, and every
 * *Wellbeing* subject page carries it for one reason: the sibling switcher at the foot of the page
 * would otherwise offer a door to the one subject the overview has taken away.
 */
data class SleepUiState(
    val nights: List<SleepNight> = emptyList(),
    val cycleTrackingOn: Boolean = false,
)
