package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.R as DsR
import ph.mart.healthapp.core.designsystem.component.AIChip
import ph.mart.healthapp.core.designsystem.component.AIChipVariant
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.Motion
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.home.R

private val MascotSize = 56.dp
private val BlockRadius = 24.dp
private val BandRadius = 16.dp

/** One Today-strip cell: a figure already on the screen below, restated. */
internal data class StripCell(val value: String, val label: String)

/**
 * The pinned block: the greeting, the day at a glance, and the model's line — one card, never
 * reordered and never hidden.
 *
 * It is one card rather than the two it replaced because they were answering the same question
 * from two separate blocks. Its corner radius is deliberately larger than the 20dp the reorderable
 * cards below use, so the pinned block reads as chrome rather than as the first card of a list the
 * user can rearrange.
 *
 * The insight lives **inside** it as an inset band. That placement is load-bearing: dismissing it
 * collapses within this block, so the cards below shift up by the band's height and no further —
 * the reason the insight was pinned in the first place. The band keeps the app's rule that
 * `tertiaryContainer` is the AI accent and appears once per screen; the [AIChip] beside the
 * greeting is a chip, not a card background.
 *
 * The greeting is still the app's one door to the coach, and the chip is what makes the tap
 * visible. It is the tap target itself now rather than the whole card being clickable — the strip
 * and the band sit in the same card, and a card that navigated away when you touched a number
 * would be the wrong answer to either.
 */
@Composable
internal fun HomeHeaderBlock(
    greeting: String,
    greetingSub: String,
    strip: List<StripCell>,
    insight: String?,
    insightDismissed: Boolean,
    onOpenCoach: () -> Unit,
    onDismissInsight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier, shape = RoundedCornerShape(BlockRadius)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            GreetingRow(greeting = greeting, greetingSub = greetingSub, onOpenCoach = onOpenCoach)
            if (strip.isNotEmpty()) TodayStrip(strip)
            InsightBand(
                insight = insight,
                visible = insight != null && !insightDismissed,
                onDismiss = onDismissInsight,
            )
        }
    }
}

@Composable
private fun GreetingRow(greeting: String, greetingSub: String, onOpenCoach: () -> Unit) {
    // Resolved out here: a semantics lambda is not a composable scope, and the chip's short label
    // is not a sentence a screen reader should be left with.
    val spoken = stringResource(R.string.home_ask_coach)
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        MascotAvatar(state = MascotState.Idle, size = MascotSize)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = greetingSub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            onClick = onOpenCoach,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier
                .heightIn(min = TapTargetMin)
                .clearAndSetSemantics { contentDescription = spoken },
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                AIChip(label = stringResource(R.string.home_ask_short), variant = AIChipVariant.Default)
            }
        }
    }
}

/**
 * Two or three figures, each one a card that is on the screen below — see `todayStripCards()` for
 * why the strip can never report something the user has hidden.
 */
@Composable
private fun TodayStrip(strip: List<StripCell>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
    ) {
        strip.forEachIndexed { index, cell ->
            if (index > 0) {
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = cell.value,
                    style = MaterialTheme.typography.titleMedium.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = cell.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The model's line, inset in the pinned block.
 *
 * The enter/exit specs are the ones this card carried when it was a card: the *arrival* is the
 * news, and the exit is what stops the block below jumping on dismiss. The last text is held in a
 * plain state so the band keeps its words through the collapse instead of blanking first — the
 * same shape `rememberUpdatedState` has.
 */
@Composable
private fun InsightBand(insight: String?, visible: Boolean, onDismiss: () -> Unit) {
    val shown = remember { mutableStateOf("") }.apply { if (insight != null) value = insight }
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(Motion.Enter, easing = Motion.EmphasizedDecelerate)) +
            fadeIn(tween(Motion.Enter)),
        exit = shrinkVertically(tween(Motion.State, easing = Motion.EmphasizedAccelerate)) +
            fadeOut(tween(Motion.Feedback)),
    ) {
        Surface(
            shape = RoundedCornerShape(BandRadius),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    AIChip(label = stringResource(DsR.string.ds_insight), variant = AIChipVariant.OnAccent)
                    Text(
                        text = shown.value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = stringResource(DsR.string.ds_insight_dismiss),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun HomeHeaderBlockPreview() {
    AppTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                HomeHeaderBlock(
                    greeting = "Good evening",
                    greetingSub = "Almost there for today.",
                    strip = listOf(
                        StripCell("1132", "kcal left"),
                        StripCell("4/8", "water"),
                        StripCell("8,432", "steps"),
                    ),
                    insight = "You're 42 g short on protein today.",
                    insightDismissed = false,
                    onOpenCoach = {},
                    onDismissInsight = {},
                )
                // Day one, or a day the model had nothing to say about: no band, no strip.
                HomeHeaderBlock(
                    greeting = "Good morning",
                    greetingSub = "Ready for breakfast?",
                    strip = emptyList(),
                    insight = null,
                    insightDismissed = false,
                    onOpenCoach = {},
                    onDismissInsight = {},
                )
            }
        }
    }
}
