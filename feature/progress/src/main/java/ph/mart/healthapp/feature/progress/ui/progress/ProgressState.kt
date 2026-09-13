package ph.mart.healthapp.feature.progress.ui.progress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.progress.ChartRange

@Composable
internal fun rememberProgressScreenState(): ProgressScreenState =
    rememberSaveable(saver = ProgressScreenState.Saver()) { ProgressScreenState() }

/** The range a subject's chart opens on, before the user picks another. */
internal val DEFAULT_CHART_RANGE = ChartRange.ThreeMonths

/**
 * UI-only, and down to what the **overview** holds: which groups are expanded, and the two log
 * sheets its empty-card hints can raise.
 *
 * Every subject page is a route now, so there is no `selectedSubject` and no pending-route
 * indirection — a tap is a callback that reaches `AppScaffold` directly. The chart ranges, the
 * add-measurement sheet, the energy check-in and the meal gallery all left with the pages that own
 * them. See `DECISIONS.md` -> **Progress, recap & the energy check-in**.
 */
internal class ProgressScreenState(
    expandedGroups: Set<SubjectGroup> = emptySet(),
    activeBloodPressureSheet: Boolean = false,
    activeCycleSheet: Boolean = false,
) {
    /** A group with nothing tracked collapses to one row; this is the ones the user has opened. */
    var expandedGroups: Set<SubjectGroup> by mutableStateOf(expandedGroups)

    var activeBloodPressureSheet: Boolean by mutableStateOf(activeBloodPressureSheet)
    var activeCycleSheet: Boolean by mutableStateOf(activeCycleSheet)

    fun toggleGroup(group: SubjectGroup) {
        expandedGroups = if (group in expandedGroups) expandedGroups - group else expandedGroups + group
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

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun Saver(): Saver<ProgressScreenState, Any> = listSaver(
            // Appended, never renumbered: an index that moves restores the wrong field into the
            // wrong overlay. It was renumbered anyway on every commit that moved a field out, both
            // halves together each time — the recap's period into `RecapViewModel`; the recap and
            // the timelapse becoming routes; the photo selection leaving with the Photos page; then
            // one field per subject page as each became a route of its own, and finally the
            // selected subject itself when the last one did.
            //
            // What is left is the overview's, and there is nothing further to move out of it.
            save = {
                listOf(
                    it.expandedGroups.map { group -> group.name },
                    it.activeBloodPressureSheet,
                    it.activeCycleSheet,
                )
            },
            restore = { saved ->
                ProgressScreenState(
                    expandedGroups = (saved[0] as List<String>).mapNotNull(::groupOrNull).toSet(),
                    activeBloodPressureSheet = saved[1] as Boolean,
                    activeCycleSheet = saved[2] as Boolean,
                )
            },
        )
    }
}

/** Null rather than a throw for a name this build doesn't know — the degrade
 * `mascotCharacterOf()` and the Home layout parser already give a retired name. */
private fun groupOrNull(name: String): SubjectGroup? = SubjectGroup.entries.firstOrNull { it.name == name }
