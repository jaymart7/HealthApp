package ph.mart.healthapp.feature.food.ui.diary

import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodSuggestion
import ph.mart.healthapp.core.data.food.MealIdea
import ph.mart.healthapp.core.data.food.MealIdeaRequest
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.perServing
import ph.mart.healthapp.core.data.health.StepDay
import ph.mart.healthapp.core.data.exercise.budgetKcal
import ph.mart.healthapp.core.data.food.dailyTotals
import ph.mart.healthapp.core.data.health.dayBurnedKcal
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.DietaryPreference
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.data.water.DEFAULT_WATER_GOAL_GLASSES
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.SERVING_UNIT

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
    /** From the profile, and read by the meal-ideas screen alone — the diary itself has no use
     * for either. [diet] is what the user picked in onboarding, and until that screen existed it
     * was stored, migrated and exported without a single reader. */
    val goal: Goal? = null,
    val diet: DietaryPreference? = null,
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
    fiberG = fiberG,
    sugarG = sugarG,
    sodiumMg = sodiumMg,
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
    fiberG = fiberG,
    sugarG = sugarG,
    sodiumMg = sodiumMg,
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
        fiberG = serving.fiberG,
        sugarG = serving.sugarG,
        sodiumMg = serving.sodiumMg,
    )
}

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
    fiberG = fiberG,
    sugarG = sugarG,
    sodiumMg = sodiumMg,
)

sealed interface FoodEvent {
    data class OnSelectDate(val dateEpochDay: Long) : FoodEvent
    data class OnAddEntry(val form: AddEntryForm) : FoodEvent
    data class OnDeleteEntry(val id: Long) : FoodEvent

    /** A logged row corrected in place. The repository retires [id] and writes the corrected row
     * in its place, so the diary keeps its order — see `FoodRepository.updateEntry`. */
    data class OnUpdateEntry(val id: Long, val form: AddEntryForm) : FoodEvent

    /** Undo, for the snackbar a delete raises. Soft delete has no restore-by-id, so the row is
     * written again from what the screen still holds — a new id for the same meal, which is what
     * the user asked for and all they can see. */
    data class OnRestoreEntry(val entry: FoodEntry) : FoodEvent
    data class OnToggleFavorite(val suggestion: FoodSuggestion, val favorite: Boolean) : FoodEvent
    data class OnSetWaterGlasses(val glasses: Int) : FoodEvent
    data class OnDeleteExercise(val id: Long) : FoodEvent

    /** Twin of [OnRestoreEntry], for the exercise section's swipe. */
    data class OnRestoreExercise(val entry: ExerciseEntry) : FoodEvent
    data class OnSaveMeal(val name: String, val items: List<SavedMealItem>) : FoodEvent
    data class OnLogSavedMeal(val meal: SavedMeal, val mealType: MealType) : FoodEvent
    data class OnDeleteSavedMeal(val id: Long) : FoodEvent
    data class OnDeleteRecipe(val id: Long) : FoodEvent
}

/**
 * What the meal-ideas screen asks with, or null when the day can't be described yet: no profile
 * means no target, and no target means no gap to fill.
 *
 * Built here, off state the diary has already combined, rather than in a ViewModel of its own —
 * that is the whole reason meal ideas is an overlay and not a route. The budget is the *same*
 * arithmetic the summary bar draws (`budgetKcal` over `dayBurnedKcal`), so the screen can never
 * offer more calories than the bar above it says are left.
 *
 * Null again once the day is full: there is nothing to suggest, and a card offering ideas against
 * a spent budget is a control that can't answer — Home's rule for the supplements card.
 */
fun FoodUiState.mealIdeaRequest(mealType: MealType): MealIdeaRequest? {
    val targets = targets ?: return null
    val goal = goal ?: return null
    val totals = entries.dailyTotals()
    val budget = budgetKcal(
        targetKcal = targets.calories,
        burnedKcal = dayBurnedKcal(exercise, steps),
        addExercise = addExerciseToBudget,
    )
    val remainingKcal = budget - totals.calories
    if (remainingKcal < MIN_IDEA_KCAL) return null
    return MealIdeaRequest(
        goal = goal,
        mealType = mealType,
        remainingKcal = remainingKcal,
        remainingProteinG = targets.proteinG - totals.proteinG,
        remainingCarbsG = targets.carbsG - totals.carbsG,
        remainingFatG = targets.fatG - totals.fatG,
        diet = diet,
    )
}

/** Below this there is no meal left in the day, only a mint. */
private const val MIN_IDEA_KCAL = 100

/** Twin of [FoodSuggestion.toAddEntryForm] — an idea seeds the sheet's fields exactly like a
 * recent or a search hit does, and stays editable and repriceable after. */
fun MealIdea.toAddEntryForm(mealType: MealType): AddEntryForm = AddEntryForm(
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
