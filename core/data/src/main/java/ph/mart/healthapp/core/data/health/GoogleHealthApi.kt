package ph.mart.healthapp.core.data.health

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.xml.datatype.DatatypeFactory
import kotlin.math.roundToInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType

/**
 * The Google Health API transport, shaped exactly like `food/OpenFoodFacts.kt`: plain
 * [HttpURLConnection], no HTTP client dependency, and every parser split from its socket so the
 * response shapes are unit-testable without a network.
 *
 * https://developers.google.com/health/reference/rest
 */
private const val BASE = "https://health.googleapis.com/v4/users/me/dataTypes"

private const val TIMEOUT_MS = 15_000

/** `pageSize` caps at 25 for exercise and sleep, 10000 elsewhere. */
internal const val SESSION_PAGE_SIZE = 25

internal const val SAMPLE_PAGE_SIZE = 500

/**
 * How far back a first sync reaches. A month of history is enough to make the diary and the
 * calorie budget look right without pulling a user's entire archive onto the device — data
 * minimisation is a verification criterion, not a nicety.
 */
internal const val BACKFILL_DAYS = 30

internal const val DAY_MILLIS = 24L * 60 * 60 * 1000

/**
 * Every window is re-queried one day behind the cursor. Data points arrive late (a watch syncs
 * hours after the workout), and `health_link`'s primary key makes the overlap free.
 */
internal const val SYNC_OVERLAP_MILLIS = DAY_MILLIS

internal sealed interface HealthResponse {
    data class Ok(val body: String) : HealthResponse

    /** Token expired — re-authorize once and retry. */
    data object Unauthorized : HealthResponse

    /** Scope revoked from myaccount.google.com. Not retryable: drop to disconnected. */
    data object Forbidden : HealthResponse
    data object Failed : HealthResponse
}

internal fun healthGet(url: String, token: String): HealthResponse =
    request(url, method = "GET", token = token, body = null)

internal fun healthPost(url: String, token: String?, body: String): HealthResponse =
    request(url, method = "POST", token = token, body = body)

private fun request(url: String, method: String, token: String?, body: String?): HealthResponse {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = TIMEOUT_MS
        readTimeout = TIMEOUT_MS
        setRequestProperty("Accept", "application/json")
        if (token != null) setRequestProperty("Authorization", "Bearer $token")
        if (body != null) {
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            doOutput = true
        }
    }
    return try {
        if (body != null) connection.outputStream.use { it.write(body.toByteArray()) }
        when (connection.responseCode) {
            HttpURLConnection.HTTP_OK -> HealthResponse.Ok(
                connection.inputStream.bufferedReader().use { it.readText() },
            )

            HttpURLConnection.HTTP_UNAUTHORIZED -> HealthResponse.Unauthorized
            HttpURLConnection.HTTP_FORBIDDEN -> HealthResponse.Forbidden
            // A 2xx with no body (revoke answers 200 with an empty one) is still a success.
            in 200..299 -> HealthResponse.Ok("")
            else -> HealthResponse.Failed
        }
    } catch (_: IOException) {
        HealthResponse.Failed
    } finally {
        connection.disconnect()
    }
}

internal val healthJson = Json { ignoreUnknownKeys = true }

/**
 * The data types FitPulse touches, each with the filter field its record shape uses: sessions
 * (exercise, sleep) are bounded by `interval.start_time`, point samples (weight) by
 * `physical_time`. Adding a type here is the whole of adding a type.
 */
/**
 * The two write targets. Kept apart from [HealthDataType] on purpose: the nutrition scope is
 * `writeonly`, so these can be created and deleted but never listed, and giving them a filter
 * field would imply otherwise.
 */
internal const val NUTRITION_LOG = "nutrition-log"
internal const val HYDRATION_LOG = "hydration-log"

internal fun createDataPointUrl(dataType: String): String = "$BASE/$dataType/dataPoints"

internal fun batchDeleteUrl(dataType: String): String = "$BASE/$dataType/dataPoints:batchDelete"

/** A maximum of 10000 names per request; FitPulse chunks well below that. */
internal const val BATCH_DELETE_SIZE = 500

internal fun batchDeleteBody(remoteNames: List<String>): String = buildJsonObject {
    putJsonArray("names") { remoteNames.forEach { add(it) } }
}.toString()

/**
 * A logged meal, as `nutrition-log` wants it.
 *
 * FitPulse stores a meal against a day and a meal slot, not a clock time, so the interval is
 * synthesised from the slot. That is the honest reading of what the user recorded — "lunch on
 * Tuesday" — and it keeps the meal in the right place on anyone else's timeline.
 */
