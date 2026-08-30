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
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.ui.shared.components.MealTypeChipRow

/**
 * [MealTypeChipRow]'s pill in a scrolling row — eight activity types don't divide a phone width
 * into readable equal shares the way four meals do, so these size to their labels and scroll.
 */
@Composable
internal fun ExerciseTypeChipRow(selected: ExerciseType, onSelect: (ExerciseType) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) {
        ExerciseType.entries.forEach { type ->
            val isSelected = type == selected
            Surface(
                onClick = { onSelect(type) },
                shape = RoundedCornerShape(999.dp),
                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.height(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(text = type.label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ExerciseTypeChipRowPreview() {
    AppTheme {
        Surface {
            Box(modifier = Modifier.padding(16.dp)) {
                ExerciseTypeChipRow(selected = ExerciseType.Run, onSelect = {})
            }
        }
    }
}
