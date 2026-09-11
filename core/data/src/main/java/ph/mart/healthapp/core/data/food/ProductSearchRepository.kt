package ph.mart.healthapp.core.data.food

/**
 * The food search's online tier: Open Food Facts' text search, behind the two local tiers
 * [searchFoods] already folds. It exists because [COMMON_FOODS] is ~120 staples and the scanner is
 * the only other door to a packaged product — you had to be holding the package to find it.
 *
 * Deliberately *not* a method on [BarcodeLookupRepository]: that one is a lookup with a cache and
 * this is a query with neither, and the search panel has no business holding a scanner's interface.
 */
interface ProductSearchRepository {
    suspend fun search(query: String): ProductSearchResult
}

sealed interface ProductSearchResult {
    /** May be empty — OFF answered and stocks nothing matching, which is not a failure. */
    data class Ok(val products: List<ScannedProduct>) : ProductSearchResult

    /** Network or server problem. The panel says so rather than reporting "no matches", which
     * would be a different and wrong answer. */
    data object Failed : ProductSearchResult
}
