package ph.mart.healthapp.feature.progress.ui.supplement.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.data.supplement.SupplementDay
import ph.mart.healthapp.core.data.supplement.adherenceByDay
import ph.mart.healthapp.core.data.supplement.averageAdherence
import ph.mart.healthapp.core.data.supplement.inRange
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreenState
import ph.mart.healthapp.feature.progress.ui.progress.ProgressUiState
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.progress.components.ChartCard
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChip
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChipRow
import ph.mart.healthapp.feature.progress.ui.progress.components.HeroValue
import ph.mart.healthapp.feature.progress.ui.progress.components.LegendEntry
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRow
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRowsCard

/**
 * Adherence, priced off each day's own snapshotted target — dropping a supplement from twice daily
 * to once next month must not turn a past day that read "2 of 2" into "2 of 1".
 *
 * A day with rows and nothing ticked is a slot with no height; a day with no rows at all draws
 * nothing. Seen-and-missed and never-tracked are different facts, and the chart says which.
 */
@Composable
internal fun ColumnScope.SupplementsDetailBody(uiState: ProgressUiState, state: ProgressScreenState) {
    val range = state.rangeFor(Subject.Supplements)
    val today = todayEpochDay()
    val from = today - (range.days ?: ChartRange.OneYear.days!!)
    val days = uiState.supplementDays.inRange(range, today)
    val byDay = days.adherenceByDay()
    val average = days.averageAdherence()

    HeroValue(value = average?.let { "${(it * 100).roundToInt()}" } ?: stringResource(R.string.progress_none), caption = stringResource(R.string.progress_supplements_hero))
    FactChipRow(chips = listOf(FactChip(stringResource(R.string.progress_supplements_days, byDay.size))))
    ChartCard(
        title = stringResource(R.string.progress_supplements_adherence),
        range = range,
        onRangeChange = { state.setRange(Subject.Supplements, it) },
        legend = listOf(LegendEntry(stringResource(R.string.progress_supplements_legend), MaterialTheme.colorScheme.primary)),
    ) {
        SupplementAdherenceChart(days = days, fromEpochDay = from, toEpochDay = today)
    }
    StatRowsCard(
        rows = listOf(
            StatRow("Average taken", average?.let { "${(it * 100).roundToInt()}%" } ?: "—"),
            StatRow("Full days", "${byDay.count { it.second >= 1f }}"),
            StatRow("Days logged", "${byDay.size}"),
        ),
    )
}

@PreviewLightDark
@Composable
private fun SupplementsDetailPreview() {
    val today = todayEpochDay()
    AppTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SupplementsDetailBody(
                    uiState = ProgressUiState(
                        supplementDays = listOf(2 to 2, 1 to 2, 2 to 2, 0 to 2, 2 to 2)
                            .mapIndexed { index, (taken, due) ->
                                SupplementDay(today - 4 + index, supplementId = 1, taken = taken, dueTimes = due)
                            },
                    ),
                    state = ProgressScreenState(),
                )
            }
        }
    }
}
