package ph.mart.healthapp.feature.training.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
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
import ph.mart.healthapp.core.data.exercise.ExerciseParseResult
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.Routine
import ph.mart.healthapp.core.data.exercise.StrengthSet
import ph.mart.healthapp.core.data.exercise.summaryLabel
import ph.mart.healthapp.core.data.resolve
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SheetActionBar
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.training.R
import ph.mart.healthapp.feature.training.ui.components.DescribeExerciseField
import ph.mart.healthapp.feature.training.ui.components.ExerciseFormFields
import ph.mart.healthapp.feature.training.ui.components.ExerciseSetsRow
import ph.mart.healthapp.feature.training.ui.components.NameChipRow

/** [dateEpochDay] is the day the entry lands on — the diary passes its selected day; 0 is today,
 * which the repository stamps.
 *
 * [editingId] names the logged activity being corrected, `0` for a new one — the same sheet,
 * seeded and saving over that row instead of adding one. An **id**, not the row: `AppScaffold` is
 * the one host of this sheet now that `:feature:food` cannot import it, and its sheet state is
 * `rememberSaveable`, which an `ExerciseEntry` is not.
 *
 * [onSaved] carries what the save just added to today's budget — 0 when there is nothing to say.
 * It is separate from [onDismiss] rather than folded into it because a dismiss also happens on a
 * swipe and a cancel, neither of which earned anything; see [LogExerciseSideEffect.Saved].
 *
 * [onOpenStrength] leaves for the strength workout screen, and the sheet builds the route itself:
 * it is this module's, and the sheet already knows the day and the row. Three doors lead there — a
 * sentence that named lifts, a routine chip, and the Sets row once Strength is the type, which
 * carries the form across as the route's `draft` so nothing typed here is lost on the way. The
 * diary reopens every row here, sets or not, so tapping one always does the same thing. The Sets
 * row is a door rather than an automatic redirect on purpose: the plain duration-and-kcal path is
 * what an imported watch session is, and it stays reachable.
 *
 * [onDeleted] hands the host a resolved "Deleted Run" and the undo for it; the sheet closes itself
 * after, the order [onSaved] keeps. */
