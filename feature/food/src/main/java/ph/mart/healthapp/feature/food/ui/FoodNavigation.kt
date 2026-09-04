package ph.mart.healthapp.feature.food.ui

import androidx.compose.foundation.ScrollState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import ph.mart.healthapp.core.navigation.route.FoodRoute
import ph.mart.healthapp.feature.food.ui.barcode.BarcodeScanScreen
import ph.mart.healthapp.feature.food.ui.diary.FoodScreen
import ph.mart.healthapp.feature.food.ui.exercise.StrengthWorkoutScreen
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

/** Authoring a strength workout — reached from the log-exercise sheet, and from tapping a logged
 * one to correct it — and, from Home's training-plan card, to start today's routine. It carries the
 * day like [BarcodeScanRoute] does, so a workout logged while reviewing a past day lands on that
 * day; [editingId] of 0 is a new one, and a non-zero id is the row being superseded, resolved by
 * the screen rather than passed through the back stack.
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

/** Logging a meal by saying or typing a sentence. Carries the day like [BarcodeScanRoute], and
 * for the same reason — a meal described while reviewing a past day belongs to that day; `0` is
 * today, the convention [StrengthWorkoutRoute] uses from Home and the FAB. */
@Serializable
data class VoiceLogRoute(val dateEpochDay: Long) : NavKey

fun EntryProviderScope<NavKey>.foodEntries(
    scrollState: ScrollState,
    onScanBarcode: (Long) -> Unit,
    onSpeakFood: (Long) -> Unit,
    onCapturePhoto: (Long) -> Unit,
    onNewRecipe: () -> Unit,
    onOpenStrength: (Long, Long) -> Unit,
    onExitFlow: () -> Unit,
) {
    entry<FoodRoute> {
        FoodScreen(
            scrollState = scrollState,
            onScanBarcode = onScanBarcode,
            onSpeakFood = onSpeakFood,
            onCapturePhoto = onCapturePhoto,
            onNewRecipe = onNewRecipe,
            onOpenStrength = onOpenStrength,
        )
    }
    entry<RecipeBuilderRoute> { RecipeBuilderScreen(onExit = onExitFlow) }
    entry<StrengthWorkoutRoute> { key ->
        StrengthWorkoutScreen(
            dateEpochDay = key.dateEpochDay,
            editingId = key.editingId,
            routineId = key.routineId,
            onExit = onExitFlow,
        )
    }
    entry<FoodCaptureRoute> { key -> PhotoCaptureScreen(dateEpochDay = key.dateEpochDay, onExit = onExitFlow) }
    entry<BarcodeScanRoute> { key -> BarcodeScanScreen(dateEpochDay = key.dateEpochDay, onExit = onExitFlow) }
    entry<VoiceLogRoute> { key -> VoiceLogScreen(dateEpochDay = key.dateEpochDay, onExit = onExitFlow) }
}
