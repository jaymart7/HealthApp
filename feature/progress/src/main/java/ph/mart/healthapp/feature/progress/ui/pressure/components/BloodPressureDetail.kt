package ph.mart.healthapp.feature.progress.ui.pressure.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.pressure.BloodPressureEvent
import ph.mart.healthapp.feature.progress.ui.pressure.BloodPressureViewModel
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
import ph.mart.healthapp.feature.progress.ui.shared.components.RangeBar
import ph.mart.healthapp.feature.progress.ui.shared.components.RangeBarChart

/** Breathing room above and below the window's own readings, so no bar sits flat on an edge. */
private const val AXIS_PAD_MMHG = 10

/**
 * Scrolls itself, like the Photos page and unlike every other subject: the list is per-*reading*
 * rather than per-day, so a 3M window can hold a couple of hundred rows. `SubjectDetail` names it
 * in `SelfScrolling` for that reason, and hands it the room instead of a scrolling column.
 *
 * The one subject on this screen that writes, which is why it carries its own ViewModel and
 * `ProgressViewModel` stays the read-only container its KDoc says it is.
 */
@Composable
internal fun BloodPressureDetailBody(
    uiState: ProgressUiState,
    state: ProgressScreenState,
    viewModel: BloodPressureViewModel = koinViewModel(),
) {
    BloodPressureDetailContent(uiState = uiState, state = state, onEvent = viewModel::handleEvent)
}

@Composable
private fun BloodPressureDetailContent(
    uiState: ProgressUiState,
    state: ProgressScreenState,
    onEvent: (BloodPressureEvent) -> Unit,
) {
    // Windowed by date rather than sliced off the end, like heart, sleep and mood: the readings
    // are sparse, so the chart needs the window's bounds to know where the gaps are.
    val today = todayEpochDay()
    val range = state.rangeFor(Subject.BloodPressure)
    val from = today - (range.days ?: ChartRange.OneYear.days!!)
    val readings = uiState.bloodPressure.inRange(range, today)
    val averages = readings.averages()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = DockedFabContentPadding),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            HeroValue(
                value = averages.systolic?.let { "$it/${averages.diastolic}" } ?: "—",
                caption = "mmHg average",
            )
        }
        item {
            FactChipRow(chips = listOf(FactChip(stringResource(R.string.progress_bp_readings_chip, averages.readings))))
        }
        item {
            ChartCard(
                title = stringResource(R.string.progress_bp_readings),
                range = range,
                onRangeChange = { state.setRange(Subject.BloodPressure, it) },
                legend = listOf(
                    LegendEntry("Diastolic to systolic, per day", MaterialTheme.colorScheme.secondary),
                ),
            ) {
                // Each day as one bar spanning its mean diastolic up to its mean systolic.
                RangeBarChart(
                    bars = readings.byDay().map { RangeBar(it.dateEpochDay, low = it.diastolic, high = it.systolic) },
                    fromEpochDay = from,
                    toEpochDay = today,
                    axisPad = AXIS_PAD_MMHG,
                )
            }
        }
        item {
            StatRowsCard(
                rows = listOfNotNull(
                    StatRow("Systolic", statLabel(averages.systolic)),
                    StatRow("Diastolic", statLabel(averages.diastolic)),
                    // Dropped rather than "—" when no reading in the window carried one: the pulse
                    // keeps its own denominator, so an absent one is absent, not zero.
                    averages.pulseBpm?.let { StatRow("Pulse", statLabel(it)) },
                    StatRow("Readings", "${averages.readings}"),
                ),
            )
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
            title = stringResource(R.string.progress_bp_delete_title),
            body = stringResource(R.string.progress_bp_delete_body),
            confirmLabel = stringResource(R.string.progress_delete),
            dismissLabel = stringResource(R.string.progress_cancel),
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
private fun BloodPressureDetailPreview() {
    AppTheme {
        Surface {
            BloodPressureDetailContent(
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

