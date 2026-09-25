package ph.mart.healthapp.feature.food.ui.library

import androidx.annotation.StringRes
import ph.mart.healthapp.core.data.food.RecipeParseResult
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm

/** The default a new recipe opens on — one portion is the honest "I haven't said yet" value, and
 * it makes the per-serving summary read as the ingredient totals until the user says otherwise. */
internal const val DEFAULT_SERVINGS = 1

/** Which of the library's three things is being edited. It picks the review's shape and the write;
 * it is persisted nowhere — a food is a `favorite_food` row, and a recipe is told from a saved meal
 * by whether it has a yield. */
enum class LibraryKind { Food, Recipe, Meal }

/** Describe is the AI box, and only a new item has one; Review is the form, which is where an
 * existing item opens. */
enum class LibraryStep { Describe, Review }

/**
 * What the user is editing — a food's fields or a recipe's, whichever [kind] says gets saved.
 * A food keeps its name inside [food], the same `AddEntryForm` every other food form in this module
 * draws; [name] is a recipe's or a saved meal's.
 */
data class LibraryItemForm(
    val kind: LibraryKind = LibraryKind.Recipe,
    val name: String = "",
    val servings: Int = DEFAULT_SERVINGS,
    val ingredients: List<SavedMealItem> = emptyList(),
    val food: AddEntryForm = AddEntryForm(),
) {
    /** Anything typed or filled at all — the difference between "back" and "discard this?". */
    fun hasContent(): Boolean = name.isNotBlank() || ingredients.isNotEmpty() || food != AddEntryForm()
}

/** [original] is the saved item as it was opened, so back can tell an edit from a look. Null on a
 * new item, and until the read lands. */
data class LibraryItemUiState(
    val filling: Boolean = false,
    val original: LibraryItemForm? = null,
)

/**
 * A model's answer as a form to check. A food comes back as one item for one portion, and is named
 * for what the user called it before the model's ingredient name — "Mum's adobo", not "Pork adobo".
 */
fun RecipeParseResult.Parsed.toForm(): LibraryItemForm {
    if (!isFood) {
        return LibraryItemForm(kind = LibraryKind.Recipe, name = name, servings = servings, ingredients = items)
    }
    val item = items.single()
    return LibraryItemForm(
        kind = LibraryKind.Food,
        food = AddEntryForm(
            name = name.ifBlank { item.name },
            portionAmount = item.portionAmount,
            portionUnit = item.portionUnit,
            calories = item.calories,
            proteinG = item.proteinG,
            carbsG = item.carbsG,
            fatG = item.fatG,
            nutrients = item.nutrients,
        ),
    )
}

/** The identity of what was opened rides every write that needs it — `LogExerciseEvent.OnSave`'s
 * `editingId` shape — rather than living in the ViewModel: null and null is a new item. */
sealed interface LibraryItemEvent {
    data class OnOpen(val savedMealId: Long?, val foodName: String?) : LibraryItemEvent
    data class OnFill(val text: String) : LibraryItemEvent
    data object OnCancelFill : LibraryItemEvent
    data class OnSave(val form: LibraryItemForm, val savedMealId: Long?, val foodName: String?) : LibraryItemEvent
    data class OnDelete(val savedMealId: Long?, val foodName: String?) : LibraryItemEvent
}

sealed interface LibraryItemSideEffect {
    data class Loaded(val form: LibraryItemForm) : LibraryItemSideEffect
    data class Filled(val form: LibraryItemForm) : LibraryItemSideEffect
    data class FillFailed(@StringRes val message: Int) : LibraryItemSideEffect

    /** Saved, deleted, or gone before it could be opened — each one leaves the screen. */
    data object Done : LibraryItemSideEffect
}
