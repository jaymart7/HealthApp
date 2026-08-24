package ph.mart.healthapp.feature.progress.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** Stands in for the real weight/photos/measurements tabs until Phase 6. */
@Composable
fun ProgressPlaceholderScreen() {
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Idle, size = 64.dp) },
        heading = "Progress",
        body = "Weight, photos, and measurements arrive in Phase 6.",
    )
}

/** Stub target for the FAB's "Log weight" action — real sheet arrives in Phase 6. */
@Composable
fun LogWeightStubScreen(onClose: () -> Unit) {
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Thinking, size = 64.dp) },
        heading = "Log weight",
        body = "The real entry sheet arrives in Phase 6.",
        actions = { SecondaryButton(label = "Back", onClick = onClose) },
    )
}

/** Stub target for the FAB's "Add photo" action — real sheet arrives in Phase 6. */
@Composable
fun AddPhotoStubScreen(onClose: () -> Unit) {
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Thinking, size = 64.dp) },
        heading = "Add photo",
        body = "The real entry sheet arrives in Phase 6.",
        actions = { SecondaryButton(label = "Back", onClick = onClose) },
    )
}

@PreviewLightDark
@Composable
private fun ProgressPlaceholderScreenPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ProgressPlaceholderScreen()
        }
    }
}
