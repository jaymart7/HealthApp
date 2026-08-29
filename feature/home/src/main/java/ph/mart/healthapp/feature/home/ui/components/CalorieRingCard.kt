package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.Motion
import ph.mart.healthapp.core.designsystem.theme.tabularNums

private val RING_SIZE = 96.dp
private val RING_STROKE = 10.dp

/**
 * Ring shows the share of the day's calorie goal already consumed; the centre reads the kcal
 * still left. [goalKcal] comes from `Profile.dailyTargets()`, never a stored copy — and already
 * has [burnedKcal] folded in by `budgetKcal()`, so the ring can't disagree with the diary's
 * summary bar. [burnedKcal] is passed separately only to name the difference on the card; pass 0
 * when the user has turned the exercise credit off.
 *
 * The arc is the app's one authored entrance: it sweeps to its share over [Motion.Settle] while
 * the numbers stay instant and true, so the ring reads as settling onto a fact rather than the
 * fact waiting on the ring.
 *
 * This needs no "have I swept yet" flag. `animateFloatAsState` doesn't animate on first
 * composition, and `HomeViewModel` is retained per nav entry — so returning to the Home tab
 * composes the ring already at its true value and it simply draws. The sweep fires only when the
 * value actually changes: cold start, and after something is logged.
 */
@Composable
fun CalorieRingCard(
    consumedKcal: Int,
    goalKcal: Int,
    modifier: Modifier = Modifier,
    burnedKcal: Int = 0,
) {
    val target = if (goalKcal > 0) (consumedKcal.toFloat() / goalKcal).coerceIn(0f, 1f) else 0f
    val progress = animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = Motion.Settle, easing = Motion.EmphasizedDecelerate),
        label = "calorieRingProgress",
    )
    AppCard(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CalorieRing(progress = progress, remainingKcal = goalKcal - consumedKcal)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Consumed",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$consumedKcal",
                        style = MaterialTheme.typography.headlineSmall.tabularNums,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = " / $goalKcal kcal",
                        style = MaterialTheme.typography.bodyMedium.tabularNums,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                if (burnedKcal > 0) {
                    Text(
                        text = "+$burnedKcal kcal from exercise",
                        style = MaterialTheme.typography.labelMedium.tabularNums,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** [progress] arrives as a [State] rather than a plain float so the sweep is read inside the
 * [Canvas] draw lambda — the whole animation lives in the Draw phase and recomposes nothing. */
@Composable
private fun CalorieRing(progress: State<Float>, remainingKcal: Int) {
    val trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh
    val progressColor: Color = MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.size(RING_SIZE), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = RING_STROKE.toPx())
            val inset = stroke.width / 2
            val arcSize = Size(
                width = size.width - stroke.width,
                height = size.height - stroke.width,
            )
            val topLeft = Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.value,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$remainingKcal",
                style = MaterialTheme.typography.titleMedium.tabularNums,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "left",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun CalorieRingCardPreview() {
    AppTheme {
        Surface {
            CalorieRingCard(
                consumedKcal = 940,
                goalKcal = 2261,
                burnedKcal = 320,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
