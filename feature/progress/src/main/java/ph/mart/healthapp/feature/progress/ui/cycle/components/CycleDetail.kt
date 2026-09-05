package ph.mart.healthapp.feature.progress.ui.cycle.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import ph.mart.healthapp.core.data.cycle.CycleDay
import ph.mart.healthapp.core.data.cycle.CyclePeriod
import ph.mart.healthapp.core.data.cycle.CycleSymptom
import ph.mart.healthapp.core.data.cycle.FlowLevel
import ph.mart.healthapp.core.data.cycle.cycleAverages
import ph.mart.healthapp.core.data.cycle.cycleDayNumber
import ph.mart.healthapp.core.data.cycle.cyclePrediction
import ph.mart.healthapp.core.data.cycle.inRange
import ph.mart.healthapp.core.data.cycle.periods
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
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
 * Where the cycle is now, and every period behind it.
 *
 * The hero and the prediction are **now-facts** and read every day on record; the stats, the chart
 * and the list are scoped to the range toggle, the split every other detail page makes. It draws
 * `DayBarChart` with the heavy level as its floor, so a light week reads light rather than filling
 * the canvas an auto-ranged axis would give it.
 *
 * No fertile window, no ovulation date: this page reports what was logged and the average between
 * periods. Anything past that is advice, and FitPulse does not give it.
 */
@Composable
internal fun CycleDetailBody(uiState: ProgressUiState, state: ProgressScreenState) {
    val today = todayEpochDay()
    val range = state.rangeFor(Subject.Cycle)
    val from = today - (range.days ?: ChartRange.OneYear.days!!)
    val windowed = uiState.cycleDays.inRange(range, today)
    val windowPeriods = windowed.periods()
    val averages = windowed.cycleAverages(today)
    // Off every day on record, not the window: "day 14" and "expected Tuesday" are facts about
    // now, and a 1M window would restart the count at whatever it happened to clip.
    val allPeriods = uiState.cycleDays.periods()
    val cycleDay = allPeriods.cycleDayNumber(today)
    val prediction = allPeriods.cyclePrediction()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HeroValue(value = cycleDay?.toString() ?: stringResource(R.string.progress_none), caption = stringResource(R.string.progress_cycle_hero))
        FactChipRow(
            chips = listOfNotNull(
                prediction?.let { FactChip(nextPeriodChip(it.daysAway(today))) },
                prediction?.let { FactChip(stringResource(R.string.progress_cycle_average_chip, it.averageCycleDays, it.basedOnCycles)) },
            ),
        )
        ChartCard(
            title = stringResource(R.string.progress_cycle_flow),
            range = range,
            onRangeChange = { state.setRange(Subject.Cycle, it) },
            legend = listOf(LegendEntry(stringResource(R.string.progress_cycle_flow_legend), MaterialTheme.colorScheme.secondary)),
        ) {
            DayBarChart(
                bars = windowed.filter { it.flow > 0 }.map { DayBar(it.dateEpochDay, it.flow) },
                fromEpochDay = from,
                toEpochDay = today,
                // The scale's own ceiling, so a light period is drawn short rather than full.
                minAxisValue = FlowLevel.Heavy.value,
            )
        }
        StatRowsCard(
            rows = listOfNotNull(
                averages.cycleDays?.let { StatRow(stringResource(R.string.progress_cycle_average), stringResource(R.string.progress_cycle_days, it.roundToInt())) },
                averages.periodDays?.let { StatRow(stringResource(R.string.progress_cycle_average_period), stringResource(R.string.progress_cycle_days, it.roundToInt())) },
                StatRow(stringResource(R.string.progress_cycle_periods), "${windowPeriods.size}"),
                StatRow(stringResource(R.string.progress_cycle_days_logged), "${averages.daysLogged}"),
            ),
        )
        PrimaryButton(
            label = stringResource(R.string.progress_cycle_log_day),
            onClick = { state.openCycleSheet() },
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        )
        // Newest first: the period someone is in, or just finished, is the one being checked.
        windowPeriods.asReversed().forEach { period ->
            PeriodRow(period = period, today = today)
        }
    }
}

/** Never hidden once it exists: a period that is late is exactly what this line is read for. */
@Composable
private fun nextPeriodChip(daysAway: Int): String = when {
    daysAway == 0 -> stringResource(R.string.progress_cycle_due_today)
    daysAway == 1 -> stringResource(R.string.progress_cycle_due_tomorrow)
    daysAway > 1 -> stringResource(R.string.progress_cycle_due_in, daysAway)
    daysAway == -1 -> stringResource(R.string.progress_cycle_late_yesterday)
    else -> stringResource(R.string.progress_cycle_late_days, abs(daysAway))
}

/** A period still running says so rather than reporting a length it hasn't reached. */
@Composable
private fun PeriodRow(period: CyclePeriod, today: Long) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (period.lengthDays == 1) {
                    formatEpochDay(period.startEpochDay)
                } else {
                    stringResource(R.string.progress_cycle_span, formatEpochDay(period.startEpochDay), formatEpochDay(period.endEpochDay))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (today in period) {
                    stringResource(R.string.progress_cycle_ongoing)
                } else {
                    stringResource(R.string.progress_cycle_length, period.lengthDays)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun CycleDetailPreview() {
    val today = todayEpochDay()
    val days = listOf(0L, 1L, 2L, 3L, 28L, 29L, 30L, 31L, 32L).map { back ->
        CycleDay(
            dateEpochDay = today - back,
            flow = if (back < 2L) FlowLevel.Heavy.value else FlowLevel.Light.value,
            symptoms = if (back == 0L) setOf(CycleSymptom.Cramps) else emptySet(),
        )
    }.sortedBy { it.dateEpochDay }
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                CycleDetailBody(
                    uiState = ProgressUiState(cycleDays = days, cycleTrackingOn = true),
                    state = ProgressScreenState(selectedSubject = Subject.Cycle),
                )
            }
        }
    }
}
