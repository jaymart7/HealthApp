package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ph.mart.healthapp.core.data.cycle.CyclePrediction
import ph.mart.healthapp.core.data.cycle.FlowLevel
import ph.mart.healthapp.core.data.cycle.TAPPABLE_FLOW
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.Motion
import ph.mart.healthapp.core.designsystem.theme.rememberFillDirection
import ph.mart.healthapp.core.designsystem.theme.stepFillProgress

private val STEP_SIZE = 32.dp

/**
 * Where you are in the cycle, and today's flow in one tap.
 *
 * The row is a **meter**, like [MoodCard]'s energy row and unlike its mood row: light, medium and
 * heavy are one scale, so filling up to the level is the honest read. Tapping the level you are
 * already on clears it, so a mis-tap is corrected by the gesture that made it.
 *
 * [FlowLevel.Unstated] fills nothing — it is what an imported Health Connect day carries, and the
 * caption says so rather than the card guessing an intensity nobody reported.
 *
 * Read-only beyond that: the history, the symptoms and any past day belong to the sheet on the
 * Progress tab, which is where a date picker can live.
 */
@Composable
fun CycleCard(
    cycleDay: Int?,
    prediction: CyclePrediction?,
    todayEpochDay: Long,
    flow: Int,
    onSetFlow: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Text(
            text = "Cycle",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = cycleDay?.let { "Day $it" } ?: "No period logged yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = cycleSubline(prediction, todayEpochDay),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        FlowRow(flow = flow, onSetFlow = onSetFlow)
        if (flow == FlowLevel.Unstated.value) {
            Text(
                text = "Imported as a period day — tap to say how heavy it was.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * The line under the day number. A prediction that has passed is reported as *late* rather than
 * hidden — a late period is the thing someone opens this card to check — and no prediction says
 * what would make one, because a card that goes quiet without saying why gives nobody a reason to
 * keep logging.
 */
internal fun cycleSubline(prediction: CyclePrediction?, todayEpochDay: Long): String {
    if (prediction == null) return "Log two periods and this shows when the next one is due."
    return when (val away = prediction.daysAway(todayEpochDay)) {
        0 -> "Next period expected today"
        1 -> "Next period expected tomorrow"
        in 2..Int.MAX_VALUE -> "Next period expected in $away days"
        -1 -> "Expected yesterday"
        else -> "Expected ${abs(away)} days ago"
    }
}

@Composable
private fun FlowRow(flow: Int, onSetFlow: (Int) -> Unit) {
    val inactiveTint = MaterialTheme.colorScheme.outlineVariant
    val activeTint = MaterialTheme.colorScheme.primary
    val filling = rememberFillDirection(flow)

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Flow",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TAPPABLE_FLOW.forEachIndexed { index, level ->
                val fill = stepFillProgress(
                    active = level.value <= flow,
                    index = index,
                    count = TAPPABLE_FLOW.size,
                    filling = filling,
                    stagger = true,
                )
                IconButton(
                    onClick = { onSetFlow(if (level.value == flow) 0 else level.value) },
                    modifier = Modifier
                        .size(STEP_SIZE)
                        // Read inside the layer lambda: the pop settles in the Draw phase.
                        .graphicsLayer {
                            scaleX = 1f + (Motion.ActiveStepScale - 1f) * fill.value
                            scaleY = scaleX
                        }
                        // The level the tap would set, not "drop 2 of 3" — WaterGlassRow's rule.
                        .clearAndSetSemantics { contentDescription = "Set flow to ${level.label}" },
                ) {
                    Icon(
                        imageVector = if (level.value <= flow) {
                            Icons.Filled.WaterDrop
                        } else {
                            Icons.Outlined.WaterDrop
                        },
                        contentDescription = null,
                        tint = lerp(inactiveTint, activeTint, fill.value),
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun CycleCardPreview() {
    AppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                CycleCard(
                    cycleDay = 14,
                    prediction = CyclePrediction(20_015, averageCycleDays = 29, basedOnCycles = 4),
                    todayEpochDay = 20_000,
                    flow = 0,
                    onSetFlow = {},
                )
                CycleCard(
                    cycleDay = 2,
                    prediction = CyclePrediction(19_998, averageCycleDays = 29, basedOnCycles = 6),
                    todayEpochDay = 20_000,
                    flow = FlowLevel.Heavy.value,
                    onSetFlow = {},
                )
                CycleCard(
                    cycleDay = null,
                    prediction = null,
                    todayEpochDay = 20_000,
                    flow = 0,
                    onSetFlow = {},
                )
            }
        }
    }
}
