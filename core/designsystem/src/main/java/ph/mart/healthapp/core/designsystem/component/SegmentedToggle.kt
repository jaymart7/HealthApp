package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import kotlin.math.roundToInt

/**
 * The width five pills already leave on a 360dp screen — the floor below which a `labelLarge`
 * label starts clipping. Pills split the track evenly while they clear it and the toggle scrolls
 * once they don't, so every caller with five or fewer options renders exactly as it did before
 * this floor existed.
 */
private val MinPillWidth = 64.dp

/** Track padding, taken off before the split so evenly-sized pills add up to the track exactly. */
private val TrackPadding = 4.dp

/** Pill-track toggle, list-driven (N options), [MaterialTheme.colorScheme.secondaryContainer] selected chip. */
@Composable
fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
) {
    val scroll = rememberScrollState()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val pillWidth = pillWidth(maxWidth, options.size)
        val pillPx = with(LocalDensity.current) { pillWidth.toPx() }
        val padPx = with(LocalDensity.current) { TrackPadding.toPx() }
        val viewportPx = with(LocalDensity.current) { maxWidth.toPx() }
        // Without this a restored selection off the right-hand end opens with no visible chip.
        // scroll.maxValue is a key because it is 0 until the first layout, so the reveal this
        // effect exists for would otherwise clamp to 0 and never happen.
        LaunchedEffect(selectedIndex, pillPx, viewportPx, scroll.maxValue) {
            scrollTargetFor(selectedIndex, pillPx, padPx, viewportPx, scroll.value)?.let { scroll.scrollTo(it) }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(trackColor)
                // Background before the scroll, so the track stays put and only the pills move.
                .horizontalScroll(scroll)
                .padding(TrackPadding),
        ) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                Surface(
                    onClick = { onSelect(index) },
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .width(pillWidth)
                        .height(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        Text(text = option, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
        }
    }
}

private fun pillWidth(available: Dp, count: Int): Dp =
    maxOf((available - TrackPadding * 2) / count.coerceAtLeast(1), MinPillWidth)

/**
 * The offset that brings pill [index] into view, or null when it already is — the minimum move,
 * because sliding a pill that was already visible drags the track out from under the finger that
 * tapped it. Offsets are content coordinates: the track padding sits inside the scroll.
 */
internal fun scrollTargetFor(index: Int, pillPx: Float, padPx: Float, viewportPx: Float, currentPx: Int): Int? {
    val leading = index * pillPx
    val trailing = leading + pillPx + padPx * 2
    return when {
        leading < currentPx -> leading.roundToInt()
        trailing > currentPx + viewportPx -> (trailing - viewportPx).roundToInt()
        else -> null
    }
}

@PreviewLightDark
@Composable
private fun SegmentedTogglePreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                SegmentedToggle(
                    options = listOf("Metric (kg, cm)", "Imperial (lb, in)"),
                    selectedIndex = 0,
                    onSelect = {},
                )
                // Six options is what the floor exists for: the track scrolls instead of squeezing.
                SegmentedToggle(
                    options = listOf("Weight", "Food", "Photos", "Body", "Mood", "Sleep"),
                    selectedIndex = 5,
                    onSelect = {},
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}
