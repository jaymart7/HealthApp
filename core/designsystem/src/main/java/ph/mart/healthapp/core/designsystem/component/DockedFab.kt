package ph.mart.healthapp.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * Extended FAB, [MaterialTheme.colorScheme.primaryContainer] fill, Level-3 shadow — the only real
 * drop shadow in the app. Positioning (docked, overlapping the nav bar) is the caller's job.
 *
 * [expanded] `false` collapses it to an icon-only circle; drive it from [rememberFabExpanded].
 */
@Composable
fun DockedFab(onClick: () -> Unit, modifier: Modifier = Modifier, label: String = "Log", expanded: Boolean = true) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 6.dp,
        modifier = modifier.height(56.dp).widthIn(min = 56.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(imageVector = AppIcons.Add, contentDescription = if (expanded) null else label, modifier = Modifier.size(20.dp))
            AnimatedVisibility(
                visible = expanded,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

/** Below this many pixels a scroll delta is fling settle or thumb jitter, not a direction change. */
private const val SCROLL_DIRECTION_SLOP = 8

/**
 * Extended at rest and while scrolling up, collapsed while scrolling down — the prototype's
 * `appScaffold.js` spec. Reads [ScrollState.value] off the snapshot rather than during
 * composition, so a scroll only recomposes the FAB when the direction actually flips.
 */
@Composable
fun rememberFabExpanded(scrollState: ScrollState): Boolean {
    var expanded by remember(scrollState) { mutableStateOf(true) }
    LaunchedEffect(scrollState) {
        var last = scrollState.value
        snapshotFlow { scrollState.value }.collect { current ->
            when {
                current <= 0 -> expanded = true
                current - last > SCROLL_DIRECTION_SLOP -> expanded = false
                last - current > SCROLL_DIRECTION_SLOP -> expanded = true
                else -> return@collect // inside the slop: keep the current state *and* the last offset
            }
            last = current
        }
    }
    return expanded
}

@PreviewLightDark
@Composable
private fun DockedFabPreview() {
    AppTheme {
        Surface {
            DockedFab(onClick = {}, modifier = Modifier.padding(16.dp))
        }
    }
}

@PreviewLightDark
@Composable
private fun DockedFabCollapsedPreview() {
    AppTheme {
        Surface {
            DockedFab(onClick = {}, modifier = Modifier.padding(16.dp), expanded = false)
        }
    }
}
