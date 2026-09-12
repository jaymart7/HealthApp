package ph.mart.healthapp.feature.progress.ui.shared

import org.junit.Assert.assertEquals
import org.junit.Test
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.feature.progress.ui.shared.components.sampleBetween
import ph.mart.healthapp.feature.progress.ui.shared.components.sampleFrames

private fun photos(count: Int): List<ProgressPhoto> =
    (0 until count).map { ProgressPhoto(id = it.toLong(), dateEpochDay = 20_000L + it, filePath = "") }

class SampleFramesTest {

    @Test
    fun `keeps every photo when the run is shorter than the strip`() {
        val all = photos(3)
        assertEquals(all, sampleFrames(all, max = 4))
    }

    @Test
    fun `always keeps the first and the last`() {
        val sampled = sampleFrames(photos(20), max = 4)
        assertEquals(4, sampled.size)
        assertEquals(0L, sampled.first().id)
        assertEquals(19L, sampled.last().id)
    }

    @Test
    fun `spreads the middle frames evenly and never repeats one`() {
        val sampled = sampleFrames(photos(10), max = 4)
        assertEquals(listOf(0L, 3L, 6L, 9L), sampled.map { it.id })
        assertEquals(sampled.distinct(), sampled)
    }

    @Test
    fun `a strip of one is still a before and after`() {
        val sampled = sampleFrames(photos(10), max = 1)
        assertEquals(listOf(0L, 9L), sampled.map { it.id })
    }
}

/** A photo on a named day, so a gap in logging is something a test can state. */
private fun photoOn(id: Long, day: Long): ProgressPhoto =
    ProgressPhoto(id = id, dateEpochDay = day, filePath = "")

class SampleBetweenTest {

    private val run = listOf(
        photoOn(1, 20_000),
        photoOn(2, 20_010),
        photoOn(3, 20_030),
        photoOn(4, 20_060),
        photoOn(5, 20_090),
        photoOn(6, 20_092),
    )

    @Test
    fun `the picked pair bounds the strip`() {
        val sampled = sampleBetween(run, older = run[1], newer = run[4])
        assertEquals(2L, sampled.first().id)
        assertEquals(5L, sampled.last().id)
    }

    @Test
    fun `fills the middle from the thirds of the interval, by date`() {
        // 20_010 -> 20_090: thirds land on 20_036 and 20_063, nearest 20_030 and 20_060.
        val sampled = sampleBetween(run, older = run[1], newer = run[4])
        assertEquals(listOf(2L, 3L, 4L, 5L), sampled.map { it.id })
    }

    @Test
    fun `a pair with nothing logged between them stays two frames`() {
        val sampled = sampleBetween(run, older = run[4], newer = run[5])
        assertEquals(listOf(5L, 6L), sampled.map { it.id })
    }

    @Test
    fun `never pads and never repeats a shot`() {
        // One photo between the ends: both thirds resolve to it, and it appears once.
        val sampled = sampleBetween(run, older = run[0], newer = run[2])
        assertEquals(listOf(1L, 2L, 3L), sampled.map { it.id })
    }

    @Test
    fun `photos outside the picked pair are never pulled in`() {
        val sampled = sampleBetween(run, older = run[1], newer = run[3])
        assertEquals(emptyList<Long>(), sampled.map { it.id }.filter { it == 1L || it == 5L || it == 6L })
    }

    @Test
    fun `two shots on the same day still make a strip`() {
        val sameDay = listOf(photoOn(1, 20_000), photoOn(2, 20_000))
        assertEquals(listOf(1L, 2L), sampleBetween(sameDay, sameDay[0], sameDay[1]).map { it.id })
    }
}
