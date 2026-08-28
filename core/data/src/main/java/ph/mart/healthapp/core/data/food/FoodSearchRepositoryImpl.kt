package ph.mart.healthapp.core.data.food

import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

private const val ENDPOINT = "https://world.openfoodfacts.org/cgi/search.pl" +
    "?search_terms=%s&search_simple=1&action=process&json=1&page_size=20" +
    "&fields=product_name,nutriments"

/**
 * Free-text product search, same transport and product mapping as [BarcodeLookupRepositoryImpl].
 *
 * ponytail: `cgi/search.pl` is the legacy free-text endpoint and Open Food Facts rate-limits it
 * (~10 requests/minute). The caller's debounce keeps normal typing under that; move to the
 * SearchALicious host (search.openfoodfacts.org) if it ever bites.
 */
internal class FoodSearchRepositoryImpl : FoodSearchRepository {

    override suspend fun search(query: String): FoodSearchResult = withContext(Dispatchers.IO) {
        // Free text typed by the user on its way into a URL.
        val terms = URLEncoder.encode(query.trim(), "UTF-8")
        if (terms.isEmpty()) return@withContext FoodSearchResult.Empty

        when (val response = openFoodFactsGet(ENDPOINT.format(terms))) {
            is OffResponse.Ok -> parseOpenFoodFactsSearch(response.body)
            OffResponse.NotFound -> FoodSearchResult.Empty
            OffResponse.Failed -> FoodSearchResult.Failed
        }
    }
}

/** Split out of the network call so the response shape is unit-testable without a socket. */
internal fun parseOpenFoodFactsSearch(body: String): FoodSearchResult {
    val root = runCatching { offJson.parseToJsonElement(body).jsonObject }.getOrNull()
        ?: return FoodSearchResult.Failed
    val products = runCatching { root["products"]?.jsonArray }.getOrNull()
        ?: return FoodSearchResult.Failed

    val hits = products.mapNotNull { runCatching { it.jsonObject.toScannedProduct() }.getOrNull() }
    return if (hits.isEmpty()) FoodSearchResult.Empty else FoodSearchResult.Hits(hits)
}
