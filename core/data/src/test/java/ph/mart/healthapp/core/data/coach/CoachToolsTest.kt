package ph.mart.healthapp.core.data.coach

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.health.SleepNight
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.progress.WeightEntry

/**
 * The coach's tool boundary.
 *
 * [parseAction] is the only way a model's output becomes something one tap from the diary, so this
 * is the file that earns the design: the whole parse is pure over `kotlinx.serialization` types,
 * which is exactly what the photo path's `org.json` parse cannot be on the JVM.
 *
 * The formatters are here for a second reason — they are what the *model* reads back, and a day
 * described two different ways is a coach contradicting itself mid-answer.
 */
class CoachToolsTest {

    private fun args(vararg pairs: Pair<String, Any>): Map<String, JsonElement> =
        pairs.associate { (key, value) ->
            key to when (value) {
                is String -> JsonPrimitive(value)
                is Int -> JsonPrimitive(value)
                is Double -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                else -> error("unsupported test value")
            }
        }

    private fun foodArgs(vararg overrides: Pair<String, Any>): Map<String, JsonElement> {
        val base = mutableMapOf<String, Any>(
            "name" to "Scrambled eggs",
            "meal" to "Breakfast",
            "calories" to 220,
            "protein_g" to 14,
            "carbs_g" to 2,
            "fat_g" to 17,
            "portion_amount" to 2.0,
            "portion_unit" to "eggs",
        )
        overrides.forEach { (key, value) -> base[key] = value }
        return args(*base.toList().toTypedArray())
    }

    // region The parse accepts what it should

    @Test
    fun `a well-formed food call becomes an action`() {
        val action = parseAction(TOOL_LOG_FOOD, foodArgs()) as CoachAction.LogFood
        assertEquals("Scrambled eggs", action.name)
        assertEquals(MealType.Breakfast, action.mealType)
        assertEquals(220, action.calories)
        assertEquals(14, action.proteinG)
        assertEquals(2, action.carbsG)
        assertEquals(17, action.fatG)
        assertEquals(2.0, action.portionAmount, 0.0)
        assertEquals("eggs", action.portionUnit)
    }

    /** Gemini is known to quote a number now and then. That is a formatting wobble, not a wrong
     * answer, so it is read rather than rejected. */
    @Test
    fun `a quoted number is read, not rejected`() {
        val action = parseAction(TOOL_LOG_FOOD, foodArgs("calories" to "220")) as CoachAction.LogFood
        assertEquals(220, action.calories)
    }

    @Test
    fun `the meal name is matched case-insensitively`() {
        val action = parseAction(TOOL_LOG_FOOD, foodArgs("meal" to "dinner")) as CoachAction.LogFood
        assertEquals(MealType.Dinner, action.mealType)
    }

    /** The portion is a label on the row, not arithmetic — the calories are already for the whole
     * of it — so a missing one falls back rather than failing an otherwise good draft. */
    @Test
    fun `a missing portion falls back instead of failing the draft`() {
        val given = foodArgs().filterKeys { it != "portion_amount" && it != "portion_unit" }
        val action = parseAction(TOOL_LOG_FOOD, given) as CoachAction.LogFood
        assertEquals(1.0, action.portionAmount, 0.0)
        assertEquals("serving", action.portionUnit)
    }

    @Test
    fun `a water call becomes an action`() {
        val action = parseAction(TOOL_LOG_WATER, args("glasses" to 2)) as CoachAction.LogWater
        assertEquals(2, action.glasses)
    }

    // endregion

    // region The parse rejects what it should

    @Test
    fun `an unknown tool is not an action`() {
        assertNull(parseAction("log_weight", args("weight_kg" to 70.0)))
    }

    /** A read tool reaching the parse would mean the caller's WRITE_TOOLS check had failed. It
     * still yields null rather than something confirmable. */
    @Test
    fun `a read tool is not an action`() {
        assertNull(parseAction(TOOL_GET_DAY, args("days_ago" to 1)))
    }

    @Test
    fun `a missing required field fails the draft`() {
        listOf("name", "meal", "calories", "protein_g", "carbs_g", "fat_g").forEach { missing ->
            val given = foodArgs().filterKeys { it != missing }
            assertNull("dropping $missing should fail the parse", parseAction(TOOL_LOG_FOOD, given))
        }
    }

