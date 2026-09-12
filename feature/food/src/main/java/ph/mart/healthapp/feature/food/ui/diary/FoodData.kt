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
import ph.mart.healthapp.core.data.food.Nutrients
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
    /** The seven nutrient targets, derived from the same profile [targets] is — null with no
     * profile, which is what leaves the summary bar's nutrient line ungraded. */
    val nutrientTargets: Nutrients? = null,
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
    /** The day a copy is being taken *from* — null unless the copy sheet is open, and what the
     * sheet's own visibility is read off. */
    val copySource: CopyDay? = null,
)

/**
 * Another day's log, loaded while the copy sheet is over the diary: what that day held, so the
 * sheet can say what there is to bring over before anything is written.
 *
 * It rides the diary's own combine rather than sitting beside it — `observeDiary` ends in
 * `reduce { newState }`, which replaces the state wholesale, so anything held outside the emission
 * is wiped the next time Room speaks.
 */
data class CopyDay(
    val dateEpochDay: Long,
    val entries: List<FoodEntry> = emptyList(),
    val exercise: List<ExerciseEntry> = emptyList(),
    val waterGlasses: Int = 0,
)

/** Nothing was logged that day, so there is nothing to copy — the sheet's one dead end. */
val CopyDay.isEmpty: Boolean
    get() = entries.isEmpty() && exercise.isEmpty() && waterGlasses == 0

/**
 * The ticked meals, re-stamped for [dateEpochDay]. `id = 0` so each row is an insert rather than a
 * collision, and the plate stays behind: a photo belongs to the meal it was taken of, which is the
 * call `FoodHistoryViewModel.logAgain` already makes for a single re-logged row.
 */
internal fun CopyDay.foodOnto(dateEpochDay: Long, meals: Set<MealType>): List<FoodEntry> =
    entries
        .filter { it.mealType in meals }
        .map { it.copy(id = 0, dateEpochDay = dateEpochDay, photoPath = null) }

/**
 * The day's workouts, re-stamped the same way, sets and all — they ride on the entry, so
 * `ExerciseRepository.addEntry` writes a copied strength session whole.
 *
 * `steps = 0` is not data loss: `addEntry` re-estimates a step count from the type and the minutes
 * when it sees zero, and the figure being dropped is the watch's own, recorded against the day it
 * was actually walked.
 */
internal fun CopyDay.exerciseOnto(dateEpochDay: Long): List<ExerciseEntry> =
    exercise.map { it.copy(id = 0, dateEpochDay = dateEpochDay, steps = 0) }

/**
 * The calorie goal the summary bar reads against: the target, plus the day's burn when the profile
 * says to credit it. One function because the diary and the day's shared image must never disagree
 * about what the budget was.
 */
internal fun FoodUiState.dayBudgetKcal(targets: DailyTargets): Int = budgetKcal(
    targetKcal = targets.calories,
    burnedKcal = dayBurnedKcal(exercise, steps),
    addExercise = addExerciseToBudget,
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
    nutrients = nutrients,
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
    nutrients = nutrients,
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
        nutrients = serving.nutrients,
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
    nutrients = nutrients,
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

    /** Keeps what is in the form as a food the user owns, without logging it. The same write
     * [OnToggleFavorite] makes — a starred food and an authored one are one row — so saving a name
     * that already exists edits it rather than adding a rival. */
    data class OnSaveMyFood(val form: AddEntryForm) : FoodEvent
    data class OnSetWaterGlasses(val glasses: Int) : FoodEvent
    data class OnDeleteExercise(val id: Long) : FoodEvent

    /** Twin of [OnRestoreEntry], for the exercise section's swipe. */
    data class OnRestoreExercise(val entry: ExerciseEntry) : FoodEvent
    data class OnSaveMeal(val name: String, val items: List<SavedMealItem>) : FoodEvent
    data class OnLogSavedMeal(val meal: SavedMeal, val mealType: MealType) : FoodEvent
    data class OnDeleteSavedMeal(val id: Long) : FoodEvent
    data class OnDeleteRecipe(val id: Long) : FoodEvent

    /** Opens the copy sheet on a source day, or closes it with null. Loading that day is a read,
     * so it goes through the ViewModel rather than living in the screen's own state — which is
     * also what keeps the open sheet across a rotation. */
    data class OnPickCopySource(val dateEpochDay: Long?) : FoodEvent

    /**
     * Brings the ticked parts of [FoodUiState.copySource] onto the day being shown.
     *
     * ponytail: no undo. `addEntries` hands back no ids, so one would mean widening the repository
     * or deleting by name match; a copied row swipe-deletes like any other. Add it if a mis-copied
     * day turns out to be common.
     */
    data class OnCopyDay(
        val meals: Set<MealType>,
        val water: Boolean,
        val exercise: Boolean,
    ) : FoodEvent
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
    nutrients = nutrients,
)
