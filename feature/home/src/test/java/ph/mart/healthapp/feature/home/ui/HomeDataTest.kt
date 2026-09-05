package ph.mart.healthapp.feature.home.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `an unloaded state is Loading, never day one`() {
        // The regression this guards: HomeUiState's all-zero default is indistinguishable from a
        // genuinely empty day, so a user with months of history would see the day-one empty state
        // fade away on every cold start.
        assertEquals(HomePhase.Loading, homePhase(HomeUiState()))
        assertEquals(
            HomePhase.Loading,
            homePhase(HomeUiState(waterGlasses = 5, foodEntryCount = 3)),
        )
    }

    @Test
    fun `a loaded state is day one only when nothing has been logged`() {
        assertEquals(HomePhase.DayOne, homePhase(HomeUiState(loaded = true)))
        assertEquals(
            HomePhase.Populated,
            homePhase(HomeUiState(loaded = true, waterGlasses = 1)),
        )
        assertEquals(
            HomePhase.Populated,
            homePhase(HomeUiState(loaded = true, foodEntryCount = 1)),
        )
        assertEquals(
            HomePhase.Populated,
            homePhase(
                HomeUiState(
                    loaded = true,
                    weightEntries = listOf(WeightEntry(dateEpochDay = 100, weightKg = 76.0)),
                ),
            ),
        )
        assertEquals(
            HomePhase.Populated,
            homePhase(HomeUiState(loaded = true, burnedKcal = 200)),
        )
        assertEquals(
            HomePhase.Populated,
            homePhase(HomeUiState(loaded = true, lastPhotoEpochDay = 100)),
        )
        // Mood is deliberately not a day-one signal, same as it isn't a streak domain.
        assertEquals(
            HomePhase.DayOne,
            homePhase(HomeUiState(loaded = true, moodLevel = 4, energyLevel = 3)),
        )
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
