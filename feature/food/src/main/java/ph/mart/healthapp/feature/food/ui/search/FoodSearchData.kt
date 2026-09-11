package ph.mart.healthapp.feature.food.ui.search

import ph.mart.healthapp.core.data.food.COMMON_FOODS
import ph.mart.healthapp.core.data.food.ScannedProduct

/**
 * [results] is the whole match — the user's own foods, then the built-in list, then [online]; see
 * [searchFoods][ph.mart.healthapp.core.data.food.searchFoods] — and [page] the slice the panel is
 * showing.
 *
 * [myFoods] and [online] are held rather than only folded because the fold has three triggers now:
 * a keystroke, Room emitting after a food is saved or renamed somewhere else in the app, and the
 * debounced Open Food Facts answer landing.
 *
 * [onlineStatus] exists for one reason. The panel's answer to an empty result has always been
 * "No matches — enter it by hand instead.", which is a lie while a request is still in flight and a
 * different lie when that request failed. The two local tiers needed no status: they are a filter
 * over lists already in memory.
 */
data class FoodSearchUiState(
    val query: String = "",
    val myFoods: List<ScannedProduct> = emptyList(),
    val online: List<ScannedProduct> = emptyList(),
    val onlineStatus: OnlineSearch = OnlineSearch.Idle,
    val results: List<ScannedProduct> = COMMON_FOODS,
    val page: Int = 0,
)

/**
 * [Idle] covers three things the panel draws identically — nothing asked yet, an answer landed, and
 * offline. Offline in particular is not a failure: the local list answering with no network is the
 * feature, so saying so would be an error message for working as designed.
 */
enum class OnlineSearch { Idle, Searching, Failed }

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
