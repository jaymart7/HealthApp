package ph.mart.healthapp.core.data.food

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Wide enough that the real match is on the first page even when FDC pads the query out with
 * fuzzy noise — see [parseFdcProduct]. */
private const val PAGE_SIZE = 25

/**
 * FDC has no barcode endpoint, so a scan is a `foods/search` restricted to branded foods; the
 * transport and the product mapping are shared with [FoodSearchRepositoryImpl] in
 * `FoodDataCentral.kt`.
 */
internal class BarcodeLookupRepositoryImpl : BarcodeLookupRepository {

    override suspend fun lookup(barcode: String): BarcodeLookupResult = withContext(Dispatchers.IO) {
        // The barcode arrives from an image decoder, so it is untrusted input on its way into a
        // URL — EAN/UPC are digits only, and anything else is not a product code.
        val code = barcode.filter(Char::isDigit)
        if (code.isEmpty()) return@withContext BarcodeLookupResult.NotFound

        // FDC stores `gtinUpc` at whatever width its source used — 028400642255 for one product,
        // 0099447210127 for the next — and matches the query token exactly, so a 12-digit scan
        // misses a 13-wide row entirely. Every padding is asked for at once: an unquoted query ORs
        // its terms, so this stays one request.
        val terms = setOf(code, code.padStart(12, '0'), code.padStart(13, '0'), code.padStart(14, '0'))
            .joinToString("%20")

        when (val response = fdcGet("foods/search", "query=$terms&dataType=Branded&pageSize=$PAGE_SIZE")) {
            is FdcResponse.Ok -> parseFdcProduct(response.body, code)
            FdcResponse.Failed -> BarcodeLookupResult.Failed
        }
    }
}

/**
 * Split out of the network call so the response shape is unit-testable without a socket.
 *
 * **The `gtinUpc` check is the whole point of this function, not a redundant one.** This is a
 * search endpoint, not a lookup: an unlisted code usually comes back with an empty `foods`, but one
 * that tokenizes to nothing (all zeros, say) makes FDC fall back to relevance and return the top of
 * the entire branded database — hundreds of thousands of real products, HTTP 200. Without comparing
 * the code back, a scan of an unlisted package would log whatever happened to rank first. Leading
 * zeros are stripped on both sides for the width mismatch above.
 *
 * A product with no name is unusable in the diary, so it counts as [BarcodeLookupResult.NotFound]
 * rather than a half-filled row.
 */
internal fun parseFdcProduct(body: String, barcode: String): BarcodeLookupResult {
    val root = runCatching { fdcJson.parseToJsonElement(body).jsonObject }.getOrNull()
        ?: return BarcodeLookupResult.Failed
    val foods = root["foods"] as? JsonArray ?: return BarcodeLookupResult.Failed

    val wanted = barcode.trimStart('0')
    val product = foods.asSequence()
        .mapNotNull { it as? JsonObject }
        .filter { it.gtinUpc()?.trimStart('0') == wanted }
        .mapNotNull { runCatching { it.toScannedProduct() }.getOrNull() }
        .firstOrNull()
        ?: return BarcodeLookupResult.NotFound

    return BarcodeLookupResult.Found(product)
}

private fun JsonObject.gtinUpc(): String? = this["gtinUpc"]?.jsonPrimitive?.contentOrNull
