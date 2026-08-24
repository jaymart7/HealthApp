package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import ph.mart.healthapp.core.designsystem.icon.DualStateIcon
import ph.mart.healthapp.core.designsystem.theme.AppTheme

data class BottomNavItem(val icon: DualStateIcon, val label: String)

/**
 * Bottom nav bar, 4 fixed tabs, [MaterialTheme.colorScheme.surfaceContainer] background. Selected
 * tab: filled icon + [MaterialTheme.colorScheme.secondaryContainer] pill indicator. Unselected:
 * outlined icon, [MaterialTheme.colorScheme.onSurfaceVariant]. No navigation-key knowledge — the
 * caller maps tab index to navigation.
 */
@Composable
fun BottomNavBar(items: List<BottomNavItem>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(80.dp),
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                NavTab(
                    icon = if (selected) item.icon.filled else item.icon.outlined,
                    label = item.label,
                    selected = selected,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NavTab(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = modifier.fillMaxHeight(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
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

@PreviewLightDark
@Composable
private fun BottomNavBarPreview() {
    AppTheme {
        Surface {
            BottomNavBar(
                items = listOf(
                    BottomNavItem(AppIcons.Home, "Home"),
                    BottomNavItem(AppIcons.Food, "Food"),
                    BottomNavItem(AppIcons.Progress, "Progress"),
                    BottomNavItem(AppIcons.Profile, "Profile"),
                ),
                selectedIndex = 0,
                onSelect = {},
            )
        }
    }
}
