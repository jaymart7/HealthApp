package ph.mart.healthapp.core.data.coach

import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureCategory
import ph.mart.healthapp.core.data.bloodpressure.DIASTOLIC_RANGE
import ph.mart.healthapp.core.data.bloodpressure.PULSE_RANGE
import ph.mart.healthapp.core.data.bloodpressure.SYSTOLIC_RANGE
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureReading
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureRepository
import ph.mart.healthapp.core.data.bloodpressure.byDay
import ph.mart.healthapp.core.data.bloodpressure.categoryOf
import ph.mart.healthapp.core.data.bloodpressure.formatBloodPressure
import ph.mart.healthapp.core.data.cycle.CycleDay
import ph.mart.healthapp.core.data.cycle.CycleRepository
import ph.mart.healthapp.core.data.cycle.CycleSymptom
import ph.mart.healthapp.core.data.cycle.FlowLevel
import ph.mart.healthapp.core.data.cycle.cycleDayNumber
import ph.mart.healthapp.core.data.cycle.flowLevelOf
import ph.mart.healthapp.core.data.cycle.periods
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.Routine
import ph.mart.healthapp.core.data.exercise.RoutineRepository
import ph.mart.healthapp.core.data.exercise.dayLabel
import ph.mart.healthapp.core.data.exercise.isPlannedOn
import ph.mart.healthapp.core.data.exercise.estimateBurnedKcal
import ph.mart.healthapp.core.data.exercise.totalBurnedKcal
import ph.mart.healthapp.core.data.fasting.DEFAULT_FAST_GOAL_HOURS
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.fasting.dateEpochDay
import ph.mart.healthapp.core.data.fasting.durationMinutes
import ph.mart.healthapp.core.data.fasting.isActive
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.FoodSuggestion
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.RecipeServing
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.dailyTotals
import ph.mart.healthapp.core.data.food.dietLine
import ph.mart.healthapp.core.data.food.perServing
import ph.mart.healthapp.core.data.food.totalKcal
import ph.mart.healthapp.core.data.health.HeartDay
import ph.mart.healthapp.core.data.health.HeartRepository
import ph.mart.healthapp.core.data.health.SleepNight
import ph.mart.healthapp.core.data.health.SleepRepository
import ph.mart.healthapp.core.data.health.StepDay
import ph.mart.healthapp.core.data.health.StepsRepository
import ph.mart.healthapp.core.data.health.formatBpm
import ph.mart.healthapp.core.data.health.formatDuration
import ph.mart.healthapp.core.data.health.formatSteps
import ph.mart.healthapp.core.data.note.NOTE_MAX_CHARS
import ph.mart.healthapp.core.data.note.NoteRepository
import ph.mart.healthapp.core.data.mood.MOOD_SCALE
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.mood.MoodRepository
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.cmToDisplayUnit
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.lengthUnitLabel
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.data.profile.round1
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.progress.fromDisplay
import ph.mart.healthapp.core.data.progress.range
import ph.mart.healthapp.core.data.progress.toDisplay
import ph.mart.healthapp.core.data.recap.REPORT_DAYS
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.progress.unitLabel
import ph.mart.healthapp.core.data.supplement.SUPPLEMENT_TIMES_PER_DAY
import ph.mart.healthapp.core.data.supplement.SupplementDay
import ph.mart.healthapp.core.data.supplement.SupplementRepository
import ph.mart.healthapp.core.data.supplement.SupplementToday
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.data.water.WaterDay
import ph.mart.healthapp.core.data.water.WaterRepository

/**
 * The coach's tools: three the app *runs*, eleven it only ever *drafts*, and one that draws.
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
 * [TOOL_SHOW_REPORT] is the third kind and the newest. It is not a read — nothing goes back into
 * the answer — and not a draft, because there is nothing to agree to: it puts a **report card** in
 * the transcript, folded by `recap()` from the same Room rows the Progress tab folds. The model
 * picks the window and writes one sentence over it; every figure on the card is the app's. That is
 * `log_exercise`'s rule (the app supplies what a model would otherwise invent) taken to its end,
 * and it is why the model is not handed the report's contents: it cannot misquote a figure it was
 * never given.
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

/** A supplement's own ceiling, not a second opinion on one: [SUPPLEMENT_TIMES_PER_DAY] is what the
 * edit sheet allows, and `setTakenToday` clamps to the row's own figure anyway. */
internal val MAX_ACTION_DOSES = SUPPLEMENT_TIMES_PER_DAY.last

/**
 * How far back a draft may reach.
 *
 * A month, [MAX_HISTORY_DAYS]' figure, because that is the span the coach can *read* and nobody
 * remembers an unlogged breakfast further back than the diary they are looking at.
 *
 * **Out of band fails the draft rather than clamping**, which is the opposite of [daysAgoOf]'s
 * silent clamp on a read, and deliberately: a model asking to read day 900 means "recently" and
 * failing that turn helps nobody, while a model asking to *write* into day 900 has misread the
 * sentence, and a row quietly landing on a day the user never named is a row they will find
 * months later without knowing how.
 */
internal const val MAX_DRAFT_DAYS_AGO = MAX_HISTORY_DAYS

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
internal const val TOOL_LOG_SUPPLEMENT = "log_supplement"
internal const val TOOL_LOG_MOOD = "log_mood"
internal const val TOOL_LOG_BLOOD_PRESSURE = "log_blood_pressure"
internal const val TOOL_LOG_MEASUREMENT = "log_measurement"
internal const val TOOL_LOG_NOTE = "log_note"
internal const val TOOL_LOG_FAST = "log_fast"

/** The only two values `log_fast` takes. Stays in Kotlin like every other word the model
 * reads: it is schema text, and it is compared against rather than shown. */
internal const val FAST_START = "start"
internal const val FAST_END = "end"
internal const val TOOL_START_ROUTINE = "start_routine"

internal const val TOOL_OPEN_SCREEN = "open_screen"

/** The tool that neither reads nor drafts: it draws. See this file's header. */
internal const val TOOL_SHOW_REPORT = "show_report"

/** The ones the model may call but the app never executes. Kept as a set rather than a `when` so
 * [CoachRepositoryImpl]'s loop can ask the question without knowing what any of them does. */
