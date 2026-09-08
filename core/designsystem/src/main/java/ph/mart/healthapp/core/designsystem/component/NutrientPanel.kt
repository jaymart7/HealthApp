package ph.mart.healthapp.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.R
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/**
 * One nutrient, resolved. Plain strings and a fraction rather than the app's `Nutrient` enum,
 * because this module is a leaf with no dependency on `:core:data` — the caller resolves the label
 * and formats the figures, and the arithmetic behind [fraction] and [overLimit] is written once in
 * `Nutrients.readings()`.
 */
data class NutrientRow(
    val label: String,
    val value: String,
    /** Null leaves the row ungraded — the reading with nothing to sit against. */
    val target: String? = null,
    val fraction: Float = 0f,
    val overLimit: Boolean = false,
)

/**
 * The nutrients a day or a range carried, as one quiet line that opens into a graded list.
 *
 * Replaces the old three-value legend and keeps its two rules. It **renders nothing when [rows] is
 * empty** — a day of quick adds has no nutrients, and a list of zeros against targets would claim
 * a shortfall that isn't one. And there are **no colour dots**: protein, carbs and fat own the
 * three semantic colours app-wide because they share a bar, and nothing here appears in that bar.
 * The only colour this panel spends is `error`, and only past a limit — the same rule the trend
 * arrows follow.
 *
 * Collapsed it shows the first three rows on one line, which are the three this app has reported
 * since before it graded anything; expanded, every row it was given. In place, not in a sheet:
 * there is no level to come back from, so nothing here needs a back handler.
 *
 * [coverage] is what stops the graded rows lying. `0` means unknown-or-none in every nutrient
 * field in this app, so a caller that knows how many of the day's foods actually carried figures
 * passes that sentence and the panel prints it under the list.
 */
@Composable
fun NutrientPanel(
    rows: List<NutrientRow>,
    modifier: Modifier = Modifier,
    coverage: String? = null,
) {
    if (rows.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    val summary = rows.take(SUMMARY_ROWS).joinToString(" · ") { "${it.label} ${it.value}" }
    // A semantics lambda cannot read a resource, so every row is spoken out here, one line above
    // the modifier that carries it.
    val spoken = rows.map { it.spoken() }.joinToString(", ")

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = summary,
                style = MaterialTheme.typography.labelSmall.tabularNums,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics { contentDescription = spoken },
            )
            Icon(
                imageVector = AppIcons.ChevronDown,
                contentDescription = stringResource(
                    if (expanded) R.string.ds_nutrients_hide else R.string.ds_nutrients_show,
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(rotation),
            )
        }
        if (expanded) {
            rows.forEach { NutrientBarRow(it) }
            if (coverage != null) {
                Text(
                    text = coverage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NutrientRow.spoken(): String = if (target != null) {
    stringResource(R.string.ds_nutrient_reading_of, label, value, target)
} else {
    stringResource(R.string.ds_nutrient_reading, label, value)
}

/** The three the collapsed line names — fiber, sugar and sodium, which lead the nutrient list for
 * exactly this reason: they are what the line has always said. */
private const val SUMMARY_ROWS = 3

@Composable
private fun NutrientBarRow(row: NutrientRow) {
    val spoken = row.spoken()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = spoken },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = row.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (row.target != null) "${row.value} / ${row.target}" else row.value,
                style = MaterialTheme.typography.labelMedium.tabularNums,
                color = if (row.overLimit) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
        if (row.target != null) {
            NutrientBar(fraction = row.fraction, overLimit = row.overLimit)
        }
    }
}

/** A track and a fill, drawn with two boxes rather than a `LinearProgressIndicator`: the app's own
 * bars are all hand-drawn to the 4dp corner the rest of the surface uses. */
@Composable
private fun NutrientBar(fraction: Float, overLimit: Boolean) {
    val fill = if (overLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        if (fraction > 0f) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .background(fill),
            ) {}
        }
    }
}

/** Nothing logged worth reporting — the panel draws no chrome at all. */
@PreviewLightDark
@Composable
private fun NutrientPanelEmptyPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp).width(320.dp)) {
                Text("(panel renders nothing above)", style = MaterialTheme.typography.labelSmall)
                NutrientPanel(rows = emptyList())
            }
        }
    }
}

/** A full day off a scanned packet: sodium past its limit, four panel nutrients present, and two
 * of the day's foods carrying no figures at all. */
@PreviewLightDark
@Composable
private fun NutrientPanelPreview() {
    AppTheme {
        Surface {
            NutrientPanel(
                rows = PREVIEW_ROWS,
                coverage = "Vitamins and minerals from 5 of 7 foods",
                modifier = Modifier.padding(16.dp).width(320.dp),
            )
        }
    }
}

private val PREVIEW_ROWS = listOf(
    NutrientRow("Fiber", "24 g", "27 g", 0.88f),
    NutrientRow("Sugar", "63 g", "49 g", 1f, overLimit = true),
    NutrientRow("Sodium", "2,780 mg", "2,300 mg", 1f, overLimit = true),
    NutrientRow("Vitamin D", "9 µg", "15 µg", 0.6f),
    NutrientRow("Calcium", "740 mg", "1,000 mg", 0.74f),
    NutrientRow("Iron", "11.2 mg", "18.0 mg", 0.62f),
    NutrientRow("Potassium", "2,410 mg", "3,400 mg", 0.71f),
)
