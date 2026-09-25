package ph.mart.healthapp.core.data.fake

import android.graphics.Bitmap
import kotlinx.coroutines.delay
import ph.mart.healthapp.core.data.exercise.ExerciseParseRepository
import ph.mart.healthapp.core.data.exercise.ExerciseParseResult
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.ParsedExercise
import ph.mart.healthapp.core.data.exercise.ParsedLiftRow
import ph.mart.healthapp.core.data.exercise.StrengthParseResult
import ph.mart.healthapp.core.data.exercise.StrengthSet
import ph.mart.healthapp.core.data.exercise.parsedSets
import ph.mart.healthapp.core.data.food.COMMON_FOODS
import ph.mart.healthapp.core.data.food.FoodRecognitionRepository
import ph.mart.healthapp.core.data.food.LabelBasis
import ph.mart.healthapp.core.data.food.LabelReading
import ph.mart.healthapp.core.data.food.LabelScanRepository
import ph.mart.healthapp.core.data.food.LabelScanResult
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
import ph.mart.healthapp.core.data.food.offlineActivity
import ph.mart.healthapp.core.data.food.offlineFoods
import ph.mart.healthapp.core.data.food.offlineQuickLog
import ph.mart.healthapp.core.data.food.quickLogResult
import ph.mart.healthapp.core.data.food.toRecognized
import ph.mart.healthapp.core.data.food.Nutrients
import ph.mart.healthapp.core.data.food.QuickLogRepository
import ph.mart.healthapp.core.data.food.QuickLogResult
import ph.mart.healthapp.core.data.food.QuickLogTurn
import ph.mart.healthapp.core.data.food.PARSE_KIND_FOOD
import ph.mart.healthapp.core.data.food.RecipeParseRepository
import ph.mart.healthapp.core.data.food.RecipeParseResult
import ph.mart.healthapp.core.data.food.mayAsk
import ph.mart.healthapp.core.data.food.recipeParseResult
import ph.mart.healthapp.core.data.insight.InsightRepository
import ph.mart.healthapp.core.data.insight.InsightRequest
import ph.mart.healthapp.core.data.insight.insightFor
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.supplement.SupplementLabelReading
import ph.mart.healthapp.core.data.supplement.SupplementScanRepository
import ph.mart.healthapp.core.data.supplement.SupplementScanResult

/**
 * The nine smaller AI features, faked off local data. The coach is next door in
 * [FakeCoachRepository], because it is the only one with a state machine worth faking carefully.
 *
 * Every one of these reuses something the app already ships, and that is the design rather than an
 * economy: a fake built from its own fixtures drifts from the real thing and stops being useful
 * the first time the real shape changes. `COMMON_FOODS` is a hand-written table of real USDA
 * figures already in the APK, and `insightFor` is the rule-based line Home already falls back to.
 *
 * All seven keep a short [delay]: a call that returns instantly hides every spinner, and the point
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
        val foods = fakePlate(photo)
        return if (foods.isEmpty()) RecognitionResult.NoFoodDetected else RecognitionResult.Success(foods)
    }
}

/** One to three foods picked by the bitmap's own dimensions, and one photo in seven with none —
 * shared by the camera flow's fake and the quick log's, which reads the same plate. */
internal fun fakePlate(photo: Bitmap): List<RecognizedFood> {
    val seed = photo.width * 31 + photo.height
    if (seed % 7 == 0) return emptyList()
    return (0 until seed.mod(3) + 1).map { offset ->
        COMMON_FOODS[(seed + offset).mod(COMMON_FOODS.size)].toRecognized()
    }
}

/**
 * A nutrition panel the camera "read", picked from [COMMON_FOODS] by the bitmap's own dimensions —
 * [FakeRecognitionRepository]'s trick, and for its reasons: deterministic per photo so a review
 * screen can be looked at twice, varied across photos so it isn't always chicken breast.
 *
 * `COMMON_FOODS` is the right table to fake this off precisely because its rows carry all seven
 * nutrients at real USDA figures and are already per 100 g — which is what a panel read reaches the
 * form as. So the confirmation screen, the panel readout and the portion repricing all see the
 * shape they will see in production.
 *
 * Every fifth distinct photo answers [LabelScanResult.NoLabelFound], so the branch that says
 * "point it at the panel" is reachable without finding an unlabelled packet.
 */
