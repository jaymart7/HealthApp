package ph.mart.healthapp.feature.food.ui

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
)

const val MINUTES_STEP = 5
const val KCAL_STEP = 10

/** Re-estimates unless the user has taken the kcal field over. The single place the form's two
 * halves are kept in sync, so the sheet never has to remember to call both. */
fun LogExerciseForm.withEstimate(weightKg: Double): LogExerciseForm =
    if (burnedEdited) this else copy(burnedKcal = estimateBurnedKcal(type, minutes, weightKg))

fun LogExerciseForm.isValid(): Boolean = minutes > 0

fun LogExerciseForm.toExerciseEntry(): ExerciseEntry = ExerciseEntry(
    type = type,
    name = name.trim(),
    minutes = minutes,
    burnedKcal = burnedKcal,
)

sealed interface LogExerciseEvent {
    data class OnSave(val form: LogExerciseForm) : LogExerciseEvent
}

sealed interface LogExerciseSideEffect {
    data object Saved : LogExerciseSideEffect
}
