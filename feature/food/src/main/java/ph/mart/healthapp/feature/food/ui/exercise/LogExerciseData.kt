package ph.mart.healthapp.feature.food.ui.exercise

import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.estimateBurnedKcal

/** [weightKg] is the latest weigh-in, falling back to the onboarding weight — it feeds the MET
 * estimate and nothing else. */
data class LogExerciseUiState(val weightKg: Double = 70.0)

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
)

/** [dateEpochDay] 0 leaves the stamping to the repository, which means today. */
fun LogExerciseForm.toExerciseEntry(dateEpochDay: Long = 0): ExerciseEntry = ExerciseEntry(
    dateEpochDay = dateEpochDay,
    type = type,
    name = name.trim(),
    minutes = minutes,
    burnedKcal = burnedKcal,
    steps = steps,
)

sealed interface LogExerciseEvent {
    /** [dateEpochDay] is the diary's selected day — 0 from the FAB, which is always today.
     * [editingId] names the row being corrected; null logs a new one. */
    data class OnSave(
        val form: LogExerciseForm,
        val dateEpochDay: Long = 0,
        val editingId: Long? = null,
    ) : LogExerciseEvent
}

sealed interface LogExerciseSideEffect {
    data object Saved : LogExerciseSideEffect
}
