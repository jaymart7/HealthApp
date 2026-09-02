package ph.mart.healthapp.feature.progress.ui.weight.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.profile.EnergyCheckIn
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.progress.goalProjection
import ph.mart.healthapp.core.data.progress.inRange
import ph.mart.healthapp.core.data.progress.withMovingAverage
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.energy.components.EnergyCheckInCard
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreenState
import ph.mart.healthapp.feature.progress.ui.progress.ProgressUiState

@Composable
internal fun WeightTabContent(
    uiState: ProgressUiState,
    state: ProgressScreenState,
    checkIn: EnergyCheckIn? = null,
) {
    if (uiState.weightEntries.isEmpty()) {
        FullScreenState(
            icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
            heading = "No weight logged yet",
            body = "Log your weight from the FAB to start tracking your trend.",
        )
        return
    }
    Column {
        SegmentedToggle(
            options = ChartRange.entries.map { it.label },
            selectedIndex = ChartRange.entries.indexOf(state.range),
            onSelect = { index -> state.range = ChartRange.entries[index] },
        )
        val filtered = uiState.weightEntries.inRange(state.range)
        val points = filtered.withMovingAverage()
        WeightProgressChart(
            points = points,
            goalWeightKg = uiState.goalWeightKg,
            unit = uiState.preferredUnit,
            modifier = Modifier.padding(top = 16.dp),
        )
        val sorted = uiState.weightEntries.sortedBy { it.dateEpochDay }
        val current = sorted.last().weightKg
        val prior = if (sorted.size >= 2) sorted[sorted.size - 2].weightKg else current
        WeightStatRow(
            currentKg = current,
            changeKg = current - prior,
            goal = uiState.goal,
            goalWeightKg = uiState.goalWeightKg,
            unit = uiState.preferredUnit,
            modifier = Modifier.padding(top = 16.dp),
        )
        // Under the row that says how far there is to go. Null (no goal weight, a Maintain
        // goal, or too little recent data to fit a rate) omits the card, same as the recap.
        goalProjection(
            weightEntries = uiState.weightEntries,
            goalWeightKg = uiState.goalWeightKg,
            goal = uiState.goal,
            todayEpochDay = todayEpochDay(),
        )?.let { projection ->
            GoalProjectionCard(
                projection = projection,
                unit = uiState.preferredUnit,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        // Under the projection, because it measures the trend that card projects. Null (no food
        // logged in the last four weeks) omits it entirely, the projection card's own rule.
        checkIn?.let {
            EnergyCheckInCard(
                checkIn = it,
                onOpen = state::openEnergyCheckIn,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun WeightTabPreview() {
    val today = todayEpochDay()
    AppTheme {
        WeightTabContent(
            uiState = ProgressUiState(
                weightEntries = listOf(
                    WeightEntry(dateEpochDay = today - 21, weightKg = 79.4),
                    WeightEntry(dateEpochDay = today - 14, weightKg = 78.1),
                    WeightEntry(dateEpochDay = today - 7, weightKg = 77.5),
                    WeightEntry(dateEpochDay = today, weightKg = 76.9),
                ),
                goalWeightKg = 72.0,
                goal = Goal.Lose,
            ),
            state = ProgressScreenState(),
        )
    }
}

/** Nothing logged: the tab is a full-screen prompt, not an empty chart. */
@PreviewLightDark
@Composable
private fun WeightTabEmptyPreview() {
    AppTheme { WeightTabContent(uiState = ProgressUiState(), state = ProgressScreenState()) }
}
