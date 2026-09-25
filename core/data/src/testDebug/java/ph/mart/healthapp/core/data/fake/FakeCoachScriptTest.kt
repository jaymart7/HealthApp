package ph.mart.healthapp.core.data.fake

import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.coach.CoachAction
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.data.coach.MAX_DRAFT_ROWS
import ph.mart.healthapp.core.data.coach.TOOL_GET_DAY
import ph.mart.healthapp.core.data.coach.TOOL_GET_HISTORY
import ph.mart.healthapp.core.data.coach.TOOL_GET_LIBRARY
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.StrengthSet
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.profile.KG_PER_LB
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.MeasurementPart

/**
 * The debug fakes' routing.
 *
 * Debug-only scaffolding earns a test for one reason: it is the part with branches, and a fake that
 * silently stops reaching a state of the screen is worse than no fake at all — the state just never
 * gets looked at. Everything else in these files is either a delegation to the real repository or a
 * `delay`.
 *
 * It lives in `src/testDebug/` because that is the only source set that can see `src/debug/`.
 */
class FakeCoachScriptTest {

    @Test
    fun `saying fail is how the failure bubble is reached`() {
        assertEquals(FakeScript.Fail, fakeCoachScript("make this fail"))
    }

    @Test
    fun `asking to log water proposes one glass`() {
        val script = fakeCoachScript("log a glass of water") as FakeScript.Propose
        assertEquals(listOf(CoachAction.LogWater(glasses = 1)), script.actions)
    }

    /** The burn stays 0 here, the way `parseAction` leaves it — `priced()` is what fills it in,
     * and the fake goes through that same call so a debug card shows a real number. */
    @Test
    fun `asking to log a workout proposes it with its duration`() {
        val script = fakeCoachScript("log a 45 minute run") as FakeScript.Propose
        val action = script.actions.single() as CoachAction.LogExercise
        assertEquals(ExerciseType.Run, action.type)
        assertEquals(45, action.minutes)
        assertEquals(0, action.burnedKcal)
    }

    /** An hour is a duration too, and a sentence with no length at all still drafts something —
     * the card is corrected by dismissing it, not by refusing to draw it. */
    @Test
    fun `a workout's length falls back to half an hour`() {
        assertEquals(60, (fakeCoachScript("log a 1 hour swim") as FakeScript.Propose).minutes())
        assertEquals(30, (fakeCoachScript("log yoga") as FakeScript.Propose).minutes())
    }

    private fun FakeScript.Propose.minutes() =
        (actions.single() as CoachAction.LogExercise).minutes

    /** The proposal card is meant to be read before it is tapped, so the figures on it have to be
     * real ones — they come off `COMMON_FOODS`, not out of thin air. */
    @Test
    fun `asking to log a food proposes it with real macros`() {
        val script = fakeCoachScript("log two eggs for breakfast") as FakeScript.Propose
        val action = script.actions.single() as CoachAction.LogFood
        assertTrue(action.name, "egg" in action.name.lowercase())
        assertEquals(MealType.Breakfast, action.mealType)
        assertTrue("calories should be real: ${action.calories}", action.calories > 0)
        assertTrue("protein should be real: ${action.proteinG}", action.proteinG > 0)
    }

    /**
     * The sentence the multi-row card exists for. One meal slot for the batch, a row per food, and
     * the order they were said — a fake that could only draft one row would leave the card
     * unreachable in a debug build.
     */
    @Test
    fun `a sentence naming three foods drafts three rows`() {
        val script = fakeCoachScript("log eggs, rice and an apple for lunch") as FakeScript.Propose
        val foods = script.actions.filterIsInstance<CoachAction.LogFood>()
        assertEquals(3, foods.size)
        assertTrue(foods.toString(), foods.all { it.mealType == MealType.Lunch })
        assertEquals(foods.map { it.name }, foods.map { it.name }.distinct())
    }

    @Test
    fun `a meal is only named when the sentence names one`() {
        val script = fakeCoachScript("i ate some rice") as FakeScript.Propose
        assertEquals(MealType.Snacks, (script.actions.single() as CoachAction.LogFood).mealType)
    }

