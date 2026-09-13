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

fun EntryProviderScope<NavKey>.coachEntries() {
    entry<CoachRoute> { route -> CoachScreen(question = route.question) }
}
