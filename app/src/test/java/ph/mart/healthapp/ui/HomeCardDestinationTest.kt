package ph.mart.healthapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ph.mart.healthapp.core.designsystem.component.HomeCard
import ph.mart.healthapp.feature.progress.ui.ProgressSubjectRoutes
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.route

/**
 * The card-to-subject map. The `when` is exhaustive, so the compiler already guarantees every card
 * answers; what it cannot check is that each one answers *correctly*, which is the four pairs below
 * where the card's name and its subject's differ.
 */
class HomeCardDestinationTest {

    @Test
    fun `water is the only card with nowhere to go`() {
        assertNull(HomeCard.Water.subject())
        val landless = HomeCard.entries.filter { it.subject() == null }
        assertEquals(listOf(HomeCard.Water), landless)
    }

    @Test
    fun `the four cards whose subject is not their own name`() {
        assertEquals(Subject.Badges, HomeCard.Streak.subject())
        assertEquals(Subject.Activity, HomeCard.Steps.subject())
        assertEquals(Subject.Strength, HomeCard.Workout.subject())
        assertEquals(Subject.Photos, HomeCard.ProgressPhoto.subject())
    }

    @Test
    fun `both nutrition cards open the same page`() {
        assertEquals(Subject.Nutrition, HomeCard.Calories.subject())
        assertEquals(Subject.Nutrition, HomeCard.Macros.subject())
    }

    /** Every destination is a route the app can actually draw, which is what keeps the push honest. */
    @Test
    fun `every destination is a subject route`() {
        HomeCard.entries.mapNotNull { it.subject() }.forEach {
            assertEquals(true, it.route() in ProgressSubjectRoutes)
        }
    }
}