internal fun nutritionLogBody(entry: FoodEntry, dayStartMillis: Long): String {
    val start = dayStartMillis + entry.mealType.hourOfDay() * 60 * 60 * 1000L
    return buildJsonObject {
        putJsonObject("nutritionLog") {
            putJsonObject("interval") {
                put("startTime", rfc3339(start))
                put("endTime", rfc3339(start + 30 * 60 * 1000L))
            }
            put("foodDisplayName", entry.name)
            put("mealType", entry.mealType.remoteName())
            putJsonObject("energy") { put("kcal", entry.calories) }
            putJsonObject("totalCarbohydrate") { put("grams", entry.carbsG) }
            putJsonObject("totalFat") { put("grams", entry.fatG) }
            putJsonArray("nutrients") {
                add(
                    buildJsonObject {
                        put("nutrient", "PROTEIN")
                        putJsonObject("quantity") { put("grams", entry.proteinG) }
                    },
                )
            }
            putJsonObject("serving") { put("amount", entry.portionAmount) }
        }
    }.toString()
}

/** A day's glasses as one hydration event. See `pushHydration` for why it's a whole day. */
internal fun hydrationLogBody(millilitres: Int, dayStartMillis: Long): String = buildJsonObject {
    putJsonObject("hydrationLog") {
        putJsonObject("interval") {
            put("startTime", rfc3339(dayStartMillis + 12 * 60 * 60 * 1000L))
            put("endTime", rfc3339(dayStartMillis + 12 * 60 * 60 * 1000L + 1000L))
        }
        putJsonObject("amountConsumed") { put("milliliters", millilitres) }
    }
}.toString()

/** `dataPoints.create` answers with the created point; its `name` is what we record. */
internal fun parseCreatedName(body: String): String? = parseRoot(body)?.string("name")

private fun MealType.remoteName(): String = when (this) {
    MealType.Breakfast -> "BREAKFAST"
    MealType.Lunch -> "LUNCH"
    MealType.Dinner -> "DINNER"
    MealType.Snacks -> "SNACK"
}

private fun MealType.hourOfDay(): Int = when (this) {
    MealType.Breakfast -> 8
    MealType.Lunch -> 12
    MealType.Dinner -> 19
    MealType.Snacks -> 16
}

internal enum class HealthDataType(
    val id: String,
    val filterField: String,
    val pageSize: Int,
) {
    Exercise("exercise", "interval.start_time", SESSION_PAGE_SIZE),
    Sleep("sleep", "interval.start_time", SESSION_PAGE_SIZE),
    Weight("weight", "physical_time", SAMPLE_PAGE_SIZE),
    Steps("steps", "interval.start_time", SAMPLE_PAGE_SIZE),
}

/**
 * One page of `dataPoints.list`, filtered to the window we don't already have. The filter is
 * AIP-160. Asking only for the window since the cursor *is* the data-minimisation story on the
 * verification form.
 */
internal fun dataPointsUrl(
    dataType: HealthDataType,
    sinceMillis: Long,
    pageToken: String? = null,
): String {
    val filter = "${dataType.filterField} >= \"${rfc3339(sinceMillis)}\""
    return buildString {
        append("$BASE/${dataType.id}/dataPoints")
        append("?pageSize=${dataType.pageSize}")
        append("&filter=").append(URLEncoder.encode(filter, "UTF-8"))
        if (pageToken != null) append("&pageToken=").append(URLEncoder.encode(pageToken, "UTF-8"))
    }
}

private val rfc3339Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

internal fun rfc3339(millis: Long): String = rfc3339Format.format(Date(millis))

/**
 * RFC-3339 in, epoch millis out. `DatatypeFactory` rather than [SimpleDateFormat] because the API
 * returns offsets and fractional seconds interchangeably, and it is already in the platform —
 * `java.time` needs API 26 and this module builds to minSdk 24 with no desugaring.
 */
private val datatypeFactory by lazy { DatatypeFactory.newInstance() }

internal fun parseRfc3339(value: String?): Long? = value?.let {
    runCatching { datatypeFactory.newXMLGregorianCalendar(it).toGregorianCalendar().timeInMillis }
        .getOrNull()
}

/** A protobuf Duration string — `"1800s"`, sometimes `"1800.5s"`. */
internal fun parseDurationSeconds(value: String?): Double? =
    value?.removeSuffix("s")?.toDoubleOrNull()

/**
 * What every imported data point has to carry: its own resource name (the dedup key) and its own
 * time (the sync cursor). Everything else is per-type.
 */
internal interface RemotePoint {
    val remoteName: String
    val timeMillis: Long
}

internal data class Page<T : RemotePoint>(val items: List<T>, val nextPageToken: String?)