internal val WRITE_TOOLS = setOf(
    TOOL_LOG_FOOD,
    TOOL_LOG_WATER,
    TOOL_LOG_EXERCISE,
    TOOL_LOG_SAVED_MEAL,
    TOOL_LOG_WEIGHT,
    TOOL_LOG_SUPPLEMENT,
    TOOL_LOG_MOOD,
    TOOL_LOG_BLOOD_PRESSURE,
    TOOL_LOG_MEASUREMENT,
    TOOL_LOG_NOTE,
    // The one that writes no row at all: its confirm flips the fasting timer, which is a state
    // and not a row. See [CoachAction.SetFast].
    TOOL_LOG_FAST,
    // The one that executes nothing *and* writes nothing: it ends the turn as a draft like the
    // rest, and the tap on that draft opens a form. See [CoachAction.StartRoutine].
    TOOL_START_ROUTINE,
    // The routine's kind with no form behind it: the tap opens a screen. [CoachAction.OpenScreen].
    TOOL_OPEN_SCREEN,
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

/**
 * The same offset [daysAgoSchema] describes, on the tools that write one.
 *
 * Separate because the bound is different and the model is told so: a read reaches a year back, a
 * draft a month, and a draft out of band fails rather than clamping.
 */
private val draftDaysAgoSchema = Schema.integer(
    description = "How many days back to log it. Leave it out or use 0 for today, 1 for " +
        "yesterday. Maximum $MAX_DRAFT_DAYS_AGO.",
)

internal val COACH_TOOLS: Tool = Tool.functionDeclarations(
    listOf(
        FunctionDeclaration(
            name = TOOL_GET_DAY,
            description = "Read one day of the user's diary in full: every food they logged with " +
                "its calories and macros, the day's totals against their targets, water, any " +
                "activity, their steps, and their sleep, mood, fasting, supplements, heart rate " +
                "and blood-pressure readings where they track those, plus anything they wrote " +
                "about the day themselves. Call this before answering " +
                "anything about a specific day.",
            parameters = mapOf("days_ago" to daysAgoSchema),
        ),
        FunctionDeclaration(
            name = TOOL_GET_HISTORY,
            description = "Read a span of recent days: calories and protein per day, their " +
                "water, any training, any steps, any sleep, any supplements, any heart rate, " +
                "any blood-pressure reading, any weigh-in and " +
                "any change in their body measurements. Call this for trends, averages, or " +
                "anything about a week or a month.",
            parameters = mapOf(
                "days" to Schema.integer(
                    description = "How many days back from today, up to $MAX_HISTORY_DAYS.",
                ),
            ),
        ),
        FunctionDeclaration(
            name = TOOL_GET_LIBRARY,
            description = "Read the meals and recipes this user has saved, the foods they log " +
                "most often, and the supplements they take, all by name and with the figures " +
                "they are logged at. Call this before answering anything about what they " +
                "usually eat, before suggesting what they could eat, and before logging a meal " +
                "or a supplement they refer to by name.",
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
                "days_ago" to draftDaysAgoSchema,
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
                "days_ago" to draftDaysAgoSchema,
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
                "days_ago" to draftDaysAgoSchema,
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
                "days_ago" to draftDaysAgoSchema,
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
        FunctionDeclaration(
            name = TOOL_LOG_SUPPLEMENT,
            description = "Propose ticking off one of the user's own supplements for today, by " +
                "its exact name from get_library. This does NOT log it: the user sees what you " +
                "proposed and taps to confirm. Only ever one they already take — you cannot add " +
                "a new supplement and must never suggest one.",
            parameters = mapOf(
                "name" to Schema.string(
                    description = "The supplement's name, exactly as get_library gave it.",
                ),
                "doses" to Schema.integer(
                    description = "How many doses to ADD to today's count, not the new total. " +
                        "Usually 1. Maximum $MAX_ACTION_DOSES.",
                ),
            ),
        ),
        FunctionDeclaration(
            name = TOOL_LOG_MOOD,
            description = "Propose recording how the user felt today. This does NOT log it: they " +
                "see what you proposed and tap to confirm. Only call it when they have told you " +
                "how they felt or how much energy they had — never ask. Pass whichever of the " +
                "two they mentioned and leave the other out.",
            parameters = mapOf(
                "mood" to Schema.integer(
                    description = "How they felt, from ${MOOD_SCALE.first} (very low) to " +
                        "${MOOD_SCALE.last} (great). Leave it out if they only mentioned energy.",
                ),
                "energy" to Schema.integer(
                    description = "How much energy they had, from ${MOOD_SCALE.first} (very low) " +
                        "to ${MOOD_SCALE.last} (great). Leave it out if they only mentioned mood.",
                ),
            ),
        ),
        FunctionDeclaration(
            name = TOOL_LOG_BLOOD_PRESSURE,
            description = "Propose recording a blood-pressure reading the user has told you. " +
                "This does NOT log it: they see the figures and tap to confirm. Pass the numbers " +
                "exactly as they gave them — the app works out which band the reading is in, so " +
                "do not categorise it, and do not say what it means.",
            parameters = mapOf(
                "systolic" to Schema.integer(
                    description = "The upper number, e.g. 118. Always the higher of the two.",
                ),
                "diastolic" to Schema.integer(
                    description = "The lower number, e.g. 76.",
                ),
                "pulse_bpm" to Schema.integer(
                    description = "The pulse the cuff showed, if they said it. Optional.",
                ),
            ),
        ),
        FunctionDeclaration(
            name = TOOL_LOG_MEASUREMENT,
            description = "Propose recording one body measurement the user has told you. This " +
                "does NOT log it: they see the figure and tap to confirm. Only call this when " +
                "they have given you the number — never ask them for one. Pass it exactly as " +
                "they said it and do not convert it: the app knows whether they measure in " +
                "centimetres or inches. One call per site.",
            parameters = mapOf(
                "part" to Schema.enumeration(
                    values = MeasurementPart.entries.map { it.name },
                    description = "Which site they measured. BodyFat is a percentage.",
                ),
                "value" to Schema.double(
                    description = "The number they gave, in their own unit, e.g. 82.5.",
                ),
            ),
        ),
        FunctionDeclaration(
            name = TOOL_LOG_NOTE,
            description = "Propose writing the user's own note on a day — what they said about " +
                "it, in their words. This does NOT save it: they see the sentence and tap to " +
                "confirm. Only when they ask you to note, jot or remember something about a " +
                "day — never write one they did not ask for, and never turn your own summary of " +
                "their day into one. Keep their wording and keep it under $NOTE_MAX_CHARS " +
                "characters. A day holds one note, so this replaces anything already written " +
                "on that day.",
            parameters = mapOf(
                "text" to Schema.string(
                    description = "What to write on the day, in the user's own words.",
                ),
                "days_ago" to draftDaysAgoSchema,
            ),
        ),
        FunctionDeclaration(
            name = TOOL_LOG_FAST,
            description = "Propose starting the user's fasting timer, or ending the fast they " +
                "have running. This does NOT do it: they see what you proposed and tap to " +
                "confirm. Only call it when they say they are starting or breaking a fast — " +
                "never suggest one, and never propose a fast for an earlier time or an earlier " +
                "day. Do not set how long the fast should be: the app uses their own goal.",
            parameters = mapOf(
                "action" to Schema.enumeration(
                    values = listOf(FAST_START, FAST_END),
                    description = "start to begin a fast now, end to finish the one running.",
                ),
            ),
        ),
        FunctionDeclaration(
            name = TOOL_START_ROUTINE,
            description = "Propose starting one of the user's own saved workout routines, by its " +
                "exact name from get_library. This does NOT log a workout and records nothing: " +
                "it opens their workout screen already filled in with that routine's lifts, and " +
                "they save it themselves. Only ever one of their own routines — you cannot " +
                "invent a workout, add a lift to one, or set how much they lift.",
            parameters = mapOf(
                "name" to Schema.string(
                    description = "The routine's name, exactly as get_library gave it.",
                ),
            ),
        ),
        FunctionDeclaration(
            name = TOOL_OPEN_SCREEN,
            description = "Propose opening a screen of the app when the user asks to see, open or " +
                "go to one. It puts a button on screen that opens it; it changes nothing. Diary " +
                "is the food diary, SupplementList is the list of supplements they take, " +
                "Supplements is the chart of how they kept up, Badges is their achievements. " +
                "Never call it in the same turn as anything else.",
            parameters = mapOf(
                "screen" to Schema.enumeration(
                    values = CoachScreen.entries.map { it.name },
                    description = "Which screen to open.",
                ),
            ),
        ),
        FunctionDeclaration(
            name = TOOL_SHOW_REPORT,
            description = "Put an interactive report card on screen summarising the user's last " +
                "7 or 30 days — their calories and macros, their weight, their training and " +
                "their steps, with charts they can open. Call this when they ask for a report, " +
                "a summary, an overview, or how their week or month went. The card carries every " +
                "figure itself, so you are not given them: introduce it in one short sentence " +
                "and state no numbers. For a specific question about a span, use get_history " +
                "instead, and never call both in one turn.",
            parameters = mapOf(
                "days" to Schema.integer(
                    description = "The window to report on: ${REPORT_DAYS.joinToString(" or ")}.",
                ),
            ),
        ),
    ),
)

/**
 * The window a `show_report` call asks for, or null for anything else.
 *
 * [parseAction]'s rule on a tool that makes no [CoachAction]: null fails the whole turn rather
 * than falling back to a window nobody asked for. A card headed "Last 7 days" that the user asked
 * a month of is worse than an apology, because nothing on it says so.
 *
 * Deliberately not clamped to the nearest legal window — see [REPORT_DAYS]. A model that answers
 * 14 has misread the schema, and rounding that to 7 or 30 invents an intent.
 */
internal fun parseShowReport(args: Map<String, JsonElement>): Int? =
    args.int("days")?.takeIf { it in REPORT_DAYS }

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
internal fun parseAction(
    name: String,
    args: Map<String, JsonElement>,
    today: Long,
): CoachAction? = when (name) {
    TOOL_LOG_FOOD -> parseLogFood(args, today)
    TOOL_LOG_WATER -> args.int("glasses")
        ?.takeIf { it in 1..MAX_ACTION_GLASSES }
        ?.let { glasses ->
            draftDay(args, today)?.let { CoachAction.LogWater(glasses = glasses, dateEpochDay = it) }
        }
    TOOL_LOG_EXERCISE -> parseLogExercise(args, today)
    TOOL_LOG_SAVED_MEAL -> parseLogSavedMeal(args, today)
    TOOL_LOG_WEIGHT -> args.double("weight")
        ?.takeIf { it in MIN_ACTION_WEIGHT..MAX_ACTION_WEIGHT }
        ?.let { CoachAction.LogWeight(weight = round1(it)) }
    TOOL_LOG_SUPPLEMENT -> parseLogSupplement(args)
    TOOL_LOG_MOOD -> parseLogMood(args)
    TOOL_LOG_BLOOD_PRESSURE -> parseLogBloodPressure(args)
    TOOL_LOG_MEASUREMENT -> parseLogMeasurement(args)
    TOOL_LOG_NOTE -> parseLogNote(args, today)
    // A verb and nothing else. Which of the two is legal depends on whether a fast is running,
    // and that is [resolve]'s question — this stays pure, with no clock and no Room.
    TOOL_LOG_FAST -> when (args.string("action")?.trim()?.lowercase()) {
        FAST_START -> CoachAction.SetFast(ending = false)
        FAST_END -> CoachAction.SetFast(ending = true)
        else -> null
    }
    // A name and nothing else: [resolve] finds the user's own routine from it, and everything the
    // card shows and the form opens with comes off that row.
    TOOL_START_ROUTINE -> args.string("name")
        ?.trim()
        ?.takeIf { it.isNotEmpty() && it.length <= MAX_NAME_CHARS }
        ?.let { CoachAction.StartRoutine(name = it) }
    TOOL_OPEN_SCREEN -> args.string("screen")?.trim()
        ?.let { screen -> CoachScreen.entries.firstOrNull { it.name.equals(screen, ignoreCase = true) } }
        ?.let(CoachAction::OpenScreen)
    else -> null
}

/**
 * Either column, or both — and **never neither**, which is a card whose Confirm writes nothing.
 *
 * A missing column is `0` rather than a failed draft: "I felt great" names no energy, and leaving
 * that column alone is what [ph.mart.healthapp.core.data.mood.MoodDay]'s own zero means. A figure
 * outside [MOOD_SCALE] does fail — a model answering `7` on a five-point scale has not read the
 * schema, and clamping it would put a number on the card that the tap does not write.
 */
private fun parseLogMood(args: Map<String, JsonElement>): CoachAction.LogMood? {
    val mood = args.optionalInt("mood", MOOD_SCALE) ?: return null
    val energy = args.optionalInt("energy", MOOD_SCALE) ?: return null
    if (mood == 0 && energy == 0) return null
    return CoachAction.LogMood(mood = mood, energy = energy)
}

/**
 * A figure the model may leave out entirely: absent is `0`, the "not set" both
 * [ph.mart.healthapp.core.data.mood.MoodDay]'s columns and [BloodPressureReading.pulseBpm] already
 * mean by it.
 *
 * Present but outside [range] is a **failure, not a clamp** — a model answering 7 on a five-point
 * scale has not read the schema, and clamping it would draw a figure on the card that the tap does
 * not write.
 */
private fun Map<String, JsonElement>.optionalInt(key: String, range: IntRange): Int? {
    if (key !in this) return 0
    return int(key)?.takeIf { it in range }
}

/**
 * Two numbers and an optional third, bounded by the cuff's own ranges rather than by
 * [MAX_ACTION_CALORIES]-style constants invented here: [SYSTOLIC_RANGE], [DIASTOLIC_RANGE] and
 * [PULSE_RANGE] are what the manual sheet's steppers already allow, so a coach-drafted reading and
 * a hand-typed one admit exactly the same figures.
 *
 * **Failing rather than clamping**, which is the opposite of `addReading`'s own behaviour and
 * deliberately: the repository clamps because a sheet has already shown the user their own number,
 * while this card's whole promise is that the figure on it is the figure that gets written.
 *
 * **The systolic has to be the higher of the two.** It is the one mistake a model actually makes
 * here — "76 over 118" read back in the order it was said — and a swapped reading is wrong twice
 * over: in the chart, and in the band [categoryOf] puts it in, which is worst-first and would call
 * it Elevated.
 */
private fun parseLogBloodPressure(args: Map<String, JsonElement>): CoachAction.LogBloodPressure? {
    val systolic = args.int("systolic")?.takeIf { it in SYSTOLIC_RANGE } ?: return null
    val diastolic = args.int("diastolic")?.takeIf { it in DIASTOLIC_RANGE } ?: return null
    if (systolic <= diastolic) return null
    val pulse = args.optionalInt("pulse_bpm", PULSE_RANGE) ?: return null
    return CoachAction.LogBloodPressure(systolic = systolic, diastolic = diastolic, pulseBpm = pulse)
}

/**
 * A site and a figure, and the figure is checked twice.
 *
 * Here it only has to be a positive number under [MAX_ACTION_WEIGHT] — the band that constant's
 * own comment explains, for the same reason: the number arrives before the unit does, and 32 is a
 * plausible arm in centimetres and in inches both. The real bound is [MeasurementPart.range], which
 * is in *stored* units and so cannot be applied until [resolve] has stamped the profile's.
 */
private fun parseLogMeasurement(args: Map<String, JsonElement>): CoachAction.LogMeasurement? {
    val part = args.string("part")
        ?.let { raw -> MeasurementPart.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } }
        ?: return null
    val value = args.double("value")?.takeIf { it > 0 && it <= MAX_ACTION_WEIGHT } ?: return null
    return CoachAction.LogMeasurement(part = part, value = round1(value))
}

/**
 * The user's own sentence and the day it is about — the one parse here with no figure in it at all.
 *
 * **Over [NOTE_MAX_CHARS] fails rather than being cut.** `NoteRepositoryImpl` caps on the way into
 * the table, so a longer draft would put a sentence on the card that the tap does not write — and
 * half a sentence reads as a bug, [MAX_REPLY_CHARS]' own argument. The trim is the same one
 * `toNoteText` makes, applied early so the length checked here is the length that lands.
 *
 * Blank fails too: an empty note is how a note is *deleted*, and a Confirm button that quietly
 * removes what the user wrote is not what "note this" asked for.
 */
private fun parseLogNote(args: Map<String, JsonElement>, today: Long): CoachAction.LogNote? {
    val text = args.string("text")?.trim()?.takeIf { it.isNotEmpty() && it.length <= NOTE_MAX_CHARS }
        ?: return null
    val date = draftDay(args, today) ?: return null
    return CoachAction.LogNote(text = text, dateEpochDay = date)
}

/**
 * A name and a count, neither of them a figure the app will do arithmetic on — [resolve] finds the
 * user's own row from the name, and the count is doses to *add*, the reading [CoachAction.LogWater]
 * already gives one.
 *
 * A missing count is one dose rather than a failed draft: "I took my creatine" names no number, and
 * one is what that sentence means. Zero is not — a draft that writes nothing is a draft with a
 * Confirm button that does nothing.
 */
private fun parseLogSupplement(args: Map<String, JsonElement>): CoachAction.LogSupplement? {
    val name = args.string("name")?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_NAME_CHARS }
        ?: return null
    val doses = (args.int("doses") ?: 1).takeIf { it in 1..MAX_ACTION_DOSES } ?: return null
    return CoachAction.LogSupplement(name = name, doses = doses)
}

