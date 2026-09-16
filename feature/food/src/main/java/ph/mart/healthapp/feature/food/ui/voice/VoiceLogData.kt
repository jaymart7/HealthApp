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
