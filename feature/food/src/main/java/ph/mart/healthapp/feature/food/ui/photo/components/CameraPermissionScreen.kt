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
 * Camera access was refused. [settingsOnly] is the dead-end case: once the system prompt is spent,
 * launching it again does nothing at all, so the button has to point at Settings instead of at a
 * dialog that will never appear.
 *
 * The caller decides which — it is the one holding the permission state.
 */
@Composable
internal fun CameraPermissionScreen(
    settingsOnly: Boolean,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
        heading = "Camera access needed",
        body = if (settingsOnly) {
            "Camera access is off for FitPulse. Turn it on in Settings to log meals from a photo, or go back and log manually."
        } else {
            "Grant camera access to log meals from a photo, or go back and log manually."
        },
        actions = {
            PrimaryButton(
                label = if (settingsOnly) "Open settings" else "Grant access",
                onClick = if (settingsOnly) onOpenSettings else onGrant,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(label = "Back", onClick = onBack, modifier = Modifier.fillMaxWidth())
        },
    )
}

@PreviewLightDark
@Composable
private fun CameraPermissionScreenPreview() {
    AppTheme { CameraPermissionScreen(settingsOnly = false, onGrant = {}, onOpenSettings = {}, onBack = {}) }
}

/** The dead end, where the prompt is spent and Settings is the only door left. */
@PreviewLightDark
@Composable
private fun CameraPermissionScreenSettingsPreview() {
    AppTheme { CameraPermissionScreen(settingsOnly = true, onGrant = {}, onOpenSettings = {}, onBack = {}) }
}
