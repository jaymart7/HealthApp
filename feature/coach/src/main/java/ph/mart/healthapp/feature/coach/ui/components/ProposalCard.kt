package ph.mart.healthapp.feature.coach.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ph.mart.healthapp.core.data.bloodpressure.categoryOf
import ph.mart.healthapp.core.data.bloodpressure.formatBloodPressure
import ph.mart.healthapp.core.data.coach.CoachAction
import ph.mart.healthapp.core.data.coach.CoachScreen
import ph.mart.healthapp.core.data.coach.draftedOn
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.exercise.RoutineLift
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.health.formatBpm
import ph.mart.healthapp.core.data.mood.MoodLevel
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.round1
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.health.formatDuration
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.data.progress.unitLabel
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.formatDayMonth
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.component.formatOneDecimal
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
    // The one that went most recently, so it can come back. Saved beside `removed` and cleared by
    // the undo itself: an undo line for a row already restored is a line that lies.
    var lastRemoved by rememberSaveable(actions) { mutableStateOf<Int?>(null) }
    val kept = actions.indices.filterNot { it in removed }
    val keptActions = kept.map { actions[it] }
    val emptied = keptActions.isEmpty()

    AppCard(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
    ) {
        // One day for the card: every row agrees on it by the time a card exists, so the first
        // dated row speaks for the rest — and striking a row out cannot change it.
        val day = dayLabel(actions.firstNotNullOfOrNull { it.draftedOn })
        DraftHeader(day = day)

        when {
            // Striking out every row is a dismissal the long way round, but it is not one until
            // the user says so. The card stays, says what happened, and the second action becomes
            // "Dismiss" — a three-row draft is still a valid write, only an empty one is not.
            emptied -> {
                ProposalHeadline(stringResource(R.string.coach_proposal_empty_title))
                ProposalDetail(stringResource(R.string.coach_proposal_empty_body))
            }
            actions.size == 1 -> SingleProposal(actions.first())
            else -> MultiProposal(
                actions = actions,
                kept = kept,
                onRemove = { index ->
                    removed = removed + index
                    lastRemoved = index
                },
            )
        }

        lastRemoved?.let { index ->
            UndoLine(
                name = actionName(actions[index]),
                onUndo = {
                    removed = removed - index
                    lastRemoved = null
                },
            )
        }

        // Resolved here rather than passed down: what gets persisted is the words the user was
        // shown, and a ViewModel cannot read a resource.
        val receipt = loggedLineFor(keptActions)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The only filled `primary` button in the conversation, and that is the point: the
            // starters are filled cards, the follow-ups are outlined pills, and this is the one
            // control on the screen that writes something. It used to be a `SecondaryButton`,
            // which made the write look like the third-most important thing on its own card.
            PrimaryButton(
                // "Log it" is a promise two of these do not keep: a routine's tap opens the
                // workout screen and writes nothing at all, and a fast's flips a timer rather than
                // logging a row. Both say what they do instead. And the label **counts what is
                // left** — striking two rows out of four and still reading "Log 4 items" would be
                // the card breaking its own promise on the button that keeps it.
                label = confirmLabel(keptActions, actions.size),
                icon = confirmIconFor(keptActions.singleOrNull()),
                enabled = !emptied,
                onClick = { onConfirm(keptActions, receipt) },
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            )
            TextButton(
                label = stringResource(
                    if (emptied) R.string.coach_proposal_empty_dismiss
                    else R.string.coach_proposal_dismiss,
                ),
                onClick = onDismiss,
            )
        }

        // Only where the tap writes nothing: the button says "Start it" and a user who reads
        // "Start" on a card full of lifts could reasonably expect the session to be logged.
        if (keptActions.singleOrNull() is CoachAction.StartRoutine) {
            ProposalDetail(stringResource(R.string.coach_proposal_routine_caveat))
        }
    }
}

/**
 * The card's first line: what this is, and — only when it is not today — which day.
 *
 * "Draft · nothing logged yet" says the card's whole promise in the place a reader starts, which is
 * the half that colour cannot carry: `tertiaryContainer` says *the model made this* and nothing
 * about whether it has happened yet. The day rides as a white chip at the row's end rather than as
 * a second heading, because it qualifies the draft rather than announcing one — and a card that
 * grew a line when backdated would move everything under it.
 */
