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

/** The range a subject's chart opens on, before the user picks another. */
internal val DEFAULT_CHART_RANGE = ChartRange.ThreeMonths

/** UI-only — which subject is open, which range its chart is showing, which photos are selected
 * has no business meaning outside this screen; the actual weight/measurement/photo data lives in
 * [ProgressUiState]. */
internal class ProgressScreenState(
    selectedSubject: Subject? = null,
    ranges: Map<Subject, ChartRange> = emptyMap(),
    expandedGroups: Set<SubjectGroup> = emptySet(),
    selectedPhotoIds: List<Long> = emptyList(),
    activeMeasurementSheet: Boolean = false,
    measurementSheetPart: MeasurementPart? = null,
    activeBloodPressureSheet: Boolean = false,
    activeCycleSheet: Boolean = false,
    activeRecap: Boolean = false,
    recapPeriod: RecapPeriod = DEFAULT_RECAP_PERIOD,
    activeTimelapse: Boolean = false,
    activeEnergyCheckIn: Boolean = false,
    pendingDeleteReadingId: Long? = null,
) {
    /** Null is the overview. A detail page is a swap-in inside this tab rather than a route, so it
     * keeps the bottom bar and the FAB and costs no second copy of [ProgressViewModel]. */
    var selectedSubject: Subject? by mutableStateOf(selectedSubject)

    /** Per subject, for the session — the range toggle now lives inside each chart card, so one
     * shared range would have a tap on the Sleep chart silently re-slice the Weight one. */
    var ranges: Map<Subject, ChartRange> by mutableStateOf(ranges)

    /** A group with nothing tracked collapses to one row; this is the ones the user has opened. */
    var expandedGroups: Set<SubjectGroup> by mutableStateOf(expandedGroups)

    var selectedPhotoIds: List<Long> by mutableStateOf(selectedPhotoIds)
    var activeMeasurementSheet: Boolean by mutableStateOf(activeMeasurementSheet)
    var measurementSheetPart: MeasurementPart? by mutableStateOf(measurementSheetPart)
    var activeBloodPressureSheet: Boolean by mutableStateOf(activeBloodPressureSheet)
    var activeCycleSheet: Boolean by mutableStateOf(activeCycleSheet)
    var activeRecap: Boolean by mutableStateOf(activeRecap)
    var recapPeriod: RecapPeriod by mutableStateOf(recapPeriod)
    var activeTimelapse: Boolean by mutableStateOf(activeTimelapse)
    var activeEnergyCheckIn: Boolean by mutableStateOf(activeEnergyCheckIn)

    /** The reading whose delete is waiting on its confirmation dialog. */
    var pendingDeleteReadingId: Long? by mutableStateOf(pendingDeleteReadingId)

    fun rangeFor(subject: Subject): ChartRange = ranges[subject] ?: DEFAULT_CHART_RANGE

    fun setRange(subject: Subject, range: ChartRange) {
        ranges = ranges + (subject to range)
    }

    fun open(subject: Subject) {
        selectedSubject = subject
    }

    fun closeSubject() {
        selectedSubject = null
    }

    fun toggleGroup(group: SubjectGroup) {
        expandedGroups = if (group in expandedGroups) expandedGroups - group else expandedGroups + group
    }

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

    fun openCycleSheet() {
        activeCycleSheet = true
    }

    fun closeCycleSheet() {
        activeCycleSheet = false
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

    fun openEnergyCheckIn() {
        activeEnergyCheckIn = true
    }

    fun closeEnergyCheckIn() {
        activeEnergyCheckIn = false
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun Saver(): Saver<ProgressScreenState, Any> = listSaver(
            save = {
                listOf(
                    it.selectedSubject?.name,
                    // Flattened to a String list: the saver's bundle takes primitives, and a
                    // subject the restoring build doesn't know is dropped rather than crashing.
                    it.ranges.flatMap { (subject, range) -> listOf(subject.name, range.name) },
                    it.expandedGroups.map { group -> group.name },
                    it.selectedPhotoIds,
                    it.activeMeasurementSheet, it.measurementSheetPart?.name,
                    it.activeBloodPressureSheet, it.activeRecap, it.recapPeriod.name, it.activeTimelapse,
                    it.activeEnergyCheckIn, it.pendingDeleteReadingId, it.activeCycleSheet,
                )
            },
            restore = { saved ->
                val flatRanges = (saved[1] as List<String>).chunked(2).filter { it.size == 2 }
                ProgressScreenState(
                    selectedSubject = (saved[0] as String?)?.let(::subjectOrNull),
                    ranges = flatRanges
                        .mapNotNull { (name, range) ->
                            subjectOrNull(name)?.let { it to ChartRange.valueOf(range) }
                        }
                        .toMap(),
                    expandedGroups = (saved[2] as List<String>).mapNotNull(::groupOrNull).toSet(),
                    selectedPhotoIds = saved[3] as List<Long>,
                    activeMeasurementSheet = saved[4] as Boolean,
                    measurementSheetPart = (saved[5] as String?)?.let(MeasurementPart::valueOf),
                    activeBloodPressureSheet = saved[6] as Boolean,
                    activeRecap = saved[7] as Boolean,
                    recapPeriod = RecapPeriod.valueOf(saved[8] as String),
                    activeTimelapse = saved[9] as Boolean,
                    activeEnergyCheckIn = saved[10] as Boolean,
                    pendingDeleteReadingId = saved[11] as Long?,
                    activeCycleSheet = saved[12] as Boolean,
                )
            },
        )
    }
}

/** Null rather than a throw for a name this build doesn't know — the degrade
 * `mascotCharacterOf()` and the Home layout parser already give a retired name. */
private fun subjectOrNull(name: String): Subject? = Subject.entries.firstOrNull { it.name == name }

private fun groupOrNull(name: String): SubjectGroup? = SubjectGroup.entries.firstOrNull { it.name == name }
