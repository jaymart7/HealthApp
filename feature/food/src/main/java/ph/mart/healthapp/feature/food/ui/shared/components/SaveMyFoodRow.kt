package ph.mart.healthapp.feature.food.ui.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R

/**
 * "Save as my food", as a switch and not a button, pinned above the action bar.
 *
 * **Mounted from the moment the form opens and dimmed until there is something worth keeping**,
 * rather than appearing when the form turns valid — which is what used to shove the action row down
 * the screen under the user's thumb.
 *
 * It became a switch because it is now beside the commit rather than above it: a switch states an
 * intention and the button acts on it, where the old button kept the food on the spot and left the
 * sheet open. Keeping and logging are still two things — the switch is what lets you say you want
 * both without pressing twice.
 *
 * In `ui/shared/` because two flows draw it: the add-entry sheet, where it was written, and the
 * label scan, whose whole answer to the barcode cache is that a panel read once becomes a food
 * the user owns.
 */
@Composable
internal fun SaveMyFoodRow(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.food_save_as_my_food),
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = stringResource(R.string.food_save_as_my_food_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@PreviewLightDark
@Composable
private fun SaveMyFoodRowPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column {
                SaveMyFoodRow(checked = true, enabled = true, onCheckedChange = {})
                SaveMyFoodRow(checked = false, enabled = false, onCheckedChange = {})
            }
        }
    }
}
