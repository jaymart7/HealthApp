package ph.mart.healthapp.core.data.insight

import kotlin.math.abs
import ph.mart.healthapp.core.data.food.DiaryTotals
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.TREND_ARROW_DEADBAND_KG
import ph.mart.healthapp.core.data.profile.WeightTrendDisplay

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

/**
 * The numbers block, formatted once for both callers.
 *
 * [InsightRepositoryImpl]'s prompt and the coach's system instruction describe the same day to the
 * same model, so they format it in the same place: a field added to [InsightRequest] and shown to
 * one but not the other would be a coach contradicting the card that sent the user to it.
 */
internal fun dayNumbersBlock(request: InsightRequest): String = buildString {
    appendLine("- Calories: ${request.caloriesConsumed} of ${request.caloriesTarget} kcal")
    appendLine("- Protein: ${request.proteinG} of ${request.proteinTargetG} g")
    appendLine("- Carbs: ${request.carbsG} of ${request.carbsTargetG} g")
    appendLine("- Fat: ${request.fatG} of ${request.fatTargetG} g")
    appendLine("- Water: ${request.waterGlasses} of ${request.waterGoalGlasses} glasses")
    appendLine("- Logging streak: ${request.streakDays} days")
    request.weightDeltaKg?.let { appendLine("- Weight change over the last week: %+.1f kg".format(it)) }
}

/**
 * The rule-based line: what the day says when the model doesn't. First matching rule wins; null
 * means there is nothing worth remarking on.
 *
 * It lives here rather than in `:feature:home` because two feature modules now fall back to it —
 * Home's insight card and the coach's failed-send bubble — and `:feature:*` modules never import
 * each other. Same reason `goalProjection()` sits in `progress/`: pure derivation over
 * `:core:data` types, no table, no repository.
 *
 * The three sentences stay in Kotlin for now — this is a pure function with a JVM test over its
 * wording, and moving them means returning a case type for a composable to resolve. Deferred by
 * decision in the localization pass, not overlooked; `goalProjectionLine()` is the twin.
 */
fun insightFor(totals: DiaryTotals, targets: DailyTargets, trend: WeightTrendDisplay): String? = when {
    totals.calories > targets.calories ->
        "You're ${totals.calories - targets.calories} kcal over today's target."
    targets.proteinG > 0 && totals.calories > 0 && totals.proteinG < targets.proteinG * 0.6 ->
        "You're ${targets.proteinG - totals.proteinG}g short on protein today."
    trend.hasPrior && abs(trend.deltaKg) >= TREND_ARROW_DEADBAND_KG ->
        "${formatDelta(trend.deltaKg)} kg over the last week — keep it steady."
    else -> null
}

/** Signed, one decimal, tabular-friendly — e.g. "-0.6", "+1.2". */
fun formatDelta(deltaKg: Double): String = "%+.1f".format(deltaKg)

/**
 * The same three rules, off the payload the model was given rather than off the screen's state.
 *
 * The coach's fallback goes through here so an unanswered question is still answered with the
 * numbers the failed call would have used — a line quoting anything else would be worse than
 * none. The reconstruction is lossless for what [insightFor] actually reads: it touches
 * `targets.calories`/`proteinG` and `trend.deltaKg`/`hasPrior` and nothing else, hence the unused
 * `floor` and `currentKg` below.
 */
fun insightFor(request: InsightRequest): String? = insightFor(
    totals = DiaryTotals(
        calories = request.caloriesConsumed,
        proteinG = request.proteinG,
        carbsG = request.carbsG,
        fatG = request.fatG,
    ),
    targets = DailyTargets(
        calories = request.caloriesTarget,
        proteinG = request.proteinTargetG,
        carbsG = request.carbsTargetG,
        fatG = request.fatTargetG,
        floor = 0,
    ),
    trend = WeightTrendDisplay(
        currentKg = 0.0,
        deltaKg = request.weightDeltaKg ?: 0.0,
        hasPrior = request.weightDeltaKg != null,
    ),
)
