package ph.mart.healthapp.feature.coach.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.insight.InsightRequest
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.coach.R
import ph.mart.healthapp.feature.coach.ui.STARTERS
import ph.mart.healthapp.feature.coach.ui.Starter

/**
 * A conversation nobody has started, and the one screen in this feature that has to teach rather
 * than answer. Three blocks, spread over the list's whole height.
 *
 * **The greeting bubble is gone.** It said "ask me about any day you've logged" in a speech bubble
 * and then a caption under it said what the coach could do — two sentences making the same promise,
 * one of them dressed as a turn that never happened. What is left is a single capability line
 * beside a 64dp mascot, and it is a *line* rather than a bubble precisely because nothing has been
 * said yet.
 *
 * **The context strip is the honest half.** It shows the three figures the coach is told about
 * before it is asked anything, which is what makes the starters credible — a coach that claims to
 * read your diary should be able to prove it above the fold. It is **read-only and never a tap
 * target**: no chevron, no CTA, no rings, no bars and no colour, because the moment it looks
 * actionable it is a second Home screen and this screen stops being a chat.
 *
 * **The starters sit at the bottom, under the thumb**, and each carries an eyebrow naming the kind
 * of question it stands in for. They are the first thing the keyboard pushes off screen, which is
 * right: once the user is typing they have stopped needing an example.
 */
@Composable
internal fun CoachEmptyState(
    onStarter: (String) -> Unit,
    modifier: Modifier = Modifier,
    request: InsightRequest? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        if (request != null) ContextStrip(request)
        CapabilityLine(modifier = Modifier.padding(vertical = 24.dp))
        Starters(onStarter = onStarter)
    }
}

/**
 * What the coach already knows, before it is asked. Three `value / goal` pairs and nothing else.
 *
 * Water reads in **glasses**, not the litres a mock can afford: glasses is what this app stores,
 * what `WaterGlassRow` taps out and what `InsightRequest` carries to the model, and a strip that
 * converted would be the one figure on the screen the coach could not repeat back.
 */
@Composable
private fun ContextStrip(request: InsightRequest, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.coach_empty_seen,
                    stringResource(R.string.coach_empty_seen_today),
                ).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SeenPair(
                    value = request.caloriesConsumed.toString(),
                    goal = request.caloriesTarget.toString(),
                    unit = stringResource(R.string.coach_empty_calories),
                    modifier = Modifier.weight(1f),
                )
                SeenPair(
                    value = request.proteinG.toString(),
                    goal = request.proteinTargetG.toString(),
                    unit = stringResource(R.string.coach_empty_protein),
                    modifier = Modifier.weight(1f),
                )
                SeenPair(
                    value = request.waterGlasses.toString(),
                    goal = request.waterGoalGlasses.toString(),
                    unit = stringResource(R.string.coach_empty_water),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** One column of the strip. Three nodes to the eye and one phrase to a screen reader — read out
 * separately they are six unlabelled numbers. */
@Composable
private fun SeenPair(value: String, goal: String, unit: String, modifier: Modifier = Modifier) {
    // A semantics lambda cannot read a resource, so the spoken form is resolved a line above it.
    val spoken = stringResource(R.string.coach_empty_pair_spoken, value, goal, unit)
    Column(modifier = modifier.clearAndSetSemantics { contentDescription = spoken }) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.tabularNums,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.coach_empty_of_goal, goal),
                style = MaterialTheme.typography.bodySmall.tabularNums,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The mascot and the one sentence that replaced the greeting. A line, not a bubble: nothing has
 * been said yet, and a bubble would be a turn that never happened. */
@Composable
private fun CapabilityLine(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MascotAvatar(state = MascotState.Idle, size = 64.dp, interactive = true)
        Text(
            text = stringResource(R.string.coach_empty_limits),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** The 2×2. Two `Row`s of two weighted cards rather than a grid: a `LazyVerticalGrid` inside a
 * `LazyColumn` is an unbounded-height crash, and four fixed cells need no laziness. */
@Composable
private fun Starters(onStarter: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.coach_starter_heading).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        STARTERS.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { starter ->
                    StarterCard(
                        starter = starter,
                        onStarter = onStarter,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * One opener: the reach it stands for, then the question.
 *
 * Filled `surfaceContainerLow`, which is the first of the screen's three commitment shapes — the
 * follow-up pills are outlined and the draft card's confirm is the only filled `primary`, so no two
 * of them read as the same control. The tap sends the **resolved question**, never the eyebrow.
 */
@Composable
private fun StarterCard(starter: Starter, onStarter: (String) -> Unit, modifier: Modifier = Modifier) {
    val question = stringResource(starter.question)
    Surface(
        onClick = { onStarter(question) },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.heightIn(min = 96.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(starter.reach).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = question,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun CoachEmptyStatePreview() {
    AppTheme {
        Surface {
            CoachEmptyState(
                onStarter = {},
                modifier = Modifier.padding(16.dp),
                request = PREVIEW_REQUEST,
            )
        }
    }
}

/** Day one, before a profile exists: no targets, so no strip — and the coach's own first sentence
 * in that state is that it has none. */
@PreviewLightDark
@Composable
private fun CoachEmptyStateNoProfilePreview() {
    AppTheme {
        Surface {
            CoachEmptyState(onStarter = {}, modifier = Modifier.padding(16.dp))
        }
    }
}

private val PREVIEW_REQUEST = InsightRequest(
    goal = Goal.Lose,
    caloriesConsumed = 1240,
    caloriesTarget = 2000,
    proteinG = 78,
    proteinTargetG = 150,
    carbsG = 120,
    carbsTargetG = 200,
    fatG = 40,
    fatTargetG = 67,
    waterGlasses = 5,
    waterGoalGlasses = 8,
    streakDays = 4,
    weightDeltaKg = -0.4,
)

