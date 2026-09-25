package ph.mart.healthapp.feature.food.ui.library

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.RecipeParseRepository
import ph.mart.healthapp.core.data.food.RecipeParseResult
import ph.mart.healthapp.core.data.network.NetworkMonitor
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.toAddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.toSuggestion

/**
 * Adds to the food library and edits what is in it — a food, a recipe or a saved meal, one screen.
 * A new item starts on the AI box; [RecipeParseRepository] says whether what was described is a
 * food or a recipe, and the review that follows is shaped by the answer.
 */
class LibraryItemViewModel(
    private val foodRepository: FoodRepository,
    private val recipeParseRepository: RecipeParseRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel(), OrbitContainerHost<LibraryItemUiState, LibraryItemUiState, LibraryItemSideEffect> {

    override val container = orbitContainer<LibraryItemUiState, LibraryItemSideEffect>(LibraryItemUiState())

    /** Cancelled by back and by the stop button — `VoiceLogViewModel.parseJob`'s reason. */
    private var fillJob: Job? = null

    fun handleEvent(event: LibraryItemEvent) {
        when (event) {
            is LibraryItemEvent.OnOpen -> onOpen(event.savedMealId, event.foodName)
            is LibraryItemEvent.OnFill -> onFill(event.text)
            LibraryItemEvent.OnCancelFill -> onCancelFill()
            is LibraryItemEvent.OnSave -> onSave(event.form, event.savedMealId, event.foodName)
            is LibraryItemEvent.OnDelete -> onDelete(event.savedMealId, event.foodName)
        }
    }

    // ponytail: scans the whole library per open — a by-id DAO read if the lists ever get large.
    private fun onOpen(savedMealId: Long?, foodName: String?) = intent {
        val form = when {
            savedMealId != null ->
                foodRepository.observeAllRecipes().first().find { it.id == savedMealId }
                    ?.let { LibraryItemForm(LibraryKind.Recipe, it.name, it.servings, it.items) }
                    ?: foodRepository.observeAllSavedMeals().first().find { it.id == savedMealId }
                        ?.let { LibraryItemForm(LibraryKind.Meal, it.name, ingredients = it.items) }

            foodName != null ->
                foodRepository.observeMyFoods().first().find { it.name == foodName }
                    // The slot is carried and ignored: a food the user owns belongs to no meal.
                    ?.let { LibraryItemForm(LibraryKind.Food, food = it.toAddEntryForm(MealType.Breakfast)) }

            else -> null
        }
        if (form == null) {
            // Deleted from under the route — there is nothing to show, so there is nowhere to stay.
            postSideEffect(LibraryItemSideEffect.Done)
            return@intent
        }
        reduce { state.copy(original = form) }
        postSideEffect(LibraryItemSideEffect.Loaded(form))
    }

    /** `SupplementsViewModel.onLookUp`'s shape: the online check is asked at the moment a request
     * is about to be spent, and each way it can fail is a sentence under the field. */
    private fun onFill(text: String) {
        if (text.isBlank() || fillJob?.isActive == true) return
        fillJob = intent {
            if (!networkMonitor.isOnline()) {
                postSideEffect(LibraryItemSideEffect.FillFailed(R.string.food_library_fill_offline))
                return@intent
            }
            reduce { state.copy(filling = true) }
            val result = recipeParseRepository.parse(text.trim())
            reduce { state.copy(filling = false) }
            postSideEffect(
                when (result) {
                    is RecipeParseResult.Parsed -> LibraryItemSideEffect.Filled(result.toForm())
                    RecipeParseResult.NothingFound ->
                        LibraryItemSideEffect.FillFailed(R.string.food_library_fill_nothing)

                    RecipeParseResult.Failed -> LibraryItemSideEffect.FillFailed(R.string.food_recipe_fill_failed)
                },
            )
        }
    }

    /** A cancelled intent never reaches its own `filling = false`, so the reset is its own. */
    private fun onCancelFill() {
        fillJob?.cancel()
        intent { reduce { state.copy(filling = false) } }
    }

    /**
     * A food goes through "Save as my food"'s own write, so a food made here and one kept from the
     * add-entry sheet cannot differ; a renamed one is moved first, because its name is its key.
     * A recipe or a saved meal is replaced whole when it already exists — see
     * [FoodRepository.updateSavedMeal] — and a yield is what keeps a recipe one.
     */
    private fun onSave(form: LibraryItemForm, savedMealId: Long?, foodName: String?) = intent {
        if (form.kind == LibraryKind.Food) {
            val food = form.food.toSuggestion()
            if (foodName != null && foodName != food.name) foodRepository.renameMyFood(foodName, food.name)
            foodRepository.setFavorite(food, favorite = true)
        } else {
            val name = form.name.trim()
            val servings = form.servings.coerceAtLeast(1).takeIf { form.kind == LibraryKind.Recipe }
            when {
                savedMealId != null -> foodRepository.updateSavedMeal(savedMealId, name, servings, form.ingredients)
                servings != null -> foodRepository.saveRecipe(name, servings, form.ingredients)
                else -> foodRepository.saveMeal(name, form.ingredients)
            }
        }
        postSideEffect(LibraryItemSideEffect.Done)
    }

    /** Soft deletes, both — anything already logged from the item stays in the diary. */
    private fun onDelete(savedMealId: Long?, foodName: String?) = intent {
        when {
            savedMealId != null -> foodRepository.deleteSavedMeal(savedMealId)
            foodName != null -> foodRepository.deleteMyFood(foodName)
        }
        postSideEffect(LibraryItemSideEffect.Done)
    }
}
