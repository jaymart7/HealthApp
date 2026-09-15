package ph.mart.healthapp.feature.progress.ui.mood

import ph.mart.healthapp.core.data.mood.MoodDay

/**
 * Every logged day, oldest first — sparse, so the page hands the chart the window's bounds rather
 * than a slice, and a day with nothing tapped is absent rather than zero.
 */
data class MoodUiState(
    val days: List<MoodDay> = emptyList(),
)
