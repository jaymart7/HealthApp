package ph.mart.healthapp.feature.food.ui.exercise

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.feature.food.ui.diary.FoodScreenState

@Composable
internal fun rememberLogExerciseState(): LogExerciseState =
    rememberSaveable(saver = LogExerciseState.Saver()) { LogExerciseState() }

/** The in-progress form only — the same UI-only-state rule [FoodScreenState] follows. */
internal class LogExerciseState(form: LogExerciseForm = LogExerciseForm()) {
    var form: LogExerciseForm by mutableStateOf(form)

    companion object {
        fun Saver(): Saver<LogExerciseState, Any> = listSaver(
            save = { listOf(it.form.type.name, it.form.name, it.form.minutes, it.form.burnedKcal, it.form.burnedEdited) },
            restore = { saved ->
                LogExerciseState(
                    form = LogExerciseForm(
                        type = ExerciseType.valueOf(saved[0] as String),
                        name = saved[1] as String,
                        minutes = saved[2] as Int,
                        burnedKcal = saved[3] as Int,
                        burnedEdited = saved[4] as Boolean,
                    ),
                )
            },
        )
    }
}
