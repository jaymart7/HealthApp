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
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.defaultMealTypeForNow
import ph.mart.healthapp.feature.food.ui.shared.toAddEntryForm

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

    /** Forms rather than the model's items, so a nudged portion reprices through the same
     * `withPortionAmount` every other review uses. */
    var foods: List<AddEntryForm> by mutableStateOf(emptyList())
    var exercises: List<ExerciseEntry> by mutableStateOf(emptyList())

    /** The food row whose portion is open — one at a time, talk-to-log's rule. */
    var expandedIndex: Int? by mutableStateOf(null)

    /** Glasses to add, and a weigh-in already in kilograms — null when the sentence said neither. */
    var waterGlasses: Int? by mutableStateOf(null)
    var weightKg: Double? by mutableStateOf(null)

    /** The follow-up waiting for an answer: the model's turn, when it is the last one. */
    val question: String? get() = turns.lastOrNull()?.takeIf { !it.fromUser }?.text

    /** What the user said last, shown above the question it prompted. */
    val lastSaid: String? get() = turns.lastOrNull { it.fromUser }?.text

    /** Everything the user said, as one sentence — what the recents strip offers back. Answers
     * join the sentence they answer ("rice and adobo, two cups"), which is exactly what a re-send
     * needs to skip the question. */
    val userSentence: String get() = turns.filter { it.fromUser }.joinToString(", ") { it.text }

    val hasResult: Boolean
        get() = foods.isNotEmpty() || exercises.isNotEmpty() || waterGlasses != null || weightKg != null

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
        clearResult()
        phase = QuickLogPhase.Input
    }

    /** A slot the user named wins over the time-of-day guess; the chips still win over both. */
    fun applyParsed(
        foods: List<RecognizedFood>,
        exercises: List<ExerciseEntry>,
        mealType: MealType?,
        waterGlasses: Int? = null,
        weightKg: Double? = null,
    ) {
        mealType?.let { this.mealType = it }
        this.foods = foods.map { it.toAddEntryForm(this.mealType) }
        this.exercises = exercises
        this.waterGlasses = waterGlasses
        this.weightKg = weightKg
        expandedIndex = null
        phase = QuickLogPhase.Review
    }

    private fun clearResult() {
        foods = emptyList()
        exercises = emptyList()
        waterGlasses = null
        weightKg = null
        expandedIndex = null
    }

    fun selectMealType(mealType: MealType) {
        this.mealType = mealType
        foods = foods.map { it.copy(mealType = mealType) }
    }

    fun updateFood(index: Int, form: AddEntryForm) {
        foods = foods.mapIndexed { i, existing -> if (i == index) form else existing }
    }

    fun toggleExpanded(index: Int) {
        expandedIndex = if (expandedIndex == index) null else index
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
        clearResult()
        message = null
        phase = QuickLogPhase.Input
    }

    fun removeFood(index: Int) {
        foods = foods.filterIndexed { i, _ -> i != index }
        expandedIndex = null
    }

    fun removeExercise(index: Int) {
        exercises = exercises.filterIndexed { i, _ -> i != index }
    }

    fun removeWater() {
        waterGlasses = null
    }

    fun removeWeight() {
        weightKg = null
    }

    companion object {
        fun Saver(): Saver<QuickLogState, Any> = listSaver(
            save = { listOf(it.text, it.mealType.name) },
            restore = { saved -> QuickLogState(text = saved[0], mealType = MealType.valueOf(saved[1])) },
        )
    }
}
