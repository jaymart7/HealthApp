package ph.mart.healthapp.feature.progress.ui.measurement.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.cmToDisplayUnit
import ph.mart.healthapp.core.data.profile.lengthUnitLabel
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/** Name, current value, delta (shrink=primary, grow=error, flat/no-prior=onSurfaceVariant), and a
 * small inline sparkline over the part's full history — whole row is tappable. */
@Composable
fun MeasurementRow(name: String, historyCm: List<Double>, unit: UnitSystem, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val current = historyCm.lastOrNull()
    val prior = if (historyCm.size >= 2) historyCm[historyCm.size - 2] else null
    val deltaCm = if (prior != null && current != null) current - prior else null

    Surface(onClick = onTap, color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = current?.let { "${formatCm(it.cmToDisplayUnit(unit))} ${unit.lengthUnitLabel()}" } ?: "No readings yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Sparkline(values = historyCm, modifier = Modifier.size(width = 56.dp, height = 24.dp))
            Text(
                text = deltaCm?.let { "${if (it > 0) "+" else ""}${formatCm(it.cmToDisplayUnit(unit))} ${unit.lengthUnitLabel()}" } ?: "—",
                style = MaterialTheme.typography.bodyMedium.tabularNums,
                color = when {
                    deltaCm == null || deltaCm == 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant
                    deltaCm < 0 -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                },
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun Sparkline(values: List<Double>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val min = values.min()
        val max = values.max()
        val range = (max - min).coerceAtLeast(0.01)
        val points = values.mapIndexed { i, v ->
            Offset(
                x = i / (values.size - 1).toFloat() * size.width,
                y = size.height - ((v - min) / range * size.height).toFloat(),
            )
        }
        for (i in 0 until points.size - 1) {
            drawLine(lineColor, points[i], points[i + 1], strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

private fun formatCm(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString() else "%.1f".format(value)

@PreviewLightDark
@Composable
private fun MeasurementRowPreview() {
    AppTheme {
        MeasurementRow(
            name = "Waist",
            historyCm = listOf(84.0, 83.2, 82.5, 81.8),
            unit = UnitSystem.Metric,
            onTap = {},
        )
    }
}
