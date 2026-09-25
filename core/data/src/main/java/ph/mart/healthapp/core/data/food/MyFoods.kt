package ph.mart.healthapp.core.data.food

/**
 * The user's own foods, offered to an online parse: the rule [offlineFoods] already follows with no
 * network, carried over to the model. A food they saved from a label is the one they mean, and its
 * figures are the label's, not an estimate — an online parse that guessed them anyway was worse than
 * the offline one.
 *
 * Three steps: [namedIn] picks the few saved foods their words could mean, [myFoodsLine] names them
 * to the model, and [preferMyFoods] puts the saved figures back on any row the model returned under
 * one of those names. The model is trusted with *which* food and *how much*, never with what a saved
 * food contains.
 */

/** Past this many candidates a word like "chicken" is matching a pantry, not naming a food. */
private const val MAX_MY_FOODS_OFFERED = 8

/** The saved foods a word of [text] could mean — [offlineFoods]' own word rule, so an online and an
 * offline parse of one sentence consider the same foods. Usually none, and then the prompt carries
 * nothing extra. */
internal fun List<ScannedProduct>.namedIn(text: String): List<ScannedProduct> {
    val words = matchWords(text)
    return filter { food -> words.any { food.namedBy(it) } }.take(MAX_MY_FOODS_OFFERED)
}

/** One prompt line naming [mine] with the unit each is saved in, or null when there are none. The unit
 * is asked for so that [preferMyFoods] can reprice rather than guess. Stays in Kotlin: prompt text. */
internal fun myFoodsLine(mine: List<ScannedProduct>): String? {
    if (mine.isEmpty()) return null
    val names = mine.joinToString(", ") { "\"${it.name}\" (${it.portionUnit})" }
    return "Some of what they said may be one of their own saved foods: $names. If it is, use that " +
        "name exactly as written and give its portion in that unit."
}

/**
 * Each row the model named after one of [mine] takes that food's own figures. The model's portion
 * reprices them through [portionFactor] — the portion stepper's rule — when it came back in the
 * saved unit. In any other unit there is nothing honest to convert by, so the row keeps the saved
 * serving as it is and goes [RecognitionConfidence.Low], which the review tags. Every other row
 * passes untouched.
 */
internal fun List<RecognizedFood>.preferMyFoods(mine: List<ScannedProduct>): List<RecognizedFood> = map { food ->
    val saved = mine.firstOrNull { it.name.equals(food.name.trim(), ignoreCase = true) } ?: return@map food
    val factor = portionFactor(from = saved.portionAmount, to = food.portionAmount)
        ?.takeIf { saved.portionUnit.equals(food.portionUnit.trim(), ignoreCase = true) }
        ?: return@map saved.toRecognized().copy(uncertainAbout = food.uncertainAbout)
    food.copy(
        name = saved.name,
        calories = saved.calories.scaledBy(factor),
        proteinG = saved.proteinG.scaledBy(factor),
        carbsG = saved.carbsG.scaledBy(factor),
        fatG = saved.fatG.scaledBy(factor),
        nutrients = saved.nutrients * factor,
    )
}
