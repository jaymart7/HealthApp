package ph.mart.healthapp.feature.training.ui

import androidx.compose.foundation.ScrollState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import ph.mart.healthapp.core.navigation.route.TrainingRoute
import ph.mart.healthapp.feature.training.ui.exercise.StrengthWorkoutScreen
import ph.mart.healthapp.feature.training.ui.training.TrainingScreen

/** Authoring a strength workout — reached from the log-exercise sheet, from tapping a logged one
 * to correct it, and from a training-plan card (Home's or this tab's) to start today's routine. It
 * carries the day like the diary's own flows do, so a workout logged while reviewing a past day
 * lands on that day; [editingId] of 0 is a new one, and a non-zero id is the row being superseded,
 * resolved by the screen rather than passed through the back stack.
 *
 * [routineId] seeds a new workout from a saved routine, and is resolved the same way for the same
 * reason: the back stack carries an id, never a row. It is meaningless beside a non-zero
 * [editingId] — a workout being corrected already has its sets. */
@Serializable
data class StrengthWorkoutRoute(
    val dateEpochDay: Long,
    val editingId: Long = 0,
    val routineId: Long = 0,
) : NavKey

/** [scrollState] is hoisted for the reason every tab's is: the FAB's scroll-collapse and
 * tap-active-tab-to-scroll-to-top both live in `AppScaffold`.
 *
 * [onLogExercise] opens a sheet `AppScaffold` hosts rather than a route — the tab's two logging
 * doors are the same two the FAB offers, and one host is what keeps predictive back closing the
 * sheet instead of the screen under it. */
fun EntryProviderScope<NavKey>.trainingEntries(
    scrollState: ScrollState,
    onLogExercise: (Long, Long) -> Unit,
    onOpenStrength: (Long, Long) -> Unit,
    onStartRoutine: (Long) -> Unit,
    onExitFlow: () -> Unit,
) {
    entry<TrainingRoute> {
        TrainingScreen(
            scrollState = scrollState,
            onLogExercise = onLogExercise,
            onOpenStrength = onOpenStrength,
            onStartRoutine = onStartRoutine,
        )
    }
    entry<StrengthWorkoutRoute> { key ->
        StrengthWorkoutScreen(
            dateEpochDay = key.dateEpochDay,
            editingId = key.editingId,
            routineId = key.routineId,
            onExit = onExitFlow,
        )
    }
}
