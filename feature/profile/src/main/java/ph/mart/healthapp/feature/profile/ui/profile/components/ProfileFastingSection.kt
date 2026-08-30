package ph.mart.healthapp.feature.profile.ui.profile.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.fasting.FAST_GOAL_HOURS
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.NumericStepperField
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * The fasting target, the second editable goal on this screen for the same reason the water one is
 * editable: Mifflin–St Jeor has nothing to say about *when* you eat, so nothing derives it.
 *
 * Nudge-only, like the water goal — 12 to 24 in whole hours is a range you step through, not one
 * you type. Changing it moves the Progress chart's goal line and prices the *next* fast; a fast
 * already recorded keeps the target it was started under.
 */
@Composable
internal fun ProfileFastingSection(
    goalHours: Int,
    onSetGoal: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSection(label = "Fasting", modifier = modifier) {
        AppCard {
            NumericStepperField(
                label = "Fasting goal · ${24 - goalHours}h eating window",
                value = "$goalHours",
                unitSuffix = "hours",
                onIncrement = { onSetGoal((goalHours + 1).coerceAtMost(FAST_GOAL_HOURS.last)) },
                onDecrement = { onSetGoal((goalHours - 1).coerceAtLeast(FAST_GOAL_HOURS.first)) },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ProfileFastingSectionPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ProfileFastingSection(
                goalHours = 16,
                onSetGoal = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
