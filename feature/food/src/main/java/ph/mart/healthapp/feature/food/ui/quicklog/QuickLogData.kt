package ph.mart.healthapp.feature.food.ui.quicklog

import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.QuickLogTurn
import ph.mart.healthapp.core.data.food.RecognizedFood
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.WeightEntry

/**
 * The two figures a parse is priced and credited against — `LogExerciseUiState`'s, for its
 * reasons — and the sentences that have become meals before, talk-to-log's strip. Everything else
 * is the conversation, which is the user's and lives in [QuickLogState].
 */
data class QuickLogUiState(
    val weightKg: Double = DEFAULT_WEIGHT_KG,
    val addExerciseToBudget: Boolean = true,
    val recentSentences: List<String> = emptyList(),
    /** The profile's: a weight said in a sentence is read in it, and the rows print in it. */
    val unit: UnitSystem = UnitSystem.Metric,
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
        val waterGlasses: Int? = null,
        val weightKg: Double? = null,
    ) : QuickLogEvent

    /** Carries the batch it reverses, so a snackbar left over from an earlier log can only ever
     * undo that log — never whichever one the ViewModel wrote last. */
    data class OnUndo(val batch: LoggedBatch) : QuickLogEvent
}

/**
 * What one Log wrote, and enough to take it back: the new rows' ids for the two soft-deleted
 * domains, the water count before the glasses were added, and the day's previous weigh-in — null
 * [weightBefore] with a [weightDay] means there was none, so undo deletes the one written.
 */
data class LoggedBatch(
    val foodIds: List<Long> = emptyList(),
    val exerciseIds: List<Long> = emptyList(),
    val waterBefore: Int? = null,
    val weightDay: Long? = null,
    val weightBefore: WeightEntry? = null,
)

sealed interface QuickLogSideEffect {
    data class Asked(val question: String) : QuickLogSideEffect

    /** [exercises] arrive already priced — the burn is the ViewModel's arithmetic, never the
     * model's figure. */
    data class Parsed(
        val foods: List<RecognizedFood>,
        val exercises: List<ExerciseEntry>,
        val mealType: MealType?,
        val waterGlasses: Int? = null,
        /** Already converted from the profile's unit — the ViewModel's arithmetic, like the burn. */
        val weightKg: Double? = null,
        /** Matched on the phone with no model — the review says so, over rows that are all guesses. */
        val offline: Boolean = false,
    ) : QuickLogSideEffect

    /** [offline] changes what the line says: "nothing edible in that" and "nothing I could match
     * without a connection" are different answers. */
    data class NothingFound(val offline: Boolean = false) : QuickLogSideEffect
    data object Failed : QuickLogSideEffect

    /** `LogExerciseSideEffect.Saved`'s figure — what the host's snackbar congratulates — and the
     * batch its Undo reverses. */
    data class Logged(val creditedKcal: Int, val batch: LoggedBatch) : QuickLogSideEffect
}
