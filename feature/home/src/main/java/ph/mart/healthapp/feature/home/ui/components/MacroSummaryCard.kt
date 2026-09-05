package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.DiaryTotals
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.home.R

private val NameWidth = 52.dp
private val FigureWidth = 72.dp
private val TrackHeight = 8.dp

/**
 * Today's macros, one row per macro, each against **its own** target.
 *
 * This replaced the single stacked `MacroBar` the card used to draw. That bar shows the *goal*
 * split — three segments whose widths are the targets, not the intake — so it could say what the
 * day was supposed to look like and never how close any one macro was to its own number. Three
 * tracks say both, and the legend that used to carry the grams is gone because each row now sits
 * beside the bar it describes.
 *
 * `MacroBar` itself is untouched: the food diary's summary and Profile's Goals card both still
 * draw it, and the goal split is exactly what those two mean.
 *
 * Colours are the app-wide fixed mapping — protein/carbs/fat = primary/tertiary/secondary — and
 * there is no status mark on any row: a macro is not a verdict, and three dots on one card would
 * be the screen grading a meal.
 */
@Composable
fun MacroSummaryCard(consumed: DiaryTotals, targets: DailyTargets, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.home_macros_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroRow(
                    name = stringResource(R.string.home_macro_protein),
                    consumedG = consumed.proteinG,
                    goalG = targets.proteinG,
                    color = MaterialTheme.colorScheme.primary,
                )
                MacroRow(
                    name = stringResource(R.string.home_macro_carbs),
                    consumedG = consumed.carbsG,
                    goalG = targets.carbsG,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                MacroRow(
                    name = stringResource(R.string.home_macro_fat),
                    consumedG = consumed.fatG,
                    goalG = targets.fatG,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun MacroRow(name: String, consumedG: Int, goalG: Int, color: Color) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(NameWidth),
        )
        Canvas(modifier = Modifier.weight(1f).height(TrackHeight)) {
            val radius = CornerRadius(size.height / 2f)
            drawRoundRect(color = trackColor, size = size, cornerRadius = radius)
            val share = if (goalG > 0) (consumedG.toFloat() / goalG).coerceIn(0f, 1f) else 0f
            val width = size.width * share
            if (width > 0f) {
                drawRoundRect(
                    color = color,
                    size = Size(width.coerceAtLeast(size.height), size.height),
                    cornerRadius = radius,
                )
            }
        }
        Text(
            text = stringResource(R.string.home_macro_figure, consumedG, goalG),
            style = MaterialTheme.typography.labelMedium.tabularNums,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.width(FigureWidth),
        )
    }
}

@PreviewLightDark
@Composable
private fun MacroSummaryCardPreview() {
    AppTheme {
        Surface {
            MacroSummaryCard(
                consumed = DiaryTotals(calories = 1560, proteinG = 108, carbsG = 148, fatG = 58),
                targets = DailyTargets(calories = 2692, proteinG = 170, carbsG = 226, fatG = 75, floor = 1500),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
