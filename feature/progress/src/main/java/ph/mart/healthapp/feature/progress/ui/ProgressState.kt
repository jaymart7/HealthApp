package ph.mart.healthapp.feature.progress.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.data.progress.MeasurementPart

@Composable
internal fun rememberProgressScreenState(): ProgressScreenState =
    rememberSaveable(saver = ProgressScreenState.Saver()) { ProgressScreenState() }

/** UI-only — which sub-tab/range/selected photos are showing has no business meaning outside
 * this screen; the actual weight/measurement/photo data lives in [ProgressUiState]. */
internal class ProgressScreenState(
    tab: ProgressTab = ProgressTab.Weight,
    range: ChartRange = ChartRange.ThreeMonths,
    selectedPhotoIds: List<Long> = emptyList(),
    activeMeasurementSheet: Boolean = false,
    measurementSheetPart: MeasurementPart? = null,
) {
    var tab: ProgressTab by mutableStateOf(tab)
    var range: ChartRange by mutableStateOf(range)
    var selectedPhotoIds: List<Long> by mutableStateOf(selectedPhotoIds)
    var activeMeasurementSheet: Boolean by mutableStateOf(activeMeasurementSheet)
    var measurementSheetPart: MeasurementPart? by mutableStateOf(measurementSheetPart)

    fun togglePhotoSelection(id: Long) {
        selectedPhotoIds = when {
            id in selectedPhotoIds -> selectedPhotoIds - id
            selectedPhotoIds.size >= 2 -> selectedPhotoIds.drop(1) + id
            else -> selectedPhotoIds + id
        }
    }

    fun openMeasurementSheet(part: MeasurementPart?) {
        measurementSheetPart = part
        activeMeasurementSheet = true
    }

    fun closeMeasurementSheet() {
        activeMeasurementSheet = false
    }

    companion object {
        fun Saver(): Saver<ProgressScreenState, Any> = listSaver(
            save = {
                listOf(
                    it.tab.name, it.range.name, it.selectedPhotoIds,
                    it.activeMeasurementSheet, it.measurementSheetPart?.name,
                )
            },
            restore = { saved ->
                ProgressScreenState(
                    tab = ProgressTab.valueOf(saved[0] as String),
                    range = ChartRange.valueOf(saved[1] as String),
                    selectedPhotoIds = saved[2] as List<Long>,
                    activeMeasurementSheet = saved[3] as Boolean,
                    measurementSheetPart = (saved[4] as String?)?.let(MeasurementPart::valueOf),
                )
            },
        )
    }
}
