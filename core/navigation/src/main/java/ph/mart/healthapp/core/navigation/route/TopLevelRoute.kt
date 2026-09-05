package ph.mart.healthapp.core.navigation.route

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavKey

@Serializable
data object FoodRoute : NavKey

@Serializable
data object ProgressRoute : NavKey

@Serializable
data object ProfileRoute : NavKey

/** The 4 bottom-nav tabs, in fixed display order. Icon assignment lives in `:app`, and so does
 * the label — this module stays a leaf with no resources of its own, so a translated tab name
 * has nowhere to live here. */
enum class TopLevelDestination(val route: NavKey) {
    Home(HomeRoute),
    Food(FoodRoute),
    Progress(ProgressRoute),
    Profile(ProfileRoute),
}
