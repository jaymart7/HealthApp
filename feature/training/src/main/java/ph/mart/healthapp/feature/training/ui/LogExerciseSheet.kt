package ph.mart.healthapp.feature.training.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.training.R
import ph.mart.healthapp.feature.training.ui.components.DescribeExerciseField
import ph.mart.healthapp.feature.training.ui.components.ExerciseFormFields

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
 * [onOpenStrength] leaves for the strength workout screen, carrying the day. It is offered only
 * once Strength is picked, and it is a door rather than an automatic redirect on purpose: the
 * plain duration-and-kcal path is what an imported watch session is, and it stays reachable. */
@Composable
fun LogExerciseSheet(
    onDismiss: () -> Unit,
    onOpenStrength: (Long) -> Unit,
    onSaved: (creditedKcal: Int) -> Unit = {},
    dateEpochDay: Long = 0,
    editingId: Long = 0,
    viewModel: LogExerciseViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
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

    viewModel.collectSideEffect { effect ->
        when (effect) {
            // Reported before the dismiss, not after: the host shows the confirmation on the
            // screen this sheet is closing onto, and the two have to be one frame's work.
            is LogExerciseSideEffect.Saved -> {
                onSaved(effect.creditedKcal)
                onDismiss()
            }

            is LogExerciseSideEffect.Parsed -> when (val result = effect.result) {
                // `withEstimate` is the caller's, and it is what prices the parse: on a new form
                // `burnedEdited` is false, so the burn falls out of the parsed type and duration
                // at the user's own weight. Nothing the model said touches the kcal field.
                is ExerciseParseResult.Success -> {
                    state.form = state.form.withParsed(result.activity)
                    describe.close()
                }

                // Both leave the sentence in the field: it is the user's, and correcting it is
                // cheaper than typing it again. `CoachFailure`'s reading of a question that
                // failed to send.
                ExerciseParseResult.NoActivityFound ->
                    describe.message = R.string.training_exercise_describe_none

                ExerciseParseResult.Failed ->
                    describe.message = R.string.training_exercise_describe_failed
            }
        }
    }

    LogExerciseContent(
        uiState = uiState,
        state = state,
        describe = describe,
        dateEpochDay = dateEpochDay,
        editingId = editing?.id,
        // A parse in flight outlives this sheet otherwise — the ViewModel does — and the spinner
        // would still be up the next time the FAB opened a blank one.
        onDismiss = {
            viewModel.handleEvent(LogExerciseEvent.OnCancelParse)
            onDismiss()
        },
        onOpenStrength = { onOpenStrength(dateEpochDay) },
        onEstimate = {
            describe.message = null
            if (viewModel.isOnline()) {
                viewModel.handleEvent(LogExerciseEvent.OnParse(describe.text))
            } else {
                // The sheet below is the manual path, so the whole of the graceful degrade is
                // saying so and spending nothing.
                describe.message = R.string.training_exercise_describe_offline
            }
        },
        onEvent = viewModel::handleEvent,
    )
}

/** Panel open, sentence and message — screen state, not the container's, the rule
 * [LogExerciseState] already documents. Saveable so a rotation mid-sentence keeps it. */
@Composable
private fun rememberDescribeState(): DescribeState = rememberSaveable(saver = DescribeState.Saver) {
    DescribeState()
}

