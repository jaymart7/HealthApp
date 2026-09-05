package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.CalendarPanel
import ph.mart.healthapp.core.designsystem.component.DiscardConfirmDialog
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.diary.FoodEvent
import ph.mart.healthapp.feature.food.ui.diary.FoodScreenState
import ph.mart.healthapp.feature.food.ui.diary.FoodUiState
import ph.mart.healthapp.feature.food.ui.diary.mealIdeaRequest
import ph.mart.healthapp.feature.food.ui.diary.rememberFoodScreenState
import ph.mart.healthapp.feature.food.ui.diary.toAddEntryForm
import ph.mart.healthapp.feature.food.ui.diary.toSavedMealItem
import ph.mart.healthapp.feature.food.ui.exercise.LogExerciseSheet
import ph.mart.healthapp.feature.food.ui.ideas.MealIdeasScreen
import ph.mart.healthapp.feature.food.ui.shared.toAddEntryForm

/**
 * Everything the diary can *open* over itself: the add-entry sheet, the save-meal sheet, the
 * exercise sheet, the calendar, and the two delete confirmations. The counterpart to [DiaryBody] —
 * see there for why the screen is split this way.
 *
 * Which meal or recipe a confirmation is asking about lives here rather than in [FoodScreenState]:
 * a rotation mid-dialog can lose the question and re-ask it, where losing the sheet's half-typed
 * form underneath would actually cost the user something.
 */
