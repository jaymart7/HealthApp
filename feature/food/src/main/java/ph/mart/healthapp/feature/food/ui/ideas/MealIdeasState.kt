package ph.mart.healthapp.feature.food.ui.ideas

import ph.mart.healthapp.core.data.food.MealIdea
import ph.mart.healthapp.core.data.food.MealIdeaRequest

/**
 * Where the one call this screen makes has got to. Not a screen-state class like
 * [BarcodeScanScreenState][ph.mart.healthapp.feature.food.ui.barcode.BarcodeScanScreenState]: this
 * *is* the call's result, so it belongs in the Orbit container that owns the call rather than in a
 * `remember` beside it.
 *
 * [Failed] carries no reason, for [MealIdeaResult][ph.mart.healthapp.core.data.food.MealIdeaResult]'s
 * reason — offline and a model with nothing to say land on the same fallback, the user's own
 * foods. Whether the radio is off only changes what the screen *says* above that list, which is
 * why [offline] rides the state rather than being asked again at draw time.
 */
sealed interface MealIdeasUiState {
    data object Idle : MealIdeasUiState
    data object Loading : MealIdeasUiState
    data class Ideas(val ideas: List<MealIdea>) : MealIdeasUiState
    data class Failed(val offline: Boolean) : MealIdeasUiState
}

sealed interface MealIdeasEvent {
    /** Sent once when the screen opens, and again by Retry — the request is rebuilt by the caller
     * each time, so a retry after logging something else asks against the budget as it is now. */
    data class OnRequest(val request: MealIdeaRequest) : MealIdeasEvent
}
