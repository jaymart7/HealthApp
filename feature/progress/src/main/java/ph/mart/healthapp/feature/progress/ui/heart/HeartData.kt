package ph.mart.healthapp.feature.progress.ui.heart

import ph.mart.healthapp.core.data.health.HeartDay

/**
 * Every imported day, oldest first — sparse and import-only, like the sleep series, so the page
 * hands the chart the window's bounds rather than a slice.
 */
data class HeartUiState(
    val days: List<HeartDay> = emptyList(),
)
