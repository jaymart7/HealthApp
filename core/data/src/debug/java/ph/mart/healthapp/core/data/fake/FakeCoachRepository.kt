package ph.mart.healthapp.core.data.fake

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import ph.mart.healthapp.core.data.coach.CoachAction
import ph.mart.healthapp.core.data.coach.CoachReply
import ph.mart.healthapp.core.data.coach.CoachRepository
import ph.mart.healthapp.core.data.coach.CoachToolbox
import ph.mart.healthapp.core.data.coach.MAX_DRAFT_ROWS
import ph.mart.healthapp.core.data.coach.TOOL_GET_DAY
import ph.mart.healthapp.core.data.coach.TOOL_GET_HISTORY
import ph.mart.healthapp.core.data.coach.TOOL_GET_LIBRARY
import ph.mart.healthapp.core.data.coach.resolve
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.ScannedProduct
import ph.mart.healthapp.core.data.insight.InsightRequest
import ph.mart.healthapp.core.data.insight.insightFor

/**
 * The coach with the model taken out and nothing else changed.
 *
 * It is `CoachRepository by real`, so `observeMessages`, `settle` and `clear` are the shipping
 * implementations untouched — only [send] is replaced. That delegation is what makes this worth
 * having: the Room write that ends a turn is the real one, reached through the public
 * `settle(question, answer, null)` path, so the emission that retires `pending`/`streaming` in
 * `CoachUiState.withMessages` arrives exactly when it does against Gemini. Nothing about the
 * screen's state machine is being simulated.
 *
 * The reads are real too. [toolbox] is the same instance the real repository holds, so
 * "what did I eat yesterday?" answers off the actual diary — which is the only way a debug build
 * can show whether `formatDay`'s output reads well in a chat bubble.
 *
 * What *is* faked is the routing: a keyword match standing in for a model choosing a tool. It is
 * deliberately crude and deliberately predictable, because its job is to reach every state of the
 * screen on demand rather than to be clever. [fakeCoachScript] is that routing, pulled out as a
 * pure function so it can be tested.
 */
internal class FakeCoachRepository(
    private val real: CoachRepository,
    private val toolbox: CoachToolbox,
) : CoachRepository by real {

    override fun send(question: String, request: InsightRequest?): Flow<CoachReply> = flow {
        delay(THINKING_MS)
        when (val script = fakeCoachScript(question)) {
            is FakeScript.Fail -> emit(CoachReply.Failed)

            is FakeScript.Propose -> {
                // The prose comes first and the card follows, which is the order the real thing
                // produces: the model says what it is about to propose, then calls the tool.
                stream(script.preamble)
                // Through `resolve` for the reason everything else here goes through the real
                // path: a workout's burn is the app's arithmetic over the user's own weigh-in and
                // a saved meal's rows are the user's own, so a fake that skipped it would draw a
                // card the real one never draws.
                val actions = script.actions.map { it.resolve(toolbox) }
                emit(
                    if (actions.any { it == null }) CoachReply.Failed
                    else CoachReply.Proposal(actions.filterNotNull().flatten()),
                )
            }

            is FakeScript.Tool -> {
                val result = toolbox.runTool(script.name, script.args).orEmpty()
                val answer = stream(script.preamble + "\n" + result)
                real.settle(question, answer, emptyList())
            }

            is FakeScript.Say -> {
                val answer = stream(script.text(request))
                real.settle(question, answer, emptyList())
            }
        }
    }

    /** Word by word, so `StreamingBubble`, the mascot's `Thinking → Idle` move and the list's
     * scroll-per-chunk all behave as they do against a real stream. Returns the whole text, which
     * is what gets persisted. */
    private suspend fun kotlinx.coroutines.flow.FlowCollector<CoachReply>.stream(text: String): String {
        val built = StringBuilder()
        text.split(' ').forEachIndexed { index, word ->
            if (index > 0) built.append(' ')
            built.append(word)
            emit(CoachReply.Partial(built.toString()))
            delay(WORD_MS)
        }
        return built.toString()
    }
}

/** About as long as the real thing takes to produce its first token. */
private const val THINKING_MS = 600L

/** Slow enough to watch, fast enough not to be annoying on every rebuild. */
private const val WORD_MS = 35L