/** Two fields, neither of them a figure: what [resolve] needs to find the meal, and the slot it
 * goes in. Everything else comes off the user's own saved row. */
private fun parseLogSavedMeal(args: Map<String, JsonElement>, today: Long): CoachAction.LogSavedMeal? {
    val name = args.string("name")?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_NAME_CHARS }
        ?: return null
    val meal = args.string("meal")
        ?.let { raw -> MealType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } }
        ?: return null
    val date = draftDay(args, today) ?: return null
    return CoachAction.LogSavedMeal(name = name, mealType = meal, dateEpochDay = date)
}

/**
 * The absolute day a drafted row lands on, resolved here and nowhere else — or null when the model
 * asked for one outside [MAX_DRAFT_DAYS_AGO], which fails the draft.
 *
 * [today] is passed in rather than read, so the parse stays the pure function [CoachToolsTest] can
 * assert against. Resolving it at the *parse* is what makes the card's promise hold across
 * midnight: the day the card drew is the day the tap writes to, not whatever day it happens to be
 * when the user gets round to tapping.
 *
 * Zero for today, which is [CoachAction.draftedOn]'s reading of it and the diary's own.
 */
private fun draftDay(args: Map<String, JsonElement>, today: Long): Long? {
    val daysAgo = args.int("days_ago") ?: 0
    if (daysAgo !in 0..MAX_DRAFT_DAYS_AGO) return null
    return if (daysAgo == 0) 0 else today - daysAgo
}

