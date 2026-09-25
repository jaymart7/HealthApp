package ph.mart.healthapp.feature.food.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.designsystem.component.AIChip
import ph.mart.healthapp.core.designsystem.component.AIChipVariant
import ph.mart.healthapp.core.designsystem.component.DiscardConfirmDialog
import ph.mart.healthapp.core.designsystem.component.DockedActionBar
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.library.components.DescribeStep
import ph.mart.healthapp.feature.food.ui.library.components.FoodReview
import ph.mart.healthapp.feature.food.ui.library.components.IngredientSheet
import ph.mart.healthapp.feature.food.ui.library.components.RecipeReview

/**
 * Adds to the food library, and edits what is already in it. A new item opens on one question —
 * "what do you want to save?" — and the model decides whether the answer is a food or a recipe;
 * the review that follows is short and shaped by that answer. An existing item, opened from its
 * row in the library, starts on the review, and its Delete lives at the foot.
 *
 * Both of [savedMealId] and [foodName] null is a new item. A recipe and a saved meal share an id
 * space, so one id names either; a food's name is its key.
 */
@Composable
fun LibraryItemScreen(
    savedMealId: Long?,
    foodName: String?,
    onExit: () -> Unit,
    viewModel: LibraryItemViewModel = koinViewModel(),
) {
    val isNew = savedMealId == null && foodName == null
    val uiState by viewModel.collectAsState()
    val state = rememberLibraryItemState(isNew)
    // Asked every time, so `original` is back after a process death; the form only takes the
    // answer once — see [LibraryItemState.loaded].
    LaunchedEffect(savedMealId, foodName) {
        if (!isNew) viewModel.handleEvent(LibraryItemEvent.OnOpen(savedMealId, foodName))
    }
    viewModel.collectSideEffect { effect ->
        when (effect) {
            is LibraryItemSideEffect.Loaded -> state.applyLoaded(effect.form)
            is LibraryItemSideEffect.Filled -> state.applyFill(effect.form)
            is LibraryItemSideEffect.FillFailed -> state.fillError = effect.message
            LibraryItemSideEffect.Done -> onExit()
        }
    }
    LibraryItemContent(
        isNew = isNew,
        filling = uiState.filling,
        original = uiState.original,
        state = state,
        onFill = { viewModel.handleEvent(LibraryItemEvent.OnFill(state.description)) },
        onCancelFill = { viewModel.handleEvent(LibraryItemEvent.OnCancelFill) },
        onSave = { viewModel.handleEvent(LibraryItemEvent.OnSave(state.form, savedMealId, foodName)) },
        onDelete = { viewModel.handleEvent(LibraryItemEvent.OnDelete(savedMealId, foodName)) },
        onExit = onExit,
    )
}

@Composable
private fun LibraryItemContent(
    isNew: Boolean,
    filling: Boolean,
    original: LibraryItemForm?,
    state: LibraryItemState,
    onFill: () -> Unit,
    onCancelFill: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onExit: () -> Unit,
) {
    val reviewingNew = isNew && state.step == LibraryStep.Review
    // Anything typed at all counts on a new item; an existing one is dirty only once it differs
    // from what was opened, so looking at a recipe and backing out never asks.
    val dirty = if (isNew) {
        state.description.isNotBlank() || state.form.hasContent()
    } else {
        original != null && state.form != original
    }
    // One level at a time: mid-fill, back stops the call (the photo flow's Analyzing rule); a new
    // item's review steps back to the question with its words still in it; otherwise back only
    // asks once there is something to lose, and an untouched screen pops like any other route.
    if (filling || reviewingNew || dirty) {
        val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(
            state = navigationState,
            onBackCompleted = {
                when {
                    filling -> onCancelFill()
                    reviewingNew -> state.step = LibraryStep.Describe
                    else -> state.dialog = LibraryDialog.Discard
                }
            },
        )
    }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        when {
            // Nothing until the item arrives: an empty form that fills a frame later reads as a bug.
            !isNew && !state.loaded -> Unit
            state.step == LibraryStep.Describe -> DescribeStep(
                text = state.description,
                filling = filling,
                error = state.fillError?.let { stringResource(it) },
                onTextChange = {
                    state.description = it
                    state.fillError = null
                },
                // A fill replaces the review, so one over a review already there asks first.
                onFill = {
                    state.fillError = null
                    if (state.form.hasContent()) state.dialog = LibraryDialog.Replace else onFill()
                },
                onStop = onCancelFill,
                onTypeFood = { state.startManual(LibraryKind.Food) },
                onBuildRecipe = { state.startManual(LibraryKind.Recipe) },
                onResume = { state.step = LibraryStep.Review }.takeIf { state.form.hasContent() },
            )
            else -> Review(isNew = isNew, state = state, onSave = onSave)
        }
    }

    state.draft?.let { draft ->
        IngredientSheet(
            draft = draft,
            isNew = state.editingIndex == NEW_INGREDIENT,
            onDraftChange = { state.draft = it },
            onDone = state::commitDraft,
            onDismiss = { state.draft = null },
        )
    }

    when (state.dialog) {
        LibraryDialog.Discard -> DiscardConfirmDialog(
            title = stringResource(R.string.food_library_discard_title),
            body = stringResource(R.string.food_recipe_not_saved),
            onConfirm = {
                state.dialog = null
                onExit()
            },
            onDismiss = { state.dialog = null },
        )
        LibraryDialog.Replace -> DiscardConfirmDialog(
            title = stringResource(R.string.food_library_replace_title),
            body = stringResource(R.string.food_library_replace_body),
            confirmLabel = stringResource(R.string.food_recipe_replace),
            onConfirm = {
                state.dialog = null
                onFill()
            },
            onDismiss = { state.dialog = null },
        )
        // Something the user built, so delete asks first — the library's rule since it had rows.
        LibraryDialog.Delete -> DiscardConfirmDialog(
            title = stringResource(R.string.food_library_delete_title, state.form.displayName()),
            body = stringResource(R.string.food_library_delete_body),
            confirmLabel = stringResource(R.string.food_library_delete),
            dismissLabel = stringResource(R.string.food_library_keep),
            onConfirm = {
                state.dialog = null
                onDelete()
            },
            onDismiss = { state.dialog = null },
        )
        null -> Unit
    }
}

