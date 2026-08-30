package ph.mart.healthapp.feature.food.ui.diary

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.exercise.budgetKcal
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodSuggestion
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.dailyTotals
import ph.mart.healthapp.core.data.health.dayBurnedKcal
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.CalendarPanel
import ph.mart.healthapp.core.designsystem.component.DiscardConfirmDialog
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.ui.diary.components.AddEntrySheet
import ph.mart.healthapp.feature.food.ui.diary.components.DiaryDateHeader
import ph.mart.healthapp.feature.food.ui.diary.components.DiarySummaryBar
import ph.mart.healthapp.feature.food.ui.diary.components.DiaryWaterRow
import ph.mart.healthapp.feature.food.ui.diary.components.EmptyDiaryDay
import ph.mart.healthapp.feature.food.ui.diary.components.MealSection
import ph.mart.healthapp.feature.food.ui.diary.components.SaveMealSheet
import ph.mart.healthapp.feature.food.ui.exercise.LogExerciseSheet
import ph.mart.healthapp.feature.food.ui.exercise.components.ExerciseSection
import ph.mart.healthapp.feature.food.ui.shared.toAddEntryForm

@Composable
fun FoodScreen(
    onScanBarcode: (Long) -> Unit,
    onNewRecipe: () -> Unit,
    scrollState: ScrollState = rememberScrollState(),
    viewModel: FoodViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    val state = rememberFoodScreenState()
    FoodContent(
        uiState = uiState,
        state = state,
        onEvent = viewModel::handleEvent,
        onScanBarcode = onScanBarcode,
        onNewRecipe = onNewRecipe,
        scrollState = scrollState,
    )
}

