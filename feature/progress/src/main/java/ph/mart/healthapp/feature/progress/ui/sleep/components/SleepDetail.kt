package ph.mart.healthapp.feature.progress.ui.sleep.components

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
import ph.mart.healthapp.core.data.health.SleepNight
import ph.mart.healthapp.core.data.health.formatDuration
import ph.mart.healthapp.core.data.health.inRange
import ph.mart.healthapp.core.data.health.sleepAverages
import ph.mart.healthapp.core.data.progress.ChartRange
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
import ph.mart.healthapp.feature.progress.ui.shared.components.DayBar
import ph.mart.healthapp.feature.progress.ui.shared.components.DayBarChart

/** The axis floor: a night is read against a full one, not against the best night in the window. */
private const val FULL_NIGHT_MINUTES = 480

/**
 * Import-only — FitPulse cannot measure sleep, so every figure here came off a watch and the page
 * offers no way to type one in.
 *
 * Windowed by date rather than sliced off the end, for the same reason the mood series is: nights
 * are sparse, so the chart needs the window's bounds to know where the gaps are.
 */
@Composable
internal fun ColumnScope.SleepDetailBody(uiState: ProgressUiState, state: ProgressScreenState) {
    val range = state.rangeFor(Subject.Sleep)
    val today = todayEpochDay()
    val from = today - (range.days ?: ChartRange.OneYear.days!!)
    val nights = uiState.sleepNights.inRange(range, today)
    val averages = nights.sleepAverages()

    HeroValue(value = averages.averageMinutes?.let(::formatDuration) ?: stringResource(R.string.progress_none), caption = stringResource(R.string.progress_sleep_hero))
    FactChipRow(chips = listOf(FactChip(stringResource(R.string.progress_from_watch))))
    ChartCard(
        title = stringResource(R.string.progress_sleep_nights),
        range = range,
        onRangeChange = { state.setRange(Subject.Sleep, it) },
        legend = listOf(LegendEntry(stringResource(R.string.progress_sleep_asleep), MaterialTheme.colorScheme.primary)),
    ) {
        DayBarChart(
            bars = nights.map { DayBar(it.dateEpochDay, it.minutesAsleep) },
            fromEpochDay = from,
            toEpochDay = today,
            minAxisValue = FULL_NIGHT_MINUTES,
        )
    }
    StatRowsCard(
        rows = listOf(
            StatRow(stringResource(R.string.progress_sleep_average), averages.averageMinutes?.let(::formatDuration) ?: stringResource(R.string.progress_none)),
            StatRow(stringResource(R.string.progress_sleep_longest), averages.longestMinutes?.let(::formatDuration) ?: stringResource(R.string.progress_none)),
            StatRow(stringResource(R.string.progress_sleep_recorded), "${averages.nights}"),
        ),
    )
}

@PreviewLightDark
@Composable
private fun SleepDetailPreview() {
    val today = todayEpochDay()
    AppTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SleepDetailBody(
                    uiState = ProgressUiState(
                        sleepNights = listOf(432, 401, 512, 388, 447).mapIndexed { index, minutes ->
                            SleepNight(today - 4 + index, minutes)
                        },
                    ),
                    state = ProgressScreenState(),
                )
            }
        }
    }
}
