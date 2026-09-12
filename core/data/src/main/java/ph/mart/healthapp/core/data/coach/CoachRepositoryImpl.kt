package ph.mart.healthapp.core.data.coach

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ThinkingConfig
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.AI_MODEL_NAME
import ph.mart.healthapp.core.data.coach.local.ChatMessageDao
import ph.mart.healthapp.core.data.coach.local.ChatMessageEntity
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.insight.InsightRequest
import ph.mart.healthapp.core.data.insight.dayNumbersBlock
import ph.mart.healthapp.core.data.logAiFailure
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.data.water.WaterRepository

/**
 * Room for the answer, the tool calls that reach it, and the reasoning that picks them.
 *
 * Raised from 300 when the coach got tools: a turn can now spend tokens deciding to call `get_day`,
 * reading the result, and *then* writing a six-line list. All three come out of one budget — the
 * lesson `AI_THINKING` is written against.
 */
private const val MAX_OUTPUT_TOKENS = 700

/**
 * The coach is the first call site in this app that leaves [ph.mart.healthapp.core.data.AI_THINKING].
 *
 * That constant's own doc comment names this case and its condition: *"If a call site ever
 * genuinely needs to reason, it raises the level **and** `maxOutputTokens` together — they are one
 * budget."* Choosing a tool and filling in its arguments is the first thing here that genuinely
 * reasons — the alternative is a model that guesses `days_ago` or invents a calorie count — so
 * both moved at once, and the shared `MINIMAL` still describes the other four call sites, which
 * flatten a photo into JSON and write a line of encouragement.
 *
 * `LOW`, not `MEDIUM`: this is picking one of four functions, not solving anything.
 */
private val COACH_THINKING: ThinkingConfig = thinkingConfig { thinkingLevel = ThinkingLevel.LOW }

/**
 * How many times one question may bounce off a tool before the coach has to answer with what it
 * has.
 *
 * ponytail: a flat round count, not a token or latency budget. Two is enough for the shape every
 * real question has — ask for a day, answer — and the third is headroom for "compare this week to
 * last". Price it properly if a tool ever fans out.
 */
private const val MAX_TOOL_ROUNDS = 3

/**
 * Plain text out, not JSON — the same call the daily insight makes, one turn longer, streamed, and
 * now able to ask the diary a question mid-answer.
 *
 * The model is rebuilt on every [send] rather than held as a field, because its system
 * instruction carries the day's numbers and those move while the screen is open: a glass of water
 * logged in another tab must not leave the coach quoting a stale figure. A `GenerativeModel` is a
 * configuration object, so this costs nothing.
 *
 * Nothing is cached, unlike the insight: an insight is one line per day, while every question is
 * its own answer.
 *
 * The answer is streamed, and the pair of rows is still written once — but *where* that write
 * happens now depends on how the turn ends. A turn that answers writes on its last chunk, exactly
 * as before. A turn that ends in a [CoachReply.Proposal] writes nothing at all and hands the
 * ending to [settle], because the user has not agreed to anything yet.
 */