@Composable
private fun Review(isNew: Boolean, state: LibraryItemState, onSave: () -> Unit) {
    val form = state.form
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                // Before the scroll, so the viewport shrinks with the keyboard and the fields at the
                // foot of the page can still be brought into view.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Every figure below may be the model's, and the app says so wherever one is.
            if (state.fromAi) {
                AIChip(label = stringResource(R.string.food_library_ai_estimate), variant = AIChipVariant.Default)
            }
            if (form.kind == LibraryKind.Food) {
                FoodReview(
                    food = form.food,
                    manualEntry = isNew && !state.fromAi,
                    onFoodChange = { state.form = form.copy(food = it) },
                )
            } else {
                RecipeReview(
                    form = form,
                    onFormChange = { state.form = it },
                    onOpenIngredient = state::openIngredient,
                    onRemoveIngredient = state::removeIngredient,
                    onAddIngredient = state::addIngredient,
                )
            }
            if (!isNew) {
                // The Profile lists' foot-of-the-sheet delete: under a rule, in `error`, and asking.
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
                TextButton(
                    label = stringResource(R.string.food_library_delete),
                    onClick = { state.dialog = LibraryDialog.Delete },
                    color = MaterialTheme.colorScheme.error,
                    icon = AppIcons.Delete,
                )
            }
        }
        DockedActionBar {
            PrimaryButton(
                label = stringResource(
                    when (form.kind) {
                        LibraryKind.Food -> R.string.food_new_food_save
                        LibraryKind.Recipe -> R.string.food_recipe_save
                        LibraryKind.Meal -> R.string.food_library_save_meal
                    },
                ),
                onClick = onSave,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The name the delete confirm asks about, whichever field holds it. */
private fun LibraryItemForm.displayName(): String = if (kind == LibraryKind.Food) food.name else name

private val PREVIEW_RECIPE = LibraryItemForm(
    kind = LibraryKind.Recipe,
    name = "Chicken adobo",
    servings = 4,
    ingredients = listOf(
        SavedMealItem("Chicken thighs", 1000.0, "g", 1640, 170, 0, 104),
        SavedMealItem("Soy sauce", 60.0, "ml", 32, 5, 3, 0),
    ),
)

@PreviewLightDark
@Composable
private fun LibraryItemDescribePreview() {
    AppTheme {
        LibraryItemContent(
            isNew = true,
            filling = false,
            original = null,
            state = LibraryItemState(),
            onFill = {},
            onCancelFill = {},
            onSave = {},
            onDelete = {},
            onExit = {},
        )
    }
}

/** A fresh AI fill: the chip says whose figures these are. */
@PreviewLightDark
@Composable
private fun LibraryItemReviewPreview() {
    AppTheme {
        LibraryItemContent(
            isNew = true,
            filling = false,
            original = null,
            state = LibraryItemState(step = LibraryStep.Review, form = PREVIEW_RECIPE, fromAi = true),
            onFill = {},
            onCancelFill = {},
            onSave = {},
            onDelete = {},
            onExit = {},
        )
    }
}

/** Opened from the library: no chip, and Delete at the foot. */
@PreviewLightDark
@Composable
private fun LibraryItemEditPreview() {
    AppTheme {
        LibraryItemContent(
            isNew = false,
            filling = false,
            original = PREVIEW_RECIPE,
            state = LibraryItemState(step = LibraryStep.Review, form = PREVIEW_RECIPE, loaded = true),
            onFill = {},
            onCancelFill = {},
            onSave = {},
            onDelete = {},
            onExit = {},
        )
    }
}