@Composable
fun LogExerciseSheet(
    onDismiss: () -> Unit,
    onOpenStrength: (StrengthWorkoutRoute) -> Unit,
    onSaved: (creditedKcal: Int) -> Unit = {},
    onDeleted: (message: String, undo: () -> Unit) -> Unit = { _, _ -> },
    dateEpochDay: Long = 0,
    editingId: Long = 0,
    viewModel: LogExerciseViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    val resources = LocalResources.current
    LaunchedEffect(editingId) {
        if (editingId > 0) viewModel.handleEvent(LogExerciseEvent.OnLoadEditing(editingId))
    }
    // The row has to be *this* row: the ViewModel outlives the sheet, so a previous edit's entry
    // is still on the state when the FAB opens a blank one. Matching the id is both the hold-back
    // (nothing composes until the read lands, or the saveable form would re-key under the user)
    // and the staleness guard, which is why there is no second `editingLoaded` flag.
    val editing = uiState.editing?.takeIf { it.id == editingId }
    if (editingId > 0 && editing == null) return
    val state = rememberLogExerciseState(editing?.toLogExerciseForm() ?: LogExerciseForm())
    val describe = rememberDescribeState()
    // The form behind "Enter manually" — saveable for the sentence's reason. A parse opens it too,
    // because the form is where a filled-in activity is reviewed before it saves.
    var manual by rememberSaveable { mutableStateOf(false) }

    // Every way out to the strength screen cancels first: this ViewModel outlives the sheet, so a
    // reply landing after it closed would seed the next blank one the FAB opens.
    val openStrength: (StrengthWorkoutRoute) -> Unit = { route ->
        viewModel.handleEvent(LogExerciseEvent.OnCancelParse)
        onOpenStrength(route)
    }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            // Reported before the dismiss, not after: the host shows the confirmation on the
            // screen this sheet is closing onto, and the two have to be one frame's work.
            is LogExerciseSideEffect.Saved -> {
                onSaved(effect.creditedKcal)
                onDismiss()
            }

            is LogExerciseSideEffect.Parsed -> when (val result = effect.result) {
                is ExerciseParseResult.Success -> if (result.activity.sets.isNotEmpty()) {
                    // A sentence that named lifts is a session, and a session needs the set list
                    // this sheet has no room for — so it goes on, parse and all, rather than
                    // asking for the lifts a second time there.
                    // Merged onto the form rather than replacing it, the cardio branch's
                    // `withParsed`: a note or a burn already typed travels on with the sets.
                    val draft = state.form.withParsed(result.activity).copy(sets = result.activity.sets)
                    openStrength(StrengthWorkoutRoute(dateEpochDay, draft = draft))
                } else {
                    // `withEstimate` is the caller's, and it is what prices the parse: on a new
                    // form `burnedEdited` is false, so the burn falls out of the parsed type and
                    // duration at the user's own weight. Nothing the model said touches the kcal
                    // field.
                    state.form = state.form.withParsed(result.activity)
                    describe.clear()
                    manual = true
                }

                // Both leave the sentence in the field: it is the user's, and correcting it is
                // cheaper than typing it again. `CoachFailure`'s reading of a question that
                // failed to send. The form opens under it, since it is the other way through.
                ExerciseParseResult.NoActivityFound -> {
                    describe.message = R.string.training_exercise_describe_none
                    manual = true
                }

                ExerciseParseResult.Failed -> {
                    describe.message = R.string.training_exercise_describe_failed
                    manual = true
                }
            }

            // The strength screen's parse; this sheet has no set list to put one in. Named rather
            // than swept into an `else`, the rule the strength screen follows for `Parsed`.
            is LogExerciseSideEffect.SetsParsed -> Unit

            // Built here, where the resources are: the host only shows it. The undo outlives the
            // sheet, which is fine — the ViewModel does too.
            is LogExerciseSideEffect.Deleted -> {
                val message = resources.getString(
                    R.string.training_exercise_deleted,
                    resources.getString(effect.entry.type.label),
                )
                onDeleted(message) { viewModel.handleEvent(LogExerciseEvent.OnRestore(effect.entry)) }
                onDismiss()
            }
        }
    }

    LogExerciseContent(
        uiState = uiState,
        state = state,
        describe = describe,
        manual = manual,
        onManual = { manual = true },
        dateEpochDay = dateEpochDay,
        editing = editing,
        // A parse in flight outlives this sheet otherwise — the ViewModel does — and the spinner
        // would still be up the next time the FAB opened a blank one.
        onDismiss = {
            viewModel.handleEvent(LogExerciseEvent.OnCancelParse)
            onDismiss()
        },
        onOpenStrength = openStrength,
        onEstimate = {
            describe.message = null
            if (viewModel.isOnline()) {
                viewModel.handleEvent(LogExerciseEvent.OnParse(describe.text))
            } else {
                // The form is the manual path, so the whole of the graceful degrade is saying so,
                // opening it and spending nothing.
                describe.message = R.string.training_exercise_describe_offline
                manual = true
            }
        },
        onEvent = viewModel::handleEvent,
    )
}

/** Sentence and message — screen state, not the container's, the rule [LogExerciseState] already
 * documents. Saveable so a rotation mid-sentence keeps it. Internal because the strength screen's
 * describe field holds one too. */
@Composable
internal fun rememberDescribeState(): DescribeState = rememberSaveable(saver = DescribeState.Saver) {
    DescribeState()
}

internal class DescribeState(
    text: String = "",
    message: Int? = null,
) {
    var text: String by mutableStateOf(text)

    /** The line under the field, as a resource id: it is decided next to a ViewModel call and
     * resolved by the composable, which is the app's rule for a message crossing that boundary. */
    var message: Int? by mutableStateOf(message)

    /** After a parse landed: the field stays, empty, so a second sentence is a fresh one. */
    fun clear() {
        text = ""
        message = null
    }

    companion object {
        val Saver: Saver<DescribeState, Any> = listSaver(
            save = { listOf(it.text, it.message ?: 0) },
            restore = { saved ->
                DescribeState(
                    text = saved[0] as String,
                    message = (saved[1] as Int).takeIf { it != 0 },
                )
            },
        )
    }
}

