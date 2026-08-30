package ph.mart.healthapp.feature.food.ui

import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodSuggestion
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.perServing
import ph.mart.healthapp.core.data.health.StepDay
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.data.water.DEFAULT_WATER_GOAL_GLASSES

data class FoodUiState(
    /** The day being shown. Every list below is that day's, and everything logged from this
     * screen is stamped with it. */
    val selectedDate: Long = todayEpochDay(),
    /** Re-read on each emission, so the header still says "Today" after a midnight rollover. */
    val today: Long = todayEpochDay(),
    val entries: List<FoodEntry> = emptyList(),
    val exercise: List<ExerciseEntry> = emptyList(),
    /** The selected day's steps, from Google Health. Null when none were imported for it. */
    val steps: StepDay? = null,
    /** From the profile — whether [exercise]'s burn raises the summary bar's goal. */
    val addExerciseToBudget: Boolean = true,
    val targets: DailyTargets? = null,
    val suggestions: List<FoodSuggestion> = emptyList(),
    /** Not the day's — saved meals are date-independent, and re-loggable onto any day. */
    val savedMeals: List<SavedMeal> = emptyList(),
    /** Date-independent for the same reason, and listed separately: a recipe seeds the form with
     * one serving, where a saved meal logs itself whole. */
    val recipes: List<Recipe> = emptyList(),
    val waterGlasses: Int = 0,
    val waterGoalGlasses: Int = DEFAULT_WATER_GOAL_GLASSES,
    val unit: UnitSystem = UnitSystem.Metric,
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

/** [dateEpochDay] 0 leaves the stamping to the repository, which means today. */
fun AddEntryForm.toFoodEntry(dateEpochDay: Long = 0): FoodEntry = FoodEntry(
    name = name,
    dateEpochDay = dateEpochDay,
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

/** Twin of [FoodSuggestion.toAddEntryForm] for a saved meal's item — it skips the form entirely,
 * because a saved meal is logged whole rather than edited one item at a time. */
fun SavedMealItem.toFoodEntry(mealType: MealType, dateEpochDay: Long): FoodEntry = FoodEntry(
    name = name,
    dateEpochDay = dateEpochDay,
    mealType = mealType,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
)

/** Twin of [FoodSuggestion.toAddEntryForm] again, and for the same reason it isn't a
 * [FoodEntry]: a recipe seeds the sheet's fields — one editable row priced at one serving, not the
 * ingredient list — so the user can log half a portion by editing the numbers before adding. */
fun Recipe.toAddEntryForm(mealType: MealType): AddEntryForm {
    val serving = perServing()
    return AddEntryForm(
        mealType = mealType,
        name = name,
        portionAmount = 1.0,
        portionUnit = SERVING_UNIT,
        calories = serving.calories,
        proteinG = serving.proteinG,
        carbsG = serving.carbsG,
        fatG = serving.fatG,
    )
}

/** Added to the add-entry sheet's portion-unit pills, so a seeded recipe shows its unit selected
 * instead of no pill at all — and so a leftovers-by-hand entry can say "serving" too.
 *
 * ponytail: the portion stepper still steps by 10, which is meaningless for servings; nobody can
 * type 0.5 there today. Editing the kcal field is the half-portion path until the stepper learns
 * a per-unit step size. */
const val SERVING_UNIT = "serving"

/** The day and meal slot are dropped: they are supplied again at log time, so the same saved meal
 * can go into any slot on any day. */
fun FoodEntry.toSavedMealItem(): SavedMealItem = SavedMealItem(
    name = name,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
)

sealed interface FoodEvent {
    data class OnSelectDate(val dateEpochDay: Long) : FoodEvent
    data class OnAddEntry(val form: AddEntryForm) : FoodEvent
    data class OnDeleteEntry(val id: Long) : FoodEvent
    data class OnToggleFavorite(val suggestion: FoodSuggestion, val favorite: Boolean) : FoodEvent
    data class OnSetWaterGlasses(val glasses: Int) : FoodEvent
    data class OnDeleteExercise(val id: Long) : FoodEvent
    data class OnSaveMeal(val name: String, val items: List<SavedMealItem>) : FoodEvent
    data class OnLogSavedMeal(val meal: SavedMeal, val mealType: MealType) : FoodEvent
    data class OnDeleteSavedMeal(val id: Long) : FoodEvent
    data class OnDeleteRecipe(val id: Long) : FoodEvent
}
