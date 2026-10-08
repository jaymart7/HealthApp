package ph.mart.healthapp.feature.training.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import ph.mart.healthapp.core.designsystem.component.SheetActionBar
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
import ph.mart.healthapp.feature.training.ui.components.WorkoutDetailsDisclosure
import ph.mart.healthapp.feature.training.ui.components.canAdd

/** The rest a fresh screen offers — the middle of [ph.mart.healthapp.feature.training.ui.components.REST_CHOICES],
 * and the one most programmes are written around. */
private const val DEFAULT_REST_SECONDS = 90

/** The editor is adding, not correcting a set already down. */
private const val NOT_EDITING = -1

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
 * a routine to start from, which Home's training-plan card and the log sheet's chips name.
 * [draft] is the log sheet's form as it stood when it handed over — see [StrengthWorkoutRoute].
 * [buildRoutine] turns the screen into Profile's routine builder: the set list and its editor,
 * with a pinned "Save routine" that names it and goes back — no workout is logged.
 */
@Composable
fun StrengthWorkoutScreen(
    dateEpochDay: Long,
    editingId: Long,
    routineId: Long = 0,
    draft: LogExerciseForm? = null,
    buildRoutine: Boolean = false,
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
    // effect, and the form they land in has to be in reach of the collector. The seed stays the
    // measure of "unsaved" even when the form starts from the sheet's draft.
    val seed = remember(uiState.editing, uiState.seedRoutine) { uiState.strengthSeed() }
    val state = rememberLogExerciseState(seed, start = strengthStart(seed, draft))
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
                    describe.clear()
                }

                // Both keep the sentence, the sheet's rule: correcting it beats retyping it.
                StrengthParseResult.NoLiftsFound ->
                    describe.message = R.string.training_strength_describe_none

                StrengthParseResult.Failed ->
                    describe.message = R.string.training_exercise_describe_failed
            }

            // The sheet's activity parse, and the sheet's delete. Neither happens on this screen.
            // Named rather than swept into an `else`, so adding another side effect still fails
            // here.
            is LogExerciseSideEffect.Parsed -> Unit
            is LogExerciseSideEffect.Deleted -> Unit
        }
    }

    StrengthWorkoutContent(
        uiState = uiState,
        dateEpochDay = dateEpochDay,
        editingId = editingId.takeIf { it > 0 },
        buildRoutine = buildRoutine,
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
 * The form this screen would open on with nothing handed over: the row being corrected, as it was
 * logged, or a new Strength workout — started from a routine when one was named. A started routine
 * seeds the same form the chip row would have, so an opened-from-Home workout and a chip-tapped
 * one are the same workout.
 *
 * The row keeps its own type here, because this is what "unsaved" is measured against: a Run the
 * sheet switched to Strength before handing over has to read as a change. Internal for
 * `LogExerciseFormTest`.
 */
internal fun LogExerciseUiState.strengthSeed(): LogExerciseForm =
    editing?.toLogExerciseForm()
        ?: seedRoutine?.let { LogExerciseForm(type = ExerciseType.Strength, name = it.name, sets = it.toSets(lastLoads)) }
        ?: LogExerciseForm(type = ExerciseType.Strength)

/**
 * Where the form begins: the sheet's [draft] when it handed one over — a typed note, a corrected
 * duration, a sentence's sets — and the [seed] otherwise. Strength either way: this screen draws
 * no type chips, so saving sets against a Run is the one outcome a missing chip row could cause.
 */
internal fun strengthStart(seed: LogExerciseForm, draft: LogExerciseForm?): LogExerciseForm =
    (draft ?: seed).copy(type = ExerciseType.Strength)

/**
 * True when this form says something [seed] doesn't — what decides whether back has to ask.
 * Compared after estimating both, because a burn the app worked out is not something the user
 * wrote: a draft whose only act was picking Strength carries an estimate the blank seed lacks,
 * and must not read as a change. Internal for `LogExerciseFormTest`.
 */
internal fun LogExerciseForm.unsavedAgainst(seed: LogExerciseForm, weightKg: Double): Boolean =
    withEstimate(weightKg) != seed.withEstimate(weightKg)

@Composable
private fun StrengthWorkoutContent(
    uiState: LogExerciseUiState,
    dateEpochDay: Long,
    editingId: Long?,
    onExit: () -> Unit,
    onEvent: (LogExerciseEvent) -> Unit,
    buildRoutine: Boolean = false,
    // Defaulted for the previews, which have no ViewModel to hold them — the sheet's `describe`
    // and `onEstimate` defaults, for the same reason.
    seed: LogExerciseForm = uiState.strengthSeed(),
    state: LogExerciseState = rememberLogExerciseState(seed),
    describe: DescribeState = DescribeState(),
    onDescribe: () -> Unit = {},
    initialEditingSet: Int = NOT_EDITING,
) {
    val form = state.form.withEstimate(uiState.weightKg)
    val correcting = editingId != null
    // A live session: the running total, the sentence and the rest timer. Neither a correction
    // nor a routine being built is one — a routine keeps no loads, no duration and no rests.
    val session = !correcting && !buildRoutine

    // The in-progress set. Three primitives rather than a saver: each is Bundle-native on its own,
    // and the draft is worth keeping across a rotation for the same reason the form is. Blank,
    // unless the preview opens on a set already being edited.
    val initialDraft = state.form.sets.getOrNull(initialEditingSet)
    var draftName by rememberSaveable { mutableStateOf(initialDraft?.exerciseName.orEmpty()) }
    var draftReps by rememberSaveable { mutableIntStateOf(initialDraft?.reps ?: 0) }
    var draftKg by rememberSaveable { mutableDoubleStateOf(initialDraft?.weightKg ?: 0.0) }
    var discardOpen by rememberSaveable { mutableStateOf(false) }
    val draft = StrengthSet(draftName, draftReps, draftKg)

    // The set being corrected, as its index in the flat list — tapping a row loads it into the
    // editor, which then updates or removes it in place. Saveable for the draft's reason.
    var editingSet by rememberSaveable { mutableIntStateOf(initialEditingSet) }
    val editedSet = form.sets.getOrNull(editingSet)
    val editorRequester = remember { BringIntoViewRequester() }
    // The list sits above the editor, so a tapped set would otherwise load somewhere off screen.
    LaunchedEffect(editingSet) { if (editingSet != NOT_EDITING) editorRequester.bringIntoView() }

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
    // Adding, removing or correcting a set makes it a different workout, so it can be saved again.
    LaunchedEffect(form.sets) { savedRoutineName = null }

    fun setDraft(set: StrengthSet) {
        draftName = set.exerciseName
        draftReps = set.reps
        draftKg = set.weightKg
    }

    // Leaving a set edit hands the editor back. A live session goes on repeating its last set —
    // nearly always what the draft held before the tap — and a correction goes back to blank, so a
    // look at one set doesn't leave the screen thinking something changed.
    fun stopEditingSet(sets: List<StrengthSet> = state.form.sets) {
        editingSet = NOT_EDITING
        setDraft(if (correcting) StrengthSet("", 0, 0.0) else sets.lastOrNull() ?: StrengthSet("", 0, 0.0))
    }

    // The one place a set lands, so it is the one place a rest starts — "Add set" begins one and
    // nothing else does. Updating a set in place starts none: it was lifted already.
    fun commit(set: StrengthSet) {
        if (editedSet != null) {
            val sets = form.sets.mapIndexed { i, old -> if (i == editingSet) set else old }
            state.form = form.copy(sets = sets)
            stopEditingSet(sets)
            return
        }
        state.form = form.copy(sets = form.sets + set)
        if (restSeconds > 0) restEndAt = System.currentTimeMillis() + restSeconds * 1000L
    }

    // Back out of a half-written workout is the one destructive gesture here, so it only
    // intercepts once there is something to lose — an untouched screen pops like any other route.
    // A draft the sheet handed over counts when it differs from the row or the blank this would
    // otherwise have opened on: a sentence's sets, a corrected duration. Two sub-levels sit above
    // that, and back leaves them first — a parse in flight, then a set being edited. One handler
    // rather than three, so which one wins can't depend on which became true first.
    val isDirty = form.unsavedAgainst(seed, uiState.weightKg) || draft.canAdd()
    if (uiState.parsing || editedSet != null || isDirty) {
        val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(
            state = navigationState,
            onBackCompleted = {
                when {
                    uiState.parsing -> onEvent(LogExerciseEvent.OnCancelParse)
                    editedSet != null -> stopEditingSet()
                    else -> discardOpen = true
                }
            },
        )
    }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Before the scroll, so the set editor's weight and reps fields — and the pinned save
            // bar — lift clear of the keyboard instead of sitting behind it.
            Column(modifier = Modifier.fillMaxSize().imePadding()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        // No docked FAB over this route, so no clearance to reserve for one.
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 24.dp),
                ) {
                    // Correcting a logged workout is not a session: no running total to watch, no
                    // sentence to say, no rest to time. The set list and its editor are the whole
                    // job, so the session tools below are a new workout's only. The total waits for
                    // a first set — "0 sets · 0 lifted" over an empty list is noise, not a figure.
                    if (session && form.sets.isNotEmpty()) {
                        VolumeSummary(sets = form.sets, unit = uiState.preferredUnit)
                    }

                    // Appends rather than replaces, so it is offered whatever is already down.
                    // First, because saying the session is the quick path and the editor below is
                    // the correction. Not on a routine: the New routine sheet that opened it is
                    // the AI path, one back away.
                    if (session) {
                        DescribeExerciseField(
                            text = describe.text,
                            parsing = uiState.parsing,
                            onTextChange = {
                                describe.text = it
                                describe.message = null
                            },
                            onEstimate = onDescribe,
                            onCancel = { onEvent(LogExerciseEvent.OnCancelParse) },
                            message = describe.message?.let { stringResource(it) },
                            promptRes = R.string.training_strength_describe_prompt,
                            placeholderRes = R.string.training_strength_describe_placeholder,
                            submitRes = R.string.training_strength_describe_submit,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // The last session and the routines are one question — what to start from — so
                    // they are one row. Offered only on an empty list: each seeds the whole of it,
                    // and once a set is down that would overwrite it, and the discard question is
                    // the wrong one to ask for a chip.
                    val last = uiState.lastWorkout
                    if (form.sets.isEmpty() && (last != null || uiState.routines.isNotEmpty())) {
                        val lastLabel = stringResource(R.string.training_strength_last_workout)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.training_strength_start_from),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            NameChipRow(
                                names = listOfNotNull(lastLabel.takeIf { last != null }) + uiState.routines.map { it.name },
                                // Names are what the row shows, so the tapped one is what finds its
                                // seed back — two routines sharing a name seed the newer, the chip
                                // nearer the start. *ponytail: a routine named exactly "Last
                                // workout" is shadowed by the chip in front of it; match by index if
                                // it ever matters.*
                                onSelect = { name ->
                                    val sets = if (last != null && name == lastLabel) {
                                        last.sets
                                    } else {
                                        uiState.routines.firstOrNull { it.name == name }?.toSets(uiState.lastLoads)
                                    }
                                    if (sets != null) state.form = form.copy(sets = sets)
                                },
                            )
                        }
                    }

                    StrengthSetList(
                        sets = form.sets,
                        unit = uiState.preferredUnit,
                        selected = editingSet,
                        onSelect = { index ->
                            editingSet = index
                            setDraft(form.sets[index])
                        },
                    )

                    if (session) {
                        RestTimerCard(
                            endAtMillis = restEndAt,
                            durationSeconds = restSeconds,
                            onDurationChange = { restSeconds = it },
                            onExtend = { restEndAt += REST_EXTEND_SECONDS * 1000L },
                            onSkip = { restEndAt = NO_REST },
                            onFinished = { restEndAt = NO_REST },
                        )
                    }

                    StrengthSetEditor(
                        draft = draft,
                        unit = uiState.preferredUnit,
                        recentLifts = uiState.recentLifts,
                        onDraftChange = { next ->
                            // Naming a lift that has history fills in what was on the bar last
                            // time — the "Last:" line under the field, one tap closer. Only into an
                            // empty draft, so a number already typed is never overwritten.
                            val lastTop = uiState.lastLifts[next.exerciseName.liftKey()]?.topSet
                            val prefill = next.exerciseName != draftName && draftReps == 0 && draftKg == 0.0
                            setDraft(
                                if (prefill && lastTop != null) next.copy(reps = lastTop.reps, weightKg = lastTop.weightKg) else next,
                            )
                        },
                        lastPerformance = uiState.lastLifts[draftName.liftKey()],
                        // The draft deliberately survives the commit: three sets of the same lift
                        // at the same load is the shape of most programmes, so pressing Add again
                        // *is* the repeat gesture and no second button is needed for it.
                        onAdd = { commit(draft) },
                        editingLabel = editedSet?.let { set ->
                            stringResource(
                                R.string.training_strength_editing_set,
                                form.sets.take(editingSet + 1).count { it.exerciseName == set.exerciseName },
                                set.exerciseName.ifBlank { stringResource(R.string.training_strength_unnamed) },
                            )
                        },
                        onRemove = {
                            val sets = form.sets.filterIndexed { i, _ -> i != editingSet }
                            state.form = form.copy(sets = sets)
                            stopEditingSet(sets)
                        },
                        onCancelEdit = { stopEditingSet() },
                        modifier = Modifier.bringIntoViewRequester(editorRequester),
                    )

                    // A routine keeps none of these — the name is asked for when it is saved.
                    if (!buildRoutine) {
                        WorkoutDetailsDisclosure(minutes = form.minutes, burnedKcal = form.burnedKcal) {
                            ExerciseFormFields(
                                form = form,
                                weightKg = uiState.weightKg,
                                onFormChange = { state.form = it },
                                showTypeChips = false,
                            )
                        }
                    }

                    // On a routine this is the pinned button's job.
                    if (!buildRoutine && form.sets.isNotEmpty()) {
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
                }

                // Pinned under the scroll, the sheets' `bottomBar` argument: a long session's save
                // must not cost a scroll past every set already down.
                SheetActionBar {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        // A correction leaves by back, which still asks before dropping an edit.
                        if (!correcting) {
                            SecondaryButton(
                                label = stringResource(R.string.training_cancel),
                                onClick = { if (isDirty) discardOpen = true else onExit() },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (buildRoutine) {
                            // Names it first, in the sheet "Save as routine" opens — the screen
                            // has no name field of its own.
                            PrimaryButton(
                                label = stringResource(R.string.training_routine_save),
                                onClick = {
                                    routineName = form.name.trim()
                                    routineSheetOpen = true
                                },
                                enabled = form.sets.toRoutineLifts().isNotEmpty(),
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            PrimaryButton(
                                label = stringResource(R.string.training_strength_save_workout),
                                onClick = { onEvent(LogExerciseEvent.OnSave(form, dateEpochDay, editingId)) },
                                // The sheet's guard, plus a set for a new workout: a blank screen
                                // must not log 30 minutes of nothing. A set-less strength entry is
                                // the sheet's own Save; a correction keeps the sheet's rule, so a
                                // row logged before sets existed stays fixable.
                                enabled = form.isValid() && (correcting || form.sets.isNotEmpty()),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
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
                        // Back to Workout routines, where the new card is waiting. Safe to pop
                        // at once: the ViewModel is the activity's, so the insert outlives this
                        // route.
                        if (buildRoutine) onExit()
                    },
                )
            }

            if (discardOpen) {
                DiscardConfirmDialog(
                    title = stringResource(
                        when {
                            buildRoutine -> R.string.training_strength_discard_routine
                            editingId == null -> R.string.training_strength_discard_new
                            else -> R.string.training_strength_discard_edit
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

/** A logged set tapped: highlighted in the list, loaded into the editor, which offers Update and
 * Remove in place of Add. */
@PreviewLightDark
@Composable
private fun StrengthWorkoutScreenEditingSetPreview() {
    AppTheme {
        StrengthWorkoutContent(
            uiState = LogExerciseUiState(
                weightKg = 74.0,
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
            initialEditingSet = 1,
        )
    }
}

/** A fresh workout with a session to repeat and a routine to start — the state the Start from row
 * exists for. */
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
                routines = listOf(Routine(id = 1, name = "Push day", lifts = emptyList())),
            ),
            dateEpochDay = 0,
            editingId = null,
            onExit = {},
            onEvent = {},
        )
    }
}

/** Profile's "Build from a workout instead": the set list, its editor and Start from — no session
 * tools, no Details — with Save routine pinned, enabled once a set is down. */
@PreviewLightDark
@Composable
private fun StrengthWorkoutScreenBuildRoutinePreview() {
    AppTheme {
        StrengthWorkoutContent(
            uiState = LogExerciseUiState(
                weightKg = 74.0,
                recentLifts = listOf("Bench press", "Squat"),
                strengthLoaded = true,
                seedRoutine = Routine(
                    id = 1,
                    name = "Push day",
                    lifts = listOf(RoutineLift("Bench press", sets = 3, reps = 8), RoutineLift("Dip", sets = 2, reps = 10)),
                ),
            ),
            dateEpochDay = 0,
            editingId = null,
            onExit = {},
            onEvent = {},
            buildRoutine = true,
        )
    }
}
