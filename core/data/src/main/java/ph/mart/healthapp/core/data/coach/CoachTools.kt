package ph.mart.healthapp.core.data.coach

import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.exercise.totalBurnedKcal
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.dailyTotals
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.fasting.dateEpochDay
import ph.mart.healthapp.core.data.fasting.durationMinutes
import ph.mart.healthapp.core.data.fasting.isActive
import ph.mart.healthapp.core.data.health.SleepNight
import ph.mart.healthapp.core.data.health.SleepRepository
import ph.mart.healthapp.core.data.health.formatDuration
import ph.mart.healthapp.core.data.mood.MOOD_SCALE
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.mood.MoodRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.data.water.WaterRepository

/**
 * The coach's tools: two the app *runs*, two it only ever *drafts*.
 *
 * The split is the whole design. A read is a local Room query with no user-visible effect, so it
 * executes the moment the model asks for it and the answer goes straight back into the same turn.
 * A write is not the coach's to make: `log_food` and `log_water` stop the stream, become a
 * [CoachAction], and wait for the user's tap — the add-entry sheet's confirm step reached through
 * a different door.
 *
 * Two read tools rather than five, because a question is nearly always about one day or about a
 * span, and one round trip beats four. That holds when a domain is added, too: sleep, mood and
 * fasting widened what [formatDay] and [formatHistory] *answer with* rather than earning
 * declarations of their own — a question is still about one day or one span, and a third and
 * fourth function are two more things for the model to pick wrong.
 *
 * Tool names, descriptions and schema text stay in Kotlin: they are model prompts, which the
 * localization rules exempt exactly as they exempt the system instruction below them.
 */

/** 0 is today. A year back is further than any series this app keeps, so anything beyond it is a
 * model that has lost the plot rather than a user asking about last spring. */
internal const val MAX_DAYS_AGO = 365

/** One month of per-day rows is what fits in an answer the user will actually read, and it is the
 * span every "how has this week/month gone?" question means. */
internal const val MAX_HISTORY_DAYS = 30

/**
 * Ceilings on a drafted row, not nutrition advice — they exist so a model that drops a decimal
 * point cannot put 90,000 kcal in front of a Confirm button. A figure past any of these fails the
 * parse and the turn fails with it, the way an over-long reply already does.
 */
internal const val MAX_ACTION_CALORIES = 5000
internal const val MAX_ACTION_MACRO_G = 1000
internal const val MAX_ACTION_GLASSES = 20

internal const val TOOL_GET_DAY = "get_day"
internal const val TOOL_GET_HISTORY = "get_history"
internal const val TOOL_LOG_FOOD = "log_food"
internal const val TOOL_LOG_WATER = "log_water"

/** The two the model may call but the app never executes. Kept as a set rather than a `when` so
 * [CoachRepositoryImpl]'s loop can ask the question without knowing what either one does. */
internal val WRITE_TOOLS = setOf(TOOL_LOG_FOOD, TOOL_LOG_WATER)

/**
 * Days are `days_ago`, never a date string.
 *
 * A model handed a date format invents dates — the wrong year, a timezone's yesterday, a 31st of
 * February. An offset has none of those failure modes, needs no parsing, and maps onto
 * [todayEpochDay] in one subtraction. The cost is that the model cannot express "last Tuesday"
 * directly, which it does not need to: it is told today's date in the system instruction and can
 * count.
 */
private val daysAgoSchema = Schema.integer(
    description = "How many days back. 0 is today, 1 is yesterday. Maximum $MAX_DAYS_AGO.",
)

