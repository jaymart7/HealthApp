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

/** The surfaces this tab asks for rather than draws — see [ProgressScreenState.pendingRoute].
 * [Page] carries which subject, because a subject page is now a route too. */
internal sealed interface ProgressDestination {
    data object Recap : ProgressDestination
    data class Page(val subject: Subject) : ProgressDestination
}

/**
 * The subjects whose pages are routes rather than swap-ins inside this tab.
 *
 * It grows by one name per conversion commit and is the whole per-commit change to this file, so
 * how far the migration has got is legible in one line. When the last subject joins it, this set,
 * [ProgressScreenState.selectedSubject] and [ProgressScreenState.pendingRoute] all go, and
 * `ProgressOverview` takes a plain `onOpenSubject` instead.
 */
private val RoutedSubjects = setOf(
    Subject.Photos, Subject.Sleep, Subject.Mood, Subject.Heart, Subject.Supplements,
    Subject.Strength, Subject.Fasting, Subject.Activity, Subject.Cycle, Subject.BloodPressure,
    Subject.Measurements, Subject.Weight, Subject.Nutrition, Subject.Badges,
)

/** UI-only — which subject is open and which sheet is up has no business meaning outside this
 * screen; the actual weight/measurement/photo data lives in [ProgressUiState]. */
internal class ProgressScreenState(
    selectedSubject: Subject? = null,
    expandedGroups: Set<SubjectGroup> = emptySet(),
    activeBloodPressureSheet: Boolean = false,
    activeCycleSheet: Boolean = false,
) {
    /** Null is the overview. A detail page is a swap-in inside this tab rather than a route, so it
     * keeps the bottom bar and the FAB and costs no second copy of [ProgressViewModel]. */
    var selectedSubject: Subject? by mutableStateOf(selectedSubject)

    /** A group with nothing tracked collapses to one row; this is the ones the user has opened. */
    var expandedGroups: Set<SubjectGroup> by mutableStateOf(expandedGroups)

    /**
     * The route a tap on this tab has asked for, consumed by [ProgressScreen] and turned into a
     * push. Every open site in here is a `state::openX` or `state::open` call threaded through the
     * overview and `SubjectDetail`'s fourteen-way dispatch — so the tap is recorded here rather
     * than the callbacks being threaded through all of it.
     *
     * Transient on purpose: it never rides the saver. After a process death the back stack has
     * already restored whichever route was open, and a surviving request would push a second copy
     * of it on top.
     */
    var pendingRoute: ProgressDestination? by mutableStateOf(null)
    var activeBloodPressureSheet: Boolean by mutableStateOf(activeBloodPressureSheet)
    var activeCycleSheet: Boolean by mutableStateOf(activeCycleSheet)

    /** A subject that has become a route asks for a push; one that has not is still selected in
     * place. Every entry point in this tab — the overview's cards, the empty-card hints, the
     * sibling switcher — funnels through here, which is what keeps that a single branch. */
    fun open(subject: Subject) {
        if (subject in RoutedSubjects) {
            pendingRoute = ProgressDestination.Page(subject)
        } else {
            selectedSubject = subject
        }
    }

    fun closeSubject() {
        selectedSubject = null
    }

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

    fun openRecap() {
        pendingRoute = ProgressDestination.Recap
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun Saver(): Saver<ProgressScreenState, Any> = listSaver(
            // Appended, never renumbered: an index that moves restores the wrong field into the
            // wrong overlay. It has been renumbered anyway on every commit that moved a field out,
            // both halves together each time — the recap's period into `RecapViewModel`; the recap
            // and the timelapse becoming routes; the photo selection and its share sheet leaving
            // with the Photos page; then, as each subject page became a route of its own, the
            // pending delete to `BloodPressureState`, the add-measurement sheet to
            // `MeasurementsState`, the energy check-in to `WeightState`, the meal gallery to
            // `NutritionState`, and finally the per-subject range map, once every page with a
            // chart owned its own range.
            //
            // What is left is the overview's: which subject is selected, which groups are open,
            // and the two sheets the overview's own empty-card hints can raise.
            // [pendingRoute] is not here and must not be: the back stack is what restores an open
            // route.
            save = {
                listOf(
                    it.selectedSubject?.name,
                    it.expandedGroups.map { group -> group.name },
                    it.activeBloodPressureSheet,
                    it.activeCycleSheet,
                )
            },
            restore = { saved ->
                ProgressScreenState(
                    selectedSubject = (saved[0] as String?)?.let(::subjectOrNull),
                    expandedGroups = (saved[1] as List<String>).mapNotNull(::groupOrNull).toSet(),
                    activeBloodPressureSheet = saved[2] as Boolean,
                    activeCycleSheet = saved[3] as Boolean,
                )
            },
        )
    }
}

/** Null rather than a throw for a name this build doesn't know — the degrade
 * `mascotCharacterOf()` and the Home layout parser already give a retired name. */
private fun subjectOrNull(name: String): Subject? = Subject.entries.firstOrNull { it.name == name }

private fun groupOrNull(name: String): SubjectGroup? = SubjectGroup.entries.firstOrNull { it.name == name }
