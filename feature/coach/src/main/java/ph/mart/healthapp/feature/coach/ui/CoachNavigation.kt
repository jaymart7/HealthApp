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
@Serializable
data object CoachRoute : NavKey

fun EntryProviderScope<NavKey>.coachEntries() {
    entry<CoachRoute> { CoachScreen() }
}
