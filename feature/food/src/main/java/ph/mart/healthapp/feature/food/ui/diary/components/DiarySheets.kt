package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import kotlinx.coroutines.launch
import ph.mart.healthapp.core.data.food.MealIdeaRequest
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
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.isSaveableFood
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
    /** "Get ideas" is a route above the tab now, so the diary hands the gap up rather than drawing
     * the screen over itself. The sheet closes first, the handover "New recipe" already makes. */
    onGetIdeas: (MealIdeaRequest) -> Unit,
    /** The diary's host, because a row thrown away from the edit sheet raises the same undo the
     * swipe does — and it is the diary the sheet has just closed onto that shows it. */
    snackbarHostState: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var pendingDeleteSavedMeal by remember { mutableStateOf<SavedMeal?>(null) }
    var pendingDeleteRecipe by remember { mutableStateOf<Recipe?>(null) }

    // The day as a picture. Needs the targets the summary bar reads against, so it is silent
    // without a profile — the same condition the bar itself draws under.
    val targets = uiState.targets
    if (state.shareOpen && targets != null) {
        ShareDaySheet(uiState = uiState, targets = targets, onDismiss = { state.shareOpen = false })
    }

    if (state.noteSheetOpen) {
        DayNoteSheet(
            dateLabel = diaryDateLabel(uiState.selectedDate, uiState.today),
            draft = state.noteDraft,
            onDraftChange = { state.noteDraft = it },
            onDismiss = state::closeNoteSheet,
            onSave = {
                onEvent(FoodEvent.OnSetNote(state.noteDraft))
                state.closeNoteSheet()
            },
        )
    }

    val activeMealSheet = state.activeMealSheet
    if (activeMealSheet != null) {
        val editingId = state.editingEntryId
        val editingEntry = editingId?.let { id -> uiState.entries.firstOrNull { it.id == id } }
        AddEntrySheet(
            mealType = activeMealSheet,
            form = state.addForm,
            view = state.sheetView,
            browseTab = state.browseTab,
            quickAddKcal = state.quickAddKcal,
            seededFromProduct = state.seededFromProduct,
            saveMyFood = state.saveMyFood,
            suggestions = uiState.suggestions,
            savedMeals = uiState.savedMeals,
            recipes = uiState.recipes,
            onViewChange = { state.sheetView = it },
            onTabChange = { state.browseTab = it },
            onQuickAddChange = { state.quickAddKcal = it },
            // A bare calorie figure, logged without reaching the form at all. `toFoodEntry()` fills
            // the blank name with QUICK_ADD_NAME and collapses the portion to one serving, which is
            // the same path the form's own blank-name commit takes — the pill is a shortcut to it,
            // not a second way of writing a row.
            onQuickAdd = {
                state.quickAddKcal?.let { kcal ->
                    onEvent(FoodEvent.OnAddEntry(AddEntryForm(mealType = activeMealSheet, calories = kcal)))
                    state.closeSheet()
                }
            },
            onSaveMyFoodChange = { state.saveMyFood = it },
            // A recipe is priced per serving, not per 100 g, so it seeds without claiming to be a
            // database row — see `FoodScreenState.seededFromProduct`.
            onSelectRecipe = { recipe ->
                state.seedForm(recipe.toAddEntryForm(activeMealSheet), fromProduct = false)
            },
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
            // The one seed that really is a per-100 g row: a search hit or a barcode match.
            onSelectProduct = { state.seedForm(it.toAddEntryForm(activeMealSheet), fromProduct = true) },
            onSelectSuggestion = { state.seedForm(it.toAddEntryForm(activeMealSheet), fromProduct = false) },
            onLogAgain = { suggestion ->
                onEvent(FoodEvent.OnAddEntry(suggestion.toAddEntryForm(activeMealSheet)))
                state.closeSheet()
            },
            onToggleFavorite = { suggestion, favorite ->
                onEvent(FoodEvent.OnToggleFavorite(suggestion, favorite))
            },
            onGetIdeas = uiState.mealIdeaRequest(activeMealSheet)
                ?.takeIf { editingId == null }
                ?.let { request ->
                    {
                        state.openIdeas(activeMealSheet)
                        onGetIdeas(request)
                    }
                },
            // One level at a time — Search → Browse, Form → Browse — and the sheet only closes once
            // there is no level left. `backFromSheet` owns that ladder so the arrow in the form's
            // top bar and the system gesture cannot disagree about it.
            onBack = { if (!state.backFromSheet()) state.closeSheet() },
            onDismiss = state::closeSheet,
            onAdd = {
                // Keeping the food is a separate write and goes first, so a food the user asked to
                // keep is kept even though the two are one press. No toast — the saved meal's rule —
                // because it appears starred at the top of Recents the moment Room emits.
                if (editingId == null && state.saveMyFood && state.addForm.isSaveableFood()) {
                    onEvent(FoodEvent.OnSaveMyFood(state.addForm))
                }
                onEvent(
                    if (editingId == null) {
                        FoodEvent.OnAddEntry(state.addForm)
                    } else {
                        FoodEvent.OnUpdateEntry(editingId, state.addForm)
                    },
                )
                state.closeSheet()
            },
            editing = editingId != null,
            loggedAt = editingEntry?.loggedAt,
            // Soft delete with an undo, the diary swipe's contract to the letter — the sheet closes
            // first so the snackbar lands on the day, not behind the scrim.
            onDelete = editingEntry?.let { entry ->
                {
                    onEvent(FoodEvent.OnDeleteEntry(entry.id))
                    state.closeSheet()
                    scope.launch {
                        val undone = snackbarHostState.showSnackbar(
                            message = context.getString(R.string.food_deleted, entry.name),
                            actionLabel = context.getString(R.string.food_undo),
                            duration = SnackbarDuration.Short,
                        ) == SnackbarResult.ActionPerformed
                        if (undone) onEvent(FoodEvent.OnRestoreEntry(entry))
                    }
                }
            },
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

    // Which day to copy *from*. Its own sheet rather than a mode on the one below: that calendar
    // moves the diary, this one picks a source and leaves the day where it is — and at expanded
    // width the diary's calendar is a permanent pane, where this is still a sheet.
    if (state.copyPickerOpen) {
        AppBottomSheet(
            title = stringResource(R.string.food_copy_pick_title),
            onDismiss = { state.copyPickerOpen = false },
        ) {
            CalendarPanel(
                // The day being shown, drawn selected — and the one day that is not a source: the
                // ViewModel ignores it, because copying a day onto itself only doubles it.
                selectedDate = uiState.selectedDate,
                markedDates = emptySet(),
                maxDate = uiState.today,
                onSelectDate = { date ->
                    onEvent(FoodEvent.OnPickCopySource(date))
                    state.copyPickerOpen = false
                },
            )
        }
    }

    // Opens off the loaded day rather than a flag, so it is still here after a rotation.
    uiState.copySource?.let { source ->
        CopyDaySheet(
            source = source,
            today = uiState.today,
            onDismiss = { onEvent(FoodEvent.OnPickCopySource(null)) },
            onCopy = { meals, water, exercise ->
                onEvent(FoodEvent.OnCopyDay(meals, water, exercise))
            },
        )
    }

    if (state.calendarOpen) {
        AppBottomSheet(
            title = stringResource(R.string.food_diary_calendar_title),
            onDismiss = { state.calendarOpen = false },
        ) {
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
                onGetIdeas = {},
                snackbarHostState = SnackbarHostState(),
            )
        }
    }
}
