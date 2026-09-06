package ph.mart.healthapp.feature.profile.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R

/** One preference, not two toggles: the profile stores a single [UnitSystem], and every screen
 * that shows a weight or a length already reads it. Flipping this here changes Home, Progress, all
 * three entry sheets and About you's own steppers at once — which is what the sublabel says, and
 * why this sits under Display rather than with the body figures it re-renders. */
@Composable
internal fun SettingsUnitsSection(
    unit: UnitSystem,
    onSelect: (UnitSystem) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.profile_section_units),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.profile_settings_units_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SegmentedToggle(
                options = listOf(
                    stringResource(R.string.profile_units_metric),
                    stringResource(R.string.profile_units_imperial),
                ),
                selectedIndex = UnitSystem.entries.indexOf(unit),
                onSelect = { index -> onSelect(UnitSystem.entries[index]) },
                trackColor = MaterialTheme.colorScheme.surfaceContainer,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SettingsUnitsSectionPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            SettingsUnitsSection(
                unit = UnitSystem.Metric,
                onSelect = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