@Composable
private fun DraftHeader(day: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = AppIcons.AiSparkle,
            contentDescription = stringResource(R.string.coach_proposal_drafted),
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.coach_proposal_draft_label).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.weight(1f),
        )
        if (day != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

/**
 * What the last `✕` took, and the way back.
 *
 * Inside the card, not a snackbar: the decision was made here, the card is still on screen, and a
 * bar at the bottom of the window would be a second place to look for the consequence of a tap.
 * One row deep — the row before last is not coming back, because an undo stack on a card the user
 * is about to confirm is a second thing to reason about for a tap that is one `✕` away from being
 * redone.
 */
@Composable
private fun UndoLine(name: String, onUndo: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = AppIcons.Undo,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.coach_proposal_removed, name),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Text(
            text = stringResource(R.string.coach_proposal_undo),
            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clickable(onClick = onUndo),
        )
    }
}

/**
 * Which verb the confirm button uses, and what it counts.
 *
 * "Log it" is the default because logging is what all but two of these do. A routine *starts*
 * something — its tap opens a form and writes nothing — and a fast starts or ends one, which is a
 * state and not a row. [single] is null for a multi-row draft, which is always foods and always
 * logs.
 *
 * Pure and `internal` so [ConfirmLabelTest] can hold it, because the interesting half is not the
 * verb: it is that a *multi-row* draft counts **what is left** rather than what was drafted. A card
 * whose title, total and macro legend all recount on a `✕` while the button still says "Log 4
 * items" is the one place on the card where a stale figure costs the user a row.
 */
@StringRes
internal fun confirmLabelFor(single: CoachAction?): Int = when {
    single is CoachAction.StartRoutine -> R.string.coach_proposal_start
    // A note is written rather than logged: "Log it" under a sentence in the user's own voice
    // reads as though the app were about to count it.
    single is CoachAction.LogNote -> R.string.coach_proposal_note_confirm
    single is CoachAction.SetFast && single.ending -> R.string.coach_proposal_end
    single is CoachAction.SetFast -> R.string.coach_proposal_start
    single is CoachAction.OpenScreen -> R.string.coach_proposal_open
    single is CoachAction.DeleteFood || single is CoachAction.DeleteExercise ->
        R.string.coach_proposal_remove_confirm
    single?.isChange == true -> R.string.coach_proposal_update
    else -> R.string.coach_proposal_confirm
}

/** A draft that changes rows already written rather than adding new ones. Its button and its logged
 * line say so: "Log 3 items" over a card that deletes one of them would be the wrong verb. */
internal val CoachAction.isChange: Boolean
    get() = this is CoachAction.EditFood || this is CoachAction.EditExercise ||
        this is CoachAction.DeleteFood || this is CoachAction.DeleteExercise ||
        this is CoachAction.SetWater

/**
 * Whether the button counts, given what is left of a draft that started with [drafted] rows.
 *
 * A single-row draft never counts — "Log it" is what a meal of one thing deserves — and a draft
 * that started with several counts even once it is down to one, because the user *took rows out*
 * and the number is the acknowledgement. Null means "no count", which is the label above alone.
 */
internal fun confirmCountFor(kept: Int, drafted: Int): Int? = kept.takeIf { drafted > 1 && it > 0 }

/** The two together, resolved. A composable rather than a pure function because only a composable
 * reads a plural, and the rule underneath it is the two functions above. */
@Composable
private fun confirmLabel(kept: List<CoachAction>, drafted: Int): String {
    val count = confirmCountFor(kept.size, drafted)
    return when {
        count == null -> stringResource(confirmLabelFor(kept.singleOrNull()))
        kept.any { it.isChange } -> pluralStringResource(R.plurals.coach_proposal_apply_items, count, count)
        else -> pluralStringResource(R.plurals.coach_proposal_confirm_items, count, count)
    }
}

/** The glyph that leads the label, and it carries what the word cannot: a `check` writes, a
 * `play_arrow` opens a screen and writes nothing. */
private fun confirmIconFor(single: CoachAction?): ImageVector = when (single) {
    is CoachAction.StartRoutine -> AppIcons.Play
    is CoachAction.SetFast -> if (single.ending) AppIcons.Check else AppIcons.Play
    is CoachAction.OpenScreen -> AppIcons.ArrowForward
    is CoachAction.DeleteFood, is CoachAction.DeleteExercise -> AppIcons.Delete
    else -> AppIcons.Check
}

/**
 * A meal of one thing is not a list, so a single draft keeps the headline layout: a kicker naming
 * where it is going, the thing itself as the title, and every figure that will be written on the
 * white panel underneath.
 *
 * The three that write no figures at all — a routine, and a fast at either end — draw a panel that
 * says what the tap *opens* rather than one that says what it writes. The panel is still there,
 * because its absence would read as a card that forgot to show its numbers.
 */
