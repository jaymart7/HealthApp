package ph.mart.healthapp.feature.coach.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ph.mart.healthapp.core.data.coach.CoachAction
import ph.mart.healthapp.core.data.coach.draftedOn
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.round1
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.formatDayMonth
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.coach.R

/**
 * The rows the coach drafted, one tap from the diary and not in it yet.
 *
 * `tertiaryContainer` because this is the app's one AI accent and this card is the only place the
 * model's output becomes a *write* — the surface where that matters most is the one that should
 * look like the model. It is deliberately not a dialog: a proposal is part of the conversation, so
 * it scrolls with the conversation, and ignoring it is as valid as answering it.
 *
 * Every figure shown is a figure that will be written. The card exists to be read before the tap,
 * so nothing here is summarised away — a user who does not want 320 kcal of it needs to see the
 * 320 before confirming, not after.
 *
 * **The day is drawn once for the whole card, and only when it is not today.** A backdated draft
 * is rare and a mislabelled one is worse than an unlabelled one, so `send()` refuses a draft whose
 * rows disagree about the day — which is what lets one label speak for all of them.
 *
 * **One row keeps the single-item layout it always had**, because a meal of one thing is not a
 * list; several rows become a list with a `✕` on each. That removal is the reason the confirm
 * hands its rows back rather than the ViewModel reading them off the state: striking out the
 * coffee is a decision made here, and only what is left was agreed to.
 */
@Composable
internal fun ProposalCard(
    actions: List<CoachAction>,
    onConfirm: (List<CoachAction>, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keyed on the draft: a new proposal starts with nothing struck out, and `rememberSaveable`
    // is what stops a rotation mid-decision putting the removed rows back. Indices rather than the
    // rows themselves, so a draft holding the same food twice loses only the one that was tapped.
    var removed by rememberSaveable(actions) { mutableStateOf(emptySet<Int>()) }
    val kept = actions.indices.filterNot { it in removed }
    val keptActions = kept.map { actions[it] }

    AppCard(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        // One day for the card: every row agrees on it by the time a card exists, so the first
        // dated row speaks for the rest — and striking a row out cannot change it.
        val day = dayLabel(actions.firstNotNullOfOrNull { it.draftedOn })
        if (actions.size == 1) {
            SingleProposal(actions.first(), day)
        } else {
            MultiProposal(
                actions = actions,
                kept = kept,
                day = day,
                onRemove = { index -> removed = removed + index },
            )
        }

        // Resolved here rather than passed down: what gets persisted is the words the user was
        // shown, and a ViewModel cannot read a resource.
        val loggedLine = loggedLineFor(keptActions)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SecondaryButton(
                label = stringResource(R.string.coach_proposal_confirm),
                // Striking out every row is a dismissal the long way round, but it is not one
                // until the user says so — the button goes quiet rather than the card vanishing.
                enabled = keptActions.isNotEmpty(),
                onClick = { onConfirm(keptActions, loggedLine) },
            )
            TextButton(label = stringResource(R.string.coach_proposal_dismiss), onClick = onDismiss)
        }
    }
}

/** A meal of one thing is not a list: the single draft keeps the layout it shipped with, where the
 * name is the headline and every figure that will be written is under it. */
