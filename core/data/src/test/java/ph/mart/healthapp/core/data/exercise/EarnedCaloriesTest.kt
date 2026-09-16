package ph.mart.healthapp.core.data.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The wording is the test — these four functions are exempt from the localization pass on exactly
 * this basis, the reading `goalProjectionLine()` and `insightFor()` already got. A phrase changed
 * without a reason to change it fails here.
 */
class EarnedCaloriesTest {

    @Test
    fun `each threshold takes the phrase it reaches, not the one below`() {
        assertEquals("a burger and fries", earnedFoodPhrase(700))
        assertEquals("a chicken breast with rice", earnedFoodPhrase(699))
        assertEquals("a chicken breast with rice", earnedFoodPhrase(450))
        assertEquals("a peanut-butter sandwich", earnedFoodPhrase(449))
        assertEquals("yoghurt and berries", earnedFoodPhrase(299))
        assertEquals("a banana", earnedFoodPhrase(199))
        assertEquals("an apple", earnedFoodPhrase(119))
        assertEquals("an apple", earnedFoodPhrase(EARNED_MIN_KCAL))
    }

    /** The floor is the same one every caller gates on, so below it there is nothing to picture. */
    @Test
    fun `under the floor there is no phrase`() {
        assertNull(earnedFoodPhrase(EARNED_MIN_KCAL - 1))
        assertNull(earnedFoodPhrase(0))
        // A negative can't arrive from `estimateBurnedKcal`, which coerces at zero — checked so a
        // future caller subtracting two figures can't produce a sentence out of one.
        assertNull(earnedFoodPhrase(-200))
    }

    @Test
    fun `the three lines name the figure and the food`() {
        assertEquals(
            "Nice work — +320 kcal on today's budget, about a peanut-butter sandwich.",
            earnedSavedLine(320),
        )
        assertEquals("+320 kcal earned — about a peanut-butter sandwich", earnedRingLine(320))
        assertEquals(
            "Today's activity bought you 320 kcal more than a rest day — about a peanut-butter sandwich.",
            earnedInsightLine(320),
        )
    }

    /**
     * Every caller gates on [EARNED_MIN_KCAL] first, so these are defensive — but each line is a
     * total function rather than one that reads "about null" if a caller ever forgets.
     */
    @Test
    fun `a line with no phrase to offer still reads as a sentence`() {
        assertEquals("Nice work — +10 kcal on today's budget.", earnedSavedLine(10))
        assertEquals("+10 kcal earned today", earnedRingLine(10))
        assertEquals("Today's activity bought you 10 kcal more than a rest day.", earnedInsightLine(10))
    }
}
