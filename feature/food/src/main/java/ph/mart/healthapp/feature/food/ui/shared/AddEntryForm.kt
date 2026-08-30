package ph.mart.healthapp.feature.food.ui.shared

import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.QUICK_ADD_NAME
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
)

/** Added to the add-entry sheet's portion-unit pills, so a seeded recipe shows its unit selected
 * instead of no pill at all — and so a leftovers-by-hand entry can say "serving" too.
 *
 * ponytail: the portion stepper still steps by 10, which is meaningless for servings; nobody can
 * type 0.5 there today. Editing the kcal field is the half-portion path until the stepper learns
 * a per-unit step size. */
const val SERVING_UNIT = "serving"

/**
 * Twin of [RecognizedFood.toAddEntryForm][ph.mart.healthapp.feature.food.ui.photo.toAddEntryForm].
 *
 * Here rather than in `barcode/` because the photo flow's manual-search fallback seeds the form
 * from a scanned product too.
 *
 * ponytail: the macros stay as looked up (per 100 g) when the user edits the portion — manual
 * entry doesn't rescale either. Scale here if that's ever reported as wrong.
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
)
