package ph.mart.healthapp.feature.progress.ui.cycle

import ph.mart.healthapp.core.data.cycle.CycleDay

/**
 * Every logged day, oldest first — sparse, and collected whether or not cycle tracking is on. The
 * switch decides what the *overview* draws; this page is only reachable when it is on.
 *
 * One field: Cycle is the subject the switcher may itself be asked to hide, so the page passes
 * `cycleTracking = true` — it is on by definition if you are standing on it.
 */
data class CycleUiState(
    val days: List<CycleDay> = emptyList(),
)
