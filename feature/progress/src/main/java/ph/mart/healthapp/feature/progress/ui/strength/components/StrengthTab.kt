package ph.mart.healthapp.feature.progress.ui.strength.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreenState
import ph.mart.healthapp.feature.progress.ui.progress.ProgressUiState
import ph.mart.healthapp.feature.progress.ui.shared.components.DayBar
import ph.mart.healthapp.feature.progress.ui.shared.components.DayBarChart
import ph.mart.healthapp.feature.progress.ui.weight.components.StatCell

/**
 * Volume over the selected range, and what each lift is best at.
 *
 * It needs nothing new from [ProgressViewModel][ph.mart.healthapp.feature.progress.ui.progress.ProgressViewModel]:
 * the sets ride on `ExerciseEntry`, and the year window is already in [ProgressUiState] for the
 * Activity tab's burn series. Both figures are folds over that list — the `badgeGroups()` call.
 *
 * The series is sparse and windowed by *date*, like Sleep, Heart and Activity: a rest day is a gap
 * and its x-position is what says so.
 */
@Composable
internal fun StrengthTabContent(uiState: ProgressUiState, state: ProgressScreenState) {
    val lifted = uiState.exerciseEntries.withSets()
    if (lifted.isEmpty()) {
        FullScreenState(
            icon = { MascotAvatar(state = MascotState.Idle, size = 64.dp) },
            heading = "Nothing lifted yet",
            body = "Log a strength workout from the diary and its sets show up here.",
        )
        return
    }
    Column {
        SegmentedToggle(
            options = ChartRange.entries.map { it.label },
            selectedIndex = ChartRange.entries.indexOf(state.range),
            onSelect = { index -> state.range = ChartRange.entries[index] },
        )
        val today = todayEpochDay()
        val from = today - (state.range.days ?: ChartRange.OneYear.days!!)
        val series = lifted.volumeByDay().inRange(state.range, today)

        DayBarChart(
            // Rounded to whole units: a bar is a few pixels wide, and no one reads a decimal off
            // one. The stat row below carries the exact figure.
            bars = series.map { DayBar(it.dateEpochDay, it.volumeKg.toInt()) },
            fromEpochDay = from,
            toEpochDay = today,
            modifier = Modifier.padding(top = 16.dp),
        )

        // Recomputed over the window rather than over the whole year, so the stats and the chart
        // above them always describe the same days.
        val windowed = lifted.filter { it.dateEpochDay >= from }
        val totals = windowed.strengthTotals()
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatCell(label = "Workouts", value = "${totals.workouts}")
            StatCell(label = "Sets", value = "${totals.sets}")
            StatCell(label = "Volume", value = volumeLabel(totals.volumeKg, uiState.preferredUnit))
        }

        // Records are all-time within the year the repository windows, not per selected range: a
        // best is a best, and re-scoring it against a 1M filter would retire records every month.
        val records = lifted.personalRecords()
        if (records.isNotEmpty()) {
            Text(
                text = "Personal records",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
            )
            records.forEach { record ->
                LiftRecordRow(record = record, unit = uiState.preferredUnit)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun StrengthTabPreview() {
    val today = todayEpochDay()
    AppTheme {
        StrengthTabContent(
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

/** Nothing has sets yet: hidden rather than a chart of zeros, like the Activity tab's. */
@PreviewLightDark
@Composable
private fun StrengthTabEmptyPreview() {
    AppTheme { StrengthTabContent(uiState = ProgressUiState(), state = ProgressScreenState()) }
}
