package ph.mart.healthapp.feature.home.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** Stands in for the real dashboard until Phase 7. */
@Composable
fun HomePlaceholderScreen() {
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Idle, size = 64.dp) },
        heading = "Home",
        body = "The real dashboard arrives in Phase 7.",
    )
}

@PreviewLightDark
@Composable
private fun HomePlaceholderScreenPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            HomePlaceholderScreen()
        }
    }
}
