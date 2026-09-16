package ph.mart.healthapp.feature.coach.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The coach, one level above the Home tab. A route rather than a sheet, for the reason
 * `FoodLibraryRoute` is one: a conversation grows past a sheet's height, and it needs the whole
 * screen once the keyboard is up. Being off the tab list is also what gives it a back toolbar and
 * hides the bottom bar and the FAB — `AppScaffold`'s existing rule, no new case.
 */
/**
 * [question] is what the door the user came through was asking — the diary's day, a Progress
 * subject — and it **fills the field rather than sending**. That is the mic's rule
 * (`RecognizerIntent` fills and stops): a send is a model call and a persisted pair of rows, and a
 * question arrived at by tapping an icon is a starting point the user will often want to narrow
 * before spending one. Null is the plain door off Home, which is every other way in.
 */
@Serializable
data class CoachRoute(val question: String? = null) : NavKey

/**
 * [onOpenDiary] is the way *out* — shown only after a draft has put rows in today's diary. It is a
 * callback rather than a route this module names, the shape `onAskCoach` already has in the other
 * direction: the Food tab is `:core:navigation`'s and switching to it is `AppScaffold`'s job.
 *
 * [onStartRoutine] is the second way out and takes the same shape for the same reason:
 * `StrengthWorkoutRoute` is `:feature:training`'s and features never name each other's. It carries
 * the routine's id, which is all the back stack ever carries — the row is resolved by the screen.
 * Confirming a drafted routine is what calls it; every other draft writes and stays.
 */
fun EntryProviderScope<NavKey>.coachEntries(
    onOpenDiary: () -> Unit,
    onStartRoutine: (Long) -> Unit,
) {
    entry<CoachRoute> { route ->
        CoachScreen(
            question = route.question,
            onOpenDiary = onOpenDiary,
            onStartRoutine = onStartRoutine,
        )
    }
}
