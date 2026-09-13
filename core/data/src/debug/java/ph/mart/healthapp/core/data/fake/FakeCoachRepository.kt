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
import ph.mart.healthapp.core.data.todayEpochDay

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
 *
 * ## What to type to reach each state
 *
 * The whole screen, in the order it is worth walking. The debug seed is what populates the diary,
 * the library and the weigh-in these answer from, and it only fires on an install with no profile
 * — so clear app data before starting.
 *
 * | Type this | Reaches |
 * | --- | --- |
 * | *(fresh install, nothing sent yet)* | the empty state and its four starters |
 * | `am I doing okay?` | a streamed plain answer, then the follow-up chips under it |
 * | `what did I eat yesterday?`, `how did 3 days ago go?` | a `get_day` round: preface, thinking mascot, then the day |
 * | `how has my week gone?`, `what's my average this month?` | `get_history` at 7 and at 30 days |
 * | `what have I saved recently?` | `get_library` |
 * | `log a glass of water` | the single-row card |
 * | `log a 45 minute run` | the same card, its burn priced off the real weigh-in |
 * | `log my weight 82.4` | the weigh-in card, in the profile's unit, with its change line |
 * | `log two eggs for breakfast` | the single-row card, food |
 * | `log eggs, rice and an apple for lunch` | the multi-row card, its per-row `✕` and a partial confirm |
 * | `log my usual Overnight oats for breakfast` | a saved meal expanded into one row per item |
 * | `log my creatine` | the supplement card, ticked off against the user's own row |
 * | `log two eggs for breakfast yesterday` | the same card, titled with the day it will write to |
 * | `took my Nothing At All supplement` | prose, then the failure bubble — no such supplement |
 * | `log my saved Nothing At All` | prose, then the failure bubble — the draft resolved to nothing |
 * | `quietly log a glass of water`, then Dismiss | a card with no prose above it: the turn is abandoned, not persisted |
 * | `log egg rice bacon salmon chicken bread milk cheese apple banana potato pasta` | a draft past [MAX_DRAFT_ROWS], rejected whole |
 * | `make this fail` | the failure bubble and its Retry |
 * | any of the above, then Stop mid-stream | the turn abandoned, the question back in the field |
 * | any answer, then its `↺` | the ask-again send |
 * | Clear, then confirm | the confirmation dialog and an emptied conversation |
 *
 * Two states stay real-AI-only and are not worth faking. **Offline** (`OFFLINE_REASON`) never
 * reaches this class at all — `CoachViewModel.onSend` short-circuits on the `NetworkMonitor`, so
 * airplane mode is the whole test. And the **`MAX_REPLY_CHARS` rejection** is `sanitizeReply`'s,
 * which only the real repository's chunks pass through; [stream] emits its own text verbatim.
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
                // produces: the model says what it is about to propose, then calls the tool. Empty
                // is the `quietly` route — a write call that came with no prose at all, which is
                // the one case a dismissal has nothing to persist.
                if (script.preamble.isNotEmpty()) stream(script.preamble)
                // Through `resolve` for the reason everything else here goes through the real
                // path: a workout's burn is the app's arithmetic over the user's own weigh-in and
                // a saved meal's rows are the user's own, so a fake that skipped it would draw a
                // card the real one never draws.
                val actions = script.actions.map { it.resolve(toolbox) }
                val rows = actions.filterNotNull().flatten()
                // Both of the real loop's failing endings, in its order: a call that resolved to
                // nothing — a saved meal in no library — and a draft past the row ceiling.
                // Rejected rather than truncated, which is why [matchedFoods] no longer caps.
                emit(
                    if (actions.any { it == null } || rows.size > MAX_DRAFT_ROWS) CoachReply.Failed
                    else CoachReply.Proposal(rows),
                )
            }

            is FakeScript.Tool -> {
                // Preface, dropped, then the answer — `CoachRepositoryImpl`'s own shape between
                // tool rounds, down to the empty partial that hands the screen back its thinking
                // mascot while the tool runs. Only the tool's output is persisted, which is what
                // the real loop's `raw.setLength(0)` means.
                stream(script.preamble)
                emit(CoachReply.Partial(""))
                val answer = stream(toolbox.runTool(script.name, script.args).orEmpty())
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
     * card. [preamble] is empty on the `quietly` route and only there. */
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
        // The same calendar words the read routing uses, read *inside* the log block: "log the
        // eggs I had yesterday" is a draft for yesterday, not a question about it. Zero is today,
        // which is what every action here means by a missing date.
        val date = daysAgoIn(asked)?.takeIf { it > 0 }?.let { todayEpochDay() - it } ?: 0L
        // First in the block, because `EXERCISE_WORDS` claims "weights" for a lifting session and
        // "log my weight 82.4" is not one.
        weighInIn(asked)?.let { weight ->
            return FakeScript.Propose(
                actions = listOf(CoachAction.LogWeight(weight = weight)),
                preamble = preamble(
                    asked,
                    "Here's today's weigh-in — the unit is whichever your profile uses:",
                ),
            )
        }
        if (WATER_WORDS.any { it in asked }) {
            return FakeScript.Propose(
                actions = listOf(CoachAction.LogWater(glasses = 1, dateEpochDay = date)),
                preamble = preamble(asked, "Sure — here's a glass of water to add:"),
            )
        }
        // Before the exercise and food matches, because a saved meal is named by the user and its
        // name can be anything — "log my usual Overnight oats" would otherwise draft a row for the
        // oats alone at `COMMON_FOODS`' figures rather than the user's own saved ones.
        savedMealNameIn(asked)?.let { name ->
            return FakeScript.Propose(
                actions = listOf(
                    CoachAction.LogSavedMeal(
                        name = name,
                        mealType = mealFor(asked),
                        dateEpochDay = date,
                    ),
                ),
                preamble = preamble(asked, "Pulling that one out of your library:"),
            )
        }
        // After the library match and before the food one: a supplement is named by the user, so
        // it has a saved meal's problem rather than a common food's — "log my magnesium" would
        // otherwise fall through to `COMMON_FOODS` and find nothing at all.
        supplementNameIn(asked)?.let { name ->
            return FakeScript.Propose(
                actions = listOf(CoachAction.LogSupplement(name = name, doses = 1)),
                preamble = preamble(asked, "Ticking that one off for today:"),
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
                        dateEpochDay = date,
                    ),
                ),
                preamble = preamble(
                    asked,
                    "Here's the session I'd add — the burn is worked out from your weight:",
                ),
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
                        dateEpochDay = date,
                    )
                },
                preamble = preamble(asked, "Here's what I'd log for that — check the numbers before you tap:"),
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

