package ph.mart.healthapp.core.data.exercise

import androidx.annotation.StringRes
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable
import ph.mart.healthapp.core.data.R

/**
 * One logged activity, and the two pure functions the whole feature turns on. They live beside
 * the repository interface for the same reason `NutritionTrend.kt` and `LoggingStreak.kt` do:
 * nothing here touches Room, so both are testable without a database.
 */
data class ExerciseEntry(
    val id: Long = 0,
    val dateEpochDay: Long = 0,
    val type: ExerciseType,
    val name: String = "",
    /**
     * Null on a **counted** workout — a strength session with [sets], measured in reps rather than
     * time, priced by [burnedKcal] for its sets. Every other activity is **timed** and has one: a
     * jog, a swim, a yoga class, and "45 minutes of weights" with no lifts named. See [isCounted].
     */
    val minutes: Int?,
    val burnedKcal: Int,
    /** Steps this activity contributed to the day. See `stepsCreditKcal()` for what it's for. */
    val steps: Int = 0,
    /**
     * What was lifted, for a strength session — empty for everything else, which is what makes
     * this one field rather than a second kind of workout. A strength entry is an ordinary
     * [ExerciseEntry], so the streak, [budgetKcal] and `burnSeries()` pick it up with no special
     * case, and an imported watch session simply arrives with none.
     */
    val sets: List<StrengthSet> = emptyList(),
)

/**
 * One set of one lift. The exercise's name rides on every set rather than on a level of its own:
 * "the exercises in this workout" is a `groupBy` over these, which is one table fewer than the
 * three a workout/exercise/set hierarchy would need — the call `SavedMealItemEntity` makes with
 * its plain `mealId` column.
 *
 * [weightKg] of 0 is **bodyweight**, and that is a real value, not the "never entered" reading
 * `mood_day`'s zero and `pulseBpm`'s zero have. It counts no volume and claims no personal
 * record, because neither figure means anything without a load — see [estimatedOneRepMax].
 *
 * Serializable because a parsed session rides `StrengthWorkoutRoute`'s key — see [ParsedExercise].
 */
@Serializable
data class StrengthSet(
    val exerciseName: String,
    val reps: Int,
    val weightKg: Double,
)

/**
 * MET values from the Compendium of Physical Activities, rounded to the moderate-intensity entry
 * of each category — the estimate is a starting point the user can overwrite, not a measurement.
 *
 * ponytail: a flat table, not a per-intensity matrix. Add intensity variants only if users say
 * the numbers are off; the kcal field is editable precisely so this table can stay coarse.
 */
enum class ExerciseType(@StringRes val label: Int, val met: Double) {
    Walk(R.string.data_exercise_type_walk, 3.5),
    Run(R.string.data_exercise_type_run, 9.8),
    Cycle(R.string.data_exercise_type_cycle, 7.5),
    Swim(R.string.data_exercise_type_swim, 7.0),
    Strength(R.string.data_exercise_type_strength, 5.0),
    Yoga(R.string.data_exercise_type_yoga, 3.0),
    Hiit(R.string.data_exercise_type_hiit, 8.0),
    Other(R.string.data_exercise_type_other, 4.0),
}

/** MET × kg × hours — the standard estimate. [weightKg] is the latest weigh-in when there is one,
 * the onboarding weight otherwise; the caller decides, this only does the arithmetic. */
fun estimateBurnedKcal(type: ExerciseType, minutes: Int, weightKg: Double): Int =
    (type.met * weightKg * minutes / 60.0).roundToInt().coerceAtLeast(0)

/**
 * Counted in reps rather than timed. Only a strength session that has its sets: that is how a
 * trainer writes push-ups, squats, burpees and every lift down — "3×10", never "4 minutes". Cardio,
 * classes and holds (a jog, a swim, yoga, HIIT, a plank) are measured in time and keep their
 * minutes, and so does a Strength row with no sets, which is what "45 minutes of weights" and an
 * imported watch session are. Holding the rule on the type as well as the sets means no parse can
 * turn a jog into a list of reps.
 */
fun isCounted(type: ExerciseType, sets: List<StrengthSet>): Boolean =
    type == ExerciseType.Strength && sets.isNotEmpty()

val ExerciseEntry.isCounted: Boolean get() = isCounted(type, sets)

/** Seconds a rep takes under load — a controlled one-up, two-down tempo. */
const val REP_SECONDS = 3

/**
 * One set's burn: [ExerciseType.Strength]'s MET over the time the reps took, at the body's weight
 * **plus** what was on the bar, so more reps and more load both cost more. A bodyweight set
 * ([StrengthSet.weightKg] 0) is the body alone. Ten push-ups at 70 kg is about 3 kcal.
 *
 * ponytail: one tempo and one MET for every lift, so a curl and a squat at the same load and reps
 * price alike. A per-lift factor is the upgrade path if users say the figures are off.
 */
fun StrengthSet.burnedKcal(bodyKg: Double): Int =
    (ExerciseType.Strength.met * (bodyKg + weightKg) * reps * REP_SECONDS / 3600.0).roundToInt().coerceAtLeast(0)

/** The sum of each set's rounded figure, so a total always matches the sets drawn above it. */
fun List<StrengthSet>.burnedKcal(bodyKg: Double): Int = sumOf { it.burnedKcal(bodyKg) }

/** The one pricing rule for an activity being logged — its sets when it is counted, MET × minutes
 * when it is timed — so the log sheet, the strength screen, the quick log and the coach agree. */
fun activityBurnedKcal(type: ExerciseType, minutes: Int?, sets: List<StrengthSet>, weightKg: Double): Int =
    if (isCounted(type, sets)) sets.burnedKcal(weightKg) else estimateBurnedKcal(type, minutes ?: 0, weightKg)

/**
 * The day's *displayed* calorie budget. The Mifflin–St Jeor target itself is never touched — this
 * is the only place burned calories are folded in, so Home and the diary cannot drift apart.
 *
 * [addExercise] exists because `calculateDailyTargets()` already multiplies BMR by an activity
 * multiplier: a user who picked "Very active" *because* they train would otherwise be credited
 * for the same workout twice.
 */
fun budgetKcal(targetKcal: Int, burnedKcal: Int, addExercise: Boolean): Int =
    targetKcal + if (addExercise) burnedKcal else 0

fun List<ExerciseEntry>.totalBurnedKcal(): Int = sumOf { it.burnedKcal }
