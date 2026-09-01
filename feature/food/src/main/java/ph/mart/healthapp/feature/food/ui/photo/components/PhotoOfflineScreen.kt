package ph.mart.healthapp.feature.food.ui.photo.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * Photo logging is the one feature that genuinely needs the network, so this offers the manual
 * path rather than only apologising.
 *
 * [retried] changes the copy after a "Try again" that found the network still down — without it
 * the tap looks ignored, which is the whole reason the flag exists.
 */
@Composable
internal fun PhotoOfflineScreen(retried: Boolean, onLogManually: () -> Unit, onRetry: () -> Unit) {
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
        heading = "No connection",
        body = if (retried) {
            "Still nothing. Photo logging needs a connection — you can log manually in the meantime."
        } else {
            "Photo logging needs a connection. You can still log manually — everything else works offline."
        },
        actions = {
            PrimaryButton(label = "Log manually", onClick = onLogManually, modifier = Modifier.fillMaxWidth())
            SecondaryButton(label = "Try again", onClick = onRetry, modifier = Modifier.fillMaxWidth())
        },
    )
}

@PreviewLightDark
@Composable
private fun PhotoOfflineScreenPreview() {
    AppTheme { PhotoOfflineScreen(retried = false, onLogManually = {}, onRetry = {}) }
}

/** The second look, after a retry that found nothing — a different sentence, same screen. */
@PreviewLightDark
@Composable
private fun PhotoOfflineScreenRetriedPreview() {
    AppTheme { PhotoOfflineScreen(retried = true, onLogManually = {}, onRetry = {}) }
}
