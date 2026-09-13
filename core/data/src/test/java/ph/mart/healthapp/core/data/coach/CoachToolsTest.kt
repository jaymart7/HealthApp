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
import ph.mart.healthapp.core.data.food.FoodSuggestion
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.health.SleepNight
import ph.mart.healthapp.core.data.health.StepDay
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.data.supplement.SupplementDay
import ph.mart.healthapp.core.data.supplement.SupplementToday
import ph.mart.healthapp.core.data.water.WaterDay

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

    /**
     * The figure is the user's own, said out loud, and it is left in whatever unit they said it
     * in: `resolve` stamps the profile's, and `settle` is the one place it becomes kilograms. The
     * defaults here are what "not resolved yet" looks like, the reading a zero burn already has.
     */
    @Test
    fun `a weight call becomes an action the app will unit itself`() {
        val action = parseAction(TOOL_LOG_WEIGHT, args("weight" to 82.4)) as CoachAction.LogWeight
        assertEquals(82.4, action.weight, 0.001)
        assertEquals(UnitSystem.Metric, action.unit)
        assertNull(action.previousKg)
    }

    /** One decimal, so the card and the row it writes cannot show two different numbers. */
    @Test
    fun `a weight is rounded to one decimal`() {
        val action = parseAction(TOOL_LOG_WEIGHT, args("weight" to 82.44999)) as CoachAction.LogWeight
        assertEquals(82.4, action.weight, 0.001)
    }

    @Test
    fun `a quoted weight is read, not rejected`() {
        val action = parseAction(TOOL_LOG_WEIGHT, args("weight" to "181")) as CoachAction.LogWeight
        assertEquals(181.0, action.weight, 0.001)
    }

    // endregion

    // region The parse rejects what it should

    /**
     * The burn is deliberately zero here: [parseAction] is pure, and `priced()` is what fills it
     * in from the user's own latest weigh-in. A model asked for a calorie figure invents one, and
     * the app already owns the MET arithmetic the log-exercise sheet uses.
     */
    @Test
    fun `an exercise call becomes an action the app will price itself`() {
        val action = parseAction(
            TOOL_LOG_EXERCISE,
            args("type" to "Run", "minutes" to 30, "name" to "Morning run"),
        ) as CoachAction.LogExercise
        assertEquals(ExerciseType.Run, action.type)
        assertEquals(30, action.minutes)
        assertEquals("Morning run", action.name)
        assertEquals(0, action.burnedKcal)
    }

    /** An empty name is what `ExerciseEntry` means by "call it by its type", so a nameless call is
     * a draft rather than a rejection — unlike a nameless food, which has nothing to show. */
    @Test
    fun `an exercise call needs no name`() {
        val action = parseAction(TOOL_LOG_EXERCISE, args("type" to "yoga", "minutes" to 45))
            as CoachAction.LogExercise
        assertEquals(ExerciseType.Yoga, action.type)
        assertEquals("", action.name)
    }

    @Test
    fun `an unknown activity type fails the draft`() {
        assertNull(parseAction(TOOL_LOG_EXERCISE, args("type" to "Parkour", "minutes" to 20)))
    }

    /** The dropped decimal, at the other end of the same card from [MAX_ACTION_CALORIES]: "a 90
     * minute run" read as 900 is a day and a half of running. */
    @Test
    fun `an absurd or missing duration fails the draft`() {
        assertNull(parseAction(TOOL_LOG_EXERCISE, args("type" to "Run", "minutes" to 900)))
        assertNull(parseAction(TOOL_LOG_EXERCISE, args("type" to "Run", "minutes" to 0)))
        assertNull(parseAction(TOOL_LOG_EXERCISE, args("type" to "Run")))
    }

    /** The same dropped decimal the other ceilings guard, in a band that has to be right before
     * the unit is known — 20 kg and 44 lb are both weights, so the band is wide and the card is
     * what catches the rest. */
    @Test
    fun `an absurd, missing or non-numeric weight fails the draft`() {
        assertNull(parseAction(TOOL_LOG_WEIGHT, args("weight" to MAX_ACTION_WEIGHT + 1)))
        assertNull(parseAction(TOOL_LOG_WEIGHT, args("weight" to 0)))
        assertNull(parseAction(TOOL_LOG_WEIGHT, args("weight" to -70)))
        assertNull(parseAction(TOOL_LOG_WEIGHT, args("weight" to "about eighty")))
        assertNull(parseAction(TOOL_LOG_WEIGHT, emptyMap()))
    }

    @Test
    fun `an unknown tool is not an action`() {
        assertNull(parseAction("delete_entry", args("id" to 4)))
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

    // region The user's own library

    private fun item(name: String, kcal: Int) = SavedMealItem(
        name = name,
        portionAmount = 1.0,
        portionUnit = "serving",
        calories = kcal,
        proteinG = 10,
        carbsG = 20,
        fatG = 5,
    )

    private val usualBreakfast =
        SavedMeal(id = 1, name = "Usual breakfast", items = listOf(item("Oats", 300), item("Banana", 90)))

    private val chilli = Recipe(id = 1, name = "Chilli", servings = 4, items = listOf(item("Beef", 800)))

    /** Names first: they are what the model has to quote back, because `log_saved_meal` matches on
     * the name and nothing else. */
    @Test
    fun `the library lists meals and recipes by name`() {
        val text = formatLibrary(listOf(usualBreakfast), listOf(chilli), emptyList())
        assertTrue(text, "\"Usual breakfast\": 2 items, 390 kcal" in text)
        // Per serving, not the whole pot — the figure the diary would actually get.
        assertTrue(text, "\"Chilli\": 200 kcal per serving" in text)
    }

    private val yogurt = FoodSuggestion(
        name = "Greek yogurt",
        portionAmount = 170.0,
        portionUnit = "g",
        calories = 140,
        proteinG = 17,
        carbsG = 9,
        fatG = 4,
        isFavorite = true,
    )

    /**
     * The half of this tool that answers *"what should I eat?"*: the foods the user actually logs,
     * at their own portion and their own figures. Full macros, because the answer is steered by
     * the protein gap and a single food is what the model would otherwise estimate.
     */
    @Test
    fun `the library lists the foods they log often, with the figures they log them at`() {
        val text = formatLibrary(emptyList(), emptyList(), listOf(yogurt))
        // "170 g", never "170.0 g" — a portion is said the way the diary says it.
        assertTrue(text, "\"Greek yogurt\": 170 g, 140 kcal, 17P/9C/4F" in text)
    }

    /** A user with nothing saved still has food they eat, and that list alone is a usable answer —
     * the empty sentence must not claim otherwise. */
    @Test
    fun `foods alone are a library`() {
        val text = formatLibrary(emptyList(), emptyList(), listOf(yogurt))
        assertTrue(text, "Greek yogurt" in text)
        assertTrue(text, "have not" !in text)
    }

    @Test
    fun `an empty library says so rather than going quiet`() {
        assertEquals(
            "They have not saved any meals or recipes, have not logged any food yet, and take " +
                "no supplements.",
            formatLibrary(emptyList(), emptyList(), emptyList()),
        )
    }

    private val creatine = SupplementToday(
        supplement = Supplement(id = 2, name = "Creatine", dose = "5 g", timesPerDay = 2),
        taken = 1,
    )

    private val vitaminD = SupplementToday(
        supplement = Supplement(id = 1, name = "Vitamin D", dose = "2000 IU", timesPerDay = 1),
        taken = 0,
    )

    /** Names and today's count: the first is what `log_supplement` matches on, the second is what
     * stops the coach drafting a dose that has already been taken. */
    @Test
    fun `the library lists supplements with their dose and today's count`() {
        val text = formatLibrary(emptyList(), emptyList(), emptyList(), listOf(creatine, vitaminD))
        assertTrue(text, "\"Creatine\" (5 g): 1 of 2 taken today" in text)
        assertTrue(text, "\"Vitamin D\" (2000 IU): 0 of 1 taken today" in text)
    }

    @Test
    fun `a supplement call becomes an action the app will match itself`() {
        val action = parseAction(TOOL_LOG_SUPPLEMENT, args("name" to "Creatine", "doses" to 2))
        assertEquals(CoachAction.LogSupplement(name = "Creatine", doses = 2), action)
        // The id is `resolve`'s to stamp on, exactly as a weigh-in's unit is.
        assertEquals(0L, (action as CoachAction.LogSupplement).supplementId)
    }

    /** "I took my creatine" names no number, and one is what it means. Zero does not — a draft
     * whose Confirm button writes nothing is worse than no draft. */
    @Test
    fun `a missing dose count is one, and zero or seven fails the draft`() {
        assertEquals(1, (parseAction(TOOL_LOG_SUPPLEMENT, args("name" to "Creatine")) as CoachAction.LogSupplement).doses)
        assertNull(parseAction(TOOL_LOG_SUPPLEMENT, args("name" to "Creatine", "doses" to 0)))
        assertNull(parseAction(TOOL_LOG_SUPPLEMENT, args("name" to "Creatine", "doses" to MAX_ACTION_DOSES + 1)))
    }

    @Test
    fun `a blank or non-string supplement name fails the draft`() {
        assertNull(parseAction(TOOL_LOG_SUPPLEMENT, args("name" to "  ", "doses" to 1)))
        assertNull(parseAction(TOOL_LOG_SUPPLEMENT, args("name" to true, "doses" to 1)))
        assertNull(parseAction(TOOL_LOG_SUPPLEMENT, args("doses" to 1)))
    }

    /** The stored spelling, not the model's: the card, the logged line and the Supplements screen
     * all have to read the same. */
    @Test
    fun `a supplement resolves by exact name, case-insensitively`() {
        val action = supplementDose("  cREATINE ", 1, listOf(vitaminD, creatine))
        assertEquals(CoachAction.LogSupplement(name = "Creatine", doses = 1, supplementId = 2), action)
    }

    /** The fuzzy match is what kept this tool out, and it is still out: "vitamin" is not
     * *Vitamin D*, and ticking the nearest thing is what a card one tap from the log must not do. */
    @Test
    fun `a supplement they do not take fails rather than guessing`() {
        assertNull(supplementDose("vitamin", 1, listOf(vitaminD, creatine)))
        assertNull(supplementDose("Creatine", 1, emptyList()))
    }

    /**
     * `setTakenToday` takes the day's *new count*, so two doses of one supplement applied one after
     * the other would land as one — [glassesToAdd]'s lesson on a second table.
     */
    @Test
    fun `doses of one supplement are summed, not applied twice`() {
        val actions = listOf(
            CoachAction.LogSupplement(name = "Creatine", doses = 1, supplementId = 2),
            logFood("Toast", 180),
            CoachAction.LogSupplement(name = "Creatine", doses = 1, supplementId = 2),
            CoachAction.LogSupplement(name = "Vitamin D", doses = 1, supplementId = 1),
        )
        assertEquals(mapOf(2L to 2, 1L to 1), actions.supplementDoses())
        assertEquals(emptyMap<Long, Int>(), listOf(logFood("Toast", 180)).supplementDoses())
    }

    /** The figures are the user's own, item for item — nothing on the card was estimated by the
     * model, which is the whole reason this tool takes only a name. */
    @Test
    fun `a saved meal resolves to its own rows`() {
        val rows = savedMealRows("usual BREAKFAST", MealType.Breakfast, listOf(usualBreakfast), emptyList())
        assertEquals(listOf("Oats", "Banana"), rows?.map { it.name })
        assertEquals(listOf(300, 90), rows?.map { it.calories })
        assertTrue(rows.toString(), rows!!.all { it.mealType == MealType.Breakfast })
    }

    /** One row at one serving, named after the recipe — how the app logs a recipe everywhere
     * else. */
    @Test
    fun `a recipe resolves to a single serving`() {
        val row = savedMealRows("Chilli", MealType.Dinner, emptyList(), listOf(chilli))?.single()
        assertEquals("Chilli", row?.name)
        assertEquals(200, row?.calories)
        assertEquals(1.0, row?.portionAmount ?: 0.0, 0.001)
    }

    /**
     * `get_library` hands the model the names verbatim, so a name matching nothing is a broken
     * call — never a near miss to guess at. The turn fails instead of putting a meal the user did
     * not name one tap from the diary.
     */
    @Test
    fun `a name in no library fails rather than guessing`() {
        assertNull(savedMealRows("Usual brekkie", MealType.Breakfast, listOf(usualBreakfast), listOf(chilli)))
    }

    /** The name is all the model supplies; the meal slot is the only other field, and neither is a
     * figure. */
    @Test
    fun `a saved-meal call parses to a name and a slot`() {
        val action = parseAction(
            TOOL_LOG_SAVED_MEAL,
            args("name" to "Usual breakfast", "meal" to "breakfast"),
        ) as CoachAction.LogSavedMeal
        assertEquals("Usual breakfast", action.name)
        assertEquals(MealType.Breakfast, action.mealType)
        assertNull(parseAction(TOOL_LOG_SAVED_MEAL, args("name" to "Usual breakfast")))
        assertNull(parseAction(TOOL_LOG_SAVED_MEAL, args("meal" to "Breakfast")))
    }

    // endregion

    // region What a settled draft writes

    /**
     * The rule the whole multi-row card rests on: the foods go down together, in the order they
     * were drafted, as the diary's own rows.
     */
    @Test
    fun `a settled draft becomes one batch of diary rows`() {
        val entries = listOf(
            logFood("Scrambled eggs", 220),
            CoachAction.LogWater(glasses = 2),
            logFood("Toast", 180),
        ).foodEntries()
        assertEquals(listOf("Scrambled eggs", "Toast"), entries.map { it.name })
        assertEquals(listOf(220, 180), entries.map { it.calories })
        assertEquals(MealType.Breakfast, entries.first().mealType)
    }

    /**
     * `setToday` takes the day's *new total*, so two water rows applied one after the other would
     * have the second overwrite the first — a draft of two glasses would land as one.
     */
    @Test
    fun `water in a draft is summed, not applied twice`() {
        val actions = listOf(
            CoachAction.LogWater(glasses = 2),
            logFood("Toast", 180),
            CoachAction.LogWater(glasses = 1),
        )
        assertEquals(3, actions.glassesToAdd())
        assertEquals(0, listOf(logFood("Toast", 180)).glassesToAdd())
    }

    private fun logFood(name: String, kcal: Int) = CoachAction.LogFood(
        name = name,
        mealType = MealType.Breakfast,
        calories = kcal,
        proteinG = 10,
        carbsG = 20,
        fatG = 5,
        portionAmount = 1.0,
        portionUnit = "serving",
    )

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

    /** Steps ride the same day tool, and they carry the goal: "8,432" is a number and
     * "8,432 of 10,000" is an answer. */
    @Test
    fun `a day carries its steps against the goal`() {
        val text = formatDay(
            label = "Today",
            foods = emptyList(),
            targetCalories = null,
            waterGlasses = 0,
            exercise = emptyList(),
            steps = 8432,
            stepGoal = 10_000,
        )
        assertTrue(text, "Steps: 8,432 of 10,000" in text)
    }

    /** No profile, so no goal — the count still stands on its own rather than failing the read. */
    @Test
    fun `steps with no goal report the count alone`() {
        val text = formatDay(
            label = "Today",
            foods = emptyList(),
            targetCalories = null,
            waterGlasses = 0,
            exercise = emptyList(),
            steps = 8432,
        )
        assertTrue(text, "Steps: 8,432" in text)
        assertTrue(text, " of " !in text)
    }

    /**
     * Absent means *untracked*, and the whole point of omitting the line is that the coach cannot
     * then nag about a watch the user does not own. A zero-filled "No sleep recorded" every day
     * would do exactly that.
     */
    /** One line for the whole checklist, the call a day's training already makes — and each
     * against that day's own `dueTimes`, so a supplement since dropped to once still reads "1 of
     * 3" on a day it was due three times. */
    @Test
    fun `a day carries its supplements against what was due that day`() {
        val text = formatDay(
            label = "Today",
            foods = emptyList(),
            targetCalories = null,
            waterGlasses = 0,
            exercise = emptyList(),
            supplements = listOf(
                "Creatine" to SupplementDay(dateEpochDay = 20_000L, supplementId = 2, taken = 1, dueTimes = 3),
                "Vitamin D" to SupplementDay(dateEpochDay = 20_000L, supplementId = 1, taken = 0, dueTimes = 1),
            ),
        )
        assertTrue(text, "Supplements: Creatine 1 of 3, Vitamin D 0 of 1" in text)
    }

    @Test
    fun `an untracked domain leaves no line at all`() {
        val text = formatDay("Today", emptyList(), null, waterGlasses = 0, exercise = emptyList())
        listOf("Steps", "Slept", "Felt", "Fasted", "Supplements").forEach {
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
        assertTrue(text, "- Today: 900 kcal, 60g protein, 0 glasses, weighed in (-0.4 kg since the last)" in text)
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
        assertTrue(text, "- Today: 1800 kcal, 120g protein, 0 glasses, 50 min activity, 360 kcal burned" in text)
        assertTrue(text, "- Yesterday: 2100 kcal, 140g protein, 0 glasses, slept 6h 40m" in text)
    }

    /**
     * Water rides every day of a span, a zero included — it is the dense group, the one thing here
     * the user does *in this app*, so a missing row is a day they drank nothing rather than a
     * domain they do not track. A model averaging a week over the days that happen to carry a
     * line is the failure this prevents.
     */
    @Test
    fun `a span carries water on every day, zeroes included`() {
        val today = 20_000L
        val text = formatHistory(
            days = 2,
            nutrition = emptyList(),
            weights = emptyList(),
            today = today,
            water = listOf(WaterDay(dateEpochDay = today, glasses = 6)),
        )
        assertTrue(text, "- Today: nothing logged, 6 glasses" in text)
        assertTrue(text, "- Yesterday: nothing logged, 0 glasses" in text)
    }

    /** A span of water alone is a span with something in it — the early return has to know that,
     * or "how much have I drunk this week?" answers "nothing logged". */
    @Test
    fun `water alone is not an empty span`() {
        val today = 20_000L
        val text = formatHistory(
            days = 1,
            nutrition = emptyList(),
            weights = emptyList(),
            today = today,
            water = listOf(WaterDay(dateEpochDay = today, glasses = 3)),
        )
        assertTrue(text, "3 glasses" in text)
    }

    /** Summed per day rather than listed: a span answers "have I kept up with them?", and the
     * denominator is that day's own `dueTimes` — the snapshot, never the current setting. */
    @Test
    fun `a span carries what was taken against what was due`() {
        val today = 20_000L
        val text = formatHistory(
            days = 2,
            nutrition = emptyList(),
            weights = emptyList(),
            today = today,
            supplements = listOf(
                SupplementDay(dateEpochDay = today, supplementId = 1, taken = 1, dueTimes = 1),
                SupplementDay(dateEpochDay = today, supplementId = 2, taken = 1, dueTimes = 2),
                SupplementDay(dateEpochDay = today - 1, supplementId = 2, taken = 3, dueTimes = 3),
            ),
        )
        assertTrue(text, "- Today: nothing logged, 0 glasses, supplements 2 of 3" in text)
        assertTrue(text, "- Yesterday: nothing logged, 0 glasses, supplements 3 of 3" in text)
    }

    /** Walking that never reached a workout. It sits on the day's line beside the training it is
     * not, because a 14,000-step day with no logged session used to read as a rest day. */
    @Test
    fun `a span carries the day's steps`() {
        val today = 20_000L
        val text = formatHistory(
            days = 1,
            nutrition = emptyList(),
            weights = emptyList(),
            today = today,
            steps = listOf(StepDay(dateEpochDay = today, steps = 14_204, burnedKcal = 480)),
        )
        assertTrue(text, "- Today: nothing logged, 0 glasses, 14,204 steps" in text)
    }

    /**
     * A tape measure is the same class of figure as a weigh-in, and gets the same rule: the change
     * leaves the device, the reading never does. This is that rule's own test — the twin of
     * `a weigh-in never sends an absolute weight`.
     */
    @Test
    fun `a measurement never sends an absolute figure`() {
        val today = 20_000L
        val text = formatHistory(
            days = 3,
            nutrition = emptyList(),
            weights = emptyList(),
            today = today,
            measurements = mapOf(
                MeasurementPart.Waist to listOf(
                    MeasurementEntry(MeasurementPart.Waist, today - 9, 86.0),
                    MeasurementEntry(MeasurementPart.Waist, today, 84.0),
                ),
            ),
        )
        listOf("86", "84").forEach { assertTrue("$it leaked into: $text", it !in text) }
        // A reading older than the window is still what the one inside it compares against.
        assertTrue(text, "measured waist (-2.0 cm since the last)" in text)
    }

    /** Two parts measured in one sitting are two clauses, not one overwriting the other — and a
     * body fat is a percentage, which is the only thing [MeasurementPart.percent] changes here. */
    @Test
    fun `two parts measured on one day both reach the line`() {
        val today = 20_000L
        val text = formatHistory(
            days = 1,
            nutrition = emptyList(),
            weights = emptyList(),
            today = today,
            measurements = mapOf(
                MeasurementPart.Waist to listOf(
                    MeasurementEntry(MeasurementPart.Waist, today - 7, 86.0),
                    MeasurementEntry(MeasurementPart.Waist, today, 84.5),
                ),
                MeasurementPart.BodyFat to listOf(
                    MeasurementEntry(MeasurementPart.BodyFat, today - 7, 22.0),
                    MeasurementEntry(MeasurementPart.BodyFat, today, 20.9),
                ),
            ),
        )
        assertTrue(text, "measured waist (-1.5 cm since the last)" in text)
        assertTrue(text, "measured body fat (-1.1 % since the last)" in text)
    }

    /** The first reading of a part has nothing behind it, exactly as the first weigh-in does — and
     * saying so beats a delta invented against zero. */
    @Test
    fun `the first measurement of a part says it has nothing to compare against`() {
        val today = 20_000L
        val text = formatHistory(
            days = 1,
            nutrition = emptyList(),
            weights = emptyList(),
            today = today,
            measurements = mapOf(
                MeasurementPart.Arms to listOf(MeasurementEntry(MeasurementPart.Arms, today, 38.0)),
            ),
        )
        assertTrue(text, "measured arms (first one, nothing to compare against)" in text)
        assertTrue(text, "38" !in text)
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
        assertTrue(text, "- Today: nothing logged, 0 glasses, 45 min activity, 150 kcal burned" in text)
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