internal val COACH_TOOLS: Tool = Tool.functionDeclarations(
    listOf(
        FunctionDeclaration(
            name = TOOL_GET_DAY,
            description = "Read one day of the user's diary in full: every food they logged with " +
                "its calories and macros, the day's totals against their targets, water, any " +
                "activity, and their sleep, mood and fasting where they track those. Call this " +
                "before answering anything about a specific day.",
            parameters = mapOf("days_ago" to daysAgoSchema),
        ),
        FunctionDeclaration(
            name = TOOL_GET_HISTORY,
            description = "Read a span of recent days: calories and protein per day, any " +
                "training, any sleep, and any weigh-in. Call this for trends, averages, or " +
                "anything about a week or a month.",
            parameters = mapOf(
                "days" to Schema.integer(
                    description = "How many days back from today, up to $MAX_HISTORY_DAYS.",
                ),
            ),
        ),
        FunctionDeclaration(
            name = TOOL_LOG_FOOD,
            description = "Propose adding a food to today's diary. This does NOT log it: the user " +
                "sees what you proposed and taps to confirm. Estimate the nutrition from what " +
                "they described.",
            parameters = mapOf(
                "name" to Schema.string(description = "What the food is called, e.g. 'Scrambled eggs'."),
                "meal" to Schema.enumeration(
                    values = MealType.entries.map { it.name },
                    description = "Which meal it belongs to.",
                ),
                "calories" to Schema.integer(description = "Calories for the whole portion."),
                "protein_g" to Schema.integer(description = "Protein in grams."),
                "carbs_g" to Schema.integer(description = "Carbohydrates in grams."),
                "fat_g" to Schema.integer(description = "Fat in grams."),
                "portion_amount" to Schema.double(description = "How much, e.g. 2 or 150."),
                "portion_unit" to Schema.string(description = "The unit, e.g. 'g', 'ml', 'serving'."),
            ),
        ),
        FunctionDeclaration(
            name = TOOL_LOG_WATER,
            description = "Propose adding glasses of water to today's total. This does NOT log " +
                "it: the user taps to confirm.",
            parameters = mapOf(
                "glasses" to Schema.integer(
                    description = "How many glasses to ADD, not the new total. Usually 1.",
                ),
            ),
        ),
    ),
)

// region The trust boundary

/**
 * Everything a model can put in front of a Confirm button passes through here, and this is the
 * only way a [CoachAction] is ever made.
 *
 * Null on anything malformed — an unknown tool, a missing field, a string where a number belongs,
 * a negative or absurd figure, a meal name that is not a [MealType]. The caller fails the whole
 * turn on a null, exactly as it does when [sanitizeReply] rejects an answer: a model that cannot
 * fill in eight fields correctly is not one whose draft should be one tap from the diary.
 *
 * A pure function over `kotlinx.serialization` types, which is the point — the photo path parses
 * its response with `org.json` and is untestable on the JVM for it. [CoachToolsTest] is what this
 * file exists in this shape for.
 */
internal fun parseAction(name: String, args: Map<String, JsonElement>): CoachAction? = when (name) {
    TOOL_LOG_FOOD -> parseLogFood(args)
    TOOL_LOG_WATER -> args.int("glasses")
        ?.takeIf { it in 1..MAX_ACTION_GLASSES }
        ?.let(CoachAction::LogWater)
    else -> null
}

private fun parseLogFood(args: Map<String, JsonElement>): CoachAction.LogFood? {
    val name = args.string("name")?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_NAME_CHARS }
        ?: return null
    val meal = args.string("meal")
        ?.let { raw -> MealType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } }
        ?: return null
    val calories = args.int("calories")?.takeIf { it in 0..MAX_ACTION_CALORIES } ?: return null
    val protein = args.macro("protein_g") ?: return null
    val carbs = args.macro("carbs_g") ?: return null
    val fat = args.macro("fat_g") ?: return null
    // The portion is a label on the row, not arithmetic — the calories above are already for the
    // whole portion. So a missing one falls back rather than failing the draft.
    val amount = args.double("portion_amount")?.takeIf { it > 0 && it <= MAX_PORTION_AMOUNT } ?: 1.0
    val unit = args.string("portion_unit")?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_UNIT_CHARS }
        ?: DEFAULT_PORTION_UNIT
    return CoachAction.LogFood(
        name = name,
        mealType = meal,
        calories = calories,
        proteinG = protein,
        carbsG = carbs,
        fatG = fat,
        portionAmount = amount,
        portionUnit = unit,
    )
}

