package ph.mart.healthapp.feature.profile.ui.routine

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.exercise.Routine
import ph.mart.healthapp.core.data.exercise.RoutineLift

@Composable
internal fun rememberNewRoutineState(): NewRoutineState = rememberSaveable(saver = NewRoutineState.Saver) {
    NewRoutineState()
}

/**
 * The New routine sheet's screen state: whether it is open, the request being typed, and — once a
 * design has answered — the [draft] being previewed, whose name is edited in place. Null [draft]
 * is the describe step. `DescribeState`'s shape, plus the draft.
 *
 * Saveable, draft included: a rotation that threw away a designed routine would cost the user a
 * second request for the same answer.
 */
internal class NewRoutineState(
    open: Boolean = false,
    request: String = "",
    draft: Routine? = null,
    @StringRes message: Int? = null,
) {
    var open: Boolean by mutableStateOf(open)
    var request: String by mutableStateOf(request)
    var draft: Routine? by mutableStateOf(draft)

    /** The line under the field, as a resource id — decided beside a ViewModel call, resolved by
     * the composable. */
    @get:StringRes
    var message: Int? by mutableStateOf(message)

    fun close() {
        open = false
        request = ""
        draft = null
        message = null
    }

    companion object {
        /** The draft as parallel lists, since a [Routine] isn't Bundle-native and four primitives
         * per lift are. */
        @Suppress("UNCHECKED_CAST")
        val Saver: Saver<NewRoutineState, Any> = listSaver(
            save = { state ->
                val lifts = state.draft?.lifts.orEmpty()
                listOf(
                    state.open,
                    state.request,
                    state.message ?: 0,
                    state.draft?.name,
                    state.draft?.days ?: 0,
                    ArrayList(lifts.map { it.exerciseName }),
                    ArrayList(lifts.map { it.sets }),
                    ArrayList(lifts.map { it.reps }),
                )
            },
            restore = { saved ->
                val names = saved[5] as List<String>
                val sets = saved[6] as List<Int>
                val reps = saved[7] as List<Int>
                NewRoutineState(
                    open = saved[0] as Boolean,
                    request = saved[1] as String,
                    message = (saved[2] as Int).takeIf { it != 0 },
                    draft = (saved[3] as String?)?.let { name ->
                        Routine(
                            id = 0,
                            name = name,
                            lifts = names.indices.map { RoutineLift(names[it], sets[it], reps[it]) },
                            days = saved[4] as Int,
                        )
                    },
                )
            },
        )
    }
}
