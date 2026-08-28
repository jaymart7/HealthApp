package ph.mart.healthapp.core.data.food

sealed interface FoodSearchResult {
    /** Hits are [ScannedProduct]s — a text search and a barcode scan resolve the same thing, so
     * they share the type and the per-100 g convention. */
    data class Hits(val products: List<ScannedProduct>) : FoodSearchResult

    /** The query was understood, nothing usable matched — the user types it in by hand. */
    data object Empty : FoodSearchResult

    /** Network or server problem; retrying the same query may well work. */
    data object Failed : FoodSearchResult
}

interface FoodSearchRepository {
    suspend fun search(query: String): FoodSearchResult
}
