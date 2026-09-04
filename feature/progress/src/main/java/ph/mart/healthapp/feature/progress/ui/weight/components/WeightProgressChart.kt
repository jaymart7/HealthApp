package ph.mart.healthapp.feature.progress.ui.weight.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.progress.WeightPoint
import ph.mart.healthapp.core.designsystem.component.formatMonth
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/** Four lines is as many as a 170dp plot can carry without the labels running together. */
private const val GRIDLINES = 4

/** Room for "184" at `labelSmall`, right-aligned against the plot. */
private val AxisGutter = 24.dp

/** The band under the plot the month labels sit in. */
private val LabelBand = 16.dp

private val PlotHeight = 170.dp

/**
 * Gridlines + actual line + 2-point trailing moving-average line + optional dashed goal marker —
 * one [Canvas], no charting library.
 *
 * The axis labels are drawn **inside** the same canvas as the gridlines they name, off the same
 * `yFor` mapping. A separate label gutter laid out beside the plot would distribute its four labels
 * evenly and be a pixel or two out at every scheme and font scale; here a label can only ever sit
 * on its own line.
 *
 * The goal is folded into the axis range like any other value, so a target far from the data
 * flattens the trend rather than dropping off the chart. That is the deliberate call: a goal line
 * you cannot see is a goal you stop steering by, and the legend names the figure either way. The
 * marker is omitted entirely when the profile carries no target weight — with the goal chip and the
 * projection insight, which hide with it.
 */
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
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall.tabularNums.copy(color = labelColor)
    val measurer = rememberTextMeasurer()

    Canvas(modifier = modifier.fillMaxWidth().height(PlotHeight + LabelBand)) {
        if (points.isEmpty()) return@Canvas

        val displayWeights = points.map { it.weightKg.kgToDisplayUnit(unit) }
        val displayAverages = points.map { it.movingAverageKg.kgToDisplayUnit(unit) }
        val displayGoal = goalWeightKg?.kgToDisplayUnit(unit)
        val allValues = displayWeights + displayAverages + listOfNotNull(displayGoal)
        val minValue = allValues.min()
        val maxValue = allValues.max()
        val valueRange = (maxValue - minValue).coerceAtLeast(1.0)

        val gutter = AxisGutter.toPx()
        val plotLeft = gutter + 8.dp.toPx()
        val plotWidth = size.width - plotLeft
        val plotHeight = PlotHeight.toPx()
        // A hair of headroom at each end so the top and bottom gridlines aren't the plot's edges.
        val inset = 8.dp.toPx()
        val usable = plotHeight - inset * 2

        fun yFor(value: Double): Float = (inset + usable - ((value - minValue) / valueRange * usable)).toFloat()
        fun valueAt(y: Float): Double = minValue + (inset + usable - y) / usable * valueRange
        fun xFor(index: Int): Float =
            if (points.size == 1) plotLeft + plotWidth / 2f else plotLeft + index / (points.size - 1).toFloat() * plotWidth

        repeat(GRIDLINES) { row ->
            val y = inset + usable / (GRIDLINES - 1) * row
            drawLine(
                color = gridColor,
                start = Offset(plotLeft, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
            val label = measurer.measure(formatKg(valueAt(y)), labelStyle)
            drawText(
                textLayoutResult = label,
                topLeft = Offset(gutter - label.size.width, y - label.size.height / 2f),
            )
        }

        displayGoal?.let { goal ->
            val y = yFor(goal)
            drawLine(
                color = goalColor,
                start = Offset(plotLeft, y),
                end = Offset(size.width, y),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
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
        drawSeries(displayWeights, actualColor, 2.5.dp.toPx())
        displayWeights.forEachIndexed { i, value ->
            drawCircle(actualColor, radius = 2.5.dp.toPx(), center = Offset(xFor(i), yFor(value)))
        }

        // Four dates across the series, first and last always among them — the ends are what say
        // how far back the chart reaches.
        val stops = if (points.size < 2) listOf(0) else listOf(0, points.size / 3, points.size * 2 / 3, points.size - 1)
        stops.distinct().forEach { index ->
            val label = measurer.measure(formatMonth(points[index].dateEpochDay), labelStyle)
            val x = (xFor(index) - label.size.width / 2f)
                .coerceIn(plotLeft, size.width - label.size.width)
            drawText(textLayoutResult = label, topLeft = Offset(x, plotHeight + 2.dp.toPx()))
        }
    }
}

@PreviewLightDark
@Composable
private fun WeightProgressChartPreview() {
    val points = (0..8).map {
        WeightPoint(
            dateEpochDay = 20_600L + it * 10,
            weightKg = 84.8 - it * 0.26,
            movingAverageKg = 84.9 - it * 0.25,
        )
    }
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                WeightProgressChart(points = points, goalWeightKg = 82.0, unit = UnitSystem.Metric)
            }
        }
    }
}

/** No target weight: the dashed marker is gone, and so is the axis room it was holding. */
@PreviewLightDark
@Composable
private fun WeightProgressChartNoGoalPreview() {
    val points = (0..5).map {
        WeightPoint(
            dateEpochDay = 20_640L + it * 6,
            weightKg = 78.0 - it * 0.3,
            movingAverageKg = 78.1 - it * 0.28,
        )
    }
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                WeightProgressChart(points = points, goalWeightKg = null, unit = UnitSystem.Metric)
            }
        }
    }
}
