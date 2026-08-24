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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** The FAB's quick-action sheet: Log food / Log weight / Add photo, each routing to a real
 * (stub-for-now) destination. */
@Composable
fun QuickActionSheet(
    onDismiss: () -> Unit,
    onLogFood: () -> Unit,
    onLogWeight: () -> Unit,
    onAddPhoto: () -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss) {
        QuickActionRow(label = "Log food", onClick = onLogFood)
        QuickActionRow(label = "Log weight", onClick = onLogWeight)
        QuickActionRow(label = "Add photo", onClick = onAddPhoto)
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
        QuickActionSheet(onDismiss = {}, onLogFood = {}, onLogWeight = {}, onAddPhoto = {})
    }
}
