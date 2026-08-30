package ph.mart.healthapp.feature.food.ui.shared.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * The prototype's 4 individually-bordered pill buttons (single-select) — visually distinct from
 * [ph.mart.healthapp.core.designsystem.component.SegmentedToggle]'s single continuous track, so
 * built here rather than reused for a shape it wasn't designed for.
 *
 * Feature-local rather than in `:core:designsystem`: it takes a [MealType], which the design
 * system has no dependency on.
 */
@Composable
internal fun MealTypeChipRow(selected: MealType, onSelect: (MealType) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().selectableGroup(),
    ) {
        MealType.entries.forEach { mealType ->
            val isSelected = mealType == selected
            Surface(
                onClick = { onSelect(mealType) },
                shape = RoundedCornerShape(999.dp),
                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                // 2dp is the system's selected weight; the 1.5dp this used to draw was a third
                // border width in a system that has exactly two.
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
                // A quarter of the width holds "Breakfast" at the default font scale and not much
                // past it, so the chip grows a second line rather than clipping the word. 48dp is
                // the floor, not the height.
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .semantics {
                        role = Role.RadioButton
                        this.selected = isSelected
                    },
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                    Text(
                        text = mealType.name,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun MealTypeChipRowPreview() {
    AppTheme {
        Surface {
            Box(modifier = Modifier.padding(16.dp)) {
                MealTypeChipRow(selected = MealType.Lunch, onSelect = {})
            }
        }
    }
}
