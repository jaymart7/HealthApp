package ph.mart.healthapp.feature.progress.ui.supplement.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.data.supplement.SupplementDay
import ph.mart.healthapp.core.data.supplement.adherenceByDay
import ph.mart.healthapp.core.data.supplement.averageAdherence
import ph.mart.healthapp.core.data.supplement.inRange
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreenState
import ph.mart.healthapp.feature.progress.ui.progress.ProgressUiState
import ph.mart.healthapp.feature.progress.ui.weight.components.StatCell

@Composable
internal fun SupplementsTabContent(uiState: ProgressUiState, state: ProgressScreenState) {
    if (uiState.supplementDays.isEmpty()) {
        FullScreenState(
            icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
            heading = "No supplements ticked yet",
            body = "Add what you take in Profile, then tick it off on Home and it shows up here.",
        )
        return
    }
    Column {
        SegmentedToggle(
            options = ChartRange.entries.map { it.label },
            selectedIndex = ChartRange.entries.indexOf(state.range),
            onSelect = { index -> state.range = ChartRange.entries[index] },
        )
        // Windowed by date rather than sliced off the end, like the Mood tab: the series is sparse,
        // so the chart needs the window's bounds to know where the gaps are.
        val today = todayEpochDay()
        val from = today - (state.range.days ?: ChartRange.OneYear.days!!)
        val days = uiState.supplementDays.inRange(state.range, today)
        SupplementAdherenceChart(
            days = days,
            fromEpochDay = from,
            toEpochDay = today,
            modifier = Modifier.padding(top = 16.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatCell(label = "Avg taken", value = percentLabel(days.averageAdherence()))
            // Days where every dose was taken — the run worth being proud of, and a stricter read
            // than the average beside it.
            StatCell(label = "Full days", value = "${days.adherenceByDay().count { it.second >= 1f }}")
            StatCell(label = "Days logged", value = "${days.adherenceByDay().size}")
        }
    }
}

private fun percentLabel(value: Float?): String = value?.let { "${(it * 100).toInt()}%" } ?: "—"

@PreviewLightDark
@Composable
private fun SupplementsTabPreview() {
    val today = todayEpochDay()
    AppTheme {
        SupplementsTabContent(
            uiState = ProgressUiState(
                supplementDays = listOf(2 to 2, 1 to 2, 2 to 2, 0 to 2, 2 to 2)
                    .mapIndexed { index, (taken, due) ->
                        SupplementDay(today - 4 + index, supplementId = 1, taken = taken, dueTimes = due)
                    },
            ),
            state = ProgressScreenState(),
        )
    }
}

/** Nothing ticked yet: the tab is a mascot rather than an empty axis. */
@PreviewLightDark
@Composable
private fun SupplementsTabEmptyPreview() {
    AppTheme {
        SupplementsTabContent(uiState = ProgressUiState(), state = ProgressScreenState())
    }
}
