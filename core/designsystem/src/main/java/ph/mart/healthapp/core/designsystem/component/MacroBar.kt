package ph.mart.healthapp.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.Motion

/** What separates the three segments. On the scale, and constant across [MacroBar]'s heights so
 * the diary's full and collapsed bars read as the same object at two sizes. */
private val SegmentGap = 4.dp

/** Grams of each macro. The shape [MacroBar] needs to fill a goal split against what was eaten. */
data class Macros(val proteinG: Int, val carbsG: Int, val fatG: Int)

/**
 * 3-segment macro split bar — protein = `primary`, carbs = `tertiary`, fat = `secondary` (fixed
 * mapping, identical in every macro visualization in the app). Segment widths
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
 *
 * The three segments are **separated and individually rounded** rather than butted together into
 * one continuous strip. Flush segments read as a single bar with colour changes along it, which is
 * a stacked progress bar — one quantity in parts. This is three quantities, each with its own goal
 * and its own fill, and a macro at 40% sitting hard against one at 90% invited exactly the reading
 * that they add up. The gap says they are three.
 *
 * [height] is the one thing a caller may vary, and the corner radius follows it rather than being
 * fixed: the diary's collapsed summary draws the same bar at 4dp, and a 4dp bar clipped at a 4dp
 * radius is a lozenge rather than a bar. Extended rather than forked, per the ≥2-callers rule —
 * six screens draw this and all of them mean the same thing by it, gaps included.
 */
@Composable
fun MacroBar(
    proteinG: Int,
    carbsG: Int,
    fatG: Int,
    modifier: Modifier = Modifier,
    consumed: Macros? = null,
    height: Dp = 8.dp,
) {
    val proteinKcal = (proteinG * 4).coerceAtLeast(0).toFloat()
    val carbsKcal = (carbsG * 4).coerceAtLeast(0).toFloat()
    val fatKcal = (fatG * 9).coerceAtLeast(0).toFloat()
    val total = (proteinKcal + carbsKcal + fatKcal).coerceAtLeast(1f)

    Row(
        modifier = modifier.fillMaxWidth().height(height),
        // spacedBy, not padding on each segment: a macro whose goal is zero draws nothing at all,
        // and this leaves no gap where it would have been.
        horizontalArrangement = Arrangement.spacedBy(SegmentGap),
    ) {
        MacroSegment(
            weight = proteinKcal / total,
            color = MaterialTheme.colorScheme.primary,
            fill = consumed?.let { fillFraction(it.proteinG, proteinG) },
            height = height,
        )
        MacroSegment(
            weight = carbsKcal / total,
            color = MaterialTheme.colorScheme.tertiary,
            fill = consumed?.let { fillFraction(it.carbsG, carbsG) },
            height = height,
        )
        MacroSegment(
            weight = fatKcal / total,
            color = MaterialTheme.colorScheme.secondary,
            fill = consumed?.let { fillFraction(it.fatG, fatG) },
            height = height,
        )
    }
}

/** No goal means nothing to be a fraction of — an unset macro reads empty rather than complete. */
internal fun fillFraction(consumedG: Int, goalG: Int): Float =
    if (goalG <= 0) 0f else (consumedG.toFloat() / goalG).coerceIn(0f, 1f)

/**
 * [fill] null draws the solid goal-split segment this bar has always drawn — so the three callers
 * that pass no [Macros] (onboarding's Confirm step, Profile's Goals card) animate nothing.
 *
 * A filled segment grows to its width rather than snapping there: logging a meal moves this bar,
 * and the movement is what says the entry landed. `Motion.Enter` because it is the same "something
 * arriving" the ladder already names, and it is spent through an animation API so **Remove
 * animations** collapses it to a cut.
 */
@Composable
private fun RowScope.MacroSegment(weight: Float, color: Color, fill: Float?, height: Dp) {
    if (weight <= 0f) return
    // Each segment carries its own clip now that they are separated — one clip on the Row would
    // round the bar's two outer ends and leave the four inner ones square.
    val shape = RoundedCornerShape(height / 2)
    if (fill == null) {
        Surface(color = color, shape = shape, modifier = Modifier.weight(weight).fillMaxHeight()) {}
        return
    }
    val animated by animateFloatAsState(
        targetValue = fill,
        animationSpec = tween(durationMillis = Motion.Enter, easing = Motion.Standard),
        label = "macroFill",
    )
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(animated).background(color))
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
 * where a single macro has run past its goal while the others have not. The last pair is the
 * diary's two heights side by side — 4dp has to read as the same bar, not a hairline. */
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
                MacroBar(146, 194, 65, consumed = Macros(62, 88, 31), height = 4.dp)
            }
        }
    }
}
