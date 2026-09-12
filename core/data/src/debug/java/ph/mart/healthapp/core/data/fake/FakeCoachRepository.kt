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
import ph.mart.healthapp.core.data.coach.TOOL_GET_DAY
import ph.mart.healthapp.core.data.coach.TOOL_GET_HISTORY
import ph.mart.healthapp.core.data.food.MealType
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
                emit(CoachReply.Proposal(script.action))
            }

            is FakeScript.Tool -> {
                val result = toolbox.runTool(script.name, script.args).orEmpty()
                val answer = stream(script.preamble + "\n" + result)
                real.settle(question, answer, null)
            }

            is FakeScript.Say -> {
                val answer = stream(script.text(request))
                real.settle(question, answer, null)
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

    data class Propose(val action: CoachAction, val preamble: String) : FakeScript

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
                action = CoachAction.LogWater(glasses = 1),
                preamble = "Sure — here's a glass of water to add:",
            )
        }
        matchedFood(asked)?.let { food ->
            return FakeScript.Propose(
                action = CoachAction.LogFood(
                    name = food.name,
                    mealType = mealFor(asked),
                    calories = food.calories,
                    proteinG = food.proteinG,
                    carbsG = food.carbsG,
                    fatG = food.fatG,
                    portionAmount = food.portionAmount,
                    portionUnit = food.portionUnit,
                ),
                preamble = "Here's what I'd log for that — check the numbers before you tap:",
            )
        }
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

/** Only the calendar words, not a general number — "log 2 eggs" must not read as "two days ago". */
private fun daysAgoIn(asked: String): Int? = when {
    "yesterday" in asked -> 1
    "today" in asked -> 0
    else -> DAYS_AGO.find(asked)?.groupValues?.get(1)?.toIntOrNull()
}

private val DAYS_AGO = Regex("""(\d+)\s+days?\s+ago""")

/** The longest word in the sentence that names something in `COMMON_FOODS`, so "eggs" beats a
 * three-letter accident. [commonFoodFor] is what handles the plural. */
private fun matchedFood(asked: String) = asked
    .split(' ', ',', '.')
    .sortedByDescending { it.length }
    .firstNotNullOfOrNull { word -> commonFoodFor(word) }

private fun mealFor(asked: String): MealType =
    MealType.entries.firstOrNull { it.name.lowercase() in asked } ?: MealType.Snacks