@Composable
private fun SingleProposal(action: CoachAction) {
    when (action) {
        is CoachAction.LogFood -> {
            ProposalKicker(
                stringResource(
                    R.string.coach_proposal_food_title,
                    stringResource(action.mealType.labelRes),
                ),
            )
            ProposalHeadline(action.name)
            ReceiptPanel {
                ReceiptHeadline(
                    label = stringResource(R.string.coach_receipt_calories),
                    value = action.calories.toString(),
                    unit = stringResource(R.string.coach_unit_kcal),
                )
                MacroColumns(
                    proteinG = action.proteinG,
                    carbsG = action.carbsG,
                    fatG = action.fatG,
                )
            }
        }

        is CoachAction.LogExercise -> {
            ProposalKicker(stringResource(R.string.coach_proposal_exercise_title))
            ProposalHeadline(activityName(action))
            ReceiptPanel {
                ReceiptHeadline(
                    label = stringResource(R.string.coach_receipt_minutes),
                    value = action.minutes.toString(),
                    unit = stringResource(R.string.coach_unit_min),
                )
                // The app's own MET estimate off the user's latest weigh-in, never the model's —
                // labelled "burned" so it can never be read as calories eaten.
                ReceiptLine(
                    label = stringResource(R.string.coach_receipt_burned),
                    value = stringResource(R.string.coach_proposal_row_kcal, action.burnedKcal),
                )
            }
        }

        is CoachAction.LogWater -> {
            ProposalKicker(stringResource(R.string.coach_proposal_water_title))
            ProposalHeadline(waterAmount(action.glasses))
        }

        is CoachAction.LogWeight -> {
            ProposalKicker(stringResource(R.string.coach_proposal_weight_title))
            ReceiptPanel {
                ReceiptHeadline(
                    label = stringResource(R.string.coach_receipt_weight),
                    value = formatOneDecimal(action.weight),
                    unit = action.unit.weightUnitLabel(),
                )
                weightChange(action)?.let {
                    ReceiptLine(label = it, value = null)
                }
            }
        }

        is CoachAction.LogSupplement -> {
            ProposalKicker(stringResource(R.string.coach_proposal_supplement_title))
            ProposalHeadline(action.name)
            ReceiptPanel {
                ReceiptLine(
                    label = stringResource(R.string.coach_receipt_dose),
                    value = supplementDoses(action.doses),
                )
            }
        }

        is CoachAction.LogMood -> {
            ProposalKicker(stringResource(R.string.coach_proposal_mood_title))
            ReceiptPanel {
                // The mood is the first line when there is one; a draft naming only the energy
                // makes the energy the first line rather than leaving a blank one above it.
                val mood = moodLine(action.mood)
                val energy = energyLine(action.energy)
                ReceiptLine(label = mood ?: energy.orEmpty(), value = null)
                if (mood != null) energy?.let { ReceiptLine(label = it, value = null) }
            }
        }

        is CoachAction.LogBloodPressure -> {
            ProposalKicker(stringResource(R.string.coach_proposal_bp_title))
            ReceiptPanel {
                ReceiptHeadline(
                    label = stringResource(R.string.coach_receipt_reading),
                    value = formatBloodPressure(action.systolic, action.diastolic),
                )
                ReceiptLine(label = bandLine(action), value = null)
            }
        }

        is CoachAction.LogMeasurement -> {
            ProposalKicker(
                stringResource(
                    R.string.coach_proposal_measurement_title,
                    stringResource(action.part.label),
                ),
            )
            ReceiptPanel {
                ReceiptHeadline(
                    label = stringResource(action.part.label),
                    value = formatOneDecimal(action.value),
                    unit = action.part.unitLabel(action.unit),
                )
            }
        }

        is CoachAction.LogNote -> {
            ProposalKicker(stringResource(R.string.coach_proposal_note_title))
            ReceiptPanel {
                ReceiptProse(action.text)
                // What the tap is about to overwrite, because `setNote` replaces rather than
                // appends. Two lines of it, under a label: enough to recognise the sentence being
                // lost, not so much that it competes with the one being written.
                if (action.replaces.isNotEmpty()) {
                    ReceiptLine(
                        label = stringResource(R.string.coach_proposal_note_replaces),
                        value = null,
                    )
                    ReceiptProse(action.replaces, maxLines = 2, muted = true)
                }
            }
        }

        is CoachAction.StartRoutine -> {
            ProposalKicker(stringResource(R.string.coach_proposal_routine_title))
            ProposalHeadline(action.name)
            // Every lift, not the first few — the one panel here that shows no figures the app
            // will write, because it writes none. A user deciding whether to start a session needs
            // to see what is in it.
            if (action.lifts.isNotEmpty()) {
                ReceiptPanel {
                    action.lifts.forEach { lift ->
                        ReceiptLine(
                            label = lift.exerciseName,
                            value = stringResource(
                                R.string.coach_proposal_routine_sets,
                                lift.sets,
                                lift.reps,
                            ),
                        )
                    }
                }
            }
        }

        // The goal on a start, the elapsed time on an end. Neither is the model's: a start takes
        // the profile's target and an end takes the running fast's own snapshotted one, both
        // stamped by `resolve`. The elapsed figure says "so far" because it is the one number on
        // any of these cards that nothing writes — the end is stamped at the tap.
        is CoachAction.SetFast -> {
            if (action.ending) {
                ProposalKicker(stringResource(R.string.coach_proposal_fast_end_title))
                ReceiptPanel {
                    ReceiptLine(
                        label = stringResource(R.string.coach_receipt_elapsed),
                        value = fastElapsed(action),
                    )
                }
            } else {
                ProposalKicker(stringResource(R.string.coach_proposal_fast_start_title))
                ReceiptPanel {
                    ReceiptLine(
                        label = stringResource(R.string.coach_receipt_goal),
                        value = fastGoal(action),
                    )
                }
            }
        }

        // A correction draws the row before and after, every figure the tap will write.
        is CoachAction.EditFood -> {
            ProposalKicker(stringResource(R.string.coach_proposal_edit_title))
            ProposalHeadline((action.after ?: action.before)?.name.orEmpty())
            ReceiptPanel {
                action.before?.let { ReceiptLine(stringResource(R.string.coach_receipt_before), foodLine(it)) }
                action.after?.let { ReceiptLine(stringResource(R.string.coach_receipt_after), foodLine(it)) }
            }
        }
        is CoachAction.EditExercise -> {
            ProposalKicker(stringResource(R.string.coach_proposal_edit_title))
            ProposalHeadline((action.after ?: action.before)?.let { exerciseName(it) }.orEmpty())
            ReceiptPanel {
                action.before?.let { ReceiptLine(stringResource(R.string.coach_receipt_before), exerciseLine(it)) }
                action.after?.let { ReceiptLine(stringResource(R.string.coach_receipt_after), exerciseLine(it)) }
            }
        }
        is CoachAction.DeleteFood -> {
            ProposalKicker(stringResource(R.string.coach_proposal_delete_title))
            ProposalHeadline(action.entry?.name.orEmpty())
            action.entry?.let { ReceiptPanel { ReceiptLine(stringResource(R.string.coach_receipt_calories), foodLine(it)) } }
        }
        is CoachAction.DeleteExercise -> {
            ProposalKicker(stringResource(R.string.coach_proposal_delete_title))
            ProposalHeadline(action.entry?.let { exerciseName(it) }.orEmpty())
            action.entry?.let { ReceiptPanel { ReceiptLine(stringResource(R.string.coach_receipt_minutes), exerciseLine(it)) } }
        }
        is CoachAction.SetWater -> {
            ProposalKicker(stringResource(R.string.coach_proposal_set_water_title))
            ProposalHeadline(waterAmount(action.glasses))
            ReceiptPanel {
                ReceiptLine(stringResource(R.string.coach_receipt_before), waterAmount(action.previous))
            }
        }

        // Nothing to write and nothing to show but where it goes.
        is CoachAction.OpenScreen -> {
            ProposalKicker(stringResource(R.string.coach_proposal_open_title))
            ProposalHeadline(stringResource(screenLabel(action.screen)))
        }

        // `resolve()` turns a saved meal into its own rows before any card is drawn, so this is
        // only ever reached if that stops being true. It renders the name rather than nothing,
        // which stays honest: the name is the whole of what the model supplied.
        is CoachAction.LogSavedMeal -> {
            ProposalKicker(
                stringResource(
                    R.string.coach_proposal_food_title,
                    stringResource(action.mealType.labelRes),
                ),
            )
            ProposalHeadline(action.name)
        }
    }
}

