package ph.mart.healthapp.feature.food.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.food.Nutrients
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.isSaveableFood

@Composable
internal fun rememberLibraryItemState(isNew: Boolean): LibraryItemState =
    rememberSaveable(saver = LibraryItemState.Saver()) {
        LibraryItemState(step = if (isNew) LibraryStep.Describe else LibraryStep.Review)
    }

/** [LibraryItemState.editingIndex] for an ingredient that is not in the list yet. */
internal const val NEW_INGREDIENT = -1

/** The one confirm that can be up at a time. */
internal enum class LibraryDialog { Discard, Replace, Delete }

/**
 * The whole of the screen's draft — UI-only, the `FoodScreenState` rule: nothing reaches Room until
 * Save, so it all has to survive a rotation on its own.
 *
 * [loaded] is what stops an existing item's read from landing twice. The screen asks for it on every
 * composition that has not seen it yet; after a rotation or a process death the form is already
 * here, and the edit in it is the user's.
 */
internal class LibraryItemState(
    step: LibraryStep = LibraryStep.Describe,
    form: LibraryItemForm = LibraryItemForm(),
    description: String = "",
    fromAi: Boolean = false,
    loaded: Boolean = false,
    draft: SavedMealItem? = null,
    editingIndex: Int = NEW_INGREDIENT,
    dialog: LibraryDialog? = null,
) {
    var step: LibraryStep by mutableStateOf(step)
    var form: LibraryItemForm by mutableStateOf(form)

    /** What the AI box holds. Kept after a fill, so a correction is back, an edit and a resend. */
    var description: String by mutableStateOf(description)

    /** The review wears the AI chip — every figure on it may be the model's. */
    var fromAi: Boolean by mutableStateOf(fromAi)
    var loaded: Boolean by mutableStateOf(loaded)

    /** The ingredient in the sheet; the sheet is up while this is non-null. */
    var draft: SavedMealItem? by mutableStateOf(draft)

    /** Where [draft] goes back to on Done — [NEW_INGREDIENT] appends it. */
    var editingIndex: Int by mutableIntStateOf(editingIndex)
    var dialog: LibraryDialog? by mutableStateOf(dialog)

    /** Why the last fill didn't land. Not saved — it answers a tap, and a rotation later it is stale. */
    var fillError: Int? by mutableStateOf(null)

    val canSave: Boolean
        get() = when (form.kind) {
            LibraryKind.Food -> form.food.isSaveableFood()
            else -> form.name.isNotBlank() && form.ingredients.isNotEmpty()
        }

    fun applyFill(filled: LibraryItemForm) {
        form = filled
        fromAi = true
        step = LibraryStep.Review
    }

    /** The two manual doors under the AI box — and the whole of the offline path. */
    fun startManual(kind: LibraryKind) {
        form = LibraryItemForm(kind = kind)
        fromAi = false
        step = LibraryStep.Review
    }

    fun applyLoaded(loadedForm: LibraryItemForm) {
        if (loaded) return
        form = loadedForm
        loaded = true
    }

    fun addIngredient() {
        draft = emptyIngredient()
        editingIndex = NEW_INGREDIENT
    }

    /** Edits in place: the row stays in the list until Done replaces it, so dismissing the sheet
     * loses nothing. */
    fun openIngredient(index: Int) {
        draft = form.ingredients.getOrNull(index) ?: return
        editingIndex = index
    }

    fun commitDraft() {
        val item = draft?.takeIf { it.name.isNotBlank() } ?: return
        val list = form.ingredients
        form = form.copy(
            ingredients = if (editingIndex in list.indices) {
                list.mapIndexed { i, old -> if (i == editingIndex) item else old }
            } else {
                list + item
            },
        )
        draft = null
    }

    fun removeIngredient(index: Int) {
        form = form.copy(ingredients = form.ingredients.filterIndexed { i, _ -> i != index })
    }

    companion object {
        /** The scalar fields ahead of the food, the draft and the list — one per entry in `save`'s
         * first list. */
        private const val HEADER_FIELDS = 11

        /**
         * Flattened primitives, restored in the same order — a `listSaver` can only hold those, and
         * a saveable list of data classes would mean making `SavedMealItem` parcelable for one
         * screen's benefit. All seven nutrients ride along: an AI fill carries them, and a rotation
         * that dropped four would drop them from the save too.
         */
        fun Saver(): Saver<LibraryItemState, Any> = listSaver(
            save = { state ->
                val f = state.form
                listOf(
                    state.step.name,
                    f.kind.name,
                    f.name,
                    f.servings,
                    state.description,
                    state.fromAi,
                    state.loaded,
                    state.draft != null,
                    state.editingIndex,
                    state.dialog?.name,
                    f.ingredients.size,
                ) +
                    f.food.flatten() +
                    (state.draft ?: emptyIngredient()).flatten() +
                    f.ingredients.flatMap { it.flatten() }
            },
            restore = { saved ->
                var rest = saved.drop(HEADER_FIELDS)
                val food = rest.take(FOOD_FIELDS).toFood()
                rest = rest.drop(FOOD_FIELDS)
                val draft = rest.take(ITEM_FIELDS).toItem()
                rest = rest.drop(ITEM_FIELDS)
                LibraryItemState(
                    step = LibraryStep.valueOf(saved[0] as String),
                    form = LibraryItemForm(
                        kind = LibraryKind.valueOf(saved[1] as String),
                        name = saved[2] as String,
                        servings = saved[3] as Int,
                        ingredients = rest.chunked(ITEM_FIELDS).take(saved[10] as Int).map { it.toItem() },
                        food = food,
                    ),
                    description = saved[4] as String,
                    fromAi = saved[5] as Boolean,
                    loaded = saved[6] as Boolean,
                    draft = draft.takeIf { saved[7] as Boolean },
                    editingIndex = saved[8] as Int,
                    dialog = (saved[9] as String?)?.let(LibraryDialog::valueOf),
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

private const val NUTRIENT_FIELDS = 7
private const val ITEM_FIELDS = 7 + NUTRIENT_FIELDS
private const val FOOD_FIELDS = 8 + NUTRIENT_FIELDS

private fun Nutrients.flatten(): List<Any> =
    listOf(fiberG, sugarG, sodiumMg, vitaminDUg, calciumMg, ironUg, potassiumMg)

private fun List<Any?>.toNutrients() = Nutrients(
    fiberG = this[0] as Int,
    sugarG = this[1] as Int,
    sodiumMg = this[2] as Int,
    vitaminDUg = this[3] as Int,
    calciumMg = this[4] as Int,
    ironUg = this[5] as Int,
    potassiumMg = this[6] as Int,
)

private fun SavedMealItem.flatten(): List<Any> =
    listOf(name, portionAmount, portionUnit, calories, proteinG, carbsG, fatG) + nutrients.flatten()

private fun List<Any?>.toItem() = SavedMealItem(
    name = this[0] as String,
    portionAmount = this[1] as Double,
    portionUnit = this[2] as String,
    calories = this[3] as Int,
    proteinG = this[4] as Int,
    carbsG = this[5] as Int,
    fatG = this[6] as Int,
    nutrients = drop(7).toNutrients(),
)

/** The fields a food the user owns keeps, plus [AddEntryForm.servingSize] so a restored form still
 * equals the one that was opened. The meal slot, photo and confidence are carried and ignored here —
 * `toSuggestion()` drops them — so they are not saved either. */
private fun AddEntryForm.flatten(): List<Any?> =
    listOf(name, portionAmount, portionUnit, calories, proteinG, carbsG, fatG, servingSize) + nutrients.flatten()

private fun List<Any?>.toFood() = AddEntryForm(
    name = this[0] as String,
    portionAmount = this[1] as Double,
    portionUnit = this[2] as String,
    calories = this[3] as Int?,
    proteinG = this[4] as Int?,
    carbsG = this[5] as Int?,
    fatG = this[6] as Int?,
    servingSize = this[7] as String?,
    nutrients = drop(8).toNutrients(),
)
