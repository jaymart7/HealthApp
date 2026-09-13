package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.dailyTotals
import ph.mart.healthapp.core.data.health.dayBurnedKcal
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.diary.FoodEvent
import ph.mart.healthapp.feature.food.ui.diary.FoodScreenState
import ph.mart.healthapp.feature.food.ui.diary.FoodUiState
import ph.mart.healthapp.feature.food.ui.diary.dayBudgetKcal
import ph.mart.healthapp.feature.food.ui.diary.rememberFoodScreenState
import ph.mart.healthapp.feature.food.ui.shared.components.LabelledActionChip
import ph.mart.healthapp.feature.food.ui.shared.components.SectionRule

/**
 * Everything the diary *shows*: the date header (which holds the day's filter), the summary
 * bar, and the scrolling list — the three logging chips, water, the four meal cards, and the
 * exercise block below its rule.
 *
 * Two pinned blocks, not three. The filter moved into the header and the three logging doors
 * moved into the scroll, which is what buys back the vertical space a food row needs to be
 * visible without scrolling on a small screen.
 *
 * Split from the sheets and dialogs in [DiarySheets] rather than left inline: `FoodContent` was a
 * 291-line composable with a fan-out of 29, which is the shape `CLAUDE.md`'s file-breakdown rule
 * exists to prevent. The two halves have nothing in common but the state they read — the body
 * never opens anything, and the overlays never scroll.
 *
 * [snackbarHostState] arrives from the caller because the host is drawn there, above the docked
 * FAB, while the two Undo messages are raised from the rows in here.
 *
 * [twoPane] only reaches the date header: beside its own calendar the label stops being a door.
 * Everything else about the day is the same at every width — one scrolling column, per `CLAUDE.md`.
 */
