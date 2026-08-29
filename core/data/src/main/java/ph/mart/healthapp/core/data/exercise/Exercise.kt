package ph.mart.healthapp.core.data.exercise

import kotlin.math.roundToInt

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
    val minutes: Int,
    val burnedKcal: Int,
    /** Steps this activity contributed to the day. See `stepsCreditKcal()` for what it's for. */
    val steps: Int = 0,
)

/**
 * MET values from the Compendium of Physical Activities, rounded to the moderate-intensity entry
 * of each category — the estimate is a starting point the user can overwrite, not a measurement.
 *
 * ponytail: a flat table, not a per-intensity matrix. Add intensity variants only if users say
 * the numbers are off; the kcal field is editable precisely so this table can stay coarse.
 */
enum class ExerciseType(val label: String, val met: Double) {
    Walk("Walk", 3.5),
    Run("Run", 9.8),
    Cycle("Cycle", 7.5),
    Swim("Swim", 7.0),
    Strength("Strength", 5.0),
    Yoga("Yoga", 3.0),
    Hiit("HIIT", 8.0),
    Other("Other", 4.0),
}

/** MET × kg × hours — the standard estimate. [weightKg] is the latest weigh-in when there is one,
 * the onboarding weight otherwise; the caller decides, this only does the arithmetic. */
fun estimateBurnedKcal(type: ExerciseType, minutes: Int, weightKg: Double): Int =
    (type.met * weightKg * minutes / 60.0).roundToInt().coerceAtLeast(0)

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
