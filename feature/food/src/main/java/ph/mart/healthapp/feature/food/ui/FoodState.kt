package ph.mart.healthapp.feature.food.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.food.MealType

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
    expandedMeals: Map<MealType, Boolean> = MealType.entries.associateWith { true },
) {
    var activeMealSheet: MealType? by mutableStateOf(activeMealSheet)
    var addForm: AddEntryForm by mutableStateOf(addForm)
    var searchQuery: String by mutableStateOf(searchQuery)
    var expandedMeals: Map<MealType, Boolean> by mutableStateOf(expandedMeals)

    fun openSheet(mealType: MealType) {
        addForm = AddEntryForm(mealType = mealType)
        activeMealSheet = mealType
    }

    fun closeSheet() {
        activeMealSheet = null
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
                ) + MealType.entries.map { m -> it.expandedMeals[m] != false }
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
                )
            },
        )
    }
}
