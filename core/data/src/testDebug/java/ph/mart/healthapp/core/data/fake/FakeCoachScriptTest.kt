package ph.mart.healthapp.core.data.fake

import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.coach.CoachAction
import ph.mart.healthapp.core.data.coach.TOOL_GET_DAY
import ph.mart.healthapp.core.data.coach.TOOL_GET_HISTORY
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.food.MealType

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
}
