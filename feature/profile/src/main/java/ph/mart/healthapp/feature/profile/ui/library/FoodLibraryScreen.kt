package ph.mart.healthapp.feature.profile.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.designsystem.component.DiscardConfirmDialog
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.ui.library.components.LibraryRow
import ph.mart.healthapp.feature.profile.ui.library.components.RenameSheet

/**
 * Every saved meal and recipe, one Nav3 level above Profile — the only screen that can reach past
 * the newest-[ph.mart.healthapp.core.data.food.MAX_SAVED_MEALS] window the add-entry sheet's panels
 * read. Without it a sixth saved meal is out of view *and* out of reach of its own delete button.
 *
 * Rename and delete only. Logging needs a meal slot and a day, and Profile has neither.
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
    val id: Long
    val name: String

    data class Meal(override val id: Long, override val name: String) : Target
    data class Dish(override val id: Long, override val name: String) : Target
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
                heading = "Nothing saved yet",
                body = "Save a meal from the diary, or build a recipe, and it shows up here.",
            )
            return@Surface
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = DockedFabContentPadding),
        ) {
            Text(
                text = "Saved meals & recipes",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (uiState.savedMeals.isNotEmpty()) {
                LibrarySection(label = "Saved meals") {
                    uiState.savedMeals.forEach { meal ->
                        LibraryRow(
                            name = meal.name,
                            summary = meal.summary(),
                            contents = meal.items.contents(),
                            onRename = { renaming = Target.Meal(meal.id, meal.name) },
                            onDelete = { pendingDelete = Target.Meal(meal.id, meal.name) },
                        )
                    }
                }
            }
            if (uiState.recipes.isNotEmpty()) {
                LibrarySection(label = "Recipes") {
                    uiState.recipes.forEach { recipe ->
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
        }
    }

    // A saved meal is something the user built, and its delete sits beside the rename — so it asks
    // first, rather than deleting with an undo the way a swiped diary row does.
    pendingDelete?.let { target ->
        val noun = if (target is Target.Meal) "saved meal" else "recipe"
        DiscardConfirmDialog(
            title = "Delete ${target.name}?",
            body = "This $noun is removed for good. Anything already logged from it stays in your diary.",
            confirmLabel = "Delete",
            dismissLabel = "Keep",
            onConfirm = {
                onEvent(
                    when (target) {
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
                        is Target.Meal -> FoodLibraryEvent.OnRenameSavedMeal(target.id, name)
                        is Target.Dish -> FoodLibraryEvent.OnRenameRecipe(target.id, name)
                    },
                )
                renaming = null
            },
        )
    }
}

@Composable
private fun LibrarySection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@PreviewLightDark
@Composable
private fun FoodLibraryPreview() {
    AppTheme {
        FoodLibraryContent(
            uiState = FoodLibraryUiState(
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
