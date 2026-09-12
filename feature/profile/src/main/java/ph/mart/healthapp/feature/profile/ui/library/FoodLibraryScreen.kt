package ph.mart.healthapp.feature.profile.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.ScannedProduct
import ph.mart.healthapp.core.designsystem.component.DiscardConfirmDialog
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.shared.components.LibraryRow
import ph.mart.healthapp.feature.profile.ui.shared.components.RenameSheet

/**
 * Everything the user owns in the food domain — their own foods, their saved meals, their recipes —
 * one Nav3 level above Profile. It is the only screen that can reach past the newest-N windows the
 * add-entry sheet's panels read: without it a sixth saved meal, or a starred food that has slipped
 * out of the suggestion panel, is out of view *and* out of reach of its own delete button.
 *
 * Rename and delete only. Logging needs a meal slot and a day, and Profile has neither — which is
 * also why the foods list here can't be edited field by field: that is the add-entry sheet's form,
 * and saving the same name again is what corrects one.
 */
@Composable
fun FoodLibraryScreen(
    viewModel: FoodLibraryViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    FoodLibraryContent(uiState = uiState, onEvent = viewModel::handleEvent)
}

/** What a confirm dialog or the rename sheet is currently pointed at. Local rather than in a
 * `rememberSaveable` holder for the same reason `FoodScreen` keeps its own: a dialog that survives
 * process death would reopen asking about a row the user has stopped looking at. */
private sealed interface Target {
    val name: String

    /** A food has no id — its name *is* its key in `favorite_food`, which is why renaming one is
     * a move rather than an update. */
    data class Food(override val name: String) : Target
    data class Meal(val id: Long, override val name: String) : Target
    data class Dish(val id: Long, override val name: String) : Target
}

