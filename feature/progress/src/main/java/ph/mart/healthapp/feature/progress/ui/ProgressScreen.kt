package ph.mart.healthapp.feature.progress.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.progress.inRange
import ph.mart.healthapp.core.data.progress.withMovingAverage
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.components.MeasurementRow
import ph.mart.healthapp.feature.progress.ui.components.PhotoComparisonScreen
import ph.mart.healthapp.feature.progress.ui.components.ProgressPhotoGrid
import ph.mart.healthapp.feature.progress.ui.components.WeightProgressChart
import ph.mart.healthapp.feature.progress.ui.components.WeightStatRow

@Composable
fun ProgressScreen(scrollState: ScrollState = rememberScrollState(), viewModel: ProgressViewModel = koinViewModel()) {
    val uiState by viewModel.collectAsState()
    val state = rememberProgressScreenState()
    ProgressContent(uiState = uiState, state = state, scrollState = scrollState)
}

@Composable
private fun ProgressContent(uiState: ProgressUiState, state: ProgressScreenState, scrollState: ScrollState = rememberScrollState()) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp)) {
                SegmentedToggle(
                    options = ProgressTab.entries.map { it.name },
                    selectedIndex = ProgressTab.entries.indexOf(state.tab),
                    onSelect = { index -> state.tab = ProgressTab.entries[index] },
                )
                // Photos scrolls itself (LazyVerticalGrid) — nesting it in the verticalScroll
                // Column measures it with infinite height and throws. The other two tabs are
                // plain Columns and need the shared scroll.
                when (state.tab) {
                    ProgressTab.Photos -> PhotosTabContent(uiState, state)
                    ProgressTab.Weight -> ScrollingTab(scrollState) { WeightTabContent(uiState, state) }
                    ProgressTab.Measurements -> ScrollingTab(scrollState) { MeasurementsTabContent(uiState, state) }
                }
            }

            val selectedPhotos = uiState.photos.filter { it.id in state.selectedPhotoIds }
            if (selectedPhotos.size == 2) {
                val (older, newer) = selectedPhotos.sortedBy { it.dateEpochDay }
                PhotoComparisonScreen(
                    photoA = older,
                    photoB = newer,
                    unit = uiState.preferredUnit,
                    onClose = { state.selectedPhotoIds = emptyList() },
                )
            }

            if (state.activeMeasurementSheet) {
                AddMeasurementSheet(
                    trackedParts = uiState.measurements.keys,
                    preselectedPart = state.measurementSheetPart,
                    unit = uiState.preferredUnit,
                    onDismiss = state::closeMeasurementSheet,
                )
            }
        }
    }
}

@Composable
private fun ScrollingTab(scrollState: ScrollState, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(top = 16.dp),
        content = content,
    )
}

@Composable
private fun WeightTabContent(uiState: ProgressUiState, state: ProgressScreenState) {
    if (uiState.weightEntries.isEmpty()) {
        FullScreenState(
            icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
            heading = "No weight logged yet",
            body = "Log your weight from the FAB to start tracking your trend.",
        )
        return
    }
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
}

@Composable
private fun PhotosTabContent(uiState: ProgressUiState, state: ProgressScreenState) {
    if (uiState.photos.isEmpty()) {
        FullScreenState(
            icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
            heading = "No progress photos yet",
            body = "Add your first photo from the FAB to start tracking changes over time.",
        )
        return
    }
    ProgressPhotoGrid(
        photos = uiState.photos,
        selectedIds = state.selectedPhotoIds,
        onToggleSelect = state::togglePhotoSelection,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun MeasurementsTabContent(uiState: ProgressUiState, state: ProgressScreenState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MeasurementPart.entries.filter { it in uiState.measurements }.forEach { part ->
            MeasurementRow(
                name = part.name,
                historyCm = uiState.measurements[part].orEmpty().sortedBy { it.dateEpochDay }.map { it.valueCm },
                unit = uiState.preferredUnit,
                onTap = { state.openMeasurementSheet(part) },
            )
        }
        PrimaryButton(
            label = "+ Add measurement",
            onClick = { state.openMeasurementSheet(null) },
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun ProgressScreenPreview() {
    val entries = listOf(
        WeightEntry(dateEpochDay = 0, weightKg = 78.0),
        WeightEntry(dateEpochDay = 1, weightKg = 77.5),
        WeightEntry(dateEpochDay = 2, weightKg = 76.9),
    )
    AppTheme {
        ProgressContent(
            uiState = ProgressUiState(
                weightEntries = entries,
                goalWeightKg = 72.0,
                goal = Goal.Lose,
                preferredUnit = UnitSystem.Metric,
            ),
            state = ProgressScreenState(),
        )
    }
}
