package ph.mart.healthapp.core.data.food

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The bits [BarcodeLookupRepositoryImpl] and [FoodSearchRepositoryImpl] share: one GET against
 * Open Food Facts and one product-object → [ScannedProduct] mapping. A barcode lookup and a text
 * search return the same product shape, so they parse it the same way.
 */

/** Open Food Facts rejects anonymous clients — an identifying User-Agent is required by their
 * API terms, not optional. */
private const val USER_AGENT = "FitPulse/1.0 (Android)"

private const val TIMEOUT_MS = 10_000

private const val KJ_PER_KCAL = 4.184

/** Open Food Facts stores every `*_100g` field per 100 g. */
internal const val PORTION_G = 100.0

internal sealed interface OffResponse {
    data class Ok(val body: String) : OffResponse
    data object NotFound : OffResponse
    data object Failed : OffResponse
}

/**
 * Plain [HttpURLConnection] against a public JSON endpoint — no HTTP client dependency for a
 * single GET, matching how [FoodRecognitionRepositoryImpl] leans on the Firebase SDK rather than
 * introducing one.
 *
 * ponytail: a cancelled request abandons the socket until the 10s timeout rather than interrupting
 * it; the result is discarded either way. Switch to `runInterruptible` if that ever shows up.
 */
internal fun openFoodFactsGet(url: String): OffResponse {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = TIMEOUT_MS
        readTimeout = TIMEOUT_MS
        setRequestProperty("User-Agent", USER_AGENT)
    }
    return try {
        when (connection.responseCode) {
            HttpURLConnection.HTTP_OK ->
                OffResponse.Ok(connection.inputStream.bufferedReader().use { it.readText() })

            HttpURLConnection.HTTP_NOT_FOUND -> OffResponse.NotFound
            else -> OffResponse.Failed
        }
    } catch (_: IOException) {
        OffResponse.Failed
    } finally {
        connection.disconnect()
    }
}

internal val offJson = Json { ignoreUnknownKeys = true }

/**
 * A product object as both endpoints return it.
 *
 * Null when the entry has no name: it is unusable in the diary, and crowd-sourced entries
 * genuinely do come back nameless.
 */
internal fun JsonObject.toScannedProduct(): ScannedProduct? {
    val name = this["product_name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
    if (name.isEmpty()) return null

    val nutriments = this["nutriments"]?.jsonObject
    // Most entries carry kcal directly; the ones that only report kJ still have usable macros.
    val kcal = nutriments.number("energy-kcal_100g")
        ?: nutriments.number("energy_100g")?.div(KJ_PER_KCAL)
        ?: 0.0

    return ScannedProduct(
        name = name,
        portionAmount = PORTION_G,
        portionUnit = "g",
        calories = kcal.roundToInt(),
        proteinG = nutriments.number("proteins_100g")?.roundToInt() ?: 0,
        carbsG = nutriments.number("carbohydrates_100g")?.roundToInt() ?: 0,
        fatG = nutriments.number("fat_100g")?.roundToInt() ?: 0,
    )
}

/** Nutriment values come back as numbers most of the time and as quoted strings some of the time;
 * both are accepted rather than dropping the field. */
private fun JsonObject?.number(key: String): Double? {
    val primitive = this?.get(key)?.jsonPrimitive ?: return null
    return primitive.doubleOrNull ?: primitive.contentOrNull?.toDoubleOrNull()
}
