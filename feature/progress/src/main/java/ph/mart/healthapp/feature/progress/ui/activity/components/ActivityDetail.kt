package ph.mart.healthapp.feature.progress.ui.activity.components

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
import ph.mart.healthapp.core.data.health.StepDay
import ph.mart.healthapp.core.data.health.burnSeries
import ph.mart.healthapp.core.data.health.formatSteps
import ph.mart.healthapp.core.data.health.inRange
import ph.mart.healthapp.core.data.health.stepAverages
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

/**
 * Two charts, because steps and calories share no axis — and one range toggle between them, on the
 * first card. Two identical toggles would be two controls for one value.
 *
 * The burn series folds imported step days together with logged workouts through the same
 * `dayBurnedKcal()` the diary uses, so a walk the watch already counted is never counted twice. It
 * shows what was *burned* and therefore ignores the "add exercise to my budget" switch: that switch
 * decides what reaches the calorie budget, not what happened.
 *
 * The step goal is the profile's current one and is not snapshotted per day — `step_day` rows are
 * replaced wholesale on every re-sync, so a target stored beside them would be overwritten. The
 * stat says "today's goal" for that reason.
 */
@Composable
internal fun ColumnScope.ActivityDetailBody(uiState: ProgressUiState, state: ProgressScreenState) {
    val range = state.rangeFor(Subject.Activity)
    val today = todayEpochDay()
    val from = today - (range.days ?: ChartRange.OneYear.days!!)
    val steps = uiState.stepDays.inRange(range, today)
    val stepStats = steps.stepAverages(uiState.stepGoal)
    val burn = burnSeries(uiState.stepDays, uiState.exerciseEntries).inRange(range, today)

    HeroValue(value = stepStats.averageSteps?.let(::formatSteps) ?: stringResource(R.string.progress_none), caption = stringResource(R.string.progress_activity_hero))
    FactChipRow(
        chips = listOf(FactChip(stringResource(R.string.progress_activity_goal_days, stepStats.daysHitGoal, stepStats.days, formatSteps(uiState.stepGoal)))),
    )
    ChartCard(
        title = stringResource(R.string.progress_activity_steps),
        range = range,
        onRangeChange = { state.setRange(Subject.Activity, it) },
        legend = listOf(
            LegendEntry(stringResource(R.string.progress_activity_steps), MaterialTheme.colorScheme.primary),
            LegendEntry(stringResource(R.string.progress_activity_goal, formatSteps(uiState.stepGoal)), MaterialTheme.colorScheme.onSurfaceVariant, dashed = true),
        ),
    ) {
        DayBarChart(
            bars = steps.map { DayBar(it.dateEpochDay, it.steps) },
            fromEpochDay = from,
            toEpochDay = today,
            minAxisValue = uiState.stepGoal,
            goalValue = uiState.stepGoal,
        )
    }
    ChartCard(
        title = stringResource(R.string.progress_activity_burned_title),
        range = null,
        onRangeChange = null,
        legend = listOf(LegendEntry(stringResource(R.string.progress_activity_burned_legend), MaterialTheme.colorScheme.primary)),
    ) {
        DayBarChart(
            bars = burn.map { DayBar(it.dateEpochDay, it.burnedKcal) },
            fromEpochDay = from,
            toEpochDay = today,
        )
    }
    StatRowsCard(
        rows = listOf(
            StatRow(stringResource(R.string.progress_activity_average), stepStats.averageSteps?.let(::formatSteps) ?: stringResource(R.string.progress_none)),
            StatRow(stringResource(R.string.progress_activity_best), stepStats.bestSteps?.let(::formatSteps) ?: stringResource(R.string.progress_none)),
            StatRow(stringResource(R.string.progress_activity_hit_goal), stringResource(R.string.progress_of, stepStats.daysHitGoal, stepStats.days)),
            StatRow(stringResource(R.string.progress_activity_burned), stringResource(R.string.progress_kcal, burn.sumOf { it.burnedKcal })),
        ),
    )
}

@PreviewLightDark
@Composable
private fun ActivityDetailPreview() {
    val today = todayEpochDay()
    AppTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ActivityDetailBody(
                    uiState = ProgressUiState(
                        stepDays = listOf(8_200, 11_400, 6_050, 12_900, 9_100)
                            .mapIndexed { index, steps ->
                                StepDay(today - 4 + index, steps = steps, burnedKcal = steps / 22)
                            },
                    ),
                    state = ProgressScreenState(),
                )
            }
        }
    }
}
