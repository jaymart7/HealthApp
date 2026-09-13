package ph.mart.healthapp.feature.progress.ui.fasting

import ph.mart.healthapp.core.data.fasting.DEFAULT_FAST_GOAL_HOURS
import ph.mart.healthapp.core.data.fasting.FastSession

/**
 * Completed fasts only, oldest first — a running one would keep growing under the chart, so the
 * repository never sends it.
 *
 * [goalHours] is the profile's **current** target and only ever moves the chart's dashed line:
 * every bar carries the target it was scored against, so raising the goal next month cannot
 * un-hit a fast already drawn.
 */
data class FastingUiState(
    val sessions: List<FastSession> = emptyList(),
    val goalHours: Int = DEFAULT_FAST_GOAL_HOURS,
)
