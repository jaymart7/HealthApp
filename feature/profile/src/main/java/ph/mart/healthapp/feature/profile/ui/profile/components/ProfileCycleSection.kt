package ph.mart.healthapp.feature.profile.ui.profile.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * The one switch that decides whether the cycle surfaces exist at all.
 *
 * Off is the default, and off means *gone* — no Home card, no Progress subject. Every other empty
 * subject on the Progress tab keeps its slot because it is empty for want of data; this one may be
 * permanently irrelevant to whoever is holding the phone, and a card that can never say anything
 * is a card that should not be there.
 *
 * The sublabel says where the data goes, because that is the first question this feature raises:
 * nowhere. It is not in the AI payloads, the widget, the watch or the weekly recap.
 */
@Composable
internal fun ProfileCycleSection(
    enabled: Boolean,
    onSetEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSection(label = "Cycle", modifier = modifier) {
        AppCard {
            SettingsRow(
                label = "Track your cycle",
                sublabel = "Adds a Home card and a Progress page for period logging. Stays on " +
                    "this phone — it is never sent to the coach or used for an insight.",
                trailing = { Switch(checked = enabled, onCheckedChange = onSetEnabled) },
            )
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
