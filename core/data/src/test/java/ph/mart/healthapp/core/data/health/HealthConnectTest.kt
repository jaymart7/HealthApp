package ph.mart.healthapp.core.data.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureReading
import ph.mart.healthapp.core.data.health.local.HealthLinkEntity

/**
 * The two-provider rules, all of which are pure: which leg owns a type, which cloud rows a
 * handover retires, and the two guards that stop one measurement landing twice.
 *
 * Everything that touches `androidx.health.connect` lives in `HealthConnectSource.kt` precisely so
 * this file can exist — the same split `GoogleHealthApiTest` gets from the parsers being separated
 * from the sockets.
 */
class HealthConnectTest {

    private val hour = 3_600_000L
    private val day = 24 * hour

    private fun link(
        name: String,
        dataType: String,
        timeMillis: Long,
        pushed: Boolean = false,
    ) = HealthLinkEntity(
        remoteName = name,
        dataType = dataType,
        localTable = "exercise_entry",
        localId = 1,
        remoteTimeMillis = timeMillis,
        pushed = pushed,
    )

    // --- precedence -------------------------------------------------------------------------

    @Test
    fun `the cloud covers exactly what Health Connect does not`() {
        assertEquals(HealthMetric.entries.toSet(), cloudMetrics(connectGranted = emptySet()))
        assertEquals(emptySet<HealthMetric>(), cloudMetrics(HealthMetric.entries.toSet()))
    }

    @Test
    fun `a partial grant splits the types rather than deciding for all of them`() {
        // The ordinary Health Connect case: some switches on, some off.
        val cloud = cloudMetrics(setOf(HealthMetric.Steps, HealthMetric.Sleep))
        assertFalse(HealthMetric.Steps in cloud)
        assertFalse(HealthMetric.Sleep in cloud)
        assertTrue(HealthMetric.Exercise in cloud)
        assertTrue(HealthMetric.Weight in cloud)
        assertTrue(HealthMetric.Heart in cloud)
        assertTrue(HealthMetric.BloodPressure in cloud)
    }

    @Test
    fun `no metric can be claimed by both legs at once`() {
        // The property the whole dedup strategy rests on: one writer per table per sync.
        HealthMetric.entries.forEach { metric ->
            assertFalse("$metric was claimed by both providers", metric in cloudMetrics(setOf(metric)))
        }
    }

    @Test
    fun `each provider gets its own cursor, so one cannot advance the other`() {
        val cloudIds = HealthDataType.entries.map { it.id }
        HealthMetric.entries.forEach { metric ->
            assertFalse(
                "${metric.connectDataType} collides with a Google Health data type",
                metric.connectDataType in cloudIds,
            )
        }
    }

    // --- the handover -----------------------------------------------------------------------

    @Test
    fun `a handover retires the cloud rows inside the window Health Connect will re-import`() {
        val since = 100 * day
        val links = listOf(
            link("old", "exercise", since - day),
            link("inside", "exercise", since + hour),
            link("edge", "exercise", since),
        )
        val retired = supersededByConnect(links, cloudDataType = "exercise", sinceMillis = since)

        // The window is inclusive at its start, matching every other cursor in this module.
        assertEquals(listOf("inside", "edge"), retired.map { it.remoteName })
    }

    @Test
    fun `history older than Health Connect's reach survives the handover`() {
        // The point of windowing it: Health Connect backfills 30 days, so a year-old workout it
        // will never see must not be deleted on the strength of a provider swap.
        val since = 100 * day
        val links = listOf(link("ancient", "exercise", since - 300 * day))
        assertTrue(supersededByConnect(links, "exercise", since).isEmpty())
    }

    @Test
    fun `a handover touches neither another type nor anything FitPulse pushed`() {
        val since = 100 * day
        val links = listOf(
            link("other-type", "weight", since + hour),
            link("sent", "exercise", since + hour, pushed = true),
            link("mine", "exercise", since + hour),
        )
        assertEquals(listOf("mine"), supersededByConnect(links, "exercise", since).map { it.remoteName })
    }

    @Test
    fun `a Health Connect record's name cannot collide with a Google Health resource name`() {
        // The two providers key health_link differently, which is exactly why the handover has to
        // work by provenance and window rather than by matching ids.
        val connect = connectName("a1b2-c3d4")
        assertFalse(connect.startsWith("users/me/"))
        assertTrue(connect.endsWith("a1b2-c3d4"))
    }

    // --- blood pressure ---------------------------------------------------------------------

    private fun remoteBp(millis: Long, systolic: Int = 128, diastolic: Int = 82) =
        RemoteBloodPressure(
            remoteName = connectName("r$millis"),
            timeMillis = millis,
            systolic = systolic,
            diastolic = diastolic,
        )

    private fun heldBp(millis: Long, systolic: Int = 128, diastolic: Int = 82) =
        BloodPressureReading(
            id = 1,
            takenAtMillis = millis,
            systolic = systolic,
            diastolic = diastolic,
        )

    @Test
    fun `a reading typed by hand and imported a moment later is one reading`() {
        val held = listOf(heldBp(1_000_000))
        assertTrue(remoteBp(1_000_000).alreadyHeld(held))
        assertTrue(remoteBp(1_000_000 + 30_000).alreadyHeld(held))
    }

    @Test
    fun `morning and evening readings are two readings, and so are two different numbers`() {
        val held = listOf(heldBp(1_000_000))
        // Same numbers, hours apart — the whole reason this table is per-reading, not per-day.
        assertFalse(remoteBp(1_000_000 + 8 * hour).alreadyHeld(held))
        // Same minute, different measurement.
        assertFalse(remoteBp(1_000_000, systolic = 140).alreadyHeld(held))
        assertFalse(remoteBp(1_000_000, diastolic = 95).alreadyHeld(held))
        assertFalse(remoteBp(1_000_000).alreadyHeld(emptyList()))
    }

    @Test
    fun `an imported reading carries no pulse rather than a pulse of zero beats`() {
        // Health Connect records a pulse as a HeartRateRecord, not a field of the reading. Zero is
        // mood_day's zero: not entered.
        assertEquals(0, remoteBp(1_000_000).toBloodPressureReading().pulseBpm)
        assertEquals(128, remoteBp(1_000_000).toBloodPressureReading().systolic)
        assertEquals(1_000_000L, remoteBp(1_000_000).toBloodPressureReading().takenAtMillis)
    }

    // --- sleep ------------------------------------------------------------------------------

    @Test
    fun `a restless hour inside a session is not counted as sleep`() {
        val start = 1_000_000L
        val end = start + 8 * hour
        assertEquals(8 * 60, asleepMinutes(start, end, awakeMillis = 0))
        assertEquals(7 * 60, asleepMinutes(start, end, awakeMillis = hour))
    }

    @Test
    fun `a session whose stages outrun it reports no sleep rather than a negative night`() {
        val start = 1_000_000L
        assertEquals(0, asleepMinutes(start, start + hour, awakeMillis = 2 * hour))
        assertEquals(0, asleepMinutes(start, start, awakeMillis = 0))
    }
}
