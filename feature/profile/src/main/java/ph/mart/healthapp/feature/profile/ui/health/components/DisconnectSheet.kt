package ph.mart.healthapp.feature.profile.ui.health.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R

@Composable
internal fun DisconnectSheet(onDismiss: () -> Unit, onConfirm: (Boolean, Boolean) -> Unit) {
    var deleteImported by remember { mutableStateOf(true) }
    var deleteSent by remember { mutableStateOf(true) }

    AppBottomSheet(onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(R.string.profile_health_disconnect_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.profile_health_disconnect_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DeleteChoice(
                label = stringResource(R.string.profile_health_delete_imported),
                checked = deleteImported,
                onToggle = { deleteImported = it },
            )
            DeleteChoice(
                label = stringResource(R.string.profile_health_delete_sent),
                checked = deleteSent,
                onToggle = { deleteSent = it },
            )
            PrimaryButton(
                label = stringResource(R.string.profile_health_disconnect),
                onClick = { onConfirm(deleteImported, deleteSent) },
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(label = stringResource(R.string.profile_cancel), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DeleteChoice(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    AppCard(onClick = { onToggle(!checked) }) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = onToggle)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun DisconnectSheetPreview() {
    AppTheme { DisconnectSheet(onDismiss = {}, onConfirm = { _, _ -> }) }
}