/**
 * The list, with what is left of it totalled underneath.
 *
 * A removed row is *gone* rather than struck through: the card's promise is that what it shows is
 * what gets written, and a greyed row still on screen is a row the eye counts. [kept] carries the
 * original indices in order, so removing the middle row does not renumber the rest — and the undo
 * line on the card is what makes that removal cheap enough to be irreversible-looking.
 */
@Composable
private fun MultiProposal(actions: List<CoachAction>, kept: List<Int>, onRemove: (Int) -> Unit) {
    ProposalHeadline(stringResource(R.string.coach_proposal_items_title, kept.size))
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        kept.forEach { index ->
            val action = actions[index]
            val name = actionName(action)
            val food = action as? CoachAction.LogFood
            ReceiptRow(
                name = name,
                detail = rowDetail(action).takeIf { food == null },
                macros = food?.let { Triple(it.proteinG, it.carbsG, it.fatG) },
                trailing = food?.calories?.toString(),
                onRemove = { onRemove(index) },
                removeLabel = stringResource(R.string.coach_proposal_remove, name),
            )
        }

        // Only the foods, and only when there are some: a workout's calories are *burned* and a
        // glass of water has none, so summing either into one "kcal" figure would print a number
        // that is true of nothing.
        val foods = kept.map { actions[it] }.filterIsInstance<CoachAction.LogFood>()
        if (foods.isNotEmpty()) {
            ReceiptTotal(
                count = kept.size,
                calories = foods.sumOf { it.calories },
                proteinG = foods.sumOf { it.proteinG },
                carbsG = foods.sumOf { it.carbsG },
                fatG = foods.sumOf { it.fatG },
            )
        }
    }
}

