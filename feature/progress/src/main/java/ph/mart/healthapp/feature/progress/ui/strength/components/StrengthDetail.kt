package ph.mart.healthapp.feature.progress.ui.strength.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.StrengthSet
import ph.mart.healthapp.core.data.exercise.inRange
import ph.mart.healthapp.core.data.exercise.personalRecords
import ph.mart.healthapp.core.data.exercise.strengthTotals
import ph.mart.healthapp.core.data.exercise.volumeByDay
import ph.mart.healthapp.core.data.exercise.volumeLabel
import ph.mart.healthapp.core.data.exercise.withSets
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
 * Volume by day, then the all-time records. The stats re-fold over the *selected* window so they
 * can't describe different days from the chart above them, while the records stay all-time —
 * re-scoring a best against a 1M filter would retire records every month.
 *
 * Records rank by estimated one-rep max, not by the heaviest bar: 100 kg × 1 and 80 kg × 8 are not
 * comparable on the load alone.
 */
@Composable
internal fun ColumnScope.StrengthDetailBody(uiState: ProgressUiState, state: ProgressScreenState) {
    val range = state.rangeFor(Subject.Strength)
    val today = todayEpochDay()
    val from = today - (range.days ?: ChartRange.OneYear.days!!)
    val lifted = uiState.exerciseEntries.withSets()
    val series = lifted.volumeByDay().inRange(range, today)
    // Recomputed over the window rather than over the whole year, so the stats and the chart above
    // them always describe the same days.
    val totals = lifted.filter { it.dateEpochDay >= from }.strengthTotals()

    HeroValue(
        value = "${totals.workouts}",
        caption = pluralStringResource(R.plurals.progress_strength_workouts, totals.workouts),
    )
    FactChipRow(chips = listOf(FactChip(stringResource(R.string.progress_strength_lifted, volumeLabel(totals.volumeKg, uiState.preferredUnit)))))
    ChartCard(
        title = stringResource(R.string.progress_strength_volume),
        range = range,
        onRangeChange = { state.setRange(Subject.Strength, it) },
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
            StatRow(stringResource(R.string.progress_strength_volume), volumeLabel(totals.volumeKg, uiState.preferredUnit)),
        ),
    )

    val records = lifted.personalRecords()
    if (records.isNotEmpty()) {
        Text(
            text = stringResource(R.string.progress_strength_records),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        Column {
            records.forEach { record ->
                LiftRecordRow(record = record, unit = uiState.preferredUnit)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun StrengthDetailPreview() {
    val today = todayEpochDay()
    AppTheme {
      Surface {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        StrengthDetailBody(
            uiState = ProgressUiState(
                exerciseEntries = listOf(
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
                ),
            ),
            state = ProgressScreenState(),
        )
        }
      }
    }
}