@Composable
private fun FoodLibraryContent(
    uiState: FoodLibraryUiState,
    onEvent: (FoodLibraryEvent) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<Target?>(null) }
    var renaming by remember { mutableStateOf<Target?>(null) }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        if (!uiState.loaded) {
            FullScreenState(
                icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
                heading = stringResource(R.string.profile_library_empty_heading),
                body = stringResource(R.string.profile_library_empty_body),
            )
            return@Surface
        }
        val myFoodsLabel = stringResource(R.string.profile_library_my_foods)
        val savedMealsLabel = stringResource(R.string.profile_library_saved_meals)
        val recipesLabel = stringResource(R.string.profile_library_recipes)
        // Lazy, unlike the other Profile lists: this screen is the one that reads *past* the
        // newest-N windows — `observeAllSavedMeals` and `observeAllRecipes` are unbounded — so
        // "how many rows can there be" has no answer, and a scrolling Column would compose every
        // one of them on open and keep them composed. `FoodHistoryScreen` is the same call.
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // First: it is the list the user authored deliberately, and the one the food search
            // leads with.
            librarySection(
                label = myFoodsLabel,
                rows = uiState.myFoods,
                first = true,
                key = { "food-${it.name}" },
            ) { food ->
                LibraryRow(
                    name = food.name,
                    summary = food.summary(),
                    contents = food.macroLine(),
                    onRename = { renaming = Target.Food(food.name) },
                    onDelete = { pendingDelete = Target.Food(food.name) },
                )
            }
            librarySection(
                label = savedMealsLabel,
                rows = uiState.savedMeals,
                first = uiState.myFoods.isEmpty(),
                key = { "meal-${it.id}" },
            ) { meal ->
                LibraryRow(
                    name = meal.name,
                    summary = meal.summary(),
                    contents = meal.items.contents(),
                    onRename = { renaming = Target.Meal(meal.id, meal.name) },
                    onDelete = { pendingDelete = Target.Meal(meal.id, meal.name) },
                )
            }
            librarySection(
                label = recipesLabel,
                rows = uiState.recipes,
                first = uiState.myFoods.isEmpty() && uiState.savedMeals.isEmpty(),
                key = { "recipe-${it.id}" },
            ) { recipe ->
                LibraryRow(
                    name = recipe.name,
                    summary = recipe.summary(),
                    contents = recipe.items.contents(),
                    onRename = { renaming = Target.Dish(recipe.id, recipe.name) },
                    onDelete = { pendingDelete = Target.Dish(recipe.id, recipe.name) },
                )
            }
        }
    }

    // A saved meal is something the user built, and its delete sits beside the rename — so it asks
    // first, rather than deleting with an undo the way a swiped diary row does.
    pendingDelete?.let { target ->
        val noun = when (target) {
            is Target.Food -> stringResource(R.string.profile_library_noun_food)
            is Target.Meal -> stringResource(R.string.profile_library_noun_meal)
            is Target.Dish -> stringResource(R.string.profile_library_noun_recipe)
        }
        DiscardConfirmDialog(
            title = stringResource(R.string.profile_delete_title, target.name),
            body = stringResource(R.string.profile_library_delete_body, noun),
            confirmLabel = stringResource(R.string.profile_delete),
            dismissLabel = stringResource(R.string.profile_keep),
            onConfirm = {
                onEvent(
                    when (target) {
                        is Target.Food -> FoodLibraryEvent.OnDeleteMyFood(target.name)
                        is Target.Meal -> FoodLibraryEvent.OnDeleteSavedMeal(target.id)
                        is Target.Dish -> FoodLibraryEvent.OnDeleteRecipe(target.id)
                    },
                )
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    renaming?.let { target ->
        RenameSheet(
            currentName = target.name,
            onDismiss = { renaming = null },
            onRename = { name ->
                onEvent(
                    when (target) {
                        is Target.Food -> FoodLibraryEvent.OnRenameMyFood(target.name, name)
                        is Target.Meal -> FoodLibraryEvent.OnRenameSavedMeal(target.id, name)
                        is Target.Dish -> FoodLibraryEvent.OnRenameRecipe(target.id, name)
                    },
                )
                renaming = null
            },
        )
    }
}

/**
 * A header and its rows, or nothing at all when the list is empty — the three sections differ only
 * in what they draw, so the emptiness check and the spacing live here once.
 *
 * [first] is what keeps the spacing scale honest: the list's own arrangement puts 8dp between every
 * item, and a section break is 24, so every header but the leading one owes another 16. The leading
 * one owes nothing — the content padding is already 16 above it.
 *
 * [key] is per-row and prefixed, because saved meals and recipes are the same table and so share an
 * id space; without the prefix a meal and a recipe could collide and Lazy would reuse the wrong
 * slot.
 */
private fun <T> LazyListScope.librarySection(
    label: String,
    rows: List<T>,
    first: Boolean,
    key: (T) -> Any,
    row: @Composable (T) -> Unit,
) {
    if (rows.isEmpty()) return
    item(key = "header-$label") {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = if (first) Modifier else Modifier.padding(top = 16.dp),
        )
    }
    items(rows, key = key) { row(it) }
}

@PreviewLightDark
@Composable
private fun FoodLibraryPreview() {
    AppTheme {
        FoodLibraryContent(
            uiState = FoodLibraryUiState(
                myFoods = listOf(
                    ScannedProduct("Mum's adobo", 1.0, "serving", 420, 28, 12, 28),
                    ScannedProduct("Whey, chocolate", 30.0, "g", 120, 24, 3, 1),
                ),
                savedMeals = listOf(
                    SavedMeal(
                        id = 1,
                        name = "Usual breakfast",
                        items = listOf(
                            SavedMealItem("Greek yogurt", 1.0, "cup", 150, 20, 8, 4),
                            SavedMealItem("Oats", 60.0, "g", 230, 8, 40, 4),
                        ),
                    ),
                    SavedMeal(
                        id = 2,
                        name = "Post-gym shake",
                        items = listOf(SavedMealItem("Whey shake", 1.0, "scoop", 120, 24, 3, 1)),
                    ),
                ),
                recipes = listOf(
                    Recipe(
                        id = 3,
                        name = "Chili",
                        servings = 4,
                        items = listOf(
                            SavedMealItem("Beef mince", 500.0, "g", 1100, 100, 0, 80),
                            SavedMealItem("Kidney beans", 400.0, "g", 380, 24, 60, 2),
                        ),
                    ),
                ),
            ),
            onEvent = {},
        )
    }
}

/** Nothing saved: the row in Profile still opens, so this state has to say what to do next. */
@PreviewLightDark
@Composable
private fun FoodLibraryEmptyPreview() {
    AppTheme { FoodLibraryContent(uiState = FoodLibraryUiState(), onEvent = {}) }
}
