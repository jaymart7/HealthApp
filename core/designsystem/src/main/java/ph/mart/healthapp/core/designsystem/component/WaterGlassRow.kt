package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.R
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.Motion
import ph.mart.healthapp.core.designsystem.theme.rememberFillDirection
import ph.mart.healthapp.core.designsystem.theme.stepFillProgress

/** The glyph inside the cell. The cell itself is [goal]-many equal shares of the row's width. */
private val GLASS_ICON = 24.dp

/**
 * One tappable glass per [goal]; the first [glasses] of them are filled. Tapping glass *n* sets
 * the count to *n*, and tapping the last filled glass clears it — so a miscount is corrected in
 * the same gesture that made it, without a separate undo control.
 *
 * Stateless and unit-free: the caller owns the count and formats the volume label. Shared because
 * both Home and the food diary show it, per CLAUDE.md's ≥2-screens rule.
 *
 * The glasses **divide the row's width** rather than wrapping onto a second line. A day's water is
 * one meter, and a meter that breaks in half at the eighth glass reads as two — which is what the
 * previous `FlowRow` did on any phone, since eight 48dp targets need 426dp and a 360dp screen
 * offers 328dp. So the cell is `weight(1f)` and the *row* keeps the 48dp height; only the width of
 * each target shrinks.
 *
 * The row fills left-to-right and clears right-to-left, staggered by ~25ms a glass, because the
 * direction *is* the gesture. Tint and scale run off the one `stepFillProgress` value so the whole
 * row reads as a single idea rather than two effects layered on each other.
 */
@Composable
fun WaterGlassRow(glasses: Int, goal: Int, onSetGlasses: (Int) -> Unit, modifier: Modifier = Modifier) {
    val emptyTint = MaterialTheme.colorScheme.outlineVariant
    val filledTint = MaterialTheme.colorScheme.primary
    val filling = rememberFillDirection(glasses)

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        repeat(goal) { index ->
            val filled = index < glasses
            val fill = stepFillProgress(active = filled, index = index, count = goal, filling = filling)
            val description = stringResource(R.string.ds_water_set, glassesAfterTap(glasses, index), goal)
            IconButton(
                onClick = { onSetGlasses(glassesAfterTap(glasses, index)) },
                // ponytail: at goal 8 on a 360dp screen each cell is ~37dp wide, under the 48dp
                // touch floor the row used to hold by wrapping. The height stays 48dp, so the
                // target is short of the minimum in one axis only. A horizontally scrolling row at
                // a fixed 48dp is the upgrade path if mistaps show up.
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    // Scale is read inside the layer lambda, so it settles in the Draw phase and
                    // never recomposes anything.
                    .graphicsLayer {
                        scaleX = 1f + (Motion.ActiveStepScale - 1f) * fill.value
                        scaleY = scaleX
                    }
                    // Each glass announces the count it would set, not "glass 4 of 8" — that's
                    // what the tap actually does.
                    .clearAndSetSemantics {
                        contentDescription = description
                    },
            ) {
                Icon(
                    // The glyph swaps off the plain boolean rather than the animated value: timing
                    // it to the tint would recompose this icon every frame for a difference nobody
                    // can see. The tint read below recomposes only this leaf.
                    //
                    // This swap *is* the "FILL axis" a Material Symbols font would give — these are
                    // static vectors, so the filled and outlined glyphs are the two ends of it.
                    imageVector = if (filled) Icons.Filled.LocalDrink else Icons.Outlined.LocalDrink,
                    contentDescription = null,
                    tint = lerp(emptyTint, filledTint, fill.value),
                    modifier = Modifier.size(GLASS_ICON),
                )
            }
        }
    }
}

/** Tapping the glass that currently sits at the end of the filled run clears it; every other tap
 * fills up to (and including) the glass tapped. Never negative. */
internal fun glassesAfterTap(current: Int, tappedIndex: Int): Int =
    (if (tappedIndex == current - 1) tappedIndex else tappedIndex + 1).coerceAtLeast(0)

/** Goal 14 is the case the row has to survive without wrapping — it is where the cell is narrowest
 * and where a glyph that didn't shrink with it would collide with its neighbour. */
@PreviewLightDark
@Composable
private fun WaterGlassRowPreview() {
    AppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                WaterGlassRow(glasses = 0, goal = 8, onSetGlasses = {})
                WaterGlassRow(glasses = 5, goal = 8, onSetGlasses = {})
                WaterGlassRow(glasses = 8, goal = 8, onSetGlasses = {})
                WaterGlassRow(glasses = 12, goal = 14, onSetGlasses = {})
            }
        }
    }
}
