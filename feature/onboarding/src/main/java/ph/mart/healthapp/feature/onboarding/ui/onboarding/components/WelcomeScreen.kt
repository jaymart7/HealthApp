package ph.mart.healthapp.feature.onboarding.ui.onboarding.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ph.mart.healthapp.core.designsystem.component.BubbleTail
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotSpeechBubble
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.onboarding.R

/** How long the whole entrance takes, and the gap between the three lines of the bottom block. */
private const val ENTRANCE_MS = 300
private const val STAGGER_MS = 40

/**
 * Onboarding step 0. No back button — this is the flow's root.
 *
 * Bottom-weighted rather than centred: a splash screen has one job, and centring gave the title,
 * the bubble and the call to action equal weight. Ordering them by importance costs nothing, gives
 * the screen a spine, and puts the button under the thumb. Rui sits in the upper third with the
 * bubble *beside* him so the tail points at his face — under him it reads as a caption.
 */
@Composable
internal fun WelcomeScreen(onGetStarted: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val mascotScale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.92f,
        animationSpec = tween(ENTRANCE_MS),
        label = "mascot",
    )
    val bubbleAlpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(ENTRANCE_MS, delayMillis = 120),
        label = "bubble",
    )

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
    ) {
        Spacer(modifier = Modifier.weight(0.4f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(contentAlignment = Alignment.Center) {
                // The flow's one decorative element, and it is a role at low alpha rather than a
                // new colour — a contrast swap moves it with everything else.
                Box(
                    modifier = Modifier
                        .size(148.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                )
                MascotAvatar(
                    state = MascotState.Celebrating,
                    size = 104.dp,
                    interactive = true,
                    modifier = Modifier.scale(mascotScale),
                )
            }
            MascotSpeechBubble(
                text = stringResource(R.string.onboarding_welcome_bubble),
                tail = BubbleTail.Start,
                modifier = Modifier.graphicsLayer { alpha = bubbleAlpha },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
            Rising(shown = shown, order = 0) {
                Text(
                    text = stringResource(R.string.onboarding_welcome_title),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 40.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Rising(shown = shown, order = 1) {
                Text(
                    text = stringResource(R.string.onboarding_welcome_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Rising(shown = shown, order = 2) {
                PrimaryButton(
                    label = stringResource(R.string.onboarding_welcome_cta),
                    onClick = onGetStarted,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** Title, body and button arrive 12dp from below, 40ms apart. One helper rather than three copies
 * of the same two animations. */
@Composable
private fun Rising(shown: Boolean, order: Int, content: @Composable () -> Unit) {
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(ENTRANCE_MS, delayMillis = order * STAGGER_MS),
        label = "rise",
    )
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 12.dp.toPx()
        },
    ) {
        content()
    }
}

@PreviewLightDark
@Composable
private fun WelcomeScreenPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            WelcomeScreen(onGetStarted = {})
        }
    }
}
