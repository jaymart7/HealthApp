package ph.mart.healthapp.feature.food.ui

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

/** Stands in for the real capture flow (Phase 5) — demonstrates the FAB's "Log food" action
 * routing to a real destination, per BUILD_PLAN's Phase 2c. */
@Composable
fun FoodCaptureStubScreen(onClose: () -> Unit) {
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Thinking, size = 64.dp) },
        heading = "Photo food logging",
        body = "Real camera capture + AI analysis arrives in Phase 5.",
        actions = {
            SecondaryButton(label = "Back", onClick = onClose)
        },
    )
}

@PreviewLightDark
@Composable
private fun FoodCaptureStubScreenPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            FoodCaptureStubScreen(onClose = {})
        }
    }
}
