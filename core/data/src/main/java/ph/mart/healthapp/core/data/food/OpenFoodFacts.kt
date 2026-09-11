package ph.mart.healthapp.core.data.food

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Open Food Facts — the app's second food source, and the first one a scan asks. It is free,
 * keyless and internationally stocked, which is what [FoodDataCentral.kt][fdcGet] is not: FDC is a
 * US database, and for a `ph.mart` app a locally-packaged product largely returns nothing there.
 *
 * Two endpoints on two hosts, one mapper between them:
 *
 * - **`api/v2/product/{code}.json`** is a real barcode *lookup*. That is why nothing here echoes the
 *   scanned code back the way [parseFdcProduct] must — FDC's check exists because `foods/search`
 *   falls back to relevance and hands back the top of the branded database, and a v2 lookup cannot
 *   do that. A difference in endpoint kind, not a check dropped for brevity. OFF also normalises
 *   zero-padding server-side, so [barcodeKey]'s stripped code resolves without FDC's four-width
 *   query: `28400642255` and `0028400642255` answer with the same product.
 * - **`search.openfoodfacts.org/search`** (search-a-licious) backs the food search's online tier.
 *   The older `cgi/search.pl` is not used: measured back to back it answered 200, then 503, then
 *   503, while this one served five rapid queries at ~0.7s each.
 *
 * **`nutriments_estimated` is never read.** OFF publishes it alongside `nutriments` and computes it
 * from the ingredient list — Nutella's real payload carries calcium and iron *only* there. It is the
 * same thing `FEATURES.md` rules out under "AI-estimated micronutrients": a number the source
 * derived rather than read off a label. Only the declared `nutriments` object is mapped, so `0` goes
 * on meaning unknown-or-none exactly as [Nutrients] says it does.
 */

private const val PRODUCT_URL = "https://world.openfoodfacts.org/api/v2/product/"

private const val SEARCH_URL = "https://search.openfoodfacts.org/search"

/** Trimming the response matters more here than it does at FDC: an untrimmed OFF product is a
 * couple of hundred kilobytes of tags, scores and ingredient trees. */
private const val FIELDS = "product_name,product_name_en,brands,nutriments"

/** The panel pages at eight; twenty is enough to fill a few pages behind the local list without
 * making the request something you wait for. */
private const val SEARCH_PAGE_SIZE = 20

/** OFF throttles generic agents, and asks to be told who is calling. */
private const val USER_AGENT = "FitPulse/1.0 (Android; ph.mart.healthapp)"

private const val TIMEOUT_MS = 10_000

/** Every `<key>_100g` OFF publishes is in **grams**, verified against live products —
 * `sodium_100g: 0.0428`, `calcium_100g: 0.0253`, `iron_100g: 0.00094`. [Nutrients] stores sodium,
 * calcium and potassium as milligrams and iron and vitamin D as micrograms, so the conversion is
 * one of these two factors and never a unit read off the payload. */
private const val MG_PER_G = 1000

private const val UG_PER_G = 1_000_000

/** Many labels declare salt where the app wants sodium; the label conversion is sodium × 2.5. */
private const val SALT_TO_SODIUM = 2.5

private val offJson = Json { ignoreUnknownKeys = true }

internal fun offProductUrl(barcode: String): String = "$PRODUCT_URL$barcode.json?fields=$FIELDS"

internal fun offSearchUrl(query: String): String =
    "$SEARCH_URL?q=${URLEncoder.encode(query, "UTF-8")}&page_size=$SEARCH_PAGE_SIZE&fields=$FIELDS"

/**
 * Plain [HttpURLConnection] against a public JSON endpoint, the same no-HTTP-client-dependency rule
 * [fdcGet] follows. Null is every failure — no network, a 5xx, a throttle — because all of them mean
 * the same thing to both callers: ask the other source.
 *
 * ponytail: a cancelled request abandons the socket until the 10s timeout rather than interrupting
 * it, exactly as the FDC leg does; the result is discarded either way.
 */
internal fun offGet(url: String): String? {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = TIMEOUT_MS
        readTimeout = TIMEOUT_MS
        setRequestProperty("User-Agent", USER_AGENT)
    }
    return try {
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            null
        }
    } catch (_: IOException) {
        null
    } finally {
        connection.disconnect()
    }
}