@Composable
private fun FoodContent(
    uiState: FoodUiState,
    state: FoodScreenState,
    onEvent: (FoodEvent) -> Unit,
    onScanBarcode: (Long) -> Unit,
    onNewRecipe: () -> Unit,
    scrollState: ScrollState = rememberScrollState(),
) {
    // Back off a past day returns to today rather than leaving the tab — one level, same rule the
    // sheets and the calendar swap-in follow. On today no handler is registered at all.
    if (uiState.selectedDate != uiState.today) {
        val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(
            state = navigationState,
            onBackCompleted = { onEvent(FoodEvent.OnSelectDate(uiState.today)) },
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Which saved meal or recipe a confirm dialog is asking about. Local rather than in
    // [FoodScreenState]: a rotation mid-dialog can lose the question and re-ask it, where losing
    // the sheet's half-typed form underneath would actually cost the user something.
    var pendingDeleteSavedMeal by remember { mutableStateOf<SavedMeal?>(null) }
    var pendingDeleteRecipe by remember { mutableStateOf<Recipe?>(null) }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                DiaryDateHeader(
                    selectedDate = uiState.selectedDate,
                    today = uiState.today,
                    onSelectDate = { date -> onEvent(FoodEvent.OnSelectDate(date)) },
                    onOpenCalendar = { state.calendarOpen = true },
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                ) {
                    AppTextField(
                        value = state.searchQuery,
                        onValueChange = { state.searchQuery = it },
                        // No visible label: the placeholder already says it, and AppTextField
                        // hands the placeholder to the screen reader when there is no label.
                        placeholder = "Filter this day's foods…",
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onScanBarcode(uiState.selectedDate) }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = AppIcons.Barcode,
                            contentDescription = "Scan barcode",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                uiState.targets?.let { targets ->
                    DiarySummaryBar(
                        consumed = uiState.entries.dailyTotals(),
                        goalKcal = budgetKcal(
                            targetKcal = targets.calories,
                            burnedKcal = dayBurnedKcal(uiState.exercise, uiState.steps),
                            addExercise = uiState.addExerciseToBudget,
                        ),
                        proteinGoalG = targets.proteinG,
                        carbsGoalG = targets.carbsG,
                        fatGoalG = targets.fatG,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = DockedFabContentPadding),
                ) {
                    DiaryWaterRow(
                        glasses = uiState.waterGlasses,
                        goalGlasses = uiState.waterGoalGlasses,
                        unit = uiState.unit,
                        onSetGlasses = { glasses -> onEvent(FoodEvent.OnSetWaterGlasses(glasses)) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    // One mascot and one sentence in place of five identical "nothing logged"
                    // lines. Only when the whole day is bare — a single empty section between two
                    // full ones is not a first-run moment, it is just an empty section.
                    if (uiState.entries.isEmpty() && uiState.exercise.isEmpty()) {
                        EmptyDiaryDay(isToday = uiState.selectedDate == uiState.today)
                    }

                    MealType.entries.forEach { mealType ->
                        val mealEntries = uiState.entries.filter { it.mealType == mealType }
                        val visibleEntries = mealEntries.filter {
                            state.searchQuery.isBlank() || it.name.contains(state.searchQuery, ignoreCase = true)
                        }
                        MealSection(
                            mealType = mealType,
                            entries = visibleEntries,
                            subtotalKcal = mealEntries.dailyTotals().calories,
                            expanded = state.expandedMeals[mealType] != false,
                            // The section has food in it and the filter hid all of it — a
                            // different sentence from a section with nothing in it.
                            filteredOut = mealEntries.isNotEmpty() && visibleEntries.isEmpty(),
                            onToggle = { state.toggleExpanded(mealType) },
                            onAdd = { state.openSheet(mealType) },
                            // Nothing logged here yet means nothing to snapshot — the whole
                            // section's entries are saved, not the filtered view.
                            onSave = if (mealEntries.isEmpty()) null else ({ state.openSaveMealSheet(mealType) }),
                            onEditEntry = state::openEditSheet,
                            onDeleteEntry = { entry ->
                                onEvent(FoodEvent.OnDeleteEntry(entry.id))
                                scope.launch {
                                    val undone = snackbarHostState.showSnackbar(
                                        message = "Deleted ${entry.name}",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short,
                                    ) == SnackbarResult.ActionPerformed
                                    if (undone) onEvent(FoodEvent.OnRestoreEntry(entry))
                                }
                            },
                        )
                    }

                    ExerciseSection(
                        entries = uiState.exercise,
                        expanded = state.exerciseExpanded,
                        onToggle = { state.exerciseExpanded = !state.exerciseExpanded },
                        onAdd = { state.openExerciseSheet() },
                        onEditEntry = { entry -> state.openExerciseSheet(entry) },
                        onDeleteEntry = { entry ->
                            onEvent(FoodEvent.OnDeleteExercise(entry.id))
                            scope.launch {
                                val undone = snackbarHostState.showSnackbar(
                                    message = "Deleted ${entry.type.label}",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short,
                                ) == SnackbarResult.ActionPerformed
                                if (undone) onEvent(FoodEvent.OnRestoreExercise(entry))
                            }
                        },
                    )
                }
            }

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
                    title = "Delete \"${meal.name}\"?",
                    body = "The meals you already logged from it stay in your diary.",
                    confirmLabel = "Delete",
                    dismissLabel = "Keep",
                    onConfirm = {
                        onEvent(FoodEvent.OnDeleteSavedMeal(meal.id))
                        pendingDeleteSavedMeal = null
                    },
                    onDismiss = { pendingDeleteSavedMeal = null },
                )
            }

            pendingDeleteRecipe?.let { recipe ->
                DiscardConfirmDialog(
                    title = "Delete \"${recipe.name}\"?",
                    body = "The entries you already logged from it stay in your diary.",
                    confirmLabel = "Delete",
                    dismissLabel = "Keep",
                    onConfirm = {
                        onEvent(FoodEvent.OnDeleteRecipe(recipe.id))
                        pendingDeleteRecipe = null
                    },
                    onDismiss = { pendingDeleteRecipe = null },
                )
            }

            // Above the docked FAB, so an Undo is never the thing hidden behind it.
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = DockedFabContentPadding),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FoodScreenPreview() {
    val entries = listOf(
        FoodEntry(id = 1, name = "Greek yogurt", mealType = MealType.Breakfast, portionAmount = 1.0, portionUnit = "cup", calories = 150, proteinG = 20, carbsG = 8, fatG = 4),
        FoodEntry(id = 2, name = "Grilled chicken breast", mealType = MealType.Lunch, portionAmount = 150.0, portionUnit = "g", calories = 210, proteinG = 32, carbsG = 2, fatG = 8),
    )
    val targets = DailyTargets(calories = 1941, proteinG = 146, carbsG = 194, fatG = 65, floor = 1500)
    AppTheme {
        FoodContent(
            uiState = FoodUiState(
                entries = entries,
                targets = targets,
                suggestions = listOf(
                    FoodSuggestion("Greek yogurt", 1.0, "cup", 150, 20, 8, 4, isFavorite = true),
                ),
                savedMeals = listOf(
                    SavedMeal(
                        id = 1,
                        name = "Usual breakfast",
                        items = listOf(SavedMealItem("Greek yogurt", 1.0, "cup", 150, 20, 8, 4)),
                    ),
                ),
            ),
            state = FoodScreenState(),
            onEvent = {},
            onScanBarcode = {},
            onNewRecipe = {},
        )
    }
}
