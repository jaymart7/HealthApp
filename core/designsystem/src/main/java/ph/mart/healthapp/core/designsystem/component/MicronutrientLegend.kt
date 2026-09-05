package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.R
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/**
 * Fiber, sugar and sodium as one quiet line — the diary's day total and Progress's range average
 * report them identically, which is what puts this here rather than in either feature.
 *
 * **Renders nothing when all three are zero.** They have no goals to sit against (Mifflin–St Jeor
 * produces calories and a macro split, and there is nothing in the profile to derive a fiber target
 * from), so an untouched day would otherwise grow a row of zeros claiming a shortfall against
 * nothing. No colour dots either: these three appear in no bar, and the three macro colours are
 * spoken for.
 */
@Composable
fun MicronutrientLegend(
    fiberG: Int,
    sugarG: Int,
    sodiumMg: Int,
    modifier: Modifier = Modifier,
) {
    if (fiberG <= 0 && sugarG <= 0 && sodiumMg <= 0) return
    val text = stringResource(R.string.ds_nutrient_legend, fiberG, sugarG, sodiumMg.grouped())
    val spoken = stringResource(R.string.ds_nutrient_legend_spoken, fiberG, sugarG, sodiumMg)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = spoken },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.tabularNums,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Sodium is the one figure in the app that routinely runs into the thousands. */
private fun Int.grouped(): String = toString().reversed().chunked(3).joinToString(",").reversed()

@PreviewLightDark
@Composable
private fun MicronutrientLegendPreview() {
    AppTheme {
        Surface {
            MicronutrientLegend(fiberG = 12, sugarG = 40, sodiumMg = 1240, modifier = Modifier.padding(16.dp))
        }
    }
}
