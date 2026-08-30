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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.exercise.budgetKcal
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodSuggestion
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.dailyTotals
import ph.mart.healthapp.core.data.health.dayBurnedKcal
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.CalendarPanel
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.ui.diary.components.AddEntrySheet
import ph.mart.healthapp.feature.food.ui.diary.components.DiaryDateHeader
import ph.mart.healthapp.feature.food.ui.diary.components.DiarySummaryBar
import ph.mart.healthapp.feature.food.ui.diary.components.DiaryWaterRow
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
                        placeholder = "Filter this day's foods…",
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onScanBarcode(uiState.selectedDate) }, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = AppIcons.Barcode,
                            contentDescription = "Scan barcode",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                uiState.targets?.let { targets ->
                    DiarySummaryBar(
                        consumedKcal = uiState.entries.dailyTotals().calories,
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
                            onToggle = { state.toggleExpanded(mealType) },
                            onAdd = { state.openSheet(mealType) },
                            // Nothing logged here yet means nothing to snapshot — the whole
                            // section's entries are saved, not the filtered view.
                            onSave = if (mealEntries.isEmpty()) null else ({ state.openSaveMealSheet(mealType) }),
                            onDeleteEntry = { id -> onEvent(FoodEvent.OnDeleteEntry(id)) },
                        )
                    }

                    ExerciseSection(
                        entries = uiState.exercise,
                        expanded = state.exerciseExpanded,
                        onToggle = { state.exerciseExpanded = !state.exerciseExpanded },
                        onAdd = { state.exerciseSheetOpen = true },
                        onDeleteEntry = { id -> onEvent(FoodEvent.OnDeleteExercise(id)) },
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
                    onDeleteRecipe = { recipe -> onEvent(FoodEvent.OnDeleteRecipe(recipe.id)) },
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
                    onDeleteSavedMeal = { meal -> onEvent(FoodEvent.OnDeleteSavedMeal(meal.id)) },
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
                        onEvent(FoodEvent.OnAddEntry(state.addForm))
                        state.closeSheet()
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

            if (state.exerciseSheetOpen) {
                LogExerciseSheet(
                    onDismiss = { state.exerciseSheetOpen = false },
                    dateEpochDay = uiState.selectedDate,
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
