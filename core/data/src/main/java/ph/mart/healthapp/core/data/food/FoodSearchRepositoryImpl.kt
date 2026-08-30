package ph.mart.healthapp.core.data.food

import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject

/**
 * Ten hits, not the panel's five: nameless rows are dropped after the fact, so there has to be
 * slack. FDC ships the whole food record — every nutrient plus the full ingredients text, roughly
 * 21 KB each — and its `nutrients=` filter is ignored on this endpoint, so `pageSize` is the only
 * thing that keeps a debounced keystroke burst off a quarter-megabyte response.
 */
private const val PAGE_SIZE = 10

/**
 * Free-text food search, same transport and product mapping as [BarcodeLookupRepositoryImpl].
 * No `dataType` filter: branded packages and whole foods ("Broccoli, raw") both belong in the
 * results, ranked by FDC's own relevance.
 */
internal class FoodSearchRepositoryImpl : FoodSearchRepository {

    override suspend fun search(query: String): FoodSearchResult = withContext(Dispatchers.IO) {
        // Free text typed by the user on its way into a URL.
        val terms = URLEncoder.encode(query.trim(), "UTF-8")
        if (terms.isEmpty()) return@withContext FoodSearchResult.Empty

        when (val response = fdcGet("foods/search", "query=$terms&pageSize=$PAGE_SIZE")) {
            is FdcResponse.Ok -> parseFdcSearch(response.body)
            FdcResponse.Failed -> FoodSearchResult.Failed
        }
    }
}

/** Split out of the network call so the response shape is unit-testable without a socket. */
internal fun parseFdcSearch(body: String): FoodSearchResult {
    val root = runCatching { fdcJson.parseToJsonElement(body).jsonObject }.getOrNull()
        ?: return FoodSearchResult.Failed
    val foods = root["foods"] as? JsonArray ?: return FoodSearchResult.Failed

    val hits = foods.mapNotNull { runCatching { it.jsonObject.toScannedProduct() }.getOrNull() }
    return if (hits.isEmpty()) FoodSearchResult.Empty else FoodSearchResult.Hits(hits)
}
