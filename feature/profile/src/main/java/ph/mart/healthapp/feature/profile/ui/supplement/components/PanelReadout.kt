package ph.mart.healthapp.feature.profile.ui.supplement.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AIChip
import ph.mart.healthapp.core.designsystem.component.AIChipVariant
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.profile.R

/**
 * The Supplement Facts panel as the model read it, line for line.
 *
 * `LabelPanelReadout`'s job with the opposite source. That one draws the four typed figures back
 * through `readings()`, because a nutrition panel's every line has a field. A supplement panel's
 * mostly does not — a multivitamin declares twenty lines and this app grades four — so what is
 * shown here is the **printed text**, not the stored figures: a bottle that says "Vitamin B12
 * 2.4 µg" has to read that way under a supplement whose `Nutrients` has nowhere to put it.
 *
 * Read-only, and the chip is what says a model was involved at all. The four this app *does* grade
 * appear in this list too, so the user checks one thing against the bottle rather than two.
 *
 * **It is the transcript, not the figures.** `DoseNutrientFields` above it is what the supplement
 * actually carries and what the day counts, and a user who corrects a misread digit there leaves
 * this list saying what the model read. That is the point rather than a defect: the two disagreeing
 * is the record of a correction, and overwriting the transcript to match would erase the evidence
 * of what was on the bottle.
 *
 * Absent when [panel] is blank, which is every supplement typed by hand.
 */
@Composable
internal fun PanelReadout(panel: String, modifier: Modifier = Modifier) {
    val lines = panel.lines().filter { it.isNotBlank() }
    if (lines.isEmpty()) return

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AIChip(
                label = stringResource(R.string.profile_supplements_scan_chip),
                variant = AIChipVariant.Default,
            )
            Text(
                text = stringResource(R.string.profile_supplements_scan_panel_caveat),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            lines.forEach { line ->
                Text(
                    text = line,
                    // Tabular, because these are figures and a column of them that jitters
                    // between two renders is the thing the rule exists for.
                    style = MaterialTheme.typography.bodyMedium.tabularNums,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PanelReadoutPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            PanelReadout(
                panel = listOf(
                    "Vitamin D 25 µg",
                    "Calcium 210 mg",
                    "Iron 18 mg",
                    "Vitamin C 90 mg",
                    "Zinc 11 mg",
                ).joinToString("\n"),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
