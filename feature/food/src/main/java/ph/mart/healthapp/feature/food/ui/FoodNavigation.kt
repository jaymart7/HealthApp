package ph.mart.healthapp.feature.food.ui

import androidx.compose.foundation.ScrollState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import ph.mart.healthapp.core.navigation.route.FoodRoute
import ph.mart.healthapp.feature.food.ui.barcode.BarcodeScanScreen
import ph.mart.healthapp.feature.food.ui.diary.FoodScreen
import ph.mart.healthapp.feature.food.ui.photo.PhotoCaptureScreen
import ph.mart.healthapp.feature.food.ui.recipe.RecipeBuilderScreen
import ph.mart.healthapp.feature.food.ui.voice.VoiceLogScreen

/** The photo-logging flow — the real 6(+1)-state one (Phase 5). Carries the day like
 * [BarcodeScanRoute], and for the same reason: a plate photographed while reviewing a past day
 * belongs to that day. `0` is today, which is what the FAB and the launcher shortcut pass. */
@Serializable
data class FoodCaptureRoute(val dateEpochDay: Long) : NavKey

/** The food diary's barcode entry point — the scan/lookup/confirm flow. Carries the diary's
 * selected day, so a scan taken while reviewing a past day is logged to that day. */
@Serializable
data class BarcodeScanRoute(val dateEpochDay: Long) : NavKey

/** Authoring a recipe — reached from the add-entry sheet, and carrying nothing: a recipe belongs
 * to no day, so unlike [BarcodeScanRoute] it has no date to pass. */
@Serializable
data object RecipeBuilderRoute : NavKey

/** Logging a meal by saying or typing a sentence. Carries the day like [BarcodeScanRoute], and
 * for the same reason — a meal described while reviewing a past day belongs to that day; `0` is
 * today, the convention `StrengthWorkoutRoute` uses from Home and the FAB. */
@Serializable
data class VoiceLogRoute(val dateEpochDay: Long) : NavKey

/** [twoPane] comes from `AppScaffold`, the one place in the app that reads the window's width, so
 * this tab is told rather than asking — which is also why `:feature:food` needs no adaptive
 * dependency of its own. It reaches the diary and nothing else: the camera flows are full-bleed at
 * every width, and the recipe screen is a form.
 *
 * [onOpenStrength] and [onLogExercise] leave this module entirely — the strength screen and the
 * log-exercise sheet are `:feature:training`'s, and a feature never imports another's types, so
 * both stay callbacks resolved in `AppScaffold`. The shape `onOpenCoach` already has. */
fun EntryProviderScope<NavKey>.foodEntries(
    scrollState: ScrollState,
    twoPane: Boolean = false,
    onScanBarcode: (Long) -> Unit,
    onSpeakFood: (Long) -> Unit,
    onCapturePhoto: (Long) -> Unit,
    onNewRecipe: () -> Unit,
    onOpenStrength: (Long, Long) -> Unit,
    onLogExercise: (Long, Long) -> Unit,
    onExitFlow: () -> Unit,
) {
    entry<FoodRoute> {
        FoodScreen(
            scrollState = scrollState,
            twoPane = twoPane,
            onScanBarcode = onScanBarcode,
            onSpeakFood = onSpeakFood,
            onCapturePhoto = onCapturePhoto,
            onNewRecipe = onNewRecipe,
            onOpenStrength = onOpenStrength,
            onLogExercise = onLogExercise,
        )
    }
    entry<RecipeBuilderRoute> { RecipeBuilderScreen(onExit = onExitFlow) }
    entry<FoodCaptureRoute> { key -> PhotoCaptureScreen(dateEpochDay = key.dateEpochDay, onExit = onExitFlow) }
    entry<BarcodeScanRoute> { key -> BarcodeScanScreen(dateEpochDay = key.dateEpochDay, onExit = onExitFlow) }
    entry<VoiceLogRoute> { key -> VoiceLogScreen(dateEpochDay = key.dateEpochDay, onExit = onExitFlow) }
}
