package ph.mart.healthapp.feature.food.ui.exercise

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.LiftPerformance
import ph.mart.healthapp.core.data.exercise.Routine
import ph.mart.healthapp.core.data.exercise.RoutineLift
import ph.mart.healthapp.core.data.exercise.StrengthSet
import ph.mart.healthapp.core.data.exercise.liftKey
import ph.mart.healthapp.core.data.exercise.toRoutineLifts
import ph.mart.healthapp.core.data.exercise.toSets
import ph.mart.healthapp.core.data.exercise.volumeKg
import ph.mart.healthapp.core.data.exercise.volumeLabel
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.designsystem.component.DiscardConfirmDialog
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.food.ui.exercise.components.ExerciseFormFields
import ph.mart.healthapp.feature.food.ui.exercise.components.NameChipRow
import ph.mart.healthapp.feature.food.ui.exercise.components.SaveRoutineSheet
import ph.mart.healthapp.feature.food.ui.exercise.components.StrengthSetEditor
import ph.mart.healthapp.feature.food.ui.exercise.components.StrengthSetList
import ph.mart.healthapp.feature.food.ui.exercise.components.canAdd

/**
 * Authors a strength workout: the duration and burn every activity carries, plus what was actually
 * lifted. Saving writes one ordinary [ExerciseEntry] with its sets attached — the streak,
 * `budgetKcal()` and the diary need no special case for it.
 *
 * A screen rather than a sub-view of the log-exercise sheet, for the reason the recipe builder
 * gives: a list plus its editor doesn't fit above a keyboard.
 *
 * [editingId] of 0 is a new workout. Non-zero names a logged one, which the ViewModel resolves —
 * the route carries an id, not the row. [routineId] is the same shape for the opposite direction:
 * a routine to start from, which Home's training-plan card names when it opens this screen.
 */
@Composable
fun StrengthWorkoutScreen(
    dateEpochDay: Long,
    editingId: Long,
    routineId: Long = 0,
    onExit: () -> Unit,
    viewModel: LogExerciseViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    LaunchedEffect(editingId, routineId) {
        viewModel.handleEvent(LogExerciseEvent.OnOpenStrength(editingId, routineId))
    }
    viewModel.collectSideEffect { effect ->
        when (effect) {
            LogExerciseSideEffect.Saved -> onExit()
        }
    }

    // Held back until the load answers. `rememberLogExerciseState` keys its saveable on the seed,
    // so composing a blank form first and re-seeding when the row lands would throw away whatever
    // had been typed in between — the guard `DiarySheets` already applies to the edit sheet.
    if (!uiState.strengthLoaded) {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {}
        return
    }

    StrengthWorkoutContent(
        uiState = uiState,
        dateEpochDay = dateEpochDay,
        editingId = editingId.takeIf { it > 0 },
        onExit = onExit,
        onEvent = viewModel::handleEvent,
    )
}

