package ph.mart.healthapp.feature.food.ui.search

import ph.mart.healthapp.core.data.food.COMMON_FOODS
import ph.mart.healthapp.core.data.food.ScannedProduct

/**
 * [results] is the whole match, [page] the slice the panel is showing. No status type: the search
 * is a filter over a list already in memory, so there is no searching, no offline and no failure
 * left to model — the one answer the panel still has to draw is "nothing matched".
 */
data class FoodSearchUiState(
    val query: String = "",
    val results: List<ScannedProduct> = COMMON_FOODS,
    val page: Int = 0,
)

/**
 * ponytail: eight rows is what keeps the add-entry sheet's own form within a scroll of the panel.
 * A lazy list would be the alternative and [AppBottomSheet][ph.mart.healthapp.core.designsystem.component.AppBottomSheet]
 * hands its children unbounded height, so paging is also the shape that fits where this is drawn.
 */
const val FOOD_PAGE_SIZE = 8

/** Empty when [page] is past the end, which is why both movers clamp. */
val FoodSearchUiState.pageItems: List<ScannedProduct>
    get() = results.drop(page * FOOD_PAGE_SIZE).take(FOOD_PAGE_SIZE)

val FoodSearchUiState.pageCount: Int
    get() = (results.size + FOOD_PAGE_SIZE - 1) / FOOD_PAGE_SIZE

sealed interface FoodSearchEvent {
    data class OnQueryChange(val query: String) : FoodSearchEvent
    data object OnNextPage : FoodSearchEvent
    data object OnPrevPage : FoodSearchEvent
}
