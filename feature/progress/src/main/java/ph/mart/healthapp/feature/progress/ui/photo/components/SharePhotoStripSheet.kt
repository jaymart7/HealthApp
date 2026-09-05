package ph.mart.healthapp.feature.progress.ui.photo.components

import android.graphics.Picture
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.shared.captureToPicture
import ph.mart.healthapp.feature.progress.ui.shared.sharePng

/** Frames on the shared strip. Four is what stays legible across a phone's width. */
private const val STRIP_FRAMES = 4

/**
 * Up to [max] frames spread evenly across [photos], the first and the last always among them —
 * a strip whose ends aren't the start and the end of the run is not the story the user is telling.
 * Pure, so the sampling is the one part of this file a JVM test can reach.
 */
internal fun sampleFrames(photos: List<ProgressPhoto>, max: Int = STRIP_FRAMES): List<ProgressPhoto> {
    val n = max.coerceAtLeast(2)
    if (photos.size <= n) return photos
    val last = photos.size - 1
    return (0 until n).map { i -> photos[(i.toLong() * last / (n - 1)).toInt()] }
}

/**
 * Preview-then-share for a run of progress photos: the sheet shows exactly the PNG that leaves the
 * app, [ShareRecapSheet]'s contract. One sheet serves both photo shares — the comparison slider
 * hands it its two photos and the timelapse hands it the whole set, because a before/after *is* a
 * two-frame strip and a second near-identical sheet would be the thing to avoid.
 *
 * No `NavigationEventHandler`: [AppBottomSheet] delegates to `ModalBottomSheet`, which already
 * takes back, and this is a leaf with no sub-level of its own.
 */
@Composable
internal fun SharePhotoStripSheet(photos: List<ProgressPhoto>, unit: UnitSystem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picture = remember { Picture() }
    val frames = remember(photos) { sampleFrames(photos) }

    AppBottomSheet(onDismiss = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .captureToPicture(picture)
                // The opaque ground a shared image needs — a captured layer is transparent
                // wherever nothing painted.
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = stripHeadline(photos, unit),
                style = MaterialTheme.typography.titleMedium.tabularNums,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                frames.forEach { photo ->
                    StripFrame(photo = photo, modifier = Modifier.weight(1f))
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                MascotAvatar(state = MascotState.Happy, size = 24.dp)
                Text(
                    text = stringResource(R.string.progress_strip_brand),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        PrimaryButton(
            label = stringResource(R.string.progress_share),
            onClick = {
                scope.launch {
                    // Zero until the sheet has drawn a frame — a tap that fast would otherwise
                    // hand the chooser an empty file.
                    if (picture.width > 0) {
                        sharePng(context, picture, "fitpulse-progress.png")
                        onDismiss()
                    }
                }
            },
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

/** "92 days · −3.2 kg" — the weight half is dropped when either end was logged without one. */
@Composable
private fun stripHeadline(photos: List<ProgressPhoto>, unit: UnitSystem): String {
    val first = photos.first()
    val last = photos.last()
    val days = last.dateEpochDay - first.dateEpochDay
    val delta = last.weightKg?.let { end -> first.weightKg?.let { end - it } }
    val span = pluralStringResource(R.plurals.progress_strip_days, days.toInt(), days)
    return delta?.let {
        val display = it.kgToDisplayUnit(unit)
        stringResource(
            R.string.progress_strip_span,
            span,
            if (display > 0) "+" else "",
            "%.1f".format(display),
            unit.weightUnitLabel(),
        )
    } ?: span
}

@Composable
private fun StripFrame(photo: ProgressPhoto, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            rememberBitmapFromFile(photo.filePath, GRID_TILE_PX)?.let {
                Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Text(
            text = formatEpochDay(photo.dateEpochDay).substringBefore(","),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun SharePhotoStripSheetPreview() {
    val today = todayEpochDay()
    AppTheme {
        SharePhotoStripSheet(
            photos = listOf(
                ProgressPhoto(id = 1, dateEpochDay = today - 92, filePath = "", weightKg = 80.1),
                ProgressPhoto(id = 2, dateEpochDay = today - 60, filePath = "", weightKg = 78.8),
                ProgressPhoto(id = 3, dateEpochDay = today - 30, filePath = "", weightKg = 77.6),
                ProgressPhoto(id = 4, dateEpochDay = today, filePath = "", weightKg = 76.9),
            ),
            unit = UnitSystem.Metric,
            onDismiss = {},
        )
    }
}