/** One imported workout, already in FitPulse's own vocabulary. */
internal data class RemoteExercise(
    override val remoteName: String,
    override val timeMillis: Long,
    val type: ExerciseType,
    val name: String,
    val minutes: Int,
    val burnedKcal: Int,
    /** The watch's own step count for the session, so the day's step credit can subtract it. */
    val steps: Int,
) : RemotePoint

/** One imported weigh-in. The API carries grams; the app is metric-first in kilograms. */
internal data class RemoteWeight(
    override val remoteName: String,
    override val timeMillis: Long,
    val weightKg: Double,
) : RemotePoint

/**
 * One bucket of steps. The API reports steps intra-day rather than as a daily total, so several
 * of these fold into one `step_day` row — which is why steps are the one imported type that does
 * not ride `health_link`: a bucket has no stable identity worth keying a link by.
 */
internal data class RemoteSteps(
    override val remoteName: String,
    override val timeMillis: Long,
    val count: Int,
) : RemotePoint

/** One imported night. [timeMillis] is when it started; [endMillis] is what dates it. */
internal data class RemoteSleep(
    override val remoteName: String,
    override val timeMillis: Long,
    val endMillis: Long,
    val minutesAsleep: Int,
) : RemotePoint

/**
 * Split from the socket so a captured response body is all a test needs.
 *
 * A data point with no usable interval is dropped rather than logged at an invented time — an
 * undated workout cannot be placed in a diary that is keyed by day.
 */
internal fun parseExercisePage(body: String): Page<RemoteExercise> {
    val root = parseRoot(body) ?: return Page(emptyList(), null)

    val exercises = root["dataPoints"]?.jsonArray.orEmpty().mapNotNull { element ->
        val point = element as? JsonObject ?: return@mapNotNull null
        val remoteName = point.string("name") ?: return@mapNotNull null
        val exercise = point["exercise"]?.jsonObject ?: return@mapNotNull null
        val interval = exercise["interval"]?.jsonObject
        val start = parseRfc3339(interval?.string("startTime")) ?: return@mapNotNull null
        val end = parseRfc3339(interval?.string("endTime"))

        // activeDuration excludes pauses, so it beats end - start when both are present.
        val seconds = parseDurationSeconds(exercise.string("activeDuration"))
            ?: end?.let { (it - start) / 1000.0 }
            ?: return@mapNotNull null

        val type = exerciseTypeOf(exercise.string("exerciseType"))
        RemoteExercise(
            remoteName = remoteName,
            timeMillis = start,
            type = type,
            name = exercise.string("displayName")?.trim().orEmpty().ifEmpty { type.label },
            minutes = (seconds / 60.0).roundToInt().coerceAtLeast(0),
            burnedKcal = exercise["metricsSummary"]?.jsonObject.number("caloriesKcal")
                ?.roundToInt()?.coerceAtLeast(0) ?: 0,
            // A session that reports no steps falls back to the cadence estimate rather than 0 —
            // otherwise a run the watch didn't count would have its steps credited a second time
            // as ordinary walking.
            steps = exercise["metricsSummary"]?.jsonObject.number("steps")
                ?.roundToInt()?.takeIf { it > 0 }
                ?: estimatedSteps(type, (seconds / 60.0).roundToInt().coerceAtLeast(0)),
        )
    }
    return Page(exercises, root.string("nextPageToken"))
}

/**
 * Weight is a point sample rather than a session: `sampleTime.physicalTime` and `weightGrams`.
 *
 * ponytail: the timestamp is read from `sampleTime.physicalTime` first and a bare `physicalTime`
 * second, because the reference documents the nested form and the wire has been known to flatten
 * it. Pin it to one path once a real response has been captured from a live account.
 */
internal fun parseWeightPage(body: String): Page<RemoteWeight> {
    val root = parseRoot(body) ?: return Page(emptyList(), null)

    val weights = root["dataPoints"]?.jsonArray.orEmpty().mapNotNull { element ->
        val point = element as? JsonObject ?: return@mapNotNull null
        val remoteName = point.string("name") ?: return@mapNotNull null
        val weight = point["weight"]?.jsonObject ?: return@mapNotNull null
        val sampleTime = weight["sampleTime"]?.jsonObject
        val time = parseRfc3339(sampleTime.string("physicalTime") ?: weight.string("physicalTime"))
            ?: return@mapNotNull null
        val grams = weight.number("weightGrams") ?: return@mapNotNull null
        // A zero or negative weigh-in is a broken scale reading, not a measurement.
        if (grams <= 0.0) return@mapNotNull null

        RemoteWeight(remoteName = remoteName, timeMillis = time, weightKg = grams / 1000.0)
    }
    return Page(weights, root.string("nextPageToken"))
}

/**
 * Sleep is a session with an optional stage breakdown. When stages are present the asleep time is
 * their sum minus anything marked awake — a night with an hour of staring at the ceiling did not
 * contain eight hours of sleep. With no stages, the interval is all there is to go on.
 */
