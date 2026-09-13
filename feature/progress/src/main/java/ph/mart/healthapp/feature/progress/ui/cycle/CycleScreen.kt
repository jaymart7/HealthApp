package ph.mart.healthapp.feature.progress.ui.cycle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.cycle.CycleDay
import ph.mart.healthapp.core.data.cycle.CycleSymptom
import ph.mart.healthapp.core.data.cycle.FlowLevel
import ph.mart.healthapp.core.data.cycle.cycleAverages
import ph.mart.healthapp.core.data.cycle.cycleDayNumber
import ph.mart.healthapp.core.data.cycle.cyclePrediction
import ph.mart.healthapp.core.data.cycle.inRange
import ph.mart.healthapp.core.data.cycle.periods
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.AppTopBar
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.cycle.components.PeriodRow
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.progress.components.ChartCard
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChip
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChipRow
import ph.mart.healthapp.feature.progress.ui.progress.components.HeroValue
import ph.mart.healthapp.feature.progress.ui.progress.components.LegendEntry
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRow
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRowsCard
import ph.mart.healthapp.feature.progress.ui.progress.components.SubjectSwitcher
import ph.mart.healthapp.feature.progress.ui.shared.components.DayBar
import ph.mart.healthapp.feature.progress.ui.shared.components.DayBarChart

/**
 * Where the cycle is now, and every period behind it — a route of its own, `SleepScreen`'s shape,
 * and the first of the converted pages that **writes**: its log sheet is the same one the
 * overview's empty-card hint opens, brought along here so the page has an entry point without
 * sending anyone back to the overview to find one.
 */
@Composable
internal fun CycleScreen(
    onSwitchSubject: (Subject) -> Unit,
    onOpenRecap: () -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CycleViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    CycleContent(
        days = uiState.days,
        onSwitchSubject = onSwitchSubject,
        onOpenRecap = onOpenRecap,
        onExitFlow = onExitFlow,
        modifier = modifier,
    )
}

@Composable
private fun CycleContent(
    days: List<CycleDay>,
    onSwitchSubject: (Subject) -> Unit,
    onOpenRecap: () -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    state: CycleState = rememberCycleState(),
) {
    val today = todayEpochDay()
    // Off every day on record, not the window: "day 14" is a fact about now, and it is also what
    // the overview's card reports, so the two agree about whether there is anything to show.
    val cycleDay = days.periods().cycleDayNumber(today)

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Zero insets: this bar sits in the scaffold's content, which its `innerPadding` has
            // already cleared of the status bar. See `SleepScreen`.
            AppTopBar(
                title = stringResource(Subject.Cycle.label),
                onBack = onExitFlow,
                windowInsets = WindowInsets(0),
                actions = {
                    IconButton(onClick = onOpenRecap) {
                        Icon(
                            imageVector = AppIcons.Share,
                            contentDescription = stringResource(R.string.progress_recap),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
            if (cycleDay == null) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        FullScreenState(
                            icon = { MascotAvatar(state = MascotState.Idle, size = 64.dp) },
                            heading = stringResource(R.string.progress_empty_cycle_heading),
                            body = stringResource(R.string.progress_empty_cycle_body),
                            // The one kind of empty page that gets a button: the sheet it opens is
                            // already on this screen, so pointing at it adds no entry point.
                            actions = {
                                PrimaryButton(
                                    label = stringResource(R.string.progress_hint_cycle),
                                    onClick = { state.sheetOpen = true },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            },
                        )
                    }
                    SubjectSwitcher(
                        subject = Subject.Cycle,
                        onSelect = onSwitchSubject,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CycleBody(days = days, cycleDay = cycleDay, today = today, state = state)
                    SubjectSwitcher(subject = Subject.Cycle, onSelect = onSwitchSubject)
                }
            }
        }
    }

    if (state.sheetOpen) {
        // Handed the days it seeds from — the sheet reads the same list the page does, so opening
        // a day twice shows what it says rather than a blank form.
        LogCycleSheet(days = days, onDismiss = { state.sheetOpen = false })
    }
}

/**
 * The hero and the prediction are **now-facts** and read every day on record; the stats, the chart
 * and the list are scoped to the range toggle, the split every other subject page makes. It draws
 * `DayBarChart` with the heavy level as its floor, so a light week reads light rather than filling
 * the canvas an auto-ranged axis would give it.
 *
 * No fertile window, no ovulation date: this page reports what was logged and the average between
 * periods. Anything past that is advice, and FitPulse does not give it.
 */
@Composable
private fun ColumnScope.CycleBody(
    days: List<CycleDay>,
    cycleDay: Int,
    today: Long,
    state: CycleState,
) {
    val range = state.range
    val from = today - range.days
    val windowed = days.inRange(range, today)
    val windowPeriods = windowed.periods()
    val averages = windowed.cycleAverages(today)
    val prediction = days.periods().cyclePrediction()

    HeroValue(value = "$cycleDay", caption = stringResource(R.string.progress_cycle_hero))
    FactChipRow(
        chips = listOfNotNull(
            prediction?.let { FactChip(nextPeriodChip(it.daysAway(today))) },
            prediction?.let { FactChip(stringResource(R.string.progress_cycle_average_chip, it.averageCycleDays, it.basedOnCycles)) },
        ),
    )
    ChartCard(
        title = stringResource(R.string.progress_cycle_flow),
        range = range,
        onRangeChange = { state.range = it },
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
        onClick = { state.sheetOpen = true },
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
    // Newest first: the period someone is in, or just finished, is the one being checked.
    windowPeriods.asReversed().forEach { period ->
        PeriodRow(period = period, today = today)
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

private fun daysPreview(today: Long): List<CycleDay> =
    listOf(0L, 1L, 2L, 3L, 28L, 29L, 30L, 31L, 32L).map { back ->
        CycleDay(
            dateEpochDay = today - back,
            flow = if (back < 2L) FlowLevel.Heavy.value else FlowLevel.Light.value,
            symptoms = if (back == 0L) setOf(CycleSymptom.Cramps) else emptySet(),
        )
    }.sortedBy { it.dateEpochDay }

@PreviewLightDark
@Composable
private fun CycleScreenPreview() {
    AppTheme {
        CycleContent(
            days = daysPreview(todayEpochDay()),
            onSwitchSubject = {},
            onOpenRecap = {},
            onExitFlow = {},
        )
    }
}

/** Nothing logged yet — the one empty page with a button, because its sheet is already here. */
@PreviewLightDark
@Composable
private fun CycleScreenEmptyPreview() {
    AppTheme {
        CycleContent(
            days = emptyList(),
            onSwitchSubject = {},
            onOpenRecap = {},
            onExitFlow = {},
        )
    }
}
