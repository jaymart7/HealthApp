package ph.mart.healthapp.core.data.insight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ph.mart.healthapp.core.data.food.DiaryTotals
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.WeightTrendDisplay

private val TARGETS = DailyTargets(calories = 2000, proteinG = 150, carbsG = 200, fatG = 67, floor = 1500)

/** The model's answer is the one string in this app that comes from outside and is rendered
 * verbatim, so every way it can be wrong is checked here. */
class InsightTest {

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
            "You're 200 kcal over today's target.",
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
}