/** A plain label/value line on the receipt panel — the figures that are not the headline, and the
 * lines (a band, a mood) that are a label with nothing to put beside them. */
@Composable
private fun ReceiptLine(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.tabularNums,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Prose on the receipt panel, where every other line is a label against a figure.
 *
 * A note is the one drafted thing that is not a number, so it wraps instead of sitting on a
 * baseline beside one. [maxLines] and [muted] are what let the note this draft is about to
 * *replace* sit under it as context rather than competing with the sentence that gets written.
 */
@Composable
private fun ReceiptProse(text: String, maxLines: Int = Int.MAX_VALUE, muted: Boolean = false) = Text(
    text = text,
    style = MaterialTheme.typography.bodyMedium,
    color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant
    else MaterialTheme.colorScheme.onSurface,
    maxLines = maxLines,
    overflow = TextOverflow.Ellipsis,
)

/** Where this is going — the meal a food joins, or the weigh-in it becomes. A kicker over the
 * title rather than a title in its own right: the *thing* is the headline and the destination
 * qualifies it. The day no longer rides here; it is a chip in [DraftHeader], where a qualifier on
 * the whole card belongs. */
@Composable
private fun ProposalKicker(text: String) = Text(
    text = text,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.onTertiaryContainer,
)

@Composable
private fun ProposalHeadline(text: String) = Text(
    text = text,
    style = MaterialTheme.typography.titleLarge,
    color = MaterialTheme.colorScheme.onTertiaryContainer,
    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
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
    is CoachAction.LogMood -> moodName(action)
    is CoachAction.LogBloodPressure ->
        formatBloodPressure(action.systolic, action.diastolic)
    is CoachAction.LogMeasurement -> stringResource(action.part.label)
    // The sentence itself: there is nothing else to call a note, and the row it sits in is the
    // one place it can be read before the tap.
    is CoachAction.LogNote -> action.text
    // Never drawn in a list — `navigatingDraftStandsAlone()` is what guarantees a routine is the
    // whole draft — but the name is the right answer if that ever stops being true.
    is CoachAction.StartRoutine -> action.name
    // Drawn in a list when a fast rides beside rows — "I broke my fast with two eggs" — which is
    // the whole reason it is not held to a routine's stand-alone rule.
    is CoachAction.SetFast -> stringResource(
        if (action.ending) R.string.coach_proposal_fast_end_title
        else R.string.coach_proposal_fast_start_title,
    )
    // Never drawn in a list, for a routine's reason.
    is CoachAction.OpenScreen -> stringResource(screenLabel(action.screen))
    is CoachAction.EditFood -> (action.after ?: action.before)?.name.orEmpty()
    is CoachAction.EditExercise -> (action.after ?: action.before)?.let { exerciseName(it) }.orEmpty()
    is CoachAction.DeleteFood -> action.entry?.name.orEmpty()
    is CoachAction.DeleteExercise -> action.entry?.let { exerciseName(it) }.orEmpty()
    is CoachAction.SetWater -> waterAmount(action.glasses)
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
    // The name already carries both columns, so there is nothing left to put on the right.
    is CoachAction.LogMood -> null
    is CoachAction.LogBloodPressure -> bandLine(action)
    is CoachAction.LogMeasurement -> measurementAmount(action)
    // The text already is the row, and what it replaces is the card's business rather than a
    // figure to put on the right.
    is CoachAction.LogNote -> null
    is CoachAction.StartRoutine -> routineLifts(action)
    is CoachAction.SetFast -> if (action.ending) fastElapsed(action) else fastGoal(action)
    is CoachAction.OpenScreen -> null
    is CoachAction.EditFood -> action.before?.let { before ->
        action.after?.let { stringResource(R.string.coach_proposal_edit_row, before.calories, it.calories) }
    }
    is CoachAction.EditExercise -> action.before?.let { before ->
        action.after?.let { stringResource(R.string.coach_proposal_edit_minutes_row, before.minutes, it.minutes) }
    }
    is CoachAction.DeleteFood, is CoachAction.DeleteExercise -> stringResource(R.string.coach_proposal_delete_row)
    is CoachAction.SetWater -> stringResource(R.string.coach_proposal_was, waterAmount(action.previous))
}

/** A food's figure and portion, the two things an edit changes. */
@Composable
private fun foodLine(entry: FoodEntry): String = stringResource(
    R.string.coach_proposal_food_line,
    entry.calories,
    formatOneDecimal(entry.portionAmount),
    entry.portionUnit,
)

@Composable
private fun exerciseName(entry: ExerciseEntry): String = entry.name.ifEmpty { stringResource(entry.type.label) }

@Composable
private fun exerciseLine(entry: ExerciseEntry): String =
    stringResource(R.string.coach_proposal_exercise_body, entry.minutes, entry.burnedKcal)

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
        single is CoachAction.LogMood ->
            stringResource(R.string.coach_proposal_logged_mood, moodName(single))
        single is CoachAction.LogBloodPressure -> stringResource(
            R.string.coach_proposal_logged_bp,
            formatBloodPressure(single.systolic, single.diastolic),
        )
        single is CoachAction.LogMeasurement -> stringResource(
            R.string.coach_proposal_logged_measurement,
            stringResource(single.part.label),
            measurementAmount(single),
        )
        // Not the note itself: a receipt is one line under a reopened answer, and five hundred
        // characters of the user's own sentence is the card again rather than a line about it.
        single is CoachAction.LogNote ->
            stringResource(R.string.coach_proposal_logged_note)
        // Nothing was logged: the tap opened a form the user has not saved yet, and a "Logged:"
        // line under the answer would be a claim the app cannot stand behind. The turn is
        // persisted with the coach's own prose, exactly as a dismissal is.
        single is CoachAction.StartRoutine -> ""
        // The routine's reason: the tap changed nothing, so there is nothing to report.
        single is CoachAction.OpenScreen -> ""
        single is CoachAction.EditFood -> single.after
            ?.let { stringResource(R.string.coach_proposal_changed_food, it.name, it.calories) }.orEmpty()
        single is CoachAction.EditExercise -> single.after
            ?.let { stringResource(R.string.coach_proposal_changed_exercise, exerciseName(it), it.minutes) }.orEmpty()
        single is CoachAction.DeleteFood ->
            stringResource(R.string.coach_proposal_removed_entry, single.entry?.name.orEmpty())
        single is CoachAction.DeleteExercise ->
            stringResource(R.string.coach_proposal_removed_entry, single.entry?.let { exerciseName(it) }.orEmpty())
        single is CoachAction.SetWater ->
            stringResource(R.string.coach_proposal_changed_water, waterAmount(single.glasses))
        actions.any { it.isChange } ->
            pluralStringResource(R.plurals.coach_proposal_changed_items, actions.size, actions.size)
        // Unlike a routine this *did* change something, so it says so — and it says the figure the
        // tap settled on rather than a total, because a fast has none.
        single is CoachAction.SetFast && single.ending -> stringResource(
            R.string.coach_proposal_logged_fast_ended,
            formatDuration(single.elapsedMinutes),
        )
        single is CoachAction.SetFast ->
            stringResource(R.string.coach_proposal_logged_fast_started, single.goalHours)
        else -> pluralStringResource(
            R.plurals.coach_proposal_logged_items,
            actions.size,
            actions.size,
            actions.filterIsInstance<CoachAction.LogFood>().sumOf { it.calories },
        )
    }
}