internal class FakeLabelScanRepository : LabelScanRepository {
    override suspend fun read(photo: Bitmap): LabelScanResult {
        delay(FAKE_LATENCY_MS)
        val seed = photo.width * 31 + photo.height
        if (seed % 5 == 0) return LabelScanResult.NoLabelFound
        val product = COMMON_FOODS[seed.mod(COMMON_FOODS.size)]
        return LabelScanResult.Found(
            LabelReading(
                name = product.name,
                // Not from the table: `COMMON_FOODS` leaves servingSize null on purpose, because a
                // serving label there would be app copy. A packet declares one, so the fake does
                // too — otherwise the portion control's third preset chip is never seen.
                servingSize = "1 serving (30 g)",
                basis = LabelBasis.Per100g,
                calories = product.calories,
                proteinG = product.proteinG,
                carbsG = product.carbsG,
                fatG = product.fatG,
                nutrients = product.nutrients,
            ),
        )
    }
}


/**
 * A multivitamin, which is the case worth looking at: four of its lines are figures this app
 * grades and the rest are text, so the confirmation's readout and the day's nutrient panel can
 * both be checked against a reading that exercises both halves.
 *
 * Hand-written rather than read from something the app ships, unlike the other six — there is no
 * local table of supplement formulas to read, and inventing one for the fake alone would be a
 * fixture pretending to be data. The photo seeds a [SupplementScanResult.NoLabelFound] every fifth
 * shot, the way [FakeLabelScanRepository] does, so the dead end is reachable too — and so does a
 * typed name, on its own rule below.
 */
internal class FakeSupplementScanRepository : SupplementScanRepository {
    override suspend fun read(photo: Bitmap): SupplementScanResult {
        delay(FAKE_LATENCY_MS)
        if ((photo.width * 31 + photo.height) % 5 == 0) return SupplementScanResult.NoLabelFound
        return SupplementScanResult.Found(multivitamin())
    }

    /**
     * The same bottle, named back with whatever was typed — which is what the real call does when
     * it recognises a product, and what makes the seeded sheet obviously a response to the name
     * rather than a fixture. A name whose length is a multiple of five answers
     * [SupplementScanResult.NoLabelFound], so "I don't know that one" is reachable without waiting
     * for the real model to not know something.
     */
    override suspend fun lookUp(name: String): SupplementScanResult {
        delay(FAKE_LATENCY_MS)
        val typed = name.trim()
        if (typed.isEmpty() || typed.length % 5 == 0) return SupplementScanResult.NoLabelFound
        return SupplementScanResult.Found(multivitamin().copy(name = typed))
    }

    private fun multivitamin() = SupplementLabelReading(
        name = "Daily Multivitamin",
        dose = "2 tablets",
        timesPerDay = 1,
        nutrients = Nutrients(vitaminDUg = 25, calciumMg = 210, ironUg = 18_000, potassiumMg = 80),
        panel = listOf(
            "Vitamin D 25 µg",
            "Calcium 210 mg",
            "Iron 18 mg",
            "Potassium 80 mg",
            "Vitamin A 900 µg",
            "Vitamin C 90 mg",
            "Vitamin E 15 mg",
            "Vitamin B12 2.4 µg",
            "Zinc 11 mg",
            "Magnesium 100 mg",
        ).joinToString("\n"),
    )
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

/** The offline matcher's food half, which is what this fake always was. */
internal fun fakeParse(text: String): List<RecognizedFood> = offlineFoods(text)

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

    override suspend fun parseSets(text: String, unit: UnitSystem): StrengthParseResult {
        delay(FAKE_LATENCY_MS)
        val sets = fakeStrengthParse(text, unit)
        return if (sets.isEmpty()) StrengthParseResult.NoLiftsFound else StrengthParseResult.Success(sets)
    }
}

/** "bench 3x8 at 60 kg" — a name, sets × reps, then an optional load and unit. */
private val FAKE_LIFT = Regex("""(?i)^\s*(.*?)\s+(\d+)\s*x\s*(\d+)(?:\s*(?:at|@)?\s*(\d+(?:\.\d+)?)\s*(kg|lb)?)?""")

/**
 * One lift per comma or "and", read by [FAKE_LIFT] and handed to [parsedSets] — the real parse's
 * last line, so the caps and the unit fallback run here rather than being bypassed.
 */