@Composable
private fun SingleProposal(action: CoachAction, day: String? = null) {
    when (action) {
        is CoachAction.LogFood -> {
            ProposalTitle(
                stringResource(
                    R.string.coach_proposal_food_title,
                    stringResource(action.mealType.labelRes),
                ),
                day,
            )
            ProposalHeadline(action.name)
            ProposalDetail(
                stringResource(
                    R.string.coach_proposal_macros,
                    action.calories,
                    action.proteinG,
                    action.carbsG,
                    action.fatG,
                ),
            )
        }

        is CoachAction.LogExercise -> {
            ProposalTitle(stringResource(R.string.coach_proposal_exercise_title), day)
            ProposalHeadline(activityName(action))
            ProposalDetail(
                stringResource(
                    R.string.coach_proposal_exercise_body,
                    action.minutes,
                    action.burnedKcal,
                ),
            )
        }

        is CoachAction.LogWater -> {
            ProposalTitle(stringResource(R.string.coach_proposal_water_title), day)
            ProposalHeadline(waterAmount(action.glasses))
        }

        is CoachAction.LogWeight -> {
            ProposalTitle(stringResource(R.string.coach_proposal_weight_title), day)
            ProposalHeadline(weightAmount(action))
            weightChange(action)?.let { ProposalDetail(it) }
        }

        is CoachAction.LogSupplement -> {
            ProposalTitle(stringResource(R.string.coach_proposal_supplement_title), day)
            ProposalHeadline(action.name)
            ProposalDetail(supplementDoses(action.doses))
        }

        // `resolve()` turns a saved meal into its own rows before any card is drawn, so this is
        // only ever reached if that stops being true. It renders the name rather than nothing,
        // which stays honest: the name is the whole of what the model supplied.
        is CoachAction.LogSavedMeal -> {
            ProposalTitle(stringResource(R.string.coach_proposal_food_title, stringResource(action.mealType.labelRes)), day)
            ProposalHeadline(action.name)
        }
    }
}

/**
 * The list, with what is left of it totalled underneath.
 *
 * A removed row is *gone* rather than struck through: the card's promise is that what it shows is
 * what gets written, and a greyed row still on screen is a row the eye counts. [kept] carries the
 * original indices in order, so removing the middle row does not renumber the rest.
 */
@Composable
private fun MultiProposal(
    actions: List<CoachAction>,
    kept: List<Int>,
    day: String?,
    onRemove: (Int) -> Unit,
) {
    ProposalTitle(stringResource(R.string.coach_proposal_items_title, kept.size), day)
    kept.forEach { index ->
        val action = actions[index]
        val name = actionName(action)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f),
            )
            rowDetail(action)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.tabularNums,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.End,
                )
            }
            IconButton(onClick = { onRemove(index) }) {
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = stringResource(R.string.coach_proposal_remove, name),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }

    // Only the foods, and only when there are some: a workout's calories are *burned* and a glass
    // of water has none, so summing either into one "kcal" figure would print a number that is
    // true of nothing.
    val foods = kept.map { actions[it] }.filterIsInstance<CoachAction.LogFood>()
    if (foods.isNotEmpty()) {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f),
        )
        ProposalDetail(
            stringResource(
                R.string.coach_proposal_items_total,
                foods.sumOf { it.calories },
                foods.sumOf { it.proteinG },
                foods.sumOf { it.carbsG },
                foods.sumOf { it.fatG },
            ),
        )
    }
}

/**
 * The kind of row, and the day when it is not today — "Add to Breakfast · Yesterday".
 *
 * The day goes here rather than on its own line because it is a qualifier on the title, not a
 * second heading, and a card that grows a line when backdated moves everything under it.
 */
@Composable
private fun ProposalTitle(text: String, day: String? = null) = Text(
    text = day?.let { stringResource(R.string.coach_proposal_title_day, text, it) } ?: text,
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onTertiaryContainer,
)

@Composable
private fun ProposalHeadline(text: String) = Text(
    text = text,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onTertiaryContainer,
    modifier = Modifier.padding(top = 4.dp),
)

@Composable
private fun ProposalDetail(text: String) = Text(
    text = text,
    style = MaterialTheme.typography.bodySmall.tabularNums,
    color = MaterialTheme.colorScheme.onTertiaryContainer,
    modifier = Modifier.padding(top = 4.dp),
)

/**
 * Null for today, which is almost every draft and needs no label at all.
 *
 * "Yesterday" is a resource and the rest is [formatDayMonth] — a month is as far back as a draft
 * reaches, so the year would be the same word on every card. The diary's own header draws the same
 * three cases; this cannot call it, because `:feature:*` modules never import each other.
 */
