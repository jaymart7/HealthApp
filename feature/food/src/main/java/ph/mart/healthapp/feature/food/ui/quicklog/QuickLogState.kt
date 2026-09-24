package ph.mart.healthapp.feature.food.ui.quicklog

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.QuickLogTurn
import ph.mart.healthapp.core.data.food.RecognizedFood
import ph.mart.healthapp.feature.food.ui.shared.defaultMealTypeForNow

enum class QuickLogPhase { Input, Thinking, Review }

@Composable
internal fun rememberQuickLogState(): QuickLogState =
    rememberSaveable(saver = QuickLogState.Saver()) { QuickLogState() }

/**
 * The conversation and what it has produced so far — the user's in-progress edit, so the screen's
 * rather than the container's.
 *
 * The field is emptied into [turns] on every send, and handed back by [restoreLast] whenever that
 * send came to nothing — cancelled, failed, or naming nothing — so a retry never sends the same
 * words twice and a typo is never lost.
 *
 * ponytail: the saver keeps the field and the slot only, `VoiceLogScreenState`'s simplification —
 * a rotation mid-conversation starts it over with nothing typed lost but the answers. Teach it the
 * turns if that is ever reported.
 */
internal class QuickLogState(
    text: String = "",
    mealType: MealType = defaultMealTypeForNow(),
) {
    var phase: QuickLogPhase by mutableStateOf(QuickLogPhase.Input)
    var text: String by mutableStateOf(text)
    var mealType: MealType by mutableStateOf(mealType)
    var turns: List<QuickLogTurn> by mutableStateOf(emptyList())

    /** The line under the conversation — nothing found, failed, offline. Resolved by the screen. */
    @get:StringRes
    var message: Int? by mutableStateOf(null)

    var foods: List<RecognizedFood> by mutableStateOf(emptyList())
    var exercises: List<ExerciseEntry> by mutableStateOf(emptyList())

    /** The follow-up waiting for an answer: the model's turn, when it is the last one. */
    val question: String? get() = turns.lastOrNull()?.takeIf { !it.fromUser }?.text

    /** What the user said last, shown above the question it prompted. */
    val lastSaid: String? get() = turns.lastOrNull { it.fromUser }?.text

    val hasResult: Boolean get() = foods.isNotEmpty() || exercises.isNotEmpty()

    val canSend: Boolean get() = text.isNotBlank() && phase != QuickLogPhase.Thinking

    /** Whether back has a level to step down — a call in flight, or a conversation to drop. */
    val canStepBack: Boolean get() = phase == QuickLogPhase.Thinking || turns.isNotEmpty()

    /** Moves the field into the conversation and returns the whole of it, which is what is sent. */
    fun send(): List<QuickLogTurn> {
        turns = turns + QuickLogTurn(fromUser = true, text = text.trim())
        text = ""
        message = null
        phase = QuickLogPhase.Thinking
        return turns
    }

    /** A question replaces whatever the last answer was: the model has said it was not enough. */
    fun applyQuestion(question: String) {
        turns = turns + QuickLogTurn(fromUser = false, text = question)
        foods = emptyList()
        exercises = emptyList()
        phase = QuickLogPhase.Input
    }

    fun applyParsed(foods: List<RecognizedFood>, exercises: List<ExerciseEntry>) {
        this.foods = foods
        this.exercises = exercises
        phase = QuickLogPhase.Review
    }

    /** The last send came to nothing: its words go back in the field, and whatever was on screen
     * before it — a review, a question — is still there. */
    fun restoreLast(@StringRes message: Int?) {
        turns.lastOrNull()?.takeIf { it.fromUser }?.let { last ->
            text = last.text
            turns = turns.dropLast(1)
        }
        this.message = message
        phase = if (hasResult) QuickLogPhase.Review else QuickLogPhase.Input
    }

    /** Back to a blank start, keeping the first sentence — it is the meal, and the rest was only
     * ever about it. */
    fun startOver() {
        turns.firstOrNull()?.let { text = it.text }
        turns = emptyList()
        foods = emptyList()
        exercises = emptyList()
        message = null
        phase = QuickLogPhase.Input
    }

    fun removeFood(index: Int) {
        foods = foods.filterIndexed { i, _ -> i != index }
    }

    fun removeExercise(index: Int) {
        exercises = exercises.filterIndexed { i, _ -> i != index }
    }

    companion object {
        fun Saver(): Saver<QuickLogState, Any> = listSaver(
            save = { listOf(it.text, it.mealType.name) },
            restore = { saved -> QuickLogState(text = saved[0], mealType = MealType.valueOf(saved[1])) },
        )
    }
}