private val LOG_WORDS = listOf("log ", "add ", "i ate", "i had", "i drank", "i weigh", "took ", "note down")
private val WATER_WORDS = listOf("water", "glass")
private val HISTORY_WORDS = listOf("week", "month", "trend", "average", "lately", "recently")
private val LIBRARY_WORDS = listOf("saved", "recipe", "library", "usual")

/**
 * What makes a sentence about a supplement. The seed's three names are in here so the common case
 * — "log my creatine" — names no category word at all and still routes, which is the sentence
 * anyone actually types.
 */
private val SUPPLEMENT_WORDS = listOf("supplement", "vitamin", "creatine", "magnesium")

/**
 * The second magic word, in [FakeScript.Fail]'s shape and for its reason.
 *
 * Say "quietly" and the draft arrives with no prose above it. That is a real ending — a model can
 * call `log_food` and say nothing — and it is the only one where a dismissal has no answer to
 * persist, so `onSettle` abandons the turn instead of writing it. Nothing else in a debug build
 * reaches `withTurnAbandoned` from a card.
 */
private const val QUIET_WORD = "quietly"

private fun preamble(asked: String, text: String): String = if (QUIET_WORD in asked) "" else text

/**
 * The number in a sentence that is about a weight — "log my weight 82.4", "i weigh 181".
 *
 * The weight word is what makes it one: without it "add 2 eggs" would draft a 2 kg weigh-in. It
 * stays as the user said it, unconverted, because that is what the model passes and what the card
 * draws — `resolve` is where the profile's unit gets stamped on it.
 */
private fun weighInIn(asked: String): Double? =
    if (WEIGH_WORDS.any { it in asked }) DECIMAL.find(asked)?.value?.toDoubleOrNull() else null

/** "weigh" covers weight, weighed and weigh-in; the two units cover a sentence that names one. */
private val WEIGH_WORDS = listOf("weigh", "kg", "lb")

private val DECIMAL = Regex("""\d+(?:\.\d+)?""")

/**
 * The name after a library word, when the sentence gave one — "log my usual **Overnight oats** for
 * breakfast".
 *
 * Null when nothing follows it, which is what keeps "what have I saved recently?" on the library
 * *tool*: this only ever runs inside the `LOG_WORDS` block, and a question is not an instruction.
 * The name goes to `savedMealRows` unchanged and is matched `equals(ignoreCase = true)`, so the
 * lowercasing costs nothing — and a name in no library resolves to null, which is how a debug build
 * reaches the failed-draft ending.
 */
private fun savedMealNameIn(asked: String): String? =
    LIBRARY_WORDS.firstOrNull { it in asked }
        ?.let { word -> asked.substringAfter(word).substringBefore(" for ").trim(' ', '.', ',', '?') }
        ?.takeIf { it.isNotEmpty() }

/**
 * The supplement named in the sentence, when there is one — [savedMealNameIn]'s shape and for its
 * reason.
 *
 * A category word ("supplement") takes what precedes it, because that is where the name sits in
 * "took my Nothing At All supplement"; a name word is itself the name. Either way it goes to
 * `supplementDose` unchanged and is matched `equals(ignoreCase = true)`, so a name the user does
 * not take resolves to null — which is how a debug build reaches the failed-draft ending here.
 */
private fun supplementNameIn(asked: String): String? {
    val word = SUPPLEMENT_WORDS.firstOrNull { it in asked } ?: return null
    if (word != "supplement") {
        // From the name word to the end of the phrase, so "vitamin d" survives and "creatine
        // today" does not — the match downstream is exact, and a trailing word is a miss.
        return (word + asked.substringAfter(word).substringBefore(" for ").substringBefore(" today"))
            .trim(' ', '.', ',', '?')
    }
    return asked.substringBefore(word)
        .substringAfterLast(" my ")
        .trim(' ', '.', ',', '?')
        .takeIf { it.isNotEmpty() }
}

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
 * Distinct by name, so "eggs and more eggs" is one row rather than two identical ones, and
 * **uncapped**: the real loop rejects a draft past [MAX_DRAFT_ROWS] rather than truncating it, so a
 * fake that capped here would answer a twelve-food sentence with ten quiet rows and leave that
 * rejection unreachable.
 */
private fun matchedFoods(asked: String) = asked
    .split(' ', ',', '.')
    .mapNotNull { word ->
        val stem = word.trim().removeSuffix("s")
        commonFoodFor(word)?.takeIf { it.namesWord(stem) }
    }
    .distinctBy { it.name }

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
