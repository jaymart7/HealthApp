package ph.mart.healthapp.feature.onboarding.ui.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.BubbleTail
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotSpeechBubble
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.StepProgressBar
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.onboarding.R

/** Welcome is the root and is not one of them. */
const val ONBOARDING_STEPS = 6

/** Above this the step count needs a line of its own, the toggles stack and the ruler fields go to
 * a column — the point where the one-row app bar stops fitting beside a 28sp headline. */
private const val REFLOW_FONT_SCALE = 1.3f

@Composable
internal fun reflowed(): Boolean = LocalDensity.current.fontScale >= REFLOW_FONT_SCALE

/** Never shorter than this, however few pixels the column has left to divide. */
val MinCardHeight = 104.dp

/** Roughly what the app bar, Rui's row, the headline and the gaps take off the top. */
private val ChromeHeight = 200.dp

/**
 * Whether the card steps can divide the column rather than scroll it.
 *
 * `weight(1f)` with a minimum height is a clip waiting to happen: on a short screen four cards at
 * their minimum are taller than the space there is to divide, and the last one simply goes missing
 * on a step that has no button under it. So the steps ask first, and fall back to a scroll with
 * fixed-height cards. A large font scale is the same problem arriving from the other direction.
 */
@Composable
internal fun cardsFit(cards: Int): Boolean {
    if (reflowed()) return false
    val available = LocalConfiguration.current.screenHeightDp.dp - ChromeHeight
    return available >= (MinCardHeight + 12.dp) * cards
}

/**
 * Every step from Goal to Confirm: a 48dp app bar, Rui's line, the headline, and whatever the step
 * puts under them.
 *
 * The chrome is one composable rather than a header each screen re-assembles because it is what
 * makes the flow read as one conversation — the avatar and the bubble occupy the same row on all
 * six screens, so Rui never appears to arrive or leave. (The old header stacked arrow, bar and
 * count in three rows and had no mascot at all, which is how the guide came to vanish between
 * steps 1 and 6.)
 *
 * [trailingAction] is the header's optional right-hand action — Skip, and only Skip. Where it is
 * present the step count moves down to the Rui row, because two things cannot have the same
 * corner. [bottomBar] is absent on the steps that advance on tap.
 *
 * [scrollable] is false for the steps whose cards divide the column with `weight(1f)`: a weight
 * inside a scrolling column has no height to divide.
 */
@Composable
internal fun OnboardingStep(
    step: Int,
    mascotState: MascotState,
    line: String,
    headline: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    mascotSize: Dp = 32.dp,
    trailingAction: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val count = stringResource(R.string.onboarding_step_of, step, ONBOARDING_STEPS)
    val stacked = reflowed()
    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    onClick = onBack,
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    // −12dp so the glyph lines up with the 16dp screen padding while the target
                    // stays the full 48dp — an arrow optically inset by its own icon padding.
                    modifier = Modifier.offset(x = (-12).dp).size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = AppIcons.Back,
                            contentDescription = stringResource(R.string.onboarding_back),
                        )
                    }
                }
                StepProgressBar(
                    currentStep = step,
                    totalSteps = ONBOARDING_STEPS,
                    showLabel = false,
                    modifier = Modifier.weight(1f),
                )
                if (trailingAction != null) {
                    trailingAction()
                } else if (!stacked) {
                    StepCount(count)
                }
            }
            // Rui's row carries the count whenever the bar's corner is taken, and whenever the
            // font is large enough that it would not have fitted there anyway.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MascotAvatar(state = mascotState, size = mascotSize)
                MascotSpeechBubble(text = line, tail = BubbleTail.Start, modifier = Modifier.weight(1f, fill = false))
                if (trailingAction != null || stacked) {
                    Spacer(modifier = Modifier.weight(1f))
                    StepCount(count)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )
            Spacer(modifier = Modifier.height(24.dp))
            content()
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (bottomBar != null) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) { bottomBar() }
        }
    }
}

@Composable
private fun StepCount(count: String) {
    Text(
        text = count,
        style = MaterialTheme.typography.labelSmall.tabularNums,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@PreviewLightDark
@Composable
private fun OnboardingStepPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            OnboardingStep(
                step = 2,
                mascotState = MascotState.Idle,
                line = "Four numbers and I can do the maths.",
                headline = "Tell us about yourself",
                onBack = {},
            ) {
                Text("content")
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun OnboardingStepWithSkipPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            OnboardingStep(
                step = 4,
                mascotState = MascotState.Idle,
                line = "Skip it if you like — this only tunes suggestions.",
                headline = "Any dietary preference?",
                onBack = {},
                trailingAction = { TextButton(label = "Skip", onClick = {}) },
            ) {
                Text("content")
            }
        }
    }
}
