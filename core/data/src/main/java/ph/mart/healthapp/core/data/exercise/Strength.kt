package ph.mart.healthapp.core.data.exercise

import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.ChartRange

/**
 * Everything the strength log derives, and nothing it stores. No table, no repository, no schema —
 * the `streak/` and `progress/GoalProjection.kt` shape: every figure here is a fold over the
 * [ExerciseEntry] window the Progress tab already holds, so a personal record can never disagree
 * with the workouts it was computed from and there is nothing to notify off (which is what keeps
 * the no-celebration rule intact).
 *
 * Its own file rather than a few more functions in `Exercise.kt` for the reason `health/Activity.kt`
 * records: JVM erasure puts every top-level `List<T>.inRange` in one file facade.
 */

/** Load × reps, summed. Bodyweight sets (see [StrengthSet]) contribute nothing — there is no load
 * to multiply, and inventing one would make a set of push-ups outweigh a squat. */
fun List<StrengthSet>.volumeKg(): Double = sumOf { it.reps * it.weightKg }

/**
 * Epley's estimate: `w × (1 + reps / 30)`. It is what makes 100 kg × 1 and 80 kg × 8 comparable —
 * ranking records on weight alone would call the first the better lift, and it isn't.
 *
 * Zero for a bodyweight set or a set with no reps: an estimate of the load someone could lift once
 * is meaningless when no load was recorded.
 */
fun estimatedOneRepMax(weightKg: Double, reps: Int): Double =
    if (weightKg <= 0.0 || reps <= 0) 0.0 else weightKg * (1 + reps / 30.0)

fun StrengthSet.estimatedOneRepMaxKg(): Double = estimatedOneRepMax(weightKg, reps)

/** Only the workouts that actually recorded lifts — a cardio row has no volume to chart. */
fun List<ExerciseEntry>.withSets(): List<ExerciseEntry> = filter { it.sets.isNotEmpty() }

/** One day's lifting, both across several sessions and across the lifts inside them. */
data class DayVolume(
    val dateEpochDay: Long,
    val volumeKg: Double,
    val sets: Int,
    val workouts: Int,
)

/** Oldest first, one entry per day that has any lifting on it — a rest day is a gap, and the
 * chart's window is what draws it as one. */
fun List<ExerciseEntry>.volumeByDay(): List<DayVolume> = withSets()
    .groupBy { it.dateEpochDay }
    .toSortedMap()
    .map { (day, entries) ->
        DayVolume(
            dateEpochDay = day,
            volumeKg = entries.sumOf { it.sets.volumeKg() },
            sets = entries.sumOf { it.sets.size },
            workouts = entries.size,
        )
    }

/** Anchored to today like [ph.mart.healthapp.core.data.health.BurnDay]'s, for the same reason:
 * a sparse series headed "1M" has to show the last 30 days with their gaps intact. */
fun List<DayVolume>.inRange(range: ChartRange, todayEpochDay: Long): List<DayVolume> {
    val days = range.days ?: return this
    return filter { it.dateEpochDay >= todayEpochDay - days }
}

/**
 * The best a lift has ever been done, by [estimatedOneRepMax] rather than by weight.
 *
 * [dateEpochDay] is when that best was **first** reached, so repeating it doesn't reset the date —
 * a record is the day it was set, not the last day it was matched.
 */
data class LiftRecord(
    val exerciseName: String,
    val bestWeightKg: Double,
    val bestReps: Int,
    val bestOneRepMaxKg: Double,
    val dateEpochDay: Long,
    val sets: Int,
)

/** One record per lift, most recently set first — the list reads as "what you've been moving". */
fun List<ExerciseEntry>.personalRecords(): List<LiftRecord> = withSets()
    .flatMap { entry -> entry.sets.map { entry.dateEpochDay to it } }
    .groupBy { (_, set) -> set.exerciseName }
    .mapNotNull { (name, dated) ->
        val best = dated
            // Earliest first, so maxByOrNull's first-wins tie break lands on the day it was set.
            .sortedBy { (day, _) -> day }
            .maxByOrNull { (_, set) -> set.estimatedOneRepMaxKg() }
            ?: return@mapNotNull null
        val (day, set) = best
        LiftRecord(
            exerciseName = name,
            bestWeightKg = set.weightKg,
            bestReps = set.reps,
            bestOneRepMaxKg = set.estimatedOneRepMaxKg(),
            dateEpochDay = day,
            sets = dated.size,
        )
    }
    .sortedWith(compareByDescending<LiftRecord> { it.dateEpochDay }.thenBy { it.exerciseName })

/**
 * The last time a lift was trained: the sets of it in the most recent workout that contained it,
 * and the heaviest of those by [estimatedOneRepMax].
 *
 * Distinct from [LiftRecord], which is all-time and answers "how strong am I". This answers "what
 * do I put on the bar today", which is the question actually being asked while a set is typed.
 */
