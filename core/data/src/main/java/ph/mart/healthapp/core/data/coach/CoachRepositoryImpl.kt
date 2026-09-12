package ph.mart.healthapp.core.data.coach

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.AI_MODEL_NAME
import ph.mart.healthapp.core.data.AI_THINKING
import ph.mart.healthapp.core.data.coach.local.ChatMessageDao
import ph.mart.healthapp.core.data.coach.local.ChatMessageEntity
import ph.mart.healthapp.core.data.insight.InsightRequest
import ph.mart.healthapp.core.data.insight.dayNumbersBlock
import ph.mart.healthapp.core.data.logAiFailure

/** A few sentences' worth. [sanitizeReply] rejects whatever gets past it, but capping here is
 * cheaper than paying for a paragraph that will be thrown away. */
private const val MAX_OUTPUT_TOKENS = 300

/**
 * Plain text out, not JSON — the same call the daily insight makes, one turn longer and streamed.
 *
 * The model is rebuilt on every [send] rather than held as a field, because its system
 * instruction carries the day's numbers and those move while the screen is open: a glass of water
 * logged in another tab must not leave the coach quoting a stale figure. A `GenerativeModel` is a
 * configuration object, so this costs nothing.
 *
 * Nothing is cached, unlike the insight: an insight is one line per day, while every question is
 * its own answer.
 *
 * The answer is streamed. The pair of rows is still written once, on the last chunk, so the
 * "persisted only when answered" rule is unchanged — what streaming buys is the screen, not the
 * history.
 */
internal class CoachRepositoryImpl(private val dao: ChatMessageDao) : CoachRepository {

    override fun observeMessages(): Flow<List<ChatMessage>> =
        dao.observeAll().map { messages -> messages.map { it.toMessage() } }

    override fun send(question: String, request: InsightRequest?): Flow<CoachReply> = flow {
        val model = Firebase.ai(
            backend = GenerativeBackend.googleAI(),
            useLimitedUseAppCheckTokens = true,
        ).generativeModel(
            modelName = AI_MODEL_NAME,
            generationConfig = generationConfig {
                maxOutputTokens = MAX_OUTPUT_TOKENS
                thinkingConfig = AI_THINKING
            },
            systemInstruction = content { text(systemPromptFor(request)) },
        )

        val chat = model.startChat(history = dao.recent(MAX_HISTORY_MESSAGES).asHistory())
        val raw = StringBuilder()
        chat.sendMessageStream(question).collect { chunk ->
            chunk.text?.let(raw::append)
            // The accumulated answer, not the chunk: a bubble renders the whole of it, so the whole
            // of it is what has to clear the trust boundary. A partial past MAX_REPLY_CHARS
            // sanitizes to null and the bubble simply stops growing — the check below then fails
            // the send, rather than truncating.
            sanitizeReply(raw.toString())?.let { emit(CoachReply.Partial(it)) }
        }

        val answer = sanitizeReply(raw.toString()) ?: return@flow emit(CoachReply.Failed)
        val now = System.currentTimeMillis()
        dao.addExchange(
            question = ChatMessageEntity(fromUser = true, text = question, sentAtMillis = now),
            // One millisecond apart so the ascending sort can never render the reply first; the
            // id tie-break in the DAO covers a clock that doesn't move between the two.
            answer = ChatMessageEntity(fromUser = false, text = answer, sentAtMillis = now + 1),
        )
    }.catch { e ->
        // `catch` rather than a `try` around the loop: wrapping an `emit` in `catch (e: Exception)`
        // swallows the CancellationException downstream cancellation throws back through it.
        logAiFailure("coach send", e)
        emit(CoachReply.Failed)
    }

    override suspend fun clear() = dao.softDeleteAll()
}

/** [ChatMessageDao.recent] returns newest-first so `LIMIT` takes the end of the conversation;
 * the model wants it in the order it was said. */
private fun List<ChatMessageEntity>.asHistory(): List<Content> =
    reversed().map { content(role = if (it.fromUser) "user" else "model") { text(it.text) } }

/**
 * The coach's standing instructions. Three of the four constraints are the insight prompt's,
 * unchanged — this surface takes free-form user input, which makes them matter more, not less:
 * - **No medical advice** — FitPulse tracks food and bodies; it does not practise medicine, and a
 *   model asked an open question will otherwise reach for symptoms and supplements.
 * - **Only the numbers given** — and, unlike the insight, the coach is told *what it does not
 *   know*, because a user will ask about yesterday, last week and last month. Saying so is the
 *   only alternative to inventing a figure that contradicts the diary two taps away.
 * - **No numbers block at all** when [request] is null: with no profile there is no day to
 *   describe, and a coach that admits it beats one improvising a target.
 */
private fun systemPromptFor(request: InsightRequest?): String = buildString {
    appendLine(
        "You are a friendly nutrition and fitness coach inside FitPulse, a food and body tracking " +
            "app. You are talking to the user who logs their day in it.",
    )
    appendLine()
    if (request == null) {
        appendLine("You have not been given any of this user's figures for today.")
    } else {
        appendLine("Today so far, for a user whose goal is ${request.goal.name.lowercase()} weight:")
        append(dayNumbersBlock(request))
    }
    appendLine()
    appendLine(
        "That summary of today is the only data you have about this user. You cannot see " +
            "yesterday, any past week, individual meals, their weight, age, sex or height. If you " +
            "are asked about anything you were not given, say plainly that you don't have it and " +
            "point them at the app screen that does — the Food tab's diary for meals, the " +
            "Progress tab for weight, measurements and trends.",
    )
    appendLine(
        "Reply in plain conversational text, in the second person, at most three short sentences. " +
            "No markdown, no bullet lists, no headings, no emoji. Give no medical advice, no " +
            "diagnosis, and no supplement or medication suggestions; if you are asked for any of " +
            "those, say that is a question for a doctor or dietitian and offer what the numbers " +
            "above can tell them instead.",
    )
}

private fun ChatMessageEntity.toMessage() = ChatMessage(
    id = id,
    fromUser = fromUser,
    text = text,
    sentAtMillis = sentAtMillis,
)
