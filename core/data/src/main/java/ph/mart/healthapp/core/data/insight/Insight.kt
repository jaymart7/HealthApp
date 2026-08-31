package ph.mart.healthapp.core.data.insight

import ph.mart.healthapp.core.data.profile.Goal

/**
 * Everything the model is told about the day — and deliberately nothing else.
 *
 * No age, sex, height, absolute weight, name or email: a one-line nudge needs the gaps between
 * what was eaten and what was targeted, not the inputs Mifflin–St Jeor chewed on to produce the
 * target. That is the same data-minimisation answer the 30-day health backfill is written
 * against, applied to the one other place FitPulse sends a user's numbers off the device.
 *
 * [weightDeltaKg] is null when there is no prior weigh-in to compare against, rather than 0.0 —
 * "no change" and "nothing to compare" are different sentences.
 */
data class InsightRequest(
    val goal: Goal,
    val caloriesConsumed: Int,
    val caloriesTarget: Int,
    val proteinG: Int,
    val proteinTargetG: Int,
    val carbsG: Int,
    val carbsTargetG: Int,
    val fatG: Int,
    val fatTargetG: Int,
    val waterGlasses: Int,
    val waterGoalGlasses: Int,
    val streakDays: Int,
    val weightDeltaKg: Double?,
)

/**
 * The one Gemini-backed feature that isn't food recognition.
 *
 * Null is a first-class answer and means "no insight": offline, a failed call, a model that had
 * nothing to say, or one that ignored the length it was given. Home reads null as "fall back to
 * the rule-based line", which is why this interface has no error type — there is nothing a caller
 * would do differently for one failure over another.
 *
 * Nothing is persisted. An insight is derived from the day, like the streak, so it has no table,
 * no export and no place in the Room schema; [dailyInsight] takes the day rather than resolving
 * it so the caller's notion of "today" and the cache's cannot drift.
 */
interface InsightRepository {
    suspend fun dailyInsight(request: InsightRequest, todayEpochDay: Long): String?
}

/**
 * Past this a model has written a paragraph, not a nudge, and the card it would land in is one
 * line of `bodyMedium` on Home. Rejecting is better than truncating: half a sentence reads as a
 * bug, while the rule-based line it displaces is always complete.
 */
internal const val MAX_INSIGHT_CHARS = 160

private val WHITESPACE = Regex("\\s+")

/**
 * The whole of the trust boundary on model output — everything the app shows passes through here.
 *
 * Kept a pure function on purpose: the photo path parses its response with [org.json.JSONObject],
 * which is stubbed in JVM unit tests and is why `FoodRecognitionRepositoryImpl` has none. A plain
 * sentence needs no JSON, so this one is testable.
 */
internal fun sanitizeInsight(raw: String?): String? {
    val line = raw?.replace(WHITESPACE, " ")?.trim()?.trim('"', '“', '”')?.trim()
    if (line.isNullOrEmpty()) return null
    // The prompt asks for this word when the day holds nothing worth saying — a model with no
    // opinion must not displace the rules, which always have one.
    if (line.trimEnd('.').equals("NONE", ignoreCase = true)) return null
    return line.takeIf { it.length <= MAX_INSIGHT_CHARS }
}
