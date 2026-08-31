package ph.mart.healthapp.feature.progress.ui.fasting.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.fasting.dateEpochDay
import ph.mart.healthapp.core.data.fasting.durationMinutes
import ph.mart.healthapp.core.data.fasting.fastingAverages
import ph.mart.healthapp.core.data.fasting.inRange
import ph.mart.healthapp.core.data.health.formatDuration
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

/** The y-axis never shrinks below a day, so a run of short fasts reads as short rather than
 * filling the canvas the way an auto-ranged axis would let it. */
private const val FULL_DAY_MINUTES = 24 * 60

@Composable
internal fun FastingTabContent(uiState: ProgressUiState, state: ProgressScreenState) {
    if (uiState.fastSessions.isEmpty()) {
        FullScreenState(
            icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
            heading = "No fasts yet",
            body = "Start one from the Home screen and it lands here when you end it.",
        )
        return
    }
    Column {
        SegmentedToggle(
            options = ChartRange.entries.map { it.label },
            selectedIndex = ChartRange.entries.indexOf(state.range),
            onSelect = { index -> state.range = ChartRange.entries[index] },
        )
        // Windowed by date rather than sliced off the end, for the same reason the mood and sleep
        // series are: fasts are sparse, so the chart needs the window's bounds to place the gaps.
        val today = todayEpochDay()
        val from = today - (state.range.days ?: ChartRange.OneYear.days!!)
        val sessions = uiState.fastSessions.inRange(state.range, today)
        DayBarChart(
            // A completed fast is placed on the day it ended, and priced with nowMillis = 0
            // because every session here has an end.
            bars = sessions.map { DayBar(it.dateEpochDay, it.durationMinutes(nowMillis = 0)) },
            fromEpochDay = from,
            toEpochDay = today,
            minAxisValue = FULL_DAY_MINUTES,
            // A fast's whole point is whether it cleared the target, and a bar you have to measure
            // against an axis doesn't say that.
            goalValue = uiState.fastingGoalHours * 60,
            modifier = Modifier.padding(top = 16.dp),
        )
        val averages = sessions.fastingAverages()
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatCell(label = "Avg fast", value = durationLabel(averages.averageMinutes))
            StatCell(label = "Longest", value = durationLabel(averages.longestMinutes))
            // Out of the fasts in the window, not out of the days in it — a week with two fasts
            // and five untracked days reports 2, not 2/7.
            StatCell(label = "Goals hit", value = "${averages.goalsHit} / ${averages.count}")
        }
    }
}

private fun durationLabel(minutes: Int?): String = minutes?.let(::formatDuration) ?: "—"

private fun previewSessions(today: Long): List<FastSession> =
    listOf(15, 17, 12, 18).mapIndexed { index, hours ->
        val end = (today - 3 + index) * 86_400_000L + 12 * 3_600_000L
        FastSession(
            id = index.toLong(),
            startMillis = end - hours * 3_600_000L,
            endMillis = end,
            goalHours = 16,
        )
    }

@PreviewLightDark
@Composable
private fun FastingTabPreview() {
    val today = todayEpochDay()
    AppTheme {
        FastingTabContent(
            uiState = ProgressUiState(fastSessions = previewSessions(today), fastingGoalHours = 16),
            state = ProgressScreenState(),
        )
    }
}

/** Nothing started yet: the invitation, not a chart of zeros. */
@PreviewLightDark
@Composable
private fun FastingTabEmptyPreview() {
    AppTheme { FastingTabContent(uiState = ProgressUiState(), state = ProgressScreenState()) }
}
