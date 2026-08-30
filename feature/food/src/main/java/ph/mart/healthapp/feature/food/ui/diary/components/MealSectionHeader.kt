package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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

/** Screen-specific to the food diary — collapsible row: chevron, meal label, subtotal, an optional
 * "save as meal" bookmark, and "+". [onSave] is null for a section with nothing in it — there is
 * nothing to snapshot yet. */
@Composable
fun MealSectionHeader(
    label: String,
    subtotalKcal: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAdd: () -> Unit,
    onSave: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (expanded) AppIcons.ChevronDown else AppIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.height(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        Text(
            text = "$subtotalKcal kcal",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onSave != null) {
            Surface(
                onClick = onSave,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp).height(24.dp),
            ) {
                Icon(
                    imageVector = AppIcons.Bookmark,
                    contentDescription = "Save $label as a meal",
                    modifier = Modifier.padding(4.dp),
                )
            }
        }
        Surface(
            onClick = onAdd,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp).height(24.dp),
        ) {
            Icon(
                imageVector = AppIcons.Add,
                contentDescription = "Add to $label",
                modifier = Modifier.padding(4.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun MealSectionHeaderPreview() {
    AppTheme {
        Surface {
            MealSectionHeader(
                label = "Breakfast",
                subtotalKcal = 470,
                expanded = true,
                onToggle = {},
                onAdd = {},
                onSave = {},
            )
        }
    }
}
