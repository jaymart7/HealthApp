package ph.mart.healthapp.core.data.food

import ph.mart.healthapp.core.data.stripMarkdown

/**
 * The recipe builder's AI field: a dish, a sentence or a pasted ingredient list in, a recipe out —
 * a name, how many portions it makes, and every ingredient priced for the amount the whole recipe
 * uses.
 *
 * Not [MealParseRepository] with a different prompt, for three reasons that each break it: a pasted
 * ingredient list is longer than [MAX_PARSE_CHARS], a recipe has more parts than [MAX_PARSED_FOODS],
 * and "chicken adobo" alone has to *become* its ingredients, where the meal parse is told to invent
 * nothing. The payload is that parse's and no wider — the user's own text, nothing from the profile.
 */
interface RecipeParseRepository {
    suspend fun parse(text: String): RecipeParseResult
}

/** [NothingFound] is its own answer for [MealParseResult.NoFoodFound]'s reason: "that wasn't a
 * recipe" and "the call didn't work" say different things under the field. */
sealed interface RecipeParseResult {
    data class Parsed(val name: String, val servings: Int, val items: List<SavedMealItem>) : RecipeParseResult
    data object NothingFound : RecipeParseResult
    data object Failed : RecipeParseResult
}

/** A pasted ingredient list with a line of method under it — not a whole blog post. */
const val MAX_RECIPE_CHARS = 2000

/** A long recipe, not a pantry. Past this the model has started listing garnishes. */
const val MAX_RECIPE_INGREDIENTS = 20

/** "Serves 500" is a model misreading a gram figure, not a batch cook. */
const val MAX_RECIPE_SERVINGS = 50

/** A dish's name, not a sentence about it. */
private const val MAX_RECIPE_NAME_CHARS = 60

/**
 * The whole of the trust boundary on the recipe parse, pure for [loggable]'s reason: the
 * [org.json] read around it is stubbed on the JVM, so the judgement is kept where a test reaches it.
 *
 * Deliberately **not** [loggable]: that drops a zero-calorie item as the model declining, but in a
 * recipe salt, water and spices are real ingredients at 0 kcal — and salt is where the sodium is.
 * A blank name is still not an ingredient. Servings are clamped rather than trusted, the call
 * [perServing] makes: 0 is a divide-by-zero and the honest reading of it is one.
 */
fun recipeParseResult(
    name: String?,
    servings: Int,
    ingredients: List<RecognizedFood>,
): RecipeParseResult {
    val items = ingredients
        .map { it.copy(name = stripMarkdown(it.name).trim()) }
        .filter { it.name.isNotBlank() }
        .take(MAX_RECIPE_INGREDIENTS)
        .map {
            SavedMealItem(
                name = it.name,
                portionAmount = it.portionAmount,
                portionUnit = it.portionUnit,
                calories = it.calories,
                proteinG = it.proteinG,
                carbsG = it.carbsG,
                fatG = it.fatG,
                nutrients = it.nutrients,
            )
        }
    if (items.isEmpty()) return RecipeParseResult.NothingFound
    return RecipeParseResult.Parsed(
        name = stripMarkdown(name.orEmpty()).trim().take(MAX_RECIPE_NAME_CHARS).trim(),
        servings = servings.coerceIn(1, MAX_RECIPE_SERVINGS),
        items = items,
    )
}
