package ph.mart.healthapp.core.data

import android.util.Log

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

private const val TAG = "FitPulseAI"

/**
 * Every AI call swallows its exception — offline, throttled, App Check refused and model-retired
 * all mean the same thing to a screen with a fallback. This is the one place that says so out
 * loud, so the difference is visible in logcat instead of only in a missing card.
 */
internal fun logAiFailure(where: String, cause: Throwable) {
    Log.w(TAG, "$where failed: ${cause.javaClass.simpleName}: ${cause.message}", cause)
}
