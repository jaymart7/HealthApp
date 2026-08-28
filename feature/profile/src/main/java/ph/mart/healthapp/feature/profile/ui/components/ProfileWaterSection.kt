package ph.mart.healthapp.feature.profile.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.water.WATER_GOAL_GLASSES
import ph.mart.healthapp.core.data.water.waterVolumeLabel
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.NumericStepperField
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** The daily water goal — the one editable target on this screen, because unlike calories and
 * macros it isn't derived from anything: Mifflin–St Jeor has nothing to say about hydration.
 * Clamped to [WATER_GOAL_GLASSES] at the edges rather than validated after the fact. */
@Composable
internal fun ProfileWaterSection(
    goalGlasses: Int,
    unit: UnitSystem,
    onSetGoal: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSection(label = "Water", modifier = modifier) {
        AppCard {
            NumericStepperField(
                label = "Daily goal · ${waterVolumeLabel(goalGlasses, unit)}",
                value = "$goalGlasses",
                unitSuffix = "glasses",
                onIncrement = { onSetGoal((goalGlasses + 1).coerceAtMost(WATER_GOAL_GLASSES.last)) },
                onDecrement = { onSetGoal((goalGlasses - 1).coerceAtLeast(WATER_GOAL_GLASSES.first)) },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ProfileWaterSectionPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ProfileWaterSection(
                goalGlasses = 8,
                unit = UnitSystem.Metric,
                onSetGoal = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