@Composable
private fun dayLabel(epochDay: Long?): String? {
    val today = todayEpochDay()
    return when (epochDay) {
        null, today -> null
        today - 1 -> stringResource(R.string.coach_proposal_day_yesterday)
        else -> formatDayMonth(epochDay)
    }
}

/** What the row is called — the same words the logged line and the remove button use, so a screen
 * reader and the diary agree with what is on the card. */
@Composable
private fun actionName(action: CoachAction): String = when (action) {
    is CoachAction.LogFood -> action.name
    is CoachAction.LogWater -> waterAmount(action.glasses)
    is CoachAction.LogExercise -> activityName(action)
    is CoachAction.LogSavedMeal -> action.name
    is CoachAction.LogWeight -> weightAmount(action)
    is CoachAction.LogSupplement -> action.name
}

/** Null where the name already is the whole row: a glass of water has no second figure. */
@Composable
private fun rowDetail(action: CoachAction): String? = when (action) {
    is CoachAction.LogFood -> stringResource(R.string.coach_proposal_row_kcal, action.calories)
    is CoachAction.LogWater -> null
    is CoachAction.LogExercise ->
        stringResource(R.string.coach_proposal_exercise_body, action.minutes, action.burnedKcal)
    // No figures to show: a saved meal carries a name until `resolve()` gives it its rows.
    is CoachAction.LogSavedMeal -> null
    is CoachAction.LogWeight -> weightChange(action)
    is CoachAction.LogSupplement -> supplementDoses(action.doses)
}

/**
 * The line appended to the persisted answer, so reopening the chat still shows what was logged.
 *
 * A single draft names the thing; several name the count and what they came to, because a list of
 * four foods in a chat bubble is the card again and the card is gone by then.
 */
@Composable
private fun loggedLineFor(actions: List<CoachAction>): String {
    val single = actions.singleOrNull()
    return when {
        single is CoachAction.LogFood ->
            stringResource(R.string.coach_proposal_logged_food, single.name, single.calories)
        single is CoachAction.LogWater ->
            stringResource(R.string.coach_proposal_logged_water, waterAmount(single.glasses))
        single is CoachAction.LogExercise ->
            stringResource(R.string.coach_proposal_logged_exercise, activityName(single), single.minutes)
        single is CoachAction.LogWeight ->
            stringResource(R.string.coach_proposal_logged_weight, weightAmount(single))
        single is CoachAction.LogSupplement ->
            stringResource(R.string.coach_proposal_logged_supplement, single.name)
        else -> pluralStringResource(
            R.plurals.coach_proposal_logged_items,
            actions.size,
            actions.size,
            actions.filterIsInstance<CoachAction.LogFood>().sumOf { it.calories },
        )
    }
}

/**
 * The figure the user gave, in the unit their profile uses and **not converted** — this is the
 * number `settle` is about to write, and the card's promise is that they are the same one.
 */
@Composable
private fun weightAmount(action: CoachAction.LogWeight): String = stringResource(
    R.string.coach_proposal_weight_body,
    formatWeight(action.weight),
    action.unit.weightUnitLabel(),
)

/**
 * What it moves by, in the same unit. Null on a first weigh-in and on no change at all, the rule
 * the water row follows — nothing to say is better said by saying nothing.
 *
 * Signed rather than an arrow, and uncoloured: whether up is good depends on the user's goal,
 * which is the trend-arrow rule, and a plain "+0.4 kg" answers without taking a side.
 */
@Composable
private fun weightChange(action: CoachAction.LogWeight): String? {
    val previous = action.previousKg?.kgToDisplayUnit(action.unit) ?: return null
    val delta = round1(action.weight - previous)
    if (delta == 0.0) return null
    return stringResource(
        R.string.coach_proposal_weight_change,
        (if (delta > 0) "+" else "\u2212") + formatWeight(abs(delta)),
        action.unit.weightUnitLabel(),
    )
}

