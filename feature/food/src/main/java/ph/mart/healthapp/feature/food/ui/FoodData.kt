package ph.mart.healthapp.feature.food.ui

import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodSuggestion
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.profile.DailyTargets

data class FoodUiState(
    val entries: List<FoodEntry> = emptyList(),
    val targets: DailyTargets? = null,
    val suggestions: List<FoodSuggestion> = emptyList(),
)

/** What the user is actively editing in the add-entry sheet — seeded fresh (not from a loaded
 * record) each time a meal section's "+" is tapped. */
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

fun AddEntryForm.isValid(): Boolean = name.isNotBlank()

fun AddEntryForm.toFoodEntry(): FoodEntry = FoodEntry(
    name = name,
    mealType = mealType,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
)

/** Twin of [ScannedProduct.toAddEntryForm][ph.mart.healthapp.core.data.food.ScannedProduct] — a
 * suggestion seeds the sheet's fields exactly like a search hit does, and stays editable after. */
fun FoodSuggestion.toAddEntryForm(mealType: MealType): AddEntryForm = AddEntryForm(
    mealType = mealType,
    name = name,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
)

sealed interface FoodEvent {
    data class OnAddEntry(val form: AddEntryForm) : FoodEvent
    data class OnDeleteEntry(val id: Long) : FoodEvent
    data class OnToggleFavorite(val suggestion: FoodSuggestion, val favorite: Boolean) : FoodEvent
}
