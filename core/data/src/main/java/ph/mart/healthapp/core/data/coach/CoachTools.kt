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
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.estimateBurnedKcal
import ph.mart.healthapp.core.data.exercise.totalBurnedKcal
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.fasting.dateEpochDay
import ph.mart.healthapp.core.data.fasting.durationMinutes
import ph.mart.healthapp.core.data.fasting.isActive
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.RecipeServing
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.dailyTotals
import ph.mart.healthapp.core.data.food.perServing
import ph.mart.healthapp.core.data.food.totalKcal
import ph.mart.healthapp.core.data.health.SleepNight
import ph.mart.healthapp.core.data.health.SleepRepository
import ph.mart.healthapp.core.data.health.StepDay
import ph.mart.healthapp.core.data.health.StepsRepository
import ph.mart.healthapp.core.data.health.formatDuration
import ph.mart.healthapp.core.data.health.formatSteps
import ph.mart.healthapp.core.data.mood.MOOD_SCALE
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.mood.MoodRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.data.profile.round1
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.progress.unitLabel
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.data.water.WaterRepository

/**
 * The coach's tools: three the app *runs*, five it only ever *drafts*.
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

/** Ten hours. Past it the model has read "a 90 minute run" as 900, which is the same dropped
 * decimal [MAX_ACTION_CALORIES] guards against at the other end of the same card. */
internal const val MAX_ACTION_MINUTES = 600

/**
 * A band wide enough to be right in either unit, because the number arrives before the unit does —
 * 20 kg and 44 lb are both weights, and nothing between 1 and 1000 is absurd in one unit while
 * being fine in the other.
 *
 * It is the dropped decimal point [MAX_ACTION_CALORIES] guards against, not a plausibility check:
 * the card shows the figure it will write, so a user who is not 8.2 of anything dismisses it.
 */
internal const val MIN_ACTION_WEIGHT = 1.0
internal const val MAX_ACTION_WEIGHT = 1000.0

internal const val TOOL_GET_DAY = "get_day"
internal const val TOOL_GET_HISTORY = "get_history"
internal const val TOOL_GET_LIBRARY = "get_library"
internal const val TOOL_LOG_FOOD = "log_food"
internal const val TOOL_LOG_WATER = "log_water"
internal const val TOOL_LOG_EXERCISE = "log_exercise"
internal const val TOOL_LOG_SAVED_MEAL = "log_saved_meal"
internal const val TOOL_LOG_WEIGHT = "log_weight"

/** The ones the model may call but the app never executes. Kept as a set rather than a `when` so
 * [CoachRepositoryImpl]'s loop can ask the question without knowing what any of them does. */
