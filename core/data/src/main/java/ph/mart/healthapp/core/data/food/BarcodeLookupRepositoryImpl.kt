package ph.mart.healthapp.core.data.food

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ph.mart.healthapp.core.data.food.local.ScannedProductDao
import ph.mart.healthapp.core.data.food.local.ScannedProductEntity

/** Wide enough that the real match is on the first page even when FDC pads the query out with
 * fuzzy noise — see [parseFdcProduct]. */
private const val PAGE_SIZE = 25

/**
 * FDC has no barcode endpoint, so a scan is a `foods/search` restricted to branded foods. It is
 * the only thing left in the app that talks to FDC — free-text search reads the shipped
 * [COMMON_FOODS] list — so the transport and the product mapping in `FoodDataCentral.kt` exist
 * for this one caller.
 *
 * A resolved product is remembered in `scanned_product`, which is what makes the scanner work
 * offline and stops a rescan re-spending the app-wide key budget. The cache is read before the
 * network, always: FDC's answer for a given GTIN doesn't change, and the quota is the thing worth
 * saving.
 */
internal class BarcodeLookupRepositoryImpl(
    private val dao: ScannedProductDao,
) : BarcodeLookupRepository {

    override suspend fun lookup(barcode: String): BarcodeLookupResult = withContext(Dispatchers.IO) {
        val code = barcodeKey(barcode) ?: return@withContext BarcodeLookupResult.NotFound

        dao.find(code)?.let { return@withContext BarcodeLookupResult.Found(it.toProduct()) }

        // FDC stores `gtinUpc` at whatever width its source used — 028400642255 for one product,
        // 0099447210127 for the next — and matches the query token exactly, so a 12-digit scan
        // misses a 13-wide row entirely. Every padding is asked for at once: an unquoted query ORs
        // its terms, so this stays one request.
        val terms = setOf(code, code.padStart(12, '0'), code.padStart(13, '0'), code.padStart(14, '0'))
            .joinToString("%20")

        when (val response = fdcGet("foods/search", "query=$terms&dataType=Branded&pageSize=$PAGE_SIZE")) {
            is FdcResponse.Ok -> parseFdcProduct(response.body, code).also { result ->
                // Only a hit is remembered. FDC gains products over time, so a stored miss would
                // blind the app to a package that starts existing next month — and the not-found
                // screen leads to manual entry, so the rescan a cached miss would save is rare.
                if (result is BarcodeLookupResult.Found) dao.upsert(result.product.toEntity(code))
            }

            FdcResponse.Failed -> BarcodeLookupResult.Failed
        }
    }
}

/**
 * The cache key and the FDC query term are the same string: digits only — the code arrives from an
 * image decoder, so it is untrusted input on its way into a URL, and EAN/UPC are digits — with
 * leading zeros stripped so a 12-wide and a 13-wide read of one package share a row rather than
 * caching the product twice. That is the identity [parseFdcProduct] already compares on.
 *
 * Null when nothing survives: an all-zeros read is not a product code, and asking FDC for one makes
 * it fall back to relevance and return the top of the entire branded database.
 */
internal fun barcodeKey(raw: String): String? =
    raw.filter(Char::isDigit).trimStart('0').takeIf { it.isNotEmpty() }

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

private fun ScannedProductEntity.toProduct() = ScannedProduct(
    name = name,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    nutrients = nutrients,
)

private fun ScannedProduct.toEntity(barcode: String) = ScannedProductEntity(
    barcode = barcode,
    name = name,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    nutrients = nutrients,
)
