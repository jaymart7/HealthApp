package ph.mart.healthapp.feature.food.ui.diary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealIdea
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.toAddEntryForm

@Composable
internal fun rememberFoodScreenState(): FoodScreenState =
    rememberSaveable(saver = FoodScreenState.Saver()) { FoodScreenState() }

/** Screen-local UI state — which meal sheet is open, its in-progress form, section expand/collapse,
 * and the diary search query. None of it has meaning outside this screen, per the skill's
 * "UI-only flag" rule; the loaded diary itself lives in [FoodUiState] (the Orbit container). */
internal class FoodScreenState(
    activeMealSheet: MealType? = null,
    addForm: AddEntryForm = AddEntryForm(),
    searchQuery: String = "",
    filterExpanded: Boolean = false,
    expandedMeals: Map<MealType, Boolean> = MealType.entries.associateWith { true },
    exerciseSheetOpen: Boolean = false,
    exerciseExpanded: Boolean = true,
    calendarOpen: Boolean = false,
    saveMealFor: MealType? = null,
    savedMealName: String = "",
    editingEntryId: Long? = null,
    editingExerciseId: Long? = null,
    ideasFor: MealType? = null,
) {
    var activeMealSheet: MealType? by mutableStateOf(activeMealSheet)
    var addForm: AddEntryForm by mutableStateOf(addForm)
    var searchQuery: String by mutableStateOf(searchQuery)

    /** Whether the date header is showing the filter field instead of the date controls.
     * Closing it clears [searchQuery] through [closeFilter] — a filter you can no longer see is
     * one you will not remember is hiding rows. */
    var filterExpanded: Boolean by mutableStateOf(filterExpanded)
    var expandedMeals: Map<MealType, Boolean> by mutableStateOf(expandedMeals)
    var exerciseSheetOpen: Boolean by mutableStateOf(exerciseSheetOpen)
    var exerciseExpanded: Boolean by mutableStateOf(exerciseExpanded)
    var calendarOpen: Boolean by mutableStateOf(calendarOpen)

    /** Which meal the ideas overlay is suggesting for — null when it's closed. It replaces the
     * sheet rather than sitting over it: the sheet is where a picked idea comes back to. */
    var ideasFor: MealType? by mutableStateOf(ideasFor)

    /**
     * Which logged row each sheet is *correcting* rather than adding to — null means a new entry.
     *
     * Ids rather than the rows themselves: the screen resolves them back off the loaded day, which
     * keeps this saveable across a rotation without teaching the saver two more record shapes.
     */
    var editingEntryId: Long? by mutableStateOf(editingEntryId)
    var editingExerciseId: Long? by mutableStateOf(editingExerciseId)

    /** Which meal section's entries the "save this meal" sheet is naming, and the name so far. */
    var saveMealFor: MealType? by mutableStateOf(saveMealFor)
    var savedMealName: String by mutableStateOf(savedMealName)

    fun openSheet(mealType: MealType) {
        addForm = AddEntryForm(mealType = mealType)
        editingEntryId = null
        activeMealSheet = mealType
    }

    /** The same sheet, seeded from a row that already exists — it saves over that row. */
    fun openEditSheet(entry: FoodEntry) {
        addForm = entry.toAddEntryForm()
        editingEntryId = entry.id
        activeMealSheet = entry.mealType
    }

    fun closeSheet() {
        activeMealSheet = null
        editingEntryId = null
    }

    /** Straight from the add-entry sheet, which closes behind it — the handover "New recipe" and
     * "Log sets instead" both make, so back from the overlay lands on the diary rather than
     * reopening a form the user has walked away from. */
    fun openIdeas(mealType: MealType) {
        closeSheet()
        ideasFor = mealType
    }

    fun closeIdeas() {
        ideasFor = null
    }

    /**
     * An idea is a seed, never a row: it reopens the sheet it came from with the fields filled in,
     * where the portion stepper reprices it and Add commits it. That is the same landing a recipe,
     * a recent and a search hit already have, which is why picking one writes nothing.
     */
    fun selectIdea(idea: MealIdea) {
        val mealType = ideasFor ?: return
        ideasFor = null
        addForm = idea.toAddEntryForm(mealType)
        editingEntryId = null
        activeMealSheet = mealType
    }

    fun openExerciseSheet(entry: ExerciseEntry? = null) {
        editingExerciseId = entry?.id
        exerciseSheetOpen = true
    }

    fun closeExerciseSheet() {
        exerciseSheetOpen = false
        editingExerciseId = null
    }

    fun openSaveMealSheet(mealType: MealType) {
        savedMealName = mealType.name
        saveMealFor = mealType
    }

    fun closeSaveMealSheet() {
        saveMealFor = null
    }

    fun closeFilter() {
        filterExpanded = false
        searchQuery = ""
    }

    fun toggleExpanded(mealType: MealType) {
        expandedMeals = expandedMeals + (mealType to (expandedMeals[mealType] != true))
    }

    companion object {
        fun Saver(): Saver<FoodScreenState, Any> = listSaver(
            save = {
                val f = it.addForm
                listOf(
                    it.activeMealSheet?.name, it.searchQuery,
                    f.mealType.name, f.name, f.portionAmount, f.portionUnit,
                    f.calories, f.proteinG, f.carbsG, f.fatG,
                ) + MealType.entries.map { m -> it.expandedMeals[m] != false } +
                    listOf(
                        it.exerciseSheetOpen, it.exerciseExpanded, it.calendarOpen,
                        it.saveMealFor?.name, it.savedMealName,
                        it.editingEntryId, it.editingExerciseId, it.ideasFor?.name,
                        // Appended, never inserted: every index below is positional, so a new
                        // field in the middle would silently re-point all of them.
                        it.filterExpanded,
                    )
            },
            restore = { saved ->
                FoodScreenState(
                    activeMealSheet = (saved[0] as String?)?.let(MealType::valueOf),
                    searchQuery = saved[1] as String,
                    addForm = AddEntryForm(
                        mealType = MealType.valueOf(saved[2] as String),
                        name = saved[3] as String,
                        portionAmount = saved[4] as Double,
                        portionUnit = saved[5] as String,
                        calories = saved[6] as Int,
                        proteinG = saved[7] as Int,
                        carbsG = saved[8] as Int,
                        fatG = saved[9] as Int,
                    ),
                    expandedMeals = MealType.entries.mapIndexed { index, m -> m to (saved[10 + index] as Boolean) }.toMap(),
                    exerciseSheetOpen = saved[10 + MealType.entries.size] as Boolean,
                    exerciseExpanded = saved[11 + MealType.entries.size] as Boolean,
                    calendarOpen = saved[12 + MealType.entries.size] as Boolean,
                    saveMealFor = (saved[13 + MealType.entries.size] as String?)?.let(MealType::valueOf),
                    savedMealName = saved[14 + MealType.entries.size] as String,
                    editingEntryId = saved[15 + MealType.entries.size] as Long?,
                    editingExerciseId = saved[16 + MealType.entries.size] as Long?,
                    ideasFor = (saved[17 + MealType.entries.size] as String?)?.let(MealType::valueOf),
                    filterExpanded = saved[18 + MealType.entries.size] as Boolean,
                )
            },
        )
    }
}
