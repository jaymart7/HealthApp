package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

/** Grams of each macro. The shape [MacroBar] needs to fill a goal split against what was eaten. */
data class Macros(val proteinG: Int, val carbsG: Int, val fatG: Int)

/**
 * Hard-edged 3-segment macro split bar — protein = `primary`, carbs = `tertiary`, fat =
 * `secondary` (fixed mapping, identical in every macro visualization in the app). Segment widths
 * are proportional to each macro's calorie contribution (protein/carbs at 4 kcal/g, fat at
 * 9 kcal/g), not raw grams.
 *
 * Pass [consumed] to fill the bar rather than draw it solid. The **widths stay the goal's split**
 * and only the fill inside each segment moves, so the bar's shape is a fixed frame the day fills
 * into — segment widths that rearranged themselves after every entry would be noise, not progress.
 * The unfilled remainder is `surfaceContainerHigh`, the tone this system already reserves for the
 * empty half of a progress track.
 *
 * A macro past its goal fills its segment and stops there. The legend beside the bar is what
 * reports the overage: when a figure and its visualisation disagree, the figure is the one telling
 * the truth.
 */
@Composable
fun MacroBar(
    proteinG: Int,
    carbsG: Int,
    fatG: Int,
    modifier: Modifier = Modifier,
    consumed: Macros? = null,
) {
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
        MacroSegment(
            weight = proteinKcal / total,
            color = MaterialTheme.colorScheme.primary,
            fill = consumed?.let { fillFraction(it.proteinG, proteinG) },
        )
        MacroSegment(
            weight = carbsKcal / total,
            color = MaterialTheme.colorScheme.tertiary,
            fill = consumed?.let { fillFraction(it.carbsG, carbsG) },
        )
        MacroSegment(
            weight = fatKcal / total,
            color = MaterialTheme.colorScheme.secondary,
            fill = consumed?.let { fillFraction(it.fatG, fatG) },
        )
    }
}

/** No goal means nothing to be a fraction of — an unset macro reads empty rather than complete. */
internal fun fillFraction(consumedG: Int, goalG: Int): Float =
    if (goalG <= 0) 0f else (consumedG.toFloat() / goalG).coerceIn(0f, 1f)

/** [fill] null draws the solid goal-split segment this bar has always drawn. */
@Composable
private fun RowScope.MacroSegment(weight: Float, color: Color, fill: Float?) {
    if (weight <= 0f) return
    if (fill == null) {
        Surface(color = color, modifier = Modifier.weight(weight).fillMaxHeight()) {}
        return
    }
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fill).background(color))
    }
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

/** The three states the filled bar has to read in: a day barely started, a day mid-way, and one
 * where a single macro has run past its goal while the others have not. */
@PreviewLightDark
@Composable
private fun MacroBarConsumedPreview() {
    AppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                MacroBar(146, 194, 65, consumed = Macros(8, 12, 3))
                MacroBar(146, 194, 65, consumed = Macros(62, 88, 31))
                MacroBar(146, 194, 65, consumed = Macros(151, 90, 64))
            }
        }
    }
}
