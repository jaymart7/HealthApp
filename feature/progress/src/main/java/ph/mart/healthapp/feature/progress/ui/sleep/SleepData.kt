package ph.mart.healthapp.feature.progress.ui.sleep

import ph.mart.healthapp.core.data.health.SleepNight

/**
 * Every imported night, oldest first — the series this page charts, read straight from the
 * repository rather than sliced off `ProgressUiState`, which is what lets the page be a route.
 */
data class SleepUiState(
    val nights: List<SleepNight> = emptyList(),
)