    /** Before the history words on purpose: "what have I saved recently?" is a library question,
     * and "recently" is one of theirs. */
    @Test
    fun `asking about saved meals reads the library`() {
        val script = fakeCoachScript("what meals have I saved recently?") as FakeScript.Tool
        assertEquals(TOOL_GET_LIBRARY, script.name)
    }

    @Test
    fun `asking about yesterday reads that day`() {
        val script = fakeCoachScript("what did I eat yesterday?") as FakeScript.Tool
        assertEquals(TOOL_GET_DAY, script.name)
        assertEquals(JsonPrimitive(1), script.args["days_ago"])
    }

    @Test
    fun `a numbered day is read too`() {
        val script = fakeCoachScript("how did 3 days ago go?") as FakeScript.Tool
        assertEquals(JsonPrimitive(3), script.args["days_ago"])
    }

    @Test
    fun `asking about a week or a month reads a span`() {
        val week = fakeCoachScript("how has my week gone?") as FakeScript.Tool
        assertEquals(TOOL_GET_HISTORY, week.name)
        assertEquals(JsonPrimitive(7), week.args["days"])

        val month = fakeCoachScript("what's my average this month?") as FakeScript.Tool
        assertEquals(JsonPrimitive(30), month.args["days"])
    }

    /**
     * The ordering rule, and the one a rewrite would get wrong: "log" is checked before
     * "yesterday", because a real model handed this sentence drafts a row rather than reading a
     * day. Getting it backwards makes logging unreachable for anyone who mentions when they ate.
     */
    @Test
    fun `logging beats reading when a sentence does both`() {
        val script = fakeCoachScript("log the eggs I had yesterday")
        assertTrue("expected a proposal, got $script", script is FakeScript.Propose)
    }

    /** A number in a food sentence must not read as a day offset. */
    @Test
    fun `a quantity is not a day offset`() {
        val script = fakeCoachScript("add 2 eggs")
        assertTrue("expected a proposal, got $script", script is FakeScript.Propose)
    }

    @Test
    fun `anything else falls through to a plain answer`() {
        assertTrue(fakeCoachScript("am I doing okay?") is FakeScript.Say)
    }

    /** With no profile there is no target, so the generic answer says so rather than improvising
     * one — the real prompt's rule for a null request. */
    @Test
    fun `the plain answer admits it has nothing when there is no profile`() {
        val say = fakeCoachScript("hello") as FakeScript.Say
        assertTrue(say.text(null), "profile" in say.text(null))
    }

    /**
     * The weight word is what makes a number a weigh-in, and it is checked before the exercise
     * match because `EXERCISE_WORDS` claims "weights" for a lifting session.
     */
    @Test
    fun `a sentence about a weight drafts a weigh-in`() {
        val script = fakeCoachScript("log my weight 82.4") as FakeScript.Propose
        assertEquals(82.4, (script.actions.single() as CoachAction.LogWeight).weight, 0.001)
        val spoken = fakeCoachScript("i weigh 181 today") as FakeScript.Propose
        assertEquals(181.0, (spoken.actions.single() as CoachAction.LogWeight).weight, 0.001)
    }

    @Test
    fun `a gym session is still an exercise, not a weigh-in`() {
        val script = fakeCoachScript("log a 40 minute gym session") as FakeScript.Propose
        assertTrue(script.actions.toString(), script.actions.single() is CoachAction.LogExercise)
    }

    /**
     * A saved meal is named by the user, so the name is whatever followed the library word — and
     * it is checked before the food match, or "log my usual Overnight oats" drafts the oats alone
     * at `COMMON_FOODS`' figures instead of the rows the user actually saved.
     */
    @Test
    fun `a named saved meal is drafted rather than read back`() {
        val script = fakeCoachScript("log my usual Overnight oats for breakfast") as FakeScript.Propose
        val action = script.actions.single() as CoachAction.LogSavedMeal
        assertEquals("overnight oats", action.name)
        assertEquals(MealType.Breakfast, action.mealType)
    }

    /** "Log the eggs I had yesterday" is a draft for yesterday, not a question about it — the
     * calendar words are read inside the log block, after it has already won. */
    @Test
    fun `a logging sentence naming a past day drafts for that day`() {
        val script = fakeCoachScript("log two eggs for breakfast yesterday") as FakeScript.Propose
        val action = script.actions.single() as CoachAction.LogFood
        assertEquals(todayEpochDay() - 1, action.dateEpochDay)
        assertEquals(0L, (fakeCoachScript("log two eggs") as FakeScript.Propose).let {
            (it.actions.single() as CoachAction.LogFood).dateEpochDay
        })
    }

