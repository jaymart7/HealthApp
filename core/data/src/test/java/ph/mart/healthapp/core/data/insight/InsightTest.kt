package ph.mart.healthapp.core.data.insight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ph.mart.healthapp.core.data.exercise.EARNED_MIN_KCAL
import ph.mart.healthapp.core.data.food.DiaryTotals
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.WeightTrendDisplay

private val TARGETS = DailyTargets(calories = 2000, proteinG = 150, carbsG = 200, fatG = 67, floor = 1500)

/** The model's answer is the one string in this app that comes from outside and is rendered
 * verbatim, so every way it can be wrong is checked here. */
class InsightTest {

    /** The same leak the coach's reply has, on a card that is one line: `MarkdownTest` has the
     * rules, and the bullet goes entirely rather than becoming a list of one. */
    @Test
    fun `markdown is stripped, and a converted bullet with it`() {
        assertEquals(
            "You're 88 g short on protein today.",
            sanitizeInsight("* **You're 88 g short** on protein today."),
        )
    }

    @Test
    fun `a plain sentence passes through`() {
        assertEquals(
            "You're 320 kcal under your target — a solid dinner still fits.",
            sanitizeInsight("You're 320 kcal under your target — a solid dinner still fits."),
        )
    }

    @Test
    fun `surrounding quotes and stray whitespace are stripped`() {
        assertEquals("Protein is lagging today.", sanitizeInsight("  \"Protein is lagging today.\"\n"))
    }

    @Test
    fun `a wrapped answer collapses to one line`() {
        assertEquals(
            "Nine days logged in a row — keep it going.",
            sanitizeInsight("Nine days logged\n  in a row — keep it going."),
        )
    }

    /** The prompt's own escape hatch: nothing worth saying must not displace the rule-based line. */
    @Test
    fun `NONE is no insight`() {
        assertNull(sanitizeInsight("NONE"))
        assertNull(sanitizeInsight("none."))
    }

    @Test
    fun `nothing at all is no insight`() {
        assertNull(sanitizeInsight(null))
        assertNull(sanitizeInsight("   \n "))
    }

    /** A model that wrote a paragraph is rejected rather than truncated — half a sentence reads
     * as a bug, and the line it would displace is always complete. */
    @Test
    fun `an answer past the cap is rejected`() {
        assertNull(sanitizeInsight("a".repeat(MAX_INSIGHT_CHARS + 1)))
        assertEquals("b".repeat(MAX_INSIGHT_CHARS), sanitizeInsight("b".repeat(MAX_INSIGHT_CHARS)))
    }

    @Test
    fun `insight prefers the calorie overage, then protein, then weight`() {
        val flatTrend = WeightTrendDisplay(currentKg = 76.0, deltaKg = 0.0, hasPrior = false)
        assertEquals(
            "You're 200 kcal over today's budget.",
            insightFor(DiaryTotals(2200, 150, 200, 67), TARGETS, flatTrend),
        )
        assertEquals(
            "You're 70g short on protein today.",
            insightFor(DiaryTotals(1000, 80, 100, 30), TARGETS, flatTrend),
        )
        // Exactly 60% of the protein goal is not "short" — the rule is strictly below.
        assertNull(insightFor(DiaryTotals(1000, 90, 100, 30), TARGETS, flatTrend))
        assertEquals(
            "-0.6 kg over the last week — keep it steady.",
            insightFor(
                DiaryTotals(1000, 140, 100, 30),
                TARGETS,
                WeightTrendDisplay(currentKg = 76.0, deltaKg = -0.6, hasPrior = true),
            ),
        )
    }

    @Test
    fun `insight is null when nothing is notable`() {
        assertNull(
            insightFor(
                DiaryTotals(1000, 140, 100, 30),
                TARGETS,
                WeightTrendDisplay(currentKg = 76.0, deltaKg = -0.1, hasPrior = true),
            ),
        )
    }

    @Test
    fun `empty diary does not trigger the protein insight`() {
        val trend = WeightTrendDisplay(currentKg = 76.0, deltaKg = 0.0, hasPrior = false)
        assertNull(insightFor(DiaryTotals(0, 0, 0, 0), TARGETS, trend))
    }

    /** The whole point of the credit: the day is only "over" once it passes the budget the
     * workout actually bought, which is the figure the calorie ring drew. */
    @Test
    fun `the overage counts the day's burn, and says so only once past it`() {
        val flatTrend = WeightTrendDisplay(currentKg = 76.0, deltaKg = 0.0, hasPrior = false)
        assertEquals(
            "You're 100 kcal over today's budget.",
            insightFor(DiaryTotals(2500, 150, 200, 67), TARGETS, flatTrend, burnedKcal = 400),
        )
        // 2,300 against a 2,000 target is over; against the 2,400 the run bought, it is not.
        assertEquals(
            "Today's activity bought you 400 kcal more than a rest day — about a peanut-butter sandwich.",
            insightFor(DiaryTotals(2300, 150, 200, 67), TARGETS, flatTrend, burnedKcal = 400),
        )
    }

    /** Above protein on purpose — a day with real burn is the day this line exists for. */
    @Test
    fun `the workout line outranks a protein shortfall`() {
        val flatTrend = WeightTrendDisplay(currentKg = 76.0, deltaKg = 0.0, hasPrior = false)
        val short = DiaryTotals(1000, 80, 100, 30)
        assertEquals("You're 70g short on protein today.", insightFor(short, TARGETS, flatTrend))
        assertEquals(
            "Today's activity bought you 220 kcal more than a rest day — about yoghurt and berries.",
            insightFor(short, TARGETS, flatTrend, burnedKcal = 220),
        )
    }

    /**
     * Two silences that matter more than any sentence: a credit under the floor is not worth a
     * line, and a caller passing 0 — which is what `addExerciseToBudget` being off looks like from
     * here — must fall straight through to the rules that shipped before this one.
     */
    @Test
    fun `a burn under the floor, or never credited, says nothing about a workout`() {
        val flatTrend = WeightTrendDisplay(currentKg = 76.0, deltaKg = 0.0, hasPrior = false)
        val quiet = DiaryTotals(1000, 140, 100, 30)
        assertNull(insightFor(quiet, TARGETS, flatTrend, burnedKcal = EARNED_MIN_KCAL - 1))
        assertNull(insightFor(quiet, TARGETS, flatTrend, burnedKcal = 0))
    }
}
