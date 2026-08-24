package ph.mart.healthapp.feature.profile.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** Stands in for the real settings screen until Phase 8. */
@Composable
fun ProfilePlaceholderScreen() {
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Idle, size = 64.dp) },
        heading = "Profile",
        body = "Goals, units, reminders, and export arrive in Phase 8.",
    )
}

@PreviewLightDark
@Composable
private fun ProfilePlaceholderScreenPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ProfilePlaceholderScreen()
        }
    }
}
