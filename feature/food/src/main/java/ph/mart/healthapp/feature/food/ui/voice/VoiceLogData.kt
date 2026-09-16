package ph.mart.healthapp.feature.food.ui.voice

import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealParseResult

/**
 * The only thing talk-to-log holds that outlives the screen: the sentences that have become meals
 * before, offered under a blank field.
 *
 * Everything else the flow works with — the sentence being typed, the slot, the reviewed rows — is
 * the user's in-progress edit and lives in `VoiceLogScreenState`.
 */
data class VoiceLogUiState(val recentSentences: List<String> = emptyList())

/**
 * What the field holds after a phrase comes back from the speech dialog. It **appends** rather than
 * replaces, the call `ChatInputBar.withSpoken` makes for the coach's question and for its reason: a
 * sentence already in the field is the user's, and a mic that ate it would be a worse mistake than
 * one that needs a comma deleted. This is the screen with more to eat — a whole meal, not a
 * half-typed question.
 *
 * It joins with a comma where the coach joins with a space, which is the one place the two differ:
 * a question continues, a meal is a **list**, and "two eggs a black coffee" is a worse thing to
 * hand the parser than "two eggs, a black coffee". A sentence that already ends in its own
 * punctuation keeps it rather than collecting a second one.
 */
// Stays in Kotlin under the pure-function-with-a-test rule: VoiceSentenceTest asserts the joins.
internal fun withSpoken(sentence: String, spoken: String): String {
    val said = spoken.trim()
    val existing = sentence.trimEnd()
    return when {
        said.isEmpty() -> sentence
        existing.isBlank() -> said
        existing.endsWith(",") || existing.endsWith(";") -> "$existing $said"
        else -> "$existing, $said"
    }
}

sealed interface VoiceLogEvent {
    data class OnParse(val text: String) : VoiceLogEvent
    data object OnCancelParse : VoiceLogEvent

    /**
     * The whole reviewed list, in one event — the diary should show the meal appear at once, not
     * a row at a time.
     *
     * [sentence] is what was said, carried alongside the rows it became so the write and the
     * remembering happen together: this is the moment the sentence is proved, and a parse the user
     * backed out of never reaches here.
     */
    data class OnLogMeal(val entries: List<FoodEntry>, val sentence: String) : VoiceLogEvent
}

sealed interface VoiceLogSideEffect {
    data class ParseFinished(val result: MealParseResult) : VoiceLogSideEffect
    data object MealLogged : VoiceLogSideEffect
}
