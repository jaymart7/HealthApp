package ph.mart.healthapp.feature.training.ui.training

import org.junit.Assert.assertEquals
import org.junit.Test
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType

private const val TODAY = 20_600L

private fun entry(id: Long, day: Long) =
    ExerciseEntry(id = id, dateEpochDay = day, type = ExerciseType.Run, minutes = 30, burnedKcal = 300)

class TrainingDataTest {

    @Test
    fun `today's rows are dropped`() {
        val entries = listOf(entry(1, TODAY - 1), entry(2, TODAY), entry(3, TODAY))

        assertEquals(listOf(1L), entries.recentSessions(TODAY).map { it.id })
    }

    /** The repository hands these over oldest first, so "newest first" is this function's job —
     * including within a day, where the id is the only thing that orders two sessions. */
    @Test
    fun `newest first, ties broken by id`() {
        val entries = listOf(entry(1, TODAY - 3), entry(2, TODAY - 1), entry(3, TODAY - 1))

        assertEquals(listOf(3L, 2L, 1L), entries.recentSessions(TODAY).map { it.id })
    }

    @Test
    fun `the limit is respected`() {
        val entries = (1L..10L).map { entry(it, TODAY - it) }

        assertEquals(RECENT_SESSIONS, entries.recentSessions(TODAY).size)
        assertEquals(2, entries.recentSessions(TODAY, limit = 2).size)
    }

    @Test
    fun `an empty history is empty`() {
        assertEquals(emptyList<ExerciseEntry>(), emptyList<ExerciseEntry>().recentSessions(TODAY))
    }
}
