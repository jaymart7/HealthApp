package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.stringResource
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
import ph.mart.healthapp.feature.home.R

/** Matches [MoodCard]'s, so the two meters on Home line up their steps. */
private val NAME_WIDTH = 52.dp

/**
 * Where you are in the cycle, and today's flow in one tap.
 *
 * The row is a **meter**, like [MoodCard]'s two rows and [WaterCard]'s glasses: light, medium and
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
            text = stringResource(R.string.home_cycle_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = cycleDay?.let { stringResource(R.string.home_cycle_day, it) }
                ?: stringResource(R.string.home_cycle_none),
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
                text = stringResource(R.string.home_cycle_imported),
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
@Composable
internal fun cycleSubline(prediction: CyclePrediction?, todayEpochDay: Long): String {
    if (prediction == null) return stringResource(R.string.home_cycle_no_prediction)
    return when (val away = prediction.daysAway(todayEpochDay)) {
        0 -> stringResource(R.string.home_cycle_due_today)
        1 -> stringResource(R.string.home_cycle_due_tomorrow)
        in 2..Int.MAX_VALUE -> stringResource(R.string.home_cycle_due_in, away)
        -1 -> stringResource(R.string.home_cycle_late_yesterday)
        else -> stringResource(R.string.home_cycle_late_days, abs(away))
    }
}

@Composable
private fun FlowRow(flow: Int, onSetFlow: (Int) -> Unit) {
    val inactiveTint = MaterialTheme.colorScheme.outlineVariant
    val activeTint = MaterialTheme.colorScheme.primary
    val filling = rememberFillDirection(flow)

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.home_cycle_flow),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(NAME_WIDTH),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            TAPPABLE_FLOW.forEachIndexed { index, level ->
                val fill = stepFillProgress(
                    active = level.value <= flow,
                    index = index,
                    count = TAPPABLE_FLOW.size,
                    filling = filling,
                    stagger = true,
                )
                // Resolved outside the semantics lambda, which cannot read a resource.
                val description = stringResource(R.string.home_cycle_set_flow, stringResource(level.label))
                IconButton(
                    onClick = { onSetFlow(if (level.value == flow) 0 else level.value) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = TapTargetMin)
                        // Read inside the layer lambda: the pop settles in the Draw phase.
                        .graphicsLayer {
                            scaleX = 1f + (Motion.ActiveStepScale - 1f) * fill.value
                            scaleY = scaleX
                        }
                        // The level the tap would set, not "drop 2 of 3" — WaterGlassRow's rule.
                        .clearAndSetSemantics { contentDescription = description },
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
