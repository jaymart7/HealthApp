package ph.mart.healthapp.feature.profile.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.ScannedProduct
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.library.components.LibrarySearchField
import ph.mart.healthapp.feature.profile.ui.library.components.LibrarySectionHeader
import ph.mart.healthapp.feature.profile.ui.library.components.MacroTriplet
import ph.mart.healthapp.feature.profile.ui.library.components.RecipeYieldPill
import ph.mart.healthapp.feature.profile.ui.shared.components.DeleteConfirmDialog
import ph.mart.healthapp.feature.profile.ui.shared.components.FigureRow
import ph.mart.healthapp.feature.profile.ui.shared.components.RenameSheet
import ph.mart.healthapp.feature.profile.ui.shared.components.RowMarker
import ph.mart.healthapp.feature.profile.ui.shared.components.SavedThingRow

/**
 * Everything the user owns in the food domain — their own foods, their saved meals, their recipes —
 * one Nav3 level above Profile. It is the only screen that can reach past the newest-N windows the
 * add-entry sheet's panels read: without it a sixth saved meal, or a starred food that has slipped
 * out of the suggestion panel, is out of view *and* out of reach of its own delete button.
 *
 * Rename and delete only, and the menu says **Rename** rather than Edit on purpose: logging needs
 * a meal slot and a day, which Profile has neither of, and a food's *fields* are corrected by
 * saving the same name again from the add-entry sheet. The rows are inert — a tap does nothing,
 * because there is nothing here for a tap to mean.
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
    initialQuery: String = "",
) {
    var pendingDelete by remember { mutableStateOf<Target?>(null) }
    var renaming by remember { mutableStateOf<Target?>(null) }
    // The query survives a rotation and nothing else: it is not persisted, never reaches the
    // ViewModel, and never reaches Room. All three lists are already in memory, so filtering is
    // what a keystroke costs — which is also why there is no debounce here and one in the food
    // history's field, where every keystroke is a query.
    var query by rememberSaveable { mutableStateOf(initialQuery) }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        if (!uiState.loaded) {
            // No search field: a box that can only ever return nothing is chrome. And no CTA —
            // this screen has no way to save anything, and the body already names the two that do.
            FullScreenState(
                icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
                heading = stringResource(R.string.profile_library_empty_heading),
                body = stringResource(R.string.profile_library_empty_body),
            )
            return@Surface
        }
        val results = remember(uiState, query) { uiState.filter(query) }
        Column(modifier = Modifier.fillMaxSize()) {
            LibrarySearchField(
                value = query,
                onValueChange = { query = it },
                totalItems = uiState.total,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 12.dp),
            )
            LibraryList(
                results = results,
                query = query,
                total = uiState.total,
                onRename = { renaming = it },
            )
        }
    }

    // A saved meal is something the user built, so its delete asks first rather than deleting with
    // an undo the way a swiped diary row does. Keep is the confirm slot; see DeleteConfirmDialog.
    pendingDelete?.let { target ->
        val noun = when (target) {
            is Target.Food -> stringResource(R.string.profile_library_noun_food)
            is Target.Meal -> stringResource(R.string.profile_library_noun_meal)
            is Target.Dish -> stringResource(R.string.profile_library_noun_recipe)
        }
        DeleteConfirmDialog(
            name = target.name,
            body = stringResource(R.string.profile_library_delete_body, noun),
            onDelete = {
                onEvent(
                    when (target) {
                        is Target.Food -> FoodLibraryEvent.OnDeleteMyFood(target.name)
                        is Target.Meal -> FoodLibraryEvent.OnDeleteSavedMeal(target.id)
                        is Target.Dish -> FoodLibraryEvent.OnDeleteRecipe(target.id)
                    },
                )
                pendingDelete = null
            },
            onKeep = { pendingDelete = null },
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
            onDelete = {
                pendingDelete = target
                renaming = null
            },
        )
    }
}

@Composable
private fun LibraryList(
    results: LibraryResults,
    query: String,
    total: Int,
    onRename: (Target) -> Unit,
) {
    val myFoodsLabel = stringResource(R.string.profile_library_my_foods)
    val savedMealsLabel = stringResource(R.string.profile_library_saved_meals)
    val recipesLabel = stringResource(R.string.profile_library_recipes)
    // Lazy, unlike the other Profile lists: this screen is the one that reads *past* the
    // newest-N windows — `observeAllSavedMeals` and `observeAllRecipes` are unbounded — so
    // "how many rows can there be" has no answer, and a scrolling Column would compose every
    // one of them on open and keep them composed. `FoodHistoryScreen` is the same call.
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (query.isNotBlank()) {
            // The denominator stays in view so a two-hit result never reads as a broken library.
            item(key = "matches") {
                Text(
                    text = pluralStringResource(
                        R.plurals.profile_library_matches,
                        results.total,
                        results.total,
                        total,
                    ),
                    style = MaterialTheme.typography.bodySmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp),
                )
            }
        }
        // First: it is the list the user authored deliberately, and the one the food search
        // leads with.
        librarySection(
            label = myFoodsLabel,
            rows = results.myFoods,
            first = true,
            key = { "food-${it.name}" },
        ) { food ->
            SavedThingRow(
                name = food.name,
                highlight = query,
                marker = { RowMarker(icon = AppIcons.Egg, contentDescription = null) },
                figures = { FigureRow(*food.figures().toTypedArray()) },
                detailContent = {
                    MacroTriplet(proteinG = food.proteinG, carbsG = food.carbsG, fatG = food.fatG)
                },
                // The row is the control now that the overflow menu is gone. These three lists
                // were the one place a row was not tappable — the menu was doing the job — so
                // this is what they gain rather than lose.
                onClick = { onRename(Target.Food(food.name)) },
            )
        }
        librarySection(
            label = savedMealsLabel,
            rows = results.savedMeals,
            first = results.myFoods.isEmpty(),
            key = { "meal-${it.id}" },
        ) { meal ->
            SavedThingRow(
                name = meal.name,
                highlight = query,
                marker = { RowMarker(icon = AppIcons.Food.outlined, contentDescription = null) },
                figures = { FigureRow(*meal.figures().toTypedArray()) },
                detail = meal.items.contents().ifEmpty { null },
                onClick = { onRename(Target.Meal(meal.id, meal.name)) },
            )
        }
        librarySection(
            label = recipesLabel,
            rows = results.recipes,
            first = results.myFoods.isEmpty() && results.savedMeals.isEmpty(),
            key = { "recipe-${it.id}" },
        ) { recipe ->
            SavedThingRow(
                name = recipe.name,
                highlight = query,
                marker = { RowMarker(icon = AppIcons.Book, contentDescription = null) },
                figures = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FigureRow(*recipe.figures().toTypedArray())
                        RecipeYieldPill(
                            servings = recipe.servings,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                },
                detail = recipe.items.contents().ifEmpty { null },
                onClick = { onRename(Target.Dish(recipe.id, recipe.name)) },
            )
        }
    }
}

/**
 * A sticky header and its rows, or nothing at all when the section is empty — which is the same
 * rule whether it was empty to begin with or a search emptied it. A heading over nothing is noise
 * either way.
 *
 * [first] is what keeps the spacing honest: every header but the leading one owes 16dp of air
 * above it, and the leading one owes nothing.
 *
 * [key] is per-row and prefixed, because saved meals and recipes are the same table and so share
 * an id space; without the prefix a meal and a recipe could collide and Lazy would reuse the wrong
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
    stickyHeader(key = "header-$label") {
        LibrarySectionHeader(label = label, count = rows.size, first = first)
    }
    items(rows, key = key) { row(it) }
}

private val PREVIEW_STATE = FoodLibraryUiState(
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
)

@PreviewLightDark
@Composable
private fun FoodLibraryPreview() {
    AppTheme { FoodLibraryContent(uiState = PREVIEW_STATE, onEvent = {}) }
}

/** Searching: the headers re-count, a section with no matches loses its header, and the matched
 * run is marked so the reason a row is in the result is visible. */
@PreviewLightDark
@Composable
private fun FoodLibrarySearchPreview() {
    AppTheme { FoodLibraryContent(uiState = PREVIEW_STATE, onEvent = {}, initialQuery = "oat") }
}

/** Nothing saved: the row in Profile still opens, so this state has to say what to do next. */
@PreviewLightDark
@Composable
private fun FoodLibraryEmptyPreview() {
    AppTheme { FoodLibraryContent(uiState = FoodLibraryUiState(), onEvent = {}) }
}
