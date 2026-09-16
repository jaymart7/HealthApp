package ph.mart.healthapp.feature.progress.ui.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Which recap periods offer to ask the coach, on `SubjectCoachTest`'s rule and for the same
 * reason: `get_history` reads a span of at most a month, so a week and a month are windows the
 * coach can answer over and a year is one it would answer over a month of data without saying so.
 *
 * This test is what stops a fourth period — or a year question — quietly arriving with a window
 * no tool can read.
 */
class RecapCoachTest {

    @Test
    fun `a week and a month ask, a year does not`() {
        assertEquals(
            listOf(RecapPeriod.Week, RecapPeriod.Month),
            RecapPeriod.entries.filter { it.coachQuestion != null },
        )
    }

    /** Both of them, named — a period whose span the coach reads must carry a question. */
    @Test
    fun `every period inside the coach's reach carries one`() {
        RecapPeriod.entries.filter { it.days <= COACH_HISTORY_DAYS }
            .forEach { assertNotNull(it.name, it.coachQuestion) }
    }

    @Test
    fun `and every period outside it carries none`() {
        RecapPeriod.entries.filter { it.days > COACH_HISTORY_DAYS }
            .forEach { assertNull(it.name, it.coachQuestion) }
    }
}

/**
 * `MAX_HISTORY_DAYS`, restated because it is `internal` to `:core:data` and a feature test cannot
 * see it. If the coach's span moves, this fails and the enum is what needs the edit.
 */
private const val COACH_HISTORY_DAYS = 30
