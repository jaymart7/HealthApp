package ph.mart.healthapp.feature.food.ui.recipe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.feature.food.ui.diary.FoodScreenState

@Composable
internal fun rememberRecipeBuilderState(): RecipeBuilderState =
    rememberSaveable(saver = RecipeBuilderState.Saver()) { RecipeBuilderState() }

/** The default a new recipe opens on — one portion is the honest "I haven't said yet" value, and
 * it makes the per-serving summary read as the ingredient totals until the user says otherwise. */
internal const val DEFAULT_SERVINGS = 1

/** The whole of the builder's draft — UI-only state, the same rule [FoodScreenState] follows.
 * Nothing here reaches Room until Save, so it all has to survive a rotation on its own. */
internal class RecipeBuilderState(
    name: String = "",
    servings: Int = DEFAULT_SERVINGS,
    ingredients: List<SavedMealItem> = emptyList(),
    draft: SavedMealItem = emptyIngredient(),
    discardOpen: Boolean = false,
) {
    var name: String by mutableStateOf(name)
    var servings: Int by mutableStateOf(servings)
    var ingredients: List<SavedMealItem> by mutableStateOf(ingredients)

    /** The ingredient being typed, before "Add ingredient" moves it into [ingredients]. */
    var draft: SavedMealItem by mutableStateOf(draft)
    var discardOpen: Boolean by mutableStateOf(discardOpen)

    /** Anything typed at all counts — back out of a named-but-empty recipe is still a loss. */
    val isDirty: Boolean
        get() = name.isNotBlank() || ingredients.isNotEmpty() || draft != emptyIngredient()

    val canSave: Boolean
        get() = name.isNotBlank() && ingredients.isNotEmpty()

    val draftIsValid: Boolean
        get() = draft.name.isNotBlank()

    fun addDraft() {
        if (!draftIsValid) return
        ingredients = ingredients + draft
        draft = emptyIngredient()
    }

    fun removeIngredient(index: Int) {
        ingredients = ingredients.filterIndexed { i, _ -> i != index }
    }

    companion object {
        /** Ingredients are flattened [FIELDS_PER_INGREDIENT] values at a time and restored by
         * [chunked] — a `listSaver` can only hold primitives, and a saveable list of data classes
         * would mean making `SavedMealItem` parcelable for one screen's benefit. */
        private const val FIELDS_PER_INGREDIENT = 7

        fun Saver(): Saver<RecipeBuilderState, Any> = listSaver(
            save = { state ->
                listOf(state.name, state.servings, state.discardOpen) +
                    state.draft.flatten() +
                    state.ingredients.flatMap { it.flatten() }
            },
            restore = { saved ->
                val rest = saved.drop(3)
                RecipeBuilderState(
                    name = saved[0] as String,
                    servings = saved[1] as Int,
                    discardOpen = saved[2] as Boolean,
                    draft = rest.take(FIELDS_PER_INGREDIENT).toIngredient(),
                    ingredients = rest.drop(FIELDS_PER_INGREDIENT)
                        .chunked(FIELDS_PER_INGREDIENT)
                        .map { it.toIngredient() },
                )
            },
        )
    }
}

internal fun emptyIngredient() = SavedMealItem(
    name = "",
    portionAmount = 100.0,
    portionUnit = "g",
    calories = 0,
    proteinG = 0,
    carbsG = 0,
    fatG = 0,
)

private fun SavedMealItem.flatten(): List<Any> =
    listOf(name, portionAmount, portionUnit, calories, proteinG, carbsG, fatG)

private fun List<Any?>.toIngredient() = SavedMealItem(
    name = this[0] as String,
    portionAmount = this[1] as Double,
    portionUnit = this[2] as String,
    calories = this[3] as Int,
    proteinG = this[4] as Int,
    carbsG = this[5] as Int,
    fatG = this[6] as Int,
)
