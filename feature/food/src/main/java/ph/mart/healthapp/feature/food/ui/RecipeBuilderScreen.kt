package ph.mart.healthapp.feature.food.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.perServing
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.DiscardConfirmDialog
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.component.NumericStepperField
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.ui.components.RecipeIngredientEditor

/**
 * Authors a recipe: a name, how many portions it makes, and its ingredients. Saving writes it once;
 * logging happens later from the add-entry sheet, one serving at a time.
 *
 * A screen rather than a sub-view of that sheet because an ingredient list plus its editor doesn't
 * fit above a keyboard — the one place in this feature where the sheet pattern was the wrong shape.
 */
@Composable
fun RecipeBuilderScreen(
    onExit: () -> Unit,
    viewModel: RecipeBuilderViewModel = koinViewModel(),
) {
    val state = rememberRecipeBuilderState()
    viewModel.collectSideEffect { effect ->
        when (effect) {
            RecipeBuilderSideEffect.Saved -> onExit()
        }
    }
    RecipeBuilderContent(
        state = state,
        onSave = {
            viewModel.handleEvent(
                RecipeBuilderEvent.OnSave(
                    name = state.name,
                    servings = state.servings,
                    items = state.ingredients,
                ),
            )
        },
        onExit = onExit,
    )
}

@Composable
private fun RecipeBuilderContent(
    state: RecipeBuilderState,
    onSave: () -> Unit,
    onExit: () -> Unit,
) {
    // Back out of a half-built recipe is the one destructive gesture on this screen, so it only
    // intercepts once there is something to lose; an untouched builder pops like any other route.
    if (state.isDirty) {
        val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(
            state = navigationState,
            onBackCompleted = { state.discardOpen = true },
        )
    }

    val perServing = Recipe(
        id = 0,
        name = state.name,
        servings = state.servings,
        items = state.ingredients,
    ).perServing()

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = DockedFabContentPadding),
            ) {
                Text(
                    text = "New recipe",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                AppTextField(
                    value = state.name,
                    onValueChange = { state.name = it },
                    label = "Recipe",
                    placeholder = "Name this dish",
                )
                NumericStepperField(
                    label = "Makes",
                    value = state.servings.toString(),
                    unitSuffix = if (state.servings == 1) "serving" else "servings",
                    onIncrement = { state.servings += 1 },
                    onDecrement = { state.servings = (state.servings - 1).coerceAtLeast(1) },
                )
                PerServingSummary(
                    calories = perServing.calories,
                    proteinG = perServing.proteinG,
                    carbsG = perServing.carbsG,
                    fatG = perServing.fatG,
                )
                IngredientList(
                    ingredients = state.ingredients,
                    onRemove = state::removeIngredient,
                )
                RecipeIngredientEditor(
                    draft = state.draft,
                    canAdd = state.draftIsValid,
                    onDraftChange = { state.draft = it },
                    onAdd = state::addDraft,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryButton(
                        label = "Cancel",
                        onClick = { if (state.isDirty) state.discardOpen = true else onExit() },
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(
                        label = "Save recipe",
                        onClick = onSave,
                        enabled = state.canSave,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (state.discardOpen) {
                DiscardConfirmDialog(
                    title = "Discard this recipe?",
                    body = "It hasn't been saved yet.",
                    onConfirm = {
                        state.discardOpen = false
                        onExit()
                    },
                    onDismiss = { state.discardOpen = false },
                )
            }
        }
    }
}

/** What one portion costs — the number the whole screen exists to produce, so it sits above the
 * ingredients rather than under them. */
@Composable
private fun PerServingSummary(calories: Int, proteinG: Int, carbsG: Int, fatG: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "$calories kcal per serving",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "P ${proteinG}g · C ${carbsG}g · F ${fatG}g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IngredientList(ingredients: List<SavedMealItem>, onRemove: (Int) -> Unit) {
    if (ingredients.isEmpty()) {
        Text(
            text = "No ingredients yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ingredients.forEachIndexed { index, ingredient ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                FoodItemRow(
                    variant = FoodItemRowVariant.Display,
                    name = ingredient.name,
                    portionAmount = ingredient.portionAmount,
                    portionUnit = ingredient.portionUnit,
                    calories = ingredient.calories,
                    proteinG = ingredient.proteinG,
                    carbsG = ingredient.carbsG,
                    fatG = ingredient.fatG,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onRemove(index) }, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = AppIcons.Delete,
                        contentDescription = "Remove ${ingredient.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun RecipeBuilderScreenPreview() {
    AppTheme {
        RecipeBuilderContent(
            state = RecipeBuilderState(
                name = "Chili",
                servings = 4,
                ingredients = listOf(
                    SavedMealItem("Beans", 400.0, "g", 480, 28, 80, 4),
                    SavedMealItem("Beef mince", 500.0, "g", 1100, 100, 0, 80),
                ),
            ),
            onSave = {},
            onExit = {},
        )
    }
}
