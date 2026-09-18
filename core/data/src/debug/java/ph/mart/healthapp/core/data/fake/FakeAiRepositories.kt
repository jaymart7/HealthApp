package ph.mart.healthapp.core.data.fake

import android.graphics.Bitmap
import kotlinx.coroutines.delay
import ph.mart.healthapp.core.data.exercise.ExerciseParseRepository
import ph.mart.healthapp.core.data.exercise.ExerciseParseResult
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.ParsedExercise
import ph.mart.healthapp.core.data.exercise.parsedExercise
import ph.mart.healthapp.core.data.food.COMMON_FOODS
import ph.mart.healthapp.core.data.food.FoodRecognitionRepository
import ph.mart.healthapp.core.data.food.MAX_MEAL_IDEAS
import ph.mart.healthapp.core.data.food.MealIdea
import ph.mart.healthapp.core.data.food.MealIdeaRepository
import ph.mart.healthapp.core.data.food.MealIdeaRequest
import ph.mart.healthapp.core.data.food.MealIdeaResult
import ph.mart.healthapp.core.data.food.MealParseRepository
import ph.mart.healthapp.core.data.food.MealParseResult
import ph.mart.healthapp.core.data.food.RecognitionConfidence
import ph.mart.healthapp.core.data.food.RecognitionResult
import ph.mart.healthapp.core.data.food.RecognizedFood
import ph.mart.healthapp.core.data.food.ScannedProduct
import ph.mart.healthapp.core.data.food.loggable
import ph.mart.healthapp.core.data.food.searchCommonFoods
import ph.mart.healthapp.core.data.insight.InsightRepository
import ph.mart.healthapp.core.data.insight.InsightRequest
import ph.mart.healthapp.core.data.insight.insightFor

/**
 * The five smaller AI features, faked off local data. The coach is next door in
 * [FakeCoachRepository], because it is the only one with a state machine worth faking carefully.
 *
 * Every one of these reuses something the app already ships, and that is the design rather than an
 * economy: a fake built from its own fixtures drifts from the real thing and stops being useful
 * the first time the real shape changes. `COMMON_FOODS` is a hand-written table of real USDA
 * figures already in the APK, and `insightFor` is the rule-based line Home already falls back to.
 *
 * All five keep a short [delay]: a call that returns instantly hides every spinner, and the point
 * of a debug build is to look at them.
 */

/** Roughly what a model round trip feels like, so a loading state is actually visible. */
private const val FAKE_LATENCY_MS = 700L

/**
 * The rule-based line, which is exactly what Home falls back to when the model says nothing.
 *
 * So the insight card looks identical to a real day on which the model had no opinion — which is
 * honest, and is why there is nothing more to write here. Null when the day holds nothing worth
 * remarking on, the same first-class answer the interface already documents.
 */
internal class FakeInsightRepository : InsightRepository {
    override suspend fun dailyInsight(request: InsightRequest, todayEpochDay: Long): String? {
        delay(FAKE_LATENCY_MS)
        return insightFor(request)
    }
}

/**
 * A plate the camera "recognised", picked from [COMMON_FOODS] by the bitmap's own dimensions.
 *
 * Deterministic on purpose — the same photo gives the same answer, so a confirmation screen can be
 * looked at twice — but varied across photos, so it isn't always chicken breast. Confidence is
 * always [RecognitionConfidence.Low], because that is the branch worth seeing: it is the one that
 * puts a warning in front of the user, and a fake that always returns High would leave it untested.
 *
 * **One to three foods**, off the same seed. A real plate is several foods and the confirmation
 * screen is a list because of it; a fake that always answered with one would leave the multi-row
 * review — and the single-row case that auto-expands — reachable only against the real model.
 *
 * Every seventh distinct photo answers [RecognitionResult.NoFoodDetected] so that branch is
 * reachable without pointing the camera at a wall.
 */
internal class FakeRecognitionRepository : FoodRecognitionRepository {
    override suspend fun recognize(photo: Bitmap): RecognitionResult {
        delay(FAKE_LATENCY_MS)
        val seed = photo.width * 31 + photo.height
        if (seed % 7 == 0) return RecognitionResult.NoFoodDetected
        val count = seed.mod(3) + 1
        val foods = (0 until count).map { offset ->
            COMMON_FOODS[(seed + offset).mod(COMMON_FOODS.size)].toRecognized()
        }
        return RecognitionResult.Success(foods)
    }
}

/**
 * `localMealIdeas`' rule — fits the budget, most protein first, at most [MAX_MEAL_IDEAS] — applied
 * to [COMMON_FOODS] rather than to the user's own foods.
 *
 * Not a call to `localMealIdeas` itself, which takes the suggestions and recipes the *screen* has
 * loaded and this repository has never seen. Reimplementing the three lines here beats plumbing
 * two lists through an interface that exists to take a budget and nothing else.
 *
 * **It ignores [MealIdeaRequest.diet]**: `COMMON_FOODS` carries no dietary tags, so a fake cannot
 * honour the one field that separates an idea from an insult. Test the vegan path against the real
 * model.
 */
internal class FakeMealIdeaRepository : MealIdeaRepository {
    override suspend fun ideas(request: MealIdeaRequest): MealIdeaResult {
        delay(FAKE_LATENCY_MS)
        val fitting = COMMON_FOODS
            .filter { it.calories in 1..request.remainingKcal.coerceAtLeast(0) }
            .sortedByDescending { it.proteinG }
            .take(MAX_MEAL_IDEAS)
        // An empty list is a real answer on a day already over target, and the screen has a state
        // for it — so it is returned as Success, not dressed up as a failure.
        return MealIdeaResult.Success(fitting.map { it.toIdea() })
    }
}

