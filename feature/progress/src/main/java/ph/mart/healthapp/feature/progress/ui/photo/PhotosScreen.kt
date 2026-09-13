package ph.mart.healthapp.feature.progress.ui.photo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.AppTopBar
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.photo.components.ProgressPhotoGrid
import ph.mart.healthapp.feature.progress.ui.shared.components.SharePhotoStripSheet

/**
 * The whole progress-photo set: a 3:4 grid under pinned month headers, the count and the span in a
 * header strip above it, and the two things the set is read for hanging off it — a comparison from
 * any two tiles, and the player from the strip.
 *
 * **A route, not a subject page.** The other thirteen subjects are charts and stat rows that
 * `SubjectDetail` swaps in behind the tab's own chrome; this one is a full-bleed grid whose only
 * job is to launch two other routes, and it had already been forced out of that page's scrolling
 * column (the since-retired `SelfScrolling` exemption) because a `LazyVerticalGrid` cannot nest
 * in one. So it owns its data
 * ([PhotosViewModel]), its selection ([PhotosState]) and its toolbar, and it draws the full width
 * of the window with no bottom bar and no FAB over it. See `DECISIONS.md` →
 * **Progress photos & timelapse**.
 */
@Composable
internal fun PhotosScreen(
    onCompare: (Long, Long) -> Unit,
    onOpenTimelapse: () -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PhotosViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    PhotosContent(
        photos = uiState.photos,
        unit = uiState.unit,
        onCompare = onCompare,
        onOpenTimelapse = onOpenTimelapse,
        onExitFlow = onExitFlow,
        modifier = modifier,
    )
}

@Composable
private fun PhotosContent(
    photos: List<ProgressPhoto>,
    unit: UnitSystem,
    onCompare: (Long, Long) -> Unit,
    onOpenTimelapse: () -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    state: PhotosState = rememberPhotosState(),
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Its own bar rather than `AppScaffold`'s, for the share: that one is built from a
            // `NavKey` and cannot reach the photo set this sheet needs. The scaffold stands down
            // for this route the way it does for the camera flows.
            //
            // Zero insets, unlike every other caller: this bar sits in the scaffold's *content*,
            // which its `innerPadding` has already cleared of the status bar. The default would
            // apply that inset a second time and leave a bar-height gap above the toolbar.
            AppTopBar(
                title = stringResource(R.string.progress_subject_photos),
                onBack = onExitFlow,
                windowInsets = WindowInsets(0),
                actions = {
                    if (photos.isNotEmpty()) {
                        IconButton(onClick = { state.sharing = true }) {
                            Icon(
                                imageVector = AppIcons.Share,
                                contentDescription = stringResource(R.string.progress_share),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
            )
            if (photos.isEmpty()) {
                // The page's, not the grid's — a grid with nothing in it has nothing to say.
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    FullScreenState(
                        icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
                        heading = stringResource(R.string.progress_empty_photos_heading),
                        body = stringResource(R.string.progress_empty_photos_body),
                    )
                }
            } else {
                ProgressPhotoGrid(
                    photos = photos,
                    selectedIds = state.selectedIds,
                    // The second pick *is* the gesture that opens a comparison. The selection is
                    // cleared on the way out because `PhotoSelectionHint` draws nothing at two
                    // picks, so a pair left standing on the grid behind the comparison would have
                    // no way back out of itself.
                    onToggleSelect = { id ->
                        state.toggle(id)
                        val picked = state.selectedIds
                        if (picked.size == COMPARISON_PICKS) {
                            state.clear()
                            onCompare(picked[0], picked[1])
                        }
                    },
                    onClearSelection = state::clear,
                    onOpenTimelapse = onOpenTimelapse,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                )
            }
        }
    }

    if (state.sharing && photos.isNotEmpty()) {
        SharePhotoStripSheet(
            photos = photos,
            unit = unit,
            onDismiss = { state.sharing = false },
        )
    }
}

@PreviewLightDark
@Composable
private fun PhotosScreenPreview() {
    val today = todayEpochDay()
    AppTheme {
        PhotosContent(
            photos = listOf(
                ProgressPhoto(id = 1, dateEpochDay = today, filePath = "", weightKg = 76.9),
                ProgressPhoto(id = 2, dateEpochDay = today - 30, filePath = "", weightKg = 78.4),
                ProgressPhoto(id = 3, dateEpochDay = today - 92, filePath = "", weightKg = 79.4),
            ),
            unit = UnitSystem.Metric,
            onCompare = { _, _ -> },
            onOpenTimelapse = {},
            onExitFlow = {},
        )
    }
}

/** Nothing shot yet — the page says what it is for, and the toolbar loses its share. */
@PreviewLightDark
@Composable
private fun PhotosScreenEmptyPreview() {
    AppTheme {
        PhotosContent(
            photos = emptyList(),
            unit = UnitSystem.Metric,
            onCompare = { _, _ -> },
            onOpenTimelapse = {},
            onExitFlow = {},
        )
    }
}
