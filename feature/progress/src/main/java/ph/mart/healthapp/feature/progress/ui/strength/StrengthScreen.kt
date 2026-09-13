package ph.mart.healthapp.feature.progress.ui.strength

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.StrengthSet
import ph.mart.healthapp.core.data.exercise.inRange
import ph.mart.healthapp.core.data.exercise.personalRecords
import ph.mart.healthapp.core.data.exercise.strengthTotals
import ph.mart.healthapp.core.data.exercise.volumeByDay
import ph.mart.healthapp.core.data.exercise.volumeLabel
import ph.mart.healthapp.core.data.exercise.withSets
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.AppTopBar
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
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
import ph.mart.healthapp.feature.progress.ui.strength.components.LiftRecordRow

/** Lifting volume and the all-time records — a route of its own, `SleepScreen`'s shape. The
 * workouts themselves are logged in `:feature:training`, so this page only reads. */
@Composable
internal fun StrengthScreen(
    onSwitchSubject: (Subject) -> Unit,
    onOpenRecap: () -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StrengthViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    StrengthContent(
        entries = uiState.entries,
        unit = uiState.unit,
        onSwitchSubject = onSwitchSubject,
        onOpenRecap = onOpenRecap,
        onExitFlow = onExitFlow,
        modifier = modifier,
    )
}

@Composable
private fun StrengthContent(
    entries: List<ExerciseEntry>,
    unit: UnitSystem,
    onSwitchSubject: (Subject) -> Unit,
    onOpenRecap: () -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    state: StrengthState = rememberStrengthState(),
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Zero insets: this bar sits in the scaffold's content, which its `innerPadding` has
            // already cleared of the status bar. See `SleepScreen`.
            AppTopBar(
                title = stringResource(Subject.Strength.label),
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
            if (entries.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        FullScreenState(
                            icon = { MascotAvatar(state = MascotState.Idle, size = 64.dp) },
                            heading = stringResource(R.string.progress_empty_strength_heading),
                            body = stringResource(R.string.progress_empty_strength_body),
                        )
                    }
                    SubjectSwitcher(
                        subject = Subject.Strength,
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
                    StrengthBody(entries = entries, unit = unit, state = state)
                    SubjectSwitcher(subject = Subject.Strength, onSelect = onSwitchSubject)
                }
            }
        }
    }
}

/**
 * Volume by day, then the all-time records. The stats re-fold over the *selected* window so they
 * can't describe different days from the chart above them, while the records stay all-time —
 * re-scoring a best against a 1M filter would retire records every month.
 *
 * Records rank by estimated one-rep max, not by the heaviest bar: 100 kg × 1 and 80 kg × 8 are not
 * comparable on the load alone.
 */
@Composable
private fun ColumnScope.StrengthBody(
    entries: List<ExerciseEntry>,
    unit: UnitSystem,
    state: StrengthState,
) {
    val range = state.range
    val today = todayEpochDay()
    val from = today - (range.days ?: ChartRange.OneYear.days!!)
    val series = entries.volumeByDay().inRange(range, today)
    // Recomputed over the window rather than over the whole year, so the stats and the chart above
    // them always describe the same days.
    val totals = entries.filter { it.dateEpochDay >= from }.strengthTotals()

    HeroValue(
        value = "${totals.workouts}",
        caption = pluralStringResource(R.plurals.progress_strength_workouts, totals.workouts),
    )
    FactChipRow(chips = listOf(FactChip(stringResource(R.string.progress_strength_lifted, volumeLabel(totals.volumeKg, unit)))))
    ChartCard(
        title = stringResource(R.string.progress_strength_volume),
        range = range,
        onRangeChange = { state.range = it },
        legend = listOf(LegendEntry(stringResource(R.string.progress_strength_volume_legend), MaterialTheme.colorScheme.primary)),
    ) {
        DayBarChart(
            // Rounded to whole units: a bar is a few pixels wide, and no one reads a decimal off
            // one. The stat row below carries the exact figure.
            bars = series.map { DayBar(it.dateEpochDay, it.volumeKg.toInt()) },
            fromEpochDay = from,
            toEpochDay = today,
        )
    }
    StatRowsCard(
        rows = listOf(
            StatRow(stringResource(R.string.progress_strength_workouts_label), "${totals.workouts}"),
            StatRow(stringResource(R.string.progress_strength_sets), "${totals.sets}"),
            StatRow(stringResource(R.string.progress_strength_volume), volumeLabel(totals.volumeKg, unit)),
        ),
    )

    val records = entries.personalRecords()
    if (records.isNotEmpty()) {
        Text(
            text = stringResource(R.string.progress_strength_records),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        Column {
            records.forEach { record ->
                LiftRecordRow(record = record, unit = unit)
            }
        }
    }
}

private fun entriesPreview(today: Long): List<ExerciseEntry> = listOf(
    ExerciseEntry(
        id = 1, dateEpochDay = today - 5, type = ExerciseType.Strength,
        minutes = 50, burnedKcal = 290,
        sets = listOf(
            StrengthSet("Squat", 5, 100.0),
            StrengthSet("Squat", 5, 102.5),
            StrengthSet("Row", 10, 60.0),
        ),
    ),
    ExerciseEntry(
        id = 2, dateEpochDay = today - 1, type = ExerciseType.Strength,
        minutes = 45, burnedKcal = 260,
        sets = listOf(
            StrengthSet("Bench press", 8, 62.5),
            StrengthSet("Dip", 12, 0.0),
        ),
    ),
    ExerciseEntry(id = 3, dateEpochDay = today, type = ExerciseType.Run, minutes = 30, burnedKcal = 363),
).withSets()

@PreviewLightDark
@Composable
private fun StrengthScreenPreview() {
    AppTheme {
        StrengthContent(
            entries = entriesPreview(todayEpochDay()),
            unit = UnitSystem.Metric,
            onSwitchSubject = {},
            onOpenRecap = {},
            onExitFlow = {},
        )
    }
}

/** Nothing lifted yet — the page is still a real page, with the way on to its siblings. */
@PreviewLightDark
@Composable
private fun StrengthScreenEmptyPreview() {
    AppTheme {
        StrengthContent(
            entries = emptyList(),
            unit = UnitSystem.Metric,
            onSwitchSubject = {},
            onOpenRecap = {},
            onExitFlow = {},
        )
    }
}
