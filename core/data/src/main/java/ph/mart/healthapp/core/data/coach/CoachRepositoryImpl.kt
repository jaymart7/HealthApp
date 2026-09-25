package ph.mart.healthapp.core.data.coach

import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.ThinkingConfig
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.UsageMetadata
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.aiModel
import ph.mart.healthapp.core.data.coach.local.ChatMessageDao
import ph.mart.healthapp.core.data.coach.local.ChatMessageEntity
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureReading
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureRepository
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.exercise.RoutineRepository
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.insight.InsightRequest
import ph.mart.healthapp.core.data.insight.dayNumbersBlock
import ph.mart.healthapp.core.data.mood.MOOD_SCALE
import ph.mart.healthapp.core.data.mood.MoodRepository
import ph.mart.healthapp.core.data.note.NoteRepository
import ph.mart.healthapp.core.data.profile.displayUnitToKg
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.progress.fromDisplay
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.recap.REPORT_DAYS
import ph.mart.healthapp.core.data.supplement.SupplementRepository
import ph.mart.healthapp.core.data.logAiFailure
import ph.mart.healthapp.core.data.logAiUsage
import ph.mart.healthapp.core.data.epochDayStartMillis
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.data.water.WaterDay
import ph.mart.healthapp.core.data.water.WaterRepository

/**
 * Room for the answer, the tool calls that reach it, and the reasoning that picks them.
 *
 * Raised from 300 when the coach got tools: a turn can now spend tokens deciding to call `get_day`,
 * reading the result, and *then* writing a six-line list. All three come out of one budget — the
 * lesson `AI_THINKING` is written against.
 *
 * Raised again from 700 when the coach could design library items: a meal plan's `save_meal` calls
 * carry every food's figures as arguments, and those come out of this same budget.
 */
