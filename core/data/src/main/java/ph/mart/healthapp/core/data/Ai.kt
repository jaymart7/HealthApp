package ph.mart.healthapp.core.data

import android.util.Log
import com.google.firebase.ai.type.ThinkingConfig
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.thinkingConfig

/**
 * The one model name for every Firebase AI Logic call in the app.
 *
 * It lives here rather than five times over because a Gemini model is a wasting asset: Google
 * retires them on a published schedule and a retired name is a 404, not a deprecation warning.
 * The whole AI surface degrades gracefully, so a stale name here reads as "the feature is quiet"
 * at every call site — which is exactly how `gemini-1.5-flash` outlived its shutdown in this
 * codebase. One constant is one line to change when the next date lands.
 */
internal const val AI_MODEL_NAME = "gemini-3.8-flash"

/**
 * The one thinking setting for every Firebase AI Logic call in the app, here for the same reason
 * [AI_MODEL_NAME] is: it is a property of the model, so five copies would go stale together.
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

private const val TAG = "FitPulseAI"

/**
 * Every AI call swallows its exception — offline, throttled, App Check refused and model-retired
 * all mean the same thing to a screen with a fallback. This is the one place that says so out
 * loud, so the difference is visible in logcat instead of only in a missing card.
 */
internal fun logAiFailure(where: String, cause: Throwable) {
    Log.w(TAG, "$where failed: ${cause.javaClass.simpleName}: ${cause.message}", cause)
}
