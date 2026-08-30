package ph.mart.healthapp.feature.progress.ui.weight.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.progress.WeightPoint
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** Gridlines + actual line + 2-point trailing moving-average line + optional dashed goal marker,
 * omitted entirely when there's no target weight — one [Canvas], no charting library. */
@Composable
fun WeightProgressChart(
    points: List<WeightPoint>,
    goalWeightKg: Double?,
    unit: UnitSystem,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val actualColor = MaterialTheme.colorScheme.primary
    val averageColor = MaterialTheme.colorScheme.secondary
    val goalColor = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        if (points.isEmpty()) return@Canvas

        val displayWeights = points.map { it.weightKg.kgToDisplayUnit(unit) }
        val displayAverages = points.map { it.movingAverageKg.kgToDisplayUnit(unit) }
        val displayGoal = goalWeightKg?.kgToDisplayUnit(unit)
        val allValues = displayWeights + displayAverages + listOfNotNull(displayGoal)
        val minValue = allValues.min()
        val maxValue = allValues.max()
        val valueRange = (maxValue - minValue).coerceAtLeast(1.0)

        fun yFor(value: Double): Float = (size.height - ((value - minValue) / valueRange * size.height)).toFloat()
        fun xFor(index: Int): Float =
            if (points.size == 1) size.width / 2f else index / (points.size - 1).toFloat() * size.width

        repeat(4) { row ->
            val y = size.height / 3f * row
            drawLine(gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }

        displayGoal?.let { goal ->
            val y = yFor(goal)
            drawLine(
                color = goalColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
        }

        fun drawSeries(values: List<Double>, color: Color, strokeWidth: Float) {
            for (i in 0 until values.size - 1) {
                drawLine(
                    color = color,
                    start = Offset(xFor(i), yFor(values[i])),
                    end = Offset(xFor(i + 1), yFor(values[i + 1])),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }

        drawSeries(displayAverages, averageColor, 2.dp.toPx())
        drawSeries(displayWeights, actualColor, 3.dp.toPx())
        displayWeights.forEachIndexed { i, value ->
            drawCircle(actualColor, radius = 3.dp.toPx(), center = Offset(xFor(i), yFor(value)))
        }
    }
}

@PreviewLightDark
@Composable
private fun WeightProgressChartPreview() {
    val points = listOf(
        WeightPoint(dateEpochDay = 0, weightKg = 78.0, movingAverageKg = 78.0),
        WeightPoint(dateEpochDay = 1, weightKg = 77.5, movingAverageKg = 77.75),
        WeightPoint(dateEpochDay = 2, weightKg = 77.8, movingAverageKg = 77.65),
        WeightPoint(dateEpochDay = 3, weightKg = 76.9, movingAverageKg = 77.35),
        WeightPoint(dateEpochDay = 4, weightKg = 76.5, movingAverageKg = 76.7),
    )
    AppTheme {
        Surface {
            Column {
                WeightProgressChart(points = points, goalWeightKg = 72.0, unit = UnitSystem.Metric)
            }
        }
    }
}
