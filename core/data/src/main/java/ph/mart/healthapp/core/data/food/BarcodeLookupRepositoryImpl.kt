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
 * A scan asks two databases in order, and remembers whichever answered.
 *
 * **Open Food Facts leads.** It is keyless, so it spends nothing from the app-wide 3600 req/hour FDC
 * budget; it is a real barcode lookup rather than a search, so its answer needs no `gtinUpc`-style
 * check; it returns a few kilobytes against FDC's 25 × ~21 KB search page; and it is stocked
 * internationally, which for a `ph.mart` app is the whole point — FDC is a US database and a locally
 * packaged product largely is not in it. It also means a clone with no `fdcApiKey` scans.
 *
 * **FDC is the fallback**, unchanged: `foods/search` restricted to branded foods, every zero-padding
 * asked for at once, and [parseFdcProduct]'s `gtinUpc` comparison — which is load-bearing there and
 * stays exactly as it was.
 *
 * [BarcodeLookupResult.Failed] only when *neither* source could answer. A genuine miss from OFF must
 * not be masked by FDC failing for want of a key, or the scan screen would offer "Try again"
 * forever instead of the manual-entry path a miss is supposed to lead to.
 *
 * A resolved product is remembered in `scanned_product` whichever source found it, which is what
 * makes the scanner work offline and stops a rescan re-spending either source's goodwill. The cache
 * is read before the network, always: a GTIN's nutrition panel doesn't change.
 */
internal class BarcodeLookupRepositoryImpl(
    private val dao: ScannedProductDao,
) : BarcodeLookupRepository {

    override suspend fun lookup(barcode: String): BarcodeLookupResult = withContext(Dispatchers.IO) {
        val code = barcodeKey(barcode) ?: return@withContext BarcodeLookupResult.NotFound

        dao.find(code)?.let { return@withContext BarcodeLookupResult.Found(it.toProduct()) }

        val off = offLookup(code)
        if (off is BarcodeLookupResult.Found) return@withContext off.remember(code)

        val fdc = fdcLookup(code)
        if (fdc is BarcodeLookupResult.Found) return@withContext fdc.remember(code)

        if (off is BarcodeLookupResult.Failed && fdc is BarcodeLookupResult.Failed) {
            BarcodeLookupResult.Failed
        } else {
            BarcodeLookupResult.NotFound
        }
    }

    /**
     * Only a hit is remembered. Both sources gain products over time, so a stored miss would blind
     * the app to a package that starts existing next month — and the not-found screen leads to
     * manual entry, so the rescan a cached miss would save is the rare one.
     */
    private suspend fun BarcodeLookupResult.Found.remember(code: String): BarcodeLookupResult {
        dao.upsert(product.toEntity(code))
        return this
    }

    /** OFF normalises zero-padding server-side, so the stripped [barcodeKey] goes straight in. */
    private fun offLookup(code: String): BarcodeLookupResult {
        val body = offGet(offProductUrl(code)) ?: return BarcodeLookupResult.Failed
        return parseOffProduct(body)
    }

    private fun fdcLookup(code: String): BarcodeLookupResult {
        // FDC stores `gtinUpc` at whatever width its source used — 028400642255 for one product,
        // 0099447210127 for the next — and matches the query token exactly, so a 12-digit scan
        // misses a 13-wide row entirely. Every padding is asked for at once: an unquoted query ORs
        // its terms, so this stays one request.
        val terms = setOf(code, code.padStart(12, '0'), code.padStart(13, '0'), code.padStart(14, '0'))
            .joinToString("%20")

        return when (val response = fdcGet("foods/search", "query=$terms&dataType=Branded&pageSize=$PAGE_SIZE")) {
            is FdcResponse.Ok -> parseFdcProduct(response.body, code)
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
