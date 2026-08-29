package ph.mart.healthapp.feature.food.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodSuggestion
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.ScannedProduct
import ph.mart.healthapp.core.data.food.dailyTotals
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.CalendarPanel
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.component.MacroInputGroup
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.data.exercise.budgetKcal
import ph.mart.healthapp.core.data.exercise.totalBurnedKcal
import ph.mart.healthapp.feature.food.ui.components.DiaryDateHeader
import ph.mart.healthapp.feature.food.ui.components.DiarySummaryBar
import ph.mart.healthapp.feature.food.ui.components.DiaryWaterRow
import ph.mart.healthapp.feature.food.ui.components.FoodSearchPanel
import ph.mart.healthapp.feature.food.ui.components.FoodSuggestionPanel
import ph.mart.healthapp.feature.food.ui.components.ExerciseSection
import ph.mart.healthapp.feature.food.ui.components.MealSectionHeader

@Composable
fun FoodScreen(
    onScanBarcode: (Long) -> Unit,
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
        scrollState = scrollState,
    )
}

@Composable
private fun FoodContent(
    uiState: FoodUiState,
    state: FoodScreenState,
    onEvent: (FoodEvent) -> Unit,
    onScanBarcode: (Long) -> Unit,
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
                            burnedKcal = uiState.exercise.totalBurnedKcal(),
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
                    onFormChange = { state.addForm = it },
                    onSelectProduct = { state.addForm = it.toAddEntryForm(activeMealSheet) },
                    onSelectSuggestion = { state.addForm = it.toAddEntryForm(activeMealSheet) },
                    onQuickAdd = { suggestion ->
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

@Composable
private fun MealSection(
    mealType: MealType,
    entries: List<FoodEntry>,
    subtotalKcal: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAdd: () -> Unit,
    onDeleteEntry: (Long) -> Unit,
) {
    Column {
        MealSectionHeader(
            label = mealType.name,
            subtotalKcal = subtotalKcal,
            expanded = expanded,
            onToggle = onToggle,
            onAdd = onAdd,
        )
        if (expanded) {
            if (entries.isEmpty()) {
                Text(
                    text = "Nothing logged for ${mealType.name} yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 38.dp, end = 16.dp, bottom = 12.dp),
                )
            }
            entries.forEach { entry ->
                key(entry.id) {
                    SwipeableFoodEntryRow(entry = entry, onDelete = { onDeleteEntry(entry.id) })
                }
            }
        }
    }
}

@Composable
private fun SwipeableFoodEntryRow(entry: FoodEntry, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDelete()
            true
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Delete",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(end = 24.dp),
                )
            }
        },
    ) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface).padding(start = 38.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
            FoodItemRow(
                variant = FoodItemRowVariant.Display,
                name = entry.name,
                portionAmount = entry.portionAmount,
                portionUnit = entry.portionUnit,
                calories = entry.calories,
                proteinG = entry.proteinG,
                carbsG = entry.carbsG,
                fatG = entry.fatG,
            )
        }
    }
}

@Composable
private fun AddEntrySheet(
    mealType: MealType,
    form: AddEntryForm,
    suggestions: List<FoodSuggestion>,
    onFormChange: (AddEntryForm) -> Unit,
    onSelectProduct: (ScannedProduct) -> Unit,
    onSelectSuggestion: (FoodSuggestion) -> Unit,
    onQuickAdd: (FoodSuggestion) -> Unit,
    onToggleFavorite: (FoodSuggestion, Boolean) -> Unit,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss) {
        Text(
            text = "Add to ${mealType.name}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Both panels seed the fields below; they stay editable either way, so this is a
            // shortcut past typing rather than a separate entry mode. Already-logged foods come
            // first — they cost no network round-trip and are the likelier match.
            FoodSuggestionPanel(
                suggestions = suggestions,
                onSelect = onSelectSuggestion,
                onQuickAdd = onQuickAdd,
                onToggleFavorite = onToggleFavorite,
            )
            FoodSearchPanel(onSelect = onSelectProduct)
            FoodItemRow(
                variant = FoodItemRowVariant.Editable,
                name = form.name,
                portionAmount = form.portionAmount,
                portionUnit = form.portionUnit,
                calories = form.calories,
                proteinG = form.proteinG,
                carbsG = form.carbsG,
                fatG = form.fatG,
                onNameChange = { onFormChange(form.copy(name = it)) },
                onPortionAmountChange = { onFormChange(form.copy(portionAmount = it)) },
                onPortionUnitChange = { onFormChange(form.copy(portionUnit = it)) },
                onCaloriesChange = { onFormChange(form.copy(calories = it)) },
            )
            MacroInputGroup(
                proteinG = form.proteinG,
                carbsG = form.carbsG,
                fatG = form.fatG,
                onProteinChange = { onFormChange(form.copy(proteinG = it)) },
                onCarbsChange = { onFormChange(form.copy(carbsG = it)) },
                onFatChange = { onFormChange(form.copy(fatG = it)) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryButton(label = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                PrimaryButton(label = "Add", onClick = onAdd, enabled = form.isValid(), modifier = Modifier.weight(1f))
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
            ),
            state = FoodScreenState(),
            onEvent = {},
            onScanBarcode = {},
        )
    }
}
