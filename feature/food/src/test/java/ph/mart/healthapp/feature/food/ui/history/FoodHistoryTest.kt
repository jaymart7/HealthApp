package ph.mart.healthapp.feature.food.ui.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import java.util.Calendar
import org.junit.Test
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.feature.food.ui.shared.toFoodEntry

/**
 * The pieces of the history search that aren't Room or Compose. Get the fold wrong and the list
 * either re-sorts itself — putting a March row above an August one because a map decided so — or
 * opens a second heading for a day that already has one.
 *
 * The two label functions are here for the reason `diaryDateLabel` has a test: a label that stays
 * in Kotlin rather than moving to `strings.xml` is exempt because a test holds its wording, and
 * this is that test.
 */
class FoodHistoryTest {

    private fun entry(name: String, day: Long) = FoodEntry(
        name = name,
        dateEpochDay = day,
        mealType = MealType.Lunch,
        portionAmount = 1.0,
        portionUnit = "serving",
        calories = 200,
        proteinG = 10,
        carbsG = 20,
        fatG = 5,
    )


    /**
     * An epoch day for a calendar date, built the way the app builds one: local midnight over a day
     * in millis. The band tests need real months, which arithmetic on a bare number can't give.
     */
    private fun epochDayOf(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        set(year, month - 1, day, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis / 86_400_000L

    @Test
    fun `rows on the same day share one group, in the order they arrived`() {
        val grouped = listOf(entry("a", 20_000), entry("b", 20_000)).groupedByDay()

        assertEquals(1, grouped.size)
        assertEquals(20_000L, grouped[0].first)
        assertEquals(listOf("a", "b"), grouped[0].second.map { it.name })
    }

    @Test
    fun `days keep the newest-first order Room returned them in`() {
        val grouped = listOf(
            entry("today", 20_000),
            entry("yesterday", 19_999),
            entry("also yesterday", 19_999),
            entry("last month", 19_970),
        ).groupedByDay()

        assertEquals(listOf(20_000L, 19_999L, 19_970L), grouped.map { it.first })
        assertEquals(2, grouped[1].second.size)
    }

    /** A day that comes back, comes back as its own group — the fold only ever merges *adjacent*
     * rows, so it can never reorder what the query ordered. */
    @Test
    fun `a day that reappears out of order opens a second group rather than jumping back`() {
        val grouped = listOf(entry("a", 20_000), entry("b", 19_999), entry("c", 20_000)).groupedByDay()

        assertEquals(listOf(20_000L, 19_999L, 20_000L), grouped.map { it.first })
    }

    @Test
    fun `no results is no groups`() {
        assertTrue(emptyList<FoodEntry>().groupedByDay().isEmpty())
    }

    /**
     * What the review screen is seeded with, and what it writes: a copy on the day the diary was
     * showing, keeping the meal the food was eaten in and nothing that would tie it to the row it
     * came from. The plate is the one that matters — two rows pointing at one file break the
     * meal-photo prune.
     */
    @Test
    fun `a reviewed row writes a copy on the diary's day, without the source row's plate`() {
        val source = entry("Chicken curry", 19_994).copy(
            id = 7,
            mealType = MealType.Dinner,
            photoPath = "/photos/meal-7.jpg",
        )

        val written = source.toReviewForm().toFoodEntry(dateEpochDay = 20_000)

        assertEquals(0L, written.id)
        assertNull(written.photoPath)
        assertEquals(MealType.Dinner, written.mealType)
        assertEquals(20_000L, written.dateEpochDay)
        assertEquals("Chicken curry", written.name)
        assertEquals(200, written.calories)
    }

    // ---- The age labels ----

    @Test
    fun `the two days with names get them`() {
        assertEquals("Today", relativeAgeLabel(20_000, today = 20_000))
        assertEquals("Yesterday", relativeAgeLabel(19_999, today = 20_000))
    }

    @Test
    fun `days up to a fortnight are counted in days`() {
        assertEquals("2 days ago", relativeAgeLabel(19_998, today = 20_000))
        assertEquals("12 days ago", relativeAgeLabel(19_988, today = 20_000))
        assertEquals("13 days ago", relativeAgeLabel(19_987, today = 20_000))
    }

    /** Every tier starts at two of its unit, so no label ever reads "1 weeks" or "1 months" — the
     * singular a string with no plural form could not have produced. */
    @Test
    fun `weeks take over at a fortnight, months at two of them`() {
        assertEquals("2 weeks ago", relativeAgeLabel(19_986, today = 20_000))
        assertEquals("8 weeks ago", relativeAgeLabel(19_941, today = 20_000))
        assertEquals("2 months ago", relativeAgeLabel(19_940, today = 20_000))
        assertEquals("3 months ago", relativeAgeLabel(19_910, today = 20_000))
    }

    /** A future day can't be logged, but a device whose clock moves backwards can produce one, and
     * "-1 days ago" is worse than calling it today. */
    @Test
    fun `a day in the future reads as today rather than a negative count`() {
        assertEquals("Today", relativeAgeLabel(20_001, today = 20_000))
    }

    // ---- The bands ----

    @Test
    fun `the last seven days are one band`() {
        assertEquals("This week", ageBandFor(20_000, today = 20_000))
        assertEquals("This week", ageBandFor(19_994, today = 20_000))
    }

    /** The day after the week ends is still this month, so it lands in the middle band rather than
     * in a band named after the month it is already in. */
    @Test
    fun `the rest of the current month is the middle band`() {
        val today = epochDayOf(2026, 9, 20)

        assertEquals("Earlier this month", ageBandFor(epochDayOf(2026, 9, 5), today))
    }

    @Test
    fun `an older month is its own name`() {
        val today = epochDayOf(2026, 9, 20)

        assertEquals("August", ageBandFor(epochDayOf(2026, 8, 14), today))
    }

    /** Two Augusts in one list is the ambiguity the year exists to close. */
    @Test
    fun `a month in another year carries the year`() {
        val today = epochDayOf(2026, 9, 20)

        assertEquals("August 2025", ageBandFor(epochDayOf(2025, 8, 14), today))
    }

    // ---- The band fold ----

    @Test
    fun `days gather under the band they belong to, in the order they arrived`() {
        val today = epochDayOf(2026, 9, 20)
        val bands = listOf(
            entry("a", today),
            entry("b", today - 3),
            entry("c", epochDayOf(2026, 9, 2)),
            entry("d", epochDayOf(2026, 8, 14)),
        ).bandedGroups(today)

        assertEquals(listOf("This week", "Earlier this month", "August"), bands.map { it.label })
        assertEquals(2, bands[0].days.size)
        assertEquals(listOf("c"), bands[1].days.single().entries.map { it.name })
    }

    @Test
    fun `no results is no bands`() {
        assertTrue(emptyList<FoodEntry>().bandedGroups(today = 20_000).isEmpty())
    }
}