internal class CoachRepositoryImpl(
    private val dao: ChatMessageDao,
    // The two `settle` writes through, and the toolbox the reads run against. The toolbox is
    // built by the Koin module rather than here so the debug build's fake coach can be handed the
    // *same* one — a faked answer then reads the same Room rows the real one would.
    private val foodRepository: FoodRepository,
    private val waterRepository: WaterRepository,
    private val toolbox: CoachToolbox,
) : CoachRepository {

    override fun observeMessages(): Flow<List<ChatMessage>> =
        dao.observeAll().map { messages -> messages.map { it.toMessage() } }

    override fun send(question: String, request: InsightRequest?): Flow<CoachReply> = flow {
        val model = Firebase.ai(
            backend = GenerativeBackend.googleAI(),
        ).generativeModel(
            modelName = AI_MODEL_NAME,
            generationConfig = generationConfig {
                maxOutputTokens = MAX_OUTPUT_TOKENS
                thinkingConfig = COACH_THINKING
            },
            tools = listOf(COACH_TOOLS),
            systemInstruction = content { text(systemPromptFor(request)) },
        )

        val chat = model.startChat(history = dao.recent(MAX_HISTORY_MESSAGES).asHistory())
        val raw = StringBuilder()
        var message: Content = content(role = "user") { text(question) }

        repeat(MAX_TOOL_ROUNDS) {
            val calls = mutableListOf<FunctionCallPart>()
            chat.sendMessageStream(message).collect { chunk ->
                chunk.text?.let(raw::append)
                // The accumulated answer, not the chunk: a bubble renders the whole of it, so the
                // whole of it is what has to clear the trust boundary. A partial past
                // MAX_REPLY_CHARS sanitizes to null and the bubble simply stops growing — the
                // check below then fails the send, rather than truncating.
                sanitizeReply(raw.toString())?.let { emit(CoachReply.Partial(it)) }
                calls += chunk.functionCalls
            }

            // Nothing asked for: the model has said its piece and this is an ordinary answer.
            if (calls.isEmpty()) return@flow finish(question, raw.toString())

            // A write call ends the turn here, unpersisted. It is a draft, and the user is the one
            // who decides whether it becomes a row — so `settle` is what writes, not this.
            calls.firstOrNull { it.name in WRITE_TOOLS }?.let { call ->
                val action = parseAction(call.name, call.args) ?: return@flow emit(CoachReply.Failed)
                return@flow emit(CoachReply.Proposal(action))
            }

            // Reads run now and go straight back into the same turn. Run before the builder, not
            // inside it: `content {}` takes a plain lambda and a tool read is suspending.
            val results = calls.map { it to (toolbox.runTool(it.name, it.args) ?: UNKNOWN_TOOL) }
            // The role is "user", not "function": the SDK's `Chat.assertComesFromUser` accepts
            // only "user" and logs the 'function' role as deprecated and due for removal.
            message = content(role = "user") {
                results.forEach { (call, result) ->
                    part(FunctionResponsePart(call.name, toolResponse(result)))
                }
            }
        }

        // Out of rounds with the model still reaching for tools. Whatever prose it produced on the
        // way is either a real answer or nothing, and `finish` already treats nothing as a failure.
        finish(question, raw.toString())
    }.catch { e ->
        // `catch` rather than a `try` around the loop: wrapping an `emit` in `catch (e: Exception)`
        // swallows the CancellationException downstream cancellation throws back through it.
        logAiFailure("coach send", e)
        emit(CoachReply.Failed)
    }

    /** The ordinary ending: sanitize, write the pair, say nothing more — flow completion is the
     * success signal, which is why there is no `Answered` variant to emit here. */
    private suspend fun FlowCollector<CoachReply>.finish(question: String, raw: String) {
        val answer = sanitizeReply(raw) ?: return emit(CoachReply.Failed)
        writeExchange(question, answer)
    }

    override suspend fun settle(question: String, answer: String, action: CoachAction?) {
        when (action) {
            is CoachAction.LogFood -> foodRepository.addEntry(
                FoodEntry(
                    name = action.name,
                    mealType = action.mealType,
                    portionAmount = action.portionAmount,
                    portionUnit = action.portionUnit,
                    calories = action.calories,
                    proteinG = action.proteinG,
                    carbsG = action.carbsG,
                    fatG = action.fatG,
                ),
            )
            // Added to the day, never assigned: `setToday` takes the new total, and a coach that
            // proposes "one glass" must not wipe the six already there.
            is CoachAction.LogWater -> waterRepository.setToday(
                waterRepository.observeToday().first() + action.glasses,
            )
            null -> Unit
        }
        writeExchange(question, answer)
    }

    /** The one write, shared by both endings, so "a question is only persisted once it has been
     * answered" stays one rule with one implementation. */
    private suspend fun writeExchange(question: String, answer: String) {
        val now = System.currentTimeMillis()
        dao.addExchange(
            question = ChatMessageEntity(fromUser = true, text = question, sentAtMillis = now),
            // One millisecond apart so the ascending sort can never render the reply first; the
            // id tie-break in the DAO covers a clock that doesn't move between the two.
            answer = ChatMessageEntity(fromUser = false, text = answer, sentAtMillis = now + 1),
        )
    }

    override suspend fun clear() = dao.softDeleteAll()
}

