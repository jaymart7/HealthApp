package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** What a [NavRail] takes out of the window's width. */
val NavRailWidth = 80.dp

/**
 * [BottomNavBar]'s sibling for a window wide enough that navigation belongs at the edge rather than
 * the bottom — phone landscape, a folded outer display, split screen, a tablet.
 *
 * It takes the same [BottomNavItem] list, index and callback, and draws the same pill: this is one
 * bar rotated, not a second design. Which of the two is used is `AppScaffold`'s decision, never a
 * `when` inside either — the app has one width rule and it lives where the navigation wiring does.
 *
 * [fab] is the docked FAB moved up here, because a rail leaves no bottom edge to dock against. It is
 * a slot rather than a lambda-and-label so this module keeps knowing nothing about what the button
 * opens.
 */
@Composable
fun NavRail(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    fab: (@Composable () -> Unit)? = null,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier.fillMaxHeight()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(NavRailWidth)
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding()
                .padding(vertical = 12.dp),
        ) {
            fab?.let {
                Box(modifier = Modifier.padding(bottom = 12.dp)) { it() }
            }
            // The tabs take the height the FAB leaves and share it, rather than stacking from the
            // top: the window this is drawn on is usually wide *because* it is short — a landscape
            // phone is ~410dp tall — and a fixed stack put the fourth tab off the bottom edge.
            // ponytail: at a very large font scale on a short window the labels can still crowd;
            // dropping to icon-only below a height threshold is the upgrade.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.weight(1f),
            ) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    RailTab(
                        icon = if (selected) item.icon.filled else item.icon.outlined,
                        label = item.label,
                        selected = selected,
                        onClick = { onSelect(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RailTab(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                shape = RoundedCornerShape(16.dp),
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val previewItems = listOf(
    BottomNavItem(AppIcons.Home, "Home"),
    BottomNavItem(AppIcons.Food, "Food"),
    BottomNavItem(AppIcons.Progress, "Progress"),
    BottomNavItem(AppIcons.Profile, "Profile"),
)

@PreviewLightDark
@Composable
private fun NavRailPreview() {
    AppTheme {
        Surface {
            NavRail(items = previewItems, selectedIndex = 0, onSelect = {})
        }
    }
}

/** With the FAB in it, which is how `AppScaffold` actually draws it. */
@PreviewLightDark
@Composable
private fun NavRailWithFabPreview() {
    AppTheme {
        Surface {
            NavRail(
                items = previewItems,
                selectedIndex = 2,
                onSelect = {},
                fab = { DockedFab(onClick = {}, expanded = false) },
            )
        }
    }
}
