package ph.mart.healthapp.feature.progress.ui.photo.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreenState
import ph.mart.healthapp.feature.progress.ui.progress.ProgressUiState

@Composable
internal fun PhotosTabContent(uiState: ProgressUiState, state: ProgressScreenState) {
    if (uiState.photos.isEmpty()) {
        FullScreenState(
            icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
            heading = "No progress photos yet",
            body = "Add your first photo from the FAB to start tracking changes over time.",
        )
        return
    }
    Column(modifier = Modifier.fillMaxSize()) {
        // Two photos is the least that plays as a sequence, and it is also the point at which
        // the comparison slider becomes reachable — one control appearing without the other
        // would read as a bug.
        if (uiState.photos.size >= 2) {
            SecondaryButton(
                label = "Play timelapse",
                onClick = state::openTimelapse,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
        }
        ProgressPhotoGrid(
            photos = uiState.photos,
            selectedIds = state.selectedPhotoIds,
            onToggleSelect = state::togglePhotoSelection,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@PreviewLightDark
@Composable
private fun PhotosTabPreview() {
    val today = todayEpochDay()
    AppTheme {
        PhotosTabContent(
            uiState = ProgressUiState(
                photos = listOf(
                    ProgressPhoto(id = 1, dateEpochDay = today - 30, filePath = "", weightKg = 79.4),
                    ProgressPhoto(id = 2, dateEpochDay = today, filePath = "", weightKg = 76.9),
                ),
            ),
            state = ProgressScreenState(),
        )
    }
}

@PreviewLightDark
@Composable
private fun PhotosTabEmptyPreview() {
    AppTheme { PhotosTabContent(uiState = ProgressUiState(), state = ProgressScreenState()) }
}
