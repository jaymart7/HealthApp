package ph.mart.healthapp.feature.progress.ui.progress

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
    activeBloodPressureSheet: Boolean = false,
    activeRecap: Boolean = false,
    recapPeriod: RecapPeriod = DEFAULT_RECAP_PERIOD,
    activeTimelapse: Boolean = false,
    pendingDeleteReadingId: Long? = null,
) {
    var tab: ProgressTab by mutableStateOf(tab)
    var range: ChartRange by mutableStateOf(range)
    var selectedPhotoIds: List<Long> by mutableStateOf(selectedPhotoIds)
    var activeMeasurementSheet: Boolean by mutableStateOf(activeMeasurementSheet)
    var measurementSheetPart: MeasurementPart? by mutableStateOf(measurementSheetPart)
    var activeBloodPressureSheet: Boolean by mutableStateOf(activeBloodPressureSheet)
    var activeRecap: Boolean by mutableStateOf(activeRecap)
    var recapPeriod: RecapPeriod by mutableStateOf(recapPeriod)
    var activeTimelapse: Boolean by mutableStateOf(activeTimelapse)

    /** The reading whose delete is waiting on its confirmation dialog. */
    var pendingDeleteReadingId: Long? by mutableStateOf(pendingDeleteReadingId)

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

    fun openBloodPressureSheet() {
        activeBloodPressureSheet = true
    }

    fun closeBloodPressureSheet() {
        activeBloodPressureSheet = false
    }

    fun openRecap() {
        activeRecap = true
    }

    fun closeRecap() {
        activeRecap = false
    }

    fun openTimelapse() {
        activeTimelapse = true
    }

    fun closeTimelapse() {
        activeTimelapse = false
    }

    companion object {
        fun Saver(): Saver<ProgressScreenState, Any> = listSaver(
            save = {
                listOf(
                    it.tab.name, it.range.name, it.selectedPhotoIds,
                    it.activeMeasurementSheet, it.measurementSheetPart?.name,
                    it.activeBloodPressureSheet, it.activeRecap, it.recapPeriod.name, it.activeTimelapse,
                    it.pendingDeleteReadingId,
                )
            },
            restore = { saved ->
                ProgressScreenState(
                    tab = ProgressTab.valueOf(saved[0] as String),
                    range = ChartRange.valueOf(saved[1] as String),
                    selectedPhotoIds = saved[2] as List<Long>,
                    activeMeasurementSheet = saved[3] as Boolean,
                    measurementSheetPart = (saved[4] as String?)?.let(MeasurementPart::valueOf),
                    activeBloodPressureSheet = saved[5] as Boolean,
                    activeRecap = saved[6] as Boolean,
                    recapPeriod = RecapPeriod.valueOf(saved[7] as String),
                    activeTimelapse = saved[8] as Boolean,
                    pendingDeleteReadingId = saved[9] as Long?,
                )
            },
        )
    }
}
