package ph.mart.healthapp.feature.progress.ui.shared.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.GRID_TILE_PX
import ph.mart.healthapp.core.designsystem.component.ShareImageSheet
import ph.mart.healthapp.core.designsystem.component.formatDayMonth
import ph.mart.healthapp.core.designsystem.component.rememberBitmapFromFile
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.R

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
 * The strip a **comparison** shares: the two shots that were picked, plus whatever was logged
 * nearest the thirds of the interval between them.
 *
 * A before and after used to share as literally two frames. Two portraits side by side make a thin
 * picture and a thinner story — the shots in between are the part that says the change was gradual
 * rather than a trick of the light, and they cost nothing, since they are already on the device.
 * The picked pair still bounds the strip: this fills the middle, it never moves the ends.
 *
 * Sampling is by **date**, not by index, so an unevenly logged month doesn't hand the middle two
 * frames to whichever week was photographed most.
 */
internal fun sampleBetween(
    photos: List<ProgressPhoto>,
    older: ProgressPhoto,
    newer: ProgressPhoto,
    max: Int = STRIP_FRAMES,
): List<ProgressPhoto> {
    val n = max.coerceAtLeast(2)
    val between = photos.filter { it.dateEpochDay > older.dateEpochDay && it.dateEpochDay < newer.dateEpochDay }
    if (between.isEmpty()) return listOf(older, newer)
    val span = newer.dateEpochDay - older.dateEpochDay
    val middle = (1 until n - 1)
        .map { i -> older.dateEpochDay + span * i / (n - 1) }
        .mapNotNull { target -> between.minByOrNull { abs(it.dateEpochDay - target) } }
        .distinctBy { it.id }
    return (listOf(older) + middle + newer).sortedBy { it.dateEpochDay }
}

/**
 * Preview-then-share for a run of progress photos: the sheet shows exactly the PNG that leaves the
 * app, [ShareRecapSheet][ph.mart.healthapp.feature.progress.ui.recap.components.ShareRecapSheet]'s
 * contract. One sheet serves all three photo shares — the comparison hands it a strip spanning its
 * pair, the timelapse and the Photos page hand it the whole set — because a second near-identical
 * sheet would be the thing to avoid.
 *
 * The frames sit on a card rather than loose on the sheet's own ground, so what the reader is
 * judging is bounded: everything inside that card is the picture, and everything outside it is the
 * app. The ground, the brand footer and the Share and Save buttons are [ShareImageSheet]'s.
 */
@Composable
internal fun SharePhotoStripSheet(photos: List<ProgressPhoto>, unit: UnitSystem, onDismiss: () -> Unit) {
    val frames = remember(photos) { sampleFrames(photos) }

    ShareImageSheet(fileName = "fitpulse-progress.png", onDismiss = onDismiss) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    frames.forEach { photo ->
                        StripFrame(photo = photo, modifier = Modifier.weight(1f))
                    }
                }
                Column {
                    Text(
                        text = stripHeadline(photos, unit),
                        style = MaterialTheme.typography.titleLarge.tabularNums,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            R.string.progress_strip_range,
                            formatDayMonth(photos.first().dateEpochDay),
                            formatDayMonth(photos.last().dateEpochDay),
                        ),
                        style = MaterialTheme.typography.labelSmall.tabularNums,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.progress_strip_caption),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
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
            text = formatDayMonth(photo.dateEpochDay),
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
