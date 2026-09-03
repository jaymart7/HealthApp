package ph.mart.healthapp.feature.food.ui.voice

import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealParseResult

sealed interface VoiceLogEvent {
    data class OnParse(val text: String) : VoiceLogEvent
    data object OnCancelParse : VoiceLogEvent

    /** The whole reviewed list, in one event — the diary should show the meal appear at once, not
     * a row at a time. */
    data class OnLogMeal(val entries: List<FoodEntry>) : VoiceLogEvent
}

sealed interface VoiceLogSideEffect {
    data class ParseFinished(val result: MealParseResult) : VoiceLogSideEffect
    data object MealLogged : VoiceLogSideEffect
}
