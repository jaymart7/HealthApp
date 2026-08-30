package ph.mart.healthapp.feature.profile.ui.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** Label (+ optional sublabel) on the left, an optional control on the right. Shared by the Units,
 * Reminders, Data and About sections — the one row shape the whole screen is built from. */
@Composable
internal fun SettingsRow(
    label: String,
    modifier: Modifier = Modifier,
    sublabel: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (sublabel != null) {
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Row(modifier = Modifier.padding(start = 12.dp)) { trailing() }
        }
    }
}

@PreviewLightDark
@Composable
private fun SettingsRowPreview() {
    AppTheme {
        Surface {
            AppCard(modifier = Modifier.padding(16.dp)) {
                SettingsRow(
                    label = "Weigh-in day",
                    sublabel = "Every Monday, 8:00 AM",
                    trailing = { Switch(checked = true, onCheckedChange = {}) },
                )
            }
        }
    }
}
