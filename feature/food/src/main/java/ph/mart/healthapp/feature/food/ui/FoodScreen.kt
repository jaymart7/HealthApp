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
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.SavedMealItem
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
import ph.mart.healthapp.core.data.health.dayBurnedKcal
import ph.mart.healthapp.feature.food.ui.components.DiaryDateHeader
import ph.mart.healthapp.feature.food.ui.components.DiarySummaryBar
import ph.mart.healthapp.feature.food.ui.components.DiaryWaterRow
import ph.mart.healthapp.feature.food.ui.components.FoodSearchPanel
import ph.mart.healthapp.feature.food.ui.components.FoodSuggestionPanel
import ph.mart.healthapp.feature.food.ui.components.RecipePanel
import ph.mart.healthapp.feature.food.ui.components.SavedMealPanel
import ph.mart.healthapp.feature.food.ui.components.ExerciseSection
import ph.mart.healthapp.feature.food.ui.components.MealSectionHeader

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

@Composable
private fun MealSection(
    mealType: MealType,
    entries: List<FoodEntry>,
    subtotalKcal: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAdd: () -> Unit,
    onSave: (() -> Unit)?,
    onDeleteEntry: (Long) -> Unit,
) {
    Column {
        MealSectionHeader(
            label = mealType.name,
            subtotalKcal = subtotalKcal,
            expanded = expanded,
            onToggle = onToggle,
            onAdd = onAdd,
            onSave = onSave,
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
    savedMeals: List<SavedMeal>,
    recipes: List<Recipe>,
    onSelectRecipe: (Recipe) -> Unit,
    onDeleteRecipe: (Recipe) -> Unit,
    onNewRecipe: () -> Unit,
    onLogSavedMeal: (SavedMeal) -> Unit,
    onDeleteSavedMeal: (SavedMeal) -> Unit,
    onFormChange: (AddEntryForm) -> Unit,
    onSelectProduct: (ScannedProduct) -> Unit,
    onSelectSuggestion: (FoodSuggestion) -> Unit,
    onLogAgain: (FoodSuggestion) -> Unit,
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
            RecipePanel(
                recipes = recipes,
                onSelect = onSelectRecipe,
                onDelete = onDeleteRecipe,
                onNewRecipe = onNewRecipe,
            )
            SavedMealPanel(
                savedMeals = savedMeals,
                onLog = onLogSavedMeal,
                onDelete = onDeleteSavedMeal,
            )
            FoodSuggestionPanel(
                suggestions = suggestions,
                onSelect = onSelectSuggestion,
                onLogAgain = onLogAgain,
                onToggleFavorite = onToggleFavorite,
            )
            FoodSearchPanel(onSelect = onSelectProduct)
            // ponytail: on a diary with recipes and recents, a quick add is still a scroll to the
            // bottom of the sheet. A compact kcal-only row at the top is the upgrade if that
            // friction shows up.
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Or add it yourself",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Leave the name blank to log calories only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
                portionUnitOptions = listOf("g", "oz", "cup", SERVING_UNIT),
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
                PrimaryButton(
                    // The label is the only thing telling the user a nameless entry will be
                    // accepted; the button itself is enabled the moment there are calories.
                    label = if (form.name.isBlank()) "Quick add" else "Add",
                    onClick = onAdd,
                    enabled = form.isValid(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Names the snapshot of [mealType]'s entries. Seeded with the meal's own name, so the fast path
 * is Save without typing; [itemCount] is there so the user can see what they're about to keep. */
@Composable
private fun SaveMealSheet(
    mealType: MealType,
    name: String,
    itemCount: Int,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss) {
        Text(
            text = "Save this ${mealType.name}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = "$itemCount ${if (itemCount == 1) "item" else "items"} — log them all again in one tap.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTextField(value = name, onValueChange = onNameChange, placeholder = "Name this meal")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryButton(label = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                PrimaryButton(label = "Save", onClick = onSave, enabled = name.isNotBlank(), modifier = Modifier.weight(1f))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SaveMealSheetPreview() {
    AppTheme {
        SaveMealSheet(
            mealType = MealType.Breakfast,
            name = "Usual breakfast",
            itemCount = 3,
            onNameChange = {},
            onDismiss = {},
            onSave = {},
        )
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
