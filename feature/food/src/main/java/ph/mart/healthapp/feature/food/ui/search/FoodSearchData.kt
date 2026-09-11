package ph.mart.healthapp.feature.food.ui.search

import ph.mart.healthapp.core.data.food.COMMON_FOODS
import ph.mart.healthapp.core.data.food.ScannedProduct

/**
 * [results] is the whole match — the user's own foods, then the built-in list, then [online]; see
 * [searchFoods][ph.mart.healthapp.core.data.food.searchFoods] — and [shown] how many of them the
 * panel has rendered so far.
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
    val shown: Int = FOOD_PAGE_SIZE,
)

/**
 * [Idle] covers three things the panel draws identically — nothing asked yet, an answer landed, and
 * offline. Offline in particular is not a failure: the local list answering with no network is the
 * feature, so saying so would be an error message for working as designed.
 */
enum class OnlineSearch { Idle, Searching, Failed }

/**
 * How many more rows reaching the bottom of the panel's results box appends.
 *
 * ponytail: a counter over a list already in memory, not Paging3 and not a lazy list —
 * [AppBottomSheet][ph.mart.healthapp.core.designsystem.component.AppBottomSheet] hands its children
 * unbounded height, and the whole result set is `COMMON_FOODS` plus one Open Food Facts answer. The
 * panel's own bounded box is what keeps the add-entry sheet's form within a scroll of the panel.
 */
const val FOOD_PAGE_SIZE = 8

val FoodSearchUiState.visibleItems: List<ScannedProduct>
    get() = results.take(shown)

val FoodSearchUiState.hasMore: Boolean
    get() = shown < results.size

/**
 * The clamp is load-bearing: at the end of the list this returns an equal state, which the state
 * flow drops, so the panel re-asking every time the box is already scrolled to the bottom cannot
 * loop. It never falls below one page, or a three-hit local answer would leave the window at three
 * when the online tier lands twenty more behind it.
 */
fun FoodSearchUiState.withMore(): FoodSearchUiState =
    copy(shown = (shown + FOOD_PAGE_SIZE).coerceAtMost(maxOf(results.size, FOOD_PAGE_SIZE)))

sealed interface FoodSearchEvent {
    data class OnQueryChange(val query: String) : FoodSearchEvent
    data object OnLoadMore : FoodSearchEvent
}