/**
 * What a faked question turns into. A sealed type rather than four booleans because the routing is
 * the only part of this file worth a test, and a test asserts far more clearly against a
 * `Propose(LogWater(1))` than against the side effects of having emitted one.
 */
internal sealed interface FakeScript {
    /** Reached by saying "fail" — the failure bubble and its Retry are otherwise unreachable
     * without turning the radio off, which also disables the tools. */
    data object Fail : FakeScript

    /** A list, because one meal is several rows — [fakeCoachScript] drafts more than one for a
     * sentence naming more than one food, which is the only way a debug build reaches the multi-row
     * card. */
    data class Propose(val actions: List<CoachAction>, val preamble: String) : FakeScript

    data class Tool(val name: String, val args: Map<String, JsonElement>, val preamble: String) : FakeScript

    /** [text] takes the day's payload because the generic answer should still quote real numbers;
     * with no profile there are none, and it says so. */
    data class Say(val text: (InsightRequest?) -> String) : FakeScript
}

/**
 * Keyword routing, pure and ordered — first match wins.
 *
 * The order matters and is the opposite of the obvious one: "log" is checked **before** "yesterday"
 * so that "log the eggs I had yesterday" drafts a row rather than reading the day, which is what a
 * real model does with that sentence. Everything falls through to [FakeScript.Say].
 *
 * These strings all stay in Kotlin, and not by the localization rules' exemption — a debug source
 * set is outside `checkUiLiterals`' scope entirely, because scaffolding that never ships is never
 * translated.
 */
internal fun fakeCoachScript(question: String): FakeScript {
    val asked = question.lowercase()

    if ("fail" in asked) return FakeScript.Fail

    if (LOG_WORDS.any { it in asked }) {
        if (WATER_WORDS.any { it in asked }) {
            return FakeScript.Propose(
                actions = listOf(CoachAction.LogWater(glasses = 1)),
                preamble = "Sure — here's a glass of water to add:",
            )
        }
        matchedExercise(asked)?.let { type ->
            return FakeScript.Propose(
                actions = listOf(
                    CoachAction.LogExercise(
                        type = type,
                        name = "",
                        minutes = minutesIn(asked),
                        // Zero, exactly as `parseAction` leaves it: `priced` fills it in.
                        burnedKcal = 0,
                    ),
                ),
                preamble = "Here's the session I'd add — the burn is worked out from your weight:",
            )
        }
        // Every food named in the sentence, not the longest one: "log eggs, toast and coffee" is
        // the sentence the multi-row card exists for, and a fake that could only ever draft one row
        // would leave it unreachable in a debug build.
        matchedFoods(asked).takeIf { it.isNotEmpty() }?.let { foods ->
            val meal = mealFor(asked)
            return FakeScript.Propose(
                actions = foods.map { food ->
                    CoachAction.LogFood(
                        name = food.name,
                        mealType = meal,
                        calories = food.calories,
                        proteinG = food.proteinG,
                        carbsG = food.carbsG,
                        fatG = food.fatG,
                        portionAmount = food.portionAmount,
                        portionUnit = food.portionUnit,
                    )
                },
                preamble = "Here's what I'd log for that — check the numbers before you tap:",
            )
        }
    }

    // Before the history words, because "what have I saved recently?" is a library question and
    // "recently" is one of theirs.
    if (LIBRARY_WORDS.any { it in asked }) {
        return FakeScript.Tool(
            name = TOOL_GET_LIBRARY,
            args = emptyMap(),
            preamble = "Here's what you've saved:",
        )
    }

    HISTORY_WORDS.firstOrNull { it in asked }?.let {
        val days = if ("month" in asked) 30 else 7
        return FakeScript.Tool(
            name = TOOL_GET_HISTORY,
            args = mapOf("days" to JsonPrimitive(days)),
            preamble = "Here's how the last $days days have gone:",
        )
    }

    daysAgoIn(asked)?.let { daysAgo ->
        return FakeScript.Tool(
            name = TOOL_GET_DAY,
            args = mapOf("days_ago" to JsonPrimitive(daysAgo)),
            preamble = "Here's that day:",
        )
    }

    return FakeScript.Say { request ->
        when {
            request == null -> "You haven't set up a profile yet, so I don't have any targets to " +
                "measure today against."
            // The rule-based line when the day has something to say, so the fake quotes numbers
            // that are actually true of today rather than inventing encouragement.
            else -> insightFor(request)?.let { "$it Ask me about a past day or your week too." }
                ?: "Today's on track against your targets. Ask me about a past day or your week " +
                    "and I'll pull it up."
        }
    }
}

