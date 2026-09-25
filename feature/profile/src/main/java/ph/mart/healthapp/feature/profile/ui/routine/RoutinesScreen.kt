package ph.mart.healthapp.feature.profile.ui.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.exercise.Routine
import ph.mart.healthapp.core.data.exercise.RoutineLift
import ph.mart.healthapp.core.designsystem.component.DockedFab
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.rememberFabExpanded
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.routine.components.RoutinePlanZone
import ph.mart.healthapp.feature.profile.ui.shared.components.DeleteConfirmDialog
import ph.mart.healthapp.feature.profile.ui.shared.components.FigureRow
import ph.mart.healthapp.feature.profile.ui.shared.components.RenameSheet
import ph.mart.healthapp.feature.profile.ui.shared.components.RowMarker
import ph.mart.healthapp.feature.profile.ui.shared.components.SavedThingRow

/**
 * Every saved workout routine, one Nav3 level above Profile — the food library's twin, and the
 * only place a routine can be renamed, removed, or put in the week.
 *
 * Rename and delete only. Starting a routine needs a workout in progress and a day to log it on,
 * and Profile has neither — the same division the food library draws against the add-entry sheet.
 * "New routine" is a door, not a builder: it opens a blank strength screen, where "Save as routine"
 * is still the one place a routine is authored.
 *
 * A screen FAB where Supplements docks a bar, by choice — at ≥840dp it sits beside the rail's own.
 * `DECISIONS.md` → **Training, strength & routines** has the call.
 *
 * The plan is the exception, and the reason the row here has a footer the other two don't: the
 * week a routine is trained on is rendered on Home and authored nowhere else, so it belongs *in*
 * the routine's card rather than hanging off the bottom of it.
 */
@Composable
fun RoutinesScreen(
    onNewRoutine: () -> Unit,
    viewModel: RoutinesViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    RoutinesContent(uiState = uiState, onEvent = viewModel::handleEvent, onNewRoutine = onNewRoutine)
}

@Composable
private fun RoutinesContent(
    uiState: RoutinesUiState,
    onEvent: (RoutinesEvent) -> Unit,
    onNewRoutine: () -> Unit,
) {
    // Local rather than saveable, for the reason the food library gives: a dialog that survived
    // process death would reopen asking about a row the user has stopped looking at.
    var pendingDelete by remember { mutableStateOf<Routine?>(null) }
    var renaming by remember { mutableStateOf<Routine?>(null) }
    val scrollState = rememberScrollState()

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!uiState.loaded) {
                FullScreenState(
                    icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
                    heading = stringResource(R.string.profile_routines_empty_heading),
                    body = stringResource(R.string.profile_routines_empty_body),
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .padding(bottom = DockedFabContentPadding),
                ) {
                    uiState.routines.forEach { routine ->
                        SavedThingRow(
                            name = routine.name,
                            marker = { RowMarker(icon = AppIcons.Dumbbell, contentDescription = null) },
                            figures = { FigureRow(*routine.figures().toTypedArray()) },
                            detail = routine.lifts.contents().ifEmpty { null },
                            onClick = { renaming = routine },
                            footer = {
                                RoutinePlanZone(
                                    days = routine.days,
                                    onDaysChange = { days -> onEvent(RoutinesEvent.OnSetDays(routine.id, days)) },
                                )
                            },
                        )
                    }
                }
            }
            DockedFab(
                onClick = onNewRoutine,
                label = stringResource(R.string.profile_routines_new),
                expanded = rememberFabExpanded(scrollState),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            )
        }
    }

    // A routine is something the user built, so its delete asks first rather than deleting with
    // an undo the way a swiped diary row does.
    pendingDelete?.let { routine ->
        DeleteConfirmDialog(
            name = routine.name,
            body = stringResource(R.string.profile_routine_delete_body),
            onDelete = {
                onEvent(RoutinesEvent.OnDelete(routine.id))
                pendingDelete = null
            },
            onKeep = { pendingDelete = null },
        )
    }

    renaming?.let { routine ->
        RenameSheet(
            currentName = routine.name,
            onDismiss = { renaming = null },
            onRename = { name ->
                onEvent(RoutinesEvent.OnRename(routine.id, name))
                renaming = null
            },
            onDelete = {
                pendingDelete = routine
                renaming = null
            },
        )
    }
}

@PreviewLightDark
@Composable
private fun RoutinesPreview() {
    AppTheme {
        RoutinesContent(
            uiState = RoutinesUiState(
                routines = listOf(
                    Routine(
                        id = 1,
                        name = "Push day",
                        lifts = listOf(
                            RoutineLift("Bench press", sets = 3, reps = 8),
                            RoutineLift("Overhead press", sets = 3, reps = 8),
                            RoutineLift("Dip", sets = 2, reps = 10),
                        ),
                        days = 0b0010101,
                    ),
                    Routine(id = 2, name = "Legs", lifts = listOf(RoutineLift("Squat", sets = 5, reps = 5))),
                ),
            ),
            onEvent = {},
            onNewRoutine = {},
        )
    }
}

/** Nothing saved: the row in Profile still opens, so this state has to say what to do next. */
@PreviewLightDark
@Composable
private fun RoutinesEmptyPreview() {
    AppTheme { RoutinesContent(uiState = RoutinesUiState(), onEvent = {}, onNewRoutine = {}) }
}
