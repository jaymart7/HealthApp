package ph.mart.healthapp.feature.profile.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.ScannedProduct
import ph.mart.healthapp.core.designsystem.component.DockedFab
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.library.components.LibrarySearchField
import ph.mart.healthapp.feature.profile.ui.shared.components.FigureRow
import ph.mart.healthapp.feature.profile.ui.shared.components.RowMarker
import ph.mart.healthapp.feature.profile.ui.shared.components.SavedThingRow

/**
 * Everything the user owns in the food domain — their own foods, their saved meals, their recipes —
 * one Nav3 level above Profile, as **one list**: A→Z, a chip per kind to narrow it, and a search
 * behind an icon. It is the only screen that can reach past the newest-N windows the add-entry
 * sheet's panels read.
 *
 * A row opens the thing itself — `:feature:food`'s add-and-edit screen, where a recipe's
 * ingredients can be seen and changed, a food's figures corrected, and either renamed or deleted.
 * Nothing here logs: logging needs a meal slot and a day, which Profile has neither of.
 *
 * Add is one door now: the same screen, opened on its AI box, which works out whether what was
 * described is a food or a recipe. This module cannot import that screen, so [onAdd],
 * [onOpenSavedMeal] and [onOpenFood] are resolved in `AppScaffold`. Saved meals cannot be added
 * here — one is a copy of a diary section, and there is no diary here to copy — but they open and
 * edit like the rest. `DECISIONS.md` → **Saved meals, recipes & the food library** has the calls.
 */
@Composable
fun FoodLibraryScreen(
    onAdd: () -> Unit,
    onOpenSavedMeal: (Long) -> Unit,
    onOpenFood: (String) -> Unit,
    viewModel: FoodLibraryViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    FoodLibraryContent(
        uiState = uiState,
        onAdd = onAdd,
        onOpenSavedMeal = onOpenSavedMeal,
        onOpenFood = onOpenFood,
    )
}

@Composable
private fun FoodLibraryContent(
    uiState: FoodLibraryUiState,
    onAdd: () -> Unit,
    onOpenSavedMeal: (Long) -> Unit,
    onOpenFood: (String) -> Unit,
    initialQuery: String? = null,
) {
    // UI-only and saveable: none of it is persisted, reaches the ViewModel or reaches Room. A null
    // query is search closed; "" is search open with nothing typed yet.
    var filter by rememberSaveable { mutableStateOf(LibraryFilter.All) }
    var query by rememberSaveable { mutableStateOf(initialQuery) }

    // Search is a level of its own: back closes it before it leaves the screen.
    if (query != null) {
        val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(state = navigationState, onBackCompleted = { query = null })
    }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!uiState.loaded) {
                // No chips and no search: a box that can only ever return nothing is chrome. Add
                // below is the call to action, and the body names it.
                FullScreenState(
                    icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
                    heading = stringResource(R.string.profile_library_empty_heading),
                    body = stringResource(R.string.profile_library_empty_body),
                )
            } else {
                // A chip whose kind emptied out (the last recipe deleted) no longer draws, so the
                // list falls back to All rather than to a filter nobody can see or undo.
                val filters = uiState.filters()
                val shown = if (filter in filters) filter else LibraryFilter.All
                val entries = remember(uiState, shown, query) { uiState.entries(shown, query.orEmpty()) }
                Column(modifier = Modifier.fillMaxSize()) {
                    val barModifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 12.dp)
                    val current = query
                    if (current != null) {
                        LibrarySearchField(
                            value = current,
                            onValueChange = { query = it },
                            onClose = { query = null },
                            modifier = barModifier,
                        )
                    } else {
                        FilterBar(
                            filters = filters,
                            selected = shown,
                            onSelect = { filter = it },
                            onSearch = { query = "" },
                            modifier = barModifier,
                        )
                    }
                    LibraryList(
                        entries = entries,
                        query = query.orEmpty(),
                        onOpenSavedMeal = onOpenSavedMeal,
                        onOpenFood = onOpenFood,
                    )
                }
            }
            DockedFab(
                onClick = onAdd,
                label = stringResource(R.string.profile_library_add),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            )
        }
    }
}

