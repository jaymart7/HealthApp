package ph.mart.healthapp.feature.onboarding.ui.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotSpeechBubble
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** Onboarding step 0. No back button — this is the flow's root. "I already have an account" is a
 * no-op per the prototype (no auth system exists yet). */
@Composable
internal fun WelcomeScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MascotAvatar(state = MascotState.Celebrating, size = 112.dp)
        Spacer(Modifier.height(24.dp))
        MascotSpeechBubble(text = "Let's build healthy habits together.")
        Spacer(Modifier.height(24.dp))
        Text(
            text = "FitPulse",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Track your body and nutrition, with Bibo by your side.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        PrimaryButton(label = "Get started", onClick = onGetStarted, modifier = Modifier.fillMaxWidth())
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
