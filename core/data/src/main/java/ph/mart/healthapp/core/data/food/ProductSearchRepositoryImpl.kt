package ph.mart.healthapp.core.data.food

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * No cache and no table. A text search is not a barcode — its answer is a ranking that changes as
 * OFF gains products, and the query space is every phrase anyone might type, so there is nothing
 * worth keying a row on. `scanned_product` stays what it is: resolved packages.
 */
internal class ProductSearchRepositoryImpl : ProductSearchRepository {

    override suspend fun search(query: String): ProductSearchResult = withContext(Dispatchers.IO) {
        val body = offGet(offSearchUrl(query)) ?: return@withContext ProductSearchResult.Failed
        parseOffSearch(body)?.let(ProductSearchResult::Ok) ?: ProductSearchResult.Failed
    }
}
