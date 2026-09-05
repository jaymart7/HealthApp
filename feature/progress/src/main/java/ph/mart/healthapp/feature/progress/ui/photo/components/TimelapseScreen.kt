package ph.mart.healthapp.feature.progress.ui.photo.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.R

/** The three playback speeds, in frames per second. */
private val TIMELAPSE_FPS = listOf(2, 4, 8)

/**
 * Every progress photo played in date order — the whole-set answer to [PhotoComparisonScreen]'s
 * two-photo one. It reads [photos] straight off the Progress screen's already-combined state and
 * derives everything else, so it needs no ViewModel and no schema: a timelapse is a way of looking
 * at the grid, not a thing to store.
 *
 * Playback loops rather than stopping at the end. A run of ten photos is a few seconds long, and
 * stopping would need a restart control for a gesture the loop already gives away for free.
 */
@Composable
internal fun TimelapseScreen(
    photos: List<ProgressPhoto>,
    unit: UnitSystem,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (photos.isEmpty()) return

    var index by rememberSaveable { mutableIntStateOf(0) }
    var playing by rememberSaveable { mutableStateOf(true) }
    var speed by rememberSaveable { mutableIntStateOf(1) }
    var sharing by rememberSaveable { mutableStateOf(false) }

    // A full-screen overlay, not a route: back has to close it rather than leave the Progress tab.
    val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(state = navigationState, onBackCompleted = onClose)

    LaunchedEffect(playing, speed, photos.size) {
        if (!playing) return@LaunchedEffect
        while (true) {
            delay(1000L / TIMELAPSE_FPS[speed])
            index = (index + 1) % photos.size
        }
    }

    val current = photos[index.coerceIn(photos.indices)]
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = stringResource(R.string.progress_timelapse_title), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)

            TimelapseFrame(photo = current, unit = unit)

            if (photos.size > 1) {
                Slider(
                    value = index.toFloat(),
                    onValueChange = { value ->
                        // Scrubbing takes over: a slider that kept advancing under the finger
                        // would fight whoever is looking for one particular week.
                        playing = false
                        index = value.roundToInt().coerceIn(photos.indices)
                    },
                    valueRange = 0f..(photos.size - 1).toFloat(),
                    steps = (photos.size - 2).coerceAtLeast(0),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { playing = !playing }) {
                    Icon(
                        imageVector = if (playing) AppIcons.Pause else AppIcons.Play,
                        contentDescription = stringResource(if (playing) R.string.progress_timelapse_pause else R.string.progress_timelapse_play),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                SegmentedToggle(
                    options = listOf(
                        stringResource(R.string.progress_timelapse_slow),
                        stringResource(R.string.progress_timelapse_normal),
                        stringResource(R.string.progress_timelapse_fast),
                    ),
                    selectedIndex = speed,
                    onSelect = { speed = it },
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(label = stringResource(R.string.progress_share), onClick = { sharing = true }, modifier = Modifier.weight(1f))
                SecondaryButton(label = stringResource(R.string.progress_close), onClick = onClose, modifier = Modifier.weight(1f))
            }
        }
    }

    if (sharing) {
        SharePhotoStripSheet(photos = photos, unit = unit, onDismiss = { sharing = false })
    }
}

/**
 * One frame with its date and weight. The last decoded bitmap is held across the swap:
 * [rememberBitmapFromFile] re-keys on the path and reports null while the next decode is in
 * flight, which at eight frames a second would otherwise strobe the frame to empty.
 */
@Composable
private fun TimelapseFrame(photo: ProgressPhoto, unit: UnitSystem, modifier: Modifier = Modifier) {
    val decoded = rememberBitmapFromFile(photo.filePath)
    var lastFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(decoded) { decoded?.let { lastFrame = it } }
    val frame = decoded ?: lastFrame
    val date = formatEpochDay(photo.dateEpochDay)
    val spoken = stringResource(R.string.progress_photo_from, date)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .semantics { contentDescription = spoken },
    ) {
        frame?.let {
            Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        FrameLabel(text = date, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
        photo.weightKg?.let { kg ->
            FrameLabel(
                text = stringResource(R.string.progress_weight_value, "%.1f".format(kg.kgToDisplayUnit(unit)), unit.weightUnitLabel()),
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                tabular = true,
            )
        }
    }
}

@Composable
private fun FrameLabel(text: String, modifier: Modifier = Modifier, tabular: Boolean = false) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Text(
            text = text,
            style = if (tabular) MaterialTheme.typography.labelMedium.tabularNums else MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun TimelapseScreenPreview() {
    val today = todayEpochDay()
    AppTheme {
        TimelapseScreen(
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
