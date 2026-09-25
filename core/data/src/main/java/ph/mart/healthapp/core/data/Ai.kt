package ph.mart.healthapp.core.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerationConfig
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.ThinkingConfig
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.UsageMetadata
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.delay

/**
 * The one model name for every Firebase AI Logic call in the app.
 *
 * It lives here rather than once per call site because a Gemini model is a wasting asset: Google
 * retires them on a published schedule and a retired name is a 404, not a deprecation warning.
 * The whole AI surface degrades gracefully, so a stale name here reads as "the feature is quiet"
 * at every call site — which is exactly how `gemini-1.5-flash` outlived its shutdown in this
 * codebase. One constant is one line to change when the next date lands.
 */
internal const val AI_MODEL_NAME = "gemini-3.5-flash-lite"

/**
 * The one thinking setting for every Firebase AI Logic call in the app, here for the same reason
 * [AI_MODEL_NAME] is: it is a property of the model, so a copy per call site would go stale together.
 *
 * Gemini 2.5 and newer think before answering unless told not to, and **thinking tokens are spent
 * from `maxOutputTokens`**. Every caller here sizes that cap for the answer alone — 60 tokens for
 * a one-line insight, 300 for a few of the coach's sentences — so a default dynamic budget spends
 * the whole allowance reasoning and the response comes back finished for `MAX_TOKENS` with nothing
 * in it. That is not a soft failure: `APIController`'s `validate()` throws `ResponseStoppedException`
 * on *any* finish reason other than `STOP`, so a half-written answer is discarded exactly like an
 * empty one, and every call site's `catch` turns it into the graceful fallback. The coach hit this
 * first because it is the longest answer; the insight was closest to hitting it next.
 *
 * `MINIMAL` rather than a zero budget: `thinkingBudget = 0` is the Gemini 2.5 idiom for switching
 * thinking off, and the 3.x models take a [ThinkingLevel] instead, where `MINIMAL` is the floor.
 * Nothing this app asks for needs reasoning — one line of encouragement, a sentence of coaching,
 * a photo turned into twelve flat JSON fields — so the cheapest, fastest setting is also the right
 * one. Raise it per call site if a task ever genuinely reasons; raise `maxOutputTokens` with it.
 */
internal val AI_THINKING: ThinkingConfig = thinkingConfig { thinkingLevel = ThinkingLevel.MINIMAL }

/**
 * How long one request may take before it is a failure. The SDK's own default is 180 s, which
 * outlasts the patience of every fallback in this app: a photo sat on "Analyzing" for three
 * minutes on a dead connection before manual entry was offered. A healthy answer on a flash-lite
 * model is seconds, and the coach's rounds are separate requests, so a minute is headroom.
 */
private const val AI_TIMEOUT_MILLIS = 60_000L

/**
 * The one way this app builds a model. Every call site passes only what is genuinely its own —
 * the output cap, the thinking level, the schema, the coach's tools — and everything that is a
 * property of *the app's* AI setup is decided here once: the backend, [AI_MODEL_NAME], the
 * timeout, and the App Check mode.
 *
 * **No limited-use App Check tokens.** Each limited-use token is a fresh Play Integrity
 * attestation — added latency on every request and a draw on its daily quota, past which App Check
 * fails and every AI feature goes quiet at once — and all it buys is replay protection, which is
 * not enforced for this project in the Firebase console. The two go back on together or not at all.
 */
internal fun aiModel(
    generationConfig: GenerationConfig,
    tools: List<Tool>? = null,
    systemInstruction: Content? = null,
): GenerativeModel = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
    modelName = AI_MODEL_NAME,
    generationConfig = generationConfig,
    tools = tools,
    systemInstruction = systemInstruction,
    requestOptions = RequestOptions(timeoutInMillis = AI_TIMEOUT_MILLIS),
)

/** How long a retry waits: long enough for a 503's "try again" to mean something, short enough that
 * the screen is still on *Analyzing* rather than on its fallback. */
private const val AI_RETRY_DELAY_MILLIS = 1_000L

/**
 * One request, the way every one-shot call site makes it: retried **once** on a [ServerException] —
 * a 5xx, which is what "the model is overloaded" arrives as — and its usage logged.
 *
 * Nothing else earns the retry. A timeout would double a minute's wait, a [QuotaExceededException]
 * does not clear in a second, and a `MAX_TOKENS` stop fails the same way twice; each of those goes
 * straight to the caller's fallback as before. The coach is not routed through here — it streams,
 * and its failure bubble already carries a Retry the user can see.
 *
 * [QuotaExceededException]: com.google.firebase.ai.type.QuotaExceededException
 */
internal suspend fun GenerativeModel.generate(where: String, prompt: Content): GenerateContentResponse {
    val response = try {
        generateContent(prompt)
    } catch (e: ServerException) {
        logAiFailure("$where, retrying", e)
        delay(AI_RETRY_DELAY_MILLIS)
        generateContent(prompt)
    }
    logAiUsage(where, response.usageMetadata)
    return response
}

private const val TAG = "FitPulseAI"

/**
 * Every AI call swallows its exception — offline, throttled, App Check refused and model-retired
 * all mean the same thing to a screen with a fallback. This is the one place that says so out
 * loud, so the difference is visible in logcat instead of only in a missing card.
 */
internal fun logAiFailure(where: String, cause: Throwable) {
    Log.w(TAG, "$where failed: ${cause.javaClass.simpleName}: ${cause.message}", cause)
}

/**
 * What a call actually spent, beside what it cost when it failed. `cached` is Gemini's implicit
 * cache — the coach's prompt is shaped for it, and this line is the only place a hit shows up;
 * `thoughts` against the site's `maxOutputTokens` is how close it runs to the `MAX_TOKENS` trap
 * [AI_THINKING] describes.
 */
internal fun logAiUsage(where: String, usage: UsageMetadata?) {
    usage ?: return
    Log.d(
        TAG,
        "$where: in=${usage.promptTokenCount} cached=${usage.cachedContentTokenCount} " +
            "thoughts=${usage.thoughtsTokenCount} out=${usage.candidatesTokenCount}",
    )
}