private const val MAX_NAME_CHARS = 60
private const val MAX_UNIT_CHARS = 16
private const val MAX_PORTION_AMOUNT = 10_000.0

/** Stays in Kotlin: it is persisted on the row, the rule every portion unit in this app follows. */
private const val DEFAULT_PORTION_UNIT = "serving"

/**
 * Gemini returns numbers as JSON numbers but has been known to quote them, so a quoted "140" is
 * read rather than rejected — that is a formatting wobble, not a wrong answer. A non-numeric
 * string still yields null, because [JsonPrimitive.intOrNull] is what decides and it does not
 * guess.
 */
private fun Map<String, JsonElement>.int(key: String): Int? =
    (this[key] as? JsonPrimitive)?.intOrNull

private fun Map<String, JsonElement>.double(key: String): Double? =
    (this[key] as? JsonPrimitive)?.doubleOrNull

private fun Map<String, JsonElement>.macro(key: String): Int? =
    int(key)?.takeIf { it in 0..MAX_ACTION_MACRO_G }

/** Null for a JSON number or boolean, not its text: a name that arrived as `true` is a broken
 * call, and coercing it would put the word "true" in the diary. */
private fun Map<String, JsonElement>.string(key: String): String? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    if (!primitive.isString) return null
    if (primitive.intOrNull != null || primitive.booleanOrNull != null) return null
    return primitive.content
}

/** 0 is today and the clamp is silent, because a model asking for day -3 or day 900 means
 * "recently" and failing the turn over it helps nobody. */
internal fun daysAgoOf(args: Map<String, JsonElement>): Int =
    (args.int("days_ago") ?: 0).coerceIn(0, MAX_DAYS_AGO)

internal fun historyDaysOf(args: Map<String, JsonElement>): Int =
    (args.int("days") ?: 7).coerceIn(1, MAX_HISTORY_DAYS)

// endregion

// region What a tool answers with

/**
 * Plain text, not JSON.
 *
 * The same call `sanitizeInsight` makes for the insight and the coach's own reply: the model reads
 * prose at least as well as it reads a nested object, and a plain string is a pure function's
 * output that a JVM test can assert on character for character. The shape deliberately echoes
 * `dayNumbersBlock()` so a tool result and the day block in the system instruction cannot
 * describe the same day two different ways.
 */
internal fun formatDay(
    label: String,
    foods: List<FoodEntry>,
    targetCalories: Int?,
    waterGlasses: Int,
    exercise: List<ExerciseEntry>,
    sleepMinutes: Int? = null,
    mood: MoodDay? = null,
    fastedMinutes: Int? = null,
): String = buildString {
    appendLine("$label:")
    if (foods.isEmpty()) {
        appendLine("- No food logged.")
    } else {
        foods.forEach {
            appendLine(
                "- ${it.name} (${it.mealType.name}): ${it.calories} kcal, " +
                    "${it.proteinG}P/${it.carbsG}C/${it.fatG}F",
            )
        }
        val totals = foods.dailyTotals()
        val target = targetCalories?.let { " of $it" } ?: ""
        appendLine(
            "Totals: ${totals.calories}$target kcal, ${totals.proteinG}P/${totals.carbsG}C/" +
                "${totals.fatG}F",
        )
    }
    appendLine("Water: $waterGlasses glasses")
    if (exercise.isEmpty()) {
        appendLine("No activity logged.")
    } else {
        exercise.forEach {
            val name = it.name.ifEmpty { it.type.name }
            appendLine("- Activity: $name, ${it.minutes} min, ${it.burnedKcal} kcal burned")
        }
    }
    // The three that are *omitted* rather than reported empty. Food, water and activity are things
    // the user does in this app, so a zero there is a fact worth stating; sleep comes off a watch,
    // fasting and the mood check-in are opt-in surfaces, and a daily "No sleep recorded" would
    // have the coach nagging about a feature that is not switched on. The system instruction
    // carries the other half of this: a category absent from a day is one the user does not track.
    sleepMinutes?.let { appendLine("Slept: ${formatDuration(it)}") }
    mood?.describe()?.let(::appendLine)
    fastedMinutes?.let { appendLine("Fasted: ${formatDuration(it)}") }
}

