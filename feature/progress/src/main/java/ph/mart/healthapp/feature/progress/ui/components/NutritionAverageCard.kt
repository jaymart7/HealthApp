package ph.mart.healthapp.feature.progress.ui.components

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
import ph.mart.healthapp.core.data.food.NutritionAverages
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.MacroBar
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/**
 * Averages across the selected range, against the profile's targets. The bar shows the **goal**
 * split (same [MacroBar] usage as Home's macro card and the diary summary); the legend carries the
 * averaged grams. `daysLogged` is spelled out because an average over four logged days in a month
 * is a different claim from one over thirty.
 */
@Composable
fun NutritionAverageCard(
    averages: NutritionAverages,
    targets: DailyTargets?,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Text(
            text = "Daily average",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (targets != null) {
                "${averages.calories} / ${targets.calories} kcal"
            } else {
                "${averages.calories} kcal"
            },
            style = MaterialTheme.typography.headlineSmall.tabularNums,
            color = MaterialTheme.colorScheme.onSurface,
        )
        MacroBar(
            proteinG = targets?.proteinG ?: averages.proteinG,
            carbsG = targets?.carbsG ?: averages.carbsG,
            fatG = targets?.fatG ?: averages.fatG,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MacroLegend("Protein", averages.proteinG, targets?.proteinG, MaterialTheme.colorScheme.primary)
            MacroLegend("Carbs", averages.carbsG, targets?.carbsG, MaterialTheme.colorScheme.tertiary)
            MacroLegend("Fat", averages.fatG, targets?.fatG, MaterialTheme.colorScheme.secondary)
        }
        Text(
            text = "Averaged over ${averages.daysLogged} logged " +
                if (averages.daysLogged == 1) "day" else "days",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun MacroLegend(label: String, averageG: Int, goalG: Int?, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        Text(
            text = if (goalG != null) "$label $averageG/${goalG}g" else "$label ${averageG}g",
            style = MaterialTheme.typography.labelMedium.tabularNums,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@PreviewLightDark
@Composable
private fun NutritionAverageCardPreview() {
    AppTheme {
        Surface {
            NutritionAverageCard(
                averages = NutritionAverages(
                    calories = 1978,
                    proteinG = 131,
                    carbsG = 186,
                    fatG = 71,
                    daysLogged = 24,
                ),
                targets = DailyTargets(calories = 1941, proteinG = 146, carbsG = 194, fatG = 65, floor = 1500),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
