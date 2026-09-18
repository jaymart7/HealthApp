package ph.mart.healthapp.feature.food.ui.label.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import ph.mart.healthapp.core.data.food.Nutrients
import ph.mart.healthapp.core.data.food.formatNutrient
import ph.mart.healthapp.core.data.food.readings
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.food.R

/**
 * What the panel gave, listed so it can be checked against the packet.
 *
 * **This exists because four of the seven are not typable.** Vitamin D, calcium, iron and potassium
 * are seeded and repriced but never entered by hand — the app's rule, and the reason a barcode
 * match carries them invisibly. A barcode match can afford that: it came out of a database nobody
 * is asked to verify. A label read is a model copying printed digits, so the figures it copied have
 * to be on screen, or the "check it before you log it" the whole review screen is for stops
 * covering exactly the numbers most worth checking.
 *
 * Read-only on purpose. It reports; the micronutrient group above still owns the three that can be
 * corrected. Nothing here is derived: [readings] with no targets is the same non-zero-in-panel-order
 * list the diary's summary draws, and [formatNutrient] is the same unit conversion.
 *
 * Absent when nothing was read, which is the honest picture of a photo that caught only half a
 * panel — an empty list rather than seven dashes.
 */
@Composable
internal fun LabelPanelReadout(nutrients: Nutrients, modifier: Modifier = Modifier) {
    val rows = nutrients.readings(targets = null)
    if (rows.isEmpty()) return

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
            Text(
                text = stringResource(R.string.food_label_panel_readout),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            rows.forEach { reading ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(reading.nutrient.labelRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatNutrient(reading.nutrient, reading.value),
                        style = MaterialTheme.typography.bodyMedium.tabularNums,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun LabelPanelReadoutPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            LabelPanelReadout(
                nutrients = Nutrients(
                    fiberG = 3,
                    sugarG = 21,
                    sodiumMg = 410,
                    vitaminDUg = 2,
                    calciumMg = 260,
                    ironUg = 1200,
                    potassiumMg = 340,
                ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
