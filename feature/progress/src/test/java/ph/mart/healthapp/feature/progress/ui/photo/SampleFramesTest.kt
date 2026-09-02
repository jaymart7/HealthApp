package ph.mart.healthapp.feature.progress.ui.photo

import org.junit.Assert.assertEquals
import org.junit.Test
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.feature.progress.ui.photo.components.sampleFrames

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
