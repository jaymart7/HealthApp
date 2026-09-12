package ph.mart.healthapp.feature.progress.ui.timelapse.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.component.rememberBitmapFromFile
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.shared.components.PhotoOverlayLabel

/**
 * One frame with its date and weight. The last decoded bitmap is held across the swap:
 * [rememberBitmapFromFile] re-keys on the path and reports null while the next decode is in
 * flight, which at eight frames a second would otherwise strobe the frame to empty.
 */
@Composable
internal fun TimelapseFrame(photo: ProgressPhoto, unit: UnitSystem, modifier: Modifier = Modifier) {
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
        PhotoOverlayLabel(text = date, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
        photo.weightKg?.let { kg ->
            PhotoOverlayLabel(
                text = stringResource(R.string.progress_weight_value, "%.1f".format(kg.kgToDisplayUnit(unit)), unit.weightUnitLabel()),
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                tabular = true,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TimelapseFramePreview() {
    AppTheme {
        Surface {
            TimelapseFrame(
                photo = ProgressPhoto(id = 1, dateEpochDay = todayEpochDay(), filePath = "", weightKg = 76.9),
                unit = UnitSystem.Metric,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