/**
 * Split out of the network call so the response shape is unit-testable without a socket.
 *
 * A code OFF does not stock answers HTTP 200 with `{"code":"…","status":0,"status_verbose":"…"}` and
 * **no `product` key**, which is the whole miss check — the server's own `status` field is not read,
 * for the reason [parseFdcProduct] does not trust its filtering either: the payload is the evidence.
 */
internal fun parseOffProduct(body: String): BarcodeLookupResult {
    val root = runCatching { offJson.parseToJsonElement(body).jsonObject }.getOrNull()
        ?: return BarcodeLookupResult.Failed
    val product = root["product"] as? JsonObject ?: return BarcodeLookupResult.NotFound
    return product.toOffProduct()?.let(BarcodeLookupResult::Found) ?: BarcodeLookupResult.NotFound
}

/** Null when the body is not an answer at all; an empty list when OFF answered and stocks nothing.
 * The two are different to the panel, which says so. */
internal fun parseOffSearch(body: String): List<ScannedProduct>? {
    val root = runCatching { offJson.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
    val hits = root["hits"] as? JsonArray ?: return null
    return hits.mapNotNull { (it as? JsonObject)?.toOffProduct() }
}

/**
 * The one mapper both endpoints share. Per 100 g at `"g"` like [COMMON_FOODS] and every FDC row, so
 * a scan, a picked food and an online hit all seed the add-entry form the same way.
 *
 * Null when the row is unusable in a diary: no name, no `nutriments` object at all (search returns
 * plenty of those — a product someone photographed but never entered a label for), or a `nutriments`
 * carrying neither energy nor a single macro.
 */
internal fun JsonObject.toOffProduct(): ScannedProduct? {
    val name = brandedName(brand = firstBrand(), description = productName()) ?: return null
    val reported = this["nutriments"] as? JsonObject ?: return null

    // Most products carry kcal directly; the ones that only report kJ still have usable macros.
    val kcal = reported.value("energy-kcal_100g") ?: reported.value("energy_100g")?.div(KJ_PER_KCAL)
    val protein = reported.value("proteins_100g")
    val carbs = reported.value("carbohydrates_100g")
    val fat = reported.value("fat_100g")
    if (kcal == null && protein == null && carbs == null && fat == null) return null

    val sodiumG = reported.value("sodium_100g") ?: reported.value("salt_100g")?.div(SALT_TO_SODIUM)

    return ScannedProduct(
        name = name,
        portionAmount = PORTION_G,
        portionUnit = "g",
        calories = kcal?.roundToInt() ?: 0,
        proteinG = protein?.roundToInt() ?: 0,
        carbsG = carbs?.roundToInt() ?: 0,
        fatG = fat?.roundToInt() ?: 0,
        nutrients = Nutrients(
            fiberG = reported.value("fiber_100g")?.roundToInt() ?: 0,
            sugarG = reported.value("sugars_100g")?.roundToInt() ?: 0,
            sodiumMg = sodiumG?.times(MG_PER_G)?.roundToInt() ?: 0,
            vitaminDUg = reported.value("vitamin-d_100g")?.times(UG_PER_G)?.roundToInt() ?: 0,
            calciumMg = reported.value("calcium_100g")?.times(MG_PER_G)?.roundToInt() ?: 0,
            ironUg = reported.value("iron_100g")?.times(UG_PER_G)?.roundToInt() ?: 0,
            potassiumMg = reported.value("potassium_100g")?.times(MG_PER_G)?.roundToInt() ?: 0,
        ),
    )
}

private fun JsonObject.productName(): String? = string("product_name") ?: string("product_name_en")

/** `brands` is a comma-separated string on the product endpoint ("Nutella, Ferrero, Yum yum") and an
 * array on the search one (`["Kelloggs","Special K"]`). The first entry is the one on the package. */
private fun JsonObject.firstBrand(): String? = when (val brands = this["brands"]) {
    is JsonArray -> brands.firstOrNull()?.jsonPrimitive?.contentOrNull
    is JsonPrimitive -> brands.contentOrNull?.substringBefore(',')
    else -> null
}?.trim()?.takeIf { it.isNotEmpty() }

private fun JsonObject.string(key: String): String? =
    this[key]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

/** Values come back as numbers most of the time and as quoted strings some of the time; both are
 * accepted rather than dropping the field, as they are at FDC. */
private fun JsonObject.value(key: String): Double? =
    this[key]?.jsonPrimitive?.let { it.doubleOrNull ?: it.contentOrNull?.toDoubleOrNull() }
