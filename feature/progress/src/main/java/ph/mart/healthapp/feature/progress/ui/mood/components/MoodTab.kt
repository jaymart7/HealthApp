package ph.mart.healthapp.feature.progress.ui.mood.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.mood.MOOD_SCALE
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.mood.inRange
import ph.mart.healthapp.core.data.mood.moodAverages
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

@Composable
internal fun MoodTabContent(uiState: ProgressUiState, state: ProgressScreenState) {
    if (uiState.moodDays.isEmpty()) {
        FullScreenState(
            icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
            heading = "No mood logged yet",
            body = "Tap how you're feeling on the Home screen and it shows up here.",
        )
        return
    }
    Column {
        SegmentedToggle(
            options = ChartRange.entries.map { it.label },
            selectedIndex = ChartRange.entries.indexOf(state.range),
            onSelect = { index -> state.range = ChartRange.entries[index] },
        )
        // Windowed by date rather than sliced off the end: the series is sparse, so the chart
        // needs the window's bounds to know where the gaps are.
        val today = todayEpochDay()
        val from = today - (state.range.days ?: ChartRange.OneYear.days!!)
        val days = uiState.moodDays.inRange(state.range, today)
        MoodTrendChart(
            days = days,
            fromEpochDay = from,
            toEpochDay = today,
            modifier = Modifier.padding(top = 16.dp),
        )
        val averages = days.moodAverages()
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatCell(label = "Avg mood", value = averageLabel(averages.mood))
            StatCell(label = "Avg energy", value = averageLabel(averages.energy))
            StatCell(label = "Days logged", value = "${averages.daysLogged}")
        }
    }
}

private fun averageLabel(value: Double?): String =
    value?.let { "%.1f / ${MOOD_SCALE.last}".format(it) } ?: "—"

@PreviewLightDark
@Composable
private fun MoodTabPreview() {
    val today = todayEpochDay()
    AppTheme {
        MoodTabContent(
            uiState = ProgressUiState(
                moodDays = listOf(4 to 3, 5 to 4, 3 to 2, 4 to 4).mapIndexed { index, (mood, energy) ->
                    MoodDay(today - 4 + index, mood, energy)
                },
            ),
            state = ProgressScreenState(),
        )
    }
}

/** A mood-only week: energy is 0 throughout, so its average cell reads "—" rather than 0.0. */
@PreviewLightDark
@Composable
private fun MoodTabEnergylessPreview() {
    val today = todayEpochDay()
    AppTheme {
        MoodTabContent(
            uiState = ProgressUiState(
                moodDays = listOf(4, 5, 3, 4).mapIndexed { index, mood -> MoodDay(today - 3 + index, mood, 0) },
            ),
            state = ProgressScreenState(),
        )
    }
}
