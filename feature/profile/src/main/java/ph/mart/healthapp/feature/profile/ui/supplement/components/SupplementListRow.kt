package ph.mart.healthapp.feature.profile.ui.supplement.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * One supplement, as the list shows it: edit and delete, and **no way to tick it**. A tick belongs
 * to a day, and Profile has none — Home's card stays the place a dose is logged. Same division
 * `LibraryRow` draws between the library and the add-entry sheet.
 */
@Composable
internal fun SupplementListRow(
    name: String,
    summary: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = AppIcons.Edit,
                    contentDescription = "Edit $name",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = AppIcons.Delete,
                    contentDescription = "Delete $name",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SupplementListRowPreview() {
    AppTheme {
        Surface {
            SupplementListRow(
                name = "Vitamin D",
                summary = "2000 IU · once daily",
                onEdit = {},
                onDelete = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
