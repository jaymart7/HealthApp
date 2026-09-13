package ph.mart.healthapp.feature.coach.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
internal fun rememberCoachScreenState(): CoachScreenState =
    rememberSaveable(saver = CoachScreenState.Saver()) { CoachScreenState() }

/**
 * The user's in-progress question and one UI-only flag.
 *
 * [draft] lives here rather than in [CoachUiState] for the reason every form in this app does:
 * it is what the user is editing, not a repository projection, and `rememberSaveable` is what
 * carries it through a rotation. [confirmingClear] is the delete confirmation — a conversation is
 * something the user authored, so clearing it asks first, like a saved meal's delete.
 */
internal class CoachScreenState(
    draft: String = "",
    confirmingClear: Boolean = false,
    prefilled: Boolean = false,
) {
    var draft: String by mutableStateOf(draft)
    var confirmingClear: Boolean by mutableStateOf(confirmingClear)

    /** Whether the route's own question has already been put in the field. Saved, because a
     * rotation would otherwise re-fill a field the user had since cleared or rewritten — and it is
     * an opening move, not a state the screen returns to. */
    var prefilled: Boolean by mutableStateOf(prefilled)

    companion object {
        fun Saver(): Saver<CoachScreenState, Any> = listSaver(
            save = { listOf(it.draft, it.confirmingClear, it.prefilled) },
            restore = { saved ->
                CoachScreenState(
                    draft = saved[0] as String,
                    confirmingClear = saved[1] as Boolean,
                    prefilled = saved[2] as Boolean,
                )
            },
        )
    }
}
