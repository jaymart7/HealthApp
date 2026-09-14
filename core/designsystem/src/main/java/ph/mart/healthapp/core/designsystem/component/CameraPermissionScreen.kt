package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.R
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * Camera access was refused. [settingsOnly] is the dead-end case: once the system prompt is spent,
 * launching it again does nothing at all, so the button has to point at Settings instead of at a
 * dialog that will never appear.
 *
 * The caller decides which — it is the one holding the permission state.
 *
 * Here rather than in a feature because all three camera flows draw it: the meal photo, the barcode
 * scan and the progress shot. The heading and both buttons say the same thing every time; only the
 * body differs, because what the camera was *for* differs — so both bodies arrive already resolved
 * from the caller, which is a composable and owns the strings.
 *
 * [extraAction] is the door a particular flow still has with the camera off. The progress shot has
 * one — the photo picker needs no permission — and losing it would make a denial cost more here
 * than it does on the meal flow, where the manual path is a screen away rather than a button.
 */
@Composable
fun CameraPermissionScreen(
    settingsOnly: Boolean,
    grantBody: String,
    settingsBody: String,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    extraAction: @Composable () -> Unit = {},
) {
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
        heading = stringResource(R.string.ds_camera_needed),
        body = if (settingsOnly) settingsBody else grantBody,
        actions = {
            PrimaryButton(
                label = stringResource(if (settingsOnly) R.string.ds_open_settings else R.string.ds_grant_access),
                onClick = if (settingsOnly) onOpenSettings else onGrant,
                modifier = Modifier.fillMaxWidth(),
            )
            extraAction()
            SecondaryButton(label = stringResource(R.string.ds_back), onClick = onBack, modifier = Modifier.fillMaxWidth())
        },
    )
}

@PreviewLightDark
@Composable
private fun CameraPermissionScreenPreview() {
    AppTheme {
        CameraPermissionScreen(
            settingsOnly = false,
            grantBody = "Grant camera access to log meals from a photo, or go back and log manually.",
            settingsBody = "",
            onGrant = {},
            onOpenSettings = {},
            onBack = {},
        )
    }
}

/** The dead end, where the prompt is spent and Settings is the only door left. */
@PreviewLightDark
@Composable
private fun CameraPermissionScreenSettingsPreview() {
    AppTheme {
        CameraPermissionScreen(
            settingsOnly = true,
            grantBody = "",
            settingsBody = "Camera access is off for FitPulse. Turn it on in Settings to log meals from a photo, or go back and log manually.",
            onGrant = {},
            onOpenSettings = {},
            onBack = {},
        )
    }
}
