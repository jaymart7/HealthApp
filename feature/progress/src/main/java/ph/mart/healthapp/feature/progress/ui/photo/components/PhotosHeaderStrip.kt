package ph.mart.healthapp.feature.progress.ui.photo.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.formatDayMonth
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.timelapse.MIN_TIMELAPSE_PHOTOS

/**
 * The count, the span it covers, and the way into the player — one 16dp row where a `HeroValue`
 * and a full-width button used to take the top third of the page between them. A photo grid is
 * the content; the number of shots in it is a caption, not a headline.
 *
 * The button appears at **two** photos, the floor the comparison slider has — one control
 * appearing without the other reads as a bug. At one photo the strip still renders: the count and
 * that single date are worth saying, and the row closing up is what says the player isn't ready.
 */
@Composable
internal fun PhotosHeaderStrip(
    photos: List<ProgressPhoto>,
    onOpenTimelapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (photos.isEmpty()) return
    val first = photos.minOf { it.dateEpochDay }
    val last = photos.maxOf { it.dateEpochDay }
    val days = (last - first).toInt()

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pluralStringResource(R.plurals.progress_photos_count, photos.size, photos.size),
                    style = MaterialTheme.typography.titleLarge.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.progress_photos_span,
                        pluralStringResource(R.plurals.progress_strip_days, days, days),
                        formatDayMonth(first),
                        formatDayMonth(last),
                    ),
                    style = MaterialTheme.typography.labelSmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (photos.size >= MIN_TIMELAPSE_PHOTOS) {
                TimelapsePill(onClick = onOpenTimelapse)
            }
        }
    }
}

/**
 * Not [ph.mart.healthapp.core.designsystem.component.TonalButton]: that one is a 48dp
 * `secondaryContainer` pill for a step in a flow, and this is a 40dp accent chip riding a card's
 * own row beside two lines of text. Giving the shared button an icon slot and a second colour
 * scheme to serve one header would make every other caller carry the branch.
 */
@Composable
private fun TimelapsePill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.heightIn(min = 40.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(imageVector = AppIcons.Play, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
                text = stringResource(R.string.progress_photos_timelapse_short),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/**
 * The numbered badge on a picked tile, and the same badge inside the hint bar — one component so
 * the thing the hint names and the thing the grid draws are visibly the same thing.
 */
@Composable
internal fun SelectionBadge(
    number: Int,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Surface(shape = CircleShape, color = color, contentColor = contentColor, shadowElevation = 1.dp, modifier = modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "$number",
                style = MaterialTheme.typography.labelMedium.tabularNums,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PhotosHeaderStripPreview() {
    val today = todayEpochDay()
    AppTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PhotosHeaderStrip(
                    photos = listOf(
                        ProgressPhoto(id = 1, dateEpochDay = today - 92, filePath = ""),
                        ProgressPhoto(id = 2, dateEpochDay = today, filePath = ""),
                    ),
                    onOpenTimelapse = {},
                )
                // One shot: the count and its date still say something; the player does not exist yet.
                PhotosHeaderStrip(
                    photos = listOf(ProgressPhoto(id = 1, dateEpochDay = today, filePath = "")),
                    onOpenTimelapse = {},
                )
            }
        }
    }
}