    /**
     * A supplement is named by the user too, and for the saved meal's reason it is checked before
     * the food match — "log my magnesium" names nothing in `COMMON_FOODS` and would otherwise fall
     * through to a plain answer.
     */
    /** The words are the user's, in their own casing, and the verb is not part of them. */
    @Test
    fun `a note verb drafts the sentence after it`() {
        val script = fakeCoachScript("Note that today was Rough") as FakeScript.Propose
        assertEquals(
            CoachAction.LogNote(text = "today was Rough"),
            script.actions.single(),
        )
        val jotted = fakeCoachScript("jot down slept badly again") as FakeScript.Propose
        assertEquals("slept badly again", (jotted.actions.single() as CoachAction.LogNote).text)
        // A day the sentence names rides along, the rule a food draft already follows.
        val yesterday = fakeCoachScript("note that yesterday was better") as FakeScript.Propose
        assertEquals(
            todayEpochDay() - 1,
            (yesterday.actions.single() as CoachAction.LogNote).dateEpochDay,
        )
    }

    /** The bare word is a question about a note, not an instruction to write one. */
    @Test
    fun `asking about a note drafts nothing`() {
        assertTrue(fakeCoachScript("what's my note for today?") !is FakeScript.Propose)
    }

    @Test
    fun `a named supplement is drafted`() {
        val script = fakeCoachScript("log my creatine") as FakeScript.Propose
        assertEquals(
            CoachAction.LogSupplement(name = "creatine", doses = 1),
            script.actions.single(),
        )
        // Two words, because the match downstream is exact and "vitamin" is not "Vitamin D".
        val two = fakeCoachScript("took my vitamin d today") as FakeScript.Propose
        assertEquals("vitamin d", (two.actions.single() as CoachAction.LogSupplement).name)
    }

    /** The category word puts the name in front of it. A name they do not take resolves to null
     * downstream, which is how a debug build reaches the failed-draft ending here. */
    @Test
    fun `a supplement named before the category word is drafted too`() {
        val script = fakeCoachScript("took my Nothing At All supplement") as FakeScript.Propose
        assertEquals("nothing at all", (script.actions.single() as CoachAction.LogSupplement).name)
    }

    @Test
    fun `a mood sentence drafts the column it named`() {
        val mood = fakeCoachScript("log my mood as great") as FakeScript.Propose
        assertEquals(CoachAction.LogMood(mood = 5), mood.actions.single())
        val energy = fakeCoachScript("log my energy as low") as FakeScript.Propose
        assertEquals(CoachAction.LogMood(energy = 2), energy.actions.single())
    }

    /** "very low" has to be found before "low" finds itself inside it. */
    @Test
    fun `the five mood words are matched longest-phrase first`() {
        val script = fakeCoachScript("log how i felt today: very low") as FakeScript.Propose
        assertEquals(CoachAction.LogMood(mood = 1), script.actions.single())
    }

    /** A mood word with no level in the sentence is a question, not a draft. */
    @Test
    fun `asking about a mood is not a draft`() {
        assertTrue(fakeCoachScript("how has my mood been lately?") !is FakeScript.Propose)
    }

    /** Both ways anyone says a reading out loud. The fake does **not** order the pair — typing
     * them backwards is how a debug build reaches `parseAction`'s swapped-reading rejection. */
    @Test
    fun `a blood pressure sentence drafts both numbers`() {
        val spoken = fakeCoachScript("log my blood pressure 118 over 76") as FakeScript.Propose
        assertEquals(
            CoachAction.LogBloodPressure(systolic = 118, diastolic = 76),
            spoken.actions.single(),
        )
        val slashed = fakeCoachScript("log my bp 130/85") as FakeScript.Propose
        assertEquals(
            CoachAction.LogBloodPressure(systolic = 130, diastolic = 85),
            slashed.actions.single(),
        )
    }

