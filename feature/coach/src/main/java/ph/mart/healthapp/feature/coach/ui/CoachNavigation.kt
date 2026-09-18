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
 *
 * [source] is the *place* that question came from — "Diary, Tue 9 Sep", "Weight" — and it is what
 * the composer's context chip names. A plain String for the reason every cross-feature reference
 * in this app is one: the diary already has the day's label on screen and a subject page already
 * has its own title, so both resolve it at the door rather than this module learning either type.
 * It travels beside [question] and is null with it: a field pre-filled from nowhere is the plain
 * door off Home, and there is nothing for a chip to say about it.
 */
@Serializable
data class CoachRoute(val question: String? = null, val source: String? = null) : NavKey

/**
 * [onOpenDiary] is the way *out* — shown only after a draft has put rows in today's diary. It is a
 * callback rather than a route this module names, the shape `onAskCoach` already has in the other
 * direction: the Food tab is `:core:navigation`'s and switching to it is `AppScaffold`'s job.
 *
 * [onExitFlow] is the back arrow. The coach draws its own `AppTopBar` — it needs the `actions`
 * slot for the overflow that holds "Clear chat", and `AppScaffold` cannot fill one from a `NavKey`
 * alone — so the arrow the scaffold used to draw is now this screen's, wired the way every other
 * self-barred route's is.
 *
 * [onStartRoutine] is the second way out and takes the same shape for the same reason:
 * `StrengthWorkoutRoute` is `:feature:training`'s and features never name each other's. It carries
 * the routine's id, which is all the back stack ever carries — the row is resolved by the screen.
 * Confirming a drafted routine is what calls it; every other draft writes and stays.
 */
fun EntryProviderScope<NavKey>.coachEntries(
    onOpenDiary: () -> Unit,
    onStartRoutine: (Long) -> Unit,
    onExitFlow: () -> Unit,
) {
    entry<CoachRoute> { route ->
        CoachScreen(
            question = route.question,
            source = route.source,
            onOpenDiary = onOpenDiary,
            onStartRoutine = onStartRoutine,
            onExitFlow = onExitFlow,
        )
    }
}
