package ph.mart.healthapp.core.data.health

import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.progress.ChartRange

/** One day of movement, both sources folded — the Progress Activity tab's burn series. */
data class BurnDay(val dateEpochDay: Long, val burnedKcal: Int, val workouts: Int, val minutes: Int)

/**
 * The daily burn series over whatever days either source has, oldest first.
 *
 * Priced with [dayBurnedKcal], the same function Home, the diary and the widget use, so an
 * imported walk cannot be counted twice: its own steps come off the day's total before the
 * remainder is credited.
 *
 * This is what was *burned*, so it ignores `Profile.addExerciseToBudget` — that switch decides
 * whether the burn reaches the calorie budget, not whether it happened.
 */
fun burnSeries(steps: List<StepDay>, exercise: List<ExerciseEntry>): List<BurnDay> {
    val stepsByDay = steps.associateBy { it.dateEpochDay }
    val exerciseByDay = exercise.groupBy { it.dateEpochDay }
    return (stepsByDay.keys + exerciseByDay.keys).sorted().map { day ->
        val entries = exerciseByDay[day].orEmpty()
        BurnDay(
            dateEpochDay = day,
            burnedKcal = dayBurnedKcal(entries, stepsByDay[day]),
            workouts = entries.size,
            minutes = entries.sumOf { it.minutes },
        )
    }
}

/** Anchored to today like [List<StepDay>.inRange], for the same reason. */
fun List<BurnDay>.inRange(range: ChartRange, todayEpochDay: Long): List<BurnDay> {
    val days = range.days ?: return this
    return filter { it.dateEpochDay >= todayEpochDay - days }
}
