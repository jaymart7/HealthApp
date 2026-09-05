package ph.mart.healthapp.feature.profile.ui.routine

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ph.mart.healthapp.core.data.exercise.Routine
import ph.mart.healthapp.core.data.exercise.RoutineLift
import ph.mart.healthapp.core.data.exercise.dayLabel
import ph.mart.healthapp.core.data.exercise.totalSets
import ph.mart.healthapp.feature.profile.R

/**
 * Every saved routine. Unlike the food library there is no window to reach past — the strength
 * screen's chip row scrolls and shows them all — so this screen exists for the other half of that
 * screen's job: a routine can be *started* there and can only be renamed or deleted here.
 */
data class RoutinesUiState(
    val routines: List<Routine> = emptyList(),
) {
    /** Distinguishes "nothing saved" from "not loaded yet" for the empty state, exactly as
     * `FoodLibraryUiState` does — a mascot that flashes before the rows arrive reads as a bug. */
    val loaded: Boolean get() = routines.isNotEmpty()
}

/** "3 lifts · 9 sets · Mon · Wed · Fri" — what the row says a routine is. No load, because a
 * routine carries none: it opens at whatever was last lifted.
 *
 * An unscheduled routine says so in words rather than trailing a blank: the picker under the row
 * is the answer, and a row that simply stopped mid-sentence would not point at it. */
@Composable
fun Routine.summary(): String =
    stringResource(
        R.string.profile_routine_summary,
        pluralStringResource(R.plurals.profile_routine_lifts, lifts.size, lifts.size),
        pluralStringResource(R.plurals.profile_routine_sets, totalSets(), totalSets()),
        dayLabel().ifEmpty { stringResource(R.string.profile_routine_no_days) },
    )

/** "Bench press 3×8, Dip 2×10" — the row's third line. The lifts are already loaded, so naming
 * them costs nothing, and a routine you can't see the lifts of is one you delete blind. */
@Composable
fun List<RoutineLift>.contents(): String {
    val format = stringResource(R.string.profile_routine_lift)
    return joinToString { format.format(it.exerciseName, it.sets, it.reps) }
}

sealed interface RoutinesEvent {
    data class OnDelete(val id: Long) : RoutinesEvent
    /** The whole mask, not one day — the picker owns the toggle, so the write is idempotent and a
     * stale emission can't flip a day the user never touched. */
    data class OnSetDays(val id: Long, val days: Int) : RoutinesEvent
    data class OnRename(val id: Long, val name: String) : RoutinesEvent
}