internal val WRITE_TOOLS = setOf(
    TOOL_LOG_FOOD,
    TOOL_LOG_WATER,
    TOOL_LOG_EXERCISE,
    TOOL_LOG_SAVED_MEAL,
    TOOL_LOG_WEIGHT,
)

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
                "activity, their steps, and their sleep, mood and fasting where they track " +
                "those. Call this before answering anything about a specific day.",
            parameters = mapOf("days_ago" to daysAgoSchema),
        ),
        FunctionDeclaration(
            name = TOOL_GET_HISTORY,
            description = "Read a span of recent days: calories and protein per day, any " +
                "training, any steps, any sleep, any weigh-in and any change in their body " +
                "measurements. Call this for trends, averages, or anything about a week or a " +
                "month.",
            parameters = mapOf(
                "days" to Schema.integer(
                    description = "How many days back from today, up to $MAX_HISTORY_DAYS.",
                ),
            ),
        ),
        FunctionDeclaration(
            name = TOOL_GET_LIBRARY,
            description = "Read the meals and recipes this user has saved, by name. Call this " +
                "before answering anything about what they usually eat, what they could make, or " +
                "before logging a meal they refer to by name.",
            parameters = emptyMap(),
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
            name = TOOL_LOG_SAVED_MEAL,
            description = "Propose adding one of the user's own saved meals or recipes to today's " +
                "diary, by its exact name from get_library. This does NOT log it: the user sees " +
                "every row and taps to confirm. Do not estimate any of its nutrition — the app " +
                "uses the figures they saved.",
            parameters = mapOf(
                "name" to Schema.string(
                    description = "The saved meal or recipe's name, exactly as get_library gave it.",
                ),
                "meal" to Schema.enumeration(
                    values = MealType.entries.map { it.name },
                    description = "Which meal it belongs to.",
                ),
            ),
        ),
        FunctionDeclaration(
            name = TOOL_LOG_EXERCISE,
            description = "Propose adding an activity to today's diary. This does NOT log it: the " +
                "user sees what you proposed and taps to confirm. Do not estimate the calories " +
                "burned — the app works that out from their own weight.",
            parameters = mapOf(
                "type" to Schema.enumeration(
                    values = ExerciseType.entries.map { it.name },
                    description = "The kind of activity. Use Other when none of the rest fit.",
                ),
                "minutes" to Schema.integer(
                    description = "How long it lasted, in minutes. Maximum $MAX_ACTION_MINUTES.",
                ),
                "name" to Schema.string(
                    description = "What to call it, e.g. 'Morning run'. Optional — leave it out " +
                        "and it is named after its type.",
                ),
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
        FunctionDeclaration(
            name = TOOL_LOG_WEIGHT,
            description = "Propose recording today's weigh-in. This does NOT log it: the user " +
                "sees the figure and taps to confirm. Only call this when the user has told you " +
                "what they weigh — never ask them for it. Pass the number exactly as they said " +
                "it and do not convert it: the app knows whether they weigh themselves in " +
                "kilograms or pounds.",
            parameters = mapOf(
                "weight" to Schema.double(
                    description = "The number they gave, in their own unit, e.g. 82.4.",
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
    TOOL_LOG_EXERCISE -> parseLogExercise(args)
    TOOL_LOG_SAVED_MEAL -> parseLogSavedMeal(args)
    TOOL_LOG_WEIGHT -> args.double("weight")
        ?.takeIf { it in MIN_ACTION_WEIGHT..MAX_ACTION_WEIGHT }
        ?.let { CoachAction.LogWeight(weight = round1(it)) }
    else -> null
}

/** Two fields, neither of them a figure: what [resolve] needs to find the meal, and the slot it
 * goes in. Everything else comes off the user's own saved row. */
private fun parseLogSavedMeal(args: Map<String, JsonElement>): CoachAction.LogSavedMeal? {
    val name = args.string("name")?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_NAME_CHARS }
        ?: return null
    val meal = args.string("meal")
        ?.let { raw -> MealType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } }
        ?: return null
    return CoachAction.LogSavedMeal(name = name, mealType = meal)
}

/**
 * The burn is left at zero here and priced afterwards by [CoachToolbox.weightKg] — this function
 * stays pure, and the figure on the card is the app's MET arithmetic rather than the model's
 * guess. A name is optional: empty is what [ExerciseEntry] means by "call it by its type".
 */
private fun parseLogExercise(args: Map<String, JsonElement>): CoachAction.LogExercise? {
    val type = args.string("type")
        ?.let { raw -> ExerciseType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } }
        ?: return null
    val minutes = args.int("minutes")?.takeIf { it in 1..MAX_ACTION_MINUTES } ?: return null
    val name = args.string("name")?.trim()?.takeIf { it.length <= MAX_NAME_CHARS }.orEmpty()
    return CoachAction.LogExercise(type = type, name = name, minutes = minutes, burnedKcal = 0)
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
    steps: Int? = null,
    stepGoal: Int? = null,
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
    // The four that are *omitted* rather than reported empty. Food, water and activity are things
    // the user does in this app, so a zero there is a fact worth stating; steps and sleep come off
    // a watch, fasting and the mood check-in are opt-in surfaces, and a daily "No sleep recorded"
    // would have the coach nagging about a feature that is not switched on. The system instruction
    // carries the other half of this: a category absent from a day is one the user does not track.
    //
    // Steps carry their goal because the goal is on the profile the caller already read, and
    // "8,432 of 10,000" is the difference between reporting a number and answering the question.
    steps?.let {
        val goal = stepGoal?.let { target -> " of ${formatSteps(target)}" }.orEmpty()
        appendLine("Steps: ${formatSteps(it)}$goal")
    }
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
 * And **a body reading is reported as a change, never as a figure.** `InsightRequest` sends
 * `weightDeltaKg` and has never sent an absolute figure, for the data-minimisation reason the
 * 30-day health backfill is written against — and a tool is not a loophole in that rule just
 * because the user asked the question out loud. A delta answers "is this going the right way?"
 * in full, which is the only thing anyone asks a coach about a trend; a model that knows the user
 * weighs 94.2 kg answers a different, unasked question. The first reading in a window has nothing
 * to compare against and says so, the distinction [WeightEntry] trends already draw.
 *
 * A tape measure is the same class of figure as a weigh-in and gets the same treatment, through
 * the same [deltaClauses] fold — one rule with one implementation, so a waist cannot quietly start
 * being sent whole while a weight is not.
 */
internal fun formatHistory(
    days: Int,
    nutrition: List<DayNutrition>,
    weights: List<WeightEntry>,
    today: Long,
    exercise: List<ExerciseEntry> = emptyList(),
    sleep: List<SleepNight> = emptyList(),
    steps: List<StepDay> = emptyList(),
    measurements: Map<MeasurementPart, List<MeasurementEntry>> = emptyMap(),
): String = buildString {
    val from = today - days + 1
    // Weigh-ins and tape measures in one map, because they are one rule — see the doc above.
    val changes = weightClauses(weights, from, today)
        .mergedWith(measurementClauses(measurements, from, today))
    val byDay = nutrition.filter { it.dateEpochDay in from..today }.associateBy { it.dateEpochDay }
    // A day's whole training, not one line per session: a week of two-a-days would otherwise be
    // fourteen lines of an answer that has six of them to spend.
    val training = exercise.filter { it.dateEpochDay in from..today }.groupBy { it.dateEpochDay }
    val slept = sleep.filter { it.dateEpochDay in from..today }.associateBy { it.dateEpochDay }
    val walked = steps.filter { it.dateEpochDay in from..today }.associateBy { it.dateEpochDay }
    if (byDay.values.none { it.isLogged } && changes.isEmpty() && training.isEmpty() &&
        slept.isEmpty() && walked.isEmpty()
    ) {
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
        val walk = walked[date]?.let { ", ${formatSteps(it.steps)} steps" }.orEmpty()
        val night = slept[date]?.let { ", slept ${formatDuration(it.minutesAsleep)}" }.orEmpty()
        val change = changes[date].orEmpty().joinToString("")
        appendLine("- $label: $food$activity$walk$night$change")
    }
}

/**
 * Each reading in the window against the one before it — including a reading from *before* the
 * window, which is what makes the oldest day in a span carry a change rather than a shrug.
 *
 * Generic over the series because a weigh-in and a tape measure differ only in the words: both
 * report a change and neither ever reports the reading. [clause] is handed the delta, or null for
 * the first reading of its kind, and returns the whole clause that goes on the day's line.
 *
 * A **list** per day, because a day can carry several: a waist and a body fat measured in the same
 * sitting are two clauses, not one overwriting the other.
 */
private fun <T> deltaClauses(
    readings: List<T>,
    from: Long,
    to: Long,
    day: (T) -> Long,
    value: (T) -> Double,
    clause: (delta: Double?) -> String,
): Map<Long, List<String>> {
    val sorted = readings.sortedBy(day)
    return buildMap {
        sorted.forEachIndexed { index, entry ->
            val date = day(entry)
            if (date !in from..to) return@forEachIndexed
            val prior = sorted.getOrNull(index - 1)
            put(date, getOrElse(date) { emptyList() } + clause(prior?.let { value(entry) - value(it) }))
        }
    }
}

private fun weightClauses(weights: List<WeightEntry>, from: Long, to: Long): Map<Long, List<String>> =
    deltaClauses(weights, from, to, WeightEntry::dateEpochDay, WeightEntry::weightKg) { delta ->
        if (delta == null) {
            ", weighed in (first one, nothing to compare against)"
        } else {
            ", weighed in (%+.1f kg since the last)".format(delta)
        }
    }

/**
 * The same fold, once per part: each part is its own series, so a waist is compared against the
 * last waist and never against a thigh.
 *
 * **Stored units, not the user's** — cm and %, matching the kg a weigh-in already reports. The
 * whole file is pure over `:core:data` types with no profile to read a preference off, and a coach
 * that quoted inches while the weight came back in kilograms would be worse than one that is
 * consistently metric. Converting both is its own pass.
 */
private fun measurementClauses(
    measurements: Map<MeasurementPart, List<MeasurementEntry>>,
    from: Long,
    to: Long,
): Map<Long, List<String>> = measurements.entries.fold(emptyMap()) { acc, (part, entries) ->
    val unit = part.unitLabel(UnitSystem.Metric)
    acc.mergedWith(
        deltaClauses(entries, from, to, MeasurementEntry::dateEpochDay, MeasurementEntry::value) { delta ->
            if (delta == null) {
                ", measured ${part.promptName()} (first one, nothing to compare against)"
            } else {
                // The number is formatted on its own and interpolated: `unit` is "%" for a body
                // fat, and a "%" inside the format string is a conversion specifier, not a sign.
                ", measured ${part.promptName()} (${"%+.1f".format(delta)} $unit since the last)"
            }
        },
    )
}

/** Prompt text, so it stays in Kotlin like every other word the model reads here. The enum's own
 * `label` is the user-facing name and needs a `Context` this file never has. */
private fun MeasurementPart.promptName(): String =
    if (this == MeasurementPart.BodyFat) "body fat" else name.lowercase()

/** One merge rule for the clause maps, so two series landing on the same day cannot lose one. */
private fun Map<Long, List<String>>.mergedWith(
    other: Map<Long, List<String>>,
): Map<Long, List<String>> = buildMap {
    putAll(this@mergedWith)
    other.forEach { (date, clauses) -> put(date, getOrElse(date) { emptyList() } + clauses) }
}

// endregion

/**
 * The user's own meals and recipes, by name.
 *
 * Names first and figures second, because the names are what the model has to quote back exactly —
 * `log_saved_meal` matches on them and nothing else. A recipe reports **per serving**, the figure
 * the diary would get, rather than the whole pot.
 */
internal fun formatLibrary(meals: List<SavedMeal>, recipes: List<Recipe>): String = buildString {
    if (meals.isEmpty() && recipes.isEmpty()) return "They have not saved any meals or recipes."
    if (meals.isNotEmpty()) {
        appendLine("Saved meals:")
        meals.forEach {
            appendLine("- \"${it.name}\": ${it.items.size} items, ${it.totalKcal()} kcal")
        }
    }
    if (recipes.isNotEmpty()) {
        appendLine("Recipes:")
        recipes.forEach {
            appendLine("- \"${it.name}\": ${it.perServing().calories} kcal per serving")
        }
    }
}

/**
 * A saved meal or recipe, by name, as the rows that would be written — or null when nothing
 * matches, which fails the turn.
 *
 * **Exact match, case- and space-insensitive; never fuzzy.** `get_library` hands the model the
 * names verbatim, so a name that matches nothing is a broken call rather than a near miss, and
 * guessing which meal was meant is the one thing a card one tap from the diary must not do — the
 * user would be confirming a meal they did not name. Meals before recipes, the order
 * [formatLibrary] lists them in.
 *
 * Every figure on the rows is the user's own. Nothing here is estimated, which is the whole point:
 * a model that could retype a saved meal's macros would be inventing figures the app already has.
 */
internal fun savedMealRows(
    name: String,
    mealType: MealType,
    meals: List<SavedMeal>,
    recipes: List<Recipe>,
): List<CoachAction.LogFood>? {
    val wanted = name.trim()
    meals.firstOrNull { it.name.equals(wanted, ignoreCase = true) }
        ?.let { meal -> return meal.items.map { it.toLogFood(it.name, mealType) } }
    return recipes.firstOrNull { it.name.equals(wanted, ignoreCase = true) }
        // One row at one serving, named after the recipe — how the app logs a recipe everywhere
        // else, and the reason `perServing()` exists.
        ?.let { recipe -> listOf(recipe.perServing().toLogFood(recipe.name, mealType)) }
}

private fun SavedMealItem.toLogFood(name: String, mealType: MealType) = CoachAction.LogFood(
    name = name,
    mealType = mealType,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
)

/** A serving is one row of one portion — the recipe's own name, its own figures, nothing
 * estimated. */
private fun RecipeServing.toLogFood(name: String, mealType: MealType) = CoachAction.LogFood(
    name = name,
    mealType = mealType,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    portionAmount = 1.0,
    portionUnit = SERVING_UNIT,
)

/** Stays in Kotlin: it is persisted on the row, the rule every portion unit in this app follows. */
private const val SERVING_UNIT = "serving"

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
    // The four the coach could not see at all: a training week, a bad night, an opt-in check-in,
    // and the walking that never reached a workout. They widen what the two existing tools
    // *answer with* rather than adding tools of their own — a question is still about one day or
    // one span, and four more function declarations would be four more things to pick wrong.
    // Measurements need no repository of their own: `progressRepository` above already has them.
    private val sleepRepository: SleepRepository,
    private val moodRepository: MoodRepository,
    private val fastingRepository: FastingRepository,
    private val stepsRepository: StepsRepository,
) {
    /** Null for a tool this does not run — which is every write tool, and is how the caller's loop
     * tells a question from an instruction without a second lookup. */
    suspend fun runTool(name: String, args: Map<String, JsonElement>): String? = when (name) {
        TOOL_GET_DAY -> getDay(daysAgoOf(args))
        TOOL_GET_HISTORY -> getHistory(historyDaysOf(args))
        TOOL_GET_LIBRARY -> getLibrary()
        else -> null
    }

    /** The whole library, not the newest five the add-entry panel shows: the model is answering
     * "what have I saved?", and a truncated list would have it deny a meal the user can see. */
    private suspend fun getLibrary(): String = formatLibrary(
        meals = foodRepository.observeAllSavedMeals().first(),
        recipes = foodRepository.observeAllRecipes().first(),
    )

    /** The two reads [savedMealRows] needs, and nothing else — the matching itself is pure, so it
     * is the part a JVM test can reach. */
    suspend fun savedMealRows(name: String, mealType: MealType): List<CoachAction.LogFood>? =
        savedMealRows(
            name = name,
            mealType = mealType,
            meals = foodRepository.observeAllSavedMeals().first(),
            recipes = foodRepository.observeAllRecipes().first(),
        )

    /**
     * What a MET estimate is priced against: the latest weigh-in, else the onboarding weight —
     * `:feature:training`'s own rule, so a coach-drafted workout and a hand-logged one of the same
     * length come out at the same number.
     *
     * Null when the user has neither, which fails the draft rather than inventing a body: a
     * default weight is a made-up figure on a card whose whole promise is that every figure shown
     * is the figure that gets written.
     */
    suspend fun weightKg(): Double? =
        progressRepository.observeWeightEntries().first().maxByOrNull { it.dateEpochDay }?.weightKg
            ?: profileRepository.observeProfile().first()?.weightKg

    /** The unit a drafted weigh-in is read in — the profile's, never the model's. Metric with no
     * profile, which is `LogWeightUiState`'s own fallback. */
    suspend fun unitSystem(): UnitSystem =
        profileRepository.observeProfile().first()?.preferredUnit ?: UnitSystem.Metric

    /** Deliberately *not* [weightKg]: that one falls back to the onboarding weight, and a card
     * saying "since your last weigh-in" must mean a weigh-in. Null until there is one. */
    suspend fun latestWeighInKg(): Double? =
        progressRepository.observeWeightEntries().first().maxByOrNull { it.dateEpochDay }?.weightKg

    private suspend fun getDay(daysAgo: Int): String {
        val today = todayEpochDay()
        val date = today - daysAgo
        val label = when (daysAgo) {
            0 -> "Today"
            1 -> "Yesterday"
            else -> "$daysAgo days ago"
        }
        // Read once: the calorie target and the step goal come off the same row, and two reads
        // could disagree if the user edits a target while the turn is in flight.
        val profile = profileRepository.observeProfile().first()
        return formatDay(
            label = label,
            foods = foodRepository.observeEntries(date).first(),
            targetCalories = profile?.dailyTargets()?.calories,
            waterGlasses = waterRepository.observeDay(date).first(),
            exercise = exerciseRepository.observeEntries(date).first(),
            // Null when nothing was imported for that day, which is what leaves the line out
            // entirely — the rule sleep, mood and fasting already follow.
            steps = stepsRepository.observeSteps(date).first()?.steps,
            stepGoal = profile?.stepGoal,
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
        steps = stepsRepository.observeDays().first(),
        measurements = progressRepository.observeMeasurements().first(),
    )
}

/**
 * Fills in what the model is not allowed to supply, and is the second half of the trust boundary
 * [parseAction] opens.
 *
 * Everything else on a proposal card is the model's own words checked against a ceiling. These two
 * are not. A calorie burn is arithmetic the app already owns ([estimateBurnedKcal]); a saved meal's
 * nutrition is a figure the *user* already saved. Asking a model for either buys an invented number
 * on the one surface that promises every figure shown is the figure written.
 *
 * It returns a **list** because one action can resolve to several: a saved meal is one row per
 * item, which is what the multi-row card exists for. Null fails the turn exactly as a rejected
 * parse does — a weigh-in the app doesn't have, or a name that is in no library.
 */
internal suspend fun CoachAction.resolve(toolbox: CoachToolbox): List<CoachAction>? = when (this) {
    is CoachAction.LogExercise -> toolbox.weightKg()
        ?.let { listOf(copy(burnedKcal = estimateBurnedKcal(type, minutes, it))) }
    is CoachAction.LogSavedMeal -> toolbox.savedMealRows(name, mealType)
    // Never null: a weigh-in needs no history to be recorded, so a first one still drafts — it
    // simply draws no change line.
    is CoachAction.LogWeight -> listOf(
        copy(unit = toolbox.unitSystem(), previousKg = toolbox.latestWeighInKg()),
    )
    else -> listOf(this)
}

/**
 * The food rows of a settled draft, as the diary stores them — one [FoodEntry] each, in the order
 * they were drafted, for a single batched write.
 *
 * Pure, and here rather than inline in `settle`, for the reason [parseAction] is: it is the part a
 * JVM test can reach, and "what gets written" is exactly the part worth pinning.
 */
internal fun List<CoachAction>.foodEntries(): List<FoodEntry> =
    filterIsInstance<CoachAction.LogFood>().map {
        FoodEntry(
            name = it.name,
            mealType = it.mealType,
            portionAmount = it.portionAmount,
            portionUnit = it.portionUnit,
            calories = it.calories,
            proteinG = it.proteinG,
            carbsG = it.carbsG,
            fatG = it.fatG,
        )
    }

/**
 * Glasses to **add** to the day, summed across the draft.
 *
 * Summed rather than applied one at a time, and that is the load-bearing half: `setToday` takes the
 * day's *new total*, so two water rows written in sequence would have the second overwrite the
 * first and a draft of two glasses would land as one.
 */
internal fun List<CoachAction>.glassesToAdd(): Int =
    filterIsInstance<CoachAction.LogWater>().sumOf { it.glasses }

/** The envelope the SDK requires around a [String] result. One key, because the result is prose
 * and prose has no fields. */
internal fun toolResponse(result: String): JsonObject =
    JsonObject(mapOf("result" to JsonPrimitive(result)))