    /** The figure has to sit *against* the site word, which is what tells a measurement from a gym
     * sentence — see the test below. */
    @Test
    fun `a measurement sentence drafts its site and figure`() {
        val script = fakeCoachScript("log my waist 82.5") as FakeScript.Propose
        val action = script.actions.single() as CoachAction.LogMeasurement
        assertEquals(MeasurementPart.Waist, action.part)
        assertEquals(82.5, action.value, 0.001)

        val fat = fakeCoachScript("log my body fat 18") as FakeScript.Propose
        assertEquals(
            MeasurementPart.BodyFat,
            (fat.actions.single() as CoachAction.LogMeasurement).part,
        )
    }

    /**
     * The measurement match runs before the exercise one, so a loose number anywhere in the
     * sentence would claim every "chest day" as a chest of 40cm. Adjacency is what stops it.
     */
    @Test
    fun `a chest day at the gym is an exercise, not a measurement`() {
        val script = fakeCoachScript("log a 40 minute gym session, chest day") as FakeScript.Propose
        assertTrue(script.actions.toString(), script.actions.single() is CoachAction.LogExercise)
    }

    /** The other half of that rule: with no name after the library word there is nothing to draft,
     * so the question stays a question. */
    @Test
    fun `a library sentence with no name still reads the library`() {
        val script = fakeCoachScript("log my usual") as FakeScript.Tool
        assertEquals(TOOL_GET_LIBRARY, script.name)
    }

    /** The second magic word. A card with no prose above it is the one ending where a dismissal
     * has no answer to persist, and nothing else in a debug build reaches it. */
    @Test
    fun `saying quietly drafts with no prose`() {
        val quiet = fakeCoachScript("quietly log a glass of water") as FakeScript.Propose
        assertEquals("", quiet.preamble)
        val spoken = fakeCoachScript("log a glass of water") as FakeScript.Propose
        assertTrue(spoken.preamble, spoken.preamble.isNotEmpty())
    }

    /** Uncapped on purpose: the real loop rejects a draft past the ceiling rather than truncating
     * it, and a fake that capped here would answer with ten quiet rows instead. */
    @Test
    fun `a sentence naming too many foods drafts past the ceiling`() {
        val script = fakeCoachScript(
            "log egg rice bacon salmon chicken bread milk cheese apple banana potato pasta",
        ) as FakeScript.Propose
        assertTrue("${script.actions.size} rows", script.actions.size > MAX_DRAFT_ROWS)
    }

    // The voice parse shares the same "match words against COMMON_FOODS" trick, and shares the
    // failure mode: too short a word matches half the table.

    @Test
    fun `a spoken meal finds its foods`() {
        val parsed = fakeParse("I had two eggs and some rice")
        assertTrue(parsed.toString(), parsed.any { "egg" in it.name.lowercase() })
        assertTrue(parsed.toString(), parsed.any { "rice" in it.name.lowercase() })
    }

    @Test
    fun `a sentence with no food in it finds nothing`() {
        assertTrue(fakeParse("um, I'm not really sure").isEmpty())
    }

    /** `loggable()` caps the list, and the fake goes through it rather than around it. */
    @Test
    fun `the parse never returns more than the cap`() {
        val many = fakeParse(
            "egg rice bacon salmon chicken bread milk cheese apple banana potato pasta yogurt",
        )
        assertTrue("${many.size} foods", many.size <= 8)
    }

    /** The debug build's describe-your-sets path: counts expand, a said unit wins, bodyweight is 0. */
    @Test
    fun `the fake strength parse reads a session`() {
        val sets = fakeStrengthParse("Bench 3x8 at 60, squats 5x5 and pull-ups 2x10, curls 1x12 @ 30 lb", UnitSystem.Metric)
        assertEquals(List(3) { StrengthSet("Bench", 8, 60.0) }, sets.take(3))
        assertEquals(List(5) { StrengthSet("squats", 5, 0.0) }, sets.subList(3, 8))
        assertEquals(List(2) { StrengthSet("pull-ups", 10, 0.0) }, sets.subList(8, 10))
        assertEquals(30 * KG_PER_LB, sets.last().weightKg, 1e-9)
        assertEquals(11, sets.size)
        assertTrue(fakeStrengthParse("two eggs and toast", UnitSystem.Metric).isEmpty())
    }
}
