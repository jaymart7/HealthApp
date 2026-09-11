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
 * What [BarcodeLookupRepositoryImpl] needs: one GET against USDA FoodData Central and one
 * food-object → [ScannedProduct] mapping. FDC has no barcode endpoint, so a scan goes through
 * `foods/search` too. Free-text search used to share this file and no longer does — it reads the
 * shipped [COMMON_FOODS] list instead, which is why nothing here has an offline path: a scan
 * without network is a scan that fails.
 */

private const val BASE_URL = "https://api.nal.usda.gov/fdc/v1/"

private const val USER_AGENT = "FitPulse/1.0 (Android)"

private const val TIMEOUT_MS = 10_000

/** Shared with the Open Food Facts mapper, which has the same kJ-only fallback. */
internal const val KJ_PER_KCAL = 4.184

/** FDC reports every search nutrient per 100 g, whatever the package's own serving size says —
 * the convention [COMMON_FOODS] follows too, so a scan and a picked food seed the same form. */
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

/** FDC reports sodium, calcium and potassium in milligrams. */
private const val NUTRIENT_SODIUM = 1093
private const val NUTRIENT_CALCIUM = 1087
private const val NUTRIENT_POTASSIUM = 1092

/** Vitamin D (D2 + D3) is reported in micrograms, which is also how [Nutrients] stores it. */
private const val NUTRIENT_VITAMIN_D = 1114

/** Iron comes back in milligrams and is stored in micrograms — a branded row routinely reports
 * a fraction of a milligram, and rounding that to an Int mg would throw it away. */
private const val NUTRIENT_IRON = 1089
private const val UG_PER_MG = 1000

internal sealed interface FdcResponse {
    data class Ok(val body: String) : FdcResponse

    /** Network problem, a 403 from a bad key, or a 429 over the hourly budget — all of them mean
     * "try again", so they are one case. A *miss* is not here: FDC answers an unmatched query with
     * HTTP 200 and an empty `foods` array, which [parseFdcProduct] handles. */
    data object Failed : FdcResponse
}

/**
 * Plain [HttpURLConnection] against a public JSON endpoint — no HTTP client dependency for a
 * single GET, matching how [FoodRecognitionRepositoryImpl] leans on the Firebase SDK rather than
 * introducing one.
 *
 * ponytail: the key is one signed key shared by every install, so the 3600 requests/hour budget is
 * app-wide rather than per-user. Only a barcode scan spends it now, which is a deliberate act
 * rather than a keystroke; a proxy that holds the key is the upgrade path if installs make it bite.
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

    val reported = this["foodNutrients"] as? JsonArray
    // Most entries carry kcal directly; the ones that only report kJ still have usable macros.
    val kcal = reported.nutrient(NUTRIENT_KCAL)
        ?: reported.nutrient(NUTRIENT_KJ)?.div(KJ_PER_KCAL)
        ?: 0.0

    return ScannedProduct(
        name = name,
        portionAmount = PORTION_G,
        portionUnit = "g",
        calories = kcal.roundToInt(),
        proteinG = reported.nutrient(NUTRIENT_PROTEIN)?.roundToInt() ?: 0,
        carbsG = reported.nutrient(NUTRIENT_CARBS)?.roundToInt() ?: 0,
        fatG = reported.nutrient(NUTRIENT_FAT)?.roundToInt() ?: 0,
        nutrients = Nutrients(
            fiberG = reported.nutrient(NUTRIENT_FIBER)?.roundToInt() ?: 0,
            sugarG = (reported.nutrient(NUTRIENT_SUGAR) ?: reported.nutrient(NUTRIENT_SUGAR_NLEA))
                ?.roundToInt() ?: 0,
            sodiumMg = reported.nutrient(NUTRIENT_SODIUM)?.roundToInt() ?: 0,
            vitaminDUg = reported.nutrient(NUTRIENT_VITAMIN_D)?.roundToInt() ?: 0,
            calciumMg = reported.nutrient(NUTRIENT_CALCIUM)?.roundToInt() ?: 0,
            ironUg = reported.nutrient(NUTRIENT_IRON)?.times(UG_PER_MG)?.roundToInt() ?: 0,
            potassiumMg = reported.nutrient(NUTRIENT_POTASSIUM)?.roundToInt() ?: 0,
        ),
    )
}

/** The brand-leads rule and the all-caps recasing both live in [brandedName], which the Open Food
 * Facts mapper shares — a scanned package reads the same however it was resolved. */
private fun JsonObject.displayName(): String? =
    brandedName(brand = string("brandName") ?: string("brandOwner"), description = string("description"))

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
