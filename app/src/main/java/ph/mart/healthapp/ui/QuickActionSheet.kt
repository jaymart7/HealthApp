package ph.mart.healthapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.R
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** The FAB's quick-action sheet: Say what you ate / Log food / Log exercise / Log weight / Add
 * photo, each routing to a real destination.
 *
 * The sentence sits above the camera because it is the path that works with the plate already
 * cleared, and because it is the only one of the two that can log a whole meal at once. */
@Composable
fun QuickActionSheet(
    onDismiss: () -> Unit,
    onSpeakFood: () -> Unit,
    onLogFood: () -> Unit,
    onLogExercise: () -> Unit,
    onLogWeight: () -> Unit,
    onAddPhoto: () -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss) {
        QuickActionRow(label = stringResource(R.string.app_quick_speak_food), onClick = onSpeakFood)
        QuickActionRow(label = stringResource(R.string.app_quick_log_food), onClick = onLogFood)
        QuickActionRow(label = stringResource(R.string.app_quick_log_exercise), onClick = onLogExercise)
        QuickActionRow(label = stringResource(R.string.app_quick_log_weight), onClick = onLogWeight)
        QuickActionRow(label = stringResource(R.string.app_quick_add_photo), onClick = onAddPhoto)
    }
}

@Composable
private fun QuickActionRow(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@PreviewLightDark
@Composable
private fun QuickActionSheetPreview() {
    AppTheme {
        QuickActionSheet(
            onDismiss = {},
            onSpeakFood = {},
            onLogFood = {},
            onLogExercise = {},
            onLogWeight = {},
            onAddPhoto = {},
        )
    }
}
