package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme

private val GLASS_SIZE = 32.dp

/**
 * One tappable glass per [goal]; the first [glasses] of them are filled. Tapping glass *n* sets
 * the count to *n*, and tapping the last filled glass clears it — so a miscount is corrected in
 * the same gesture that made it, without a separate undo control.
 *
 * Stateless and unit-free: the caller owns the count and formats the volume label. Shared because
 * both Home and the food diary show it, per CLAUDE.md's ≥2-screens rule.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WaterGlassRow(glasses: Int, goal: Int, onSetGlasses: (Int) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        repeat(goal) { index ->
            val filled = index < glasses
            IconButton(
                onClick = { onSetGlasses(glassesAfterTap(glasses, index)) },
                modifier = Modifier
                    .size(GLASS_SIZE)
                    // Each glass announces the count it would set, not "glass 4 of 8" — that's
                    // what the tap actually does.
                    .clearAndSetSemantics {
                        contentDescription = "Set ${glassesAfterTap(glasses, index)} of $goal glasses"
                    },
            ) {
                Icon(
                    imageVector = if (filled) Icons.Filled.LocalDrink else Icons.Outlined.LocalDrink,
                    contentDescription = null,
                    tint = if (filled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                )
            }
        }
    }
}

/** Tapping the glass that currently sits at the end of the filled run clears it; every other tap
 * fills up to (and including) the glass tapped. Never negative. */
internal fun glassesAfterTap(current: Int, tappedIndex: Int): Int =
    (if (tappedIndex == current - 1) tappedIndex else tappedIndex + 1).coerceAtLeast(0)

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
