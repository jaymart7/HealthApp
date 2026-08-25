package ph.mart.healthapp.feature.profile.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** One preference, not two toggles: the profile stores a single [UnitSystem], and every screen
 * that shows a weight or a length already reads it. Flipping this here changes Home, Progress and
 * all three entry sheets at once. */
@Composable
internal fun ProfileUnitsSection(
    unit: UnitSystem,
    onSelect: (UnitSystem) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSection(label = "Units", modifier = modifier) {
        AppCard {
            SegmentedToggle(
                options = listOf("Metric (kg, cm)", "Imperial (lb, in)"),
                selectedIndex = UnitSystem.entries.indexOf(unit),
                onSelect = { index -> onSelect(UnitSystem.entries[index]) },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ProfileUnitsSectionPreview() {
    AppTheme {
        Surface {
            ProfileUnitsSection(
                unit = UnitSystem.Metric,
                onSelect = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
