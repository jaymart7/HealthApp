package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ph.mart.healthapp.core.data.exercise.budgetKcal
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.dailyTotals
import ph.mart.healthapp.core.data.health.dayBurnedKcal
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.ui.diary.FoodEvent
import ph.mart.healthapp.feature.food.ui.diary.FoodScreenState
import ph.mart.healthapp.feature.food.ui.diary.FoodUiState
import ph.mart.healthapp.feature.food.ui.diary.rememberFoodScreenState
import ph.mart.healthapp.feature.food.ui.exercise.components.ExerciseSection

/**
 * Everything the diary *shows*: the date header, the day's filter, the summary bar, and the
 * scrolling list of meal sections and exercise.
 *
 * Split from the sheets and dialogs in [DiarySheets] rather than left inline: `FoodContent` was a
 * 291-line composable with a fan-out of 29, which is the shape `CLAUDE.md`'s file-breakdown rule
 * exists to prevent. The two halves have nothing in common but the state they read — the body
 * never opens anything, and the overlays never scroll.
 *
 * [snackbarHostState] arrives from the caller because the host is drawn there, above the docked
 * FAB, while the two Undo messages are raised from the rows in here.
 */
@Composable
internal fun DiaryBody(
    uiState: FoodUiState,
    state: FoodScreenState,
    onEvent: (FoodEvent) -> Unit,
    onScanBarcode: (Long) -> Unit,
    onSpeakFood: (Long) -> Unit,
    onOpenStrength: (Long, Long) -> Unit,
    snackbarHostState: SnackbarHostState,
    scrollState: ScrollState = rememberScrollState(),
) {
    val scope = rememberCoroutineScope()

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
            // The two fast paths that belong to the day being shown, so both carry it: a scan or a
            // sentence taken while reviewing Tuesday is logged to Tuesday.
            IconButton(onClick = { onSpeakFood(uiState.selectedDate) }, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = AppIcons.Mic,
                    contentDescription = "Say what you ate",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
                unit = uiState.unit,
                expanded = state.exerciseExpanded,
                onToggle = { state.exerciseExpanded = !state.exerciseExpanded },
                onAdd = { state.openExerciseSheet() },
                // A workout with sets reopens on the screen that can show them; everything
                // else reopens in the sheet that logged it.
                onEditEntry = { entry ->
                    if (entry.sets.isEmpty()) {
                        state.openExerciseSheet(entry)
                    } else {
                        onOpenStrength(uiState.selectedDate, entry.id)
                    }
                },
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
}

@PreviewLightDark
@Composable
private fun DiaryBodyPreview() {
    AppTheme {
        DiaryBody(
            uiState = FoodUiState(
                targets = DailyTargets(calories = 2200, proteinG = 165, carbsG = 220, fatG = 73, floor = 1500),
                waterGlasses = 4,
            ),
            state = rememberFoodScreenState(),
            onEvent = {},
            onScanBarcode = {},
            onSpeakFood = {},
            onOpenStrength = { _, _ -> },
            snackbarHostState = SnackbarHostState(),
        )
    }
}