    @Test
    fun `a non-numeric string where a number belongs fails the draft`() {
        assertNull(parseAction(TOOL_LOG_FOOD, foodArgs("calories" to "about four hundred")))
    }

    /** A name that arrived as a number or a boolean is a broken call, and coercing it would put
     * the word "true" in the diary. */
    @Test
    fun `a non-string name fails the draft`() {
        assertNull(parseAction(TOOL_LOG_FOOD, foodArgs("name" to true)))
        assertNull(parseAction(TOOL_LOG_FOOD, foodArgs("name" to 12)))
    }

    @Test
    fun `a blank name fails the draft`() {
        assertNull(parseAction(TOOL_LOG_FOOD, foodArgs("name" to "   ")))
    }

    @Test
    fun `an unknown meal fails the draft`() {
        assertNull(parseAction(TOOL_LOG_FOOD, foodArgs("meal" to "Brunch")))
    }

    /** The ceiling that exists so a dropped decimal point cannot put 90,000 kcal in front of a
     * Confirm button. */
    @Test
    fun `an absurd or negative figure fails the draft`() {
        assertNull(parseAction(TOOL_LOG_FOOD, foodArgs("calories" to MAX_ACTION_CALORIES + 1)))
        assertNull(parseAction(TOOL_LOG_FOOD, foodArgs("calories" to -1)))
        assertNull(parseAction(TOOL_LOG_FOOD, foodArgs("protein_g" to MAX_ACTION_MACRO_G + 1)))
        assertNull(parseAction(TOOL_LOG_FOOD, foodArgs("fat_g" to -5)))
    }

    @Test
    fun `zero glasses and a tankful both fail the draft`() {
        assertNull(parseAction(TOOL_LOG_WATER, args("glasses" to 0)))
        assertNull(parseAction(TOOL_LOG_WATER, args("glasses" to -1)))
        assertNull(parseAction(TOOL_LOG_WATER, args("glasses" to MAX_ACTION_GLASSES + 1)))
        assertNull(parseAction(TOOL_LOG_WATER, emptyMap()))
    }

    // endregion

    // region Day offsets

    @Test
    fun `a missing or out-of-range day offset clamps rather than failing`() {
        assertEquals(0, daysAgoOf(emptyMap()))
        assertEquals(0, daysAgoOf(args("days_ago" to -3)))
        assertEquals(MAX_DAYS_AGO, daysAgoOf(args("days_ago" to 900)))
        assertEquals(7, daysAgoOf(args("days_ago" to 7)))
    }

    @Test
    fun `a missing history span defaults to a week and clamps to a month`() {
        assertEquals(7, historyDaysOf(emptyMap()))
        assertEquals(1, historyDaysOf(args("days" to 0)))
        assertEquals(MAX_HISTORY_DAYS, historyDaysOf(args("days" to 365)))
    }

    // endregion

    // region What the model reads back

    private fun food(name: String, meal: MealType, kcal: Int) = FoodEntry(
        name = name,
        mealType = meal,
        portionAmount = 1.0,
        portionUnit = "serving",
        calories = kcal,
        proteinG = 10,
        carbsG = 20,
        fatG = 5,
    )

    @Test
    fun `a day lists every food, its totals and its target`() {
        val text = formatDay(
            label = "Yesterday",
            foods = listOf(food("Oats", MealType.Breakfast, 300), food("Chicken", MealType.Lunch, 450)),
            targetCalories = 2000,
            waterGlasses = 5,
            exercise = listOf(
                ExerciseEntry(type = ExerciseType.Walk, name = "Walk", minutes = 30, burnedKcal = 120),
            ),
        )
        assertTrue(text, "- Oats (Breakfast): 300 kcal, 10P/20C/5F" in text)
        assertTrue(text, "- Chicken (Lunch): 450 kcal, 10P/20C/5F" in text)
        assertTrue(text, "Totals: 750 of 2000 kcal, 20P/40C/10F" in text)
        assertTrue(text, "Water: 5 glasses" in text)
        assertTrue(text, "Walk, 30 min, 120 kcal burned" in text)
    }

