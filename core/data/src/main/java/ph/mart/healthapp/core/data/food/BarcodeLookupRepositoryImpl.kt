package ph.mart.healthapp.core.data.food

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val ENDPOINT =
    "https://world.openfoodfacts.org/api/v2/product/%s.json?fields=product_name,nutriments"

/** Open Food Facts rejects anonymous clients — an identifying User-Agent is required by their
 * API terms, not optional. */
private const val USER_AGENT = "FitPulse/1.0 (Android)"

private const val TIMEOUT_MS = 10_000

private const val KJ_PER_KCAL = 4.184

/** Open Food Facts stores every `*_100g` field per 100 g. */
private const val PORTION_G = 100.0

/**
 * Plain [HttpURLConnection] against one public JSON endpoint — no HTTP client dependency for a
 * single GET, matching how [FoodRecognitionRepositoryImpl] leans on the Firebase SDK rather than
 * introducing one.
 *
 * ponytail: a cancelled lookup abandons the socket until the 10s timeout rather than interrupting
 * it; the result is discarded either way. Switch to `runInterruptible` if that ever shows up.
 */
internal class BarcodeLookupRepositoryImpl : BarcodeLookupRepository {

    override suspend fun lookup(barcode: String): BarcodeLookupResult = withContext(Dispatchers.IO) {
        // The barcode arrives from an image decoder, so it is untrusted input on its way into a
        // URL — EAN/UPC are digits only, and anything else is not a product code.
        val code = barcode.filter(Char::isDigit)
        if (code.isEmpty()) return@withContext BarcodeLookupResult.NotFound

        val connection = (URL(ENDPOINT.format(code)).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
        }
        try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK ->
                    parseOpenFoodFactsProduct(connection.inputStream.bufferedReader().use { it.readText() })

                HttpURLConnection.HTTP_NOT_FOUND -> BarcodeLookupResult.NotFound
                else -> BarcodeLookupResult.Failed
            }
        } catch (_: IOException) {
            BarcodeLookupResult.Failed
        } finally {
            connection.disconnect()
        }
    }
}

private val json = Json { ignoreUnknownKeys = true }

/**
 * Split out of the network call so the response shape is unit-testable without a socket.
 *
 * A product with no name is unusable in the diary, so it counts as [BarcodeLookupResult.NotFound]
 * rather than a half-filled row — crowd-sourced entries genuinely do come back nameless.
 */
internal fun parseOpenFoodFactsProduct(body: String): BarcodeLookupResult {
    val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        ?: return BarcodeLookupResult.Failed
    if (root["status"]?.jsonPrimitive?.intOrNull != 1) return BarcodeLookupResult.NotFound

    val product = root["product"]?.jsonObject ?: return BarcodeLookupResult.NotFound
    val name = product["product_name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
    if (name.isEmpty()) return BarcodeLookupResult.NotFound

    val nutriments = product["nutriments"]?.jsonObject
    // Most entries carry kcal directly; the ones that only report kJ still have usable macros.
    val kcal = nutriments.number("energy-kcal_100g")
        ?: nutriments.number("energy_100g")?.div(KJ_PER_KCAL)
        ?: 0.0

    return BarcodeLookupResult.Found(
        ScannedProduct(
            name = name,
            portionAmount = PORTION_G,
            portionUnit = "g",
            calories = kcal.roundToInt(),
            proteinG = nutriments.number("proteins_100g")?.roundToInt() ?: 0,
            carbsG = nutriments.number("carbohydrates_100g")?.roundToInt() ?: 0,
            fatG = nutriments.number("fat_100g")?.roundToInt() ?: 0,
        ),
    )
}

/** Nutriment values come back as numbers most of the time and as quoted strings some of the time;
 * both are accepted rather than dropping the field. */
private fun JsonObject?.number(key: String): Double? {
    val primitive = this?.get(key)?.jsonPrimitive ?: return null
    return primitive.doubleOrNull ?: primitive.contentOrNull?.toDoubleOrNull()
}