private const val MAX_OUTPUT_TOKENS = 2000

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
 * The model is a field, and everything it is built from is fixed text: [COACH_SYSTEM_PROMPT] and
 * the tools are byte-identical on every request from every user, which is what lets Gemini's
 * implicit cache hold those ~6k tokens instead of re-reading them each round. The day's numbers
 * still move while the screen is open — a glass logged in another tab must not leave the coach
 * quoting a stale figure — so they ride the question itself, read fresh on every [send] by
 * [contextFor]. Put back at the head of the system instruction, any log would change the prefix
 * and cost the cache the whole of it.
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
    private val exerciseRepository: ExerciseRepository,
    // The fourth `settle` writes through. A read goes through the toolbox; a write goes through
    // the ordinary repository the matching sheet uses, which is what makes a coach-drafted row
    // indistinguishable from a hand-typed one.
    private val progressRepository: ProgressRepository,
    // The fifth, and the second whose write call takes a day's *total* rather than a delta — which
    // is why `supplementDoses()` sums before anything reaches it.
    private val supplementRepository: SupplementRepository,
    // The sixth and seventh. Both are manual-entry-only domains — a cuff reading has no provider at
    // all and a mood is two taps — so the coach is a second door onto them rather than a third.
    // `progressRepository` above already covers the measurements, which share the weigh-in's table
    // neighbourhood and its unit rule.
    private val moodRepository: MoodRepository,
    private val bloodPressureRepository: BloodPressureRepository,
    // The eighth, and the only one whose write is a *state transition* rather than a row: a
    // confirmed fast draft starts or stops the timer Home's card and the widget already drive.
    private val fastingRepository: FastingRepository,
    // The ninth, and the only one the coach could already *read* before it could write: `get_day`
    // has handed the model the day's note since the note shipped.
    private val noteRepository: NoteRepository,
    // The tenth, for a routine the coach designed; starting one still writes nothing.
    private val routineRepository: RoutineRepository,
    private val toolbox: CoachToolbox,
) : CoachRepository {

    private val model = aiModel(
        generationConfig = generationConfig {
            maxOutputTokens = MAX_OUTPUT_TOKENS
            thinkingConfig = COACH_THINKING
        },
        tools = listOf(COACH_TOOLS),
        systemInstruction = content { text(COACH_SYSTEM_PROMPT) },
    )

    override fun observeMessages(): Flow<List<ChatMessage>> =
        dao.observeAll().map { messages -> messages.map { it.toMessage() } }

    override fun send(question: String, request: InsightRequest?): Flow<CoachReply> = flow {
        // Read before the builder, not inside it: `content {}` takes a plain lambda and a profile
        // read is suspending — the same reason a tool read runs above `content` further down.
        val context = contextFor(request, toolbox.dietLine(), toolbox.profileLine())

        val todayStart = epochDayStartMillis(todayEpochDay())
        val history = dao.recent(MAX_HISTORY_MESSAGES).filter { it.sentAtMillis >= todayStart }
        val chat = model.startChat(history = history.asHistory())
        val raw = StringBuilder()
        // The context is a part of this message only: history is rebuilt from Room with the bare
        // question, so an old turn never carries old numbers and the prefix stays append-only.
        var message: Content = content(role = "user") { text(context); text(question) }
        // The window a `show_report` round asked for, carried to `finish` and written on the
        // answer row. Null on every other turn, which is nearly all of them.
        var reportDays: Int? = null

        repeat(MAX_TOOL_ROUNDS) {
            val calls = mutableListOf<FunctionCallPart>()
            var usage: UsageMetadata? = null
            chat.sendMessageStream(message).collect { chunk ->
                usage = chunk.usageMetadata ?: usage
                chunk.text?.let(raw::append)
                // The accumulated answer, not the chunk: a bubble renders the whole of it, so the
                // whole of it is what has to clear the trust boundary. A partial past
                // MAX_REPLY_CHARS sanitizes to null and the bubble simply stops growing — the
                // check below then fails the send, rather than truncating.
                sanitizeReply(raw.toString())?.let { emit(CoachReply.Partial(it)) }
                calls += chunk.functionCalls
            }
            logAiUsage("coach round", usage)

            // Nothing asked for: the model has said its piece and this is an ordinary answer.
            if (calls.isEmpty()) return@flow finish(question, raw.toString(), reportDays)

            // A write call ends the turn here, unpersisted. It is a draft, and the user is the one
            // who decides whether it becomes a row — so `settle` is what writes, not this.
            //
            // *Every* write call, not the first: one meal is several `log_food` calls in one
            // round, and taking one of them left the other two out of a card sitting under an
            // answer that said all three were drafted. `parseAction` and `priced` are unchanged —
            // each call clears the same boundary it always did, one at a time.
            val writes = calls.filter { it.name in WRITE_TOOLS }
            if (writes.isNotEmpty()) {
                // One card, one kind — `navigatingDraftStandsAlone()`'s rule, one tool earlier. A
                // draft and a report in the same round are two cards under one answer, and the
                // report would be the one silently dropped: the write branch returns from here.
                if (calls.any { it.name == TOOL_SHOW_REPORT }) return@flow emit(CoachReply.Failed)
                // One bad call fails the whole turn, the rule a lone bad call already followed: a
                // meal missing the row that would not parse is a meal the user logs without
                // noticing. Same for a draft past the row ceiling — rejected, never truncated.
                //
                // `resolve` is the second half of the boundary and can return several rows for one
                // call: a saved meal is one row per item, so `flatMap` is what flattens "log my
                // usual breakfast and a coffee" into one card.
                val today = todayEpochDay()
                val actions = writes.flatMap { call ->
                    parseAction(call.name, call.args, today)?.resolve(toolbox)
                        ?: return@flow emit(CoachReply.Failed)
                }
                if (actions.size > MAX_DRAFT_ROWS) return@flow emit(CoachReply.Failed)
                // One card, one day. A draft whose rows disagree could only ever be labelled
                // correctly for some of them, and the card's promise is that what it shows is what
                // gets written.
                if (actions.draftDay(today) == null) return@flow emit(CoachReply.Failed)
                // And one card, one kind, where that kind is a routine: its Confirm leaves the
                // screen, so it cannot also be the tap that writes a meal.
                if (!actions.navigatingDraftStandsAlone()) return@flow emit(CoachReply.Failed)
                // And one card, one fasting transition: a draft holding two would start and end a
                // fast on the same tap. It may still ride beside rows — "I broke my fast with two
                // eggs" is one sentence — which is where it parts company with a routine.
                if (!actions.fastDraftIsSingular()) return@flow emit(CoachReply.Failed)
                return@flow emit(CoachReply.Proposal(actions))
            }

            // Whatever prose came with a *read* call is "let me check yesterday", not an answer,
            // so it is dropped rather than carried into the round that answers — otherwise the
            // two concatenate, unseparated, into the bubble and into the row `finish` writes. The
            // empty partial hands the screen back its thinking mascot while the tool runs.
            //
            // Below the write check, never above it: a write call's prose *is* the answer, and it
            // is the copy `settle` persists. And a turn that spends every round reaching for tools
            // now arrives at `finish` with nothing, which already reads as a failure — which is
            // the honest ending, rather than persisting "let me look that up" as the reply.
            raw.setLength(0)
            emit(CoachReply.Partial(""))

            // A report is neither a read nor a draft: nothing of it goes into the answer and
            // there is nothing to confirm. The window is stamped here and the card is drawn from
            // Room once `finish` has written the row — so what goes back to the model is an
            // instruction, not data. It cannot misquote a figure it was never handed, and the one
            // sentence it writes has the whole of MAX_REPLY_CHARS to itself.
            //
            // A malformed window fails the turn rather than picking one, `parseAction`'s rule.
            calls.firstOrNull { it.name == TOOL_SHOW_REPORT }?.let { call ->
                reportDays = parseShowReport(call.args) ?: return@flow emit(CoachReply.Failed)
            }

            // Reads run now and go straight back into the same turn. Run before the builder, not
            // inside it: `content {}` takes a plain lambda and a tool read is suspending.
            val results = calls.map {
                val answer = if (it.name == TOOL_SHOW_REPORT) REPORT_DRAWN
                else toolbox.runTool(it.name, it.args) ?: UNKNOWN_TOOL
                it to answer
            }
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
        finish(question, raw.toString(), reportDays)
    }.catch { e ->
        // `catch` rather than a `try` around the loop: wrapping an `emit` in `catch (e: Exception)`
        // swallows the CancellationException downstream cancellation throws back through it.
        logAiFailure("coach send", e)
        emit(CoachReply.Failed)
    }

    /** The ordinary ending: sanitize, write the pair, say nothing more — flow completion is the
     * success signal, which is why there is no `Answered` variant to emit here. */
    private suspend fun FlowCollector<CoachReply>.finish(
        question: String,
        raw: String,
        reportDays: Int? = null,
    ) {
        val answer = sanitizeReply(raw) ?: return emit(CoachReply.Failed)
        writeExchange(question, answer, report = reportDays)
    }

    /**
     * By kind, not row by row. The foods go down as **one** `addEntries`, so a drafted meal appears
     * in the diary at once rather than as four rows arriving in sequence — the same call, for the
     * same reason, that `FoodViewModel.onLogSavedMeal` makes. The glasses are summed into a single
     * `setToday` for a stronger reason: it takes the day's *new total*, so two proposals applied
     * one after the other would have the second overwrite the first.
     */
    override suspend fun settle(
        question: String,
        answer: String,
        actions: List<CoachAction>,
        receipt: String?,
        report: Int?,
    ) {
        actions.foodEntries().takeIf { it.isNotEmpty() }?.let { foodRepository.addEntries(it) }

        // Changes to rows already there, through the calls the diary's own edit sheet and swipe
        // make — so a coach-made correction is indistinguishable from a hand-made one, and a
        // delete is the same soft delete. `resolve` built each `after` from the row as it stood.
        actions.forEach {
            when (it) {
                is CoachAction.EditFood -> it.after?.let { entry -> foodRepository.updateEntry(entry) }
                is CoachAction.DeleteFood -> foodRepository.deleteEntry(it.entryId)
                is CoachAction.EditExercise -> it.after?.let { entry -> exerciseRepository.updateEntry(entry) }
                is CoachAction.DeleteExercise -> exerciseRepository.deleteEntry(it.entryId)
                // New library items, through the calls the library's own editors make.
                is CoachAction.SaveMeal -> foodRepository.saveMeal(it.name, it.items)
                is CoachAction.SaveRecipe -> foodRepository.saveRecipe(it.name, it.servings, it.items)
                is CoachAction.CreateRoutine -> routineRepository.addRoutine(it.name, it.lifts, it.days)
                else -> Unit
            }
        }

        // A corrected total goes down *before* any glasses are added, so "I had five, and one more
        // just now" lands as six rather than as five.
        actions.filterIsInstance<CoachAction.SetWater>().lastOrNull()?.let {
            val day = it.dateEpochDay.takeIf { d -> d > 0 } ?: todayEpochDay()
            waterRepository.upsertDay(WaterDay(dateEpochDay = day, glasses = it.glasses))
        }

        // Added to the day, never assigned: a coach that proposes "one glass" must not wipe the
        // six already there. `observeDay`/`upsertDay` rather than `setToday`, which is that pair
        // with today baked in — one path now that a draft can name a past day.
        actions.glassesToAdd().forEach { (date, glasses) ->
            val day = date.takeIf { it > 0 } ?: todayEpochDay()
            val current = waterRepository.observeDay(day).first()
            waterRepository.upsertDay(WaterDay(dateEpochDay = day, glasses = current + glasses))
        }

        // `dateEpochDay` rides through at whatever the card drew — zero for today, which is what
        // `ExerciseRepository.addEntry` already reads it as. One call each: it has no batch write,
        // and two workouts in one draft is not the shape anyone asks for.
        actions.filterIsInstance<CoachAction.LogExercise>().forEach {
            exerciseRepository.addEntry(
                ExerciseEntry(
                    dateEpochDay = it.dateEpochDay,
                    type = it.type,
                    name = it.name,
                    minutes = it.minutes,
                    burnedKcal = it.burnedKcal,
                ),
            )
        }

        // Keyed on the day, so a second weigh-in today *replaces* today's rather than appending —
        // the weigh-in sheet's own behaviour, and what lets the card's change line be read as
        // "this is what it is about to overwrite". The conversion happens here and nowhere else:
        // the figure on the card is the one the user said, in the unit they said it in.
        actions.filterIsInstance<CoachAction.LogWeight>().forEach {
            progressRepository.upsertWeightEntry(
                WeightEntry(
                    dateEpochDay = todayEpochDay(),
                    weightKg = it.weight.displayUnitToKg(it.unit),
                ),
            )
        }

        // Read once for the whole draft: `setTakenToday` takes the day's new count, so each id
        // needs what is already there. A dose past the supplement's own `timesPerDay` is clamped by
        // the repository and lands as a no-op — deliberately not `nextTaken()`, which wraps back to
        // zero: "I took it" must never untick a completed day.
        val doses = actions.supplementDoses()
        if (doses.isNotEmpty()) {
            val today = supplementRepository.observeToday().first()
            doses.forEach { (id, added) ->
                val taken = today.firstOrNull { it.supplement.id == id }?.taken ?: 0
                supplementRepository.setTakenToday(id, taken + added)
            }
        }

        // Folded to one row before anything is written: a mood is an absolute value, so two rows
        // applied in sequence would have the second's empty column blank the first's. Each column
        // is set only when it was drafted — `setTodayMood`/`setTodayEnergy` leave the other alone,
        // which is what lets "I felt great" record a mood without claiming an energy.
        actions.moodToSet()?.let { mood ->
            if (mood.mood > 0) moodRepository.setTodayMood(mood.mood)
            if (mood.energy > 0) moodRepository.setTodayEnergy(mood.energy)
        }

        // Stamped now, because now is when they told us — the table is keyed per reading rather
        // than per day, so a morning and an evening are two rows and neither overwrites the other.
        actions.filterIsInstance<CoachAction.LogBloodPressure>().forEach {
            bloodPressureRepository.addReading(
                BloodPressureReading(
                    takenAtMillis = System.currentTimeMillis(),
                    systolic = it.systolic,
                    diastolic = it.diastolic,
                    pulseBpm = it.pulseBpm,
                ),
            )
        }

        // The weigh-in's rule one table over: the conversion happens here and nowhere else, so the
        // figure on the card is the one the user said, in the unit they said it in. Keyed on part
        // and day, so a second waist today replaces today's rather than appending.
        actions.filterIsInstance<CoachAction.LogMeasurement>().forEach {
            progressRepository.upsertMeasurementEntry(
                MeasurementEntry(
                    part = it.part,
                    dateEpochDay = todayEpochDay(),
                    value = it.part.fromDisplay(it.value, it.unit),
                ),
            )
        }

        // Folded before the write for `moodToSet()`'s reason and half of it: a day holds one
        // note, so the last one the user agreed to is the one that lands. `setNote` is the sheet's
        // own call, so the trim and the 500-character cap stay in `NoteRepositoryImpl` — and the
        // day is the card's, which is the day the note is *about* rather than the day it was said.
        actions.noteToWrite()?.let {
            noteRepository.setNote(it.dateEpochDay.takeIf { d -> d > 0 } ?: todayEpochDay(), it.text)
        }

        // The one settled action that writes no row at all: it flips the timer Home's card and
        // the widget drive. Both calls already no-op against a state that has moved — `start()`
        // while a fast is open, `stop()` while none is — so a card left on screen while the user
        // starts a fast in another tab does nothing rather than something wrong. `resolve` is what
        // makes that a rarity instead of the normal case.
        actions.filterIsInstance<CoachAction.SetFast>().singleOrNull()?.let {
            if (it.ending) fastingRepository.stop() else fastingRepository.start(it.goalHours)
        }

        writeExchange(question, answer, receipt, report)
    }

    /** The one write, shared by both endings, so "a question is only persisted once it has been
     * answered" stays one rule with one implementation. */
    private suspend fun writeExchange(
        question: String,
        answer: String,
        receipt: String? = null,
        report: Int? = null,
    ) {
        val now = System.currentTimeMillis()
        dao.addExchange(
            question = ChatMessageEntity(fromUser = true, text = question, sentAtMillis = now),
            // One millisecond apart so the ascending sort can never render the reply first; the
            // id tie-break in the DAO covers a clock that doesn't move between the two.
            answer = ChatMessageEntity(
                fromUser = false,
                text = answer,
                sentAtMillis = now + 1,
                // Only ever on the answer: a receipt is about what the coach's turn did, and the
                // question row is the user's own words.
                receipt = receipt,
                // The same reading one column over, and the same reason it is a column rather
                // than something the answer carries: the card is the app reporting, not the coach
                // talking. Storing the *window* and not the figures is what lets a report reopened
                // next week be re-folded against the rows as they are then.
                report = report,
            ),
        )
    }

    override suspend fun clear() = dao.softDeleteAll()
}

/** What a read tool answers when the model invents a name. Stays in Kotlin: it is prompt text the
 * user never sees. */
private const val UNKNOWN_TOOL = "That tool does not exist."

/** What `show_report` answers with. Deliberately an instruction and not data — see the call site.
 * Stays in Kotlin for [UNKNOWN_TOOL]'s reason: the user never sees it. */
private const val REPORT_DRAWN =
    "The report card is now on screen and carries every figure itself. Introduce it in one short " +
        "sentence and state no numbers from it."

/** [ChatMessageDao.recent] returns newest-first so `LIMIT` takes the end of the conversation;
 * the model wants it in the order it was said. */
private fun List<ChatMessageEntity>.asHistory(): List<Content> =
    reversed().map { content(role = if (it.fromUser) "user" else "model") { text(it.text) } }

/**
 * The coach's standing instructions — fixed text, and it has to stay fixed: this and the tools are
 * the prefix Gemini's implicit cache holds, so anything that varies by user or by minute belongs
 * in [contextFor] instead.
 *
 * The paragraph that used to list what the coach *cannot* see is gone, because it is no longer
 * true: `get_day` and `get_history` reach any day the app has. What replaces it is narrower and
 * more important — **call a tool rather than guess.** A model that can read the diary and answers
 * from memory anyway is worse than one that admitted it had nothing, because the figure it invents
 * now looks sourced.
 *
 * Two constraints survive the rewrite untouched:
 * - **No medical advice.** A coach that reaches further is a coach a user trusts further, so this
 *   matters more than it did, not less — and it grew a clause when the tools began carrying blood
 *   pressure: the band on a reading is the app's, handed over by `categoryOf()`, and the model may
 *   repeat it but never derive one and never say what a reading means. `log_blood_pressure` asks
 *   nothing more of it — a drafted reading is two numbers read back, and the card's band is
 *   `categoryOf()`'s too.
 * - **No numbers block at all** when there is no profile — [contextFor]'s half of the rule now: with
 *   no profile there is no target to be over or under, and a coach that admits it beats one
 *   improvising one. The tools still work — a diary can be read without a profile.
 */
private val COACH_SYSTEM_PROMPT: String = buildString {
    appendLine(
        "You are a friendly nutrition and fitness coach inside FitPulse, a food and body tracking " +
            "app. You are talking to the user who logs their day in it.",
    )
    appendLine()
    appendLine(
        "Their latest message opens with a block the app adds, not words they typed: where their " +
            "day stands right now — their goal and today's numbers against their targets, their " +
            "profile and their diet — and today's internal day number. It is the current state of " +
            "their day; nothing in it needs looking up again.",
    )
    appendLine()
    appendLine(
        "You can read the rest of their diary with tools. Use get_day for any single day — it " +
            "returns every food they logged with its calories and macros, their water and their " +
            "activity. Use get_history for a week, a month, a trend or an average, and " +
            "get_library for the meals, recipes, foods and workout routines they have saved. " +
            "Days are given as how many days back from today, where 0 is today and 1 is yesterday, " +
            "so just count backwards. Never state a figure " +
            "you were not given or did not read from a tool — call the tool instead of guessing, " +
            "and if a tool comes back empty, say plainly that nothing was logged. Do not narrate " +
            "that you are about to look something up: call the tool and answer. A day may also " +
            "carry their steps against their step goal, their sleep, how they felt, a " +
            "completed fast, the supplements they ticked off against what was due, their heart " +
            "rate, any blood-pressure readings they took, their weigh-in, their body " +
            "measurements and their cycle; a span " +
            "carries their water, their training, their steps, their sleep, their supplements, " +
            "their heart rate, their blood pressure, their period days and every " +
            "weigh-in and body measurement with its figure and its change since the one before, " +
            "in their own units. Where one of those is missing " +
            "from a day, the user does not track it at all — answer with what is there and do " +
            "not ask them for it. Water is the exception and is given for every day of a span, " +
            "so a zero there means they logged none that day, not that they do not track it.",
    )
    appendLine(
        "If the user asks you to log something, call log_food, log_water, log_exercise, " +
            "log_saved_meal, log_weight, log_supplement, log_mood, log_blood_pressure, " +
            "log_measurement, log_note or log_fast. These " +
            "do not log anything themselves: the user sees what you drafted and taps to confirm " +
            "it, so say what you are proposing in the same reply. Call log_food once per food: a " +
            "meal of three things is three calls in the same turn, and they are drafted together " +
            "as one card. Estimate the nutrition of a " +
            "food from their description; do not estimate the calories an activity burned, " +
            "because the app works that out from their own weight; and if they name one of their " +
            "own saved meals or recipes, call get_library for its exact name and then " +
            "log_saved_meal with it rather than retyping what is in it. If the user tells you " +
            "what they weigh, call log_weight with the number exactly as they said it. If they " +
            "say they took one of their own supplements, call get_library " +
            "for its exact name and then log_supplement with it — only ever one they already " +
            "take, and never as a suggestion. To log something for an earlier day, pass days_ago " +
            "on the same call — 1 for yesterday, up to $MAX_DRAFT_DAYS_AGO — and say which day " +
            "you are proposing; every row of one draft has to be for the same day, so draft two " +
            "days as two separate turns. A weigh-in, a supplement, a mood, a blood-pressure " +
            "reading and a measurement are always today; a note can name an earlier day, " +
            "the way a food can.",
    )
    appendLine(
        "You can also change what is already logged. Call get_day for that day first — every " +
            "food and activity in it carries an id like #123 — then call edit_food, " +
            "edit_exercise or delete_entry with that id and the same days_ago, up to " +
            "$MAX_DRAFT_DAYS_AGO days back. Never guess an id. Pass only what they asked to " +
            "change; for a different amount of a food pass portion_amount alone and the app " +
            "reprices it, and never set the calories burned for an activity. To correct a day's " +
            "water to a total they give you, call set_water; to add glasses, log_water. Nothing " +
            "changes until they confirm, so say what you are proposing.",
    )
    appendLine(
        "Three of those record something they told you about themselves, and all three follow " +
            "log_weight's rule: the number is theirs and you are only reading it back. If they " +
            "say how they felt or how much energy they had, call log_mood " +
            "with whichever of the two they mentioned, on a scale of ${MOOD_SCALE.first} to " +
            "${MOOD_SCALE.last} where ${MOOD_SCALE.last} is best, and leave the other one out. If " +
            "they give you a blood-pressure reading, call log_blood_pressure with the numbers " +
            "exactly as they said them — the app works out which band it falls in, so do not " +
            "categorise it yourself and do not say what it means. If they tell you a body " +
            "measurement, call log_measurement with the figure exactly as they gave it and do not " +
            "convert it; one call per site.",
    )
    appendLine(
        "A note on a day is that rule again, in words rather than numbers. If they ask you to " +
            "note, jot down or remember something about a day, call log_note with what they " +
            "said, in their own wording — you are writing their sentence down, not summarising " +
            "their day in yours. Never write one they did not ask for, and never suggest one. A " +
            "day holds one note, so it replaces whatever is already written there; use days_ago " +
            "for an earlier day, and keep it to a sentence or two.",
    )
    appendLine(
        "Fasting is a timer rather than a log. If they say they are starting a fast, call " +
            "log_fast with start; if they say they are breaking or ending one, call it with end. " +
            "Do not set how long the fast should be — the app uses the goal on their profile — " +
            "and do not propose one for an earlier time or an earlier day, because the tap starts " +
            "or ends it there and then. Only ever when they have said so: never suggest a fast, " +
            "never suggest ending one, and if they are not fasting at all, leave it alone.",
    )
    appendLine(
        "When they ask what to eat, what to have for a meal or what you would recommend, work " +
            "out what is left of their day from the numbers the app gave you and answer with food that fits " +
            "it. Call get_library first and prefer what is already theirs — a saved meal, a " +
            "recipe, or a food they log often — over something new, and say which of theirs it " +
            "is. Estimating the calories and macros of a food you are *suggesting* is expected " +
            "and is not the same thing as stating one of their logged figures, which still only " +
            "ever comes from a tool. Keep portions ordinary and cookable, and once they pick one, " +
            "draft it with log_food or log_saved_meal so they can confirm it into their diary.",
    )
    appendLine(
        "When they ask what to train, which workout to do, or to start one, call get_library and " +
            "answer with one of their own routines — it tells you the lifts in each and which " +
            "weekday each is planned for, so prefer the one planned for today. Then call " +
            "start_routine with its exact name. That logs nothing and records nothing: it opens " +
            "their workout screen already filled in with that routine's lifts, and they save it " +
            "themselves, so say which routine you are proposing. Only ever one of their own, " +
            "and never draft starting a routine in the same turn as anything else. If they have " +
            "no routines, offer to design one.",
    )
    appendLine(
        "You can also add to their library when they ask you to design, plan or save something. " +
            "Call save_meal for a meal — a meal plan is one save_meal per meal in the same turn — " +
            "save_recipe for a recipe with its servings, and create_routine for a workout routine " +
            "with its lifts, sets, reps and, if they said, its weekdays. Check get_library first " +
            "and give each a name they do not already use. Estimate a food's nutrition the way " +
            "you would for log_food. A routine never carries a weight or a load: never say how " +
            "much they should lift. Nothing is saved until they confirm, and saving a meal does " +
            "not log it.",
    )
    appendLine(
        "When they ask to see, open or go to a part of the app — a chart, their diary, a list, " +
            "a setting — call open_screen with the screen that fits. It changes nothing: it puts " +
            "a button under your reply that takes them there, so say where it goes in one short " +
            "sentence. Never call it in the same turn as anything else.",
    )
    appendLine(
        "When they ask for a report, a summary, an overview, or how their week or month has " +
            "gone overall, call show_report with ${REPORT_DAYS.joinToString(" or ")} rather than " +
            "answering in prose. It puts a card on screen carrying their calories and macros, " +
            "their weight, their training and their steps for that window, with charts they can " +
            "open — so introduce it in one short sentence and do not state any figures, because " +
            "you have not been given the ones on it. A specific question about a span is still " +
            "get_history. Never call both in the same turn, and never call show_report in a turn " +
            "where you are also logging something.",
    )
    appendLine(
        "Reply in plain conversational text, in the second person. Keep it to three short " +
            "sentences, or up to six short lines when a list genuinely answers the question " +
            "better — one item per line, starting with \"- \". No markdown, no headings, no bold, " +
            "no emoji. Give no medical advice, no diagnosis, and no supplement or medication " +
            "suggestions; if you are asked for any of those, say that is a question for a doctor " +
            "or dietitian and offer what their logged numbers can tell them instead. A blood " +
            "pressure reading comes with the band the app has already put it in — you may repeat " +
            "that band, but never work one out yourself, never call a reading good or bad, and " +
            "never say what it or a heart rate means for their health. Those are questions for a " +
            "doctor, and you can still tell them which way the numbers have moved. Their cycle is " +
            "context for how they felt, ate or trained and nothing more: never predict a period, " +
            "a fertile window or ovulation, and never make a fertility or contraception claim.",
    )
}

/**
 * Everything the coach is told that moves: the day's numbers, the profile, the diet and today's day
 * number. It rides the question as a part of its own rather than the system instruction — see the
 * class doc for why. Read on every [CoachRepositoryImpl.send], so a glass logged a minute ago is in
 * it.
 */
private fun contextFor(
    request: InsightRequest?,
    dietLine: String?,
    profileLine: String?,
): String = buildString {
    appendLine("[From the app, not typed by the user]")
    if (request == null) {
        appendLine("You have not been given any of this user's targets, because they have not set up a profile yet.")
    } else {
        appendLine("Today so far, for a user whose goal is ${request.goal.name.lowercase()} weight:")
        append(dayNumbersBlock(request))
    }
    profileLine?.let(::appendLine)
    dietLine?.let(::appendLine)
    appendLine("Today is day number ${todayEpochDay()} internally.")
}

private fun ChatMessageEntity.toMessage() = ChatMessage(
    id = id,
    fromUser = fromUser,
    text = text,
    sentAtMillis = sentAtMillis,
    receipt = receipt,
    report = report,
)
