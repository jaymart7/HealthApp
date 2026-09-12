package ph.mart.healthapp.core.data

import android.util.Log
import ph.mart.healthapp.core.data.coach.CoachRepository
import ph.mart.healthapp.core.data.coach.CoachToolbox
import ph.mart.healthapp.core.data.fake.FakeCoachRepository
import ph.mart.healthapp.core.data.fake.FakeInsightRepository
import ph.mart.healthapp.core.data.fake.FakeMealIdeaRepository
import ph.mart.healthapp.core.data.fake.FakeMealParseRepository
import ph.mart.healthapp.core.data.fake.FakeRecognitionRepository
import ph.mart.healthapp.core.data.food.FoodRecognitionRepository
import ph.mart.healthapp.core.data.food.MealIdeaRepository
import ph.mart.healthapp.core.data.food.MealParseRepository
import ph.mart.healthapp.core.data.insight.InsightRepository

/**
 * **Flip this to hit the real models.** Left `false`, a debug build never calls Firebase AI Logic
 * at all and therefore bills nothing.
 *
 * All five call sites at once rather than five switches, because the reason to flip is always the
 * same — *checking a change against the real thing before a release* — and five booleans is five
 * ways to leave one on by accident. Splitting it per feature is one line if that ever stops being
 * true.
 *
 * A debug-only escape hatch in the shape `FORCE_ONBOARDING` already uses in `AppRoot.kt`, except
 * that this one lives in a source set rather than behind `BuildConfig.DEBUG`: five fake
 * repositories are more than a boolean's worth of code to keep out of a release build by
 * convention, and the release twin of this file keeps them out by construction.
 */
private const val USE_REAL_AI = true

/**
 * The five fakes exist so a debug build can be *iterated on*, not merely run cheaply.
 *
 * A stub returning null everywhere would cost the same and hide the things this app's AI surface
 * actually gets wrong — a streamed answer that scrolls badly, a proposal card with an absurd
 * figure in it, a parse that returns nothing. So each fake answers from **data that already
 * exists locally**: the coach's own `CoachToolbox` reads the real Room rows, and the other four
 * reuse the fallbacks the app already ships for offline. What is faked is the model, and nothing
 * else — the tool loop, the trust boundaries, the sanitizers and every Room write are the real
 * code either way.
 */
internal fun debugCoach(real: CoachRepository, toolbox: CoachToolbox): CoachRepository? =
    ifFaking { FakeCoachRepository(real, toolbox) }

internal fun debugInsight(): InsightRepository? = ifFaking { FakeInsightRepository() }

internal fun debugRecognition(): FoodRecognitionRepository? = ifFaking { FakeRecognitionRepository() }

internal fun debugMealIdeas(): MealIdeaRepository? = ifFaking { FakeMealIdeaRepository() }

internal fun debugMealParse(): MealParseRepository? = ifFaking { FakeMealParseRepository() }

/**
 * Says out loud which mode the build is in, once, under the tag `logAiFailure` already uses.
 *
 * Worth the four lines: the failure mode this whole change exists to prevent is a build quietly
 * spending money, and "quietly" is the operative word — every AI call site in this app swallows
 * its exception and degrades gracefully, so a real call and a faked one look identical on screen.
 * `logcat -s FitPulseAI` is then the answer to "is this build costing me anything?".
 */
private fun <T> ifFaking(fake: () -> T): T? {
    if (!announced) {
        announced = true
        Log.i(
            "FitPulseAI",
            if (USE_REAL_AI) "USE_REAL_AI is on — this debug build calls Gemini and bills for it."
            else "AI is faked in this debug build. No Firebase AI Logic calls will be made.",
        )
    }
    return if (USE_REAL_AI) null else fake()
}

private var announced = false
