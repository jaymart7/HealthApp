package ph.mart.healthapp.feature.coach.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.coach.CoachAction
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.coach.R

/**
 * A row the coach drafted, one tap from the diary and not in it yet.
 *
 * `tertiaryContainer` because this is the app's one AI accent and this card is the only place the
 * model's output becomes a *write* — the surface where that matters most is the one that should
 * look like the model. It is deliberately not a dialog: a proposal is part of the conversation, so
 * it scrolls with the conversation, and ignoring it is as valid as answering it.
 *
 * Every figure shown is a figure that will be written. The card exists to be read before the tap,
 * so nothing here is summarised away — a user who does not want 320 kcal of it needs to see the
 * 320 before confirming, not after.
 */
@Composable
internal fun ProposalCard(
    action: CoachAction,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Resolved here rather than passed down: what gets persisted is the words the user was shown,
    // and a ViewModel cannot read a resource.
    val loggedLine = when (action) {
        is CoachAction.LogFood ->
            stringResource(R.string.coach_proposal_logged_food, action.name, action.calories)
        is CoachAction.LogWater ->
            stringResource(R.string.coach_proposal_logged_water, waterAmount(action.glasses))
    }

    AppCard(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        when (action) {
            is CoachAction.LogFood -> {
                Text(
                    text = stringResource(
                        R.string.coach_proposal_food_title,
                        stringResource(action.mealType.labelRes),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = action.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = stringResource(
                        R.string.coach_proposal_macros,
                        action.calories,
                        action.proteinG,
                        action.carbsG,
                        action.fatG,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            is CoachAction.LogWater -> {
                Text(
                    text = stringResource(R.string.coach_proposal_water_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = waterAmount(action.glasses),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SecondaryButton(
                label = stringResource(R.string.coach_proposal_confirm),
                onClick = { onConfirm(loggedLine) },
            )
            TextButton(label = stringResource(R.string.coach_proposal_dismiss), onClick = onDismiss)
        }
    }
}

/** One glass reads as "1 glass", not "1 glasses" — the only place the coach counts something the
 * user can have exactly one of. */
@Composable
private fun waterAmount(glasses: Int): String = if (glasses == 1) {
    stringResource(R.string.coach_proposal_water_body_one)
} else {
    stringResource(R.string.coach_proposal_water_body, glasses)
}

@PreviewLightDark
@Composable
private fun ProposalCardFoodPreview() {
    AppTheme {
        Surface {
            ProposalCard(
                action = CoachAction.LogFood(
                    name = "Scrambled eggs on toast",
                    mealType = MealType.Breakfast,
                    calories = 420,
                    proteinG = 22,
                    carbsG = 31,
                    fatG = 23,
                    portionAmount = 1.0,
                    portionUnit = "serving",
                ),
                onConfirm = {},
                onDismiss = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ProposalCardWaterPreview() {
    AppTheme {
        Surface {
            ProposalCard(
                action = CoachAction.LogWater(glasses = 1),
                onConfirm = {},
                onDismiss = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
