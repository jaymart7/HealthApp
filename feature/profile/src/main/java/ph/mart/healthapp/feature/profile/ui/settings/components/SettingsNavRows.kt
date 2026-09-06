package ph.mart.healthapp.feature.profile.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.shared.components.AppListRow
import ph.mart.healthapp.feature.profile.ui.shared.components.IconTile
import ph.mart.healthapp.feature.profile.ui.shared.components.NavChevron

/**
 * The three rows on Settings that leave it. **None of them carries a count or a cached state** —
 * unchanged rule: a number here is one more thing that can go stale, and the screen it opens is
 * where counting is honest. Google Health's is the sharpest case, since connection state is
 * whatever Google says right now and a sublabel claiming "Connected" survives a revocation
 * elsewhere; Reminders' sublabel names what is inside rather than how many are on.
 */
@Composable
internal fun SettingsNavRow(
    label: String,
    sublabel: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier,
    ) {
        AppListRow(
            label = label,
            sublabel = sublabel,
            leading = { IconTile(icon = icon, contentDescription = null) },
            trailing = { NavChevron() },
        )
    }
}

/** The way into the Home card editor. It sits under Display because that is what it is — the third
 * choice about how the app looks, beside the scheme and the buddy. */
@Composable
internal fun SettingsHomeLayoutSection(onOpenHomeLayout: () -> Unit, modifier: Modifier = Modifier) {
    SettingsNavRow(
        label = stringResource(R.string.profile_layout_row),
        sublabel = stringResource(R.string.profile_layout_row_sub),
        icon = AppIcons.Home.outlined,
        onClick = onOpenHomeLayout,
        modifier = modifier,
    )
}

/** The way into the eight reminder switches, which are a screen of their own rather than a card
 * on this one. The sublabel is a static description of what is in there, not a count of what is
 * on — see [SettingsNavRow]. */
@Composable
internal fun SettingsRemindersSection(onOpenReminders: () -> Unit, modifier: Modifier = Modifier) {
    SettingsNavRow(
        label = stringResource(R.string.profile_settings_reminders_row),
        sublabel = stringResource(R.string.profile_settings_reminders_row_sub),
        icon = AppIcons.Bell,
        onClick = onOpenReminders,
        modifier = modifier,
    )
}

/** The way into the Google Health connection. */
@Composable
internal fun SettingsConnectionsSection(onOpenHealth: () -> Unit, modifier: Modifier = Modifier) {
    SettingsNavRow(
        label = stringResource(R.string.profile_connections_health),
        sublabel = stringResource(R.string.profile_connections_health_sub),
        icon = AppIcons.Link,
        onClick = onOpenHealth,
        modifier = modifier,
    )
}

@PreviewLightDark
@Composable
private fun SettingsNavRowsPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                SettingsHomeLayoutSection(onOpenHomeLayout = {})
                SettingsRemindersSection(onOpenReminders = {})
                SettingsConnectionsSection(onOpenHealth = {})
            }
        }
    }
}
