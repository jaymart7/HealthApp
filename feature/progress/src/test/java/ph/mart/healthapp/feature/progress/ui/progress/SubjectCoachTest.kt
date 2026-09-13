package ph.mart.healthapp.feature.progress.ui.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Which subject pages offer to ask the coach.
 *
 * The list is the whole point and it is a *closed* one: the coach's tools reach food and macros,
 * water, training, sleep, mood, fasting and the weight trend, so those seven pages carry the
 * action and the other seven carry nothing. A button on Heart or Measurements would buy a shrug,
 * and a shrug reads as a broken feature — the rule the coach's own follow-up chips follow.
 *
 * This test is what stops a new subject quietly arriving with a question the coach cannot answer:
 * adding one here means adding the tool that answers it.
 */
class SubjectCoachTest {

    @Test
    fun `only the subjects the coach has tools for offer to ask it`() {
        val offered = Subject.entries.filter { it.coachQuestion != null }
        assertEquals(
            listOf(
                Subject.Weight,
                Subject.Nutrition,
                Subject.Fasting,
                Subject.Activity,
                Subject.Strength,
                Subject.Sleep,
                Subject.Mood,
            ),
            offered,
        )
    }

    /** The four read-only surfaces and the body pages the coach cannot see: no action, no shrug. */
    @Test
    fun `the subjects it cannot answer carry nothing`() {
        listOf(
            Subject.Photos,
            Subject.Measurements,
            Subject.Supplements,
            Subject.Cycle,
            Subject.Heart,
            Subject.BloodPressure,
            Subject.Badges,
        ).forEach { assertNull(it.name, it.coachQuestion) }
    }
}
