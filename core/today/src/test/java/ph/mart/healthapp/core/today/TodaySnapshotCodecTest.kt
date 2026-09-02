package ph.mart.healthapp.core.today

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The wire format, which is the only part of this module with a way to go wrong. The two APKs
 * ship independently, so the version-skew cases below are the real ones — not the round trip.
 */
class TodaySnapshotCodecTest {

    private val snapshot = TodaySnapshot(
        dateEpochDay = 20_000,
        consumedKcal = 1450,
        budgetKcal = 2000,
        glasses = 5,
        goalGlasses = 8,
        waterLabel = "1.3 L",
        streakDays = 12,
        steps = 8432,
        fastingUntilMillis = 1_700_000_000_000L,
        darkThemeOn = true,
    )

    @Test
    fun `a snapshot survives the round trip intact`() {
        assertEquals(snapshot, decodeSnapshot(encodeSnapshot(snapshot)))
    }

    @Test
    fun `a field from a newer phone is ignored rather than fatal`() {
        val newer = """{"consumedKcal":1450,"budgetKcal":2000,"moodScore":4}"""
        val decoded = decodeSnapshot(newer)
        assertEquals(1450, decoded?.consumedKcal)
        assertEquals(2000, decoded?.budgetKcal)
    }

    @Test
    fun `a field an older phone never sent falls back to its default`() {
        val older = """{"consumedKcal":1450,"budgetKcal":2000}"""
        val decoded = decodeSnapshot(older)
        assertEquals(0, decoded?.steps)
        assertNull(decoded?.fastingUntilMillis)
    }

    @Test
    fun `garbage decodes to null rather than throwing`() {
        assertNull(decodeSnapshot("not json"))
        assertNull(decodeSnapshot(""))
    }

    @Test
    fun `an empty snapshot claims neither progress nor a hit water goal`() {
        val empty = TodaySnapshot()
        assertEquals(0f, empty.progress, 0f)
        assertFalse(empty.waterGoalReached)
    }

    @Test
    fun `over budget reports a negative remainder but a full bar`() {
        val over = snapshot.copy(consumedKcal = 2400)
        assertEquals(-400, over.remainingKcal)
        assertEquals(1f, over.progress, 0f)
    }

    @Test
    fun `adding a glass stops at the goal`() {
        assertEquals(6, snapshot.glassesAfterAdd)
        assertFalse(snapshot.waterGoalReached)

        val atGoal = snapshot.copy(glasses = 8)
        assertEquals(8, atGoal.glassesAfterAdd)
        assertTrue(atGoal.waterGoalReached)
    }
}
