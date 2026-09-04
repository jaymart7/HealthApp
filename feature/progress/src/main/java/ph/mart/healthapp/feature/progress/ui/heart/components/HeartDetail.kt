package ph.mart.healthapp.feature.progress.ui.heart.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.health.HeartDay
import ph.mart.healthapp.core.data.health.heartAverages
import ph.mart.healthapp.core.data.health.inRange
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
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
 * The one chart in the app whose bars are not zero-based — nobody's heart visits 0–45 bpm, so a
 * zero-based axis would squash the beats that actually differ. Each bar spans that day's lowest
 * reading up to its average, which is what the legend names.
 *
 * "Lowest" is never called a resting rate: FitPulse aggregates whatever samples the watch happened
 * to take, and calling a minimum "resting" would claim a measurement nobody made.
 */
@Composable
internal fun ColumnScope.HeartDetailBody(uiState: ProgressUiState, state: ProgressScreenState) {
    val range = state.rangeFor(Subject.Heart)
    val today = todayEpochDay()
    val from = today - (range.days ?: ChartRange.OneYear.days!!)
    val days = uiState.heartDays.inRange(range, today)
    val averages = days.heartAverages()

    HeroValue(value = averages.averageBpm?.toString() ?: "—", caption = "bpm average")
    FactChipRow(chips = listOf(FactChip("From your paired watch")))
    ChartCard(
        title = "Daily range",
        range = range,
        onRangeChange = { state.setRange(Subject.Heart, it) },
        legend = listOf(LegendEntry("Lowest to average, per day", MaterialTheme.colorScheme.secondary)),
    ) {
        HeartTrendChart(days = days, fromEpochDay = from, toEpochDay = today)
    }
    StatRowsCard(
        rows = listOf(
            StatRow("Average", averages.averageBpm?.let { "$it bpm" } ?: "—"),
            StatRow("Lowest", averages.lowestBpm?.let { "$it bpm" } ?: "—"),
            StatRow("Days recorded", "${averages.days}"),
        ),
    )
}

@PreviewLightDark
@Composable
private fun HeartDetailPreview() {
    val today = todayEpochDay()
    AppTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HeartDetailBody(
                    uiState = ProgressUiState(
                        heartDays = listOf(68 to 52, 71 to 55, 66 to 49, 74 to 58)
                            .mapIndexed { index, (average, low) ->
                                HeartDay(today - 3 + index, averageBpm = average, minBpm = low)
                            },
                    ),
                    state = ProgressScreenState(),
                )
            }
        }
    }
}
