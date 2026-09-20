package ph.mart.healthapp.feature.progress.ui.nutrition.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.NutrientReading
import ph.mart.healthapp.core.data.food.Nutrients
import ph.mart.healthapp.core.data.food.NutritionAverages
import ph.mart.healthapp.core.data.food.formatNutrient
import ph.mart.healthapp.core.data.food.isEmpty
import ph.mart.healthapp.core.data.food.plus
import ph.mart.healthapp.core.data.food.readings
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.MacroBar
import ph.mart.healthapp.core.designsystem.component.NutrientPanel
import ph.mart.healthapp.core.designsystem.component.NutrientRow
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
    nutrientTargets: Nutrients? = null,
    /** The average day's supplement figures over the same denominator [averages] uses. They join
     * the nutrient panel and nothing above it — a supplement has no calories. */
    supplementNutrients: Nutrients = Nutrients(),
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
        // No coverage line here, unlike the diary's: the averaged-over-N-days line right below
        // already says how thin the window is, and a second denominator on the same card would
        // only invite the two to be read against each other. The supplements note is the one
        // exception, and it is not a denominator — it says what the rows are a sum *of*, which
        // the line below cannot.
        NutrientPanel(
            rows = (averages.nutrients + supplementNutrients).readings(nutrientTargets).toRows(),
            coverage = stringResource(R.string.progress_nutrition_with_supplements)
                .takeIf { !supplementNutrients.isEmpty },
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

/**
 * Two lines, not one: three "Protein 131/146g" labels side by side are wider than a narrow phone,
 * and a `Row` squeezes rather than wraps, so each one broke into three or four lines of its own.
 * Stacking the figure under the name keeps the strip a single row at every phone width. The
 * diary's summary bar answers the same problem with a `FlowRow` — a bar can afford a second row
 * where a card's legend cannot.
 */
@Composable
private fun MacroLegend(label: String, averageG: Int, goalG: Int?, color: Color) {
    // Resolved above the semantics lambda, which cannot read a resource. The two lines are one
    // phrase to a screen reader, which is what the single Text used to give it for free.
    val spoken = if (goalG != null) {
        stringResource(R.string.progress_macro_of_goal, label, averageG, goalG)
    } else {
        stringResource(R.string.progress_macro_plain, label, averageG)
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = if (goalG != null) {
                stringResource(R.string.progress_macro_figure, averageG, goalG)
            } else {
                stringResource(R.string.progress_macro_figure_plain, averageG)
            },
            style = MaterialTheme.typography.labelMedium.tabularNums,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // Under the name, not under the dot: 8dp dot + the 4dp gap above.
            modifier = Modifier.padding(start = 12.dp),
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
                    nutrients = Nutrients(
                        fiberG = 24,
                        sugarG = 63,
                        sodiumMg = 2180,
                        vitaminDUg = 7,
                        calciumMg = 780,
                        ironUg = 11_400,
                        potassiumMg = 2410,
                    ),
                    daysLogged = 24,
                ),
                targets = DailyTargets(calories = 1941, proteinG = 146, carbsG = 194, fatG = 65, floor = 1500),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** Twin of the diary summary bar's mapper, and duplicated for the same reason `MacroLegend` is:
 * `:core:designsystem` is a leaf module that cannot see the `Nutrient` enum, and the two features
 * that draw this cannot import each other. */
@Composable
private fun List<NutrientReading>.toRows(): List<NutrientRow> = map { reading ->
    NutrientRow(
        label = stringResource(reading.nutrient.labelRes),
        value = formatNutrient(reading.nutrient, reading.value),
        target = reading.target?.let { formatNutrient(reading.nutrient, it) },
        fraction = reading.fraction,
        overLimit = reading.overLimit,
    )
}
