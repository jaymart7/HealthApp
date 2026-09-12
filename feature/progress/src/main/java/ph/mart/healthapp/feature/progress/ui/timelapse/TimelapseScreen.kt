package ph.mart.healthapp.feature.progress.ui.timelapse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.shared.components.SharePhotoStripSheet
import ph.mart.healthapp.feature.progress.ui.timelapse.components.TimelapseFrame

/**
 * Every progress photo played in date order — the whole-set answer to `PhotoComparisonScreen`'s
 * two-photo one.
 *
 * A full-screen overlay inside the Progress tab rather than a route, which is why it wires its own
 * `NavigationBackHandler`. Playback loops rather than stopping at the end: a run of ten photos is
 * a few seconds long, and stopping would need a restart control for a gesture the loop already
 * gives away for free.
 */
@Composable
internal fun TimelapseScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TimelapseViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    if (!uiState.playable) return
    TimelapseContent(photos = uiState.photos, unit = uiState.unit, onClose = onClose, modifier = modifier)
}

@Composable
private fun TimelapseContent(
    photos: List<ProgressPhoto>,
    unit: UnitSystem,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    state: TimelapseState = rememberTimelapseState(),
) {
    if (photos.isEmpty()) return

    // A full-screen overlay, not a route: back has to close it rather than leave the Progress tab.
    val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(state = navigationState, onBackCompleted = onClose)

    LaunchedEffect(state.playing, state.speed, photos.size) {
        if (!state.playing) return@LaunchedEffect
        while (true) {
            delay(1000L / TIMELAPSE_FPS[state.speed])
            state.index = (state.index + 1) % photos.size
        }
    }

    val current = photos[state.index.coerceIn(photos.indices)]
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = stringResource(R.string.progress_timelapse_title), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)

            TimelapseFrame(photo = current, unit = unit)

            if (photos.size > 1) {
                Slider(
                    value = state.index.toFloat(),
                    onValueChange = { value -> state.scrubTo(value.roundToInt(), photos.size) },
                    valueRange = 0f..(photos.size - 1).toFloat(),
                    steps = (photos.size - 2).coerceAtLeast(0),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { state.playing = !state.playing }) {
                    Icon(
                        imageVector = if (state.playing) AppIcons.Pause else AppIcons.Play,
                        contentDescription = stringResource(if (state.playing) R.string.progress_timelapse_pause else R.string.progress_timelapse_play),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                SegmentedToggle(
                    options = listOf(
                        stringResource(R.string.progress_timelapse_slow),
                        stringResource(R.string.progress_timelapse_normal),
                        stringResource(R.string.progress_timelapse_fast),
                    ),
                    selectedIndex = state.speed,
                    onSelect = { state.speed = it },
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(label = stringResource(R.string.progress_share), onClick = { state.sharing = true }, modifier = Modifier.weight(1f))
                SecondaryButton(label = stringResource(R.string.progress_close), onClick = onClose, modifier = Modifier.weight(1f))
            }
        }
    }

    if (state.sharing) {
        SharePhotoStripSheet(photos = photos, unit = unit, onDismiss = { state.sharing = false })
    }
}

@PreviewLightDark
@Composable
private fun TimelapseScreenPreview() {
    val today = todayEpochDay()
    AppTheme {
        TimelapseContent(
            photos = listOf(
                ProgressPhoto(id = 1, dateEpochDay = today - 60, filePath = "", weightKg = 80.0),
                ProgressPhoto(id = 2, dateEpochDay = today - 30, filePath = "", weightKg = 78.4),
                ProgressPhoto(id = 3, dateEpochDay = today, filePath = "", weightKg = 76.9),
            ),
            unit = UnitSystem.Metric,
            onClose = {},
        )
    }
}
