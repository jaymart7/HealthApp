package ph.mart.healthapp.core.data.food

import androidx.annotation.StringRes
import ph.mart.healthapp.core.data.R

/** Whether a target is something to reach or a ceiling to stay under. Sugar and sodium are the
 * two limits; grading them the same direction as calcium would congratulate a user for a salty
 * day. */
enum class NutrientDirection { Reach, StayUnder }

/**
 * The seven graded nutrients, in the order every panel draws them: the three this app has always
 * reported first, then the four from the Nutrition Facts panel.
 *
 * An enum rather than seven hand-written rows because the panel, its legend and its accessibility
 * text all iterate the same list. Labels live in `:core:data`'s `strings.xml` beside the six enums
 * already there — `:core:designsystem` has no dependency on this module, so the alternative is the
 * same seven labels copied into every feature that draws them.
 */
enum class Nutrient(
    @StringRes val labelRes: Int,
    val direction: NutrientDirection,
) {
    Fiber(R.string.data_nutrient_fiber, NutrientDirection.Reach),
    Sugar(R.string.data_nutrient_sugar, NutrientDirection.StayUnder),
    Sodium(R.string.data_nutrient_sodium, NutrientDirection.StayUnder),
    VitaminD(R.string.data_nutrient_vitamin_d, NutrientDirection.Reach),
    Calcium(R.string.data_nutrient_calcium, NutrientDirection.Reach),
    Iron(R.string.data_nutrient_iron, NutrientDirection.Reach),
    Potassium(R.string.data_nutrient_potassium, NutrientDirection.Reach),
}

fun Nutrients.valueOf(nutrient: Nutrient): Int = when (nutrient) {
    Nutrient.Fiber -> fiberG
    Nutrient.Sugar -> sugarG
    Nutrient.Sodium -> sodiumMg
    Nutrient.VitaminD -> vitaminDUg
    Nutrient.Calcium -> calciumMg
    Nutrient.Iron -> ironUg
    Nutrient.Potassium -> potassiumMg
}

/**
 * A stored figure as the user reads it, unit included.
 *
 * Stays in Kotlin, and has a test over its wording: unit symbols are not copy, and the one thing
 * here that *is* a decision — iron stored in micrograms and shown in milligrams to one decimal —
 * is arithmetic rather than language. Sodium, potassium and calcium are grouped because they
 * routinely run into the thousands.
 */
fun formatNutrient(nutrient: Nutrient, value: Int): String = when (nutrient) {
    Nutrient.Fiber, Nutrient.Sugar -> "$value g"
    Nutrient.Sodium, Nutrient.Calcium, Nutrient.Potassium -> "${value.grouped()} mg"
    Nutrient.VitaminD -> "$value µg"
    // Micrograms in, milligrams out — the unit the panel prints and the packet shows.
    Nutrient.Iron -> "${(value / 100).let { "${it / 10}.${it % 10}" }} mg"
}

/** Digit grouping without a `NumberFormat` allocation per row, and without a locale: these are
 * figures, and the app's one existing grouped number (sodium) has always read this way. */
internal fun Int.grouped(): String = toString().reversed().chunked(3).joinToString(",").reversed()

/**
 * One nutrient as a panel draws it: what was eaten, and what it is measured against.
 *
 * The arithmetic lives here rather than in the composable because two features draw this — the
 * diary's day and Progress's range — and `:core:designsystem` cannot see [Nutrients] (it is a leaf
 * module with no dependency on `:core:data`, which is also why the labels are resources here). So
 * each feature resolves three strings and the rule is written once.
 */
data class NutrientReading(
    val nutrient: Nutrient,
    val value: Int,
    /** Null when there is no profile to derive one from — the panel then reports without grading,
     * which is what this app did for every nutrient before targets existed. */
    val target: Int?,
) {
    val fraction: Float
        get() = if (target == null || target <= 0) 0f else (value.toFloat() / target).coerceIn(0f, 1f)

    /** Only a [NutrientDirection.StayUnder] nutrient can be *over*. Passing a calcium goal is
     * good news and must never draw as an error. */
    val overLimit: Boolean
        get() = target != null && nutrient.direction == NutrientDirection.StayUnder && value > target
}

/**
 * The rows a panel shows, in enum order.
 *
 * **A nutrient with no value is absent, not zero.** `0` means unknown-or-none everywhere in this
 * app, so a day with no calcium figure gets no calcium row rather than one reading 0 of 1000 mg —
 * the built-in food list has no calcium for half its rows, and grading a blank as a shortfall is
 * the one thing this feature must not do. What is missing is explained by the coverage count
 * instead; see [DiaryTotals.foodsWithMicronutrients].
 */
fun Nutrients.readings(targets: Nutrients?): List<NutrientReading> =
    Nutrient.entries.mapNotNull { nutrient ->
        val value = valueOf(nutrient)
        if (value <= 0) null else NutrientReading(nutrient, value, targets?.valueOf(nutrient)?.takeIf { it > 0 })
    }