data class LiftPerformance(
    val exerciseName: String,
    val dateEpochDay: Long,
    val topSet: StrengthSet,
    val sets: Int,
)

/**
 * One [LiftPerformance] per lift, keyed by [liftKey] — free-text names, matched the way the chip
 * row matches them.
 *
 * Derived, no table and no query of its own: the strength screen already reads
 * [RECENT_STRENGTH_WORKOUTS] workouts for its repeat seed and its chips, and this is a third fold
 * over that same list.
 *
 * ponytail: that window is the ceiling — a lift untouched for ten sessions reads as new and seeds
 * at bodyweight. A per-lift `MAX(date)` query is the upgrade if a routine user with a long split
 * ever notices.
 */
fun List<ExerciseEntry>.lastPerformances(): Map<String, LiftPerformance> = withSets()
    // Oldest first, so a later workout's entry simply overwrites an earlier one's.
    .sortedBy { it.dateEpochDay }
    .flatMap { entry -> entry.sets.map { entry to it } }
    .filter { (_, set) -> set.exerciseName.isNotBlank() }
    .groupBy { (entry, set) -> set.exerciseName.liftKey() to entry.dateEpochDay }
    .entries
    .associate { (key, dated) ->
        val (name, day) = key
        val sets = dated.map { it.second }
        name to LiftPerformance(
            exerciseName = sets.first().exerciseName.trim(),
            dateEpochDay = day,
            topSet = sets.maxBy { it.estimatedOneRepMaxKg() },
            sets = sets.size,
        )
    }

data class StrengthTotals(val workouts: Int, val sets: Int, val volumeKg: Double)

fun List<ExerciseEntry>.strengthTotals(): StrengthTotals {
    val lifted = withSets()
    return StrengthTotals(
        workouts = lifted.size,
        sets = lifted.sumOf { it.sets.size },
        volumeKg = lifted.sumOf { it.sets.volumeKg() },
    )
}

/** The set editor's suggestion chips: lift names from the newest workouts first, deduplicated.
 * [limit] is a display cap — the name field is free text, and this only saves typing. */
fun List<ExerciseEntry>.recentLiftNames(limit: Int = MAX_LIFT_SUGGESTIONS): List<String> =
    withSets()
        .sortedByDescending { it.dateEpochDay }
        .flatMap { entry -> entry.sets.map { it.exerciseName } }
        .filter { it.isNotBlank() }
        .distinct()
        .take(limit)

/** Same reasoning as `MAX_SUGGESTIONS`: the chips sit above the editor, and a longer row pushes
 * the fields it belongs to off the screen. */
const val MAX_LIFT_SUGGESTIONS = 8

/** How many past workouts the strength screen reads on entry — enough for the repeat seed (the
 * first) and a useful spread of [recentLiftNames], and small enough to be one indexed query. */
const val RECENT_STRENGTH_WORKOUTS = 10

// --- labels ------------------------------------------------------------------------------------
// Here rather than in a feature, for the reason `formatSteps` sits in `health/`: the diary row, the
// strength screen and the Progress tab all print these, and `:feature:*` modules never import each
// other. Unit conversion lives one package over, in `profile/Units.kt`.

/** Trims the pointless ".0" — 60 kg is "60", 62.5 kg is "62.5". */
fun formatLoad(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString() else "%.1f".format(value)

/** "60 kg × 8", or "Bodyweight × 20" when there was no load to name. */
fun StrengthSet.loadLabel(unit: UnitSystem): String =
    if (weightKg <= 0.0) {
        "Bodyweight × $reps"
    } else {
        "${formatLoad(weightKg.kgToDisplayUnit(unit))} ${unit.weightUnitLabel()} × $reps"
    }

/** Volume is a big number, so it gets thousands separators the individual loads don't need. */
fun volumeLabel(volumeKg: Double, unit: UnitSystem): String =
    "%,d %s".format(volumeKg.kgToDisplayUnit(unit).toLong(), unit.weightUnitLabel())

/** The diary row's one line under a strength workout: what it was, not how it went. */
fun List<StrengthSet>.summaryLabel(unit: UnitSystem): String {
    if (isEmpty()) return ""
    val lifts = distinctBy { it.exerciseName }.size
    val volume = volumeKg()
    return listOfNotNull(
        "$lifts ${if (lifts == 1) "exercise" else "exercises"}",
        "$size ${if (size == 1) "set" else "sets"}",
        volumeLabel(volume, unit).takeIf { volume > 0 },
    ).joinToString(" · ")
}

/** "Last: 60 kg × 8 · 3 sets" — the one line under the exercise field that says what to beat. */
fun LiftPerformance.label(unit: UnitSystem): String =
    "Last: ${topSet.loadLabel(unit)} · $sets ${if (sets == 1) "set" else "sets"}"
