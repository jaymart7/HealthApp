package ph.mart.healthapp.core.data.health

import kotlinx.coroutines.flow.Flow
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.estimateBurnedKcal
import ph.mart.healthapp.core.data.exercise.totalBurnedKcal

/** One day of walking, as Home, the diary and the widget show it. */
data class StepDay(val dateEpochDay: Long, val steps: Int, val burnedKcal: Int)

/** "8,432" — the only shape the UI renders, so the formatting lives with the model. The Int
 * overload is what the widget uses: it holds a count, not the whole day. */
fun formatSteps(steps: Int): String = "%,d".format(steps)

fun StepDay.formatSteps(): String = formatSteps(steps)

/**
 * Moderate walking cadence — the same intensity [ExerciseType.Walk]'s MET of 3.5 describes, so
 * converting steps to minutes and back through the MET table stays self-consistent.
 */
const val STEPS_PER_MINUTE = 100

/**
 * What a workout is assumed to have contributed when the watch didn't say. Only the types that
 * actually produce steps: a swim or a strength set can burn a great deal without moving a
 * pedometer, and crediting it steps it never took would subtract a day's real walking.
 */
fun estimatedSteps(type: ExerciseType, minutes: Int): Int = when (type) {
    ExerciseType.Walk, ExerciseType.Run, ExerciseType.Hiit -> (minutes * STEPS_PER_MINUTE).coerceAtLeast(0)
    else -> 0
}

/**
 * Steps to kcal through the existing MET table, so a step credit and a manually logged Walk of
 * the same length agree to the calorie.
 */
fun stepsBurnedKcal(steps: Int, weightKg: Double): Int =
    estimateBurnedKcal(ExerciseType.Walk, steps / STEPS_PER_MINUTE, weightKg)

/**
 * The share of [day]'s stored burn that no logged workout already claims.
 *
 * An imported walk lands in `exercise_entry` and already raises the budget, and the same steps
 * are also in the day's total — so the workout's own step count comes off before anything is
 * credited. Scaled from the stored burn rather than recomputed, so the figure can't drift when
 * the user's weight changes.
 *
 * ponytail: what this does *not* subtract is the walking already priced into
 * `calculateDailyTargets()`'s activity multiplier. `Profile.addExerciseToBudget` is already the
 * user's answer to "is my multiplier covering this?", and steps ride that one switch rather than
 * gaining a second knob. If the credit reads high for sedentary users, the upgrade is a baseline
 * step count per `ActivityLevel`, subtracted here.
 */
fun stepsCreditKcal(day: StepDay?, exercise: List<ExerciseEntry>): Int {
    if (day == null || day.steps <= 0) return 0
    val credited = (day.steps - exercise.sumOf { it.steps }).coerceAtLeast(0)
    return (day.burnedKcal.toLong() * credited / day.steps).toInt()
}

/**
 * The day's whole calorie credit — what every `budgetKcal()` caller passes as its `burnedKcal`.
 * One function so Home, the diary and the widget cannot arrive at different numbers.
 */
fun dayBurnedKcal(exercise: List<ExerciseEntry>, steps: StepDay?): Int =
    exercise.totalBurnedKcal() + stepsCreditKcal(steps, exercise)

/**
 * Read-only by design, exactly like [SleepRepository]: every row comes from Google Health, so
 * there is no `upsert` on the public surface and no manual entry path to keep consistent with one.
 *
 * Not a streak domain — a phone counting steps in a pocket is not "you logged something".
 */
interface StepsRepository {
    /** Today. Null when nothing has been imported for it — the card is hidden, not zeroed. */
    fun observeToday(): Flow<StepDay?>

    /** One day's steps — the diary, which can be pointed at any past day. */
    fun observeSteps(dateEpochDay: Long): Flow<StepDay?>
}
