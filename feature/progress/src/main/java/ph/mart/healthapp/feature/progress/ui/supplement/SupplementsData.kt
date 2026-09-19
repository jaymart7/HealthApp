package ph.mart.healthapp.feature.progress.ui.supplement

import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.data.supplement.SupplementDay

/**
 * Every day with a row, oldest first. Sparse on purpose: a day with rows and nothing ticked is a
 * miss, a day with no rows at all is a gap, and the chart draws the two differently.
 *
 * [supplements] is the active list beside it, which the catch-up checklist needs to name a row and
 * to know what was due on the day being shown. Two fields and no profile — Supplements sits in
 * Nutrition, so its sibling switcher never has to decide whether to draw Cycle.
 */
data class SupplementsUiState(
    val days: List<SupplementDay> = emptyList(),
    val supplements: List<Supplement> = emptyList(),
)