/** The target a start will run to — the profile's, stamped by `resolve`, never the model's. */
@Composable
private fun fastGoal(action: CoachAction.SetFast): String =
    stringResource(R.string.coach_proposal_fast_goal, action.goalHours)

/** How long the fast has run. `formatDuration` because a duration is written one way in this app,
 * and "so far" because this one is still growing while the card sits there. */
@Composable
private fun fastElapsed(action: CoachAction.SetFast): String =
    stringResource(R.string.coach_proposal_fast_elapsed, formatDuration(action.elapsedMinutes))

/**
 * The figure the user gave, in the unit their profile uses and **not converted** — this is the
 * number `settle` is about to write, and the card's promise is that they are the same one.
 */
@Composable
private fun weightAmount(action: CoachAction.LogWeight): String = stringResource(
    R.string.coach_proposal_weight_body,
    formatOneDecimal(action.weight),
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
        (if (delta > 0) "+" else "\u2212") + formatOneDecimal(abs(delta)),
        action.unit.weightUnitLabel(),
    )
}

/**
 * What the workout opens with — "Bench press 3x8 · Squat 3x5".
 *
 * Every lift, not the first few: this is the card's usual promise in the one place it is not about
 * figures being *written*, and a user deciding whether to start a session needs to see what is in
 * it. Null for a routine with no lifts, the rule the water row follows.
 *
 * `map` before the join because it is inline and `joinToString`'s transform is not — a
 * `stringResource` cannot be read from the latter. The separator is punctuation, not copy, the
 * same reading `Routine.dayLabel()` gives its own.
 */
