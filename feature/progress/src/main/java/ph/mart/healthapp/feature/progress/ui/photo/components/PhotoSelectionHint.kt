package ph.mart.healthapp.feature.progress.ui.photo.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R

/**
 * What teaches the grid's one non-obvious gesture. Tapping two tiles opens a comparison, and
 * nothing about a grid of photos says so — the old screen marked a picked tile with a border and
 * left the reader to discover the rest by accident.
 *
 * Two states, because the third can't be seen: at two picks the comparison is already on screen,
 * so a bar describing what a second tap will do would only ever be read on its way out.
 *
 * - **none** — quiet `surfaceContainer`, the rule stated plainly.
 * - **one** — `primaryContainer`, the same numbered badge the tile now carries, and the way back
 *   out. Picking one photo and changing your mind otherwise means finding that tile again.
 */
@Composable
internal fun PhotoSelectionHint(
    selectedCount: Int,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selectedCount >= 2) return
    val picked = selectedCount == 1

    Surface(
        color = if (picked) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (picked) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (picked) {
                SelectionBadge(
                    number = 1,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    contentColor = MaterialTheme.colorScheme.primaryContainer,
                )
            } else {
                Icon(
                    imageVector = AppIcons.Compare,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = stringResource(
                    if (picked) R.string.progress_photos_hint_one else R.string.progress_photos_hint_none,
                ),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            if (picked) {
                TextButton(label = stringResource(R.string.progress_photos_clear), onClick = onClear)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PhotoSelectionHintPreview() {
    AppTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PhotoSelectionHint(selectedCount = 0, onClear = {})
                PhotoSelectionHint(selectedCount = 1, onClear = {})
            }
        }
    }
}
