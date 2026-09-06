package ph.mart.healthapp.feature.profile.ui.profile.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.shared.components.AppListRow

/**
 * The one switch that decides whether the cycle surfaces exist at all.
 *
 * Off is the default, and off means *gone* — no Home card, no Progress subject. Every other empty
 * subject on the Progress tab keeps its slot because it is empty for want of data; this one may be
 * permanently irrelevant to whoever is holding the phone, and a card that can never say anything
 * is a card that should not be there.
 *
 * It stays on Profile rather than moving to Settings with the rest of the switches: it is a fact
 * about the person, it creates surfaces rather than restyling them, and a privacy-sensitive switch
 * should not be two levels deep.
 *
 * The sublabel says where the data goes, because that is the first question this feature raises:
 * nowhere. It is not in the AI payloads, the widget, the watch or the weekly recap. The line that
 * appears when it goes on is the other half of the same honesty — a switch that quietly adds two
 * surfaces elsewhere in the app should say which two.
 */
@Composable
internal fun ProfileCycleSection(
    enabled: Boolean,
    onSetEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        AppListRow(
            label = stringResource(R.string.profile_cycle_track),
            sublabel = stringResource(R.string.profile_cycle_track_sub),
            trailing = { Switch(checked = enabled, onCheckedChange = onSetEnabled) },
        )
        AnimatedVisibility(visible = enabled) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Icon(
                    imageVector = AppIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(R.string.profile_cycle_reveal),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ProfileCycleSectionPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ProfileCycleSection(enabled = true, onSetEnabled = {}, modifier = Modifier.padding(16.dp))
        }
    }
}

/** Off: the two surfaces it would create are not there, and neither is the line saying so. */
@PreviewLightDark
@Composable
private fun ProfileCycleSectionOffPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ProfileCycleSection(enabled = false, onSetEnabled = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
