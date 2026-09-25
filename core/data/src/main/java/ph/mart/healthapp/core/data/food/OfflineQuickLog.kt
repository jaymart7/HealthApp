package ph.mart.healthapp.core.data.food

import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.ParsedExercise
import ph.mart.healthapp.core.data.exercise.parsedExercise

/**
 * The quick log with no network: the sentence matched word by word against the user's own foods
 * and the built-in table, an activity read off its type and a stated duration, and water and a
 * weigh-in read off the two phrases people actually say. The offline-first rule, applied to the
 * one AI surface that can still do something useful without the model.
 *
 * It began as the debug fakes' matcher and still is — `FakeMealParseRepository`,
 * `FakeExerciseParseRepository` and `FakeQuickLogRepository` all call it, so there is one copy of
 * the plural rule and one of the word-start rule, and a debug build walks the same path an offline
 * phone does.
 *
 * **Everything it finds is a guess.** Every food comes back [RecognitionConfidence.Low], so the
 * review tags each row; it never asks a follow-up, because a question with no model behind it would
 * be a form pretending to be a conversation; and an activity with no duration said is no activity,
 * because offline there is nothing to estimate one from.
 */
fun offlineQuickLog(text: String, myFoods: List<ScannedProduct> = emptyList()): QuickLogResult {
    val glasses = waterGlassesIn(text)
    val weight = BODY_WEIGHT.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
    // Read, then taken out, so "3 glasses of water" is not also three foods and "weighed 80" is not
    // eighty minutes of anything.
    val rest = text.replace(WATER_GLASSES, " ").replace(BODY_WEIGHT, " ")
    return quickLogResult(
        question = null,
        foods = offlineFoods(rest, myFoods),
        activities = listOf(offlineActivity(rest, defaultMinutes = null)),
        mayAsk = false,
        mealType = mealSlotIn(text)?.name,
        waterGlasses = glasses,
        weight = weight,
    )
}

/**
 * Every word against the user's own foods first — a custom row is the one they mean — then the
 * built-in table. Distinct by name, capped and judged by [loggable] like every other parse.
 */
internal fun offlineFoods(text: String, myFoods: List<ScannedProduct> = emptyList()): List<RecognizedFood> =
    matchWords(text)
        .mapNotNull { word -> myFoods.firstOrNull { it.namedBy(word) } ?: commonFoodFor(word) }
        .distinctBy { it.name }
        .map { it.toRecognized() }
        .loggable()

/** The words of [text] worth matching a food against — lowercased, long enough not to match half
 * the table, and not water. One copy, because [namedIn] must pick the saved foods this path would. */
internal fun matchWords(text: String): List<String> = text
    .split(' ', ',', '.', '\n')
    .map { it.trim().lowercase() }
    .filter { it.length >= MIN_MATCH_CHARS && it !in NOT_FOOD }

/**
 * One word against the built-in table, **singularised on a miss**, and only where a word of the
 * food's name *starts* with it.
 *
 * `searchCommonFoods` asks whether the name contains the query, and every row in `COMMON_FOODS`
 * is singular — "Egg, whole, boiled", "Banana". People say "eggs" and "two bananas", so a literal
 * match finds nothing for the most common sentence there is. Dropping a trailing "s" on the second
 * pass is the whole fix; it is wrong for "hummus" and "couscous", which is why it is only ever a
 * fallback after the exact match has already failed.
 *
 * The word-start rule is the coach fake's, lifted here once this became a real path: "contains"
 * alone reads "ran" as *Orange* and "and" as *Sandwich*.
 */
internal fun commonFoodFor(word: String): ScannedProduct? {
    val term = word.trim().lowercase()
    if (term.length < MIN_MATCH_CHARS) return null
    val singular = term.removeSuffix("es").takeIf { it.length >= MIN_MATCH_CHARS } ?: term.removeSuffix("s")
    return listOf(term, term.removeSuffix("s"), singular).distinct().firstNotNullOfOrNull { stem ->
        searchCommonFoods(stem).firstOrNull { it.namesWord(stem) }
    }
}

