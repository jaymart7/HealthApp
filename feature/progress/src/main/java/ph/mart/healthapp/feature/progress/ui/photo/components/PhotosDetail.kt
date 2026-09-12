package ph.mart.healthapp.feature.progress.ui.photo.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreenState
import ph.mart.healthapp.feature.progress.ui.progress.ProgressUiState

/**
 * Scrolls itself — a `LazyVerticalGrid` cannot be nested in a `verticalScroll` column, which is why
 * `SubjectDetail` names this one in `SelfScrolling`. The empty state is the page's, not this
 * body's.
 *
 * Nothing but the grid: the count, the span and the way into the player are the grid's own header
 * items now, so the page has one scroller rather than a fixed column sitting on top of one. That
 * is what lets the month headers pin.
 */
@Composable
internal fun PhotosDetailBody(uiState: ProgressUiState, state: ProgressScreenState) {
    ProgressPhotoGrid(
        photos = uiState.photos,
        selectedIds = state.selectedPhotoIds,
        onToggleSelect = state::togglePhotoSelection,
        onClearSelection = state::clearPhotoSelection,
        onOpenTimelapse = state::openTimelapse,
        modifier = Modifier.fillMaxSize(),
    )
}

@PreviewLightDark
@Composable
private fun PhotosDetailPreview() {
    val today = todayEpochDay()
    AppTheme {
        PhotosDetailBody(
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
