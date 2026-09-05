package ph.mart.healthapp.feature.progress.ui.fasting.components

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
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.fasting.dateEpochDay
import ph.mart.healthapp.core.data.fasting.durationMinutes
import ph.mart.healthapp.core.data.fasting.fastingAverages
import ph.mart.healthapp.core.data.fasting.inRange
import ph.mart.healthapp.core.data.health.formatDuration
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

/** A fast is read against the day it sits in, not against the longest one in the window. */
private const val FULL_DAY_MINUTES = 24 * 60

/**
 * Completed fasts only, each placed on the day it **ended** — a 16-hour fast started at 20:00 is
 * yesterday evening's discipline paying off at lunchtime.
 *
 * The dashed line is the profile's *current* target, while every bar was scored against the target
 * in force when it was logged. Raising the goal next month moves the line and prices the next fast;
 * it never un-hits one already drawn.
 */
@Composable
internal fun ColumnScope.FastingDetailBody(uiState: ProgressUiState, state: ProgressScreenState) {
    val range = state.rangeFor(Subject.Fasting)
    val today = todayEpochDay()
    val from = today - (range.days ?: ChartRange.OneYear.days!!)
    val sessions = uiState.fastSessions.inRange(range, today)
    val averages = sessions.fastingAverages()

    HeroValue(value = averages.averageMinutes?.let(::formatDuration) ?: stringResource(R.string.progress_none), caption = stringResource(R.string.progress_fasting_hero))
    FactChipRow(chips = listOf(FactChip(stringResource(R.string.progress_fasting_goal, uiState.fastingGoalHours))))
    ChartCard(
        title = stringResource(R.string.progress_fasting_title),
        range = range,
        onRangeChange = { state.setRange(Subject.Fasting, it) },
        legend = listOf(
            LegendEntry(stringResource(R.string.progress_fasting_legend), MaterialTheme.colorScheme.primary),
            LegendEntry(stringResource(R.string.progress_fasting_goal, uiState.fastingGoalHours), MaterialTheme.colorScheme.onSurfaceVariant, dashed = true),
        ),
    ) {
        DayBarChart(
            // Priced with nowMillis = 0: only completed fasts reach here, so no clock enters this.
            bars = sessions.map { DayBar(it.dateEpochDay, it.durationMinutes(nowMillis = 0)) },
            fromEpochDay = from,
            toEpochDay = today,
            minAxisValue = FULL_DAY_MINUTES,
            goalValue = uiState.fastingGoalHours * 60,
        )
    }
    StatRowsCard(
        rows = listOf(
            StatRow(stringResource(R.string.progress_fasting_average), averages.averageMinutes?.let(::formatDuration) ?: stringResource(R.string.progress_none)),
            StatRow(stringResource(R.string.progress_fasting_longest), averages.longestMinutes?.let(::formatDuration) ?: stringResource(R.string.progress_none)),
            StatRow(stringResource(R.string.progress_fasting_goals_hit), stringResource(R.string.progress_of, averages.goalsHit, averages.count)),
        ),
    )
}

@PreviewLightDark
@Composable
private fun FastingDetailPreview() {
    val today = todayEpochDay()
    val dayMillis = 86_400_000L
    AppTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FastingDetailBody(
                    uiState = ProgressUiState(
                        fastSessions = listOf(16, 14, 18, 15).mapIndexed { index, hours ->
                            val end = (today - 3 + index) * dayMillis
                            FastSession(
                                id = index.toLong(),
                                startMillis = end - hours * 3_600_000L,
                                endMillis = end,
                                goalHours = 16,
                            )
                        },
                    ),
                    state = ProgressScreenState(),
                )
            }
        }
    }
}
