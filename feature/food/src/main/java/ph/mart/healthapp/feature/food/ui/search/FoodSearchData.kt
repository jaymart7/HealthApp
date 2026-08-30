package ph.mart.healthapp.feature.food.ui.search

import ph.mart.healthapp.core.data.food.FoodSearchResult
import ph.mart.healthapp.core.data.food.ScannedProduct

data class FoodSearchUiState(
    val query: String = "",
    val status: SearchStatus = SearchStatus.Idle,
)

sealed interface SearchStatus {
    /** Nothing typed yet, or too little to search on. */
    data object Idle : SearchStatus
    data object Searching : SearchStatus
    data class Results(val products: List<ScannedProduct>) : SearchStatus
    data object Empty : SearchStatus

    /** Offline or a server problem — the same copy covers both, since the user's move is the same:
     * type it in by hand. */
    data object Failed : SearchStatus
}

sealed interface FoodSearchEvent {
    data class OnQueryChange(val query: String) : FoodSearchEvent
}

fun FoodSearchResult.toStatus(): SearchStatus = when (this) {
    is FoodSearchResult.Hits -> SearchStatus.Results(products)
    FoodSearchResult.Empty -> SearchStatus.Empty
    FoodSearchResult.Failed -> SearchStatus.Failed
}