internal fun parseSleepPage(body: String): Page<RemoteSleep> {
    val root = parseRoot(body) ?: return Page(emptyList(), null)

    val nights = root["dataPoints"]?.jsonArray.orEmpty().mapNotNull { element ->
        val point = element as? JsonObject ?: return@mapNotNull null
        val remoteName = point.string("name") ?: return@mapNotNull null
        val sleep = point["sleep"]?.jsonObject ?: return@mapNotNull null
        val interval = sleep["interval"]?.jsonObject
        val start = parseRfc3339(interval.string("startTime")) ?: return@mapNotNull null
        val end = parseRfc3339(interval.string("endTime")) ?: return@mapNotNull null

        val stages = sleep["stages"]?.jsonArray.orEmpty().mapNotNull { it as? JsonObject }
        val asleepMillis = if (stages.isEmpty()) {
            end - start
        } else {
            stages.filterNot { it.string("type").orEmpty().uppercase().contains("AWAKE") }
                .sumOf { stage ->
                    val stageStart = parseRfc3339(stage.string("startTime"))
                    val stageEnd = parseRfc3339(stage.string("endTime"))
                    if (stageStart != null && stageEnd != null) (stageEnd - stageStart) else 0L
                }
        }
        if (asleepMillis <= 0L) return@mapNotNull null

        RemoteSleep(
            remoteName = remoteName,
            timeMillis = start,
            endMillis = end,
            minutesAsleep = (asleepMillis / 60_000.0).roundToInt(),
        )
    }
    return Page(nights, root.string("nextPageToken"))
}

/**
 * Steps come back as intervals carrying a count. The caller groups them by local day and sums.
 *
 * ponytail: the count is read from `count`, then `steps`, then `delta`, because the reference and
 * the wire have used more than one name for it and this module has no way to try a live account.
 * Pin it to one field once a real response has been captured — same outstanding job as
 * `parseWeightPage`'s timestamp.
 */
internal fun parseStepsPage(body: String): Page<RemoteSteps> {
    val root = parseRoot(body) ?: return Page(emptyList(), null)

    val buckets = root["dataPoints"]?.jsonArray.orEmpty().mapNotNull { element ->
        val point = element as? JsonObject ?: return@mapNotNull null
        val remoteName = point.string("name") ?: return@mapNotNull null
        val steps = point["steps"]?.jsonObject ?: return@mapNotNull null
        // An undated bucket cannot be placed on a day, so it is dropped rather than invented —
        // the same treatment parseExercisePage gives a workout with no interval.
        val start = parseRfc3339(steps["interval"]?.jsonObject.string("startTime"))
            ?: return@mapNotNull null
        val count = (steps.number("count") ?: steps.number("steps") ?: steps.number("delta"))
            ?.roundToInt() ?: return@mapNotNull null
        if (count <= 0) return@mapNotNull null

        RemoteSteps(remoteName = remoteName, timeMillis = start, count = count)
    }
    return Page(buckets, root.string("nextPageToken"))
}

private fun parseRoot(body: String): JsonObject? =
    runCatching { healthJson.parseToJsonElement(body).jsonObject }.getOrNull()

/**
 * The API ships 175+ activity types and keeps adding them, so this matches on keywords instead of
 * enumerating a table that would silently rot. Anything unrecognised lands on [ExerciseType.Other]
 * — the entry still carries its own name and its measured burn, so nothing is lost but the icon.
 */
internal fun exerciseTypeOf(remoteType: String?): ExerciseType {
    val value = remoteType.orEmpty().uppercase()
    return when {
        value.contains("RUN") || value.contains("JOG") -> ExerciseType.Run
        value.contains("WALK") || value.contains("HIK") -> ExerciseType.Walk
        value.contains("BIK") || value.contains("CYCL") || value.contains("SPIN") -> ExerciseType.Cycle
        value.contains("SWIM") -> ExerciseType.Swim
        value.contains("YOGA") || value.contains("PILATES") || value.contains("STRETCH") -> ExerciseType.Yoga
        value.contains("INTERVAL") || value.contains("HIIT") || value.contains("BOOTCAMP") -> ExerciseType.Hiit
        value.contains("STRENGTH") || value.contains("WEIGHT") || value.contains("CROSSFIT") -> ExerciseType.Strength
        else -> ExerciseType.Other
    }
}

private fun JsonObject?.string(key: String): String? =
    this?.get(key)?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() }

/** Numeric fields come back as numbers most of the time and as quoted strings some of the time. */
private fun JsonObject?.number(key: String): Double? {
    val primitive = this?.get(key)?.jsonPrimitive ?: return null
    return primitive.doubleOrNull ?: primitive.contentOrNull?.toDoubleOrNull()
}
