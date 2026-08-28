package ph.mart.healthapp.core.data.food

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val ENDPOINT =
    "https://world.openfoodfacts.org/api/v2/product/%s.json?fields=product_name,nutriments"

/** One GET against the product endpoint; the transport and the product mapping are shared with
 * [FoodSearchRepositoryImpl] in `OpenFoodFacts.kt`. */
internal class BarcodeLookupRepositoryImpl : BarcodeLookupRepository {

    override suspend fun lookup(barcode: String): BarcodeLookupResult = withContext(Dispatchers.IO) {
        // The barcode arrives from an image decoder, so it is untrusted input on its way into a
        // URL — EAN/UPC are digits only, and anything else is not a product code.
        val code = barcode.filter(Char::isDigit)
        if (code.isEmpty()) return@withContext BarcodeLookupResult.NotFound

        when (val response = openFoodFactsGet(ENDPOINT.format(code))) {
            is OffResponse.Ok -> parseOpenFoodFactsProduct(response.body)
            OffResponse.NotFound -> BarcodeLookupResult.NotFound
            OffResponse.Failed -> BarcodeLookupResult.Failed
        }
    }
}

/**
 * Split out of the network call so the response shape is unit-testable without a socket.
 *
 * A product with no name is unusable in the diary, so it counts as [BarcodeLookupResult.NotFound]
 * rather than a half-filled row.
 */
internal fun parseOpenFoodFactsProduct(body: String): BarcodeLookupResult {
    val root = runCatching { offJson.parseToJsonElement(body).jsonObject }.getOrNull()
        ?: return BarcodeLookupResult.Failed
    if (root["status"]?.jsonPrimitive?.intOrNull != 1) return BarcodeLookupResult.NotFound

    val product = root["product"]?.jsonObject?.toScannedProduct()
        ?: return BarcodeLookupResult.NotFound
    return BarcodeLookupResult.Found(product)
}
