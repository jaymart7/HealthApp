package ph.mart.healthapp.feature.food.ui

import androidx.compose.foundation.ScrollState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import ph.mart.healthapp.core.data.food.MealIdea
import ph.mart.healthapp.core.data.food.MealIdeaRequest
import ph.mart.healthapp.core.navigation.route.FoodRoute
import ph.mart.healthapp.feature.food.ui.barcode.BarcodeScanScreen
import ph.mart.healthapp.feature.food.ui.diary.FoodScreen
import ph.mart.healthapp.feature.food.ui.history.FoodHistoryScreen
import ph.mart.healthapp.feature.food.ui.ideas.MealIdeasScreen
import ph.mart.healthapp.feature.food.ui.label.LabelScanScreen
import ph.mart.healthapp.feature.food.ui.library.LibraryItemScreen
import ph.mart.healthapp.feature.food.ui.photo.PhotoCaptureScreen
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

/**
 * Reading a nutrition panel with the camera. Carries the day like [BarcodeScanRoute] and for its
 * reason — a packet read while reviewing a past day belongs to that day.
 *
 * A route of its own rather than a state inside [BarcodeScanRoute], although that is the only place
 * it is reached from: the two flows bind different CameraX use cases, and two
 * `LifecycleCameraController`s alive in one composition is a binding race rather than a shape.
 * Every camera flow in this app is a route for the same reason.
 */
@Serializable
data class LabelScanRoute(val dateEpochDay: Long) : NavKey

/**
 * Searching everything ever logged. Carries the day like [BarcodeScanRoute] and for the same
 * reason — a row re-logged from here belongs to the day the diary was showing — plus whatever was
 * already typed into that day's filter, so walking up from a day that had no match doesn't cost
 * the user their word twice. [query] is empty when the filter was closed.
 */
@Serializable
data class FoodHistoryRoute(val dateEpochDay: Long, val query: String) : NavKey

/**
 * What fits in the rest of the day. Carries the whole [MealIdeaRequest] rather than a day and a
 * meal: the diary has already combined the targets, the day's totals and the earned calories to
 * answer the question, and rebuilding that here would be a second copy of its whole observer for a
 * screen that writes nothing.
 *
 * A route rather than the overlay it started as, for the reason `RecapRoute` is one: no bottom bar,
 * no FAB, and back that leaves rather than closes — all three are what a route already is.
 */
@Serializable
data class MealIdeasRoute(val request: MealIdeaRequest) : NavKey

/**
 * Adding to the food library, or editing one thing in it — reached from Profile → Food library
 * (which cannot import this module, so `:app` pushes it) and from the add-entry sheet's recipes.
 * Both null is a new item, which opens on the AI box. [savedMealId] names a recipe or a saved meal,
 * which share an id space; [foodName] names a food, whose name is its key. No date: nothing in the
 * library belongs to a day, so unlike [BarcodeScanRoute] there is none to pass.
 */
@Serializable
data class LibraryItemRoute(val savedMealId: Long? = null, val foodName: String? = null) : NavKey

/** Logging a meal by saying or typing a sentence. Carries the day like [BarcodeScanRoute], and
 * for the same reason — a meal described while reviewing a past day belongs to that day; `0` is
 * today, the convention `StrengthWorkoutRoute` uses from Home and the FAB. */
@Serializable
data class VoiceLogRoute(val dateEpochDay: Long) : NavKey

/** [twoPane] comes from `AppScaffold`, the one place in the app that reads the window's width, so
 * this tab is told rather than asking — which is also why `:feature:food` needs no adaptive
 * dependency of its own. It reaches the diary and nothing else: the camera flows are full-bleed at
 * every width, and the library's add-and-edit screen is a form.
 *
 * [onExitFlow] is what a flow calls when it is finished: the four camera-side flows, the library's
 * add-and-edit screen, and the history search, which took its bar over when the review screen
 * behind a result brought one of its own.
 *
 * [onOpenStrength] and [onLogExercise] leave this module entirely — the strength screen and the
 * log-exercise sheet are `:feature:training`'s, and a feature never imports another's types, so
 * both stay callbacks resolved in `AppScaffold`. The shape `onOpenCoach` already has. */
fun EntryProviderScope<NavKey>.foodEntries(
    scrollState: ScrollState,
    twoPane: Boolean = false,
    onScanBarcode: (Long) -> Unit,
    /** Reached only from the barcode flow's two not-found states — see [BarcodeScanScreen]. It is a
     * route above this tab like the rest, so `:app` resolves it. */
    onScanLabel: (Long) -> Unit,
    onSpeakFood: (Long) -> Unit,
    onCapturePhoto: (Long) -> Unit,
    onOpenHistory: (Long, String) -> Unit,
    onNewRecipe: () -> Unit,
    /** The gap the diary worked out, carried to [MealIdeasRoute] — `:app` pushes it. */
    onGetIdeas: (MealIdeaRequest) -> Unit,
    /** An idea picked on that route. It seeds the diary's add-entry sheet rather than logging, so
     * it travels back down through [pendingIdea] once `:app` has popped the route. */
    onSelectIdea: (MealIdea) -> Unit,
    /** The idea on its way back to the diary, held by `:app` for the one recomposition between the
     * pop and the sheet reopening. Null the rest of the time. */
    pendingIdea: MealIdea? = null,
    onIdeaConsumed: () -> Unit = {},
    onOpenStrength: (Long, Long) -> Unit,
    onLogExercise: (Long, Long) -> Unit,
    /** The day's own question, carried to the coach — which lives above this tab, so like
     * `onOpenStrength` it stays a callback `AppScaffold` resolves. */
    onAskCoach: (question: String, source: String) -> Unit,
    onExitFlow: () -> Unit,
) {
    entry<FoodRoute> {
        FoodScreen(
            scrollState = scrollState,
            twoPane = twoPane,
            onScanBarcode = onScanBarcode,
            onSpeakFood = onSpeakFood,
            onCapturePhoto = onCapturePhoto,
            onOpenHistory = onOpenHistory,
            onNewRecipe = onNewRecipe,
            onGetIdeas = onGetIdeas,
            pendingIdea = pendingIdea,
            onIdeaConsumed = onIdeaConsumed,
            onOpenStrength = onOpenStrength,
            onLogExercise = onLogExercise,
            onAskCoach = onAskCoach,
        )
    }
    entry<LibraryItemRoute> { key ->
        LibraryItemScreen(savedMealId = key.savedMealId, foodName = key.foodName, onExit = onExitFlow)
    }
    entry<MealIdeasRoute> { key -> MealIdeasScreen(request = key.request, onSelect = onSelectIdea) }
    entry<FoodCaptureRoute> { key -> PhotoCaptureScreen(dateEpochDay = key.dateEpochDay, onExit = onExitFlow) }
    entry<BarcodeScanRoute> { key ->
        BarcodeScanScreen(
            dateEpochDay = key.dateEpochDay,
            onExit = onExitFlow,
            onScanLabel = { onScanLabel(key.dateEpochDay) },
        )
    }
    entry<LabelScanRoute> { key -> LabelScanScreen(dateEpochDay = key.dateEpochDay, onExit = onExitFlow) }
    entry<VoiceLogRoute> { key -> VoiceLogScreen(dateEpochDay = key.dateEpochDay, onExit = onExitFlow) }
    entry<FoodHistoryRoute> { key ->
        FoodHistoryScreen(dateEpochDay = key.dateEpochDay, query = key.query, onExit = onExitFlow)
    }
}
