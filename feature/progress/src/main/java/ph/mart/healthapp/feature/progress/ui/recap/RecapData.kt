package ph.mart.healthapp.feature.progress.ui.recap

import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.GoalProjection
import ph.mart.healthapp.feature.progress.ui.shared.DEFAULT_RECAP_PERIOD
import ph.mart.healthapp.feature.progress.ui.shared.Recap
import ph.mart.healthapp.feature.progress.ui.shared.RecapPeriod

/**
 * The folded report, not the series it was folded from. The period is the container's rather than
 * the screen's because it is what *selects* the report — keeping it here is what stops this state
 * being a second copy of `ProgressUiState`'s dozen lists.
 *
 * [report] is null when nothing at all was logged in the window; the screen says so plainly rather
 * than drawing a page of zeros.
 */
data class RecapUiState(
    val report: Recap? = null,
    val period: RecapPeriod = DEFAULT_RECAP_PERIOD,
    val goal: Goal? = null,
    val unit: UnitSystem = UnitSystem.Metric,
    val projection: GoalProjection? = null,
)

sealed interface RecapEvent {
    data class OnPeriodChange(val period: RecapPeriod) : RecapEvent
}