/** Drops a trailing ".0" — 82 rather than 82.0, the same helper the weigh-in sheet and every
 * measurement row keep locally. */
private fun formatWeight(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString() else "%.1f".format(value)

/** An unnamed activity is called after its type — what [ph.mart.healthapp.core.data.exercise.ExerciseEntry]
 * means by an empty name, resolved here because only a composable can read the enum's label. */
@Composable
private fun activityName(action: CoachAction.LogExercise): String =
    action.name.ifEmpty { stringResource(action.type.label) }

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
                actions = listOf(
                    CoachAction.LogFood(
                        name = "Scrambled eggs on toast",
                        mealType = MealType.Breakfast,
                        calories = 420,
                        proteinG = 22,
                        carbsG = 31,
                        fatG = 23,
                        portionAmount = 1.0,
                        portionUnit = "serving",
                    ),
                ),
                onConfirm = { _, _ -> },
                onDismiss = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** The whole point of the list layout: one sentence, three rows, one total. */
@PreviewLightDark
@Composable
private fun ProposalCardMealPreview() {
    AppTheme {
        Surface {
            ProposalCard(
                actions = listOf(
                    food("Scrambled eggs", 220, 14, 2, 17),
                    food("Toast, 2 slices", 180, 6, 30, 3),
                    food("Black coffee", 5, 0, 1, 0),
                ),
                onConfirm = { _, _ -> },
                onDismiss = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** Mixed kinds in one draft — "log my eggs and a glass of water". No kcal total: a glass of water
 * has none, so the footer speaks for the foods alone. */
@PreviewLightDark
@Composable
private fun ProposalCardMixedPreview() {
    AppTheme {
        Surface {
            ProposalCard(
                actions = listOf(
                    food("Scrambled eggs", 220, 14, 2, 17),
                    CoachAction.LogWater(glasses = 1),
                    CoachAction.LogExercise(
                        type = ExerciseType.Run,
                        name = "Morning run",
                        minutes = 30,
                        burnedKcal = 343,
                    ),
                ),
                onConfirm = { _, _ -> },
                onDismiss = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ProposalCardExercisePreview() {
    AppTheme {
        Surface {
            ProposalCard(
                actions = listOf(
                    CoachAction.LogExercise(
                        type = ExerciseType.Run,
                        name = "Morning run",
                        minutes = 30,
                        burnedKcal = 343,
                    ),
                ),
                onConfirm = { _, _ -> },
                onDismiss = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** Both halves of the weigh-in card: the figure, and what it moves by. */
@PreviewLightDark
@Composable
private fun ProposalCardWeightPreview() {
    AppTheme {
        Surface {
            ProposalCard(
                actions = listOf(
                    CoachAction.LogWeight(
                        weight = 82.4,
                        unit = UnitSystem.Metric,
                        previousKg = 83.0,
                    ),
                ),
                onConfirm = { _, _ -> },
                onDismiss = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** The doses this tick adds, never the day's new total — [CoachAction.LogSupplement]'s own rule,
 * and the reason it reads "1 dose" rather than "1 of 2". */
@Composable
private fun supplementDoses(doses: Int): String =
    pluralStringResource(R.plurals.coach_proposal_supplement_doses, doses, doses)

@PreviewLightDark
@Composable
private fun ProposalCardSupplementPreview() {
    AppTheme {
        Surface {
            ProposalCard(
                actions = listOf(CoachAction.LogSupplement(name = "Creatine", doses = 1, supplementId = 2)),
                onConfirm = { _, _ -> },
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
                actions = listOf(CoachAction.LogWater(glasses = 1)),
                onConfirm = { _, _ -> },
                onDismiss = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

private fun food(name: String, kcal: Int, protein: Int, carbs: Int, fat: Int) = CoachAction.LogFood(
    name = name,
    mealType = MealType.Breakfast,
    calories = kcal,
    proteinG = protein,
    carbsG = carbs,
    fatG = fat,
    portionAmount = 1.0,
    portionUnit = "serving",
)
