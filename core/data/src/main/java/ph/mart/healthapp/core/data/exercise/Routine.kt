package ph.mart.healthapp.core.data.exercise

/**
 * A named workout template and the three pure functions it turns on. Beside the repository
 * interface, for the reason `Exercise.kt` and `Strength.kt` are: nothing here touches Room, so
 * all of it is testable without a database.
 *
 * A routine is the saved-meal pattern applied to workouts — authored by naming a session that is
 * already on screen, re-used by seeding a new one, and never linked to either.
 */
data class Routine(
    val id: Long,
    val name: String,
    val lifts: List<RoutineLift>,
    /** The weekdays it is planned for — see `RoutineEntity.days` and `TrainingPlan.kt`. Defaulted
     * so every construction site that predates the plan stays valid, the `ExerciseEntry.sets`
     * precedent. */
    val days: Int = 0,
)

/** One planned lift. There is deliberately no load: see [RoutineLift]'s entity KDoc and [toSets].
 *
 * ponytail: a per-lift target load is the upgrade path if users ask for one — it would ride here
 * and in `routine_lift`, and [toSets] would prefer it over the last-lifted figure. */
data class RoutineLift(
    val exerciseName: String,
    val sets: Int,
    val reps: Int,
)

fun Routine.totalSets(): Int = lifts.sumOf { it.sets }

/**
 * The one place free-text lift names are matched. Names are typed (the chips only save typing), so
 * "Bench press" and "bench press" are the same lift — `LiftNameChipRow` already compares that way,
 * and everything keyed on a lift here has to agree with it.
 */
fun String.liftKey(): String = trim().lowercase()

/**
 * Collapses a logged set list into the routine it describes: one entry per lift, in the order the
 * lifts were first performed, counting its sets and taking its **most common** rep figure — 8/8/6
 * is a routine of 8s with a set that fell short, not a routine of 6s.
 *
 * Blank names are dropped rather than saved as a nameless lift; the set editor can't produce one
 * (`StrengthSet.canAdd()` requires a name) but a seeded form edited down could.
 */
fun List<StrengthSet>.toRoutineLifts(): List<RoutineLift> = asSequence()
    .filter { it.exerciseName.isNotBlank() }
    .groupBy { it.exerciseName.liftKey() }
    .map { (_, sets) ->
        RoutineLift(
            exerciseName = sets.first().exerciseName.trim(),
            sets = sets.size,
            // eachCount() keeps encounter order and maxByOrNull returns the first maximum, so a
            // tie lands on the rep figure that was hit first.
            reps = sets.groupingBy { it.reps }.eachCount().maxByOrNull { it.value }?.key ?: 0,
        )
    }

/**
 * Expands the routine into the sets to start today with, pricing each lift at **what was last
 * lifted** ([lastLoads], keyed by [liftKey]) and falling back to 0 — bodyweight, which is a real
 * value here — for a lift with no history.
 *
 * That fallback is the whole design: the load moves every week while the rep scheme doesn't, so
 * the routine carries the scheme and the log carries the load. Starting a routine therefore opens
 * at last week's numbers with nothing to maintain, and every seeded set stays editable.
 */
fun Routine.toSets(lastLoads: Map<String, Double>): List<StrengthSet> = lifts.flatMap { lift ->
    List(lift.sets) {
        StrengthSet(
            exerciseName = lift.exerciseName,
            reps = lift.reps,
            weightKg = lastLoads[lift.exerciseName.liftKey()] ?: 0.0,
        )
    }
}
