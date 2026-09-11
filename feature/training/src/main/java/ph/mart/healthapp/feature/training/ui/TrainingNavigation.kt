package ph.mart.healthapp.feature.training.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Authoring a strength workout — reached from the log-exercise sheet, from tapping a logged one
 * to correct it, and from Home's training-plan card to start today's routine. It carries the day
 * like the diary's own flows do, so a workout logged while reviewing a past day lands on that day;
 * [editingId] of 0 is a new one, and a non-zero id is the row being superseded, resolved by the
 * screen rather than passed through the back stack.
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

/**
 * One route, no tab. This module owns the *doing* of training — the log-exercise sheet and the
 * strength screen — but draws no surface of its own: today's plan is Home's card, today's sessions
 * are the diary's exercise block, and the history is the Progress tab's. [LogExerciseSheet] is not
 * here because `AppScaffold` hosts it directly, the way it hosts every other FAB sheet.
 */
fun EntryProviderScope<NavKey>.trainingEntries(onExitFlow: () -> Unit) {
    entry<StrengthWorkoutRoute> { key ->
        StrengthWorkoutScreen(
            dateEpochDay = key.dateEpochDay,
            editingId = key.editingId,
            routineId = key.routineId,
            onExit = onExitFlow,
        )
    }
}
