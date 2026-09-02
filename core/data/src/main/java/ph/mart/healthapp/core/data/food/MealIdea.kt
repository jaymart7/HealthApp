package ph.mart.healthapp.core.data.food

import ph.mart.healthapp.core.data.profile.DietaryPreference
import ph.mart.healthapp.core.data.profile.Goal

/**
 * What the model is told when the user asks what fits in the rest of the day — and, again,
 * deliberately nothing else.
 *
 * This is the *second* payload type that leaves the device, beside
 * [InsightRequest][ph.mart.healthapp.core.data.insight.InsightRequest], and that is not a widening
 * of what FitPulse sends. The insight describes the whole day — water, streak, the week's weight
 * delta — because a one-line nudge can be about any of it; a meal idea can only be about the gap
 * it has to fill, so a payload shaped for this call sends strictly less than reusing that one
 * would. No age, sex, height, absolute weight, name, or diary rows, exactly as there.
 *
 * [diet] is the one field neither other call sends, and it is a four-value enum the user picked in
 * onboarding — until now stored, migrated and exported without a single reader. It is the
 * difference between an idea and an insult: a screen that suggests grilled chicken to a vegan has
 * failed at the only thing it does.
 *
 * [remainingKcal] and the three macro gaps may be negative on a day already over target; the
 * screen doesn't offer the button then, but the type doesn't pretend it can't happen.
 */
data class MealIdeaRequest(
    val goal: Goal,
    val mealType: MealType,
    val remainingKcal: Int,
    val remainingProteinG: Int,
    val remainingCarbsG: Int,
    val remainingFatG: Int,
    val diet: DietaryPreference?,
)

/**
 * One suggestion: the same ten fields every seeded food in this app carries, so it seeds the
 * add-entry sheet through a `toAddEntryForm` twin like a search hit, a recipe or a scanned product
 * does — and is repriced by `withPortionAmount()` the moment the user changes the portion.
 *
 * It is not a [RecognizedFood]: that one means "what the camera saw" and carries a confidence the
 * model reports per photo. An idea is not an identification of anything.
 */
data class MealIdea(
    val name: String,
    val portionAmount: Double,
    val portionUnit: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val fiberG: Int = 0,
    val sugarG: Int = 0,
    val sodiumMg: Int = 0,
)

/** No error *type*, for [CoachReply][ph.mart.healthapp.core.data.coach.CoachReply]'s reason:
 * offline, throttled, App Check refused and a model that answered with nothing usable all land on
 * the same fallback — the user's own foods. */
sealed interface MealIdeaResult {
    data class Success(val ideas: List<MealIdea>) : MealIdeaResult
    data object Failed : MealIdeaResult
}

/**
 * The third Gemini-backed feature, and the first that is asked for something the user hasn't done
 * yet.
 *
 * Nothing is cached, unlike the daily insight's one line per day: the budget moves with every row
 * logged, so an idea from two meals ago is an answer to a question nobody is asking any more. That
 * is the coach's rule — an explicit tap gets a fresh answer.
 */
interface MealIdeaRepository {
    suspend fun ideas(request: MealIdeaRequest): MealIdeaResult
}

/** Three cards is what fits above the fold on the smallest screen in scope, and a fourth idea is
 * one more thing to weigh up in a flow whose whole point is not deciding. */
const val MAX_MEAL_IDEAS = 3

/**
 * How far past the remaining budget an idea may land before it is dropped rather than shown.
 *
 * Not zero: a 650 kcal plate against 640 left is the right answer given rounding and a portion
 * nobody weighs, and rejecting it would leave the screen emptier than it should be. Not generous
 * either — the header says how much is left, and a card twice that size makes the screen a liar.
 */
private const val MEAL_IDEA_KCAL_TOLERANCE = 1.2

/**
 * The whole of the trust boundary on the model's ideas — everything the screen renders passes
 * through here.
 *
 * A pure function for the reason [sanitizeInsight][ph.mart.healthapp.core.data.insight.sanitizeInsight]
 * is one: the JSON parse around it uses [org.json.JSONObject], which is stubbed in JVM unit tests,
 * so the judgement is kept on this side of it where a test can reach it.
 *
 * Filtering rather than truncating a bad list: an idea with no name or no calories is not a
 * shorter idea, it is not one.
 */
fun List<MealIdea>.fitting(remainingKcal: Int): List<MealIdea> {
    val ceiling = remainingKcal * MEAL_IDEA_KCAL_TOLERANCE
    return filter { it.name.isNotBlank() && it.calories > 0 && it.calories <= ceiling }
        .take(MAX_MEAL_IDEAS)
}

/**
 * What the screen shows offline, and when the call fails: the user's own foods that still fit.
 *
 * Derived, not stored — no table, no repository, no query of its own, the `mergeSuggestions()` and
 * `badgeGroups()` shape. It folds the two lists the diary has already loaded for its add-entry
 * sheet, so it costs one pass over at most ten rows and works with the radio off.
 *
 * Ordered by protein descending because that is the gap this app already nags about (see
 * `insightFor`), and because "most protein for the calories you have left" is a rule a user can
 * predict — a scoring function blending four macros is not.
 */
fun localMealIdeas(
    suggestions: List<FoodSuggestion>,
    recipes: List<Recipe>,
    remainingKcal: Int,
): List<MealIdea> = (suggestions.map { it.toMealIdea() } + recipes.map { it.toMealIdea() })
    .filter { it.calories in 1..remainingKcal }
    .sortedByDescending { it.proteinG }
    .take(MAX_MEAL_IDEAS)

private fun FoodSuggestion.toMealIdea() = MealIdea(
    name = name,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    fiberG = fiberG,
    sugarG = sugarG,
    sodiumMg = sodiumMg,
)

/** One serving, priced by the recipe's own [perServing] — the same figure
 * `Recipe.toAddEntryForm()` seeds the sheet with, so picking the idea and picking the recipe from
 * its panel log the identical row. */
private fun Recipe.toMealIdea(): MealIdea {
    val serving = perServing()
    return MealIdea(
        name = name,
        portionAmount = 1.0,
        portionUnit = "serving",
        calories = serving.calories,
        proteinG = serving.proteinG,
        carbsG = serving.carbsG,
        fatG = serving.fatG,
        fiberG = serving.fiberG,
        sugarG = serving.sugarG,
        sodiumMg = serving.sodiumMg,
    )
}
