package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * Extended FAB, [MaterialTheme.colorScheme.primaryContainer] fill, Level-3 shadow — the only real
 * drop shadow in the app (M3's default FAB elevation is already the 6dp we want). Positioning is
 * the caller's job; it floats above the nav bar, never on top of it — leave
 * [FabBottomClearance] below a screen's last item.
 *
 * [expanded] `false` collapses it to an icon-only circle; drive it from [rememberFabExpanded].
 */
@Composable
fun DockedFab(onClick: () -> Unit, modifier: Modifier = Modifier, label: String = "Log", expanded: Boolean = true) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        expanded = expanded,
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        icon = {
            Icon(
                imageVector = AppIcons.Add,
                contentDescription = if (expanded) null else label,
                modifier = Modifier.size(20.dp),
            )
        },
        text = {
            Text(text = label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
        },
        modifier = modifier,
    )
}

/**
 * Vertical room a screen must leave below its last item so [DockedFab] never covers it: 56dp FAB +
 * 16dp inset + 16dp breathing room. A component metric, not a value off the spacing scale.
 */
val FabBottomClearance = 88.dp

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
