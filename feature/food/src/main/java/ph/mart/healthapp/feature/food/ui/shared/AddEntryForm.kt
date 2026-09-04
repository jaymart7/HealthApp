package ph.mart.healthapp.feature.food.ui.shared

import kotlin.math.roundToInt
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodSuggestion
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.QUICK_ADD_NAME
import ph.mart.healthapp.core.data.food.RecognizedFood
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.ScannedProduct

/** What the user is actively editing in the add-entry sheet — seeded fresh (not from a loaded
 * record) each time a meal section's "+" is tapped.
 *
 * It lives here rather than with the diary because all three logging paths — the diary sheet, the
 * photo flow's confirmation, and the barcode flow's — edit and log this same form. */
data class AddEntryForm(
    val mealType: MealType = MealType.Breakfast,
    val name: String = "",
    val portionAmount: Double = 100.0,
    val portionUnit: String = "g",
    val calories: Int = 0,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatG: Int = 0,
    val fiberG: Int = 0,
    val sugarG: Int = 0,
    val sodiumMg: Int = 0,
)

/** A bare calorie figure is enough — that is the quick add. The guard is deliberately shared with
 * the photo and barcode confirmation screens: all three log through [toFoodEntry], which fills the
 * blank, so clearing a name there degrades to a quick add rather than deadlocking the button. */
fun AddEntryForm.isValid(): Boolean = name.isNotBlank() || calories > 0

/** [dateEpochDay] 0 leaves the stamping to the repository, which means today.
 *
 * A blank name is a quick add: it becomes [QUICK_ADD_NAME], and the portion collapses to one
 * serving because the form's default 100 g would be a number the user never supplied. */
fun AddEntryForm.toFoodEntry(dateEpochDay: Long = 0): FoodEntry = FoodEntry(
    name = name.ifBlank { QUICK_ADD_NAME },
    dateEpochDay = dateEpochDay,
    mealType = mealType,
    portionAmount = if (name.isBlank()) 1.0 else portionAmount,
    portionUnit = if (name.isBlank()) SERVING_UNIT else portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    fiberG = fiberG,
    sugarG = sugarG,
    sodiumMg = sodiumMg,
)

/**
 * Inverse of [toFoodEntry], for reopening a logged row to correct it.
 *
 * [QUICK_ADD_NAME] maps back to a blank name so a quick add reopens as one — the sheet's button
 * says "Quick add" again, and saving round-trips the row straight back to itself.
 */
fun FoodEntry.toAddEntryForm(): AddEntryForm = AddEntryForm(
    mealType = mealType,
    name = name.takeIf { it != QUICK_ADD_NAME }.orEmpty(),
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

/**
 * The form as a food the user owns, for "Save as my food".
 *
 * A food the user authored and a food they starred are the same `favorite_food` row — see
 * `FoodRepository.setFavorite` — so authoring needs no second type and no second write path, only
 * this map. The name is the row's identity, which is what makes saving the same name twice an edit.
 */
fun AddEntryForm.toSuggestion(): FoodSuggestion = FoodSuggestion(
    name = name.trim(),
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    fiberG = fiberG,
    sugarG = sugarG,
    sodiumMg = sodiumMg,
    isFavorite = true,
)

/** A food with no name is a quick add and a food with no calories is nothing — neither is
 * something to keep. The sheet hides the button rather than disabling it, so this is what it
 * hides on. */
fun AddEntryForm.isSaveableFood(): Boolean = name.isNotBlank() && calories > 0

/** Added to the add-entry sheet's portion-unit pills, so a seeded recipe shows its unit selected
 * instead of no pill at all — and so a leftovers-by-hand entry can say "serving" too. */
const val SERVING_UNIT = "serving"

/**
 * What the camera saw, or what the user said they ate — both arrive as a [RecognizedFood].
 *
 * Here rather than in `photo/` because the voice flow seeds one form per parsed item through it,
 * and a helper two flows reach for does not stay in whichever one declared it first.
 */
fun RecognizedFood.toAddEntryForm(mealType: MealType): AddEntryForm = AddEntryForm(
    mealType = mealType,
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

/**
 * Twin of [RecognizedFood.toAddEntryForm].
 *
 * Here rather than in `barcode/` because the photo flow's manual-search fallback seeds the form
 * from a scanned product too.
 */
fun ScannedProduct.toAddEntryForm(mealType: MealType): AddEntryForm = AddEntryForm(
    mealType = mealType,
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

/**
 * Changing the portion reprices what the portion is made of.
 *
 * Every seeded value in this app is a figure *for a stated amount* — 539 kcal per 100 g off Open
 * Food Facts, an AI estimate for the plate it saw, a recipe's serving. Moving the amount without
 * moving those numbers means the barcode screen's own instruction ("adjust the portion to match
 * what you ate") writes 539 kcal against 30 g of Nutella, silently and in the direction that
 * inflates the day.
 *
 * The factor is applied to the current pair rather than to a remembered original, so there is no
 * seed to carry around and no way for a re-seeded form to disagree with itself. Each step rounds,
 * so a run of stepper taps can land a unit off the one-shot answer — bounded, not compounding, and
 * a kilocalorie either way. A zero or absent starting portion has no price-per-unit to scale from,
 * so the amount moves alone.
 */
fun AddEntryForm.withPortionAmount(amount: Double): AddEntryForm {
    val factor = portionFactor(from = portionAmount, to = amount) ?: return copy(portionAmount = amount)
    return copy(
        portionAmount = amount,
        calories = scale(calories, factor),
        proteinG = scale(proteinG, factor),
        carbsG = scale(carbsG, factor),
        fatG = scale(fatG, factor),
        fiberG = scale(fiberG, factor),
        sugarG = scale(sugarG, factor),
        sodiumMg = scale(sodiumMg, factor),
    )
}

/** Twin of [AddEntryForm.withPortionAmount] for the recipe builder's ingredient draft, which edits
 * a [SavedMealItem] rather than a form but is priced exactly the same way. */
fun SavedMealItem.withPortionAmount(amount: Double): SavedMealItem {
    val factor = portionFactor(from = portionAmount, to = amount) ?: return copy(portionAmount = amount)
    return copy(
        portionAmount = amount,
        calories = scale(calories, factor),
        proteinG = scale(proteinG, factor),
        carbsG = scale(carbsG, factor),
        fatG = scale(fatG, factor),
        fiberG = scale(fiberG, factor),
        sugarG = scale(sugarG, factor),
        sodiumMg = scale(sodiumMg, factor),
    )
}

/** Null when there is nothing to scale from or to — the caller then moves the amount alone. */
private fun portionFactor(from: Double, to: Double): Double? =
    if (from <= 0.0 || to < 0.0) null else to / from

private fun scale(value: Int, factor: Double): Int = (value * factor).roundToInt()