private val LOG_WORDS = listOf("log ", "add ", "i ate", "i had", "i drank", "note down")
private val WATER_WORDS = listOf("water", "glass")
private val HISTORY_WORDS = listOf("week", "month", "trend", "average", "lately", "recently")
private val LIBRARY_WORDS = listOf("saved", "recipe", "library", "usual")

/** Only the calendar words, not a general number — "log 2 eggs" must not read as "two days ago". */
private fun daysAgoIn(asked: String): Int? = when {
    "yesterday" in asked -> 1
    "today" in asked -> 0
    else -> DAYS_AGO.find(asked)?.groupValues?.get(1)?.toIntOrNull()
}

private val DAYS_AGO = Regex("""(\d+)\s+days?\s+ago""")

/**
 * Every word in the sentence that names something in `COMMON_FOODS`, in the order they were said,
 * one row each. [commonFoodFor] is what handles the plural.
 *
 * **The word has to start a word of the food's name**, which longest-word-wins used to hide:
 * `searchCommonFoods` is a plain substring match, so "i ate some rice" finds "ate" inside *Water*
 * as well as the rice — and one row per match turns that near-miss into a phantom row on a card
 * whose whole promise is that what it shows is what gets written. A whole-word prefix keeps
 * "rice" → *Brown rice, cooked* and drops "ate" → *Water*.
 *
 * Distinct by name, so "eggs and more eggs" is one row rather than two identical ones, and capped
 * the way a real draft is.
 */
private fun matchedFoods(asked: String) = asked
    .split(' ', ',', '.')
    .mapNotNull { word ->
        val stem = word.trim().removeSuffix("s")
        commonFoodFor(word)?.takeIf { it.namesWord(stem) }
    }
    .distinctBy { it.name }
    .take(MAX_DRAFT_ROWS)

private fun ScannedProduct.namesWord(stem: String): Boolean =
    stem.isNotEmpty() && name.split(' ', ',', '(', '-').any { it.startsWith(stem, ignoreCase = true) }

/**
 * Checked before the food match, so "log a 30 minute run" drafts a workout — "run" names no food,
 * but "i had a swim" would otherwise fall through to the generic answer.
 */
private fun matchedExercise(asked: String): ExerciseType? =
    EXERCISE_WORDS.entries.firstOrNull { (word, _) -> word in asked }?.value

private val EXERCISE_WORDS = linkedMapOf(
    "ran" to ExerciseType.Run,
    "run" to ExerciseType.Run,
    "jog" to ExerciseType.Run,
    "walk" to ExerciseType.Walk,
    "cycl" to ExerciseType.Cycle,
    "bike" to ExerciseType.Cycle,
    "swim" to ExerciseType.Swim,
    "swam" to ExerciseType.Swim,
    "lift" to ExerciseType.Strength,
    "gym" to ExerciseType.Strength,
    "weights" to ExerciseType.Strength,
    "yoga" to ExerciseType.Yoga,
    "hiit" to ExerciseType.Hiit,
    "workout" to ExerciseType.Other,
)

/** Half an hour when the sentence names no length — a real model asks or assumes too, and the
 * card is there to be corrected by dismissing it. */
private fun minutesIn(asked: String): Int =
    MINUTES.find(asked)?.groupValues?.get(1)?.toIntOrNull()
        ?: HOURS.find(asked)?.groupValues?.get(1)?.toIntOrNull()?.times(60)
        ?: 30

private val MINUTES = Regex("""(\d+)\s*(?:min|minute)""")
private val HOURS = Regex("""(\d+)\s*(?:hour|hr|h\b)""")

private fun mealFor(asked: String): MealType =
    MealType.entries.firstOrNull { it.name.lowercase() in asked } ?: MealType.Snacks
