package ph.mart.healthapp.feature.progress.ui.nutrition.components

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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.NutritionAverages
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.MacroBar
import ph.mart.healthapp.core.designsystem.component.MicronutrientLegend
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.R

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
            text = stringResource(R.string.progress_nutrition_daily_average),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (targets != null) {
                stringResource(R.string.progress_nutrition_of_target, averages.calories, targets.calories)
            } else {
                stringResource(R.string.progress_nutrition_plain, averages.calories)
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
            MacroLegend(stringResource(R.string.progress_macro_protein), averages.proteinG, targets?.proteinG, MaterialTheme.colorScheme.primary)
            MacroLegend(stringResource(R.string.progress_macro_carbs), averages.carbsG, targets?.carbsG, MaterialTheme.colorScheme.tertiary)
            MacroLegend(stringResource(R.string.progress_macro_fat), averages.fatG, targets?.fatG, MaterialTheme.colorScheme.secondary)
        }
        MicronutrientLegend(
            fiberG = averages.fiberG,
            sugarG = averages.sugarG,
            sodiumMg = averages.sodiumMg,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = pluralStringResource(
                R.plurals.progress_nutrition_averaged,
                averages.daysLogged,
                averages.daysLogged,
            ),
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
            text = if (goalG != null) {
                stringResource(R.string.progress_macro_of_goal, label, averageG, goalG)
            } else {
                stringResource(R.string.progress_macro_plain, label, averageG)
            },
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
                    fiberG = 24,
                    sugarG = 63,
                    sodiumMg = 2180,
                    daysLogged = 24,
                ),
                targets = DailyTargets(calories = 1941, proteinG = 146, carbsG = 194, fatG = 65, floor = 1500),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
