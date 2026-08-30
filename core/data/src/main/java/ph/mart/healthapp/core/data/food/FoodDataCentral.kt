package ph.mart.healthapp.core.data.food

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ph.mart.healthapp.core.data.BuildConfig

/**
 * The bits [BarcodeLookupRepositoryImpl] and [FoodSearchRepositoryImpl] share: one GET against
 * USDA FoodData Central and one food-object → [ScannedProduct] mapping. FDC has no barcode
 * endpoint, so both a scan and a text search go through `foods/search` and come back in the same
 * shape — they parse it the same way.
 */

private const val BASE_URL = "https://api.nal.usda.gov/fdc/v1/"

private const val USER_AGENT = "FitPulse/1.0 (Android)"

private const val TIMEOUT_MS = 10_000

private const val KJ_PER_KCAL = 4.184

/** FDC reports every search nutrient per 100 g, whatever the package's own serving size says. */
internal const val PORTION_G = 100.0

/** FDC identifies nutrients by number, not by name. */
private const val NUTRIENT_KCAL = 1008
private const val NUTRIENT_KJ = 1062
private const val NUTRIENT_PROTEIN = 1003
private const val NUTRIENT_FAT = 1004
private const val NUTRIENT_CARBS = 1005
private const val NUTRIENT_FIBER = 1079

/** Branded rows report sugar as 2000 ("Sugars, total including NLEA"); Foundation and legacy rows
 * use 1063. Neither id is present on every food, so both are tried before giving up. */
private const val NUTRIENT_SUGAR = 2000
private const val NUTRIENT_SUGAR_NLEA = 1063

/** FDC already reports sodium in milligrams — the one nutrient here that isn't grams. */
private const val NUTRIENT_SODIUM = 1093

internal sealed interface FdcResponse {
    data class Ok(val body: String) : FdcResponse

    /** Network problem, a 403 from a bad key, or a 429 over the hourly budget — all of them mean
     * "try again", so they are one case. A *miss* is not here: FDC answers an unmatched query with
     * HTTP 200 and an empty `foods` array, which the parsers handle. */
    data object Failed : FdcResponse
}

/**
 * Plain [HttpURLConnection] against a public JSON endpoint — no HTTP client dependency for a
 * single GET, matching how [FoodRecognitionRepositoryImpl] leans on the Firebase SDK rather than
 * introducing one.
 *
 * ponytail: the key is one signed key shared by every install, so the 3600 requests/hour budget is
 * app-wide rather than per-user. The caller's debounce keeps ordinary typing well inside it; a
 * proxy that holds the key is the upgrade path if installs ever make it bite.
 *
 * ponytail: a cancelled request abandons the socket until the 10s timeout rather than interrupting
 * it; the result is discarded either way. Switch to `runInterruptible` if that ever shows up.
 */
internal fun fdcGet(path: String, params: String): FdcResponse {
    // No key configured (a fresh clone with no `fdcApiKey` gradle property) would spend a round
    // trip to be told 403. Fail here instead.
    val key = BuildConfig.FDC_API_KEY
    if (key.isEmpty()) return FdcResponse.Failed

    val connection = (URL("$BASE_URL$path?$params&api_key=$key").openConnection() as HttpURLConnection).apply {
        connectTimeout = TIMEOUT_MS
        readTimeout = TIMEOUT_MS
        setRequestProperty("User-Agent", USER_AGENT)
    }
    return try {
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            FdcResponse.Ok(connection.inputStream.bufferedReader().use { it.readText() })
        } else {
            FdcResponse.Failed
        }
    } catch (_: IOException) {
        FdcResponse.Failed
    } finally {
        connection.disconnect()
    }
}

internal val fdcJson = Json { ignoreUnknownKeys = true }

/**
 * A food object as `foods/search` returns it.
 *
 * Null when the entry has no description: it is unusable in the diary.
 */
internal fun JsonObject.toScannedProduct(): ScannedProduct? {
    val name = displayName() ?: return null

    val nutrients = this["foodNutrients"] as? JsonArray
    // Most entries carry kcal directly; the ones that only report kJ still have usable macros.
    val kcal = nutrients.nutrient(NUTRIENT_KCAL)
        ?: nutrients.nutrient(NUTRIENT_KJ)?.div(KJ_PER_KCAL)
        ?: 0.0

    return ScannedProduct(
        name = name,
        portionAmount = PORTION_G,
        portionUnit = "g",
        calories = kcal.roundToInt(),
        proteinG = nutrients.nutrient(NUTRIENT_PROTEIN)?.roundToInt() ?: 0,
        carbsG = nutrients.nutrient(NUTRIENT_CARBS)?.roundToInt() ?: 0,
        fatG = nutrients.nutrient(NUTRIENT_FAT)?.roundToInt() ?: 0,
        fiberG = nutrients.nutrient(NUTRIENT_FIBER)?.roundToInt() ?: 0,
        sugarG = (nutrients.nutrient(NUTRIENT_SUGAR) ?: nutrients.nutrient(NUTRIENT_SUGAR_NLEA))
            ?.roundToInt() ?: 0,
        sodiumMg = nutrients.nutrient(NUTRIENT_SODIUM)?.roundToInt() ?: 0,
    )
}

/**
 * FDC shouts its branded descriptions ("SPICY SWEET CHILI FLAVORED TORTILLA CHIPS") while its
 * Foundation rows are ordinary prose ("Broccoli, raw"), so only the all-caps ones are recased —
 * title-casing everything would turn "Broccoli, raw" into "Broccoli, Raw". The brand leads, since
 * two brands' version of the same product are otherwise indistinguishable in the diary.
 */
private fun JsonObject.displayName(): String? {
    val description = string("description")?.normalizeCase().orEmpty()
    if (description.isEmpty()) return null

    val brand = (string("brandName") ?: string("brandOwner"))?.normalizeCase().orEmpty()
    return when {
        brand.isEmpty() -> description
        description.startsWith(brand, ignoreCase = true) -> description
        else -> "$brand · $description"
    }
}

/** All-caps in, title case out; anything already carrying lowercase is left as its source wrote it. */
private fun String.normalizeCase(): String {
    val trimmed = trim()
    if (trimmed != trimmed.uppercase()) return trimmed
    return trimmed.split(' ').joinToString(" ") { word ->
        word.lowercase().replaceFirstChar(Char::uppercaseChar)
    }
}

private fun JsonObject.string(key: String): String? =
    this[key]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

/**
 * Nutrients arrive as a list keyed by [nutrientId][id] rather than as named fields. Values come
 * back as numbers most of the time and as quoted strings some of the time; both are accepted rather
 * than dropping the field.
 */
private fun JsonArray?.nutrient(id: Int): Double? {
    val entry = this.orEmpty().firstOrNull {
        (it as? JsonObject)?.get("nutrientId")?.jsonPrimitive?.number()?.toInt() == id
    } ?: return null
    return (entry as JsonObject)["value"]?.jsonPrimitive?.number()
}

private fun JsonPrimitive.number(): Double? = doubleOrNull ?: contentOrNull?.toDoubleOrNull()
