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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.dailyTotals
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.AppTextField
<<<<<<< HEAD
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
=======
import ph.mart.healthapp.core.designsystem.component.FabBottomClearance
>>>>>>> refs/heads/debug-seed-data
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.component.MacroInputGroup
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.ui.components.DiarySummaryBar
import ph.mart.healthapp.feature.food.ui.components.MealSectionHeader

@Composable
fun FoodScreen(scrollState: ScrollState = rememberScrollState(), viewModel: FoodViewModel = koinViewModel()) {
    val uiState by viewModel.collectAsState()
    val state = rememberFoodScreenState()
    FoodContent(uiState = uiState, state = state, scrollState = scrollState, onEvent = viewModel::handleEvent)
}

@Composable
private fun FoodContent(uiState: FoodUiState, state: FoodScreenState, onEvent: (FoodEvent) -> Unit, scrollState: ScrollState = rememberScrollState()) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppTextField(
                    value = state.searchQuery,
                    onValueChange = { state.searchQuery = it },
                    placeholder = "Search foods…",
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                )

                uiState.targets?.let { targets ->
                    DiarySummaryBar(
                        consumedKcal = uiState.entries.dailyTotals().calories,
                        goalKcal = targets.calories,
                        proteinGoalG = targets.proteinG,
                        carbsGoalG = targets.carbsG,
                        fatGoalG = targets.fatG,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
<<<<<<< HEAD
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = DockedFabContentPadding),
=======
                        .verticalScroll(scrollState)
                        .padding(bottom = FabBottomClearance),
>>>>>>> refs/heads/debug-seed-data
                ) {
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
                }
            }

            val activeMealSheet = state.activeMealSheet
            if (activeMealSheet != null) {
                AddEntrySheet(
                    mealType = activeMealSheet,
                    form = state.addForm,
                    onFormChange = { state.addForm = it },
                    onDismiss = state::closeSheet,
                    onAdd = {
                        onEvent(FoodEvent.OnAddEntry(state.addForm))
                        state.closeSheet()
                    },
                )
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
    onFormChange: (AddEntryForm) -> Unit,
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
            uiState = FoodUiState(entries = entries, targets = targets),
            state = FoodScreenState(),
            onEvent = {},
        )
    }
}
