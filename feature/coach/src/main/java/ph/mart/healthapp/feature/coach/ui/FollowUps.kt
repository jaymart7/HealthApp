package ph.mart.healthapp.feature.coach.ui

import ph.mart.healthapp.core.data.insight.InsightRequest
import ph.mart.healthapp.feature.coach.R

/**
 * What to ask next, once the coach has answered something.
 *
 * A finished answer is a dead end: the empty conversation gets [STARTERS] and everything after it
 * gets a text field. These are the same idea one turn later, and they are **derived from the day's
 * own numbers rather than generated** — a second model call per turn would double the token cost
 * of every question for chips that a protein gap and a weigh-in already predict. The tap sends the
 * resolved text verbatim, exactly as a starter does, so nothing downstream knows the difference.
 *
 * Every line here is a question the coach can actually answer: the first four off the day payload
 * it is already told about, the rest through `get_day` and `get_history`. A chip that needs a tool
 * the coach does not have is worse than no chip, because the shrug looks like a broken feature.
 *
 * ponytail: a flat ordered rule list, not a ranking. Three is what fits a wrapped row without
 * pushing the input bar off screen, and the order is "about today" before "about the span".
 */
internal const val MAX_FOLLOW_UPS = 3

/** A gap worth a question. Below it the answer is "you're basically there", which no one taps. */
private const val PROTEIN_GAP_G = 20

internal fun followUpsFor(request: InsightRequest?): List<Int> {
    // No profile means no targets, so none of the gap rules can fire — and the coach's own first
    // sentence in that state is that it has none. What is left is the diary, which needs no target
    // to be read back.
    val fromDay = request?.let {
        listOfNotNull(
            R.string.coach_followup_protein.takeIf { _ ->
                it.proteinTargetG - it.proteinG >= PROTEIN_GAP_G
            },
            R.string.coach_starter_dinner.takeIf { _ -> it.caloriesConsumed < it.caloriesTarget },
            R.string.coach_followup_water.takeIf { _ -> it.waterGlasses < it.waterGoalGlasses },
            // Null, not zero, is "nothing to compare against" — asking about a trend that has one
            // reading is asking for a shrug.
            R.string.coach_followup_weight.takeIf { _ -> it.weightDeltaKg != null },
        )
    }.orEmpty()

    // The filler is what keeps the row the same height every turn, and all three read the diary.
    return (fromDay + FOLLOW_UP_FILLER).distinct().take(MAX_FOLLOW_UPS)
}

private val FOLLOW_UP_FILLER = listOf(
    R.string.coach_followup_training,
    R.string.coach_starter_week,
    R.string.coach_starter_yesterday,
)
