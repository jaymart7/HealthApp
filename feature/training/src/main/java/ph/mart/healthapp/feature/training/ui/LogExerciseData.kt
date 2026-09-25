package ph.mart.healthapp.feature.training.ui

import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseParseResult
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.LiftPerformance
import ph.mart.healthapp.core.data.exercise.ParsedExercise
import ph.mart.healthapp.core.data.exercise.Routine
import ph.mart.healthapp.core.data.exercise.RoutineLift
import ph.mart.healthapp.core.data.exercise.StrengthParseResult
import ph.mart.healthapp.core.data.exercise.StrengthSet
import ph.mart.healthapp.core.data.exercise.estimateBurnedKcal
import ph.mart.healthapp.core.data.profile.UnitSystem

/**
 * [weightKg] is the latest weigh-in, falling back to the onboarding weight — it feeds the MET
 * estimate and nothing else. [addExerciseToBudget] is the profile's, and feeds the save
 * confirmation and nothing else.
 *
 * The three fields below are the strength screen's, and are loaded only when it asks
 * ([LogExerciseEvent.OnOpenStrength]) — the sheet shares this container and would otherwise pay
 * for reads it never shows. [editing] is the workout the strength route named by id, which is why
 * it is resolved here rather than handed in like the sheet's `editing` parameter.
 */
data class LogExerciseUiState(
    val weightKg: Double = 70.0,
    val preferredUnit: UnitSystem = UnitSystem.Metric,
    /** From the profile — whether a saved workout actually raises today's budget. Read for one
     * purpose: [LogExerciseSideEffect.Saved]'s figure. With the switch off the credit is zero and
     * the confirmation says nothing, because there is nothing it could truthfully say. */
    val addExerciseToBudget: Boolean = true,
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
    /** True while the describe field's sentence is with the model. The strength screen shares this
     * container and never sets it — it has no describe field, the reason [editing] is loaded on
     * demand rather than always. */
    val parsing: Boolean = false,
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

/**
 * What a parsed sentence does to the form: the three fields the model answered, and nothing else.
 *
 * [LogExerciseForm.burnedEdited] is deliberately left alone rather than set. On a new form it is
 * false, so the caller's [withEstimate] prices the parsed type and duration at the user's own
 * weight — which is the whole reason the model is never asked for a burn. And if the user had
 * already moved the kcal stepper before describing the workout, the latch holds their figure
 * exactly as it holds it against a type chip.
 */
fun LogExerciseForm.withParsed(activity: ParsedExercise): LogExerciseForm = copy(
    type = activity.type,
    name = activity.name,
    minutes = activity.minutes,
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

    /** Fired once when the sheet opens on a row being corrected. The sheet is handed an id
     * rather than a row — it is hosted by `AppScaffold`, whose sheet state is `rememberSaveable`
     * and an `ExerciseEntry` is not — so the row is resolved here, onto [LogExerciseUiState.editing].
     * The sheet holds its form back until that row names the same id, which is the hold-back
     * [LogExerciseUiState.strengthLoaded] is for the strength screen, without a second flag. */
    data class OnLoadEditing(val id: Long) : LogExerciseEvent

    /** Fired once when the strength screen opens: it resolves the workout being corrected (if
     * [editingId] is non-zero), the session to repeat, the lift-name chips and what each lift was
     * last trained at, in one intent — and starts observing the saved routines. */
    data class OnOpenStrength(val editingId: Long = 0, val routineId: Long = 0) : LogExerciseEvent

    /** The sentence in the describe field, on its way to the model. The sheet checks
     * `isOnline()` before firing this one: offline is the sheet's own message and spends nothing,
     * the recheck-at-the-moment-of-the-call rule `NetworkMonitor.isOnline` is written for. */
    data class OnParse(val text: String) : LogExerciseEvent

    /** [OnParse] for the strength screen: a session, parsed into sets. The same `isOnline()` check
     * comes first, and [OnCancelParse] withdraws it too. */
    data class OnParseSets(val text: String) : LogExerciseEvent

    /** Back, or the cancel button, while a parse is in flight. It abandons the call and leaves the
     * sentence in the field — a model can hang, and a spinner with no way out would cost the user
     * what they typed. */
    data object OnCancelParse : LogExerciseEvent

    /** Names the workout on screen as a routine. It logs nothing: [OnSave] is still what writes
     * the session, and the two are deliberately independent. */
    data class OnSaveRoutine(val name: String, val lifts: List<RoutineLift>) : LogExerciseEvent
}

sealed interface LogExerciseSideEffect {
    /**
     * [creditedKcal] is what this save just added to today's budget, and **0 means say nothing**:
     * the profile's switch is off, or the save was a correction to a row logged earlier.
     *
     * It rides the side effect rather than being read back off a repository by whoever shows it,
     * because the one screen that could — `AppScaffold`, which hosts both this sheet and the
     * strength route — has no ViewModel and is not about to grow one for a sentence.
     */
    data class Saved(val creditedKcal: Int) : LogExerciseSideEffect

    /**
     * All three answers on one side effect, `VoiceLogSideEffect.ParseFinished`'s shape and for its
     * reason: what a parse becomes is the *form's*, which is screen state
     * ([LogExerciseState]) rather than the container's, so the result is handed over rather than
     * reduced onto it.
     */
    data class Parsed(val result: ExerciseParseResult) : LogExerciseSideEffect

    /** [Parsed]'s twin for the strength screen, handed over for the same reason: the sets land in
     * the screen's form, not on the container. */
    data class SetsParsed(val result: StrengthParseResult) : LogExerciseSideEffect
}
