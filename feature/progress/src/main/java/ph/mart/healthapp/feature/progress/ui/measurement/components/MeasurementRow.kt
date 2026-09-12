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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.R

/** Name, current value, delta (shrink=primary, grow=error, flat/no-prior=onSurfaceVariant), and a
 * small inline sparkline over the part's full history — whole row is tappable.
 *
 * [history] arrives already in display units and [unitLabel] already resolved, because a
 * centimetre and a body fat percentage are not the same kind of number and the part is what knows
 * which: see [toDisplay][ph.mart.healthapp.core.data.progress.toDisplay]. The row formats figures
 * and converts nothing. */
@Composable
fun MeasurementRow(name: String, history: List<Double>, unitLabel: String, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val current = history.lastOrNull()
    val prior = if (history.size >= 2) history[history.size - 2] else null
    val delta = if (prior != null && current != null) current - prior else null

    Surface(onClick = onTap, color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = current?.let {
                        stringResource(R.string.progress_measurement_value, formatMeasurement(it), unitLabel)
                    } ?: stringResource(R.string.progress_measurement_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Sparkline(values = history, modifier = Modifier.size(width = 56.dp, height = 24.dp))
            Text(
                text = delta?.let {
                    stringResource(
                        R.string.progress_measurement_delta,
                        if (it > 0) "+" else "",
                        formatMeasurement(it),
                        unitLabel,
                    )
                } ?: stringResource(R.string.progress_none),
                style = MaterialTheme.typography.bodyMedium.tabularNums,
                color = when {
                    delta == null || delta == 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant
                    delta < 0 -> MaterialTheme.colorScheme.primary
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

internal fun formatMeasurement(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString() else "%.1f".format(value)

@PreviewLightDark
@Composable
private fun MeasurementRowPreview() {
    AppTheme {
        MeasurementRow(
            name = "Waist",
            history = listOf(84.0, 83.2, 82.5, 81.8),
            unitLabel = "cm",
            onTap = {},
        )
    }
}