internal fun fakeStrengthParse(text: String, unit: UnitSystem): List<StrengthSet> =
    parsedSets(
        text.split(',', '\n').flatMap { it.split(" and ") }.mapNotNull { chunk ->
            FAKE_LIFT.find(chunk)?.destructured?.let { (lift, sets, reps, weight, liftUnit) ->
                ParsedLiftRow(lift, sets.toIntOrNull(), reps.toIntOrNull(), weight.toDoubleOrNull(), liftUnit)
            }
        },
        unit,
    )

/** The offline matcher's activity half, with half an hour standing in for the model's "shortest
 * plausible duration" when none was said. */
internal fun fakeExerciseParse(text: String): ParsedExercise? = offlineActivity(text, defaultMinutes = FAKE_MINUTES)

private const val FAKE_MINUTES = 30

internal class FakeRecipeParseRepository : RecipeParseRepository {
    override suspend fun parse(text: String): RecipeParseResult {
        delay(FAKE_LATENCY_MS)
        return fakeRecipeParse(text)
    }
}

private val SERVINGS_SAID = Regex("""(?:for|serves|makes)\s+(\d+)""", RegexOption.IGNORE_CASE)

/**
 * [fakeParse] over the whole text for the ingredients, the words before the first break for the
 * name, and "for 4" / "serves 4" for the servings — so "Chili for 4, beef, beans" really comes back
 * as Chili, four portions, beef and beans. It ends on [recipeParseResult] so the caps and the clamp
 * are exercised rather than bypassed. A dish named with no listed ingredient the table knows finds
 * nothing, which is this fake's way of reaching that branch.
 *
 * One known food and no yield said is a food, so "banana" saves as a food and "Chili for 4, beef"
 * as a recipe — both halves of the library's review are reachable without a model.
 */
internal fun fakeRecipeParse(text: String): RecipeParseResult {
    val ingredients = fakeParse(text)
    val servingsSaid = SERVINGS_SAID.find(text)?.groupValues?.get(1)?.toIntOrNull()
    return recipeParseResult(
        name = text.split(',', ':', '\n').first().split(Regex("""\s+for\s+""", RegexOption.IGNORE_CASE)).first(),
        servings = servingsSaid ?: 1,
        ingredients = ingredients,
        kind = if (ingredients.size == 1 && servingsSaid == null) PARSE_KIND_FOOD else "recipe",
    )
}

internal class FakeQuickLogRepository : QuickLogRepository {
    override suspend fun parse(turns: List<QuickLogTurn>, photo: Bitmap?): QuickLogResult {
        delay(FAKE_LATENCY_MS)
        if (photo == null) return fakeQuickLog(turns)
        // The plate the camera fake would have seen, plus whatever the words add; a photo is never
        // met with "what did you eat?", which is the question it just answered.
        val words = offlineQuickLog(turns.filter { it.fromUser }.joinToString(" ") { it.text })
            as? QuickLogResult.Parsed
        return quickLogResult(
            question = null,
            foods = (fakePlate(photo) + words?.foods.orEmpty()).distinctBy { it.name },
            activities = words?.activities.orEmpty(),
            mayAsk = false,
            mealType = words?.mealType?.name,
            waterGlasses = words?.waterGlasses,
            weight = words?.weight,
        )
    }
}

/**
 * [offlineQuickLog] over everything the user has said, so an answer simply extends the sentence,
 * plus the model's two usual questions: how long, when an activity came with no duration, and a
 * generic one when nothing matched at all — so the follow-up and its cap can both be walked through
 * in a debug build. Past the cap it answers what the offline parse found, which is the model's
 * "estimate and flag it" with nothing to estimate from.
 */
internal fun fakeQuickLog(turns: List<QuickLogTurn>): QuickLogResult {
    val said = turns.filter { it.fromUser }.joinToString(" ") { it.text }
    val found = offlineQuickLog(said)
    val question = when {
        offlineActivity(said, defaultMinutes = null) == null && fakeExerciseParse(said) != null ->
            "How long did it last?"
        found == QuickLogResult.NothingFound -> "What did you eat, or what did you do?"
        else -> null
    }
    return if (question != null && turns.mayAsk()) QuickLogResult.Question(question) else found
}

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