/** The chips, and the search icon at the row's end. The chips scroll sideways rather than wrap, so
 * the bar is one row at any font scale and the icon never leaves its corner. */
@Composable
private fun FilterBar(
    filters: List<LibraryFilter>,
    selected: LibraryFilter,
    onSelect: (LibraryFilter) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
        ) {
            filters.forEach { filter ->
                FilterChip(
                    selected = filter == selected,
                    onClick = { onSelect(filter) },
                    label = { Text(stringResource(filter.label())) },
                )
            }
        }
        IconButton(onClick = onSearch) {
            Icon(
                imageVector = AppIcons.Search,
                contentDescription = stringResource(R.string.profile_library_search),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun LibraryFilter.label(): Int = when (this) {
    LibraryFilter.All -> R.string.profile_library_filter_all
    LibraryFilter.Foods -> R.string.profile_library_filter_foods
    LibraryFilter.Recipes -> R.string.profile_library_filter_recipes
    LibraryFilter.Meals -> R.string.profile_library_filter_meals
}

@Composable
private fun LibraryList(
    entries: List<LibraryEntry>,
    query: String,
    onOpenSavedMeal: (Long) -> Unit,
    onOpenFood: (String) -> Unit,
) {
    // Lazy, unlike the other Profile lists: `observeAllSavedMeals` and `observeAllRecipes` are
    // unbounded, so "how many rows can there be" has no answer, and a scrolling Column would compose
    // every one of them on open. `FoodHistoryScreen` is the same call.
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        // The last row comes to rest clear of the FAB rather than under it.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp + DockedFabContentPadding),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (entries.isEmpty()) {
            item(key = "none") {
                Text(
                    text = stringResource(R.string.profile_library_no_matches),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp),
                )
            }
        }
        items(entries, key = { it.key }) { entry ->
            when (entry) {
                is LibraryEntry.Food -> SavedThingRow(
                    name = entry.name,
                    highlight = query,
                    marker = { RowMarker(icon = AppIcons.Egg, contentDescription = null) },
                    figures = { FigureRow(*entry.food.figures().toTypedArray()) },
                    onClick = { onOpenFood(entry.food.name) },
                )
                is LibraryEntry.Dish -> SavedThingRow(
                    name = entry.name,
                    highlight = query,
                    marker = { RowMarker(icon = AppIcons.Book, contentDescription = null) },
                    figures = { FigureRow(*entry.recipe.figures().toTypedArray()) },
                    onClick = { onOpenSavedMeal(entry.recipe.id) },
                )
                is LibraryEntry.Meal -> SavedThingRow(
                    name = entry.name,
                    highlight = query,
                    marker = { RowMarker(icon = AppIcons.Food.outlined, contentDescription = null) },
                    figures = { FigureRow(*entry.meal.figures().toTypedArray()) },
                    onClick = { onOpenSavedMeal(entry.meal.id) },
                )
            }
        }
    }
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
    AppTheme { FoodLibraryContent(uiState = PREVIEW_STATE, onAdd = {}, onOpenSavedMeal = {}, onOpenFood = {}) }
}

/** Searching: the chips give way to the field, and the matched run is marked. */
@PreviewLightDark
@Composable
private fun FoodLibrarySearchPreview() {
    AppTheme {
        FoodLibraryContent(
            uiState = PREVIEW_STATE,
            onAdd = {},
            onOpenSavedMeal = {},
            onOpenFood = {},
            initialQuery = "ch",
        )
    }
}

/** Nothing saved: the row in Profile still opens, so this state has to say what to do next — and
 * Add is how. */
@PreviewLightDark
@Composable
private fun FoodLibraryEmptyPreview() {
    AppTheme {
        FoodLibraryContent(uiState = FoodLibraryUiState(), onAdd = {}, onOpenSavedMeal = {}, onOpenFood = {})
    }
}
