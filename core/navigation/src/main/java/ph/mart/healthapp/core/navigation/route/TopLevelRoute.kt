package ph.mart.healthapp.core.navigation.route

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavKey

@Serializable
data object FoodRoute : NavKey

@Serializable
data object TrainingRoute : NavKey

@Serializable
data object ProgressRoute : NavKey

@Serializable
data object ProfileRoute : NavKey

/** The 5 bottom-nav tabs, in fixed display order — the two logging tabs together, then the
 * history, then the person. Icon assignment lives in `:app`, and so does
 * the label — this module stays a leaf with no resources of its own, so a translated tab name
 * has nowhere to live here. */
enum class TopLevelDestination(val route: NavKey) {
    Home(HomeRoute),
    Food(FoodRoute),
    Training(TrainingRoute),
    Progress(ProgressRoute),
    Profile(ProfileRoute),
}
