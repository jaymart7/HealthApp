package ph.mart.healthapp.feature.progress.ui.timelapse.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
import ph.mart.healthapp.feature.progress.ui.timelapse.fadeMillis

/**
 * One frame with its date and weight, crossfading into the next.
 *
 * **Hold-under, not cross-dissolve.** The outgoing shot stays at full opacity underneath while the
 * incoming one fades in above it; the two are never both translucent, so the stage colour never
 * shows between them. Fading both at once is what makes a player strobe — at eight frames a second
 * the flash between shots reads as the photos flickering rather than as one changing into another.
 * The layer underneath is fully covered by the time the fade ends, so keeping it costs a draw and
 * nothing else.
 *
 * The last decoded bitmap is still held across the swap for the other reason:
 * [rememberBitmapFromFile] re-keys on the path and reports null while the next decode is in
 * flight, which would otherwise blank the frame between every pair.
 */
@Composable
internal fun TimelapseFrame(
    photo: ProgressPhoto,
    unit: UnitSystem,
    speed: Int,
    modifier: Modifier = Modifier,
) {
    val decoded = rememberBitmapFromFile(photo.filePath)
    var shown by remember { mutableStateOf<ImageBitmap?>(null) }
    var previous by remember { mutableStateOf<ImageBitmap?>(null) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(decoded) {
        val next = decoded ?: return@LaunchedEffect
        previous = shown
        shown = next
        alpha.snapTo(0f)
        alpha.animateTo(1f, tween(durationMillis = fadeMillis(speed), easing = LinearEasing))
        // Fully covered by now, so the layer underneath can go rather than being drawn for the
        // rest of the run.
        previous = null
    }
    val date = formatEpochDay(photo.dateEpochDay)
    val spoken = stringResource(R.string.progress_photo_from, date)

    Box(
        modifier = modifier
            // Height-first — see `ComparisonSlider`.
            .aspectRatio(0.75f, matchHeightConstraintsFirst = true)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .semantics { contentDescription = spoken },
    ) {
        previous?.let {
            Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        shown?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha.value },
            )
        }
        PhotoOverlayLabel(text = date, modifier = Modifier.align(Alignment.BottomStart).padding(12.dp))
        photo.weightKg?.let { kg ->
            PhotoOverlayLabel(
                text = stringResource(R.string.progress_weight_value, "%.1f".format(kg.kgToDisplayUnit(unit)), unit.weightUnitLabel()),
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
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
                speed = 1,
            )
        }
    }
}