    /**
     * The three the coach used to be blind to. They ride on the existing day tool rather than on
     * tools of their own, so this is where "the coach can see my sleep" is actually asserted.
     */
    @Test
    fun `a day carries sleep, mood and a finished fast when they are tracked`() {
        val text = formatDay(
            label = "Today",
            foods = emptyList(),
            targetCalories = null,
            waterGlasses = 0,
            exercise = emptyList(),
            sleepMinutes = 432,
            mood = MoodDay(dateEpochDay = 20_000L, mood = 4, energy = 2),
            fastedMinutes = 980,
        )
        assertTrue(text, "Slept: 7h 12m" in text)
        assertTrue(text, "Felt: mood 4/5, energy 2/5" in text)
        assertTrue(text, "Fasted: 16h 20m" in text)
    }

    /**
     * Absent means *untracked*, and the whole point of omitting the line is that the coach cannot
     * then nag about a watch the user does not own. A zero-filled "No sleep recorded" every day
     * would do exactly that.
     */
    @Test
    fun `an untracked domain leaves no line at all`() {
        val text = formatDay("Today", emptyList(), null, waterGlasses = 0, exercise = emptyList())
        listOf("Slept", "Felt", "Fasted").forEach {
            assertTrue("$it appeared for an untracked domain: $text", it !in text)
        }
    }

    /** `mood_day` stores 0 for "not set", never a zero score — so a day where only the face was
     * tapped reports the face and says nothing about energy. */
    @Test
    fun `a half-filled check-in reports only the half that was filled`() {
        val text = formatDay(
            label = "Today",
            foods = emptyList(),
            targetCalories = null,
            waterGlasses = 0,
            exercise = emptyList(),
            mood = MoodDay(dateEpochDay = 20_000L, mood = 0, energy = 5),
        )
        assertTrue(text, "Felt: energy 5/5" in text)
        assertTrue(text, "mood" !in text)
    }

    /** An empty day has to say so out loud. A model handed a blank block fills it in. */
    @Test
    fun `an empty day says nothing was logged rather than going quiet`() {
        val text = formatDay("Today", emptyList(), targetCalories = null, waterGlasses = 0, exercise = emptyList())
        assertTrue(text, "No food logged." in text)
        assertTrue(text, "No activity logged." in text)
        assertTrue(text, "Water: 0 glasses" in text)
    }

    /**
     * `observeDailyNutrition()` returns a dense zero-filled series, so a day the user never opened
     * the app arrives looking like a day they ate nothing. Dropping those rows silently would let
     * the model average over them and report a figure nobody ate.
     */
    @Test
    fun `unlogged days are named, not dropped`() {
        val today = 20_000L
        val text = formatHistory(
            days = 3,
            nutrition = listOf(
                DayNutrition(dateEpochDay = today - 2, calories = 1800, proteinG = 120, carbsG = 0, fatG = 0),
                DayNutrition(dateEpochDay = today - 1, calories = 0, proteinG = 0, carbsG = 0, fatG = 0),
                DayNutrition(dateEpochDay = today, calories = 900, proteinG = 60, carbsG = 0, fatG = 0),
            ),
            weights = listOf(
                WeightEntry(dateEpochDay = today - 2, weightKg = 72.8),
                WeightEntry(dateEpochDay = today, weightKg = 72.4),
            ),
            today = today,
        )
        assertTrue(text, "- 2 days ago: 1800 kcal, 120g protein" in text)
        assertTrue(text, "- Yesterday: nothing logged" in text)
        assertTrue(text, "- Today: 900 kcal, 60g protein, weighed in (-0.4 kg since the last)" in text)
    }

