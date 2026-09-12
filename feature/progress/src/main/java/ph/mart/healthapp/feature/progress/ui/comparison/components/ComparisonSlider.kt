package ph.mart.healthapp.feature.progress.ui.comparison.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.component.rememberBitmapFromFile
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.comparison.ComparisonPair
import ph.mart.healthapp.feature.progress.ui.comparison.ComparisonState
import ph.mart.healthapp.feature.progress.ui.comparison.rememberComparisonState
import ph.mart.healthapp.feature.progress.ui.shared.components.PhotoOverlayLabel

/** Half the handle, in dp — it is clamped by this so it stays fully inside the clipped frame. */
private val HandleRadius = 16.dp

/**
 * Before/after: the older shot fills the frame and the newer one is drawn over it, clipped to the
 * right of a draggable divider.
 */
@Composable
internal fun ComparisonSlider(
    pair: ComparisonPair,
    state: ComparisonState,
    modifier: Modifier = Modifier,
) {
    val bitmapA = rememberBitmapFromFile(pair.older.filePath)
    val bitmapB = rememberBitmapFromFile(pair.newer.filePath)
    var widthPx by remember { mutableFloatStateOf(0f) }
    val labelA = formatEpochDay(pair.older.dateEpochDay)
    val labelB = formatEpochDay(pair.newer.dateEpochDay)
    val spoken = stringResource(R.string.progress_compare_spoken, labelA, labelB)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, drag ->
                    change.consume()
                    state.dividerFraction = (state.dividerFraction + drag / size.width).coerceIn(0f, 1f)
                }
            }
            // Drag-only would leave this unreachable with a screen reader or a switch device.
            .semantics {
                contentDescription = spoken
                progressBarRangeInfo = ProgressBarRangeInfo(state.dividerFraction, 0f..1f)
                setProgress { target -> state.dividerFraction = target.coerceIn(0f, 1f); true }
            },
    ) {
        bitmapA?.let {
            Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        bitmapB?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                // `dividerFraction` is read here and in the two offset lambdas below only —
                // dragging repaints without recomposing either Image.
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        clipRect(left = size.width * state.dividerFraction) { this@drawWithContent.drawContent() }
                    },
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset((widthPx * state.dividerFraction).roundToInt(), 0) }
                .fillMaxHeight()
                .width(2.dp)
                .background(MaterialTheme.colorScheme.surface),
        )
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset {
                    // Clamped so the handle stays fully inside the clipped frame at either extreme;
                    // the divider line itself is allowed to run off the edge.
                    val half = HandleRadius.roundToPx()
                    val max = (widthPx.roundToInt() - 2 * half).coerceAtLeast(0)
                    IntOffset(((widthPx * state.dividerFraction).roundToInt() - half).coerceIn(0, max), 0)
                }
                .size(32.dp),
        ) {
            Icon(imageVector = AppIcons.Compare, contentDescription = null, modifier = Modifier.padding(6.dp))
        }

        PhotoOverlayLabel(text = labelA, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
        PhotoOverlayLabel(text = labelB, modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp))
    }
}

@PreviewLightDark
@Composable
private fun ComparisonSliderPreview() {
    AppTheme {
        Surface {
            ComparisonSlider(
                pair = ComparisonPair(
                    older = ProgressPhoto(id = 1, dateEpochDay = 0, filePath = "", weightKg = 80.0),
                    newer = ProgressPhoto(id = 2, dateEpochDay = 30, filePath = "", weightKg = 77.5),
                ),
                state = rememberComparisonState(),
            )
        }
    }
}