@Composable
internal fun DiaryBody(
    uiState: FoodUiState,
    state: FoodScreenState,
    onEvent: (FoodEvent) -> Unit,
    onScanBarcode: (Long) -> Unit,
    onSpeakFood: (Long) -> Unit,
    onCapturePhoto: (Long) -> Unit,
    onOpenHistory: (Long, String) -> Unit,
    onOpenStrength: (Long, Long) -> Unit,
    onLogExercise: (Long, Long) -> Unit,
    onAskCoach: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    summaryCollapsed: Boolean = false,
    twoPane: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    // The snackbar text is built in a coroutine, outside composition — so the exercise type's
    // label is resolved through the context rather than with `stringResource`.
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        DiaryDateHeader(
            selectedDate = uiState.selectedDate,
            today = uiState.today,
            onSelectDate = { date -> onEvent(FoodEvent.OnSelectDate(date)) },
            onAskCoach = onAskCoach,
            onOpenCalendar = if (twoPane) null else ({ state.calendarOpen = true }),
            filterExpanded = state.filterExpanded,
            onFilterExpandedChange = { open ->
                if (open) state.filterExpanded = true else state.closeFilter()
            },
            query = state.searchQuery,
            onQueryChange = { state.searchQuery = it },
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
        )
        uiState.targets?.let { targets ->
            val burned = dayBurnedKcal(uiState.exercise, uiState.steps)
            DiarySummaryBar(
                consumed = uiState.entries.dailyTotals(),
                goalKcal = uiState.dayBudgetKcal(targets),
                proteinGoalG = targets.proteinG,
                carbsGoalG = targets.carbsG,
                fatGoalG = targets.fatG,
                nutrientTargets = uiState.nutrientTargets,
                collapsed = summaryCollapsed,
                // The same burn dayBudgetKcal() just folded in, so the credit line can never
                // claim a credit the goal above it did not actually receive.
                burnedKcal = burned,
                exerciseCredited = uiState.addExerciseToBudget,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        val dayIsEmpty = uiState.entries.isEmpty() && uiState.exercise.isEmpty()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = DockedFabContentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // The three fast paths that belong to the day being shown, so all three carry it: a
            // sentence, a scan or a photo taken while reviewing Tuesday is logged to Tuesday. The
            // FAB's copies of these carry 0 instead — it launches outside the diary's date context.
            //
            // They scroll rather than pinning, and they are labelled rather than bare glyphs: as
            // three unlabelled icons in the pinned area they asked the user to know what a
            // microphone did to a diary before tapping it, and they cost the screen a third pinned
            // block. Once they scroll away the FAB's sheet holds all three.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabelledActionChip(
                    label = stringResource(R.string.food_chip_say),
                    icon = AppIcons.Mic,
                    onClick = { onSpeakFood(uiState.selectedDate) },
                    modifier = Modifier.weight(1f),
                )
                LabelledActionChip(
                    label = stringResource(R.string.food_chip_scan),
                    icon = AppIcons.Barcode,
                    onClick = { onScanBarcode(uiState.selectedDate) },
                    modifier = Modifier.weight(1f),
                )
                LabelledActionChip(
                    label = stringResource(R.string.food_chip_photo),
                    icon = AppIcons.Camera,
                    onClick = { onCapturePhoto(uiState.selectedDate) },
                    modifier = Modifier.weight(1f),
                )
            }

            DiaryWaterRow(
                glasses = uiState.waterGlasses,
                goalGlasses = uiState.waterGoalGlasses,
                unit = uiState.unit,
                onSetGlasses = { glasses -> onEvent(FoodEvent.OnSetWaterGlasses(glasses)) },
            )

            MealType.entries.forEachIndexed { index, mealType ->
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
                    // On a bare day the mascot below speaks once for the whole screen, so
                    // the per-section line stays quiet rather than repeating it four times.
                    dayIsEmpty = dayIsEmpty,
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
                                message = context.getString(R.string.food_deleted, entry.name),
                                actionLabel = context.getString(R.string.food_undo),
                                duration = SnackbarDuration.Short,
                            ) == SnackbarResult.ActionPerformed
                            if (undone) onEvent(FoodEvent.OnRestoreEntry(entry))
                        }
                    },
                )
                // One mascot and one sentence in place of five identical "nothing logged" lines,
                // and it sits *inside* the run rather than above it: all four cards still render,
                // so the day reads as a day with a note in it. Only when the whole day is bare — a
                // single empty section between two full ones is not a first-run moment.
                if (dayIsEmpty && index == 1) {
                    EmptyDiaryDay(isToday = uiState.selectedDate == uiState.today)
                }
            }

            // Exercise leaves the run of meals here. The break plus the label is what stops a
            // fifth section reading as a fifth meal; the block below carries the rest of the
            // argument in its outline and its signed subtotal.
            SectionRule(
                label = stringResource(R.string.food_exercise_adds_today),
                modifier = Modifier.padding(top = 12.dp),
            )

            ExerciseSection(
                entries = uiState.exercise,
                unit = uiState.unit,
                expanded = state.exerciseExpanded,
                dayIsEmpty = dayIsEmpty,
                onToggle = { state.exerciseExpanded = !state.exerciseExpanded },
                onAdd = { onLogExercise(uiState.selectedDate, 0) },
                // A workout with sets reopens on the screen that can show them; everything
                // else reopens in the sheet that logged it.
                onEditEntry = { entry ->
                    if (entry.sets.isEmpty()) {
                        onLogExercise(uiState.selectedDate, entry.id)
                    } else {
                        onOpenStrength(uiState.selectedDate, entry.id)
                    }
                },
                onDeleteEntry = { entry ->
                    onEvent(FoodEvent.OnDeleteExercise(entry.id))
                    scope.launch {
                        val undone = snackbarHostState.showSnackbar(
                            message = context.getString(R.string.food_deleted, context.getString(entry.type.label)),
                            actionLabel = context.getString(R.string.food_undo),
                            duration = SnackbarDuration.Short,
                        ) == SnackbarResult.ActionPerformed
                        if (undone) onEvent(FoodEvent.OnRestoreExercise(entry))
                    }
                },
            )

            // Three doors at the foot of the scroll rather than in the date header — that row
            // already carries three 48dp buttons and a label it goes out of its way to protect at
            // large font scales. Home's "Rearrange your Home" link is the shape. The row wraps
            // rather than shrinking, so a large font scale costs a line and not a label.
            //
            // Search and copy are always here, including on a bare day: a day with nothing on it
            // is exactly when you want to look backwards, and exactly when you want yesterday
            // again. Sharing is not — there is no day to share yet. The search carries the day's
            // filter query along, so a word already typed into the header survives the step up to
            // every day.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                TextButton(
                    label = stringResource(R.string.food_history_link),
                    onClick = { onOpenHistory(uiState.selectedDate, state.searchQuery) },
                )
                TextButton(
                    label = stringResource(R.string.food_copy_link),
                    onClick = { state.copyPickerOpen = true },
                )
                if (!dayIsEmpty && uiState.targets != null) {
                    TextButton(
                        label = stringResource(R.string.food_share_day),
                        onClick = { state.shareOpen = true },
                    )
                }
            }
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
            onCapturePhoto = {},
            onOpenHistory = { _, _ -> },
            onOpenStrength = { _, _ -> },
            onLogExercise = { _, _ -> },
            onAskCoach = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
