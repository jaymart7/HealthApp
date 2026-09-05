package ph.mart.healthapp.feature.home.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.designsystem.component.HomeCard

class HomeRowsTest {

    @Test
    fun `two adjacent halves pair and a full takes its own row`() {
        val rows = homeRows(
            listOf(HomeCard.Calories, HomeCard.Streak, HomeCard.Weight, HomeCard.Water),
            fastRunning = false,
        )
        assertEquals(
            listOf(
                listOf(HomeCard.Calories),
                listOf(HomeCard.Streak, HomeCard.Weight),
                listOf(HomeCard.Water),
            ),
            rows,
        )
    }

    @Test
    fun `an unpaired half falls back to a full-width row`() {
        val rows = homeRows(
            listOf(HomeCard.Streak, HomeCard.Weight, HomeCard.Heart),
            fastRunning = false,
        )
        assertEquals(
            listOf(listOf(HomeCard.Streak, HomeCard.Weight), listOf(HomeCard.Heart)),
            rows,
        )
    }

    @Test
    fun `a running fast is full width and an idle one pairs`() {
        val order = listOf(HomeCard.Fasting, HomeCard.Steps)
        assertEquals(listOf(listOf(HomeCard.Fasting, HomeCard.Steps)), homeRows(order, fastRunning = false))
        assertEquals(
            listOf(listOf(HomeCard.Fasting), listOf(HomeCard.Steps)),
            homeRows(order, fastRunning = true),
        )
    }

    @Test
    fun `gated cards are removed before pairing, so the survivors re-pair`() {
        // The handoff's artboard C: no profile (Calories, Macros gone), no watch (Steps, Sleep,
        // Heart gone), cycle tracking off. What is left has to close up, not leave holes.
        val all = HomeCard.entries
        val gone = setOf(
            HomeCard.Calories, HomeCard.Macros, HomeCard.Steps, HomeCard.Sleep,
            HomeCard.Heart, HomeCard.Cycle, HomeCard.Workout, HomeCard.BloodPressure,
        )
        val rows = homeRows(all.filterNot { it in gone }, fastRunning = false)
        assertEquals(
            listOf(
                // Water, Mood and Supplements are full width; Streak and Weight close up around
                // the four gated cards that used to sit between them.
                listOf(HomeCard.Water),
                listOf(HomeCard.Streak, HomeCard.Weight),
                // Fasting is a half with a full-width neighbour, so it takes the whole row
                // rather than leaving a gap beside it. Same for the photo card at the end.
                listOf(HomeCard.Fasting),
                listOf(HomeCard.Mood),
                listOf(HomeCard.Supplements),
                listOf(HomeCard.ProgressPhoto),
            ),
            rows,
        )
        assertTrue(rows.all { it.size <= 2 })
        assertEquals(all.size - gone.size, rows.sumOf { it.size })
    }

    @Test
    fun `every card survives the walk exactly once, in order`() {
        listOf(true, false).forEach { running ->
            val rows = homeRows(HomeCard.entries, fastRunning = running)
            assertEquals(HomeCard.entries.toList(), rows.flatten())
        }
    }

    @Test
    fun `the strip takes the first three visible priority cards`() {
        assertEquals(
            listOf(HomeCard.Calories, HomeCard.Water, HomeCard.Steps),
            todayStripCards(HomeCard.entries),
        )
    }

    @Test
    fun `a hidden card takes its own cell with it`() {
        val visible = HomeCard.entries.filterNot { it == HomeCard.Calories || it == HomeCard.Steps }
        assertEquals(
            listOf(HomeCard.Water, HomeCard.Streak, HomeCard.Weight),
            todayStripCards(visible),
        )
    }

    @Test
    fun `one survivor is not a summary`() {
        assertTrue(todayStripCards(listOf(HomeCard.Water, HomeCard.Mood)).isEmpty())
        assertTrue(todayStripCards(emptyList()).isEmpty())
        assertEquals(
            listOf(HomeCard.Water, HomeCard.Weight),
            todayStripCards(listOf(HomeCard.Water, HomeCard.Weight, HomeCard.Mood)),
        )
    }
}
