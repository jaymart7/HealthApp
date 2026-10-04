package ph.mart.healthapp.core.data.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ph.mart.healthapp.core.data.R
import ph.mart.healthapp.core.data.phrase

/**
 * Which phrase each credit reaches, and that every line carries both the figure and the food. The
 * wording itself is `data_earned_*`.
 */
class EarnedCaloriesTest {

    private val sandwich = phrase(R.string.data_earned_food_sandwich)

    @Test
    fun `each threshold takes the phrase it reaches, not the one below`() {
        assertEquals(phrase(R.string.data_earned_food_burger), earnedFood(700))
        assertEquals(phrase(R.string.data_earned_food_chicken), earnedFood(699))
        assertEquals(phrase(R.string.data_earned_food_chicken), earnedFood(450))
        assertEquals(sandwich, earnedFood(449))
        assertEquals(phrase(R.string.data_earned_food_yoghurt), earnedFood(299))
        assertEquals(phrase(R.string.data_earned_food_banana), earnedFood(199))
        assertEquals(phrase(R.string.data_earned_food_apple), earnedFood(119))
        assertEquals(phrase(R.string.data_earned_food_apple), earnedFood(EARNED_MIN_KCAL))
    }

    /** The floor is the same one every caller gates on, so below it there is nothing to picture. */
    @Test
    fun `under the floor there is no phrase`() {
        assertNull(earnedFood(EARNED_MIN_KCAL - 1))
        assertNull(earnedFood(0))
        // A negative can't arrive from `estimateBurnedKcal`, which coerces at zero — checked so a
        // future caller subtracting two figures can't produce a sentence out of one.
        assertNull(earnedFood(-200))
    }

    @Test
    fun `the three lines name the figure and the food`() {
        assertEquals(phrase(R.string.data_earned_saved_about, 320, sandwich), earnedSavedLine(320))
        assertEquals(phrase(R.string.data_earned_ring_about, 320, sandwich), earnedRingLine(320))
        assertEquals(phrase(R.string.data_earned_insight_about, 320, sandwich), earnedInsightLine(320))
    }

    /**
     * Every caller gates on [EARNED_MIN_KCAL] first, so these are defensive — but each line is a
     * total function rather than one that reads "about null" if a caller ever forgets.
     */
    @Test
    fun `a line with no phrase to offer still reads as a sentence`() {
        assertEquals(phrase(R.string.data_earned_saved, 10), earnedSavedLine(10))
        assertEquals(phrase(R.string.data_earned_ring, 10), earnedRingLine(10))
        assertEquals(phrase(R.string.data_earned_insight, 10), earnedInsightLine(10))
    }
}