/** Null when neither half was tapped, and each half omitted on its own: `mood_day` stores 0 for
 * "not set", which is the reading [MoodDay]'s own doc gives it — never a zero score. */
private fun MoodDay.describe(): String? {
    val parts = listOfNotNull(
        "mood ${mood}/${MOOD_SCALE.last}".takeIf { mood in MOOD_SCALE },
        "energy ${energy}/${MOOD_SCALE.last}".takeIf { energy in MOOD_SCALE },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(", ", prefix = "Felt: ")
}

/**
 * One line per day, newest last.
 *
 * Two rules the shape exists for. **Days with nothing logged are named rather than dropped** —
 * `observeDailyNutrition()` returns a dense zero-filled series, so a silent omission would let the
 * model average over days the user never opened the app and report a number nobody ate.
 *
 * And **a weigh-in is reported as a change, never as a weight.** `InsightRequest` sends
 * `weightDeltaKg` and has never sent an absolute figure, for the data-minimisation reason the
 * 30-day health backfill is written against — and a tool is not a loophole in that rule just
 * because the user asked the question out loud. A delta answers "is this going the right way?"
 * in full, which is the only thing anyone asks a coach about a trend; a model that knows the user
 * weighs 94.2 kg answers a different, unasked question. The first weigh-in in a window has nothing
 * to compare against and says so, the distinction [WeightEntry] trends already draw.
 */
internal fun formatHistory(
    days: Int,
    nutrition: List<DayNutrition>,
    weights: List<WeightEntry>,
    today: Long,
    exercise: List<ExerciseEntry> = emptyList(),
    sleep: List<SleepNight> = emptyList(),
): String = buildString {
    val from = today - days + 1
    val deltas = weightDeltas(weights, from, today)
    val byDay = nutrition.filter { it.dateEpochDay in from..today }.associateBy { it.dateEpochDay }
    // A day's whole training, not one line per session: a week of two-a-days would otherwise be
    // fourteen lines of an answer that has six of them to spend.
    val training = exercise.filter { it.dateEpochDay in from..today }.groupBy { it.dateEpochDay }
    val slept = sleep.filter { it.dateEpochDay in from..today }.associateBy { it.dateEpochDay }
    if (byDay.values.none { it.isLogged } && deltas.isEmpty() && training.isEmpty() && slept.isEmpty()) {
        return "Nothing logged in the last $days days."
    }
    appendLine("The last $days days, oldest first:")
    // The window is what the lines are counted off, not the nutrition series: it is dense and
    // zero-filled today, but a day carrying only a workout or a night's sleep must still get a
    // line, and iterating the range is what makes that true of any series shape.
    (from..today).forEach { date ->
        val ago = (today - date).toInt()
        val label = when (ago) {
            0 -> "Today"
            1 -> "Yesterday"
            else -> "$ago days ago"
        }
        val day = byDay[date]
        val food = if (day?.isLogged == true) {
            "${day.calories} kcal, ${day.proteinG}g protein"
        } else {
            "nothing logged"
        }
        val activity = training[date]?.let {
            ", ${it.sumOf { entry -> entry.minutes }} min activity, ${it.totalBurnedKcal()} kcal burned"
        }.orEmpty()
        val night = slept[date]?.let { ", slept ${formatDuration(it.minutesAsleep)}" }.orEmpty()
        appendLine("- $label: $food$activity$night${deltas[date].orEmpty()}")
    }
}

/**
 * Each weigh-in in the window against the one before it — including a weigh-in from *before* the
 * window, which is what makes the oldest day in a span carry a change rather than a shrug.
 */
private fun weightDeltas(weights: List<WeightEntry>, from: Long, to: Long): Map<Long, String> {
    val sorted = weights.sortedBy { it.dateEpochDay }
    return buildMap {
        sorted.forEachIndexed { index, entry ->
            if (entry.dateEpochDay !in from..to) return@forEachIndexed
            val prior = sorted.getOrNull(index - 1)
            put(
                entry.dateEpochDay,
                if (prior == null) {
                    ", weighed in (first one, nothing to compare against)"
                } else {
                    ", weighed in (%+.1f kg since the last)".format(entry.weightKg - prior.weightKg)
                },
            )
        }
    }
}

// endregion

/**
 * The repositories a read tool reaches, behind one call.
 *
 * It sits here rather than in [CoachRepositoryImpl] so that file stays about the conversation and
 * this one stays about the tools. Cross-domain reach inside `:core:data` is the shape
 * `insight/InsightContext.kt` already has — it combines these same five repositories — and none of
 * it crosses a module boundary.
 *
 * `.first()` on each flow: a tool answers about an instant, and the turn it answers into is
 * already over by the time the next glass of water lands.
 */
internal class CoachToolbox(
    private val foodRepository: FoodRepository,
    private val progressRepository: ProgressRepository,
    private val waterRepository: WaterRepository,
    private val exerciseRepository: ExerciseRepository,
    private val profileRepository: ProfileRepository,
    // The three the coach could not see at all: a training week, a bad night, an opt-in check-in.
    // They widen what the two existing tools *answer with* rather than adding tools of their own —
    // a question is still about one day or one span, and three more function declarations would be
    // three more things for the model to pick wrong.
    private val sleepRepository: SleepRepository,
    private val moodRepository: MoodRepository,
    private val fastingRepository: FastingRepository,
) {
    /** Null for a tool this does not run — which is every write tool, and is how the caller's loop
     * tells a question from an instruction without a second lookup. */
    suspend fun runTool(name: String, args: Map<String, JsonElement>): String? = when (name) {
        TOOL_GET_DAY -> getDay(daysAgoOf(args))
        TOOL_GET_HISTORY -> getHistory(historyDaysOf(args))
        else -> null
    }

    private suspend fun getDay(daysAgo: Int): String {
        val today = todayEpochDay()
        val date = today - daysAgo
        val label = when (daysAgo) {
            0 -> "Today"
            1 -> "Yesterday"
            else -> "$daysAgo days ago"
        }
        return formatDay(
            label = label,
            foods = foodRepository.observeEntries(date).first(),
            targetCalories = profileRepository.observeProfile().first()?.dailyTargets()?.calories,
            waterGlasses = waterRepository.observeDay(date).first(),
            exercise = exerciseRepository.observeEntries(date).first(),
            sleepMinutes = sleepRepository.observeNights().first()
                .firstOrNull { it.dateEpochDay == date }?.minutesAsleep,
            mood = moodRepository.observeDays().first().firstOrNull { it.dateEpochDay == date },
            // Finished fasts only, and a finished one is dated by the day it *ended* — the reading
            // `FastSession.dateEpochDay` already gives it, and the reason the clock passed to
            // `durationMinutes` is never read for one.
            fastedMinutes = fastingRepository.observeSessions().first()
                .firstOrNull { !it.isActive && it.dateEpochDay == date }
                ?.durationMinutes(System.currentTimeMillis()),
        )
    }

    /** `observeRecentEntries()` is already windowed to a year and `observeNights()` to what the
     * watch has sent, so a span reads two flows rather than one per day. */
    private suspend fun getHistory(days: Int): String = formatHistory(
        days = days,
        nutrition = foodRepository.observeDailyNutrition().first(),
        weights = progressRepository.observeWeightEntries().first(),
        today = todayEpochDay(),
        exercise = exerciseRepository.observeRecentEntries().first(),
        sleep = sleepRepository.observeNights().first(),
    )
}

/** The envelope the SDK requires around a [String] result. One key, because the result is prose
 * and prose has no fields. */
internal fun toolResponse(result: String): JsonObject =
    JsonObject(mapOf("result" to JsonPrimitive(result)))
