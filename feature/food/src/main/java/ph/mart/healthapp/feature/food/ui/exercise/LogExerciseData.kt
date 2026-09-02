package ph.mart.healthapp.feature.food.ui.exercise

import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.LiftPerformance
import ph.mart.healthapp.core.data.exercise.Routine
import ph.mart.healthapp.core.data.exercise.RoutineLift
import ph.mart.healthapp.core.data.exercise.StrengthSet
import ph.mart.healthapp.core.data.exercise.estimateBurnedKcal
import ph.mart.healthapp.core.data.profile.UnitSystem

/**
 * [weightKg] is the latest weigh-in, falling back to the onboarding weight — it feeds the MET
 * estimate and nothing else.
 *
 * The three fields below are the strength screen's, and are loaded only when it asks
 * ([LogExerciseEvent.OnOpenStrength]) — the sheet shares this container and would otherwise pay
 * for reads it never shows. [editing] is the workout the strength route named by id, which is why
 * it is resolved here rather than handed in like the sheet's `editing` parameter.
 */
data class LogExerciseUiState(
    val weightKg: Double = 70.0,
    val preferredUnit: UnitSystem = UnitSystem.Metric,
    val editing: ExerciseEntry? = null,
    /** The most recent strength session — what "Repeat last workout" seeds from. */
    val lastWorkout: ExerciseEntry? = null,
    val recentLifts: List<String> = emptyList(),
    /** The saved routines, newest first — the "Start a routine" chips. */
    val routines: List<Routine> = emptyList(),
    /** The routine this screen was opened *on*, when Home's plan card started one. Resolved here
     * rather than passed down the back stack, exactly like [editing], and folded into the form's
     * seed so the screen composes once — see [strengthLoaded]. */
    val seedRoutine: Routine? = null,
    /** What each lift looked like the last time it was trained, keyed by
     * [ph.mart.healthapp.core.data.exercise.liftKey]. Drawn under the exercise field as one line,
     * and it is also what a started routine seeds its loads from. */
    val lastLifts: Map<String, LiftPerformance> = emptyMap(),
    /** False until [LogExerciseEvent.OnOpenStrength] has answered. The strength screen holds its
     * content back on it: seeding a `rememberSaveable` form from a row that arrives an emission
     * later would re-key the saver and wipe what the user had already typed. */
    val strengthLoaded: Boolean = false,
) {
    /** [lastLifts] reduced to the one figure a routine needs: what was on the bar. */
    val lastLoads: Map<String, Double> get() = lastLifts.mapValues { it.value.topSet.weightKg }
}

/**
 * [burnedEdited] latches the moment the user touches the kcal stepper: after that, changing the
 * type or duration must not silently overwrite the number they chose.
 */
data class LogExerciseForm(
    val type: ExerciseType = ExerciseType.Walk,
    val name: String = "",
    val minutes: Int = 30,
    val burnedKcal: Int = 0,
    val burnedEdited: Boolean = false,
    /** Carried across an edit untouched — see `ExerciseRepository.updateEntry`. Zero on a new
     * form, which is what makes the repository fill in the estimate on insert. */
    val steps: Int = 0,
    /** What was lifted, in the order it was logged. Empty for every activity but a strength
     * session authored on the strength screen — the sheet never touches it, and carries whatever
     * it was opened with straight back out. */
    val sets: List<StrengthSet> = emptyList(),
)

const val MINUTES_STEP = 5
const val KCAL_STEP = 10

/** Re-estimates unless the user has taken the kcal field over. The single place the form's two
 * halves are kept in sync, so the sheet never has to remember to call both. */
fun LogExerciseForm.withEstimate(weightKg: Double): LogExerciseForm =
    if (burnedEdited) this else copy(burnedKcal = estimateBurnedKcal(type, minutes, weightKg))

fun LogExerciseForm.isValid(): Boolean = minutes > 0

/**
 * Inverse of [toExerciseEntry], for reopening a logged activity to correct it.
 *
 * [burnedEdited] is seeded **true**, and that latch is the whole point: without it [withEstimate]
 * would overwrite the stored figure with a fresh MET estimate at today's weight the instant the
 * sheet opened — which is exactly what "a later weigh-in must not rewrite what a past workout
 * burned" forbids. The number on screen stays the one that was logged until the user moves it.
 */
fun ExerciseEntry.toLogExerciseForm(): LogExerciseForm = LogExerciseForm(
    type = type,
    name = name,
    minutes = minutes,
    burnedKcal = burnedKcal,
    burnedEdited = true,
    steps = steps,
    sets = sets,
)

/** [dateEpochDay] 0 leaves the stamping to the repository, which means today. */
fun LogExerciseForm.toExerciseEntry(dateEpochDay: Long = 0): ExerciseEntry = ExerciseEntry(
    dateEpochDay = dateEpochDay,
    type = type,
    name = name.trim(),
    minutes = minutes,
    burnedKcal = burnedKcal,
    steps = steps,
    sets = sets.map { it.copy(exerciseName = it.exerciseName.trim()) },
)

sealed interface LogExerciseEvent {
    /** [dateEpochDay] is the diary's selected day — 0 from the FAB, which is always today.
     * [editingId] names the row being corrected; null logs a new one. */
    data class OnSave(
        val form: LogExerciseForm,
        val dateEpochDay: Long = 0,
        val editingId: Long? = null,
    ) : LogExerciseEvent

    /** Fired once when the strength screen opens: it resolves the workout being corrected (if
     * [editingId] is non-zero), the session to repeat, the lift-name chips and what each lift was
     * last trained at, in one intent — and starts observing the saved routines. */
    data class OnOpenStrength(val editingId: Long = 0, val routineId: Long = 0) : LogExerciseEvent

    /** Names the workout on screen as a routine. It logs nothing: [OnSave] is still what writes
     * the session, and the two are deliberately independent. */
    data class OnSaveRoutine(val name: String, val lifts: List<RoutineLift>) : LogExerciseEvent
}

sealed interface LogExerciseSideEffect {
    data object Saved : LogExerciseSideEffect
}
