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

/** [initial] seeds the form when an existing activity is being corrected. It doubles as the
 * saveable key, so opening a different row re-seeds rather than showing the last one's numbers. */
@Composable
internal fun rememberLogExerciseState(initial: LogExerciseForm = LogExerciseForm()): LogExerciseState =
    rememberSaveable(initial, saver = LogExerciseState.Saver()) { LogExerciseState(initial) }

/** The in-progress form only — the same UI-only-state rule [FoodScreenState] follows. */
internal class LogExerciseState(form: LogExerciseForm = LogExerciseForm()) {
    var form: LogExerciseForm by mutableStateOf(form)

    companion object {
        fun Saver(): Saver<LogExerciseState, Any> = listSaver(
            save = {
                listOf(
                    it.form.type.name, it.form.name, it.form.minutes,
                    it.form.burnedKcal, it.form.burnedEdited, it.form.steps,
                )
            },
            restore = { saved ->
                LogExerciseState(
                    form = LogExerciseForm(
                        type = ExerciseType.valueOf(saved[0] as String),
                        name = saved[1] as String,
                        minutes = saved[2] as Int,
                        burnedKcal = saved[3] as Int,
                        burnedEdited = saved[4] as Boolean,
                        steps = saved[5] as Int,
                    ),
                )
            },
        )
    }
}
