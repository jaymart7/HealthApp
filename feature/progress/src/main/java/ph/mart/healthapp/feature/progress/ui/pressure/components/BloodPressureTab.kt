package ph.mart.healthapp.feature.progress.ui.pressure.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureReading
import ph.mart.healthapp.core.data.bloodpressure.averages
import ph.mart.healthapp.core.data.bloodpressure.byDay
import ph.mart.healthapp.core.data.bloodpressure.inRange
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.DiscardConfirmDialog
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.pressure.BloodPressureEvent
import ph.mart.healthapp.feature.progress.ui.pressure.BloodPressureViewModel
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreenState
import ph.mart.healthapp.feature.progress.ui.progress.ProgressUiState
import ph.mart.healthapp.feature.progress.ui.shared.components.RangeBar
import ph.mart.healthapp.feature.progress.ui.shared.components.RangeBarChart
import ph.mart.healthapp.feature.progress.ui.weight.components.StatCell

/** Breathing room above and below the window's own readings, so no bar sits flat on an edge. */
private const val AXIS_PAD_MMHG = 10

/**
 * Scrolls itself, like the Photos tab and unlike the other eight: the reading list is per-reading
 * rather than per-day, so a 3M window can hold a couple of hundred rows. That is why
 * `ProgressScreen` dispatches this one outside `ScrollingTab`.
 */
@Composable
internal fun BloodPressureTabContent(
    uiState: ProgressUiState,
    state: ProgressScreenState,
    viewModel: BloodPressureViewModel = koinViewModel(),
) {
    BloodPressureTabBody(uiState = uiState, state = state, onEvent = viewModel::handleEvent)
}

@Composable
private fun BloodPressureTabBody(
    uiState: ProgressUiState,
    state: ProgressScreenState,
    onEvent: (BloodPressureEvent) -> Unit,
) {
    if (uiState.bloodPressure.isEmpty()) {
        // An invitation, not a "connect something" — unlike Heart and Sleep, there is nothing to
        // import here. The only way a reading arrives is the user typing one in.
        FullScreenState(
            icon = { MascotAvatar(state = MascotState.Idle, size = 64.dp) },
            heading = "No readings yet",
            body = "Log the two numbers off your cuff and they'll chart here.",
            actions = {
                PrimaryButton(
                    label = "Log a reading",
                    onClick = { state.openBloodPressureSheet() },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )
        return
    }

    // Windowed by date rather than sliced off the end, like heart, sleep and mood: the readings
    // are sparse, so the chart needs the window's bounds to know where the gaps are.
    val today = todayEpochDay()
    val from = today - (state.range.days ?: ChartRange.OneYear.days!!)
    val readings = uiState.bloodPressure.inRange(state.range, today)
    val averages = readings.averages()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            SegmentedToggle(
                options = ChartRange.entries.map { it.label },
                selectedIndex = ChartRange.entries.indexOf(state.range),
                onSelect = { index -> state.range = ChartRange.entries[index] },
            )
        }
        item {
            // Each day as one bar spanning its mean diastolic up to its mean systolic.
            RangeBarChart(
                bars = readings.byDay().map { RangeBar(it.dateEpochDay, low = it.diastolic, high = it.systolic) },
                fromEpochDay = from,
                toEpochDay = today,
                axisPad = AXIS_PAD_MMHG,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatCell(label = "Systolic", value = statLabel(averages.systolic))
                StatCell(label = "Diastolic", value = statLabel(averages.diastolic))
                // Hidden rather than "—" when no reading in the window carried one: the pulse
                // keeps its own denominator, so an absent one is absent, not zero.
                if (averages.pulseBpm != null) {
                    StatCell(label = "Pulse", value = statLabel(averages.pulseBpm))
                }
                StatCell(label = "Readings", value = "${averages.readings}")
            }
        }
        item {
            PrimaryButton(
                label = "+ Add reading",
                onClick = { state.openBloodPressureSheet() },
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
        }
        // Newest first: the reading someone just took is the one they want to check or correct.
        items(readings.asReversed(), key = { it.id }) { reading ->
            BloodPressureRow(
                reading = reading,
                onDelete = { state.pendingDeleteReadingId = reading.id },
            )
        }
    }

    val pendingId = state.pendingDeleteReadingId
    if (pendingId != null) {
        // Asked rather than undone: the diary's swipe-and-undo needs a snackbar host Progress
        // doesn't have, and a reading is a number the user typed, not a row they swiped.
        DiscardConfirmDialog(
            title = "Delete this reading?",
            body = "It disappears from the chart and the averages.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            onConfirm = {
                onEvent(BloodPressureEvent.OnDelete(pendingId))
                state.pendingDeleteReadingId = null
            },
            onDismiss = { state.pendingDeleteReadingId = null },
        )
    }
}

private fun statLabel(value: Int?): String = value?.toString() ?: "—"

@PreviewLightDark
@Composable
private fun BloodPressureTabPreview() {
    AppTheme {
        Surface {
            BloodPressureTabBody(
                uiState = ProgressUiState(
                    bloodPressure = listOf(
                        Triple(132, 86, 74), Triple(128, 82, 71), Triple(121, 79, 0), Triple(126, 84, 68),
                    ).mapIndexed { index, (systolic, diastolic, pulse) ->
                        BloodPressureReading(
                            id = index.toLong() + 1,
                            takenAtMillis = System.currentTimeMillis() - (3L - index) * 86_400_000L,
                            systolic = systolic,
                            diastolic = diastolic,
                            pulseBpm = pulse,
                        )
                    },
                ),
                state = ProgressScreenState(),
                onEvent = {},
            )
        }
    }
}

/** Nothing logged yet: an invitation with the button that fixes it. */
@PreviewLightDark
@Composable
private fun BloodPressureTabEmptyPreview() {
    AppTheme {
        Surface {
            BloodPressureTabBody(uiState = ProgressUiState(), state = ProgressScreenState(), onEvent = {})
        }
    }
}
