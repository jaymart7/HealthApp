package ph.mart.healthapp.core.data.insight

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FirebaseAIException
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig

private const val MODEL_NAME = "gemini-2.5-flash"

/** One sentence's worth. A cap here is cheaper than trusting the prompt's "under 120 characters",
 * and [sanitizeInsight] rejects whatever gets through anyway. */
private const val MAX_OUTPUT_TOKENS = 60

/**
 * Plain text out, not JSON: the photo path needs twelve fields and pays for a `responseSchema`
 * plus an `org.json` parse, while this needs one sentence. That choice is what keeps the only
 * validation ([sanitizeInsight]) a pure, testable function.
 *
 * Cached per day in one `@Volatile` field, the same shape the Google Health access token uses —
 * Home re-reads its flows on every water tap and every logged meal, and none of those is worth a
 * model call. The cost of holding it in memory rather than Room is one extra call after process
 * death, against a table, a migration and an export decision.
 */
internal class InsightRepositoryImpl : InsightRepository {

    private val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
        modelName = MODEL_NAME,
        generationConfig = generationConfig { maxOutputTokens = MAX_OUTPUT_TOKENS },
    )

    @Volatile
    private var cached: Pair<Long, String>? = null

    override suspend fun dailyInsight(request: InsightRequest, todayEpochDay: Long): String? {
        cached?.let { (day, text) -> if (day == todayEpochDay) return text }

        val insight = try {
            sanitizeInsight(model.generateContent(content { text(promptFor(request)) }).text)
        } catch (_: FirebaseAIException) {
            // Offline, throttled, App Check refused — all the same to the caller, which falls
            // back to the rule-based line either way.
            null
        } ?: return null

        // Only a usable answer is cached: a failure must not silence the feature for the day.
        cached = todayEpochDay to insight
        return insight
    }
}

/**
 * Numbers in, one sentence out. The constraints are not decoration:
 * - **No medical advice** — FitPulse is a food and body-tracking app, not a clinician, and a
 *   model asked for encouragement will otherwise reach for supplements and symptoms.
 * - **Only the numbers given** — anything else is invention, and the card sits next to the
 *   calorie ring those same numbers drew.
 * - **`NONE`** — a day with nothing notable in it must be able to say so, or the model will
 *   manufacture significance out of an empty morning.
 */
private fun promptFor(request: InsightRequest): String = buildString {
    appendLine(
        "You are a nutrition coach writing one short line for a food-tracking app's home screen.",
    )
    appendLine("Today so far, for a user whose goal is ${request.goal.name.lowercase()} weight:")
    append(dayNumbersBlock(request))
    appendLine()
    appendLine(
        "Reply with exactly one sentence, in the second person, under 120 characters, based only " +
            "on the numbers above. No preamble, no greeting, no emoji, no quotation marks. Give " +
            "no medical advice, no diagnosis, and no supplement or medication suggestions. If " +
            "nothing about the day is worth remarking on, reply with the single word NONE.",
    )
}
