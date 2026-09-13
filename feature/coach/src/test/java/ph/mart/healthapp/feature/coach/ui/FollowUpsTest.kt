package ph.mart.healthapp.feature.coach.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.insight.InsightRequest
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.feature.coach.R

/**
 * The follow-up chips are a pure rule over the day rather than a second model call, which is what
 * makes them free — and what makes them testable. The thing worth pinning is that the day's own
 * gaps come out first, because a filler-only row is the empty state's starters with extra steps.
 */
class FollowUpsTest {

    private val onTrack = InsightRequest(
        goal = Goal.Lose,
        caloriesConsumed = 2000,
        caloriesTarget = 2000,
        proteinG = 150,
        proteinTargetG = 150,
        carbsG = 200,
        carbsTargetG = 200,
        fatG = 60,
        fatTargetG = 60,
        waterGlasses = 8,
        waterGoalGlasses = 8,
        streakDays = 4,
        weightDeltaKg = null,
    )

    @Test
    fun `a protein gap and room left in the day lead the row`() {
        val followUps = followUpsFor(onTrack.copy(proteinG = 60, caloriesConsumed = 1200))
        assertEquals(R.string.coach_followup_protein, followUps.first())
        assertTrue(followUps.toString(), R.string.coach_starter_dinner in followUps)
    }

    /** Null is "nothing to compare against", not "no change" — a trend question with one reading
     * behind it buys a shrug. */
    @Test
    fun `a weigh-in with nothing behind it offers no trend question`() {
        assertFalse(R.string.coach_followup_weight in followUpsFor(onTrack))
        assertTrue(R.string.coach_followup_weight in followUpsFor(onTrack.copy(weightDeltaKg = -0.4)))
    }

    /** No profile means no targets, so no gap rule can fire — but the diary still reads back, and
     * a row that vanished on the day-one user would look broken. */
    @Test
    fun `a day with no targets still offers the diary`() {
        assertEquals(MAX_FOLLOW_UPS, followUpsFor(null).size)
    }

    @Test
    fun `the row is never longer than it fits and never repeats itself`() {
        val followUps = followUpsFor(
            onTrack.copy(proteinG = 0, caloriesConsumed = 0, waterGlasses = 0, weightDeltaKg = -0.4),
        )
        assertEquals(MAX_FOLLOW_UPS, followUps.size)
        assertEquals(followUps.distinct(), followUps)
    }
}
