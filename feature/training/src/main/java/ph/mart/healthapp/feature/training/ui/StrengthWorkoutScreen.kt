package ph.mart.healthapp.feature.training.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import ph.mart.healthapp.core.data.exercise.StrengthParseResult
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
import ph.mart.healthapp.feature.training.R
import ph.mart.healthapp.feature.training.ui.components.DescribeExerciseField
import ph.mart.healthapp.feature.training.ui.components.ExerciseFormFields
import ph.mart.healthapp.feature.training.ui.components.NO_REST
import ph.mart.healthapp.feature.training.ui.components.NameChipRow
import ph.mart.healthapp.feature.training.ui.components.REST_EXTEND_SECONDS
import ph.mart.healthapp.feature.training.ui.components.RestTimerCard
import ph.mart.healthapp.feature.training.ui.components.SaveRoutineSheet
import ph.mart.healthapp.feature.training.ui.components.StrengthSetEditor
import ph.mart.healthapp.feature.training.ui.components.StrengthSetList
import ph.mart.healthapp.feature.training.ui.components.canAdd

/** The rest a fresh screen offers — the middle of [ph.mart.healthapp.feature.training.ui.components.REST_CHOICES],
 * and the one most programmes are written around. */
private const val DEFAULT_REST_SECONDS = 90

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
    onSaved: (creditedKcal: Int) -> Unit = {},
    viewModel: LogExerciseViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    LaunchedEffect(editingId, routineId) {
        viewModel.handleEvent(LogExerciseEvent.OnOpenStrength(editingId, routineId))
    }
    // Every way off this route — save, discard, a clean back — lands here, so a parse still in
    // flight cannot leave `parsing` up on a ViewModel the log sheet may go on to share.
    DisposableEffect(Unit) {
        onDispose { viewModel.handleEvent(LogExerciseEvent.OnCancelParse) }
    }

    // Held back until the load answers. `rememberLogExerciseState` keys its saveable on the seed,
    // so composing a blank form first and re-seeding when the row lands would throw away whatever
    // had been typed in between — the guard `DiarySheets` already applies to the edit sheet.
    if (!uiState.strengthLoaded) {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {}
        return
    }

    // Held here rather than in the content, the sheet's shape: a parse's sets arrive as a side
    // effect, and the form they land in has to be in reach of the collector.
    val seed = remember(uiState.editing, uiState.seedRoutine) { uiState.strengthSeed() }
    val state = rememberLogExerciseState(seed)
    val describe = rememberDescribeState()

    viewModel.collectSideEffect { effect ->
        when (effect) {
            // [onExit] stays a bare pop, so the back and discard paths below are untouched — only
            // a *save* has a figure to report, and it reports it here.
            is LogExerciseSideEffect.Saved -> {
                onSaved(effect.creditedKcal)
                onExit()
            }

            // Appended, never replacing what is down — and no rest starts, because `commit()` is
            // the one place that does and these sets were lifted before anyone typed them.
            is LogExerciseSideEffect.SetsParsed -> when (val result = effect.result) {
                is StrengthParseResult.Success -> {
                    state.form = state.form.copy(sets = state.form.sets + result.sets)
                    describe.close()
                }

                // Both keep the sentence, the sheet's rule: correcting it beats retyping it.
                StrengthParseResult.NoLiftsFound ->
                    describe.message = R.string.training_strength_describe_none

                StrengthParseResult.Failed ->
                    describe.message = R.string.training_exercise_describe_failed
            }

            // The sheet's activity parse. The type is Strength by definition here, so this screen
            // asks for sets instead. Named rather than swept into an `else`, so adding another
            // side effect still fails here.
            is LogExerciseSideEffect.Parsed -> Unit
        }
    }

    StrengthWorkoutContent(
        uiState = uiState,
        dateEpochDay = dateEpochDay,
        editingId = editingId.takeIf { it > 0 },
        onExit = onExit,
        onEvent = viewModel::handleEvent,
        seed = seed,
        state = state,
        describe = describe,
        onDescribe = {
            describe.message = null
            if (viewModel.isOnline()) {
                viewModel.handleEvent(LogExerciseEvent.OnParseSets(describe.text))
            } else {
                // The set editor below is the manual path; saying so is the whole degrade.
                describe.message = R.string.training_exercise_describe_offline
            }
        },
    )
}

/**
 * Strength whatever it arrived as: this screen draws no type chips, and it can be reached from the
 * edit sheet with a cardio row already seeded in it. Saving sets against a Run is the one outcome
 * the missing chip row makes possible.
 *
 * A started routine seeds the same form the chip row would have — one seeding path, so an
 * opened-from-Home workout and a chip-tapped one are the same workout.
 */
private fun LogExerciseUiState.strengthSeed(): LogExerciseForm {
    val form = editing?.toLogExerciseForm()
        ?: seedRoutine?.let { LogExerciseForm(name = it.name, sets = it.toSets(lastLoads)) }
        ?: LogExerciseForm()
    return form.copy(type = ExerciseType.Strength)
}