@Composable
private fun StrengthWorkoutContent(
    uiState: LogExerciseUiState,
    dateEpochDay: Long,
    editingId: Long?,
    onExit: () -> Unit,
    onEvent: (LogExerciseEvent) -> Unit,
) {
    // Strength whatever it arrived as: this screen draws no type chips, and it can be reached
    // from the edit sheet with a cardio row already seeded in it. Saving sets against a Run is
    // the one outcome the missing chip row makes possible.
    //
    // A started routine seeds the same form the chip row below would have — one seeding path, so
    // an opened-from-Home workout and a chip-tapped one are the same workout.
    val seed = remember(uiState.editing, uiState.seedRoutine) {
        val started = uiState.seedRoutine
        val form = uiState.editing?.toLogExerciseForm()
            ?: started?.let { LogExerciseForm(name = it.name, sets = it.toSets(uiState.lastLoads)) }
            ?: LogExerciseForm()
        form.copy(type = ExerciseType.Strength)
    }
    val state = rememberLogExerciseState(seed)
    val form = state.form.withEstimate(uiState.weightKg)

    // The in-progress set. Three primitives rather than a saver: each is Bundle-native on its own,
    // and the draft is worth keeping across a rotation for the same reason the form is.
    var draftName by rememberSaveable { mutableStateOf("") }
    var draftReps by rememberSaveable { mutableIntStateOf(0) }
    var draftKg by rememberSaveable { mutableDoubleStateOf(0.0) }
    var discardOpen by rememberSaveable { mutableStateOf(false) }
    val draft = StrengthSet(draftName, draftReps, draftKg)

    // The routine sheet's name, and what it was saved as. There is no toast or snackbar here (the
    // saved-meal path has none either), so the button reporting its own result is the confirmation.
    var routineName by rememberSaveable { mutableStateOf("") }
    var routineSheetOpen by rememberSaveable { mutableStateOf(false) }
    var savedRoutineName by rememberSaveable { mutableStateOf<String?>(null) }
    // Adding or removing a set makes it a different workout, so it can be saved again.
    LaunchedEffect(form.sets.size) { savedRoutineName = null }

    fun commit(set: StrengthSet) {
        state.form = form.copy(sets = form.sets + set)
    }

    // Back out of a half-written workout is the one destructive gesture here, so it only
    // intercepts once there is something to lose — an untouched screen pops like any other route.
    val isDirty = state.form != seed || draft.canAdd()
    if (isDirty) {
        val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(
            state = navigationState,
            onBackCompleted = { discardOpen = true },
        )
    }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    // No docked FAB over this route, so no clearance to reserve for one.
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 24.dp),
            ) {
                VolumeSummary(sets = form.sets, unit = uiState.preferredUnit)

                // Offered only on an untouched new workout: once a set is down, "repeat" would
                // overwrite what is already there, and the discard question is the wrong one to
                // ask for a button press.
                val last = uiState.lastWorkout
                if (last != null && form.sets.isEmpty()) {
                    SecondaryButton(
                        label = "Repeat last workout",
                        onClick = { state.form = form.copy(sets = last.sets) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Same guard, same reason: a routine seeds the whole list, so offering it once a
                // set is down would overwrite what is already there.
                if (uiState.routines.isNotEmpty() && form.sets.isEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Start a routine",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        NameChipRow(
                            names = uiState.routines.map { it.name },
                            // Names are what the row shows, so the tapped one is what finds the
                            // routine back — two routines sharing a name seed the newer, which is
                            // the one the chip nearer the start is.
                            onSelect = { name ->
                                uiState.routines.firstOrNull { it.name == name }?.let { routine ->
                                    state.form = form.copy(sets = routine.toSets(uiState.lastLoads))
                                }
                            },
                        )
                    }
                }

                StrengthSetList(
                    sets = form.sets,
                    unit = uiState.preferredUnit,
                    onRemove = { index ->
                        state.form = form.copy(sets = form.sets.filterIndexed { i, _ -> i != index })
                    },
                )

                StrengthSetEditor(
                    draft = draft,
                    unit = uiState.preferredUnit,
                    recentLifts = uiState.recentLifts,
                    onDraftChange = {
                        draftName = it.exerciseName
                        draftReps = it.reps
                        draftKg = it.weightKg
                    },
                    lastPerformance = uiState.lastLifts[draftName.liftKey()],
                    // The draft deliberately survives the commit: three sets of the same lift at
                    // the same load is the shape of most programmes, so pressing Add again *is*
                    // the repeat gesture and no second button is needed for it.
                    onAdd = { commit(draft) },
                )

                ExerciseFormFields(
                    form = form,
                    weightKg = uiState.weightKg,
                    onFormChange = { state.form = it },
                    showTypeChips = false,
                )

                if (form.sets.isNotEmpty()) {
                    SecondaryButton(
                        label = savedRoutineName?.let { "Saved as \"$it\"" } ?: "Save as routine",
                        onClick = {
                            routineName = form.name.trim()
                            routineSheetOpen = true
                        },
                        enabled = savedRoutineName == null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryButton(
                        label = "Cancel",
                        onClick = { if (isDirty) discardOpen = true else onExit() },
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(
                        label = "Save workout",
                        onClick = { onEvent(LogExerciseEvent.OnSave(form, dateEpochDay, editingId)) },
                        // The same guard the sheet uses: a workout is still a duration. A session
                        // with no sets saves as the plain strength entry it always was.
                        enabled = form.isValid(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (routineSheetOpen) {
                val lifts = form.sets.toRoutineLifts()
                SaveRoutineSheet(
                    name = routineName,
                    liftCount = lifts.size,
                    setCount = form.sets.size,
                    onNameChange = { routineName = it },
                    onDismiss = { routineSheetOpen = false },
                    onSave = {
                        onEvent(LogExerciseEvent.OnSaveRoutine(routineName, lifts))
                        savedRoutineName = routineName.trim()
                        routineSheetOpen = false
                    },
                )
            }

            if (discardOpen) {
                DiscardConfirmDialog(
                    title = if (editingId == null) "Discard this workout?" else "Discard these changes?",
                    body = "It hasn't been saved yet.",
                    onConfirm = {
                        discardOpen = false
                        onExit()
                    },
                    onDismiss = { discardOpen = false },
                )
            }
        }
    }
}

/** Total volume — the number the screen exists to produce, so it sits above the sets rather than
 * under them, exactly where the recipe builder puts its per-serving figure. */
@Composable
private fun VolumeSummary(sets: List<StrengthSet>, unit: UnitSystem) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = volumeLabel(sets.volumeKg(), unit),
                style = MaterialTheme.typography.titleMedium.tabularNums,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${sets.size} ${if (sets.size == 1) "set" else "sets"} · " +
                    "${sets.distinctBy { it.exerciseName }.size} lifted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun StrengthWorkoutScreenPreview() {
    AppTheme {
        StrengthWorkoutContent(
            uiState = LogExerciseUiState(
                weightKg = 74.0,
                recentLifts = listOf("Bench press", "Squat", "Row"),
                lastLifts = mapOf(
                    "bench press" to LiftPerformance(
                        exerciseName = "Bench press",
                        dateEpochDay = 20_000,
                        topSet = StrengthSet("Bench press", reps = 8, weightKg = 57.5),
                        sets = 3,
                    ),
                ),
                strengthLoaded = true,
                editing = ExerciseEntry(
                    id = 1,
                    type = ExerciseType.Strength,
                    name = "Push day",
                    minutes = 45,
                    burnedKcal = 260,
                    sets = listOf(
                        StrengthSet("Bench press", 8, 60.0),
                        StrengthSet("Bench press", 8, 62.5),
                        StrengthSet("Dip", 10, 0.0),
                    ),
                ),
            ),
            dateEpochDay = 0,
            editingId = 1,
            onExit = {},
            onEvent = {},
        )
    }
}

/** A fresh workout with a session to repeat and routines to start — the state those two rows
 * exist for. */
@PreviewLightDark
@Composable
private fun StrengthWorkoutScreenEmptyPreview() {
    AppTheme {
        StrengthWorkoutContent(
            uiState = LogExerciseUiState(
                weightKg = 74.0,
                recentLifts = listOf("Bench press", "Squat"),
                strengthLoaded = true,
                lastWorkout = ExerciseEntry(
                    id = 9,
                    type = ExerciseType.Strength,
                    minutes = 45,
                    burnedKcal = 260,
                    sets = listOf(StrengthSet("Squat", 5, 100.0)),
                ),
            ),
            dateEpochDay = 0,
            editingId = null,
            onExit = {},
            onEvent = {},
        )
    }
}