/**
 * A genuinely useful local parse: every word of the sentence is looked up in [COMMON_FOODS], and
 * whatever matches is what was "heard".
 *
 * "Two eggs and some rice" really does come back as egg and rice, which makes this the one fake
 * that is arguably better than nothing for testing — the review screen gets plausible rows with
 * real macros. It ends on the same `loggable()` filter the real parse does, so the cap and the
 * blank-name rule are exercised rather than bypassed.
 */
internal class FakeMealParseRepository : MealParseRepository {
    override suspend fun parse(text: String): MealParseResult {
        delay(FAKE_LATENCY_MS)
        val found = fakeParse(text)
        return if (found.isEmpty()) MealParseResult.NoFoodFound else MealParseResult.Success(found)
    }
}

/** Pulled out of the class so the routing is a pure function a JVM test can reach — the reason
 * `sanitizeReply` and `parseAction` are ones. */
internal fun fakeParse(text: String): List<RecognizedFood> = text
    .split(' ', ',', '.', '\n')
    .mapNotNull { commonFoodFor(it) }
    .distinctBy { it.name }
    .map { it.toRecognized() }
    .loggable()

/**
 * One word against the built-in table, **singularised on a miss.**
 *
 * `searchCommonFoods` asks whether the food's *name* contains the query, and every row in
 * `COMMON_FOODS` is singular — "Egg, whole, boiled", "Banana". People say "eggs" and "two bananas",
 * so a literal match finds nothing for the most common sentence either fake will ever see. Dropping
 * a trailing "s" on the second pass is the whole fix; it is wrong for "hummus" and "couscous",
 * which is why it is only ever a *fallback* after the exact match has already failed.
 *
 * Shared by the voice parse and the coach's proposal routing because they are the same trick, and
 * a second copy would be a second place for the plural bug to come back.
 */
internal fun commonFoodFor(word: String): ScannedProduct? {
    val term = word.trim().lowercase()
    if (term.length < MIN_MATCH_CHARS) return null
    searchCommonFoods(term).firstOrNull()?.let { return it }
    val singular = term.removeSuffix("es").takeIf { it.length >= MIN_MATCH_CHARS }
        ?: term.removeSuffix("s")
    return searchCommonFoods(term.removeSuffix("s")).firstOrNull()
        ?: searchCommonFoods(singular).firstOrNull()
}

/**
 * The sentence against [ExerciseType]'s own names, and the first number in it as the duration.
 *
 * Crude on purpose and still genuinely useful: "45 minute run" really does come back as Run at 45
 * minutes, which is the case the sheet's whole seeding path turns on. A sentence naming nothing it
 * recognises answers [ExerciseParseResult.NoActivityFound] — the branch worth being able to see
 * without pointing a real model at a shopping list.
 */
internal class FakeExerciseParseRepository : ExerciseParseRepository {
    override suspend fun parse(text: String): ExerciseParseResult {
        delay(FAKE_LATENCY_MS)
        val activity = fakeExerciseParse(text)
        return if (activity == null) {
            ExerciseParseResult.NoActivityFound
        } else {
            ExerciseParseResult.Success(activity)
        }
    }
}

/**
 * The words people actually use for four of the eight types. The enum's own names cover the rest
 * ("yoga", "swim", "walk"), so this is only the gap between what a type is called and what it is
 * said as — and [ExerciseType.Other] is deliberately absent, because nobody says "other".
 */
private val FAKE_SYNONYMS = mapOf(
    "jog" to ExerciseType.Run,
    "ran" to ExerciseType.Run,
    "bike" to ExerciseType.Cycle,
    "cycling" to ExerciseType.Cycle,
    "gym" to ExerciseType.Strength,
    "lift" to ExerciseType.Strength,
    "weights" to ExerciseType.Strength,
)

private val FIRST_NUMBER = Regex("""\d+""")

/**
 * Pulled out of the class so the routing is a pure function a JVM test can reach — [fakeParse]'s
 * reason, and the same reason `parsedExercise` is the last line of it: the caps and the
 * blank-name rule are exercised here rather than bypassed.
 *
 * The whole sentence becomes the note, which is what the real parse is asked for too — a short
 * phrase in the user's own words.
 */
internal fun fakeExerciseParse(text: String): ParsedExercise? {
    val words = text.lowercase().split(' ', ',', '.', '\n')
    val type = words.firstNotNullOfOrNull { word ->
        ExerciseType.entries.firstOrNull { it.name.lowercase() == word } ?: FAKE_SYNONYMS[word]
    } ?: return null
    // No number said is not no workout: the real model is asked to estimate the shortest
    // plausible duration, and half an hour is this fake's version of that.
    val minutes = FIRST_NUMBER.find(text)?.value?.toIntOrNull() ?: 30
    return parsedExercise(type = type.name, name = text.trim(), minutes = minutes)
}

/** Below this a "word" matches half the table — "an" is in "banana", "pan" and "pancake". */
private const val MIN_MATCH_CHARS = 3

private fun ScannedProduct.toRecognized() = RecognizedFood(
    name = name,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    nutrients = nutrients,
    confidence = RecognitionConfidence.Low,
)

private fun ScannedProduct.toIdea() = MealIdea(
    name = name,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    nutrients = nutrients,
)
