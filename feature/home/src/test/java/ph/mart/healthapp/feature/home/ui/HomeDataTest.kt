package ph.mart.healthapp.feature.home.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.progress.WeightEntry

class HomeDataTest {

    @Test
    fun `days since photo is null with no photos and never negative`() {
        assertNull(daysSincePhoto(null, todayEpochDay = 100))
        assertEquals(12L, daysSincePhoto(88, todayEpochDay = 100))
        assertEquals(0L, daysSincePhoto(105, todayEpochDay = 100))
    }

    @Test
    fun `a day is day one until something real is logged`() {
        // Home itself draws from the first launch; this is the AI insight's gate — the model is
        // not asked about a day with nothing in it.
        assertTrue(HomeUiState().isDayOne)
        assertFalse(HomeUiState(waterGlasses = 1).isDayOne)
        assertFalse(HomeUiState(foodEntryCount = 1).isDayOne)
        assertFalse(
            HomeUiState(
                weightEntries = listOf(WeightEntry(dateEpochDay = 100, weightKg = 76.0)),
            ).isDayOne,
        )
        assertFalse(HomeUiState(burnedKcal = 200).isDayOne)
        assertFalse(HomeUiState(lastPhotoEpochDay = 100).isDayOne)
        // Mood is deliberately not a day-one signal, same as it isn't a streak domain.
        assertTrue(HomeUiState(moodLevel = 4, energyLevel = 3).isDayOne)
    }

    @Test
    fun `greeting matches the prototype copy for each part of the day`() {
        assertEquals("Good morning", greetingFor(8))
        assertEquals("Good afternoon", greetingFor(12))
        assertEquals("Good evening", greetingFor(18))
    }

    @Test
    fun `the sub-line splits on the same hours the greeting does`() {
        assertEquals("Ready for breakfast?", greetingSubFor(8))
        assertEquals("How's the day going?", greetingSubFor(12))
        assertEquals("Almost there for today.", greetingSubFor(18))
        // The boundaries themselves, since two functions now have to agree on them.
        assertEquals(greetingFor(11), greetingFor(0))
        assertEquals(greetingSubFor(17), greetingSubFor(12))
        assertEquals(greetingSubFor(23), greetingSubFor(18))
    }
}