    /**
     * The data-minimisation rule `InsightRequest` is built around, applied to a tool: a weigh-in
     * leaves the device as a *change*, never as a weight. A tool is not a loophole in that just
     * because the user asked the question out loud.
     */
    @Test
    fun `a weigh-in never sends an absolute weight`() {
        val today = 20_000L
        val text = formatHistory(
            days = 3,
            nutrition = (0L..2L).map {
                DayNutrition(dateEpochDay = today - it, calories = 1500, proteinG = 90, carbsG = 0, fatG = 0)
            },
            weights = listOf(
                WeightEntry(dateEpochDay = today - 5, weightKg = 94.9),
                WeightEntry(dateEpochDay = today - 2, weightKg = 94.2),
                WeightEntry(dateEpochDay = today, weightKg = 93.6),
            ),
            today = today,
        )
        listOf("94.9", "94.2", "93.6").forEach {
            assertTrue("$it leaked into: $text", it !in text)
        }
        // A weigh-in older than the window is still what the oldest day in it compares against.
        assertTrue(text, "-0.7 kg since the last" in text)
        assertTrue(text, "-0.6 kg since the last" in text)
    }

    @Test
    fun `the very first weigh-in says it has nothing to compare against`() {
        val today = 20_000L
        val text = formatHistory(
            days = 1,
            nutrition = listOf(
                DayNutrition(dateEpochDay = today, calories = 1500, proteinG = 90, carbsG = 0, fatG = 0),
            ),
            weights = listOf(WeightEntry(dateEpochDay = today, weightKg = 80.0)),
            today = today,
        )
        assertTrue(text, "nothing to compare against" in text)
        assertTrue(text, "80" !in text)
    }

    @Test
    fun `a span with nothing in it says so in one line`() {
        val today = 20_000L
        val text = formatHistory(
            days = 2,
            nutrition = listOf(
                DayNutrition(dateEpochDay = today - 1, calories = 0, proteinG = 0, carbsG = 0, fatG = 0),
                DayNutrition(dateEpochDay = today, calories = 0, proteinG = 0, carbsG = 0, fatG = 0),
            ),
            weights = emptyList(),
            today = today,
        )
        assertEquals("Nothing logged in the last 2 days.", text)
    }

    /**
     * "How did my training week go?" was unanswerable: a span carried food and weigh-ins only.
     * Two-a-days collapse into one line per day, because six lines is the whole answer's budget.
     */
    @Test
    fun `a span carries the day's training and sleep`() {
        val today = 20_000L
        val text = formatHistory(
            days = 2,
            nutrition = listOf(
                DayNutrition(dateEpochDay = today - 1, calories = 2100, proteinG = 140, carbsG = 0, fatG = 0),
                DayNutrition(dateEpochDay = today, calories = 1800, proteinG = 120, carbsG = 0, fatG = 0),
            ),
            weights = emptyList(),
            today = today,
            exercise = listOf(
                ExerciseEntry(dateEpochDay = today, type = ExerciseType.Run, minutes = 30, burnedKcal = 300),
                ExerciseEntry(dateEpochDay = today, type = ExerciseType.Yoga, minutes = 20, burnedKcal = 60),
            ),
            sleep = listOf(SleepNight(dateEpochDay = today - 1, minutesAsleep = 400)),
        )
        assertTrue(text, "- Today: 1800 kcal, 120g protein, 50 min activity, 360 kcal burned" in text)
        assertTrue(text, "- Yesterday: 2100 kcal, 140g protein, slept 6h 40m" in text)
    }

    /** The series a day's line is counted off is the window, not the nutrition list: a day holding
     * only a workout still gets one. */
    @Test
    fun `a day with training but no food is still a line`() {
        val today = 20_000L
        val text = formatHistory(
            days = 1,
            nutrition = emptyList(),
            weights = emptyList(),
            today = today,
            exercise = listOf(
                ExerciseEntry(dateEpochDay = today, type = ExerciseType.Walk, minutes = 45, burnedKcal = 150),
            ),
        )
        assertTrue(text, "- Today: nothing logged, 45 min activity, 150 kcal burned" in text)
    }

    /** The window is what bounds the answer, not the series handed in — a year of dense rows must
     * not become a year of lines because the model asked for a week. */
    @Test
    fun `only the requested window is described`() {
        val today = 20_000L
        val text = formatHistory(
            days = 2,
            nutrition = (0L..9L).map {
                DayNutrition(dateEpochDay = today - it, calories = 1000, proteinG = 50, carbsG = 0, fatG = 0)
            },
            weights = emptyList(),
            today = today,
        )
        assertEquals(2, text.lines().count { it.startsWith("- ") })
    }

    // endregion
}