internal class DescribeState(
    open: Boolean = false,
    text: String = "",
    message: Int? = null,
) {
    var open: Boolean by mutableStateOf(open)
    var text: String by mutableStateOf(text)

    /** The line under the field, as a resource id: it is decided next to a ViewModel call and
     * resolved by the composable, which is the app's rule for a message crossing that boundary. */
    var message: Int? by mutableStateOf(message)

    fun close() {
        open = false
        text = ""
        message = null
    }

    companion object {
        val Saver: Saver<DescribeState, Any> = listSaver(
            save = { listOf(it.open, it.text, it.message ?: 0) },
            restore = { saved ->
                DescribeState(
                    open = saved[0] as Boolean,
                    text = saved[1] as String,
                    message = (saved[2] as Int).takeIf { it != 0 },
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
    editingId: Long?,
    onDismiss: () -> Unit,
    onOpenStrength: () -> Unit,
    onEvent: (LogExerciseEvent) -> Unit,
    describe: DescribeState = DescribeState(),
    onEstimate: () -> Unit = {},
) {
    // Seeded from the form's own fields, so the estimate is right on the first frame too — the
    // form is the single source, and `withEstimate` is a no-op once the user takes the field over.
    val form = state.form.withEstimate(uiState.weightKg)

    AppBottomSheet(
        title = stringResource(if (editingId == null) R.string.training_exercise_log else R.string.training_exercise_edit),
        onDismiss = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // The panel is a sub-level of this sheet, so back steps through it rather than past
            // it — the rule every swap-in view in this app follows. Registered **inside** the
            // sheet's own window, exactly as `SheetDatePicker`'s calendar is, or `ModalBottomSheet`
            // takes the gesture first and closes the whole thing. Mounted only while the panel is
            // open, so an untouched sheet dismisses on back as it always did.
            if (describe.open) {
                val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
                NavigationBackHandler(
                    state = navigationState,
                    onBackCompleted = {
                        // A parse in flight is the inner level: back abandons it and leaves the
                        // sentence, and a second back closes the panel.
                        if (uiState.parsing) {
                            onEvent(LogExerciseEvent.OnCancelParse)
                        } else {
                            describe.close()
                        }
                    },
                )
            }
            // Absent when correcting a logged activity: every figure on that form is already the
            // user's own, and a parse that rewrote its type and duration is noise on the one path
            // where there is nothing left to guess.
            if (editingId == null) {
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
                    onEstimate = onEstimate,
                    onCancel = { onEvent(LogExerciseEvent.OnCancelParse) },
                    message = describe.message?.let { stringResource(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            ExerciseFormFields(
                form = form,
                weightKg = uiState.weightKg,
                onFormChange = { state.form = it },
            )
            // Sets need a list and an editor, which don't fit above a keyboard — the argument the
            // recipe builder already made. So the sheet hands off rather than growing a sub-view.
            if (form.type == ExerciseType.Strength) {
                SecondaryButton(
                    label = stringResource(R.string.training_exercise_log_sets),
                    onClick = onOpenStrength,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            PrimaryButton(
                label = stringResource(R.string.training_save),
                onClick = { onEvent(LogExerciseEvent.OnSave(form, dateEpochDay, editingId)) },
                enabled = form.isValid(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun LogExerciseSheetPreview() {
    AppTheme {
        LogExerciseContent(
            uiState = LogExerciseUiState(weightKg = 74.0),
            state = LogExerciseState(form = LogExerciseForm(type = ExerciseType.Run, minutes = 30)),
            dateEpochDay = 0,
            editingId = null,
            onDismiss = {},
            onOpenStrength = {},
            onEvent = {},
        )
    }
}

/** The describe panel open over the form it fills in — the sub-level back steps through. */
@PreviewLightDark
@Composable
private fun LogExerciseSheetDescribePreview() {
    AppTheme {
        LogExerciseContent(
            uiState = LogExerciseUiState(weightKg = 74.0),
            state = LogExerciseState(form = LogExerciseForm(type = ExerciseType.Walk, minutes = 30)),
            describe = DescribeState(open = true, text = "45 minute run along the river"),
            dateEpochDay = 0,
            editingId = null,
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
            dateEpochDay = 0,
            editingId = null,
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
            uiState = LogExerciseUiState(weightKg = 74.0),
            state = LogExerciseState(
                form = ExerciseEntry(id = 1, type = ExerciseType.Run, name = "Riverside loop", minutes = 30, burnedKcal = 363)
                    .toLogExerciseForm(),
            ),
            dateEpochDay = 0,
            editingId = 1,
            onDismiss = {},
            onOpenStrength = {},
            onEvent = {},
        )
    }
}
