package ph.mart.healthapp.core.data.exercise

/**
 * Below this a credit is not worth a sentence: it is inside [estimateBurnedKcal]'s own error, and
 * a ten-minute stroll announcing itself is the noise that makes the real ones stop landing.
 *
 * It is the floor for the three lines below — the one wording source for every surface that says
 * what the day's burn *bought*. They sit beside [budgetKcal] because that is the arithmetic they
 * describe: the credit they name is its one and only fold-in point, so a line here can never
 * claim a budget the ring above it did not actually receive. Four surfaces draw from them — the
 * snackbar a saved workout raises, Home's calorie ring and its earned arc, and `insightFor()`'s
 * workout rule — which is what stops them describing one fact three different ways.
 *
 * **Every caller gates on `Profile.addExerciseToBudget`.** With the switch off the burn is
 * genuinely not added, and a sentence saying a workout bought something is then a lie the
 * arithmetic contradicts — `DiarySummaryBar`'s existing rule, applied to the words rather than to
 * a figure.
 */
const val EARNED_MIN_KCAL = 50

/**
 * The largest phrase the credit covers, or null under [EARNED_MIN_KCAL].
 *
 * Hand-written rather than derived from `COMMON_FOODS`: every row there is per 100 g, so pricing
 * a credit against it yields "0.7 of a chicken breast" — a true figure nobody pictures. These are
 * whole portions at roughly their real cost, which is the only thing that makes the translation
 * worth doing at all.
 *
 * ponytail: one flat table, no goal- or diet-awareness, so a vegetarian is offered a burger. A
 * `DietaryPreference` filter is the upgrade path if that gets reported.
 */
fun earnedFoodPhrase(kcal: Int): String? = when {
    kcal >= 700 -> "a burger and fries"
    kcal >= 450 -> "a chicken breast with rice"
    kcal >= 300 -> "a peanut-butter sandwich"
    kcal >= 200 -> "yoghurt and berries"
    kcal >= 120 -> "a banana"
    kcal >= EARNED_MIN_KCAL -> "an apple"
    else -> null
}

/**
 * The snackbar, the moment a workout saves — the one surface here that is a *confirmation of an
 * action the user just took* rather than a report of the day.
 *
 * That is what separates it from the streak celebration `DECISIONS.md` rules out: it fires on a
 * save, not on a threshold, so there is no "already celebrated" state for it to need.
 */
fun earnedSavedLine(kcal: Int): String = earnedFoodPhrase(kcal)
    ?.let { "Nice work — +$kcal kcal on today's budget, about $it." }
    ?: "Nice work — +$kcal kcal on today's budget."

/** The promoted line under Home's calorie ring, beside the arc that draws the same share. */
fun earnedRingLine(kcal: Int): String = earnedFoodPhrase(kcal)
    ?.let { "+$kcal kcal earned — about $it" }
    ?: "+$kcal kcal earned today"

/**
 * `insightFor()`'s workout rule. "Activity", not "workout": the figure it is handed is
 * [dayBurnedKcal][ph.mart.healthapp.core.data.health.dayBurnedKcal], which folds the day's step
 * credit in beside anything logged by hand.
 */
fun earnedInsightLine(kcal: Int): String = earnedFoodPhrase(kcal)
    ?.let { "Today's activity bought you $kcal kcal more than a rest day — about $it." }
    ?: "Today's activity bought you $kcal kcal more than a rest day."