@Composable
private fun StrengthWorkoutContent(
    uiState: LogExerciseUiState,
    dateEpochDay: Long,
    editingId: Long?,
    onExit: () -> Unit,
    onEvent: (LogExerciseEvent) -> Unit,
    // Defaulted for the previews, which have no ViewModel to hold them — the sheet's `describe`
    // and `onEstimate` defaults, for the same reason.
    seed: LogExerciseForm = uiState.strengthSeed(),
    state: LogExerciseState = rememberLogExerciseState(seed),
    describe: DescribeState = DescribeState(),
    onDescribe: () -> Unit = {},
) {
    val form = state.form.withEstimate(uiState.weightKg)

    // The in-progress set. Three primitives rather than a saver: each is Bundle-native on its own,
    // and the draft is worth keeping across a rotation for the same reason the form is.
    var draftName by rememberSaveable { mutableStateOf("") }
    var draftReps by rememberSaveable { mutableIntStateOf(0) }
    var draftKg by rememberSaveable { mutableDoubleStateOf(0.0) }
    var discardOpen by rememberSaveable { mutableStateOf(false) }
    val draft = StrengthSet(draftName, draftReps, draftKg)

    // The rest between sets: the chosen length, and when the running one is up (0 = not resting).
    // Two more Bundle-native primitives for the draft's reason — a rotation mid-rest must not
    // restart it — and deliberately not part of the form: a rest is not part of the workout, so it
    // is saved with nothing and makes nothing dirty.
    var restSeconds by rememberSaveable { mutableIntStateOf(DEFAULT_REST_SECONDS) }
    var restEndAt by rememberSaveable { mutableLongStateOf(NO_REST) }

    // The routine sheet's name, and what it was saved as. There is no toast or snackbar here (the
    // saved-meal path has none either), so the button reporting its own result is the confirmation.
    var routineName by rememberSaveable { mutableStateOf("") }
    var routineSheetOpen by rememberSaveable { mutableStateOf(false) }
    var savedRoutineName by rememberSaveable { mutableStateOf<String?>(null) }
    // Adding or removing a set makes it a different workout, so it can be saved again.
    LaunchedEffect(form.sets.size) { savedRoutineName = null }

    // The one place a set lands, so it is the one place a rest starts — "Add set" begins one and
    // nothing else does.
    fun commit(set: StrengthSet) {
        state.form = form.copy(sets = form.sets + set)
        if (restSeconds > 0) restEndAt = System.currentTimeMillis() + restSeconds * 1000L
    }

    // Back out of a half-written workout is the one destructive gesture here, so it only
    // intercepts once there is something to lose — an untouched screen pops like any other route.
    // The describe panel is a sub-level above that: back abandons a parse in flight, then closes
    // the panel, and only then asks about the workout. One handler rather than two, so which one
    // wins can't depend on which condition happened to become true first.
    val isDirty = state.form != seed || draft.canAdd()
    if (describe.open || isDirty) {
        val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(
            state = navigationState,
            onBackCompleted = {
                when {
                    describe.open && uiState.parsing -> onEvent(LogExerciseEvent.OnCancelParse)
                    describe.open -> describe.close()
                    else -> discardOpen = true
                }
            },
        )
    }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    // Before the scroll, so the set editor's weight and reps fields lift clear of the
                    // keyboard instead of sitting behind it.
                    .imePadding()
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
                        label = stringResource(R.string.training_strength_repeat),
                        onClick = { state.form = form.copy(sets = last.sets) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Same guard, same reason: a routine seeds the whole list, so offering it once a
                // set is down would overwrite what is already there.
                if (uiState.routines.isNotEmpty() && form.sets.isEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.training_strength_start_routine),
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

                // Appends rather than replaces, so unlike the two rows above it is offered whatever
                // is already down — correcting a logged workout included, where "and I forgot the
                // curls" is the likeliest sentence.
                DescribeExerciseField(
                    open = describe.open,
                    text = describe.text,
                    parsing = uiState.parsing,
                    onOpen = { describe.open = true },
                    onClose = describe::close,
                    onTextChange = {
                        describe.text = it
                        describe.message = null
                    },
                    onEstimate = onDescribe,
                    onCancel = { onEvent(LogExerciseEvent.OnCancelParse) },
                    message = describe.message?.let { stringResource(it) },
                    labelRes = R.string.training_strength_describe,
                    promptRes = R.string.training_strength_describe_prompt,
                    placeholderRes = R.string.training_strength_describe_placeholder,
                    submitRes = R.string.training_strength_describe_submit,
                    modifier = Modifier.fillMaxWidth(),
                )

                StrengthSetList(
                    sets = form.sets,
                    unit = uiState.preferredUnit,
                    onRemove = { index ->
                        state.form = form.copy(sets = form.sets.filterIndexed { i, _ -> i != index })
                    },
                )

                RestTimerCard(
                    endAtMillis = restEndAt,
                    durationSeconds = restSeconds,
                    onDurationChange = { restSeconds = it },
                    onExtend = { restEndAt += REST_EXTEND_SECONDS * 1000L },
                    onSkip = { restEndAt = NO_REST },
                    onFinished = { restEndAt = NO_REST },
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
                        label = savedRoutineName?.let { stringResource(R.string.training_strength_saved_routine, it) }
                            ?: stringResource(R.string.training_strength_save_routine),
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
                        label = stringResource(R.string.training_cancel),
                        onClick = { if (isDirty) discardOpen = true else onExit() },
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(
                        label = stringResource(R.string.training_strength_save_workout),
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
                    title = stringResource(
                        if (editingId == null) {
                            R.string.training_strength_discard_new
                        } else {
                            R.string.training_strength_discard_edit
                        },
                    ),
                    body = stringResource(R.string.training_not_saved),
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
                text = stringResource(
                    R.string.training_strength_summary,
                    pluralStringResource(R.plurals.training_strength_sets, sets.size, sets.size),
                    sets.distinctBy { it.exerciseName }.size,
                ),
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
