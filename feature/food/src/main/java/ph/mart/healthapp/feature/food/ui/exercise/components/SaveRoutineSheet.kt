package ph.mart.healthapp.feature.food.ui.exercise.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R

/**
 * Names the workout on screen as a routine — [SaveMealSheet]'s twin, one domain over: a routine is
 * authored by naming a session that already exists, not built from nothing in an editor.
 *
 * [liftCount] and [setCount] are there so the user can see what they are about to keep. Saving the
 * routine does not log the workout: the two buttons at the bottom of the screen still do that.
 */
@Composable
internal fun SaveRoutineSheet(
    name: String,
    liftCount: Int,
    setCount: Int,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.food_routine_sheet_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = stringResource(
                R.string.food_routine_sheet_body,
                pluralStringResource(R.plurals.food_routine_lifts, liftCount, liftCount),
                pluralStringResource(R.plurals.food_strength_sets, setCount, setCount),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTextField(value = name, onValueChange = onNameChange, placeholder = stringResource(R.string.food_routine_placeholder))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryButton(label = stringResource(R.string.food_cancel), onClick = onDismiss, modifier = Modifier.weight(1f))
                PrimaryButton(label = stringResource(R.string.food_save), onClick = onSave, enabled = name.isNotBlank(), modifier = Modifier.weight(1f))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SaveRoutineSheetPreview() {
    AppTheme {
        SaveRoutineSheet(
            name = "Push day",
            liftCount = 3,
            setCount = 9,
            onNameChange = {},
            onDismiss = {},
            onSave = {},
        )
    }
}
