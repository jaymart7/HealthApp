package ph.mart.healthapp.feature.profile.ui.routine

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.exercise.Routine
import ph.mart.healthapp.core.data.exercise.RoutineLift
import ph.mart.healthapp.core.designsystem.component.DiscardConfirmDialog
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.ui.shared.components.LibraryRow
import ph.mart.healthapp.feature.profile.ui.shared.components.RenameSheet

/**
 * Every saved workout routine, one Nav3 level above Profile — the food library's twin, and the
 * only place a routine can be renamed or removed.
 *
 * Rename and delete only. Starting a routine needs a workout in progress and a day to log it on,
 * and Profile has neither — the same division the food library draws against the add-entry sheet.
 */
@Composable
fun RoutinesScreen(
    viewModel: RoutinesViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    RoutinesContent(uiState = uiState, onEvent = viewModel::handleEvent)
}

@Composable
private fun RoutinesContent(
    uiState: RoutinesUiState,
    onEvent: (RoutinesEvent) -> Unit,
) {
    // Local rather than saveable, for the reason the food library gives: a dialog that survived
    // process death would reopen asking about a row the user has stopped looking at.
    var pendingDelete by remember { mutableStateOf<Routine?>(null) }
    var renaming by remember { mutableStateOf<Routine?>(null) }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        if (!uiState.loaded) {
            FullScreenState(
                icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
                heading = "No routines yet",
                body = "Log a strength workout, tap \"Save as routine\", and it shows up here.",
            )
            return@Surface
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            uiState.routines.forEach { routine ->
                LibraryRow(
                    name = routine.name,
                    summary = routine.summary(),
                    contents = routine.lifts.contents(),
                    onRename = { renaming = routine },
                    onDelete = { pendingDelete = routine },
                )
            }
        }
    }

    // A routine is something the user built, and its delete sits beside the rename — so it asks
    // first, rather than deleting with an undo the way a swiped diary row does.
    pendingDelete?.let { routine ->
        DiscardConfirmDialog(
            title = "Delete ${routine.name}?",
            body = "This routine is removed for good. Workouts you logged from it stay in your diary.",
            confirmLabel = "Delete",
            dismissLabel = "Keep",
            onConfirm = {
                onEvent(RoutinesEvent.OnDelete(routine.id))
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
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
                    ),
                    Routine(id = 2, name = "Legs", lifts = listOf(RoutineLift("Squat", sets = 5, reps = 5))),
                ),
            ),
            onEvent = {},
        )
    }
}

/** Nothing saved: the row in Profile still opens, so this state has to say what to do next. */
@PreviewLightDark
@Composable
private fun RoutinesEmptyPreview() {
    AppTheme { RoutinesContent(uiState = RoutinesUiState(), onEvent = {}) }
}
