package ph.mart.healthapp.feature.progress.ui.timelapse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.progress.ProgressPhoto

private fun photoOn(id: Long, day: Long): ProgressPhoto =
    ProgressPhoto(id = id, dateEpochDay = day, filePath = "")

/**
 * Three shots in a week, then three weeks of nothing, then one more — the shape the timeline
 * exists to show and the shape an index slider draws as four equal steps.
 */
private val unevenRun = listOf(
    photoOn(1, 20_000),
    photoOn(2, 20_002),
    photoOn(3, 20_004),
    photoOn(4, 20_025),
)

class TimelapseTimelineTest {

    @Test
    fun `ticks sit where the shots were actually taken`() {
        val fractions = tickFractions(unevenRun)
        assertEquals(0f, fractions.first(), 0.0001f)
        assertEquals(1f, fractions.last(), 0.0001f)
        // The first three crowd the left; the gap is the empty stretch after them.
        assertEquals(2f / 25f, fractions[1], 0.0001f)
        assertEquals(4f / 25f, fractions[2], 0.0001f)
        assertTrue("the gap should be the widest stretch", fractions[3] - fractions[2] > fractions[2] - fractions[0])
    }

    @Test
    fun `an evenly logged run gives evenly spaced ticks`() {
        val even = (0 until 5).map { photoOn(it.toLong(), 20_000L + it * 10) }
        assertEquals(listOf(0f, 0.25f, 0.5f, 0.75f, 1f), tickFractions(even))
    }

    @Test
    fun `a set logged entirely on one day falls back to even spacing`() {
        // No range to place them in — stacking every tick at 0f would draw one mark for the set.
        val sameDay = (0 until 3).map { photoOn(it.toLong(), 20_000) }
        assertEquals(listOf(0f, 0.5f, 1f), tickFractions(sameDay))
    }

    @Test
    fun `a single shot and an empty set have no range to divide by`() {
        assertEquals(listOf(0f), tickFractions(listOf(photoOn(1, 20_000))))
        assertEquals(emptyList<Float>(), tickFractions(emptyList()))
    }

    @Test
    fun `scrubbing snaps to the nearer end of a gap, not the nearer index`() {
        // Ticks land at 0, 0.08, 0.16, 1.0. Halfway along the track is inside the empty stretch,
        // and the shot before the gap is the nearer of the two bounding it — an index-based
        // scrubber would have answered the fourth shot, since 0.5 is past its halfway point.
        assertEquals(2, nearestFrame(0.5f, unevenRun))
        assertEquals(3, nearestFrame(0.7f, unevenRun))
    }

    @Test
    fun `the ends of the track are the ends of the run`() {
        assertEquals(0, nearestFrame(0f, unevenRun))
        assertEquals(unevenRun.lastIndex, nearestFrame(1f, unevenRun))
        // Out of range from a fling or a stray semantics value.
        assertEquals(0, nearestFrame(-2f, unevenRun))
        assertEquals(unevenRun.lastIndex, nearestFrame(4f, unevenRun))
    }

    @Test
    fun `every fade finishes before the frame it is fading into is due`() {
        TIMELAPSE_FPS.indices.forEach { speed ->
            assertTrue(
                "speed $speed fades for ${fadeMillis(speed)}ms inside ${frameIntervalMillis(speed)}ms",
                fadeMillis(speed) < frameIntervalMillis(speed),
            )
        }
    }

    @Test
    fun `a speed index out of range is clamped rather than thrown`() {
        assertEquals(fadeMillis(0), fadeMillis(-1))
        assertEquals(fadeMillis(TIMELAPSE_FPS.lastIndex), fadeMillis(99))
        assertEquals(frameIntervalMillis(0), frameIntervalMillis(-1))
    }
}
