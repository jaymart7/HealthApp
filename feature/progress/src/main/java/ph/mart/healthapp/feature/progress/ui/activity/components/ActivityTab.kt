package ph.mart.healthapp.feature.progress.ui.activity.components

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
import ph.mart.healthapp.core.data.health.StepDay
import ph.mart.healthapp.core.data.health.burnSeries
import ph.mart.healthapp.core.data.health.formatSteps
import ph.mart.healthapp.core.data.health.inRange
import ph.mart.healthapp.core.data.health.stepAverages
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
 * Steps and calorie burn over the selected range — the only tab drawing two charts, because the two
 * series answer the same question in different units and neither can be plotted on the other's axis.
 *
 * Both series are sparse and windowed by *date*, like Sleep and Heart: a day the watch never sent
 * and a day with no workout are both gaps, and their x-position is what says so.
 */
@Composable
internal fun ActivityTabContent(uiState: ProgressUiState, state: ProgressScreenState) {
    if (uiState.stepDays.isEmpty() && uiState.exerciseEntries.isEmpty()) {
        FullScreenState(
            icon = { MascotAvatar(state = MascotState.Idle, size = 64.dp) },
            heading = "No activity yet",
            body = "Connect Google Health for steps, or log a workout from the diary.",
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

        val steps = uiState.stepDays.inRange(state.range, today)
        SectionLabel(text = "Steps", modifier = Modifier.padding(top = 16.dp))
        DayBarChart(
            bars = steps.map { DayBar(it.dateEpochDay, it.steps) },
            fromEpochDay = from,
            toEpochDay = today,
            minAxisValue = uiState.stepGoal,
            goalValue = uiState.stepGoal,
            modifier = Modifier.padding(top = 8.dp),
        )
        val stepStats = steps.stepAverages(uiState.stepGoal)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatCell(label = "Avg steps", value = stepsLabel(stepStats.averageSteps))
            StatCell(label = "Best day", value = stepsLabel(stepStats.bestSteps))
            // "today's goal", not "goal": the target isn't snapshotted per day, so raising it
            // re-scores every day in the window. See Profile.stepGoal.
            StatCell(label = "Hit today's goal", value = "${stepStats.daysHitGoal} of ${stepStats.days}")
        }

        // Both sources folded by burnSeries, so an imported walk isn't counted twice. This is what
        // was burned — whether it reaches the calorie budget is addExerciseToBudget's business.
        val burn = burnSeries(uiState.stepDays, uiState.exerciseEntries).inRange(state.range, today)
        SectionLabel(text = "Calories burned", modifier = Modifier.padding(top = 24.dp))
        DayBarChart(
            bars = burn.map { DayBar(it.dateEpochDay, it.burnedKcal) },
            fromEpochDay = from,
            toEpochDay = today,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatCell(label = "Workouts", value = "${burn.sumOf { it.workouts }}")
            StatCell(label = "Minutes", value = "${burn.sumOf { it.minutes }}")
            StatCell(label = "Burned", value = "%,d kcal".format(burn.sumOf { it.burnedKcal }))
        }
    }
}

/** The only tab that needs to name its charts, so the label lives here rather than in the theme. */
@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

private fun stepsLabel(steps: Int?): String = steps?.let(::formatSteps) ?: "—"

@PreviewLightDark
@Composable
private fun ActivityTabPreview() {
    val today = todayEpochDay()
    AppTheme {
        ActivityTabContent(
            uiState = ProgressUiState(
                stepDays = listOf(8_200, 11_400, 6_050, 12_900).mapIndexed { index, steps ->
                    StepDay(today - 3 + index, steps, burnedKcal = steps / 27)
                },
                exerciseEntries = listOf(
                    ExerciseEntry(id = 1, dateEpochDay = today - 2, type = ExerciseType.Run, minutes = 35, burnedKcal = 400, steps = 3_500),
                    ExerciseEntry(id = 2, dateEpochDay = today, type = ExerciseType.Strength, minutes = 45, burnedKcal = 260),
                ),
            ),
            state = ProgressScreenState(),
        )
    }
}

/** No watch and no logged workout: hidden rather than two charts of zeros. */
@PreviewLightDark
@Composable
private fun ActivityTabEmptyPreview() {
    AppTheme { ActivityTabContent(uiState = ProgressUiState(), state = ProgressScreenState()) }
}
