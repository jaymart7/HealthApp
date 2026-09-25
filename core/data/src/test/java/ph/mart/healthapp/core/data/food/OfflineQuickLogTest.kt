package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.exercise.ExerciseType

class OfflineQuickLogTest {

    private fun parsed(text: String, myFoods: List<ScannedProduct> = emptyList()) =
        offlineQuickLog(text, myFoods) as QuickLogResult.Parsed

    @Test
    fun `plurals find their singular rows`() {
        val names = parsed("two eggs and a banana").foods.map { it.name }

        assertTrue(names.toString(), names.any { it.startsWith("Egg") })
        assertTrue(names.toString(), "Banana" in names)
    }

    /** Offline, every match is a guess, and the review tags it as one. */
    @Test
    fun `every offline food is a low-confidence guess`() {
        assertTrue(parsed("a banana").foods.all { it.confidence == RecognitionConfidence.Low })
    }

    @Test
    fun `the user's own food wins over the built-in table`() {
        val mine = ScannedProduct("Banana bread, homemade", 1.0, "slice", 240, 4, 38, 8)

        assertEquals(listOf("Banana bread, homemade"), parsed("banana", listOf(mine)).foods.map { it.name })
    }

    /** "Contains" alone read "ran" as Orange — the word-start rule is what stops it. */
    @Test
    fun `a word only matches the start of a word in the name`() {
        assertEquals(QuickLogResult.NothingFound, offlineQuickLog("i ran and swam"))
    }

    /** Plain water is water — "Tuna, canned in water" is what a word match made of it. */
    @Test
    fun `glasses of water are water, not food`() {
        val result = parsed("3 glasses of water")

        assertEquals(3, result.waterGlasses)
        assertTrue(result.foods.toString(), result.foods.isEmpty())
    }

    @Test
    fun `a glass is one`() {
        assertEquals(1, parsed("a glass of water").waterGlasses)
    }

    @Test
    fun `a stated weight is read, not taken for minutes`() {
        val result = parsed("weighed 80.5 and ran 20 min")

        assertEquals(80.5, result.weight!!, 0.001)
        assertEquals(20, result.activities.single().minutes)
    }

    @Test
    fun `hours become minutes`() {
        assertEquals(60, parsed("1 hour of yoga").activities.single().minutes)
    }

    /** Offline there is nothing to estimate a duration from, so none said is no activity. */
    @Test
    fun `an activity with no duration is no activity offline`() {
        assertEquals(QuickLogResult.NothingFound, offlineQuickLog("went for a run"))
    }

    @Test
    fun `the glasses count is not the run's duration`() {
        val result = parsed("3 glasses of water and a 25 minute run")

        assertEquals(ExerciseType.Run, result.activities.single().type)
        assertEquals(25, result.activities.single().minutes)
    }

    @Test
    fun `a named meal sets the slot`() {
        assertEquals(MealType.Lunch, parsed("banana for lunch").mealType)
        assertEquals(MealType.Snacks, parsed("a banana snack").mealType)
    }
}