@Composable
private fun routineLifts(action: CoachAction.StartRoutine): String? = action.lifts
    .map { stringResource(R.string.coach_proposal_routine_lift, it.exerciseName, it.sets, it.reps) }
    .takeIf { it.isNotEmpty() }
    ?.joinToString(" \u00b7 ")

/** An unnamed activity is called after its type — what [ph.mart.healthapp.core.data.exercise.ExerciseEntry]
 * means by an empty name, resolved here because only a composable can read the enum's label. */
@Composable
private fun activityName(action: CoachAction.LogExercise): String =
    action.name.ifEmpty { stringResource(action.type.label) }

/**
 * The five names [MoodLevel] already carries — and they are the *energy* control's names too, which
 * is what its own resource comment says, so the card and the Mood page cannot drift apart.
 *
 * Null for `0`, which is [ph.mart.healthapp.core.data.mood.MoodDay]'s "not set": a draft naming
 * only the mood leaves the energy line off rather than printing a zero.
 */
@Composable
private fun levelName(level: Int): String? =
    MoodLevel.entries.firstOrNull { it.value == level }?.let { stringResource(it.label) }

@Composable
private fun moodLine(level: Int): String? =
    levelName(level)?.let { stringResource(R.string.coach_proposal_mood_body, it) }

@Composable
private fun energyLine(level: Int): String? =
    levelName(level)?.let { stringResource(R.string.coach_proposal_mood_energy, it) }

/** Both columns on one line — what a list row and the logged line need, where the single card has
 * two lines to spend. Never empty: the parse refuses a draft with neither column set. */
@Composable
private fun moodName(action: CoachAction.LogMood): String {
    val mood = moodLine(action.mood)
    val energy = energyLine(action.energy)
    return when {
        mood != null && energy != null ->
            stringResource(R.string.coach_proposal_mood_both, mood, energy)
        else -> mood ?: energy.orEmpty()
    }
}

/**
 * **The app's band, not the model's.** [categoryOf] is worst-first and is the same call the Blood
 * pressure card and the prompt's own payload make, so the label under the figure here is the label
 * the user will see on the page the tap writes to. The pulse joins it when the cuff showed
 * one and the user said it.
 */
@Composable
private fun bandLine(action: CoachAction.LogBloodPressure): String {
    val band = stringResource(categoryOf(action.systolic, action.diastolic).label)
    if (action.pulseBpm <= 0) return band
    return stringResource(R.string.coach_proposal_bp_band_pulse, band, formatBpm(action.pulseBpm))
}

/** The figure the user gave and the unit their profile uses — not converted, for [weightAmount]'s
 * reason: this is the number `settle` is about to turn into what the table stores. */
@Composable
private fun measurementAmount(action: CoachAction.LogMeasurement): String = stringResource(
    R.string.coach_proposal_measurement_body,
    formatOneDecimal(action.value),
    action.part.unitLabel(action.unit),
)

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

