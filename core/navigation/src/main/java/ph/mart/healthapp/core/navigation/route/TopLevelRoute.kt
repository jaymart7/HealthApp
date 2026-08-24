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

/** The 4 bottom-nav tabs, in fixed display order. Icon assignment lives in `:app` (this module
 * stays a leaf — it never depends on `:core:designsystem`). */
enum class TopLevelDestination(val route: NavKey, val label: String) {
    Home(HomeRoute, "Home"),
    Food(FoodRoute, "Food"),
    Progress(ProgressRoute, "Progress"),
    Profile(ProfileRoute, "Profile"),
}
