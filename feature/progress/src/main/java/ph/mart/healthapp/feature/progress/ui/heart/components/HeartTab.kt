package ph.mart.healthapp.feature.progress.ui.heart.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.health.HeartDay
import ph.mart.healthapp.core.data.health.formatBpm
import ph.mart.healthapp.core.data.health.heartAverages
import ph.mart.healthapp.core.data.health.inRange
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreenState
import ph.mart.healthapp.feature.progress.ui.progress.ProgressUiState
import ph.mart.healthapp.feature.progress.ui.weight.components.StatCell

/**
 * The second stat reads "Lowest", never "Resting". FitPulse aggregates whatever samples the watch
 * happened to take, and a minimum is a minimum — calling it a resting heart rate would claim a
 * measurement nobody made.
 */
@Composable
internal fun HeartTabContent(uiState: ProgressUiState, state: ProgressScreenState) {
    if (uiState.heartDays.isEmpty()) {
        FullScreenState(
            icon = { MascotAvatar(state = MascotState.Idle, size = 64.dp) },
            heading = "No heart data yet",
            body = "Connect Google Health in Profile and your readings show up here.",
        )
        return
    }
    Column {
        SegmentedToggle(
            options = ChartRange.entries.map { it.label },
            selectedIndex = ChartRange.entries.indexOf(state.range),
            onSelect = { index -> state.range = ChartRange.entries[index] },
        )
        // Windowed by date rather than sliced off the end, like sleep and mood: the readings are
        // sparse, so the chart needs the window's bounds to know where the gaps are.
        val today = todayEpochDay()
        val from = today - (state.range.days ?: ChartRange.OneYear.days!!)
        val days = uiState.heartDays.inRange(state.range, today)
        HeartTrendChart(
            days = days,
            fromEpochDay = from,
            toEpochDay = today,
            modifier = Modifier.padding(top = 16.dp),
        )
        val averages = days.heartAverages()
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatCell(label = "Avg", value = bpmLabel(averages.averageBpm))
            StatCell(label = "Lowest", value = bpmLabel(averages.lowestBpm))
            StatCell(label = "Days", value = "${averages.days}")
        }
    }
}

private fun bpmLabel(bpm: Int?): String = bpm?.let(::formatBpm) ?: "—"

@PreviewLightDark
@Composable
private fun HeartTabPreview() {
    val today = todayEpochDay()
    AppTheme {
        HeartTabContent(
            uiState = ProgressUiState(
                heartDays = listOf(68 to 52, 71 to 55, 66 to 49, 74 to 58)
                    .mapIndexed { index, (average, min) -> HeartDay(today - 3 + index, average, min) },
            ),
            state = ProgressScreenState(),
        )
    }
}

/** No watch connected: hidden rather than a chart of zeros. */
@PreviewLightDark
@Composable
private fun HeartTabEmptyPreview() {
    AppTheme { HeartTabContent(uiState = ProgressUiState(), state = ProgressScreenState()) }
}
