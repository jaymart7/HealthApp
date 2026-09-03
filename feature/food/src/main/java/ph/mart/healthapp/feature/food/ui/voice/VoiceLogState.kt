package ph.mart.healthapp.feature.food.ui.voice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.RecognitionConfidence
import ph.mart.healthapp.core.data.food.RecognizedFood
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.defaultMealTypeForNow
import ph.mart.healthapp.feature.food.ui.shared.toAddEntryForm

enum class VoiceFlow { Input, Parsing, Review, NothingHeard, Offline, Failed }

@Composable
internal fun rememberVoiceLogScreen(): VoiceLogScreenState =
    rememberSaveable(saver = VoiceLogScreenState.Saver()) { VoiceLogScreenState() }

/**
 * Screen-local flow/UI state: the sentence, the meal slot it goes into, and the rows the parse came
 * back with while they are being reviewed.
 *
 * The saver carries the *sentence and the slot only*. Restoring to [VoiceFlow.Input] with the words
 * still in the field is coherent — re-estimating is one tap, and the sentence is the part the user
 * actually authored.
 * ponytail: a rotation mid-review loses an unlogged parse, the same flagged simplification
 * `PhotoCaptureScreenState` makes; teach the saver the row list if that is ever reported.
 */
internal class VoiceLogScreenState(
    text: String = "",
    mealType: MealType = defaultMealTypeForNow(),
) {
    var flow: VoiceFlow by mutableStateOf(VoiceFlow.Input)
    var text: String by mutableStateOf(text)
    var mealType: MealType by mutableStateOf(mealType)

    /** The rows as they stand — edited, repriced, some removed. */
    var items: List<AddEntryForm> by mutableStateOf(emptyList())

    /** The parse as it arrived, so [isDirty] can tell an untouched list from a corrected one. */
    private var parsed: List<AddEntryForm> by mutableStateOf(emptyList())

    /** Which row is open for editing — one at a time, so the list stays scannable. */
    var expandedIndex: Int? by mutableStateOf(null)

    /** True when the model flagged any item; the notice is about the batch, since one uncertain
     * portion is a reason to read all of them. */
    var anyLowConfidence: Boolean by mutableStateOf(false)

    /** Non-null exactly while the discard dialog is up — `PhotoCaptureScreenState`'s lambda, for
     * its reason: the Discard *button* leaves the flow, and back steps to the sentence. */
    var pendingDiscard: (() -> Unit)? by mutableStateOf(null)

    val isDirty: Boolean get() = items != parsed

    fun applyParsed(foods: List<RecognizedFood>) {
        val seeded = foods.map { it.toAddEntryForm(mealType) }
        items = seeded
        parsed = seeded
        anyLowConfidence = foods.any { it.confidence == RecognitionConfidence.Low }
        expandedIndex = null
        flow = VoiceFlow.Review
    }

    fun updateItem(index: Int, form: AddEntryForm) {
        items = items.mapIndexed { i, existing -> if (i == index) form else existing }
    }

    fun removeItem(index: Int) {
        items = items.filterIndexed { i, _ -> i != index }
        expandedIndex = null
    }

    fun toggleExpanded(index: Int) {
        expandedIndex = if (expandedIndex == index) null else index
    }

    /** Both lists move, so changing the slot is not an edit to discard — the call
     * `PhotoCaptureScreenState.selectMealType` makes. */
    fun selectMealType(mealType: MealType) {
        this.mealType = mealType
        items = items.map { it.copy(mealType = mealType) }
        parsed = parsed.map { it.copy(mealType = mealType) }
    }

    /** Back to the sentence, keeping it — the rows are what is being thrown away. */
    fun backToInput() {
        items = emptyList()
        parsed = emptyList()
        expandedIndex = null
        flow = VoiceFlow.Input
    }

    companion object {
        fun Saver(): Saver<VoiceLogScreenState, Any> = listSaver(
            save = { listOf(it.text, it.mealType.name) },
            restore = { saved ->
                VoiceLogScreenState(
                    text = saved[0] as String,
                    mealType = MealType.valueOf(saved[1] as String),
                )
            },
        )
    }
}