/** Both columns set: the mood is the headline and the energy the line under it. */
@PreviewLightDark
@Composable
private fun ProposalCardMoodPreview() {
    AppTheme {
        Surface {
            ProposalCard(
                actions = listOf(CoachAction.LogMood(mood = 4, energy = 2)),
                onConfirm = { _, _ -> },
                onDismiss = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** The band is `categoryOf()`'s — 118/76 is Normal — and the pulse joins it when it was given. */
@PreviewLightDark
@Composable
private fun ProposalCardBloodPressurePreview() {
    AppTheme {
        Surface {
            ProposalCard(
                actions = listOf(
                    CoachAction.LogBloodPressure(systolic = 118, diastolic = 76, pulseBpm = 64),
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
private fun ProposalCardMeasurementPreview() {
    AppTheme {
        Surface {
            ProposalCard(
                actions = listOf(
                    CoachAction.LogMeasurement(
                        part = MeasurementPart.Waist,
                        value = 82.5,
                        unit = UnitSystem.Metric,
                    ),
                ),
                onConfirm = { _, _ -> },
                onDismiss = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** The sentence is the user's own, and the card is the only place it can be read before the tap. */
@PreviewLightDark
@Composable
private fun ProposalCardNotePreview() {
    AppTheme {
        Surface {
            ProposalCard(
                actions = listOf(CoachAction.LogNote(text = "Rough day — slept badly and skipped the gym.")),
                onConfirm = { _, _ -> },
                onDismiss = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** A day that already has a note: what the tap replaces is drawn under what it writes. */
@PreviewLightDark
@Composable
private fun ProposalCardNoteReplacesPreview() {
    AppTheme {
        Surface {
            ProposalCard(
                actions = listOf(
                    CoachAction.LogNote(
                        text = "Felt much better after the walk.",
                        replaces = "Rough day — slept badly and skipped the gym.",
                    ),
                ),
                onConfirm = { _, _ -> },
                onDismiss = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** The one card whose Confirm writes nothing: it says "Start it", and every lift on it is the
 * user's own. */
@PreviewLightDark
@Composable
private fun ProposalCardRoutinePreview() {
    AppTheme {
        Surface {
            ProposalCard(
                actions = listOf(
                    CoachAction.StartRoutine(
                        name = "Push day",
                        routineId = 1,
                        lifts = listOf(
                            RoutineLift("Bench press", sets = 3, reps = 8),
                            RoutineLift("Overhead press", sets = 3, reps = 8),
                            RoutineLift("Triceps pushdown", sets = 3, reps = 12),
                        ),
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
private fun ProposalCardEditPreview() {
    val rice = FoodEntry(
        id = 42,
        name = "Rice",
        mealType = MealType.Lunch,
        portionAmount = 1.0,
        portionUnit = "cup",
        calories = 200,
        proteinG = 4,
        carbsG = 44,
        fatG = 0,
    )
    AppTheme {
        Surface {
            ProposalCard(
                actions = listOf(
                    CoachAction.EditFood(
                        entryId = 42,
                        portionAmount = 0.5,
                        before = rice,
                        after = rice.copy(portionAmount = 0.5, calories = 100, proteinG = 2, carbsG = 22),
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
private fun ProposalCardOpenScreenPreview() {
    AppTheme {
        Surface {
            ProposalCard(
                actions = listOf(CoachAction.OpenScreen(CoachScreen.Sleep)),
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

/** A fast about to start: the goal is the profile's, and the button is the routine's verb because
 * a start is a start. */
@PreviewLightDark
@Composable
private fun ProposalCardFastStartPreview() {
    AppTheme {
        Surface {
            ProposalCard(
                actions = listOf(CoachAction.SetFast(ending = false, goalHours = 16)),
                onConfirm = { _, _ -> },
                onDismiss = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** And the other half of the one card that confirms a state: the elapsed time says "so far"
 * because it is still growing while the card sits there. */
@PreviewLightDark
@Composable
private fun ProposalCardFastEndPreview() {
    AppTheme {
        Surface {
            ProposalCard(
                actions = listOf(
                    CoachAction.SetFast(ending = true, goalHours = 16, elapsedMinutes = 972),
                ),
                onConfirm = { _, _ -> },
                onDismiss = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** What a screen is called on the card. `:core:data` cannot hold a label, and the Progress
 * subjects' own labels are `:feature:progress`'s, so this is the coach's own list. */
@StringRes
internal fun screenLabel(screen: CoachScreen): Int = when (screen) {
    CoachScreen.Home -> R.string.coach_screen_home
    CoachScreen.Diary -> R.string.coach_screen_diary
    CoachScreen.Progress -> R.string.coach_screen_progress
    CoachScreen.Profile -> R.string.coach_screen_profile
    CoachScreen.FoodLibrary -> R.string.coach_screen_food_library
    CoachScreen.Routines -> R.string.coach_screen_routines
    CoachScreen.SupplementList -> R.string.coach_screen_supplement_list
    CoachScreen.HealthConnections -> R.string.coach_screen_health_connections
    CoachScreen.HomeLayout -> R.string.coach_screen_home_layout
    CoachScreen.Weight -> R.string.coach_screen_weight
    CoachScreen.Photos -> R.string.coach_screen_photos
    CoachScreen.Measurements -> R.string.coach_screen_measurements
    CoachScreen.Nutrition -> R.string.coach_screen_nutrition
    CoachScreen.Water -> R.string.coach_screen_water
    CoachScreen.Fasting -> R.string.coach_screen_fasting
    CoachScreen.Supplements -> R.string.coach_screen_supplements
    CoachScreen.Activity -> R.string.coach_screen_activity
    CoachScreen.Strength -> R.string.coach_screen_strength
    CoachScreen.Sleep -> R.string.coach_screen_sleep
    CoachScreen.Mood -> R.string.coach_screen_mood
    CoachScreen.Cycle -> R.string.coach_screen_cycle
    CoachScreen.Heart -> R.string.coach_screen_heart
    CoachScreen.BloodPressure -> R.string.coach_screen_blood_pressure
    CoachScreen.Badges -> R.string.coach_screen_badges
}
