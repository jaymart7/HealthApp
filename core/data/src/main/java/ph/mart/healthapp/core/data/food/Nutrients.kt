package ph.mart.healthapp.core.data.food

import kotlin.math.roundToInt

/**
 * The seven nutrients an entry carries besides calories and the three macros: the fiber, sugar
 * and sodium this app has always tracked, plus the four the FDA Nutrition Facts panel mandates.
 *
 * One value type rather than seven fields repeated across nine carriers. Every path that used to
 * copy three lines — [dailyTotals], [averages][ph.mart.healthapp.core.data.food.averages],
 * [dailySeries], a recipe's per-serving divide, the add-entry form's portion repricing — now
 * copies one, and adding an eighth nutrient later is one field rather than nine edits.
 *
 * **Units are in the field names**, the convention `sodiumMg` already set. Iron and vitamin D are
 * stored in *micrograms* while calcium, potassium and sodium are milligrams, because these are
 * `Int` like every nutrient figure in the app and iron at Int milligrams would round a 0.4 mg food
 * to nothing — over a day's eight entries that compounds into a shortfall the user never had.
 * Display divides; see `formatNutrient`.
 *
 * `0` means unknown-or-none, exactly as it does for a missing FDC macro. Nothing in this type can
 * tell the two apart, which is why every graded surface reports its coverage alongside — see
 * [DiaryTotals.foodsWithMicronutrients].
 */
data class Nutrients(
    val fiberG: Int = 0,
    val sugarG: Int = 0,
    val sodiumMg: Int = 0,
    val vitaminDUg: Int = 0,
    val calciumMg: Int = 0,
    val ironUg: Int = 0,
    val potassiumMg: Int = 0,
)

operator fun Nutrients.plus(other: Nutrients): Nutrients = Nutrients(
    fiberG = fiberG + other.fiberG,
    sugarG = sugarG + other.sugarG,
    sodiumMg = sodiumMg + other.sodiumMg,
    vitaminDUg = vitaminDUg + other.vitaminDUg,
    calciumMg = calciumMg + other.calciumMg,
    ironUg = ironUg + other.ironUg,
    potassiumMg = potassiumMg + other.potassiumMg,
)

/** Integer division, matching how [averages] has always meaned its macros. A zero divisor is the
 * caller's bug everywhere this is used — they all guard on an empty list first — so it returns
 * [Nutrients] rather than throwing into a chart. */
operator fun Nutrients.div(n: Int): Nutrients {
    if (n <= 0) return Nutrients()
    return Nutrients(
        fiberG = fiberG / n,
        sugarG = sugarG / n,
        sodiumMg = sodiumMg / n,
        vitaminDUg = vitaminDUg / n,
        calciumMg = calciumMg / n,
        ironUg = ironUg / n,
        potassiumMg = potassiumMg / n,
    )
}

/** What a portion change and a recipe's servings both do: scale every figure by one factor,
 * rounded rather than truncated so a doubled 1 mg does not stay 1 mg. */
operator fun Nutrients.times(factor: Double): Nutrients = Nutrients(
    fiberG = (fiberG * factor).roundToInt(),
    sugarG = (sugarG * factor).roundToInt(),
    sodiumMg = (sodiumMg * factor).roundToInt(),
    vitaminDUg = (vitaminDUg * factor).roundToInt(),
    calciumMg = (calciumMg * factor).roundToInt(),
    ironUg = (ironUg * factor).roundToInt(),
    potassiumMg = (potassiumMg * factor).roundToInt(),
)

/** Nothing worth reporting. What `NutrientPanel` renders nothing for — a day of quick adds has
 * no nutrients to grade, and a row of zeros against a target would claim a shortfall that isn't
 * one. */
val Nutrients.isEmpty: Boolean
    get() = fiberG <= 0 && sugarG <= 0 && sodiumMg <= 0 &&
        vitaminDUg <= 0 && calciumMg <= 0 && ironUg <= 0 && potassiumMg <= 0

/** True when this carries at least one of the four panel nutrients. Fiber, sugar and sodium are
 * deliberately excluded: the built-in food list has filled those since before this type existed,
 * so counting them would report full coverage for a day with no vitamin data at all. */
val Nutrients.hasMicronutrients: Boolean
    get() = vitaminDUg > 0 || calciumMg > 0 || ironUg > 0 || potassiumMg > 0
