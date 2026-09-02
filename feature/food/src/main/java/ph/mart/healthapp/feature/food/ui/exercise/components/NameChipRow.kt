package ph.mart.healthapp.feature.food.ui.exercise.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * [ExerciseTypeChipRow]'s pill, over free-text names rather than a fixed enum — they size to their
 * labels and scroll, because a lift name (or a routine's) is as long as it is.
 *
 * Two callers: the set editor's recent-lift chips and the strength screen's routine chips. Matching
 * is case-insensitive, the way every free-text lift name in this app is matched.
 *
 * [selected] is optional: the routine row has no selection to show, since tapping a chip there
 * seeds a form rather than setting a value.
 */
@Composable
internal fun NameChipRow(
    names: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    selected: String = "",
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) {
        names.forEach { name ->
            val isSelected = selected.isNotBlank() && name.equals(selected, ignoreCase = true)
            Surface(
                onClick = { onSelect(name) },
                shape = RoundedCornerShape(999.dp),
                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.height(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(text = name, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun NameChipRowPreview() {
    AppTheme {
        Surface {
            NameChipRow(
                names = listOf("Bench press", "Squat", "Deadlift"),
                onSelect = {},
                selected = "Squat",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** The routine row's shape: nothing is selected, because a tap seeds a workout rather than
 * choosing a value. */
@PreviewLightDark
@Composable
private fun NameChipRowUnselectedPreview() {
    AppTheme {
        Surface {
            NameChipRow(names = listOf("Push day", "Pull day", "Legs"), onSelect = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
