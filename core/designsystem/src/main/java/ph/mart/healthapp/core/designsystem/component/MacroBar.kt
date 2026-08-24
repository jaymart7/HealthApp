package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * Hard-edged 3-segment macro split bar — protein = `primary`, carbs = `tertiary`, fat =
 * `secondary` (fixed mapping, identical in every macro visualization in the app). Segment widths
 * are proportional to each macro's calorie contribution (protein/carbs at 4 kcal/g, fat at
 * 9 kcal/g), not raw grams.
 */
@Composable
fun MacroBar(proteinG: Int, carbsG: Int, fatG: Int, modifier: Modifier = Modifier) {
    val proteinKcal = (proteinG * 4).coerceAtLeast(0).toFloat()
    val carbsKcal = (carbsG * 4).coerceAtLeast(0).toFloat()
    val fatKcal = (fatG * 9).coerceAtLeast(0).toFloat()
    val total = (proteinKcal + carbsKcal + fatKcal).coerceAtLeast(1f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
    ) {
        MacroSegment(weight = proteinKcal / total, color = MaterialTheme.colorScheme.primary)
        MacroSegment(weight = carbsKcal / total, color = MaterialTheme.colorScheme.tertiary)
        MacroSegment(weight = fatKcal / total, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun RowScope.MacroSegment(weight: Float, color: Color) {
    if (weight <= 0f) return
    Surface(color = color, modifier = Modifier.weight(weight).fillMaxHeight()) {}
}

@PreviewLightDark
@Composable
private fun MacroBarPreview() {
    AppTheme {
        Surface {
            MacroBar(proteinG = 120, carbsG = 180, fatG = 60, modifier = Modifier.padding(16.dp))
        }
    }
}
