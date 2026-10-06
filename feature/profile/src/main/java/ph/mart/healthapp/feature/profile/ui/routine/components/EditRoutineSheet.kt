package ph.mart.healthapp.feature.profile.ui.routine.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
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
import ph.mart.healthapp.core.data.exercise.Routine
import ph.mart.healthapp.core.data.exercise.RoutineLift
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R

/**
 * One saved routine, whole and editable: the name, every lift with its sets and reps, and the
 * week it is planned for. Nothing is written until Save, which replaces the routine with what the
 * sheet holds.
 *
 * The draft lives here rather than in the screen: it is discarded on dismiss, and nothing on the
 * other side of Save needs to have seen it. The plan zone here edits the draft only; the card's
 * own zone still writes on every tap.
 *
 * **Delete sits left of Save, in the same row, smaller** — a `TextButton` in `error` with its
 * glyph, which wraps its label while Save takes the rest. Unlike `SheetDeleteAction` on
 * Supplements it is not under a rule, by the user's call. It does not delete anything itself: the
 * screen dismisses this sheet and raises `DeleteConfirmDialog`, the one place that asks.
 */
@Composable
internal fun EditRoutineSheet(
    routine: Routine,
    onSave: (Routine) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(routine.id) { mutableStateOf(routine) }
    AppBottomSheet(title = stringResource(R.string.profile_routine_edit), onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RoutineFields(draft = draft, onDraftChange = { draft = it })
            RoutinePlanZone(days = draft.days, onDaysChange = { draft = draft.copy(days = it) })
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(
                    label = stringResource(R.string.profile_delete),
                    onClick = onDelete,
                    icon = AppIcons.Delete,
                    color = MaterialTheme.colorScheme.error,
                )
                PrimaryButton(
                    label = stringResource(R.string.profile_routine_design_save),
                    onClick = { onSave(draft.trimmed()) },
                    enabled = draft.isSaveable(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun EditRoutineSheetPreview() {
    AppTheme {
        EditRoutineSheet(
            routine = Routine(
                id = 1,
                name = "Push day",
                lifts = listOf(
                    RoutineLift("Bench press", sets = 3, reps = 8),
                    RoutineLift("Overhead press", sets = 3, reps = 8),
                    RoutineLift("Dip", sets = 2, reps = 10),
                ),
                days = 0b0010101,
            ),
            onSave = {},
            onDelete = {},
            onDismiss = {},
        )
    }
}
