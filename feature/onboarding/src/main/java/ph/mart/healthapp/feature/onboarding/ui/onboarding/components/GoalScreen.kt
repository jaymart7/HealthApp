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
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.designsystem.component.CardIcon
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.SelectableCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.onboarding.R
import ph.mart.healthapp.feature.onboarding.ui.onboarding.GOAL_OPTIONS
import ph.mart.healthapp.feature.onboarding.ui.onboarding.GoalOption
import ph.mart.healthapp.feature.onboarding.ui.shared.components.MinCardHeight
import ph.mart.healthapp.feature.onboarding.ui.shared.components.OnboardingStep
import ph.mart.healthapp.feature.onboarding.ui.shared.components.cardsFit

/**
 * Onboarding step 1 of 6. **No Next button**: a tap selects the card, the selection is held for
 * [ph.mart.healthapp.feature.onboarding.ui.onboarding.SELECTION_HOLD_MS], and the step advances.
 *
 * The disabled Next this replaces was never a decision — it was a receipt for one already made,
 * and a mandatory single-select step has nothing else to confirm. The hold is what keeps it from
 * feeling like a mis-tap: the check lands and the icon circle flips before the screen moves, and
 * the header's back arrow is the undo.
 *
 * The three cards take `weight(1f)` so they divide the content column rather than stacking against
 * the top of it, which is what left 400dp of nothing underneath them.
 */
@Composable
internal fun GoalScreen(
    options: List<GoalOption>,
    selected: Goal?,
    onSelect: (GoalOption) -> Unit,
    onBack: () -> Unit,
) {
    val fits = cardsFit(cards = 3)
    OnboardingStep(
        step = 1,
        mascotState = if (selected == null) MascotState.Idle else MascotState.Happy,
        line = stringResource(options.firstOrNull { it.goal == selected }?.bubble ?: R.string.onboarding_goal_bubble),
        headline = stringResource(R.string.onboarding_goal_title),
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
                    leadingIcon = option.icon,
                    icon = CardIcon.Circle,
                    selected = selected == option.goal,
                    onClick = { onSelect(option) },
                    modifier = if (fits) Modifier.weight(1f).heightIn(min = MinCardHeight) else Modifier.height(MinCardHeight),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun GoalScreenPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            GoalScreen(options = GOAL_OPTIONS, selected = Goal.Lose, onSelect = {}, onBack = {})
        }
    }
}

/** The state the step opens in, and the one the layout has to hold without a void under it. */
@PreviewLightDark
@Composable
private fun GoalScreenUnselectedPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            GoalScreen(options = GOAL_OPTIONS, selected = null, onSelect = {}, onBack = {})
        }
    }
}