/** What a read tool answers when the model invents a name. Stays in Kotlin: it is prompt text the
 * user never sees. */
private const val UNKNOWN_TOOL = "That tool does not exist."

/** [ChatMessageDao.recent] returns newest-first so `LIMIT` takes the end of the conversation;
 * the model wants it in the order it was said. */
private fun List<ChatMessageEntity>.asHistory(): List<Content> =
    reversed().map { content(role = if (it.fromUser) "user" else "model") { text(it.text) } }

/**
 * The coach's standing instructions.
 *
 * The paragraph that used to list what the coach *cannot* see is gone, because it is no longer
 * true: `get_day` and `get_history` reach any day the app has. What replaces it is narrower and
 * more important — **call a tool rather than guess.** A model that can read the diary and answers
 * from memory anyway is worse than one that admitted it had nothing, because the figure it invents
 * now looks sourced.
 *
 * Two constraints survive the rewrite untouched:
 * - **No medical advice.** A coach that reaches further is a coach a user trusts further, so this
 *   matters more than it did, not less.
 * - **No numbers block at all** when [request] is null: with no profile there is no target to be
 *   over or under, and a coach that admits it beats one improvising one. The tools still work —
 *   a diary can be read without a profile.
 */
private fun systemPromptFor(request: InsightRequest?): String = buildString {
    appendLine(
        "You are a friendly nutrition and fitness coach inside FitPulse, a food and body tracking " +
            "app. You are talking to the user who logs their day in it.",
    )
    appendLine()
    if (request == null) {
        appendLine("You have not been given any of this user's targets, because they have not set up a profile yet.")
    } else {
        appendLine("Today so far, for a user whose goal is ${request.goal.name.lowercase()} weight:")
        append(dayNumbersBlock(request))
    }
    appendLine()
    appendLine(
        "You can read the rest of their diary with tools. Use get_day for any single day — it " +
            "returns every food they logged with its calories and macros, their water and their " +
            "activity. Use get_history for a week, a month, a trend or an average. Days are given " +
            "as how many days back from today, where 0 is today and 1 is yesterday; today is day " +
            "number ${todayEpochDay()} internally, so just count backwards. Never state a figure " +
            "you were not given or did not read from a tool — call the tool instead of guessing, " +
            "and if a tool comes back empty, say plainly that nothing was logged.",
    )
    appendLine(
        "If the user asks you to log something, call log_food or log_water. These do not log " +
            "anything themselves: the user sees what you drafted and taps to confirm it, so say " +
            "what you are proposing in the same reply. Estimate the nutrition from their " +
            "description. You cannot edit or delete anything, and you cannot log for a past day — " +
            "point them at the Food tab's diary for that.",
    )
    appendLine(
        "Reply in plain conversational text, in the second person. Keep it to three short " +
            "sentences, or up to six short lines when a list genuinely answers the question " +
            "better — one item per line, starting with \"- \". No markdown, no headings, no bold, " +
            "no emoji. Give no medical advice, no diagnosis, and no supplement or medication " +
            "suggestions; if you are asked for any of those, say that is a question for a doctor " +
            "or dietitian and offer what their logged numbers can tell them instead.",
    )
}

private fun ChatMessageEntity.toMessage() = ChatMessage(
    id = id,
    fromUser = fromUser,
    text = text,
    sentAtMillis = sentAtMillis,
)
