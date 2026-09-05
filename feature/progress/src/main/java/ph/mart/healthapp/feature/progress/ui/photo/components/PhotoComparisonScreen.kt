package ph.mart.healthapp.feature.progress.ui.photo.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlin.math.roundToInt
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.R

/**
 * Before/after slider: [photoA] (the older shot) fills the frame, [photoB] is drawn over it clipped
 * to the right of a draggable divider.
 */
@Composable
fun PhotoComparisonScreen(photoA: ProgressPhoto, photoB: ProgressPhoto, unit: UnitSystem, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var sharing by rememberSaveable { mutableStateOf(false) }

    // A full-screen overlay, not a route: back has to clear the selection rather than leave the
    // Progress tab entirely.
    val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(state = navigationState, onBackCompleted = onClose)

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = stringResource(R.string.progress_compare_title), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            ComparisonSlider(photoA = photoA, photoB = photoB)
            val delta = photoB.weightKg?.let { b -> photoA.weightKg?.let { a -> b - a } }
            if (delta != null) {
                Text(
                    text = stringResource(
                        R.string.progress_weight_value,
                        "${if (delta > 0) "+" else ""}${"%.1f".format(delta.kgToDisplayUnit(unit))}",
                        unit.weightUnitLabel(),
                    ),
                    style = MaterialTheme.typography.titleMedium.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(label = stringResource(R.string.progress_share), onClick = { sharing = true }, modifier = Modifier.weight(1f))
                SecondaryButton(label = stringResource(R.string.progress_close), onClick = onClose, modifier = Modifier.weight(1f))
            }
        }
    }

    if (sharing) {
        // A before/after is a two-frame strip, so it shares through the same sheet the timelapse does.
        SharePhotoStripSheet(photos = listOf(photoA, photoB), unit = unit, onDismiss = { sharing = false })
    }
}

@Composable
private fun ComparisonSlider(photoA: ProgressPhoto, photoB: ProgressPhoto, modifier: Modifier = Modifier) {
    val bitmapA = rememberBitmapFromFile(photoA.filePath)
    val bitmapB = rememberBitmapFromFile(photoB.filePath)
    var fraction by remember { mutableFloatStateOf(0.5f) }
    var widthPx by remember { mutableFloatStateOf(0f) }
    val labelA = formatEpochDay(photoA.dateEpochDay)
    val labelB = formatEpochDay(photoB.dateEpochDay)
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
                    fraction = (fraction + drag / size.width).coerceIn(0f, 1f)
                }
            }
            // Drag-only would leave this unreachable with a screen reader or a switch device.
            .semantics {
                contentDescription = spoken
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                setProgress { target -> fraction = target.coerceIn(0f, 1f); true }
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
                // `fraction` is read here and in the divider's offset lambda only — dragging
                // repaints without recomposing either Image.
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        clipRect(left = size.width * fraction) { this@drawWithContent.drawContent() }
                    },
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset((widthPx * fraction).roundToInt(), 0) }
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
                    val half = 16.dp.roundToPx()
                    val max = (widthPx.roundToInt() - 2 * half).coerceAtLeast(0)
                    IntOffset(((widthPx * fraction).roundToInt() - half).coerceIn(0, max), 0)
                }
                .size(32.dp),
        ) {
            Icon(imageVector = AppIcons.Compare, contentDescription = null, modifier = Modifier.padding(6.dp))
        }

        DateLabel(labelA, Modifier.align(Alignment.BottomStart).padding(8.dp))
        DateLabel(labelB, Modifier.align(Alignment.BottomEnd).padding(8.dp))
    }
}

@Composable
private fun DateLabel(text: String, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun PhotoComparisonScreenPreview() {
    AppTheme {
        PhotoComparisonScreen(
            photoA = ProgressPhoto(id = 1, dateEpochDay = 0, filePath = "", weightKg = 80.0),
            photoB = ProgressPhoto(id = 2, dateEpochDay = 30, filePath = "", weightKg = 77.5),
            unit = UnitSystem.Metric,
            onClose = {},
        )
    }
}
