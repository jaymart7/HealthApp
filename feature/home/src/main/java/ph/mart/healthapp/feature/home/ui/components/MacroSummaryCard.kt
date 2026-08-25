package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.DiaryTotals
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.designsystem.component.MacroBar
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/**
 * The bar shows the **goal** macro split (same [MacroBar] usage as Food's diary summary and
 * Profile's Goals card); the legend carries today's consumed-against-goal grams. Colours are the
 * app-wide fixed mapping baked into [MacroBar] — protein/carbs/fat = primary/tertiary/secondary.
 */
@Composable
fun MacroSummaryCard(consumed: DiaryTotals, targets: DailyTargets, modifier: Modifier = Modifier) {
    HomeCard(modifier = modifier) {
        Text(
            text = "Macros",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MacroBar(
            proteinG = targets.proteinG,
            carbsG = targets.carbsG,
            fatG = targets.fatG,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MacroLegend("Protein", consumed.proteinG, targets.proteinG, MaterialTheme.colorScheme.primary)
            MacroLegend("Carbs", consumed.carbsG, targets.carbsG, MaterialTheme.colorScheme.tertiary)
            MacroLegend("Fat", consumed.fatG, targets.fatG, MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun MacroLegend(label: String, consumedG: Int, goalG: Int, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        Text(
            text = "$label $consumedG/${goalG}g",
            style = MaterialTheme.typography.labelMedium.tabularNums,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@PreviewLightDark
@Composable
private fun MacroSummaryCardPreview() {
    AppTheme {
        Surface {
            MacroSummaryCard(
                consumed = DiaryTotals(calories = 940, proteinG = 62, carbsG = 88, fatG = 31),
                targets = DailyTargets(calories = 1941, proteinG = 146, carbsG = 194, fatG = 65, floor = 1500),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
