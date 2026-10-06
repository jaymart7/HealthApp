package ph.mart.healthapp.feature.training.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.training.R

/**
 * The log sheet's door to the strength screen, drawn once Strength is the type: what the sets are
 * ([summary], the diary row's own line) or that there are none yet, and a chevron saying the tap
 * goes somewhere. One row rather than a summary line over a button, so the sheet's form stays
 * short enough that its fields and its pinned Save share the screen.
 *
 * `surfaceContainerHigh` against the sheet's `surfaceContainerLow`, so it reads as a thing to tap
 * rather than as one more caption.
 */
@Composable
internal fun ExerciseSetsRow(
    summary: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Icon(imageVector = AppIcons.Dumbbell, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.training_exercise_sets),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = summary ?: stringResource(R.string.training_exercise_sets_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ExerciseSetsRowPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            ExerciseSetsRow(summary = "2 exercises · 3 sets · 1,020 kg", onClick = {}, modifier = Modifier.padding(16.dp))
        }
    }
}

/** A Strength activity with nothing lifted yet — the row invites the first set. */
@PreviewLightDark
@Composable
private fun ExerciseSetsRowEmptyPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            ExerciseSetsRow(summary = null, onClick = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
