package ph.mart.healthapp.feature.progress.ui.comparison.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.component.rememberBitmapFromFile
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.comparison.ComparisonPair
import ph.mart.healthapp.feature.progress.ui.shared.components.PhotoOverlayLabel

/**
 * The same pair, both shots whole. A wipe is the better read on a small change in one place; two
 * halves are the better read on a shape — a posture, a waist, an outline is a thing the divider
 * is always covering half of.
 *
 * The halves keep their bottom-outer labels, so the older shot's date stays on the outside edge of
 * the pair in either framing.
 */
@Composable
internal fun ComparisonSideBySide(pair: ComparisonPair, modifier: Modifier = Modifier) {
    val labelA = formatEpochDay(pair.older.dateEpochDay)
    val labelB = formatEpochDay(pair.newer.dateEpochDay)
    val spoken = stringResource(R.string.progress_compare_between, labelA, labelB)

    Row(
        modifier = modifier
            // Two 3:4 halves side by side are one 3:2 block, sized height-first for the same
            // reason the slider is.
            .aspectRatio(1.5f, matchHeightConstraintsFirst = true)
            .semantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Half(photo = pair.older) {
            PhotoOverlayLabel(text = labelA, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
        }
        Half(photo = pair.newer) {
            PhotoOverlayLabel(text = labelB, modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp))
        }
    }
}

@Composable
private fun RowScope.Half(photo: ProgressPhoto, label: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        rememberBitmapFromFile(photo.filePath)?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        label()
    }
}

@PreviewLightDark
@Composable
private fun ComparisonSideBySidePreview() {
    AppTheme {
        Surface {
            ComparisonSideBySide(
                pair = ComparisonPair(
                    older = ProgressPhoto(id = 1, dateEpochDay = 0, filePath = "", weightKg = 80.0),
                    newer = ProgressPhoto(id = 2, dateEpochDay = 30, filePath = "", weightKg = 77.5),
                ),
            )
        }
    }
}
