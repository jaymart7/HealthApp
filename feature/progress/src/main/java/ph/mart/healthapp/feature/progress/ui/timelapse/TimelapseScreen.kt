package ph.mart.healthapp.feature.progress.ui.timelapse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.shared.components.PhotoOverlayStage
import ph.mart.healthapp.feature.progress.ui.shared.components.SharePhotoStripSheet
import ph.mart.healthapp.feature.progress.ui.timelapse.components.TimelapseFrame
import ph.mart.healthapp.feature.progress.ui.timelapse.components.TimelapseTimeline

/** The one emphasised control on the screen. Everything else here is a setting. */
private val TransportButtonSize = 56.dp

/**
 * Every progress photo played in date order — the whole-set answer to `PhotoComparisonScreen`'s
 * two-photo one.
 *
 * A route rather than an overlay drawn over the Progress tab, so it wears the toolbar's back arrow
 * and none of the tab's chrome, and wires no back handler of its own. Playback loops rather than
 * stopping at the end: a run of ten photos is
 * a few seconds long, and stopping would need a restart control for a gesture the loop already
 * gives away for free.
 */
@Composable
internal fun TimelapseScreen(
    modifier: Modifier = Modifier,
    viewModel: TimelapseViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    if (!uiState.playable) return
    TimelapseContent(photos = uiState.photos, unit = uiState.unit, modifier = modifier)
}

@Composable
private fun TimelapseContent(
    photos: List<ProgressPhoto>,
    unit: UnitSystem,
    modifier: Modifier = Modifier,
    state: TimelapseState = rememberTimelapseState(),
) {
    if (photos.isEmpty()) return

    LaunchedEffect(state.playing, state.speed, photos.size) {
        if (!state.playing) return@LaunchedEffect
        while (true) {
            delay(frameIntervalMillis(state.speed))
            state.index = (state.index + 1) % photos.size
        }
    }

    val index = state.index.coerceIn(photos.indices)
    val current = photos[index]

    PhotoOverlayStage(onShare = { state.sharing = true }, modifier = modifier) {
        TimelapseFrame(
            photo = current,
            unit = unit,
            speed = state.speed,
            // See `PhotoOverlayStage`: the picture yields to the controls, not the other way round.
            modifier = Modifier.weight(1f, fill = false),
        )

        TimelapseTimeline(
            photos = photos,
            index = index,
            onScrubTo = { frame -> state.scrubTo(frame, photos.size) },
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlayButton(playing = state.playing, onClick = { state.playing = !state.playing })
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.progress_timelapse_frame_of, index + 1, photos.size),
                    style = MaterialTheme.typography.titleSmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        if (state.playing) {
                            R.string.progress_timelapse_status_playing
                        } else {
                            R.string.progress_timelapse_status_paused
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SpeedChip(speed = state.speed, onClick = { state.cycleSpeed() })
        }
    }

    if (state.sharing) {
        SharePhotoStripSheet(photos = photos, unit = unit, onDismiss = { state.sharing = false })
    }
}

@Composable
private fun PlayButton(playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 2.dp,
        modifier = modifier.size(TransportButtonSize),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (playing) AppIcons.Pause else AppIcons.Play,
                contentDescription = stringResource(
                    if (playing) R.string.progress_timelapse_pause else R.string.progress_timelapse_play,
                ),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/**
 * Demoted on purpose. It used to be a full-width segmented toggle sharing a row with the play
 * button, which gave "how fast does this run" the same weight as "does it run" — a choice made
 * once at the same size as the control used every time.
 */
@Composable
private fun SpeedChip(speed: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(
        when (speed) {
            0 -> R.string.progress_timelapse_slow
            2 -> R.string.progress_timelapse_fast
            else -> R.string.progress_timelapse_normal
        },
    )
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.heightIn(min = 32.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(imageVector = AppIcons.Timer, contentDescription = null, modifier = Modifier.size(12.dp))
            Text(
                text = stringResource(R.string.progress_timelapse_speed, label, TIMELAPSE_FPS[speed]),
                style = MaterialTheme.typography.labelSmall.tabularNums,
            )
        }
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
        )
    }
}
