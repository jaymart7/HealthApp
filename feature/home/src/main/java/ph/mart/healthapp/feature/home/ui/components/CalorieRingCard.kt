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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.exercise.EARNED_MIN_KCAL
import ph.mart.healthapp.core.data.exercise.earnedRingLine
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.Motion
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.home.R

private val RING_SIZE = 120.dp
private val RING_STROKE = 24.dp

/**
 * The day's calories, and the one card on Home with promoted visual weight.
 *
 * Ring shows the share of the day's calorie goal already consumed; the centre reads the kcal still
 * left. [goalKcal] comes from `Profile.dailyTargets()`, never a stored copy — and already has
 * [burnedKcal] folded in by `budgetKcal()`, so the ring can't disagree with the diary's summary
 * bar. [burnedKcal] is passed separately only to name the difference on the card; pass 0 when the
 * user has turned the exercise credit off.
 *
 * It is the hero because it is the figure the whole app is arranged around, and because a screen
 * where every card carries the same weight is the flat scroll this redesign was fixing. It is also
 * the reason nothing else got promoted: two heroes is no hero.
 *
 * The share of the track [burnedKcal] bought is drawn as its own arc in `primaryContainer`, behind
 * the progress sweep so eating into it reads as spending it. `primaryContainer` because every
 * other candidate is spoken for: `tertiaryContainer` is the AI accent's and nothing else's, and
 * `secondary`/`tertiary` carry Fat and Carbs wherever a macro is in the room.
 *
 * Both the arc and the line are held back under [EARNED_MIN_KCAL] rather than at zero: a credit
 * that small moves a day's budget by under two percent — an arc nobody can see under a sentence
 * congratulating them for it. The diary's summary bar still states any credit at all, flatly,
 * which is where a 30 kcal walk is accounted for.
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
    onClick: (() -> Unit)? = null,
) {
    val target = if (goalKcal > 0) (consumedKcal.toFloat() / goalKcal).coerceIn(0f, 1f) else 0f
    val progress = animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = Motion.Settle, easing = Motion.EmphasizedDecelerate),
        label = "calorieRingProgress",
    )
    AppCard(modifier = modifier, onClick = onClick) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CalorieRing(
                progress = progress,
                remainingKcal = goalKcal - consumedKcal,
                earnedShare = if (goalKcal > 0 && burnedKcal >= EARNED_MIN_KCAL) {
                    (burnedKcal.toFloat() / goalKcal).coerceIn(0f, 1f)
                } else {
                    0f
                },
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.home_calories_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // Inside the budget the profile set. Over it is neutral, not `error`: the
                    // budget is a target the user chose, and a day is not a verdict.
                    StatusDot(if (consumedKcal <= goalKcal) StatusMark.OnTrack else StatusMark.None)
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$consumedKcal",
                        style = MaterialTheme.typography.headlineSmall.tabularNums,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.home_calories_of_goal, goalKcal),
                        style = MaterialTheme.typography.bodyMedium.tabularNums,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.home_calories_consumed),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (burnedKcal >= EARNED_MIN_KCAL) {
                        Text(
                            text = earnedRingLine(burnedKcal),
                            style = MaterialTheme.typography.bodyMedium.tabularNums,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

/** [progress] arrives as a [State] rather than a plain float so the sweep is read inside the
 * [Canvas] draw lambda — the whole animation lives in the Draw phase and recomposes nothing. */
@Composable
private fun CalorieRing(progress: State<Float>, remainingKcal: Int, earnedShare: Float) {
    val trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh
    val earnedColor: Color = MaterialTheme.colorScheme.primaryContainer
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
            // The tail of the track, so the slice the day was *given* sits where the day ends —
            // and drawn before the sweep, so consuming it paints over it rather than beside it.
            if (earnedShare > 0f) {
                drawArc(
                    color = earnedColor,
                    startAngle = -90f + 360f * (1f - earnedShare),
                    sweepAngle = 360f * earnedShare,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
            }
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
                style = MaterialTheme.typography.headlineMedium.tabularNums,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.home_calories_left),
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                CalorieRingCard(consumedKcal = 1560, goalKcal = 2692, burnedKcal = 431)
                // A credit big enough to name a meal, on a day barely started — the earned arc is
                // the whole tail of the ring.
                CalorieRingCard(consumedKcal = 420, goalKcal = 3010, burnedKcal = 749)
                // Over budget, and with the exercise credit switched off: no dot, no red.
                CalorieRingCard(consumedKcal = 2810, goalKcal = 2261)
            }
        }
    }
}
