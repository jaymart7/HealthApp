package ph.mart.healthapp.feature.onboarding.ui.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.maintenanceMultiplier
import ph.mart.healthapp.core.designsystem.component.CardIcon
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.SelectableCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.onboarding.R
import ph.mart.healthapp.feature.onboarding.ui.onboarding.ACTIVITY_OPTIONS
import ph.mart.healthapp.feature.onboarding.ui.onboarding.ActivityOption
import ph.mart.healthapp.feature.onboarding.ui.shared.components.MinCardHeight
import ph.mart.healthapp.feature.onboarding.ui.shared.components.OnboardingStep
import ph.mart.healthapp.feature.onboarding.ui.shared.components.cardsFit

/**
 * Onboarding step 3 of 6. Step 1's treatment with four cards, and one addition: the selected card
 * prints what the level does to the maintenance figure.
 *
 * Activity is the input people guess at most and the one with the largest effect on step 6.
 * Showing the multiplier at the moment of choosing is the difference between a quiz question and a
 * control, and it plants the arithmetic the confirm step then pays off — so that screen is not the
 * first time a number appears. The figure comes from
 * [ph.mart.healthapp.core.data.profile.maintenanceMultiplier], which is the same table
 * `calculateDailyTargets` divides by: a second copy would be a second answer.
 */
@Composable
internal fun ActivityScreen(
    options: List<ActivityOption>,
    selected: ActivityLevel?,
    onSelect: (ActivityOption) -> Unit,
    onBack: () -> Unit,
) {
    val fits = cardsFit(cards = 4)
    OnboardingStep(
        step = 3,
        mascotState = if (selected == null) MascotState.Idle else MascotState.Happy,
        line = stringResource(R.string.onboarding_activity_bubble),
        headline = stringResource(R.string.onboarding_activity_title),
        onBack = onBack,
        scrollable = !fits,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().then(if (fits) Modifier.weight(1f) else Modifier).semantics {
                isTraversalGroup = true
                selectableGroup()
            },
        ) {
            options.forEach { option ->
                SelectableCard(
                    title = stringResource(option.title),
                    subtitle = stringResource(option.subtitle),
                    supporting = stringResource(
                        R.string.onboarding_activity_multiplier,
                        option.level.maintenanceMultiplier().toString(),
                    ),
                    leadingIcon = option.icon,
                    icon = CardIcon.Circle,
                    selected = selected == option.level,
                    onClick = { onSelect(option) },
                    modifier = if (fits) Modifier.weight(1f).heightIn(min = MinCardHeight) else Modifier.height(MinCardHeight),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ActivityScreenPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ActivityScreen(options = ACTIVITY_OPTIONS, selected = ActivityLevel.Light, onSelect = {}, onBack = {})
        }
    }
}

@PreviewLightDark
@Composable
private fun ActivityScreenUnselectedPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ActivityScreen(options = ACTIVITY_OPTIONS, selected = null, onSelect = {}, onBack = {})
        }
    }
}