@Composable
internal fun DiarySheets(
    uiState: FoodUiState,
    state: FoodScreenState,
    onEvent: (FoodEvent) -> Unit,
    onNewRecipe: () -> Unit,
    onOpenStrength: (Long, Long) -> Unit,
) {
    var pendingDeleteSavedMeal by remember { mutableStateOf<SavedMeal?>(null) }
    var pendingDeleteRecipe by remember { mutableStateOf<Recipe?>(null) }

    val activeMealSheet = state.activeMealSheet
    if (activeMealSheet != null) {
        AddEntrySheet(
            mealType = activeMealSheet,
            form = state.addForm,
            suggestions = uiState.suggestions,
            savedMeals = uiState.savedMeals,
            recipes = uiState.recipes,
            onSelectRecipe = { recipe -> state.addForm = recipe.toAddEntryForm(activeMealSheet) },
            onDeleteRecipe = { recipe -> pendingDeleteRecipe = recipe },
            // The builder is a screen, not a sub-view of this sheet: an ingredient list
            // doesn't fit above a keyboard. Closing first means back from it lands on the
            // diary rather than reopening a stale form.
            onNewRecipe = {
                state.closeSheet()
                onNewRecipe()
            },
            onLogSavedMeal = { meal ->
                onEvent(FoodEvent.OnLogSavedMeal(meal, activeMealSheet))
                state.closeSheet()
            },
            onDeleteSavedMeal = { meal -> pendingDeleteSavedMeal = meal },
            onFormChange = { state.addForm = it },
            onSelectProduct = { state.addForm = it.toAddEntryForm(activeMealSheet) },
            onSelectSuggestion = { state.addForm = it.toAddEntryForm(activeMealSheet) },
            onLogAgain = { suggestion ->
                onEvent(FoodEvent.OnAddEntry(suggestion.toAddEntryForm(activeMealSheet)))
                state.closeSheet()
            },
            onToggleFavorite = { suggestion, favorite ->
                onEvent(FoodEvent.OnToggleFavorite(suggestion, favorite))
            },
            // The sheet stays open and the form is left alone: keeping a food and logging it are
            // two different intentions, and the user may well want both. No toast — the saved
            // meal's rule — because the food appears starred in the suggestion panel just above
            // the moment Room emits, which is the same "the badge lighting up is the reward"
            // answer the streak gives.
            onSaveMyFood = { onEvent(FoodEvent.OnSaveMyFood(state.addForm)) },
            onGetIdeas = if (state.editingEntryId == null && uiState.mealIdeaRequest(activeMealSheet) != null) {
                { state.openIdeas(activeMealSheet) }
            } else {
                null
            },
            onDismiss = state::closeSheet,
            onAdd = {
                val editingId = state.editingEntryId
                onEvent(
                    if (editingId == null) {
                        FoodEvent.OnAddEntry(state.addForm)
                    } else {
                        FoodEvent.OnUpdateEntry(editingId, state.addForm)
                    },
                )
                state.closeSheet()
            },
            editing = state.editingEntryId != null,
        )
    }

    // A full-screen overlay over the diary, drawn last so it covers it — the shape the recap
    // and the timelapse use over the Progress tab, and the reason neither is a route. The request
    // is rebuilt from the live state each time, so a day that moved while the overlay was opening
    // asks against what is actually left.
    val ideasFor = state.ideasFor
    val ideasRequest = ideasFor?.let { uiState.mealIdeaRequest(it) }
    if (ideasRequest != null) {
        MealIdeasScreen(
            request = ideasRequest,
            suggestions = uiState.suggestions,
            recipes = uiState.recipes,
            onSelect = state::selectIdea,
            onClose = state::closeIdeas,
        )
    }

    val saveMealFor = state.saveMealFor
    if (saveMealFor != null) {
        SaveMealSheet(
            mealType = saveMealFor,
            name = state.savedMealName,
            itemCount = uiState.entries.count { it.mealType == saveMealFor },
            onNameChange = { state.savedMealName = it },
            onDismiss = state::closeSaveMealSheet,
            onSave = {
                onEvent(
                    FoodEvent.OnSaveMeal(
                        name = state.savedMealName,
                        items = uiState.entries
                            .filter { it.mealType == saveMealFor }
                            .map { it.toSavedMealItem() },
                    ),
                )
                state.closeSaveMealSheet()
            },
        )
    }

    // The row being corrected is resolved off the loaded day rather than held in screen
    // state, so the saver stays flat. The guard is what makes that safe: after a rotation
    // the day arrives an emission later, and a sheet composed against a null row would
    // seed itself blank and save as a *new* activity.
    val editingExercise = state.editingExerciseId?.let { id -> uiState.exercise.find { it.id == id } }
    if (state.exerciseSheetOpen && (state.editingExerciseId == null || editingExercise != null)) {
        LogExerciseSheet(
            onDismiss = state::closeExerciseSheet,
            // Closing first means back from the workout screen lands on the diary rather than
            // reopening a stale sheet — the same handover "New recipe" makes.
            onOpenStrength = { date ->
                val id = state.editingExerciseId ?: 0
                state.closeExerciseSheet()
                onOpenStrength(date, id)
            },
            dateEpochDay = uiState.selectedDate,
            editing = editingExercise,
        )
    }

    if (state.calendarOpen) {
        AppBottomSheet(onDismiss = { state.calendarOpen = false }) {
            CalendarPanel(
                selectedDate = uiState.selectedDate,
                // No dots: which days have entries would cost a query the diary otherwise
                // never makes. Add it if the calendar starts feeling blind.
                markedDates = emptySet(),
                maxDate = uiState.today,
                onSelectDate = { date ->
                    onEvent(FoodEvent.OnSelectDate(date))
                    state.calendarOpen = false
                },
                onBack = { state.calendarOpen = false },
            )
        }
    }

    // A saved meal and a recipe are things the user authored, and the row's delete icon
    // sits beside the one that logs it. A swipe on a diary row gets an undo instead —
    // recovery beats a confirmation when the gesture is deliberate and the loss is one row.
    pendingDeleteSavedMeal?.let { meal ->
        DiscardConfirmDialog(
            title = stringResource(R.string.food_delete_meal_title, meal.name),
            body = stringResource(R.string.food_delete_meal_body),
            confirmLabel = stringResource(R.string.food_delete),
            dismissLabel = stringResource(R.string.food_keep),
            onConfirm = {
                onEvent(FoodEvent.OnDeleteSavedMeal(meal.id))
                pendingDeleteSavedMeal = null
            },
            onDismiss = { pendingDeleteSavedMeal = null },
        )
    }

    pendingDeleteRecipe?.let { recipe ->
        DiscardConfirmDialog(
            title = stringResource(R.string.food_delete_meal_title, recipe.name),
            body = stringResource(R.string.food_delete_recipe_body),
            confirmLabel = stringResource(R.string.food_delete),
            dismissLabel = stringResource(R.string.food_keep),
            onConfirm = {
                onEvent(FoodEvent.OnDeleteRecipe(recipe.id))
                pendingDeleteRecipe = null
            },
            onDismiss = { pendingDeleteRecipe = null },
        )
    }
}

/** A sheet needs a scrim behind it or it renders invisible in isolation. */
@PreviewLightDark
@Composable
private fun DiarySheetsPreview() {
    AppTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            DiarySheets(
                uiState = FoodUiState(),
                state = rememberFoodScreenState().apply { calendarOpen = true },
                onEvent = {},
                onNewRecipe = {},
                onOpenStrength = { _, _ -> },
            )
        }
    }
}
