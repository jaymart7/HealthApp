package ph.mart.healthapp.feature.profile.ui.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * The one row shape Profile, Settings, Reminders and About you are all built from: an optional
 * leading slot, a label with an optional sublabel, and an optional trailing slot.
 *
 * Successor to `SettingsRow`, which had no leading slot and no minimum height. 64dp is what makes
 * a row a *row* rather than a line of text — the redesign's whole argument is that a doorway and a
 * switch should not read identically, and the leading [IconTile] is half of that. There is no
 * separate `SwitchRow`: a switch is what [trailing] is for, and one row shape with two slots beats
 * three near-identical ones.
 */
@Composable
internal fun AppListRow(
    label: String,
    modifier: Modifier = Modifier,
    sublabel: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (sublabel != null) {
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

/** The chevron that says a row leaves the screen. Its own composable because four cards draw it
 * and a nav row without one is indistinguishable from a switch row whose switch failed to load. */
@Composable
internal fun NavChevron() {
    Icon(
        imageVector = AppIcons.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(end = 4.dp),
    )
}

@PreviewLightDark
@Composable
private fun AppListRowPreview() {
    AppTheme {
        Surface {
            AppCard(modifier = Modifier.padding(16.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                AppListRow(
                    label = "Supplements",
                    sublabel = "What you take each day — it shows up on Home",
                    leading = { IconTile(icon = AppIcons.Supplement, contentDescription = null) },
                    trailing = { NavChevron() },
                )
                AppListRow(
                    label = "Weigh-in day",
                    sublabel = "Every Monday, 8:00 AM",
                    trailing = { Switch(checked = true, onCheckedChange = {}) },
                )
            }
        }
    }
}
