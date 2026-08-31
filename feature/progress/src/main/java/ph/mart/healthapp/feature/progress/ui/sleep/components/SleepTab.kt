package ph.mart.healthapp.feature.progress.ui.sleep.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.health.SleepNight
import ph.mart.healthapp.core.data.health.formatDuration
import ph.mart.healthapp.core.data.health.inRange
import ph.mart.healthapp.core.data.health.sleepAverages
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreenState
import ph.mart.healthapp.feature.progress.ui.shared.components.DayBar
import ph.mart.healthapp.feature.progress.ui.shared.components.DayBarChart
import ph.mart.healthapp.feature.progress.ui.progress.ProgressUiState
import ph.mart.healthapp.feature.progress.ui.weight.components.StatCell

/** A full night. The y-axis never shrinks below it, so a run of four-hour nights reads as short
 * rather than filling the canvas the way an auto-ranged axis would let it. */
private const val FULL_NIGHT_MINUTES = 480

@Composable
internal fun SleepTabContent(uiState: ProgressUiState, state: ProgressScreenState) {
    if (uiState.sleepNights.isEmpty()) {
        FullScreenState(
            icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
            heading = "No sleep data yet",
            body = "Connect Google Health in Profile and your nights show up here.",
        )
        return
    }
    Column {
        SegmentedToggle(
            options = ChartRange.entries.map { it.label },
            selectedIndex = ChartRange.entries.indexOf(state.range),
            onSelect = { index -> state.range = ChartRange.entries[index] },
        )
        // Windowed by date rather than sliced off the end, for the same reason the mood series is:
        // nights are sparse, so the chart needs the window's bounds to know where the gaps are.
        val today = todayEpochDay()
        val from = today - (state.range.days ?: ChartRange.OneYear.days!!)
        val nights = uiState.sleepNights.inRange(state.range, today)
        DayBarChart(
            bars = nights.map { DayBar(it.dateEpochDay, it.minutesAsleep) },
            fromEpochDay = from,
            toEpochDay = today,
            minAxisValue = FULL_NIGHT_MINUTES,
            modifier = Modifier.padding(top = 16.dp),
        )
        val averages = nights.sleepAverages()
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatCell(label = "Avg night", value = durationLabel(averages.averageMinutes))
            StatCell(label = "Longest", value = durationLabel(averages.longestMinutes))
            StatCell(label = "Nights", value = "${averages.nights}")
        }
    }
}

private fun durationLabel(minutes: Int?): String = minutes?.let(::formatDuration) ?: "—"

@PreviewLightDark
@Composable
private fun SleepTabPreview() {
    val today = todayEpochDay()
    AppTheme {
        SleepTabContent(
            uiState = ProgressUiState(
                sleepNights = listOf(432, 401, 512, 388).mapIndexed { index, minutes ->
                    SleepNight(today - 3 + index, minutes)
                },
            ),
            state = ProgressScreenState(),
        )
    }
}

/** No watch connected: hidden rather than a chart of zeros. */
@PreviewLightDark
@Composable
private fun SleepTabEmptyPreview() {
    AppTheme { SleepTabContent(uiState = ProgressUiState(), state = ProgressScreenState()) }
}
