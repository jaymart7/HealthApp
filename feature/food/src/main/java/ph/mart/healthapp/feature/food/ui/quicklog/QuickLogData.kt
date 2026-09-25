package ph.mart.healthapp.feature.food.ui.quicklog

import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.QuickLogTurn
import ph.mart.healthapp.core.data.food.RecognizedFood

/**
 * The two figures a parse is priced and credited against — `LogExerciseUiState`'s, for its
 * reasons — and the sentences that have become meals before, talk-to-log's strip. Everything else
 * is the conversation, which is the user's and lives in [QuickLogState].
 */
data class QuickLogUiState(
    val weightKg: Double = DEFAULT_WEIGHT_KG,
    val addExerciseToBudget: Boolean = true,
    val recentSentences: List<String> = emptyList(),
)

/** Only ever seen before the first profile emission, which lands before anyone can type. */
private const val DEFAULT_WEIGHT_KG = 70.0

sealed interface QuickLogEvent {
    /** The whole conversation, every time — the model is stateless and the corrections only mean
     * something beside what they correct. */
    data class OnSend(val turns: List<QuickLogTurn>) : QuickLogEvent
    data object OnCancel : QuickLogEvent

    /** Both kinds in one event, so a "toast and a run" lands in the diary at once. [sentence] is
     * what the user said, remembered for the recents strip when the log was a meal. */
    data class OnLog(
        val foods: List<FoodEntry>,
        val exercises: List<ExerciseEntry>,
        val sentence: String,
    ) : QuickLogEvent
}

sealed interface QuickLogSideEffect {
    data class Asked(val question: String) : QuickLogSideEffect

    /** [exercises] arrive already priced — the burn is the ViewModel's arithmetic, never the
     * model's figure. */
    data class Parsed(
        val foods: List<RecognizedFood>,
        val exercises: List<ExerciseEntry>,
        val mealType: MealType?,
    ) : QuickLogSideEffect
    data object NothingFound : QuickLogSideEffect
    data object Failed : QuickLogSideEffect

    /** `LogExerciseSideEffect.Saved`'s figure: what the host's snackbar congratulates. */
    data class Logged(val creditedKcal: Int) : QuickLogSideEffect
}