/** A saved food a said word could mean, plural or not — "bars" is still the *Protein bar*. */
internal fun ScannedProduct.namedBy(word: String): Boolean = namesWord(word) || namesWord(word.removeSuffix("s"))

/** Whether a word of the name starts with [stem] — "egg" names *Egg white*, "ran" names nothing. */
internal fun ScannedProduct.namesWord(stem: String): Boolean =
    stem.isNotEmpty() && name.split(' ', ',', '(', '-').any { it.startsWith(stem, ignoreCase = true) }

/**
 * The sentence against [ExerciseType]'s own names plus the words people use for them, and a
 * duration: minutes or hours said, else the first bare number left in the sentence.
 *
 * [defaultMinutes] is what a sentence with no number gets. Offline passes null — no duration said is
 * no activity, because nothing is there to estimate one; the debug fakes pass half an hour, their
 * stand-in for the model's "shortest plausible duration".
 */
internal fun offlineActivity(text: String, defaultMinutes: Int?): ParsedExercise? {
    val words = text.lowercase().split(' ', ',', '.', '\n')
    val type = words.firstNotNullOfOrNull { word ->
        ExerciseType.entries.firstOrNull { it.name.lowercase() == word } ?: ACTIVITY_WORDS[word]
    } ?: return null
    val minutes = MINUTES.find(text)?.groupValues?.get(1)?.toIntOrNull()
        ?: HOURS.find(text)?.groupValues?.get(1)?.toIntOrNull()?.times(MINUTES_PER_HOUR)
        ?: BARE_NUMBER.find(text)?.value?.toIntOrNull()
        ?: defaultMinutes
    // The whole sentence is the note — what the real parse is asked for too, their own words.
    return parsedExercise(type = type.name, name = text.trim(), minutes = minutes)
}

/** "3 glasses", "a glass of water". A number past a word ("two") is past this; a digit is not. */
internal fun waterGlassesIn(text: String): Int? {
    val said = WATER_GLASSES.find(text)?.groupValues?.get(1)?.lowercase() ?: return null
    return said.toIntOrNull() ?: 1
}

/** The meal named, if one was — "snack" for Snacks, the one slot whose name is not the word. */
internal fun mealSlotIn(text: String): MealType? =
    MealType.entries.firstOrNull { text.contains(it.name.removeSuffix("s"), ignoreCase = true) }

/** Below this a "word" matches half the table. */
private const val MIN_MATCH_CHARS = 3

private const val MINUTES_PER_HOUR = 60

/** Plain water is water, not a food — the real prompt says the same. "Tuna, canned in water" is
 * what a word match makes of it otherwise. */
private val NOT_FOOD = setOf("water", "glass", "glasses")

/**
 * The words people use for four of the eight types. The enum's own names cover the rest ("yoga",
 * "swim", "walk"), and [ExerciseType.Other] is deliberately absent, because nobody says "other".
 */
private val ACTIVITY_WORDS = mapOf(
    "jog" to ExerciseType.Run,
    "ran" to ExerciseType.Run,
    "bike" to ExerciseType.Cycle,
    "cycling" to ExerciseType.Cycle,
    "gym" to ExerciseType.Strength,
    "lift" to ExerciseType.Strength,
    "weights" to ExerciseType.Strength,
)

private val WATER_GLASSES = Regex("""\b(\d+|a|an|one)\s+glass(?:es)?(?:\s+of\s+water)?""", RegexOption.IGNORE_CASE)
private val BODY_WEIGHT = Regex("""\bweigh\w*\s+(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
private val MINUTES = Regex("""(\d+)\s*min""", RegexOption.IGNORE_CASE)
private val HOURS = Regex("""(\d+)\s*(?:h|hr|hrs|hour|hours)\b""", RegexOption.IGNORE_CASE)
private val BARE_NUMBER = Regex("""\b\d+\b""")

/** A table row as a model-shaped item — and a guess, always. */
internal fun ScannedProduct.toRecognized() = RecognizedFood(
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