@Composable
private fun LogExerciseContent(
    uiState: LogExerciseUiState,
    state: LogExerciseState,
    dateEpochDay: Long,
    editing: ExerciseEntry?,
    onDismiss: () -> Unit,
    onOpenStrength: (StrengthWorkoutRoute) -> Unit,
    onEvent: (LogExerciseEvent) -> Unit,
    describe: DescribeState = DescribeState(),
    manual: Boolean = false,
    onManual: () -> Unit = {},
    onEstimate: () -> Unit = {},
) {
    // Seeded from the form's own fields, so the estimate is right on the first frame too — the
    // form is the single source, and `withEstimate` is a no-op once the user takes the field over.
    val form = state.form.withEstimate(uiState.weightKg)
    val showForm = editing != null || manual

    AppBottomSheet(
        title = stringResource(if (editing == null) R.string.training_exercise_log else R.string.training_exercise_edit),
        onDismiss = onDismiss,
        // Grown to the form's full height, or the sheet opens half-way and cuts off the pinned
        // bar below — Save behind the gesture bar and Delete off the screen.
        expanded = showForm,
        // Pinned once the form is open, the add-entry sheet's reason: a sheet whose action is the
        // last thing in a scroll makes saving cost a scroll past everything already decided — and
        // a workout's form was tall enough to push Save off the bottom.
        bottomBar = if (!showForm) {
            null
        } else {
            {
                SheetActionBar {
                    PrimaryButton(
                        label = stringResource(R.string.training_save),
                        onClick = { onEvent(LogExerciseEvent.OnSave(form, dateEpochDay, editing?.id)) },
                        enabled = form.isValid(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // The food edit sheet's Delete, for its reason: a swipe on the diary row was
                    // the only way to remove one, and a gesture nobody is shown is not a feature.
                    if (editing != null) {
                        TextButton(
                            label = stringResource(R.string.training_exercise_delete),
                            onClick = { onEvent(LogExerciseEvent.OnDelete(editing)) },
                            color = MaterialTheme.colorScheme.error,
                            icon = AppIcons.Delete,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
    ) {
        // The bar takes the sheet's bottom gutter with it, so the form keeps a gap of its own above
        // the bar's rule rather than running flush into it.
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = if (showForm) 12.dp else 0.dp),
        ) {
            // A parse in flight is the one sub-level here: back abandons it and leaves the
            // sentence, and the next back dismisses the sheet. Registered **inside** the sheet's
            // own window, exactly as `SheetDatePicker`'s calendar is, or `ModalBottomSheet` takes
            // the gesture first and closes the whole thing. The manual form is not a level — it
            // opens *below* a field that stays on screen, so there is nothing for back to return to.
            if (uiState.parsing) {
                val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
                NavigationBackHandler(
                    state = navigationState,
                    onBackCompleted = { onEvent(LogExerciseEvent.OnCancelParse) },
                )
            }
            // Absent when correcting a logged activity, and so are the routines: every figure on
            // that form is already the user's own, and a parse that rewrote its type and duration
            // is noise on the one path where there is nothing left to guess.
            if (editing == null) {
                DescribeExerciseField(
                    text = describe.text,
                    parsing = uiState.parsing,
                    onTextChange = {
                        describe.text = it
                        describe.message = null
                    },
                    onEstimate = onEstimate,
                    onCancel = { onEvent(LogExerciseEvent.OnCancelParse) },
                    message = describe.message?.let { stringResource(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
                // Starting a routine needs a day and a workout to put it in, and this sheet has
                // the day — which is why it is here and not in Profile's routine list. Gone once
                // the form opens: the user has chosen to type it in, and two unlabelled chip rows
                // stacked — routines over activity types — read as one choice.
                if (!manual && uiState.routines.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.training_strength_start_routine),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        NameChipRow(
                            names = uiState.routines.map { it.name },
                            // Two routines sharing a name start the newer, the strength screen's
                            // own rule for the same row.
                            onSelect = { name ->
                                uiState.routines.firstOrNull { it.name == name }?.let { routine ->
                                    onOpenStrength(StrengthWorkoutRoute(dateEpochDay, routineId = routine.id))
                                }
                            },
                        )
                    }
                }
                if (!manual) {
                    TextButton(label = stringResource(R.string.training_exercise_manual), onClick = onManual)
                }
            }
            if (showForm) {
                ExerciseFormFields(
                    form = form,
                    weightKg = uiState.weightKg,
                    onFormChange = { state.form = it },
                    // A logged workout with sets stays Strength: a chip that switched it would
                    // leave its set list attached to a swim — the strength screen's own argument.
                    showTypeChips = form.sets.isEmpty(),
                )
                // Sets need a list and an editor, which don't fit above a keyboard — the argument
                // the recipe builder already made. So the sheet hands off rather than growing a
                // sub-view, and hands the form over with it: whatever was typed here is still
                // there on the other side.
                if (form.type == ExerciseType.Strength) {
                    ExerciseSetsRow(
                        summary = form.sets.summaryLabel(uiState.preferredUnit)?.resolve(LocalResources.current),
                        onClick = { onOpenStrength(StrengthWorkoutRoute(dateEpochDay, editing?.id ?: 0, draft = form)) },
                    )
                }
            }
        }
    }
}

private val PREVIEW_RUN = ExerciseEntry(id = 1, type = ExerciseType.Run, name = "Riverside loop", minutes = 30, burnedKcal = 363)

private val PREVIEW_WORKOUT = ExerciseEntry(
    id = 3,
    type = ExerciseType.Strength,
    name = "Push day",
    minutes = 45,
    burnedKcal = 260,
    sets = listOf(
        StrengthSet("Bench press", 8, 60.0),
        StrengthSet("Bench press", 8, 62.5),
        StrengthSet("Dip", 10, 0.0),
    ),
)

private val PreviewRoutines = listOf(
    Routine(id = 1, name = "Push day", lifts = emptyList()),
    Routine(id = 2, name = "Leg day", lifts = emptyList()),
)

/** A new entry: the sentence first, the routines under it, and the form one tap away. */
@PreviewLightDark
@Composable
private fun LogExerciseSheetPreview() {
    AppTheme {
        LogExerciseContent(
            uiState = LogExerciseUiState(weightKg = 74.0, routines = PreviewRoutines),
            state = LogExerciseState(form = LogExerciseForm()),
            describe = DescribeState(text = "45 minute run along the river"),
            dateEpochDay = 0,
            editing = null,
            onDismiss = {},
            onOpenStrength = {},
            onEvent = {},
        )
    }
}

/** "Enter manually" taken, or a parse landed: the form opens under the field it was filled from. */
@PreviewLightDark
@Composable
private fun LogExerciseSheetManualPreview() {
    AppTheme {
        LogExerciseContent(
            uiState = LogExerciseUiState(weightKg = 74.0),
            state = LogExerciseState(form = LogExerciseForm(type = ExerciseType.Run, minutes = 30)),
            manual = true,
            dateEpochDay = 0,
            editing = null,
            onDismiss = {},
            onOpenStrength = {},
            onEvent = {},
        )
    }
}

/** Strength picked: the one type that offers a door out to a screen that can hold a set list. */
@PreviewLightDark
@Composable
private fun LogExerciseSheetStrengthPreview() {
    AppTheme {
        LogExerciseContent(
            uiState = LogExerciseUiState(weightKg = 74.0),
            state = LogExerciseState(form = LogExerciseForm(type = ExerciseType.Strength, minutes = 45)),
            manual = true,
            dateEpochDay = 0,
            editing = null,
            onDismiss = {},
            onOpenStrength = {},
            onEvent = {},
        )
    }
}

/** Correcting a logged activity: the title says so, and the burn is the figure that was logged
 * rather than a fresh estimate — [LogExerciseForm.burnedEdited] is what holds it there. */
@PreviewLightDark
@Composable
private fun LogExerciseSheetEditingPreview() {
    AppTheme {
        LogExerciseContent(
            uiState = LogExerciseUiState(weightKg = 74.0, routines = PreviewRoutines),
            state = LogExerciseState(form = PREVIEW_RUN.toLogExerciseForm()),
            dateEpochDay = 0,
            editing = PREVIEW_RUN,
            onDismiss = {},
            onOpenStrength = {},
            onEvent = {},
        )
    }
}

/** Correcting a logged workout that has sets: no type chips, its Sets row, and Delete under the
 * pinned Save. */
@PreviewLightDark
@Composable
private fun LogExerciseSheetEditingSetsPreview() {
    AppTheme {
        LogExerciseContent(
            uiState = LogExerciseUiState(weightKg = 74.0),
            state = LogExerciseState(form = PREVIEW_WORKOUT.toLogExerciseForm()),
            dateEpochDay = 0,
            editing = PREVIEW_WORKOUT,
            onDismiss = {},
            onOpenStrength = {},
            onEvent = {},
        )
    }
}