/**
 * The burn is left at zero here and priced afterwards by [CoachToolbox.weightKg] — this function
 * stays pure, and the figure on the card is the app's MET arithmetic rather than the model's
 * guess. A name is optional: empty is what [ExerciseEntry] means by "call it by its type".
 */
private fun parseLogExercise(args: Map<String, JsonElement>, today: Long): CoachAction.LogExercise? {
    val type = args.string("type")
        ?.let { raw -> ExerciseType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } }
        ?: return null
    val minutes = args.int("minutes")?.takeIf { it in 1..MAX_ACTION_MINUTES } ?: return null
    val name = args.string("name")?.trim()?.takeIf { it.length <= MAX_NAME_CHARS }.orEmpty()
    val date = draftDay(args, today) ?: return null
    return CoachAction.LogExercise(
        type = type,
        name = name,
        minutes = minutes,
        burnedKcal = 0,
        dateEpochDay = date,
    )
}

private fun parseLogFood(args: Map<String, JsonElement>, today: Long): CoachAction.LogFood? {
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
    val date = draftDay(args, today) ?: return null
    return CoachAction.LogFood(
        name = name,
        mealType = meal,
        calories = calories,
        proteinG = protein,
        carbsG = carbs,
        fatG = fat,
        portionAmount = amount,
        portionUnit = unit,
        dateEpochDay = date,
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
    supplements: List<Pair<String, SupplementDay>> = emptyList(),
    heart: HeartDay? = null,
    bloodPressure: List<BloodPressureReading> = emptyList(),
    note: String? = null,
    weightKg: Double? = null,
    measurements: List<MeasurementEntry> = emptyList(),
    cycle: CycleDay? = null,
    cycleDay: Int? = null,
    unit: UnitSystem = UnitSystem.Metric,
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
    // One line for the whole checklist, the call a day's training already makes: five supplements
    // is five lines of an answer with six to spend. Each against that day's own `dueTimes`, never
    // the supplement's current one — the snapshot rule [SupplementDay] is written around.
    if (supplements.isNotEmpty()) {
        appendLine(
            supplements.joinToString(", ", prefix = "Supplements: ") { (name, day) ->
                "$name ${day.taken} of ${day.dueTimes}"
            },
        )
    }
    // The sixth and seventh of the omitted-when-absent group, for the same reason steps and sleep
    // are in it: one comes off a watch and the other off a cuff, and a daily "No readings" would
    // have the coach asking about hardware the user does not own.
    //
    // The lowest reading is never called a resting rate — [HeartDay]'s own rule, and the prompt
    // repeats it for the same reason the app does.
    heart?.let { appendLine("Heart: ${formatBpm(it.averageBpm)} average, ${it.minBpm} lowest") }
    // Every reading of the day, not the day's mean: `blood_pressure_reading` is keyed per reading
    // precisely because a morning and an evening are the thing being measured, and folding them
    // here would throw away the half the user asked about. Each carries the band **the app**
    // assigned it — [categoryOf] is worst-first and the prompt forbids the model deriving one.
    if (bloodPressure.isNotEmpty()) {
        appendLine(
            bloodPressure.joinToString(", ", prefix = "Blood pressure: ") {
                "${formatBloodPressure(it.systolic, it.diastolic)} " +
                    "(${categoryOf(it.systolic, it.diastolic).promptName()})"
            },
        )
    }
    // The body figures themselves, in the user's own unit — see `formatHistory`'s doc for when that
    // stopped being a change-only rule. Omitted when absent, like everything below food and water.
    weightKg?.let { appendLine("Weighed in: ${weightFigure(it, unit)}") }
    if (measurements.isNotEmpty()) {
        appendLine(
            measurements.joinToString(", ", prefix = "Measured: ") {
                "${it.part.promptName()} ${measurementFigure(it.part, it.value, unit)}"
            },
        )
    }
    cycleLine(cycle, cycleDay)?.let(::appendLine)

    // Last, and omitted when blank for the reason the six above are: a day nobody wrote about is
    // not a day with an empty note on it. It is also the only thing in this payload the *user*
    // composed rather than the app measured, so it goes in verbatim — summarising someone's own
    // sentence back at them is what a coach is for, not what a tool should do on the way in.
    note?.takeIf { it.isNotBlank() }?.let { appendLine("Note: $it") }
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

/** A stored kilogram figure in the unit the user reads it in, the one the app shows them. */
private fun weightFigure(kg: Double, unit: UnitSystem): String =
    "${String.format(Locale.US, "%.1f", kg.kgToDisplayUnit(unit))} ${unit.weightUnitLabel()}"

/** The same for a tape site; a body fat stays a percentage in either unit. Interpolated rather
 * than formatted whole, because the label is "%" for a body fat. */
private fun measurementFigure(part: MeasurementPart, stored: Double, unit: UnitSystem): String =
    "${String.format(Locale.US, "%.1f", part.toDisplay(stored, unit))} ${part.unitLabel(unit)}"

/**
 * The day's cycle, or null for a user who does not track it.
 *
 * The flow and symptoms are the user's own taps, reported and never graded. Nothing predicted
 * goes out — no next period and no fertile window, the rule FEATURES.md keeps for the whole app.
 */
private fun cycleLine(cycle: CycleDay?, cycleDay: Int?): String? {
    val parts = listOfNotNull(
        cycleDay?.let { "day $it of their cycle" },
        cycle?.let { flowName(it.flow) },
    ) + cycle?.symptoms.orEmpty().map { it.promptName() }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(", ", prefix = "Cycle: ")
}

/** Null for no flow, which a symptom-only day has. */
private fun flowName(flow: Int): String? = when (flowLevelOf(flow)) {
    FlowLevel.Unstated -> "period"
    FlowLevel.Light -> "period, light flow"
    FlowLevel.Medium -> "period, medium flow"
    FlowLevel.Heavy -> "period, heavy flow"
    null -> null
}

/**
 * Who the user is, in one paragraph of the system instruction: the Mifflin–St Jeor inputs, the
 * goal behind their targets, and the unit they read figures in. The weight is the latest weigh-in,
 * else the onboarding one — [CoachToolbox.weightKg]'s fallback.
 *
 * Null with no profile, which appends nothing, [dietLine]'s rule.
 */
internal fun profileLine(profile: Profile?, latestWeighInKg: Double?): String? {
    profile ?: return null
    val unit = profile.preferredUnit
    val height = String.format(Locale.US, "%.1f", profile.heightCm.cmToDisplayUnit(unit))
    val target = profile.targetWeightKg?.let { ", aiming for ${weightFigure(it, unit)}" }.orEmpty()
    val units = if (unit == UnitSystem.Imperial) "pounds and inches" else "kilograms and centimetres"
    return "About them: ${profile.sex.name.lowercase()}, ${profile.age} years old, $height " +
        "${unit.lengthUnitLabel()} tall, weighing ${weightFigure(latestWeighInKg ?: profile.weightKg, unit)}" +
        "$target, activity level ${profile.activityLevel.name.lowercase()}. Their step goal is " +
        "${formatSteps(profile.stepGoal)} and their fasting goal ${profile.fastingGoalHours} hours. " +
        "They read figures in $units, so answer in those."
}

/** Prompt text, for [MeasurementPart.promptName]'s reason. */
private fun CycleSymptom.promptName(): String = when (this) {
    CycleSymptom.MoodSwings -> "mood swings"
    CycleSymptom.Tender -> "tender breasts"
    CycleSymptom.BackPain -> "back pain"
    else -> name.lowercase()
}

/**
 * One line per day, newest last.
 *
 * Two rules the shape exists for. **Days with nothing logged are named rather than dropped** —
 * `observeDailyNutrition()` returns a dense zero-filled series, so a silent omission would let the
 * model average over days the user never opened the app and report a number nobody ate.
 *
 * And **a body reading goes out as the figure and its change**, in the user's own [unit]. It used to
 * be the change alone, for data minimisation, and that rule was reversed on purpose: the user asked
 * for a coach that sees everything they logged, and a coach that cannot say "you were 82.4 last
 * Monday" was answering around the question. `InsightRequest` — the Home card — still sends a delta
 * only; this is the coach's tool. The first reading in a window has nothing to compare against and
 * says so, the distinction [WeightEntry] trends already draw.
 *
 * A tape measure is the same class of figure as a weigh-in and gets the same treatment, through
 * the same [deltaClauses] fold — one rule with one implementation.
 *
 * [cycle] is the days of a period and the symptoms tapped, reported and never predicted. It
 * reverses the older rule that cycle data never reached an AI payload, for the reason above.
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
    water: List<WaterDay> = emptyList(),
    supplements: List<SupplementDay> = emptyList(),
    heart: List<HeartDay> = emptyList(),
    bloodPressure: List<BloodPressureReading> = emptyList(),
    cycle: List<CycleDay> = emptyList(),
    unit: UnitSystem = UnitSystem.Metric,
): String = buildString {
    val from = today - days + 1
    // Weigh-ins and tape measures in one map, because they are one rule — see the doc above.
    val changes = weightClauses(weights, from, today, unit)
        .mergedWith(measurementClauses(measurements, from, today, unit))
    val periods = cycle.filter { it.dateEpochDay in from..today && it.logged }
        .associateBy { it.dateEpochDay }
    val byDay = nutrition.filter { it.dateEpochDay in from..today }.associateBy { it.dateEpochDay }
    // A day's whole training, not one line per session: a week of two-a-days would otherwise be
    // fourteen lines of an answer that has six of them to spend.
    val training = exercise.filter { it.dateEpochDay in from..today }.groupBy { it.dateEpochDay }
    val slept = sleep.filter { it.dateEpochDay in from..today }.associateBy { it.dateEpochDay }
    val walked = steps.filter { it.dateEpochDay in from..today }.associateBy { it.dateEpochDay }
    val drank = water.filter { it.dateEpochDay in from..today }.associateBy { it.dateEpochDay }
    // Summed per day rather than listed: a span answers "have I been keeping up with them?", and
    // the denominator is that day's own `dueTimes` for the reason `adherenceByDay()` uses it.
    val taken = supplements.filter { it.dateEpochDay in from..today }
        .groupBy { it.dateEpochDay }
        .mapValues { (_, rows) -> rows.sumOf { it.taken } to rows.sumOf { it.dueTimes } }
    val beats = heart.filter { it.dateEpochDay in from..today }.associateBy { it.dateEpochDay }
    // `byDay()` rather than a second fold here: a day someone measured four times is not four
    // days, and that arithmetic already has one implementation the Progress chart draws from.
    val cuff = bloodPressure.byDay().filter { it.dateEpochDay in from..today }
        .associateBy { it.dateEpochDay }
    if (byDay.values.none { it.isLogged } && changes.isEmpty() && training.isEmpty() &&
        slept.isEmpty() && walked.isEmpty() && drank.none { it.value.glasses > 0 } &&
        taken.isEmpty() && beats.isEmpty() && cuff.isEmpty() && periods.isEmpty()
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
        // Stated on every day including a zero, unlike the four above it: water belongs to the
        // dense group — it is something the user does *in this app*, so a missing row is a day
        // they drank nothing rather than a domain they do not track, and a model left to infer
        // that averages a week over the days that happen to carry a line.
        val glasses = ", ${drank[date]?.glasses ?: 0} glasses"
        val pills = taken[date]?.let { (had, due) -> ", supplements $had of $due" }.orEmpty()
        val bpm = beats[date]?.let { ", ${formatBpm(it.averageBpm)} average" }.orEmpty()
        // The day's mean, with the band that mean falls in — the same grade the day tool puts on
        // each reading, so a span and a day cannot describe the same Tuesday two different ways.
        val pressure = cuff[date]?.let {
            ", blood pressure ${formatBloodPressure(it.systolic, it.diastolic)} " +
                "(${categoryOf(it.systolic, it.diastolic).promptName()})"
        }.orEmpty()
        val change = changes[date].orEmpty().joinToString("")
        val period = periods[date]?.let { day ->
            (listOfNotNull(flowName(day.flow)) + day.symptoms.map { it.promptName() })
                .joinToString(", ", prefix = ", cycle: ")
        }.orEmpty()
        appendLine("- $label: $food$glasses$activity$walk$night$pills$bpm$pressure$change$period")
    }
}

/**
 * Each reading in the window against the one before it — including a reading from *before* the
 * window, which is what makes the oldest day in a span carry a change rather than a shrug.
 *
 * Generic over the series because a weigh-in and a tape measure differ only in the words. [clause]
 * is handed the reading and its delta, or null for the first reading of its kind, and returns the
 * whole clause that goes on the day's line.
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
    clause: (entry: T, delta: Double?) -> String,
): Map<Long, List<String>> {
    val sorted = readings.sortedBy(day)
    return buildMap {
        sorted.forEachIndexed { index, entry ->
            val date = day(entry)
            if (date !in from..to) return@forEachIndexed
            val prior = sorted.getOrNull(index - 1)
            put(date, getOrElse(date) { emptyList() } + clause(entry, prior?.let { value(entry) - value(it) }))
        }
    }
}

private fun weightClauses(
    weights: List<WeightEntry>,
    from: Long,
    to: Long,
    unit: UnitSystem,
): Map<Long, List<String>> =
    deltaClauses(weights, from, to, WeightEntry::dateEpochDay, WeightEntry::weightKg) { entry, delta ->
        val figure = weightFigure(entry.weightKg, unit)
        if (delta == null) {
            ", weighed $figure (first one, nothing to compare against)"
        } else {
            val change = String.format(Locale.US, "%+.1f", delta.kgToDisplayUnit(unit))
            ", weighed $figure ($change ${unit.weightUnitLabel()} since the last)"
        }
    }

/**
 * The same fold, once per part: each part is its own series, so a waist is compared against the
 * last waist and never against a thigh. In the user's own unit, as the weight is.
 */
private fun measurementClauses(
    measurements: Map<MeasurementPart, List<MeasurementEntry>>,
    from: Long,
    to: Long,
    unit: UnitSystem,
): Map<Long, List<String>> = measurements.entries.fold(emptyMap()) { acc, (part, entries) ->
    val label = part.unitLabel(unit)
    acc.mergedWith(
        deltaClauses(entries, from, to, MeasurementEntry::dateEpochDay, MeasurementEntry::value) { entry, delta ->
            val figure = measurementFigure(part, entry.value, unit)
            if (delta == null) {
                ", measured ${part.promptName()} $figure (first one, nothing to compare against)"
            } else {
                // The number is formatted on its own and interpolated: `label` is "%" for a body
                // fat, and a "%" inside the format string is a conversion specifier, not a sign.
                val change = String.format(Locale.US, "%+.1f", part.toDisplay(delta, unit))
                ", measured ${part.promptName()} $figure ($change $label since the last)"
            }
        },
    )
}

/** Prompt text, so it stays in Kotlin like every other word the model reads here. The enum's own
 * `label` is the user-facing name and needs a `Context` this file never has. */
private fun MeasurementPart.promptName(): String =
    if (this == MeasurementPart.BodyFat) "body fat" else name.lowercase()

/**
 * The same call for a blood-pressure band, and the reason it exists at all: the band on a reading
 * is **the app's**, not the model's. [categoryOf] is worst-first and load-bearing — 185/70 is a
 * crisis and a normal-first chain would read its diastolic and call it elevated — so handing the
 * model the answer is what keeps it from deriving a different one. The prompt forbids it deriving
 * one either way.
 *
 * Prompt text, in Kotlin for [MeasurementPart.promptName]'s reason: the enum's own `label` is a
 * `@StringRes` and this file has no `Context` to resolve it with. Only the two staged bands need
 * anything done to them; the rest read correctly as they are declared.
 */
private fun BloodPressureCategory.promptName(): String = when (this) {
    BloodPressureCategory.Stage1 -> "Stage 1"
    BloodPressureCategory.Stage2 -> "Stage 2"
    else -> name
}

/** One merge rule for the clause maps, so two series landing on the same day cannot lose one. */
private fun Map<Long, List<String>>.mergedWith(
    other: Map<Long, List<String>>,
): Map<Long, List<String>> = buildMap {
    putAll(this@mergedWith)
    other.forEach { (date, clauses) -> put(date, getOrElse(date) { emptyList() } + clauses) }
}

// endregion

/**
 * The user's own meals and recipes, and the foods they actually eat, by name.
 *
 * Names first and figures second, because the names are what the model has to quote back exactly —
 * `log_saved_meal` matches on them and nothing else. A recipe reports **per serving**, the figure
 * the diary would get, rather than the whole pot.
 *
 * [supplements] is the user's own list with today's count on each, because `log_supplement`
 * matches on the name the same way and because a coach that cannot see a tick drafts one that has
 * already been taken. The dose rides along as the label it is — the app does no arithmetic on
 * "5 g", and the alternative is a model inventing one.
 *
 * [foods] is `observeSuggestions()` — the user's starred favorites and recently logged foods, the
 * same short list the add-entry sheet offers for a one-tap re-log. It is here so that *"what should
 * I eat tonight?"* can be answered with food this user demonstrably eats, at the portion and the
 * figures they log it at, rather than with something invented. It carries full macros where a
 * saved meal carries only a calorie total: a recommendation is steered by the protein gap, and a
 * single food is what the model would otherwise have to estimate.
 *
 * [routines] is the same idea one domain over, and the reason this tool answers *"what should I
 * train today?"* as well as *"what should I eat?"*: `start_routine` matches on these names and
 * nothing else, the lifts are what the card shows and the form opens with, and the weekdays are
 * what makes "today" a real answer rather than a pick from a list. [today] is only read to say
 * which of them is on the plan now.
 */
internal fun formatLibrary(
    meals: List<SavedMeal>,
    recipes: List<Recipe>,
    foods: List<FoodSuggestion>,
    supplements: List<SupplementToday> = emptyList(),
    routines: List<Routine> = emptyList(),
    today: Long = todayEpochDay(),
): String = buildString {
    if (meals.isEmpty() && recipes.isEmpty() && foods.isEmpty() && supplements.isEmpty() &&
        routines.isEmpty()
    ) {
        return "They have not saved any meals or recipes, have not logged any food yet, take " +
            "no supplements and have no workout routines."
    }
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
    if (foods.isNotEmpty()) {
        appendLine("Foods they log often:")
        foods.forEach {
            appendLine(
                "- \"${it.name}\": ${it.portionAmount.formatPortion()} ${it.portionUnit}, " +
                    "${it.calories} kcal, ${it.proteinG}P/${it.carbsG}C/${it.fatG}F",
            )
        }
    }
    if (supplements.isNotEmpty()) {
        appendLine("Supplements they take:")
        supplements.forEach {
            val dose = it.supplement.dose.takeIf(String::isNotBlank)?.let { d -> " ($d)" }.orEmpty()
            appendLine(
                "- \"${it.supplement.name}\"$dose: ${it.taken} of ${it.supplement.timesPerDay} " +
                    "taken today",
            )
        }
    }
    if (routines.isNotEmpty()) {
        appendLine("Workout routines they have saved:")
        routines.forEach { routine ->
            // Unscheduled routines say so rather than printing a blank, the rule `dayLabel()`'s own
            // callers keep; "planned for today" is the clause that answers "what should I train?".
            val plan = when {
                routine.isPlannedOn(today) -> "planned for today"
                routine.days != 0 -> "planned for ${routine.dayLabel()}"
                else -> "not on their weekly plan"
            }
            val lifts = routine.lifts.joinToString(", ") {
                "${it.exerciseName} ${it.sets}x${it.reps}"
            }
            appendLine("- \"${routine.name}\" ($plan): $lifts")
        }
    }
}

/** `:core:designsystem` has the same one-liner for the diary's rows and this module cannot import
 * it — a portion is written "2", never "2.0", wherever this app says one out loud. */
private fun Double.formatPortion(): String =
    if (this == toLong().toDouble()) toLong().toString() else toString()

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
    dateEpochDay: Long = 0,
): List<CoachAction.LogFood>? {
    val wanted = name.trim()
    meals.firstOrNull { it.name.equals(wanted, ignoreCase = true) }
        ?.let { meal -> return meal.items.map { it.toLogFood(it.name, mealType, dateEpochDay) } }
    return recipes.firstOrNull { it.name.equals(wanted, ignoreCase = true) }
        // One row at one serving, named after the recipe — how the app logs a recipe everywhere
        // else, and the reason `perServing()` exists.
        ?.let { recipe -> listOf(recipe.perServing().toLogFood(recipe.name, mealType, dateEpochDay)) }
}

/**
 * One of the user's own supplements, by the name they gave it — or null when nothing matches, which
 * fails the turn.
 *
 * [savedMealRows]' rule applied to a second list, for its reason: `get_library` hands the model the
 * names verbatim, so a name matching nothing is a broken call rather than a near miss. Guessing
 * that "vitamin" meant *Vitamin D* would put a tick the user never asked for one tap from their
 * log — and a supplement is the one domain where the fuzzy match was what kept this tool out.
 */
internal fun supplementDose(
    name: String,
    doses: Int,
    supplements: List<SupplementToday>,
): CoachAction.LogSupplement? {
    val wanted = name.trim()
    val match = supplements.firstOrNull { it.supplement.name.equals(wanted, ignoreCase = true) }
        ?: return null
    // The stored name, not the model's spelling of it: the card, the logged line and the
    // Supplements screen all have to read the same.
    return CoachAction.LogSupplement(
        name = match.supplement.name,
        doses = doses,
        supplementId = match.supplement.id,
    )
}

/**
 * One of the user's own routines, by the name they gave it — or null when nothing matches, which
 * fails the turn.
 *
 * [savedMealRows]' rule applied to a third list, for its reason: `get_library` hands the model the
 * names verbatim, so a name matching nothing is a broken call rather than a near miss. Guessing
 * that "legs" meant *Leg day* would open a workout the user did not name, already filled in with
 * lifts they did not ask for.
 *
 * The stored name, the stored id and the stored lifts: everything the card draws and the form
 * opens with is the user's own, and the model supplied only the name it was given.
 */
internal fun routineToStart(
    name: String,
    routines: List<Routine>,
): CoachAction.StartRoutine? {
    val wanted = name.trim()
    val match = routines.firstOrNull { it.name.equals(wanted, ignoreCase = true) } ?: return null
    return CoachAction.StartRoutine(
        name = match.name,
        routineId = match.id,
        lifts = match.lifts,
    )
}

/**
 * The fasting timer's two transitions, checked against the state they are about to change — or
 * **null when they disagree with it**, which fails the turn.
 *
 * That is the whole reason this function exists. `FastingRepository.start()` is a no-op while a
 * fast is already open and `stop()` is one while none is, so a card drawn without this check could
 * offer a Confirm that does nothing at all — and a proposal card's promise is that the tap does
 * what the card says. Failing the turn is [supplementDose]'s ruling on a name that matches nothing,
 * for its reason: the honest ending is a shrug, not a button that lies.
 *
 * Both figures are the app's. A start takes the profile's [goalHours]; an end takes **the running
 * fast's own**, never the profile's, because `fast_session.goalHours` is snapshotted at the start
 * precisely so that raising the target next month cannot re-price a fast already under way.
 *
 * Pure, with the clock and the two reads passed in — the shape [savedMealRows], [supplementDose]
 * and [routineToStart] all keep, and what lets [CoachToolsTest] pin the four cases.
 */
internal fun fastDraft(
    ending: Boolean,
    active: FastSession?,
    goalHours: Int,
    nowMillis: Long,
): CoachAction.SetFast? = when {
    ending && active != null -> CoachAction.SetFast(
        ending = true,
        goalHours = active.goalHours,
        elapsedMinutes = active.durationMinutes(nowMillis),
    )
    !ending && active == null -> CoachAction.SetFast(ending = false, goalHours = goalHours)
    else -> null
}

/**
 * Whether a draft holds at most one fasting transition.
 *
 * Unlike [navigatingDraftStandsAlone] this permits company: *"I broke my fast with two eggs"* is one
 * sentence, both halves are writes, and both are on the card to be read before the tap — the
 * stand-alone rule a routine has is about its Confirm *leaving the screen*, which this one does
 * not. What it cannot hold is two of these: one tap would start and end a fast, and no sentence
 * means that.
 *
 * Pure and here rather than inline in `send`, for [navigatingDraftStandsAlone]'s reason.
 */
internal fun List<CoachAction>.fastDraftIsSingular(): Boolean =
    count { it is CoachAction.SetFast } <= 1

/**
 * Whether a draft whose Confirm navigates — a routine, or a screen to open — holds nothing else.
 *
 * That Confirm leaves the screen, and a button that both writes a meal and navigates away is two
 * decisions on one tap — the half that happened off screen being the half nobody notices. So a
 * mixed draft fails the whole turn, the ruling `draftDay` already makes about a card whose rows
 * disagree about the day.
 *
 * Pure and here rather than inline in `send`, for [parseAction]'s reason: it is the part a JVM
 * test can reach.
 */
internal fun List<CoachAction>.navigatingDraftStandsAlone(): Boolean =
    none { it is CoachAction.StartRoutine || it is CoachAction.OpenScreen } || size == 1

private fun SavedMealItem.toLogFood(name: String, mealType: MealType, dateEpochDay: Long) =
    CoachAction.LogFood(
        name = name,
        mealType = mealType,
        calories = calories,
        proteinG = proteinG,
        carbsG = carbsG,
        fatG = fatG,
        portionAmount = portionAmount,
        portionUnit = portionUnit,
        dateEpochDay = dateEpochDay,
    )

/** A serving is one row of one portion — the recipe's own name, its own figures, nothing
 * estimated. */
private fun RecipeServing.toLogFood(name: String, mealType: MealType, dateEpochDay: Long) =
    CoachAction.LogFood(
        name = name,
        mealType = mealType,
        calories = calories,
        proteinG = proteinG,
        carbsG = carbsG,
        fatG = fatG,
        portionAmount = 1.0,
        portionUnit = SERVING_UNIT,
        dateEpochDay = dateEpochDay,
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
    // The user's own list, read for the same reason the saved meals are: `log_supplement` matches
    // on a name this publishes, and today's count is what stops the coach drafting a dose that has
    // already been taken.
    private val supplementRepository: SupplementRepository,
    // The last two the coach could not see, and the two Progress pages that carried no question
    // because of it. They widen the same two tools for the same reason the four above did — a
    // question about a heartbeat is still a question about one day or one span.
    private val heartRepository: HeartRepository,
    private val bloodPressureRepository: BloodPressureRepository,
    // The user's own workout templates, read for the supplements' reason: `start_routine` matches
    // on a name this publishes, and the weekdays are what let "what should I train today?" be
    // answered with the routine that is actually on the plan.
    private val routineRepository: RoutineRepository,
    // The day in the user's own words. It widens `get_day` alone: `get_history` is one line per
    // day for up to a month, and free text would swamp the span it exists to summarise.
    private val noteRepository: NoteRepository,
    // The last domain the coach could not see, held back until the user asked for a coach that
    // sees everything. Reported, never predicted — see `cycleLine`.
    private val cycleRepository: CycleRepository,
) {
    /** Null for a tool this does not run — which is every write tool, and is how the caller's loop
     * tells a question from an instruction without a second lookup. */
    suspend fun runTool(name: String, args: Map<String, JsonElement>): String? = when (name) {
        TOOL_GET_DAY -> getDay(daysAgoOf(args))
        TOOL_GET_HISTORY -> getHistory(historyDaysOf(args))
        TOOL_GET_LIBRARY -> getLibrary()
        else -> null
    }

    /**
     * The whole library, not the newest five the add-entry panel shows: the model is answering
     * "what have I saved?", and a truncated list would have it deny a meal the user can see.
     *
     * The foods are the opposite call and deliberately so — `observeSuggestions()` is already
     * capped at `MAX_SUGGESTIONS`, because "what do I eat" is answered by the handful they keep
     * going back to, not by every row the diary holds.
     */
    private suspend fun getLibrary(): String = formatLibrary(
        meals = foodRepository.observeAllSavedMeals().first(),
        recipes = foodRepository.observeAllRecipes().first(),
        foods = foodRepository.observeSuggestions().first(),
        supplements = supplementRepository.observeToday().first(),
        routines = routineRepository.observeRoutines().first(),
        today = todayEpochDay(),
    )

    /**
     * What the user won't eat, as the one sentence the meal-idea prompt already says it in.
     *
     * Null for `None` and for no profile at all, which appends nothing — the coach is told what to
     * avoid or it is told nothing, never that there are "no restrictions". It lives on the toolbox
     * because the toolbox is where this file's profile reads are, even though the caller spends it
     * on the system instruction rather than on a tool result.
     */
    suspend fun dietLine(): String? =
        dietLine(profileRepository.observeProfile().first()?.dietaryPreference)

    /** Who they are, for the system instruction beside [dietLine]. Null with no profile. */
    suspend fun profileLine(): String? =
        profileLine(profileRepository.observeProfile().first(), latestWeighInKg())

    /** The two reads [savedMealRows] needs, and nothing else — the matching itself is pure, so it
     * is the part a JVM test can reach. */
    suspend fun savedMealRows(
        name: String,
        mealType: MealType,
        dateEpochDay: Long,
    ): List<CoachAction.LogFood>? =
        savedMealRows(
            name = name,
            mealType = mealType,
            meals = foodRepository.observeAllSavedMeals().first(),
            recipes = foodRepository.observeAllRecipes().first(),
            dateEpochDay = dateEpochDay,
        )

    /** The one read [supplementDose] needs — the matching itself is pure, [savedMealRows]' shape. */
    suspend fun supplementDose(name: String, doses: Int): CoachAction.LogSupplement? =
        supplementDose(name, doses, supplementRepository.observeToday().first())

    /** The one read [routineToStart] needs — the matching itself is pure, the same shape again. */
    suspend fun routineToStart(name: String): CoachAction.StartRoutine? =
        routineToStart(name, routineRepository.observeRoutines().first())

    /** The two reads [fastDraft] needs — the running fast, and the goal a *new* one would take.
     * The rule itself is pure, the same shape a fourth time. */
    suspend fun fastDraft(ending: Boolean): CoachAction.SetFast? = fastDraft(
        ending = ending,
        active = fastingRepository.observeActive().first(),
        goalHours = profileRepository.observeProfile().first()?.fastingGoalHours
            ?: DEFAULT_FAST_GOAL_HOURS,
        nowMillis = System.currentTimeMillis(),
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

    /** What a drafted note is about to replace, blank on a day nobody has written about — the one
     * thing a card has to say that the model is not allowed to supply. Zero is today, the reading
     * [CoachAction.draftedOn] gives it. */
    suspend fun existingNote(dateEpochDay: Long): String =
        noteRepository.observeForDate(dateEpochDay.takeIf { it > 0 } ?: todayEpochDay())
            .first().text

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
        val names = supplementRepository.allSupplements().associate { it.id to it.name }
        val cycle = cycleRepository.observeDays().first()
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
            // Named from the current list, counted off that day's own row: a supplement dropped
            // since keeps the name its past days point at, which is what the soft delete is for.
            supplements = supplementRepository.observeDays().first()
                .filter { it.dateEpochDay == date }
                .mapNotNull { day ->
                    names[day.supplementId]?.let { it to day }
                },
            // `observeDays()` rather than `observeToday()`: a day tool reads any day, and the
            // null for a day nothing was imported for is what leaves the line out.
            heart = heartRepository.observeDays().first().firstOrNull { it.dateEpochDay == date },
            // Every reading taken that day, in the order they were taken — the table is keyed per
            // reading, and a morning and an evening are two answers, not one.
            bloodPressure = bloodPressureRepository.observeReadings().first()
                .filter { it.dateEpochDay == date },
            // Blank whenever nothing was written, which is what leaves the line out entirely —
            // the rule steps, sleep, mood and fasting already follow.
            note = noteRepository.observeForDate(date).first().text,
            weightKg = progressRepository.observeWeightEntries().first()
                .firstOrNull { it.dateEpochDay == date }?.weightKg,
            measurements = progressRepository.observeMeasurements().first().values.flatten()
                .filter { it.dateEpochDay == date },
            cycle = cycle.firstOrNull { it.dateEpochDay == date && it.logged },
            cycleDay = cycle.periods().cycleDayNumber(date),
            unit = profile?.preferredUnit ?: UnitSystem.Metric,
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
        // `allDays()` holds only the days with a glass in them, which is why the line prints a
        // zero for the rest rather than leaving them out.
        water = waterRepository.allDays(),
        supplements = supplementRepository.observeDays().first(),
        heart = heartRepository.observeDays().first(),
        bloodPressure = bloodPressureRepository.observeReadings().first(),
        cycle = cycleRepository.observeDays().first(),
        unit = unitSystem(),
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
    is CoachAction.LogSavedMeal -> toolbox.savedMealRows(name, mealType, dateEpochDay)
    // Never null: a weigh-in needs no history to be recorded, so a first one still drafts — it
    // simply draws no change line.
    is CoachAction.LogWeight -> listOf(
        copy(unit = toolbox.unitSystem(), previousKg = toolbox.latestWeighInKg()),
    )
    // Where the model's spelling becomes the user's own row. Null when it names nothing they take,
    // which fails the turn rather than ticking the nearest thing.
    is CoachAction.LogSupplement -> toolbox.supplementDose(name, doses)?.let(::listOf)
    // The second half of [CoachAction.LogMeasurement]'s two checks, and the half that needs a unit:
    // `range()` is in stored units, so 32 inches is in band for an arm and 32 centimetres is not.
    // Null fails the turn rather than clamping, the rule the parse above already keeps.
    is CoachAction.LogMeasurement -> toolbox.unitSystem().let { unit ->
        listOf(copy(unit = unit)).takeIf { part.fromDisplay(value, unit) in part.range() }
    }
    // [CoachAction.LogSupplement]'s line one domain over: the model's spelling becomes the user's
    // own row, and a name that is in no library fails the turn rather than opening the nearest
    // workout.
    is CoachAction.StartRoutine -> toolbox.routineToStart(name)?.let(::listOf)
    // Never null: a day with nothing written still drafts, it simply draws no replaces line.
    // The old text is read here and nowhere else — the write does not need it, only the card does.
    is CoachAction.LogNote -> listOf(copy(replaces = toolbox.existingNote(dateEpochDay)))
    // The only branch that resolves against a *state* rather than a row. Null when the timer
    // disagrees with the draft — a start against an open fast, an end against none — which fails
    // the turn rather than drawing a Confirm that would be one of the repository's no-ops.
    is CoachAction.SetFast -> toolbox.fastDraft(ending)?.let(::listOf)
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
            // Zero is today, which is what `FoodRepository.addEntries` already reads it as — the
            // coach needs no special case to log into the day the card named.
            dateEpochDay = it.dateEpochDay,
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
 * Glasses to **add**, summed per day.
 *
 * Summed rather than applied one at a time, and that is the load-bearing half: a water write takes
 * the day's *new total*, so two rows applied in sequence would have the second overwrite the first
 * and a draft of two glasses would land as one. Keyed by day now that a draft can be backdated —
 * the same reason [supplementDoses] is keyed by id.
 */
internal fun List<CoachAction>.glassesToAdd(): Map<Long, Int> =
    filterIsInstance<CoachAction.LogWater>()
        .groupBy { it.dateEpochDay }
        .mapValues { (_, rows) -> rows.sumOf { it.glasses } }

/**
 * The one day every row of a draft agrees on, or **null when they disagree**, which fails the turn.
 *
 * The card draws one day for the whole card, so a draft holding yesterday's eggs and today's
 * coffee could only ever label one of them correctly — and the card's whole promise is that what
 * it shows is what gets written. A weigh-in and a supplement tick are always today
 * ([CoachAction.draftedOn] is null for them), so a backdated draft cannot quietly carry one.
 *
 * "Log the eggs I had yesterday and a coffee just now" is the sentence this refuses. It is not one
 * anybody types, and refusing it costs a re-ask; labelling it wrong costs a row on the wrong day.
 */
internal fun List<CoachAction>.draftDay(today: Long): Long? =
    map { it.draftedOn ?: today }.distinct().singleOrNull()

/**
 * Doses to **add** today, summed per supplement.
 *
 * [glassesToAdd]'s lesson on a second table: `setTakenToday` takes the day's *new count*, so two
 * doses of the same supplement written one after the other would have the second overwrite the
 * first and land as one. Two *different* supplements are two entries, because they are two rows.
 */
internal fun List<CoachAction>.supplementDoses(): Map<Long, Int> =
    filterIsInstance<CoachAction.LogSupplement>()
        .groupBy { it.supplementId }
        .mapValues { (_, actions) -> actions.sumOf { it.doses } }

/**
 * The one mood row a settled draft writes, folded from however many it holds — or null when it
 * holds none.
 *
 * [glassesToAdd]'s lesson a third time, with the opposite arithmetic. Water and doses *add*, so
 * they sum; a mood is an absolute value, so the last one the user agreed to wins. **Per column**,
 * which is the load-bearing half: a draft of "felt great" then "energy was low" is two actions
 * whose zeros must not erase each other, and folding them into one row is what stops the second
 * write blanking the first's column.
 */
internal fun List<CoachAction>.moodToSet(): CoachAction.LogMood? {
    val moods = filterIsInstance<CoachAction.LogMood>()
    if (moods.isEmpty()) return null
    return CoachAction.LogMood(
        mood = moods.lastOrNull { it.mood > 0 }?.mood ?: 0,
        energy = moods.lastOrNull { it.energy > 0 }?.energy ?: 0,
    )
}

/**
 * The one note a settled draft writes, or null when it holds none.
 *
 * [moodToSet]'s fold with none of its per-column care: a day holds one note and a note is
 * absolute, so the last one the user agreed to is the one that lands. Two in a draft is a model
 * repeating itself rather than two things to write, and writing both would leave the first
 * invisible behind the second anyway.
 */
internal fun List<CoachAction>.noteToWrite(): CoachAction.LogNote? =
    filterIsInstance<CoachAction.LogNote>().lastOrNull()

/** The envelope the SDK requires around a [String] result. One key, because the result is prose
 * and prose has no fields. */
internal fun toolResponse(result: String): JsonObject =
    JsonObject(mapOf("result" to JsonPrimitive(result)))
